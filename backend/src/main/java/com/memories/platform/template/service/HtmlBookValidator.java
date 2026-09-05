package com.memories.platform.template.service;

import com.memories.platform.template.dto.HtmlBook;
import com.memories.platform.template.exception.InvalidTemplateContractException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** A deliberately small HTML dialect: no executable content or external resource URLs. */
public final class HtmlBookValidator {
    private static final Set<String> TAGS = Set.of("section", "div", "header", "footer", "article",
            "h1", "h2", "h3", "h4", "p", "span", "strong", "em", "small", "br", "hr",
            "img", "figure", "figcaption", "ul", "ol", "li", "a", "time", "blockquote");
    private static final Set<String> TYPES = Set.of("COVER", "CONTENT", "GALLERY", "SCHEDULE",
            "LOCATION", "RSVP", "GUEST_MESSAGES", "CLOSING");
    private static final Set<String> ROOT_FIELDS = Set.of("title", "summary", "eventStartAt", "cover.deliveryUrl");
    private static final Map<String, Set<String>> FIELDS = Map.of(
            "members", Set.of("fullName", "displayName", "description", "avatar.deliveryUrl", "roleCode"),
            "sections", Set.of("title", "contentText"),
            "images", Set.of("caption", "altText", "asset.deliveryUrl"),
            "events", Set.of("title", "description", "startAt", "endAt", "timezone"),
            "locations", Set.of("name", "address", "mapUrl"));
    private static final Set<String> CSS_PROPERTIES = Set.of("color", "background", "background-color",
            "border", "border-top", "border-bottom", "border-left", "border-right", "border-color",
            "border-width", "border-style", "border-radius", "padding", "padding-top", "padding-bottom",
            "padding-left", "padding-right", "margin", "margin-top", "margin-bottom", "margin-left",
            "margin-right", "font-family", "font-size", "font-weight", "font-style", "line-height",
            "letter-spacing", "text-align", "text-transform", "text-decoration", "display", "gap",
            "row-gap", "column-gap", "grid-template-columns", "align-items", "justify-content",
            "flex-direction", "flex-wrap", "width", "max-width", "min-height", "height", "max-height",
            "object-fit", "object-position", "aspect-ratio", "box-shadow", "white-space", "overflow-wrap");
    private static final Pattern CLASS = Pattern.compile("[a-z][a-z0-9-]*( [a-z][a-z0-9-]*)*");
    private static final Pattern SELECTOR = Pattern.compile("\\.[a-z][a-z0-9-]*( +\\.[a-z][a-z0-9-]*)*");
    private static final Pattern CSS_VALUE = Pattern.compile("[a-zA-Z0-9#.,% ()/+*'\\\"-]+");
    private static final Pattern FUNCTIONS = Pattern.compile("([a-zA-Z-]+)\\s*\\(");
    private static final Set<String> SAFE_FUNCTIONS = Set.of("rgb", "rgba", "hsl", "hsla", "linear-gradient",
            "radial-gradient", "repeat", "minmax", "clamp", "calc", "min", "max");

    private HtmlBookValidator() {}

    public static void validate(String renderer, HtmlBook book) {
        if (!"html-book".equals(renderer)) {
            require(book == null, "Only html-book accepts book content.");
            return;
        }
        require(book != null && book.config() != null, "Book configuration is required.");
        HtmlBook.Config config = book.config();
        require(config.background() != null && config.background().matches("#[0-9a-fA-F]{6}"), "Invalid background color.");
        require(config.paper() != null && config.paper().matches("#[0-9a-fA-F]{6}"), "Invalid paper color.");
        require("ltr".equals(config.direction()) || "rtl".equals(config.direction()), "Direction must be ltr or rtl.");
        require("flip".equals(config.effect()) || "none".equals(config.effect()), "Effect must be flip or none.");
        require(Double.isFinite(config.aspectRatio()) && config.aspectRatio() >= 0.6 && config.aspectRatio() <= 1,
                "Page aspect ratio must be between 0.6 and 1.");
        require(book.pages() != null && book.pages().size() >= 2 && book.pages().size() <= 16,
                "A book needs 2 to 16 pages.");
        validateCss(book.css());
        Set<String> keys = new HashSet<>();
        Set<String> slots = new HashSet<>();
        int total = book.css().length();
        for (int index = 0; index < book.pages().size(); index++) {
            HtmlBook.Page page = book.pages().get(index);
            require(page != null && page.key() != null && page.key().matches("[a-z][a-z0-9-]{0,59}")
                    && keys.add(page.key()), "Page keys must be unique lowercase identifiers.");
            require(page.name() != null && !page.name().isBlank() && page.name().length() <= 120, "Invalid page name.");
            require(page.type() != null && TYPES.contains(page.type()), "Unknown page type.");
            require(index == 0 ? "COVER".equals(page.type()) : !"COVER".equals(page.type()), "Only the first page is the cover.");
            require(page.html() != null && !page.html().isBlank() && page.html().length() <= 20000,
                    "Each page must contain at most 20,000 characters of HTML.");
            total += page.html().length();
            validateHtml(page.html(), slots);
        }
        require(total <= 120000, "Book HTML and CSS must not exceed 120,000 characters.");
        require(slots.contains("RSVP") && slots.contains("GUEST_MESSAGES"), "Book must contain one RSVP and one GUEST_MESSAGES slot.");
    }

    private static void validateHtml(String html, Set<String> slots) {
        try {
            // XHTML fragments allow the platform parser to reject malformed markup without a new dependency.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            var document = builder.parse(new InputSource(new StringReader("<root>" + html + "</root>")));
            int[] count = {0};
            for (Node node = document.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling()) {
                validateNode(node, null, slots, 0, count);
            }
        } catch (InvalidTemplateContractException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid("HTML must be well-formed: close tags, use <img /> and escape & as &amp;.");
        }
    }

    private static void validateNode(Node node, String context, Set<String> slots, int depth, int[] count) {
        require(++count[0] <= 500 && depth <= 20, "HTML is too deeply nested or contains too many nodes.");
        if (node.getNodeType() == Node.TEXT_NODE) return;
        require(node instanceof Element, "Only elements and text are accepted.");
        Element element = (Element) node;
        String tag = element.getTagName();
        require(TAGS.contains(tag), "Unsupported HTML tag: " + tag);
        if (element.hasAttribute("data-repeat")) {
            String repeat = element.getAttribute("data-repeat");
            require(context == null && FIELDS.containsKey(repeat), "Use one list binding per branch; nested repeats are not supported.");
            context = repeat;
        }
        var attributes = element.getAttributes();
        for (int index = 0; index < attributes.getLength(); index++) {
            Node attribute = attributes.item(index);
            String value = attribute.getNodeValue();
            switch (attribute.getNodeName()) {
                case "class" -> require(CLASS.matcher(value).matches(), "Use lowercase class names.");
                case "alt" -> require("img".equals(tag) && value.length() <= 255, "Invalid image alt text.");
                case "data-repeat" -> { /* checked above */ }
                case "data-bind", "data-alt" -> require(isField(value, context), "Unknown text binding: " + value);
                case "data-src" -> require("img".equals(tag) && isField(value, context)
                        && value.endsWith(".deliveryUrl"), "Images must bind a media delivery URL.");
                case "data-href" -> require("a".equals(tag) && "locations".equals(context)
                        && "item.mapUrl".equals(value), "Links must bind item.mapUrl in locations.");
                case "data-format" -> require("date".equals(value) && element.hasAttribute("data-bind"), "Unknown text format.");
                case "data-slot" -> require("div".equals(tag) && context == null
                        && Set.of("RSVP", "GUEST_MESSAGES", "MAP").contains(value) && slots.add(value)
                        && !element.hasChildNodes() && attributes.getLength() == 1,
                        "Slots must be empty, unique div elements outside repeated lists.");
                default -> throw invalid("Unsupported HTML attribute: " + attribute.getNodeName());
            }
        }
        require(!"img".equals(tag) || element.hasAttribute("data-src"), "Images require data-src.");
        require(!element.hasAttribute("data-bind") || !element.hasChildNodes(), "Text binding elements must be empty.");
        require(!Set.of("img", "br", "hr").contains(tag) || !element.hasChildNodes(), "Void tags cannot contain content.");
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            validateNode(child, context, slots, depth + 1, count);
        }
    }

    private static boolean isField(String field, String context) {
        return ROOT_FIELDS.contains(field)
                || field.matches("themeConfig\\.[a-zA-Z][a-zA-Z0-9]{0,49}")
                || (context != null && field.startsWith("item.") && FIELDS.get(context).contains(field.substring(5)));
    }

    private static void validateCss(String css) {
        require(css != null && css.length() <= 20000, "CSS must contain at most 20,000 characters.");
        String remaining = css.trim();
        while (!remaining.isEmpty()) {
            int open = remaining.indexOf('{');
            int close = remaining.indexOf('}');
            require(open > 0 && close > open, "CSS must be a list of class rules.");
            String selector = remaining.substring(0, open).trim();
            require(SELECTOR.matcher(selector).matches(), "CSS selectors must contain classes separated by spaces.");
            String declarations = remaining.substring(open + 1, close);
            for (String declaration : declarations.split(";")) {
                if (declaration.isBlank()) continue;
                int colon = declaration.indexOf(':');
                require(colon > 0, "Invalid CSS declaration.");
                String property = declaration.substring(0, colon).trim();
                String value = declaration.substring(colon + 1).trim();
                require(CSS_PROPERTIES.contains(property) && CSS_VALUE.matcher(value).matches(), "Unsupported CSS property or value: " + property);
                var functions = FUNCTIONS.matcher(value);
                while (functions.find()) require(SAFE_FUNCTIONS.contains(functions.group(1)), "Unsupported CSS function.");
            }
            remaining = remaining.substring(close + 1).trim();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw invalid(message);
    }

    private static InvalidTemplateContractException invalid(String message) {
        return new InvalidTemplateContractException("TEMPLATE_BOOK_INVALID", message);
    }
}

package com.memories.platform.template.dto;

import java.util.List;

public record HtmlBook(Config config, String css, List<Page> pages) {
    public record Config(String background, String paper, String direction, String effect,
                         boolean desktopSpread, double aspectRatio) {}

    public record Page(String key, String name, String type, String html) {}
}

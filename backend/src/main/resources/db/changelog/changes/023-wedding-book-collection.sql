--liquibase formatted sql

--changeset memories:023-wedding-book-collection

INSERT INTO templates (id, code, name, memory_type, description, status, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000001', 'WEDDING_MINIMAL_IVORY', 'Minimal Ivory', 'WEDDING', 'Giấy ngà, khoảng trắng rộng và chữ serif. Thiệp sáu trang với bố cục tối giản.', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO template_versions (id, template_id, version_no, component_key, renderer_version, cover_required,
    config_schema, default_config, required_sections, section_contracts, book_config, css_content,
    status, published_at, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000101', '60000000-0000-0000-0000-000000000001', 1, 'html-book', '1', FALSE,
    '{"type":"object","properties":{"subtitle":{"type":"string","maxLength":200}},"additionalProperties":false}'::jsonb,
    '{"subtitle":"Trân trọng kính mời"}'::jsonb, '[]'::jsonb,
    '{"STORY":{"configSchema":{"type":"object","additionalProperties":false}},"GALLERY":{"configSchema":{"type":"object","additionalProperties":false}}}'::jsonb,
    '{"background":"#e5dfd5","paper":"#fffdf5","direction":"ltr","effect":"flip","desktopSpread":true,"aspectRatio":0.72}'::jsonb,
    '.cover { text-align: center; color: #39362e; font-family: Georgia; }
.interior { color: #39362e; font-family: Georgia; }
.eyebrow { text-transform: uppercase; letter-spacing: 3px; font-size: 11px; }
.subtitle { font-size: 16px; }
.date { font-size: 13px; letter-spacing: 1px; }
.lead { font-size: 20px; line-height: 1.8; margin: 28px 0; }
.name { font-size: 26px; font-style: italic; }
.people { margin: 26px 0; }
.story { margin: 24px 0; }
.event { margin: 26px 0; }
.venue { margin: 28px 0; }
.photo { margin: 16px 0; }
.closing { text-align: center; padding: 28px 10px; }
.minimal { padding: 48px 12px; } .names { font-size: 48px; font-weight: 400; letter-spacing: -2px; margin: 60px 0; } .people { border-top: 1px solid #d8d0c1; padding-top: 20px; } .photo { border-radius: 2px; }', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000101', 0, 'cover', 'Bìa', 'COVER', '<section class="cover minimal"><p class="eyebrow">Together with our families</p><hr /><p class="subtitle" data-bind="themeConfig.subtitle"></p><h1 class="names" data-bind="title"></h1><p class="date" data-bind="eventStartAt" data-format="date"></p><hr /><p>Ngày chung đôi</p></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000101', 1, 'invitation', 'Lời mời', 'CONTENT', '<section class="interior"><p class="eyebrow">01 / Lời mời</p><h2>Hạnh phúc là<br />có nhau.</h2><p class="lead" data-bind="summary"></p><hr /><div class="people" data-repeat="members"><p class="name" data-bind="item.fullName"></p><p data-bind="item.description"></p></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000101', 2, 'story', 'Chuyện chúng mình', 'GALLERY', '<section class="interior"><p class="eyebrow">02 / Kỷ niệm</p><h2>Những điều<br />ta gìn giữ</h2><article class="story" data-repeat="sections"><h3 data-bind="item.title"></h3><p data-bind="item.contentText"></p></article><div class="album"><figure data-repeat="images"><img class="photo" data-src="item.asset.deliveryUrl" data-alt="item.altText" /><figcaption data-bind="item.caption"></figcaption></figure></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000101', 3, 'schedule', 'Ngày chung vui', 'SCHEDULE', '<section class="interior"><p class="eyebrow">Save the date</p><h2>Ngày chung vui</h2><article class="event" data-repeat="events"><p class="date" data-bind="item.startAt" data-format="date"></p><h3 data-bind="item.title"></h3><p data-bind="item.description"></p></article></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000101', 4, 'venue', 'Địa điểm & hồi đáp', 'LOCATION', '<section class="interior"><p class="eyebrow">Hẹn gặp bạn</p><h2>Nơi hạnh phúc<br />bắt đầu</h2><article class="venue" data-repeat="locations"><h3 data-bind="item.name"></h3><p data-bind="item.address"></p><a data-href="item.mapUrl">Xem đường đi</a></article><div data-slot="RSVP"></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000101', 5, 'closing', 'Lời chúc', 'CLOSING', '<section class="interior closing"><p class="eyebrow">With love</p><h2>Cảm ơn bạn<br />đã bên chúng mình</h2><p>Sự hiện diện của bạn là món quà ý nghĩa trong ngày trọng đại.</p><h3 data-bind="title"></h3><div data-slot="GUEST_MESSAGES"></div></section>');

INSERT INTO templates (id, code, name, memory_type, description, status, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000002', 'WEDDING_BOTANICAL_ROMANCE', 'Botanical Romance', 'WEDDING', 'Vòm lá olive, đường viền mềm và trang album như một khu vườn nhỏ.', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO template_versions (id, template_id, version_no, component_key, renderer_version, cover_required,
    config_schema, default_config, required_sections, section_contracts, book_config, css_content,
    status, published_at, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000102', '60000000-0000-0000-0000-000000000002', 1, 'html-book', '1', FALSE,
    '{"type":"object","properties":{"subtitle":{"type":"string","maxLength":200}},"additionalProperties":false}'::jsonb,
    '{"subtitle":"Trân trọng kính mời"}'::jsonb, '[]'::jsonb,
    '{"STORY":{"configSchema":{"type":"object","additionalProperties":false}},"GALLERY":{"configSchema":{"type":"object","additionalProperties":false}}}'::jsonb,
    '{"background":"#dbe3d5","paper":"#f8faef","direction":"ltr","effect":"flip","desktopSpread":true,"aspectRatio":0.72}'::jsonb,
    '.cover { text-align: center; color: #2d4534; font-family: Georgia; }
.interior { color: #2d4534; font-family: Georgia; }
.eyebrow { text-transform: uppercase; letter-spacing: 3px; font-size: 11px; }
.subtitle { font-size: 16px; }
.date { font-size: 13px; letter-spacing: 1px; }
.lead { font-size: 20px; line-height: 1.8; margin: 28px 0; }
.name { font-size: 26px; font-style: italic; }
.people { margin: 26px 0; }
.story { margin: 24px 0; }
.event { margin: 26px 0; }
.venue { margin: 28px 0; }
.photo { margin: 16px 0; }
.closing { text-align: center; padding: 28px 10px; }
.botanical { border: 2px solid #647c52; border-radius: 180px 180px 12px 12px; background: radial-gradient(ellipse at top, #e2ebd2, #f8faef); padding: 38px 20px; } .sprig { display: block; font-size: 70px; color: #647c52; text-align: center; line-height: 1.2; } .names { font-weight: 400; font-style: italic; margin: 28px 0; } .seal { border: 1px solid #647c52; border-radius: 50%; padding: 24px 8px; max-width: 130px; margin: 24px auto; font-size: 15px; } .garden { border-left: 2px solid #b5c6a2; padding-left: 22px; } .photo { border-radius: 120px 120px 8px 8px; } .event { border-left: 3px solid #647c52; padding-left: 18px; }', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000102', 0, 'cover', 'Bìa', 'COVER', '<section class="cover botanical"><span class="sprig">❦</span><p class="eyebrow">A garden wedding</p><p data-bind="themeConfig.subtitle"></p><h1 class="names" data-bind="title"></h1><div class="seal">Mãi bên nhau</div><p class="date" data-bind="eventStartAt" data-format="date"></p><span class="sprig">❧</span></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000102', 1, 'invitation', 'Lời mời', 'CONTENT', '<section class="interior garden"><span class="sprig">❦</span><h2>Tình yêu<br />đơm hoa</h2><p class="lead" data-bind="summary"></p><div class="pair"><div class="people" data-repeat="members"><p class="name" data-bind="item.fullName"></p><p data-bind="item.description"></p></div></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000102', 2, 'story', 'Chuyện chúng mình', 'GALLERY', '<section class="interior garden"><p class="eyebrow">Our growing love</p><article class="story" data-repeat="sections"><h3 data-bind="item.title"></h3><p data-bind="item.contentText"></p></article><h2>Một vườn<br />thương nhớ</h2><div class="album"><figure data-repeat="images"><img class="photo" data-src="item.asset.deliveryUrl" data-alt="item.altText" /><figcaption data-bind="item.caption"></figcaption></figure></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000102', 3, 'schedule', 'Ngày chung vui', 'SCHEDULE', '<section class="interior"><p class="eyebrow">Save the date</p><h2>Ngày chung vui</h2><article class="event" data-repeat="events"><p class="date" data-bind="item.startAt" data-format="date"></p><h3 data-bind="item.title"></h3><p data-bind="item.description"></p></article></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000102', 4, 'venue', 'Địa điểm & hồi đáp', 'LOCATION', '<section class="interior"><p class="eyebrow">Hẹn gặp bạn</p><h2>Nơi hạnh phúc<br />bắt đầu</h2><article class="venue" data-repeat="locations"><h3 data-bind="item.name"></h3><p data-bind="item.address"></p><a data-href="item.mapUrl">Xem đường đi</a></article><div data-slot="RSVP"></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000102', 5, 'closing', 'Lời chúc', 'CLOSING', '<section class="interior closing"><p class="eyebrow">With love</p><h2>Cảm ơn bạn<br />đã bên chúng mình</h2><p>Sự hiện diện của bạn là món quà ý nghĩa trong ngày trọng đại.</p><h3 data-bind="title"></h3><div data-slot="GUEST_MESSAGES"></div></section>');

INSERT INTO templates (id, code, name, memory_type, description, status, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000003', 'WEDDING_MODERN_EDITORIAL', 'Modern Editorial', 'WEDDING', 'Bìa ảnh cưới khổ lớn, chữ đậm và bố cục tạp chí hiện đại.', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO template_versions (id, template_id, version_no, component_key, renderer_version, cover_required,
    config_schema, default_config, required_sections, section_contracts, book_config, css_content,
    status, published_at, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000103', '60000000-0000-0000-0000-000000000003', 1, 'html-book', '1', FALSE,
    '{"type":"object","properties":{"subtitle":{"type":"string","maxLength":200}},"additionalProperties":false}'::jsonb,
    '{"subtitle":"Trân trọng kính mời"}'::jsonb, '[]'::jsonb,
    '{"STORY":{"configSchema":{"type":"object","additionalProperties":false}},"GALLERY":{"configSchema":{"type":"object","additionalProperties":false}}}'::jsonb,
    '{"background":"#cfcbc5","paper":"#f7f5ef","direction":"ltr","effect":"flip","desktopSpread":true,"aspectRatio":0.72}'::jsonb,
    '.cover { text-align: center; color: #252523; font-family: Arial; }
.interior { color: #252523; font-family: Arial; }
.eyebrow { text-transform: uppercase; letter-spacing: 3px; font-size: 11px; }
.subtitle { font-size: 16px; }
.date { font-size: 13px; letter-spacing: 1px; }
.lead { font-size: 20px; line-height: 1.8; margin: 28px 0; }
.name { font-size: 26px; font-style: italic; }
.people { margin: 26px 0; }
.story { margin: 24px 0; }
.event { margin: 26px 0; }
.venue { margin: 28px 0; }
.photo { margin: 16px 0; }
.closing { text-align: center; padding: 28px 10px; }
.editorial { text-align: left; padding: 0; } .masthead { border-top: 3px solid #252523; border-bottom: 1px solid #252523; padding: 14px 0; } .edition { font-size: 12px; text-align: right; } .names { font-size: 60px; text-transform: uppercase; letter-spacing: -3px; margin: 24px 0; line-height: 1; } .portrait { aspect-ratio: 0.9; object-fit: cover; } .caption-row { display: flex; justify-content: space-between; gap: 16px; padding-top: 16px; font-size: 12px; } .chapter { font-size: 70px; font-weight: 700; color: #8d5946; } .pair { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; } .album { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; } .event { border-top: 2px solid #252523; padding-top: 24px; }', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000103', 0, 'cover', 'Bìa', 'COVER', '<section class="cover editorial"><div class="masthead"><p class="eyebrow">THE WEDDING ISSUE</p><p class="edition">No. 01 — Forever</p></div><h1 class="names" data-bind="title"></h1><img class="portrait" data-src="cover.deliveryUrl" data-alt="title" /><div class="caption-row"><p data-bind="themeConfig.subtitle"></p><p class="date" data-bind="eventStartAt" data-format="date"></p></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000103', 1, 'invitation', 'Lời mời', 'CONTENT', '<section class="interior"><p class="chapter">01.</p><h2>WE ARE<br />GETTING<br />MARRIED.</h2><p class="lead" data-bind="summary"></p><div class="pair"><div class="people" data-repeat="members"><p class="name" data-bind="item.fullName"></p><p data-bind="item.description"></p></div></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000103', 2, 'story', 'Chuyện chúng mình', 'GALLERY', '<section class="interior"><div class="masthead"><h2>THE STORY</h2><p class="eyebrow">Một hành trình / Hai người</p></div><article class="story" data-repeat="sections"><h3 data-bind="item.title"></h3><p data-bind="item.contentText"></p></article><div class="album"><figure data-repeat="images"><img class="photo" data-src="item.asset.deliveryUrl" data-alt="item.altText" /><figcaption data-bind="item.caption"></figcaption></figure></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000103', 3, 'schedule', 'Ngày chung vui', 'SCHEDULE', '<section class="interior"><p class="eyebrow">Save the date</p><h2>Ngày chung vui</h2><article class="event" data-repeat="events"><p class="date" data-bind="item.startAt" data-format="date"></p><h3 data-bind="item.title"></h3><p data-bind="item.description"></p></article></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000103', 4, 'venue', 'Địa điểm & hồi đáp', 'LOCATION', '<section class="interior"><p class="eyebrow">Hẹn gặp bạn</p><h2>Nơi hạnh phúc<br />bắt đầu</h2><article class="venue" data-repeat="locations"><h3 data-bind="item.name"></h3><p data-bind="item.address"></p><a data-href="item.mapUrl">Xem đường đi</a></article><div data-slot="RSVP"></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000103', 5, 'closing', 'Lời chúc', 'CLOSING', '<section class="interior closing"><p class="eyebrow">With love</p><h2>Cảm ơn bạn<br />đã bên chúng mình</h2><p>Sự hiện diện của bạn là món quà ý nghĩa trong ngày trọng đại.</p><h3 data-bind="title"></h3><div data-slot="GUEST_MESSAGES"></div></section>');

INSERT INTO templates (id, code, name, memory_type, description, status, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000004', 'WEDDING_CLASSIC_GOLD', 'Classic Gold', 'WEDDING', 'Giấy champagne, khung viền kép ánh vàng và bố cục nghi lễ trang trọng.', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO template_versions (id, template_id, version_no, component_key, renderer_version, cover_required,
    config_schema, default_config, required_sections, section_contracts, book_config, css_content,
    status, published_at, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000104', '60000000-0000-0000-0000-000000000004', 1, 'html-book', '1', FALSE,
    '{"type":"object","properties":{"subtitle":{"type":"string","maxLength":200}},"additionalProperties":false}'::jsonb,
    '{"subtitle":"Trân trọng kính mời"}'::jsonb, '[]'::jsonb,
    '{"STORY":{"configSchema":{"type":"object","additionalProperties":false}},"GALLERY":{"configSchema":{"type":"object","additionalProperties":false}}}'::jsonb,
    '{"background":"#28251f","paper":"#fff4dc","direction":"ltr","effect":"flip","desktopSpread":true,"aspectRatio":0.72}'::jsonb,
    '.cover { text-align: center; color: #463822; font-family: Georgia; }
.interior { color: #463822; font-family: Georgia; }
.eyebrow { text-transform: uppercase; letter-spacing: 3px; font-size: 11px; }
.subtitle { font-size: 16px; }
.date { font-size: 13px; letter-spacing: 1px; }
.lead { font-size: 20px; line-height: 1.8; margin: 28px 0; }
.name { font-size: 26px; font-style: italic; }
.people { margin: 26px 0; }
.story { margin: 24px 0; }
.event { margin: 26px 0; }
.venue { margin: 28px 0; }
.photo { margin: 16px 0; }
.closing { text-align: center; padding: 28px 10px; }
.classic { border: 6px double #a5823e; padding: 34px 20px; background: linear-gradient(135deg, #fff8e6, #eddbaf, #fff8e6); } .crest { font-size: 72px; color: #a5823e; text-align: center; } .gold-rule { height: 2px; background: linear-gradient(90deg, #fff4dc, #a5823e, #fff4dc); margin: 30px 0; } .names { font-style: italic; font-weight: 400; font-size: 46px; margin: 34px 0; } .formal { border: 1px solid #a5823e; padding: 24px 18px; text-align: center; } .photo { border: 5px double #a5823e; padding: 5px; } .event { border-bottom: 1px solid #a5823e; padding-bottom: 22px; text-align: center; }', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000104', 0, 'cover', 'Bìa', 'COVER', '<section class="cover classic"><div class="crest">◇</div><p class="eyebrow">Wedding Invitation</p><div class="gold-rule"></div><p data-bind="themeConfig.subtitle"></p><h1 class="names" data-bind="title"></h1><p>Trăm năm hạnh phúc</p><div class="gold-rule"></div><p class="date" data-bind="eventStartAt" data-format="date"></p></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000104', 1, 'invitation', 'Lời mời', 'CONTENT', '<section class="interior formal"><p class="eyebrow">Lễ thành hôn</p><h2>Trân trọng<br />kính mời</h2><div class="gold-rule"></div><div class="people" data-repeat="members"><p class="name" data-bind="item.fullName"></p><p data-bind="item.description"></p></div><p class="lead" data-bind="summary"></p></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000104', 2, 'story', 'Chuyện chúng mình', 'GALLERY', '<section class="interior formal"><div class="crest">◇</div><h2>Dấu ấn<br />tình yêu</h2><article class="story" data-repeat="sections"><h3 data-bind="item.title"></h3><p data-bind="item.contentText"></p></article><div class="album"><figure data-repeat="images"><img class="photo" data-src="item.asset.deliveryUrl" data-alt="item.altText" /><figcaption data-bind="item.caption"></figcaption></figure></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000104', 3, 'schedule', 'Ngày chung vui', 'SCHEDULE', '<section class="interior"><p class="eyebrow">Save the date</p><h2>Ngày chung vui</h2><article class="event" data-repeat="events"><p class="date" data-bind="item.startAt" data-format="date"></p><h3 data-bind="item.title"></h3><p data-bind="item.description"></p></article></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000104', 4, 'venue', 'Địa điểm & hồi đáp', 'LOCATION', '<section class="interior"><p class="eyebrow">Hẹn gặp bạn</p><h2>Nơi hạnh phúc<br />bắt đầu</h2><article class="venue" data-repeat="locations"><h3 data-bind="item.name"></h3><p data-bind="item.address"></p><a data-href="item.mapUrl">Xem đường đi</a></article><div data-slot="RSVP"></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000104', 5, 'closing', 'Lời chúc', 'CLOSING', '<section class="interior closing"><p class="eyebrow">With love</p><h2>Cảm ơn bạn<br />đã bên chúng mình</h2><p>Sự hiện diện của bạn là món quà ý nghĩa trong ngày trọng đại.</p><h3 data-bind="title"></h3><div data-slot="GUEST_MESSAGES"></div></section>');

INSERT INTO templates (id, code, name, memory_type, description, status, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000005', 'WEDDING_WATERCOLOR_GARDEN', 'Watercolor Garden', 'WEDDING', 'Mảng màu pastel mềm, khung ảnh hữu cơ và lời mời gần gũi.', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO template_versions (id, template_id, version_no, component_key, renderer_version, cover_required,
    config_schema, default_config, required_sections, section_contracts, book_config, css_content,
    status, published_at, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000105', '60000000-0000-0000-0000-000000000005', 1, 'html-book', '1', FALSE,
    '{"type":"object","properties":{"subtitle":{"type":"string","maxLength":200}},"additionalProperties":false}'::jsonb,
    '{"subtitle":"Trân trọng kính mời"}'::jsonb, '[]'::jsonb,
    '{"STORY":{"configSchema":{"type":"object","additionalProperties":false}},"GALLERY":{"configSchema":{"type":"object","additionalProperties":false}}}'::jsonb,
    '{"background":"#eadbe0","paper":"#fff7f6","direction":"ltr","effect":"flip","desktopSpread":true,"aspectRatio":0.72}'::jsonb,
    '.cover { text-align: center; color: #68434f; font-family: Georgia; }
.interior { color: #68434f; font-family: Georgia; }
.eyebrow { text-transform: uppercase; letter-spacing: 3px; font-size: 11px; }
.subtitle { font-size: 16px; }
.date { font-size: 13px; letter-spacing: 1px; }
.lead { font-size: 20px; line-height: 1.8; margin: 28px 0; }
.name { font-size: 26px; font-style: italic; }
.people { margin: 26px 0; }
.story { margin: 24px 0; }
.event { margin: 26px 0; }
.venue { margin: 28px 0; }
.photo { margin: 16px 0; }
.closing { text-align: center; padding: 28px 10px; }
.watercolor { border-radius: 44% 44% 8px 8px; padding: 40px 20px; background: radial-gradient(ellipse at top left, #edc6d8, #fff7f6, #e2ebd8); } .bloom { font-size: 84px; color: #aa6f87; display: block; text-align: center; line-height: 1.4; } .names { font-style: italic; font-weight: 400; font-size: 46px; margin: 24px 0; } .handwritten { font-style: italic; font-size: 24px; margin-top: 34px; } .soft { background: radial-gradient(ellipse at bottom right, #f0d9df, #fff7f6); border-radius: 22px; padding: 22px; } .photo { border-radius: 40% 12% 32% 10%; } .event { background: #f4e5e8; border-radius: 18px; padding: 18px; }', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000105', 0, 'cover', 'Bìa', 'COVER', '<section class="cover watercolor"><p class="eyebrow">Một ngày thật đẹp</p><div class="bloom">✿</div><h1 class="names" data-bind="title"></h1><p class="subtitle" data-bind="themeConfig.subtitle"></p><p class="date" data-bind="eventStartAt" data-format="date"></p><p class="handwritten">Hẹn gặp bạn<br />giữa những yêu thương</p></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000105', 1, 'invitation', 'Lời mời', 'CONTENT', '<section class="interior soft"><h2>Gửi bạn<br />một lời hẹn</h2><p class="lead" data-bind="summary"></p><span class="bloom">✿</span><div class="people" data-repeat="members"><p class="name" data-bind="item.fullName"></p><p data-bind="item.description"></p></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000105', 2, 'story', 'Chuyện chúng mình', 'GALLERY', '<section class="interior soft"><p class="eyebrow">Little moments, big love</p><h2>Những mảnh<br />dịu dàng</h2><div class="album"><figure data-repeat="images"><img class="photo" data-src="item.asset.deliveryUrl" data-alt="item.altText" /><figcaption data-bind="item.caption"></figcaption></figure></div><article class="story" data-repeat="sections"><h3 data-bind="item.title"></h3><p data-bind="item.contentText"></p></article></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000105', 3, 'schedule', 'Ngày chung vui', 'SCHEDULE', '<section class="interior"><p class="eyebrow">Save the date</p><h2>Ngày chung vui</h2><article class="event" data-repeat="events"><p class="date" data-bind="item.startAt" data-format="date"></p><h3 data-bind="item.title"></h3><p data-bind="item.description"></p></article></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000105', 4, 'venue', 'Địa điểm & hồi đáp', 'LOCATION', '<section class="interior"><p class="eyebrow">Hẹn gặp bạn</p><h2>Nơi hạnh phúc<br />bắt đầu</h2><article class="venue" data-repeat="locations"><h3 data-bind="item.name"></h3><p data-bind="item.address"></p><a data-href="item.mapUrl">Xem đường đi</a></article><div data-slot="RSVP"></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000105', 5, 'closing', 'Lời chúc', 'CLOSING', '<section class="interior closing"><p class="eyebrow">With love</p><h2>Cảm ơn bạn<br />đã bên chúng mình</h2><p>Sự hiện diện của bạn là món quà ý nghĩa trong ngày trọng đại.</p><h3 data-bind="title"></h3><div data-slot="GUEST_MESSAGES"></div></section>');

INSERT INTO templates (id, code, name, memory_type, description, status, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000006', 'WEDDING_VIETNAMESE_HERITAGE', 'Vietnamese Heritage', 'WEDDING', 'Đỏ son, giấy kem và hoa văn hình học gợi thiệp cưới truyền thống Việt.', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO template_versions (id, template_id, version_no, component_key, renderer_version, cover_required,
    config_schema, default_config, required_sections, section_contracts, book_config, css_content,
    status, published_at, created_at, updated_at)
VALUES ('60000000-0000-0000-0000-000000000106', '60000000-0000-0000-0000-000000000006', 1, 'html-book', '1', FALSE,
    '{"type":"object","properties":{"subtitle":{"type":"string","maxLength":200}},"additionalProperties":false}'::jsonb,
    '{"subtitle":"Trân trọng kính mời"}'::jsonb, '[]'::jsonb,
    '{"STORY":{"configSchema":{"type":"object","additionalProperties":false}},"GALLERY":{"configSchema":{"type":"object","additionalProperties":false}}}'::jsonb,
    '{"background":"#732b30","paper":"#fff3dc","direction":"ltr","effect":"flip","desktopSpread":true,"aspectRatio":0.72}'::jsonb,
    '.cover { text-align: center; color: #6b252a; font-family: Georgia; }
.interior { color: #6b252a; font-family: Georgia; }
.eyebrow { text-transform: uppercase; letter-spacing: 3px; font-size: 11px; }
.subtitle { font-size: 16px; }
.date { font-size: 13px; letter-spacing: 1px; }
.lead { font-size: 20px; line-height: 1.8; margin: 28px 0; }
.name { font-size: 26px; font-style: italic; }
.people { margin: 26px 0; }
.story { margin: 24px 0; }
.event { margin: 26px 0; }
.venue { margin: 28px 0; }
.photo { margin: 16px 0; }
.closing { text-align: center; padding: 28px 10px; }
.heritage { border: 5px double #ad783e; background: #842f35; color: #ffe8b7; padding: 30px 18px; } .double-happiness { font-size: 86px; line-height: 1.4; color: #f4ce83; } .names { font-size: 42px; font-weight: 400; margin: 30px 0; } .lattice { text-align: center; letter-spacing: 8px; color: #ad783e; margin: 24px 0; } .traditional { border-top: 6px double #ad783e; border-bottom: 6px double #ad783e; padding: 26px 12px; text-align: center; } .blessing { font-style: italic; margin-top: 24px; } .photo { border: 3px solid #ad783e; padding: 5px; } .event { border-left: 3px solid #842f35; padding-left: 20px; }', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000106', 0, 'cover', 'Bìa', 'COVER', '<section class="cover heritage"><div class="double-happiness">囍</div><p class="eyebrow">THIỆP MỜI</p><h2>Lễ Thành Hôn</h2><div class="lattice">◇ — ◇ — ◇</div><h1 class="names" data-bind="title"></h1><p data-bind="themeConfig.subtitle"></p><p class="date" data-bind="eventStartAt" data-format="date"></p><div class="lattice">◇ — ◇ — ◇</div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000106', 1, 'invitation', 'Lời mời', 'CONTENT', '<section class="interior traditional"><p class="eyebrow">Chung vui cùng gia đình</p><h2>Trân trọng<br />báo hỷ</h2><div class="lattice">◇ — ◇ — ◇</div><div class="people" data-repeat="members"><p class="name" data-bind="item.fullName"></p><p data-bind="item.description"></p></div><p class="lead" data-bind="summary"></p></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000106', 2, 'story', 'Chuyện chúng mình', 'GALLERY', '<section class="interior traditional"><p class="eyebrow">Duyên lành</p><h2>Ngày chung đôi</h2><article class="story" data-repeat="sections"><h3 data-bind="item.title"></h3><p data-bind="item.contentText"></p></article><div class="album"><figure data-repeat="images"><img class="photo" data-src="item.asset.deliveryUrl" data-alt="item.altText" /><figcaption data-bind="item.caption"></figcaption></figure></div><p class="blessing">Trăm năm tình viên mãn<br />Bạc đầu nghĩa phu thê</p></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000106', 3, 'schedule', 'Ngày chung vui', 'SCHEDULE', '<section class="interior"><p class="eyebrow">Save the date</p><h2>Ngày chung vui</h2><article class="event" data-repeat="events"><p class="date" data-bind="item.startAt" data-format="date"></p><h3 data-bind="item.title"></h3><p data-bind="item.description"></p></article></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000106', 4, 'venue', 'Địa điểm & hồi đáp', 'LOCATION', '<section class="interior"><p class="eyebrow">Hẹn gặp bạn</p><h2>Nơi hạnh phúc<br />bắt đầu</h2><article class="venue" data-repeat="locations"><h3 data-bind="item.name"></h3><p data-bind="item.address"></p><a data-href="item.mapUrl">Xem đường đi</a></article><div data-slot="RSVP"></div></section>');
INSERT INTO template_pages (template_version_id, page_order, page_key, name, page_type, html_content)
VALUES ('60000000-0000-0000-0000-000000000106', 5, 'closing', 'Lời chúc', 'CLOSING', '<section class="interior closing"><p class="eyebrow">With love</p><h2>Cảm ơn bạn<br />đã bên chúng mình</h2><p>Sự hiện diện của bạn là món quà ý nghĩa trong ngày trọng đại.</p><h3 data-bind="title"></h3><div data-slot="GUEST_MESSAGES"></div></section>');

--rollback DELETE FROM template_versions WHERE id IN ('60000000-0000-0000-0000-000000000101','60000000-0000-0000-0000-000000000102','60000000-0000-0000-0000-000000000103','60000000-0000-0000-0000-000000000104','60000000-0000-0000-0000-000000000105','60000000-0000-0000-0000-000000000106');
--rollback DELETE FROM templates WHERE code IN ('WEDDING_MINIMAL_IVORY','WEDDING_BOTANICAL_ROMANCE','WEDDING_MODERN_EDITORIAL','WEDDING_CLASSIC_GOLD','WEDDING_WATERCOLOR_GARDEN','WEDDING_VIETNAMESE_HERITAGE');


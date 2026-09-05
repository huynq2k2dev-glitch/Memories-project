package com.memories.platform.template.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplatePage {
    @Column(name = "page_key", nullable = false, length = 60)
    private String key;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "page_type", nullable = false, length = 30)
    private String type;

    @Column(name = "html_content", nullable = false, columnDefinition = "text")
    private String html;

    public TemplatePage(String key, String name, String type, String html) {
        this.key = key;
        this.name = name;
        this.type = type;
        this.html = html;
    }
}

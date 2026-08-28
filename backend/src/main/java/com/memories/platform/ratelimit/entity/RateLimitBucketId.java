package com.memories.platform.ratelimit.entity;

import com.memories.platform.ratelimit.constants.RateLimitScope;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Getter
@NoArgsConstructor
public class RateLimitBucketId implements Serializable {

    private RateLimitScope scope;
    private String subjectKey;

    public RateLimitBucketId(RateLimitScope scope, String subjectKey) {
        this.scope = scope;
        this.subjectKey = subjectKey;
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) {
            return true;
        }
        if (!(candidate instanceof RateLimitBucketId other)) {
            return false;
        }
        return scope == other.scope && Objects.equals(subjectKey, other.subjectKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, subjectKey);
    }
}

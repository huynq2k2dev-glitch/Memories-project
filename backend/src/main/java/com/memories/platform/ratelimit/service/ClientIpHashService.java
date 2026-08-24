package com.memories.platform.ratelimit.service;

import com.memories.platform.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class ClientIpHashService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKeySpec secretKey;
    private final boolean trustForwardedFor;

    public ClientIpHashService(RateLimitProperties properties) {
        byte[] secret = Base64.getDecoder().decode(properties.ipHashSecret());
        if (secret.length < 32) {
            throw new IllegalArgumentException("IP_HASH_SECRET must contain at least 256 bits");
        }
        this.secretKey = new SecretKeySpec(secret, HMAC_ALGORITHM);
        this.trustForwardedFor = properties.trustForwardedFor();
    }

    public String hash(HttpServletRequest request) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            return HexFormat.of().formatHex(
                    mac.doFinal(clientAddress(request).getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC-SHA256 is not available", exception);
        }
    }

    private String clientAddress(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null) {
                String firstAddress = forwardedFor.split(",", 2)[0].trim();
                if (!firstAddress.isBlank() && firstAddress.length() <= 64) {
                    return firstAddress;
                }
            }
        }
        return request.getRemoteAddr();
    }
}

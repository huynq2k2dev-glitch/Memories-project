package com.memories.platform.memory.service;

import com.memories.platform.memory.constants.MemoryScheduleConstants;
import com.memories.platform.memory.exception.InvalidMemoryCoordinatesException;
import com.memories.platform.memory.exception.InvalidMemoryEventTimeException;
import com.memories.platform.memory.exception.InvalidMemoryTimezoneException;
import com.memories.platform.memory.exception.UnsafeMemoryMapUrlException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class MemoryScheduleValidationService {

    private static final BigDecimal MINIMUM_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAXIMUM_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MINIMUM_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAXIMUM_LONGITUDE = BigDecimal.valueOf(180);

    public void requireValidCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new InvalidMemoryCoordinatesException();
        }
        if (latitude != null
                && (latitude.compareTo(MINIMUM_LATITUDE) < 0
                || latitude.compareTo(MAXIMUM_LATITUDE) > 0
                || longitude.compareTo(MINIMUM_LONGITUDE) < 0
                || longitude.compareTo(MAXIMUM_LONGITUDE) > 0)) {
            throw new InvalidMemoryCoordinatesException();
        }
    }

    public void requireSafeMapUrl(String mapUrl) {
        if (mapUrl == null) {
            return;
        }
        try {
            URI uri = new URI(mapUrl);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !isAllowedMapLocation(host.toLowerCase(Locale.ROOT), uri.getPath())) {
                throw new UnsafeMemoryMapUrlException();
            }
        } catch (URISyntaxException exception) {
            throw new UnsafeMemoryMapUrlException();
        }
    }

    public void requireValidTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new InvalidMemoryTimezoneException();
        }
    }

    public void requireValidTimeRange(Instant startAt, Instant endAt) {
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new InvalidMemoryEventTimeException();
        }
    }

    private boolean isAllowedMapLocation(String host, String path) {
        if (MemoryScheduleConstants.DIRECT_MAP_HOSTS.contains(host)) {
            return true;
        }
        if (("google.com".equals(host) || "www.google.com".equals(host))
                && isMapsPath(path)) {
            return true;
        }
        return "goo.gl".equals(host) && isMapsPath(path);
    }

    private boolean isMapsPath(String path) {
        return path != null && ("/maps".equals(path) || path.startsWith("/maps/"));
    }
}

package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.common.time.LocalDateTimes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/** SHA-256 hex of the booking request fields (design 04 §5). */
public final class RequestHasher {

    private RequestHasher() {
    }

    public static String hash(UUID customerId, UUID vehicleId, UUID dealershipId, UUID serviceTypeId,
                              LocalDateTime startTime) {
        String canonical = String.join("|",
                customerId.toString(),
                vehicleId.toString(),
                dealershipId.toString(),
                serviceTypeId.toString(),
                LocalDateTimes.format(startTime));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

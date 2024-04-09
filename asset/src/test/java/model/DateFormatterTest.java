package model;

import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static model.DateFormatter.*;
import static org.junit.jupiter.api.Assertions.*;

class DateFormatterTest {

    @Test
    void testExpired() {
        Instant now = Instant.now();
        boolean expired = isExpired(now, "2024-01-01 01:01:01");
        assertTrue(expired);
        expired = isExpired(now, "2026-01-01 01:01:01");
        assertFalse(expired);
    }

    @Test
    void testCheckFormat() {
        assertDoesNotThrow(() -> checkDateFormat("2026-01-01 01:01:00"));
        assertThrows(ChaincodeException.class, () -> checkDateFormat("2026-01-01T01:01:000z"));
    }

    @Test
    void testToInstant() {
        Instant now = Instant.now();
        String formattedNowStr = DATE_TIME_FORMATTER.format(now);
        Instant formattedNow = LocalDateTime.parse(formattedNowStr, DATE_TIME_FORMATTER).toInstant(ZoneOffset.UTC);
        assertEquals(formattedNow, DateFormatter.toInstant(formattedNowStr));
    }

}
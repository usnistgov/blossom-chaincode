package model;

import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

public class DateFormatter {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.from(ZoneOffset.UTC));

    public static void checkDateFormat(String date) {
        try {
            LocalDateTime.parse(date, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ChaincodeException(e.getMessage());
        }
    }

    public static boolean isExpired(Instant txTs, String expiration) {
        Instant exp = toInstant(expiration);
        return txTs.isAfter(exp);
    }

    public static Instant toInstant(String str) {
        LocalDateTime date = LocalDateTime.parse(str, DATE_TIME_FORMATTER);
        return date.toInstant(ZoneOffset.UTC);
    }

}

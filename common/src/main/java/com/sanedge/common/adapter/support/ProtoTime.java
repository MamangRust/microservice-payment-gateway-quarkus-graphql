package com.sanedge.common.adapter.support;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Shared proto-timestamp -> {@link Instant} conversion for adapters.
 *
 * <p>
 * Mirrors the Go {@code parseTime} helper: an empty or unparseable value yields
 * {@code null} instead of an error.
 */
public final class ProtoTime {

    private ProtoTime() {
    }

    public static Instant parse(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (DateTimeParseException e) {
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}

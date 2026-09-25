package com.cafemanager.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Formats de date utilisés dans l'interface, sur les tickets et dans SQLite. */
public final class DateUtil {

    public static final DateTimeFormatter DB = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter TIME_SEC = DateTimeFormatter.ofPattern("HH:mm:ss");

    private DateUtil() {
    }

    public static String toDb(LocalDateTime dt) {
        return dt == null ? null : dt.format(DB);
    }

    public static LocalDateTime fromDb(String s) {
        return (s == null || s.isBlank()) ? null : LocalDateTime.parse(s, DB);
    }

    public static String date(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DATE);
    }

    public static String time(LocalDateTime dt) {
        return dt == null ? "" : dt.format(TIME);
    }
}

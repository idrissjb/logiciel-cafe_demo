package com.cafemanager.util;

import java.util.Locale;
import java.util.Set;

/** Estimation simple de la solidité d'un mot de passe (0 = vide, 1 = faible … 4 = fort). */
public final class PasswordStrength {

    private static final Set<String> COMMON = Set.of(
            "password", "motdepasse", "azerty", "azerty123", "qwerty", "qwerty123", "admin", "admin123",
            "abc123", "iloveyou", "cafe", "cafe123", "000000", "111111", "123123");

    private PasswordStrength() {
    }

    public static int score(String pw) {
        if (pw == null || pw.isEmpty()) {
            return 0;
        }
        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean symbol = false;
        for (char c : pw.toCharArray()) {
            if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                symbol = true;
            }
        }
        int classes = (lower ? 1 : 0) + (upper ? 1 : 0) + (digit ? 1 : 0) + (symbol ? 1 : 0);

        int s = 0;
        if (pw.length() >= 6) {
            s++;
        }
        if (pw.length() >= 10) {
            s++;
        }
        if (classes >= 2) {
            s++;
        }
        if (classes >= 3 && pw.length() >= 8) {
            s++;
        }
        if (pw.length() < 6 || isPredictable(pw)) {
            s = Math.min(s, 1);
        }
        return Math.max(s, 1);
    }

    public static String label(int score) {
        return switch (score) {
            case 1 -> "Faible";
            case 2 -> "Moyen";
            case 3 -> "Bon";
            case 4 -> "Fort";
            default -> "";
        };
    }

    /** Mots de passe courants, caractère répété (aaaaaa) ou suite (123456, abcdef). */
    private static boolean isPredictable(String pw) {
        if (COMMON.contains(pw.toLowerCase(Locale.ROOT))) {
            return true;
        }
        boolean same = true;
        boolean up = true;
        boolean down = true;
        for (int i = 1; i < pw.length(); i++) {
            int d = pw.charAt(i) - pw.charAt(i - 1);
            same &= d == 0;
            up &= d == 1;
            down &= d == -1;
        }
        return pw.length() > 1 && (same || up || down);
    }
}

package com.cafemanager.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Les montants sont toujours manipulés en centimes (long) pour éviter toute erreur d'arrondi.
 * Cette classe gère l'affichage et la lecture des montants saisis.
 */
public final class Money {

    private static volatile String currency = "DH";

    private Money() {
    }

    public static void setCurrency(String c) {
        currency = (c == null || c.isBlank()) ? "DH" : c.trim();
    }

    public static String currency() {
        return currency;
    }

    /** 1250 -> "12.50" */
    public static String plain(long cents) {
        long abs = Math.abs(cents);
        return (cents < 0 ? "-" : "") + (abs / 100) + "." + String.format("%02d", abs % 100);
    }

    /** 1250 -> "12.50 DH" */
    public static String fmt(long cents) {
        return plain(cents) + " " + currency;
    }

    /** Affichage compact : 800 -> "8 DH", 750 -> "7.50 DH", 245000 -> "2 450 DH" (séparateur de milliers). */
    public static String compact(long cents) {
        long abs = Math.abs(cents);
        String whole = group(abs / 100);
        String txt = (abs % 100 == 0) ? whole : whole + "." + String.format("%02d", abs % 100);
        return (cents < 0 ? "-" : "") + txt + " " + currency;
    }

    /** Nombre entier avec espaces insécables tous les 3 chiffres. */
    public static String group(long n) {
        String s = Long.toString(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) {
                sb.append(' ');
            }
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    /** "12,5" ou "12.50" -> 1250. Lève NumberFormatException si invalide. */
    public static long parse(String text) {
        if (text == null) {
            throw new NumberFormatException("vide");
        }
        String t = text.trim().replace(',', '.').replace(" ", "").replace(" ", "");
        t = t.replaceAll("(?i)" + java.util.regex.Pattern.quote(currency) + "$", "").trim();
        if (t.isEmpty()) {
            throw new NumberFormatException("vide");
        }
        return new BigDecimal(t).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** Pourcentage d'un montant en centimes, arrondi au centime le plus proche. */
    public static long percentOf(long cents, long percent) {
        return BigDecimal.valueOf(cents).multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValue();
    }

    /** Part de TVA contenue dans un montant TTC. */
    public static long vatIncluded(long ttcCents, double ratePercent) {
        if (ratePercent <= 0) {
            return 0;
        }
        BigDecimal ttc = BigDecimal.valueOf(ttcCents);
        BigDecimal ht = ttc.divide(BigDecimal.valueOf(1 + ratePercent / 100.0), 0, RoundingMode.HALF_UP);
        return ttc.subtract(ht).longValue();
    }
}

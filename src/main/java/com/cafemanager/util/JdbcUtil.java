package com.cafemanager.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/** Petites aides JDBC (valeurs nulles, booléens). */
public final class JdbcUtil {

    private JdbcUtil() {
    }

    public static void setLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setLong(index, value);
        }
    }

    public static Long getLong(ResultSet rs, String column) throws SQLException {
        long v = rs.getLong(column);
        return rs.wasNull() ? null : v;
    }

    public static boolean getBool(ResultSet rs, String column) throws SQLException {
        return rs.getInt(column) != 0;
    }

    /** Identifiant de la dernière ligne insérée sur cette connexion (portable entre versions du pilote). */
    public static long lastInsertId(java.sql.Connection c) throws SQLException {
        try (java.sql.Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    public static String like(String text) {
        return "%" + text.trim().replace("%", "").replace("_", "") + "%";
    }
}

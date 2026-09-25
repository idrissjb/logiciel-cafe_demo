package com.cafemanager.dao;

import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.DatabaseConnection;
import com.cafemanager.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Requêtes d'agrégation pour le dashboard. Seuls les tickets PAYÉS comptent. */
public class StatsDAO {

    public record NameValue(String name, long value) {
    }

    /** {chiffre d'affaires en centimes, nombre de tickets} sur [from, to[. */
    public long[] revenueAndCount(LocalDateTime from, LocalDateTime to) {
        return one("SELECT COALESCE(SUM(total),0), COUNT(*) FROM orders WHERE status='PAID' AND paid_at >= ? AND paid_at < ?",
                from, to);
    }

    /** heure (0-23) -> {CA, nombre de tickets}. */
    public Map<Integer, long[]> byHour(LocalDateTime from, LocalDateTime to) {
        Map<Integer, long[]> map = new LinkedHashMap<>();
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT CAST(strftime('%H', paid_at) AS INTEGER) AS h, SUM(total), COUNT(*) FROM orders "
                             + "WHERE status='PAID' AND paid_at >= ? AND paid_at < ? GROUP BY h ORDER BY h")) {
            ps.setString(1, DateUtil.toDb(from));
            ps.setString(2, DateUtil.toDb(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt(1), new long[]{rs.getLong(2), rs.getLong(3)});
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Statistiques indisponibles", e);
        }
        return map;
    }

    /** Chiffre d'affaires (lignes) par catégorie. */
    public List<NameValue> revenueByCategory(LocalDateTime from, LocalDateTime to) {
        return names("SELECT COALESCE(c.name, 'Autres') AS n, SUM(oi.line_total) AS v FROM order_items oi "
                + "JOIN orders o ON o.id = oi.order_id "
                + "LEFT JOIN products p ON p.id = oi.product_id LEFT JOIN categories c ON c.id = p.category_id "
                + "WHERE o.status='PAID' AND o.paid_at >= ? AND o.paid_at < ? GROUP BY n ORDER BY v DESC", from, to, 0);
    }

    /** Produits les plus vendus (quantités). */
    public List<NameValue> topProducts(LocalDateTime from, LocalDateTime to, int limit) {
        return names("SELECT oi.product_name AS n, SUM(oi.quantity) AS v FROM order_items oi "
                + "JOIN orders o ON o.id = oi.order_id WHERE o.status='PAID' AND o.paid_at >= ? AND o.paid_at < ? "
                + "GROUP BY n ORDER BY v DESC, n LIMIT ?", from, to, limit);
    }

    /** Quantités vendues dont la catégorie ressemble à {@code pattern} (ex. "Caf%"). */
    public long unitsSoldInCategoryLike(LocalDateTime from, LocalDateTime to, String pattern) {
        return one("SELECT COALESCE(SUM(oi.quantity),0), 0 FROM order_items oi JOIN orders o ON o.id = oi.order_id "
                + "LEFT JOIN products p ON p.id = oi.product_id LEFT JOIN categories c ON c.id = p.category_id "
                + "WHERE o.status='PAID' AND o.paid_at >= ? AND o.paid_at < ? AND c.name LIKE '" + pattern.replace("'", "") + "'",
                from, to)[0];
    }

    public long unitsSold(LocalDateTime from, LocalDateTime to) {
        return one("SELECT COALESCE(SUM(oi.quantity),0), 0 FROM order_items oi JOIN orders o ON o.id = oi.order_id "
                + "WHERE o.status='PAID' AND o.paid_at >= ? AND o.paid_at < ?", from, to)[0];
    }

    /** Jour -> chiffre d'affaires, pour chaque jour de [from, to] (les jours sans vente valent 0). */
    public Map<LocalDate, Long> revenuePerDay(LocalDate from, LocalDate to) {
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            result.put(d, 0L);
        }
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT substr(paid_at, 1, 10) AS d, SUM(total) FROM orders "
                             + "WHERE status='PAID' AND paid_at >= ? AND paid_at < ? GROUP BY d")) {
            ps.setString(1, DateUtil.toDb(from.atStartOfDay()));
            ps.setString(2, DateUtil.toDb(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(LocalDate.parse(rs.getString(1)), rs.getLong(2));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Statistiques indisponibles", e);
        }
        return result;
    }

    // ---------------------------------------------------------------- helpers

    private long[] one(String sql, LocalDateTime from, LocalDateTime to) {
        try (Connection c = DatabaseConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, DateUtil.toDb(from));
            ps.setString(2, DateUtil.toDb(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new long[]{rs.getLong(1), rs.getLong(2)} : new long[]{0, 0};
            }
        } catch (SQLException e) {
            throw new DataAccessException("Statistiques indisponibles", e);
        }
    }

    private List<NameValue> names(String sql, LocalDateTime from, LocalDateTime to, int limit) {
        try (Connection c = DatabaseConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, DateUtil.toDb(from));
            ps.setString(2, DateUtil.toDb(to));
            if (limit > 0) {
                ps.setInt(3, limit);
            }
            List<NameValue> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new NameValue(rs.getString(1), rs.getLong(2)));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Statistiques indisponibles", e);
        }
    }
}

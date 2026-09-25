package com.cafemanager.dao;

import com.cafemanager.model.DiscountMode;
import com.cafemanager.model.Order;
import com.cafemanager.model.OrderItem;
import com.cafemanager.model.OrderStatus;
import com.cafemanager.model.PaymentMethod;
import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.DatabaseConnection;
import com.cafemanager.util.DateUtil;
import com.cafemanager.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès aux commandes (orders) et à leurs lignes (order_items). */
public class OrderDAO {

    private static final String SELECT =
            "SELECT o.id, o.ticket_number, o.table_id, t.name AS table_name, o.server_id, u.full_name AS server_name, "
                    + "o.created_at, o.paid_at, o.status, o.discount_mode, o.discount_value, o.payment_method, o.total AS stored_total "
                    + "FROM orders o LEFT JOIN tables_cafe t ON t.id = o.table_id "
                    + "LEFT JOIN users u ON u.id = o.server_id ";

    // ------------------------------------------------------------ numérotation

    /** Numéro que recevra le prochain ticket enregistré (affiché sur une commande vide). */
    public String peekNextTicketNumber() {
        try (Connection c = DatabaseConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT value FROM sequences WHERE name = 'ticket'")) {
            long v = rs.next() ? rs.getLong(1) : 0;
            return format(v + 1);
        } catch (SQLException e) {
            throw new DataAccessException("Lecture du compteur de tickets impossible", e);
        }
    }

    public static String format(long number) {
        return String.format("%06d", number);
    }

    /** Alloue le numéro suivant. Doit être appelé dans une transaction (verrou d'écriture SQLite). */
    private String allocateTicketNumber(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("UPDATE sequences SET value = value + 1 WHERE name = 'ticket'");
            try (ResultSet rs = st.executeQuery("SELECT value FROM sequences WHERE name = 'ticket'")) {
                rs.next();
                return format(rs.getLong(1));
            }
        }
    }

    // ------------------------------------------------------------- écriture

    /** Insère ou met à jour la commande et remplace ses lignes. Attribue id et numéro à la première sauvegarde. */
    public void save(Connection c, Order o) throws SQLException {
        long subtotal = o.subtotal();
        long discount = o.discount();
        long total = subtotal - discount;
        if (o.getId() == null) {
            o.setTicketNumber(allocateTicketNumber(c));
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO orders(ticket_number, table_id, server_id, created_at, status, subtotal, discount, "
                            + "discount_mode, discount_value, total) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, o.getTicketNumber());
                JdbcUtil.setLong(ps, 2, o.getTableId());
                JdbcUtil.setLong(ps, 3, o.getServerId());
                ps.setString(4, DateUtil.toDb(o.getCreatedAt()));
                ps.setString(5, o.getStatus().name());
                ps.setLong(6, subtotal);
                ps.setLong(7, discount);
                ps.setString(8, o.getDiscountMode().name());
                ps.setLong(9, o.getDiscountValue());
                ps.setLong(10, total);
                ps.executeUpdate();
                o.setId(JdbcUtil.lastInsertId(c));
            }
        } else {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE orders SET table_id=?, server_id=?, status=?, subtotal=?, discount=?, discount_mode=?, "
                            + "discount_value=?, total=? WHERE id=?")) {
                JdbcUtil.setLong(ps, 1, o.getTableId());
                JdbcUtil.setLong(ps, 2, o.getServerId());
                ps.setString(3, o.getStatus().name());
                ps.setLong(4, subtotal);
                ps.setLong(5, discount);
                ps.setString(6, o.getDiscountMode().name());
                ps.setLong(7, o.getDiscountValue());
                ps.setLong(8, total);
                ps.setLong(9, o.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM order_items WHERE order_id = ?")) {
                ps.setLong(1, o.getId());
                ps.executeUpdate();
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO order_items(order_id, product_id, product_name, unit_price, quantity, line_total) "
                        + "VALUES (?,?,?,?,?,?)")) {
            for (OrderItem it : o.getItems()) {
                ps.setLong(1, o.getId());
                JdbcUtil.setLong(ps, 2, it.getProductId());
                ps.setString(3, it.getProductName());
                ps.setLong(4, it.getUnitPriceCents());
                ps.setInt(5, it.getQuantity());
                ps.setLong(6, it.lineTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Marque la commande comme payée (dans la transaction de paiement). */
    public void markPaid(Connection c, Order o) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE orders SET status = 'PAID', paid_at = ?, payment_method = ? WHERE id = ?")) {
            ps.setString(1, DateUtil.toDb(o.getPaidAt()));
            ps.setString(2, o.getPaymentMethod().name());
            ps.setLong(3, o.getId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection c, long orderId, OrderStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE orders SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, orderId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection c, long orderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM orders WHERE id = ?")) {
            ps.setLong(1, orderId);
            ps.executeUpdate();
        }
    }

    /**
     * Supprime une commande vide (jamais payée) et rend son numéro au compteur s'il était le dernier attribué :
     * ainsi une saisie abandonnée ne crée pas de "trou" dans la numérotation.
     */
    public void deleteEmptyDraft(Connection c, long orderId, String ticketNumber) throws SQLException {
        delete(c, orderId);
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE sequences SET value = value - 1 WHERE name = 'ticket' AND value = ?")) {
            ps.setLong(1, Long.parseLong(ticketNumber));
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------- lecture

    public Optional<Order> findById(long id) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE o.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Order o = map(rs);
                loadItems(c, o);
                return Optional.of(o);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lecture de la commande impossible", e);
        }
    }

    public Optional<Order> findOpenByTable(long tableId) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     SELECT + "WHERE o.table_id = ? AND o.status IN ('OPEN','PENDING_PAYMENT') ORDER BY o.id DESC LIMIT 1")) {
            ps.setLong(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Order o = map(rs);
                loadItems(c, o);
                return Optional.of(o);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lecture de la commande impossible", e);
        }
    }

    /** Historique : tickets payés entre deux dates (incluses), avec recherche libre. Sans les lignes. */
    public List<Order> findHistory(LocalDate from, LocalDate to, String search) {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE o.status = 'PAID' AND o.paid_at >= ? AND o.paid_at < ? ");
        boolean hasSearch = search != null && !search.isBlank();
        if (hasSearch) {
            sql.append("AND (o.ticket_number LIKE ? OR u.full_name LIKE ? OR t.name LIKE ?) ");
        }
        sql.append("ORDER BY o.paid_at DESC, o.id DESC LIMIT 2000");
        try (Connection c = DatabaseConnection.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            ps.setString(1, DateUtil.toDb(from.atStartOfDay()));
            ps.setString(2, DateUtil.toDb(to.plusDays(1).atStartOfDay()));
            if (hasSearch) {
                String like = JdbcUtil.like(search);
                ps.setString(3, like);
                ps.setString(4, like);
                ps.setString(5, like);
            }
            List<Order> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture de l'historique impossible", e);
        }
    }

    private void loadItems(Connection c, Order o) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT product_id, product_name, unit_price, quantity FROM order_items WHERE order_id = ? ORDER BY id")) {
            ps.setLong(1, o.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    o.getItems().add(new OrderItem(JdbcUtil.getLong(rs, "product_id"), rs.getString("product_name"),
                            rs.getLong("unit_price"), rs.getInt("quantity")));
                }
            }
        }
    }

    private Order map(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getLong("id"));
        o.setTicketNumber(rs.getString("ticket_number"));
        o.setTableId(JdbcUtil.getLong(rs, "table_id"));
        o.setTableName(rs.getString("table_name"));
        o.setServerId(JdbcUtil.getLong(rs, "server_id"));
        o.setServerName(rs.getString("server_name"));
        o.setCreatedAt(DateUtil.fromDb(rs.getString("created_at")));
        o.setPaidAt(DateUtil.fromDb(rs.getString("paid_at")));
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setDiscount(DiscountMode.valueOf(rs.getString("discount_mode")), rs.getLong("discount_value"));
        String pm = rs.getString("payment_method");
        o.setPaymentMethod(pm == null ? null : PaymentMethod.valueOf(pm));
        o.setStoredTotal(rs.getLong("stored_total"));
        return o;
    }
}

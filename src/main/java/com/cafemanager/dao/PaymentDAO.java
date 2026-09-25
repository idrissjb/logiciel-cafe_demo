package com.cafemanager.dao;

import com.cafemanager.model.Payment;
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
import java.util.Optional;

public class PaymentDAO {

    public void insert(Connection c, Payment p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO payments(order_id, method, amount_due, amount_received, change_given, paid_at, cashier_id) "
                        + "VALUES (?,?,?,?,?,?,?)")) {
            ps.setLong(1, p.getOrderId());
            ps.setString(2, p.getMethod().name());
            ps.setLong(3, p.getAmountDueCents());
            ps.setLong(4, p.getAmountReceivedCents());
            ps.setLong(5, p.getChangeCents());
            ps.setString(6, DateUtil.toDb(p.getPaidAt()));
            JdbcUtil.setLong(ps, 7, p.getCashierId());
            ps.executeUpdate();
            p.setId(JdbcUtil.lastInsertId(c));
        }
    }

    public Optional<Payment> findByOrder(long orderId) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, order_id, method, amount_due, amount_received, change_given, paid_at, cashier_id "
                             + "FROM payments WHERE order_id = ? ORDER BY id DESC LIMIT 1")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Payment p = new Payment();
                p.setId(rs.getLong("id"));
                p.setOrderId(rs.getLong("order_id"));
                p.setMethod(PaymentMethod.valueOf(rs.getString("method")));
                p.setAmountDueCents(rs.getLong("amount_due"));
                p.setAmountReceivedCents(rs.getLong("amount_received"));
                p.setChangeCents(rs.getLong("change_given"));
                p.setPaidAt(DateUtil.fromDb(rs.getString("paid_at")));
                p.setCashierId(JdbcUtil.getLong(rs, "cashier_id"));
                return Optional.of(p);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lecture du paiement impossible", e);
        }
    }
}

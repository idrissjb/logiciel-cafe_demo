package com.cafemanager.dao;

import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.DatabaseConnection;
import com.cafemanager.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** Table "tickets" : un ticket émis par commande payée, avec le suivi des impressions. */
public class TicketDAO {

    public void insert(Connection c, String ticketNumber, long orderId, LocalDateTime issuedAt) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO tickets(ticket_number, order_id, issued_at) VALUES (?,?,?)")) {
            ps.setString(1, ticketNumber);
            ps.setLong(2, orderId);
            ps.setString(3, DateUtil.toDb(issuedAt));
            ps.executeUpdate();
        }
    }

    /** Enregistre une impression et retourne le nombre total d'impressions du ticket. */
    public int registerPrint(long orderId) {
        try (Connection c = DatabaseConnection.get()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE tickets SET print_count = print_count + 1, last_printed_at = ? WHERE order_id = ?")) {
                ps.setString(1, DateUtil.toDb(LocalDateTime.now()));
                ps.setLong(2, orderId);
                ps.executeUpdate();
            }
            return printCount(c, orderId);
        } catch (SQLException e) {
            throw new DataAccessException("Enregistrement de l'impression impossible", e);
        }
    }

    public int printCount(long orderId) {
        try (Connection c = DatabaseConnection.get()) {
            return printCount(c, orderId);
        } catch (SQLException e) {
            throw new DataAccessException("Lecture impossible", e);
        }
    }

    private int printCount(Connection c, long orderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT print_count FROM tickets WHERE order_id = ?")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}

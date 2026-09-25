package com.cafemanager.dao;

import com.cafemanager.model.CafeTable;
import com.cafemanager.model.TableStatus;
import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.DatabaseConnection;
import com.cafemanager.util.DateUtil;
import com.cafemanager.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TableDAO {

    /** Table + résumé de sa commande en cours (s'il y en a une). */
    private static final String SELECT =
            "SELECT t.id, t.name, t.status, t.display_order, "
                    + "o.id AS oid, o.total AS ototal, o.created_at AS ocreated, "
                    + "(SELECT COALESCE(SUM(quantity), 0) FROM order_items WHERE order_id = o.id) AS oitems "
                    + "FROM tables_cafe t LEFT JOIN orders o ON o.table_id = t.id AND o.status IN ('OPEN','PENDING_PAYMENT') ";

    public List<CafeTable> findAll() {
        try (Connection c = DatabaseConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(SELECT + "ORDER BY t.display_order, t.id")) {
            List<CafeTable> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des tables impossible", e);
        }
    }

    public Optional<CafeTable> findById(long id) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE t.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lecture de la table impossible", e);
        }
    }

    public CafeTable insert(String name) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO tables_cafe(name, status, display_order) "
                             + "VALUES (?, 'LIBRE', (SELECT COALESCE(MAX(display_order),0)+1 FROM tables_cafe))")) {
            ps.setString(1, name);
            ps.executeUpdate();
            CafeTable t = new CafeTable();
            t.setName(name);
            t.setId(JdbcUtil.lastInsertId(c));
            return t;
        } catch (SQLException e) {
            throw new DataAccessException("Création de la table impossible : " + e.getMessage(), e);
        }
    }

    public void rename(long id, String name) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement("UPDATE tables_cafe SET name = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Renommage impossible : " + e.getMessage(), e);
        }
    }

    public void delete(long id) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tables_cafe WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Suppression de la table impossible : " + e.getMessage(), e);
        }
    }

    /** Version transactionnelle (utilisée avec les commandes/paiements). */
    public void setStatus(Connection c, long id, TableStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE tables_cafe SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void setStatus(long id, TableStatus status) {
        try (Connection c = DatabaseConnection.get()) {
            setStatus(c, id, status);
        } catch (SQLException e) {
            throw new DataAccessException("Modification de l'état de la table impossible", e);
        }
    }

    private CafeTable map(ResultSet rs) throws SQLException {
        CafeTable t = new CafeTable();
        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setStatus(TableStatus.valueOf(rs.getString("status")));
        t.setDisplayOrder(rs.getInt("display_order"));
        t.setOpenOrderId(JdbcUtil.getLong(rs, "oid"));
        if (t.getOpenOrderId() != null) {
            t.setOpenOrderTotal(rs.getLong("ototal"));
            t.setOpenOrderItems(rs.getInt("oitems"));
            t.setOpenSince(DateUtil.fromDb(rs.getString("ocreated")));
        }
        return t;
    }
}

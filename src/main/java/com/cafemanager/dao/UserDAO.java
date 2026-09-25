package com.cafemanager.dao;

import com.cafemanager.model.Role;
import com.cafemanager.model.User;
import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.DatabaseConnection;
import com.cafemanager.util.DateUtil;
import com.cafemanager.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private static final String SELECT =
            "SELECT id, username, password_hash, salt, full_name, role, active FROM users ";

    public Optional<User> findByUsername(String username) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lecture utilisateur impossible", e);
        }
    }

    public List<User> findAll() {
        try (Connection c = DatabaseConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(SELECT + "ORDER BY role, full_name COLLATE NOCASE")) {
            List<User> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des utilisateurs impossible", e);
        }
    }

    public User insert(User u) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO users(username, password_hash, salt, full_name, role, active, created_at) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getSalt());
            ps.setString(4, u.getFullName());
            ps.setString(5, u.getRole().name());
            ps.setInt(6, u.isActive() ? 1 : 0);
            ps.setString(7, DateUtil.toDb(LocalDateTime.now()));
            ps.executeUpdate();
            u.setId(JdbcUtil.lastInsertId(c));
            return u;
        } catch (SQLException e) {
            throw new DataAccessException("Création de l'utilisateur impossible : " + e.getMessage(), e);
        }
    }

    /** Met à jour l'utilisateur ; le mot de passe n'est modifié que si un nouveau hash est fourni. */
    public void update(User u, boolean updatePassword) {
        String sql = updatePassword
                ? "UPDATE users SET username=?, full_name=?, role=?, active=?, password_hash=?, salt=? WHERE id=?"
                : "UPDATE users SET username=?, full_name=?, role=?, active=? WHERE id=?";
        try (Connection c = DatabaseConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getFullName());
            ps.setString(3, u.getRole().name());
            ps.setInt(4, u.isActive() ? 1 : 0);
            if (updatePassword) {
                ps.setString(5, u.getPasswordHash());
                ps.setString(6, u.getSalt());
                ps.setLong(7, u.getId());
            } else {
                ps.setLong(5, u.getId());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Modification de l'utilisateur impossible : " + e.getMessage(), e);
        }
    }

    public void delete(long id) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Suppression de l'utilisateur impossible : " + e.getMessage(), e);
        }
    }

    /** Nombre d'administrateurs actifs (on n'en supprime jamais le dernier). */
    public int countActiveAdmins() {
        try (Connection c = DatabaseConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'ADMINISTRATEUR' AND active = 1")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture impossible", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(JdbcUtil.getBool(rs, "active"));
        return u;
    }
}

package com.cafemanager.dao;

import com.cafemanager.model.Category;
import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.DatabaseConnection;
import com.cafemanager.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDAO {

    private static final String SELECT = "SELECT id, name, icon, display_order, active FROM categories ";

    public List<Category> findAll() {
        return query(SELECT + "ORDER BY display_order, name");
    }

    public List<Category> findActive() {
        return query(SELECT + "WHERE active = 1 ORDER BY display_order, name");
    }

    public Optional<Category> findById(long id) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lecture catégorie impossible", e);
        }
    }

    public Category insert(Category cat) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO categories(name, icon, display_order, active) VALUES (?,?,?,?)")) {
            ps.setString(1, cat.getName());
            ps.setString(2, cat.getIcon());
            ps.setInt(3, cat.getDisplayOrder());
            ps.setInt(4, cat.isActive() ? 1 : 0);
            ps.executeUpdate();
            cat.setId(JdbcUtil.lastInsertId(c));
            return cat;
        } catch (SQLException e) {
            throw new DataAccessException("Création de la catégorie impossible : " + e.getMessage(), e);
        }
    }

    public void update(Category cat) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE categories SET name=?, icon=?, display_order=?, active=? WHERE id=?")) {
            ps.setString(1, cat.getName());
            ps.setString(2, cat.getIcon());
            ps.setInt(3, cat.getDisplayOrder());
            ps.setInt(4, cat.isActive() ? 1 : 0);
            ps.setLong(5, cat.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Modification de la catégorie impossible : " + e.getMessage(), e);
        }
    }

    public void delete(long id) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Suppression de la catégorie impossible : " + e.getMessage(), e);
        }
    }

    public int countProducts(long categoryId) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM products WHERE category_id = ?")) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lecture impossible", e);
        }
    }

    public int nextDisplayOrder() {
        try (Connection c = DatabaseConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(display_order), 0) + 1 FROM categories")) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture impossible", e);
        }
    }

    private List<Category> query(String sql) {
        try (Connection c = DatabaseConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Category> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des catégories impossible", e);
        }
    }

    private Category map(ResultSet rs) throws SQLException {
        return new Category(rs.getLong("id"), rs.getString("name"), rs.getString("icon"),
                rs.getInt("display_order"), JdbcUtil.getBool(rs, "active"));
    }
}

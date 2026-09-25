package com.cafemanager.dao;

import com.cafemanager.model.Product;
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

import com.cafemanager.util.DateUtil;
import java.time.LocalDateTime;

public class ProductDAO {

    private static final String SELECT =
            "SELECT p.id, p.name, p.category_id, c.name AS category_name, p.price, p.active, p.favorite, p.icon, p.image_path "
                    + "FROM products p LEFT JOIN categories c ON c.id = p.category_id ";

    /** Tous les produits (écran d'administration). */
    public List<Product> findAll() {
        return query(SELECT + "ORDER BY COALESCE(c.display_order, 999), p.name COLLATE NOCASE");
    }

    /** Produits vendables en caisse : produit actif dans une catégorie active. */
    public List<Product> findAvailable() {
        return query(SELECT + "WHERE p.active = 1 AND (c.active = 1 OR c.id IS NULL) "
                + "ORDER BY COALESCE(c.display_order, 999), p.name COLLATE NOCASE");
    }

    public Product insert(Product p) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO products(name, category_id, price, active, favorite, icon, image_path, created_at) "
                             + "VALUES (?,?,?,?,?,?,?,?)")) {
            bind(ps, p);
            ps.setString(8, DateUtil.toDb(LocalDateTime.now()));
            ps.executeUpdate();
            p.setId(JdbcUtil.lastInsertId(c));
            return p;
        } catch (SQLException e) {
            throw new DataAccessException("Création du produit impossible : " + e.getMessage(), e);
        }
    }

    public void update(Product p) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE products SET name=?, category_id=?, price=?, active=?, favorite=?, icon=?, image_path=? WHERE id=?")) {
            bind(ps, p);
            ps.setLong(8, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Modification du produit impossible : " + e.getMessage(), e);
        }
    }

    public void delete(long id) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Suppression du produit impossible : " + e.getMessage(), e);
        }
    }

    public void setActive(long id, boolean active) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement("UPDATE products SET active = ? WHERE id = ?")) {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Modification impossible", e);
        }
    }

    private void bind(PreparedStatement ps, Product p) throws SQLException {
        ps.setString(1, p.getName());
        ps.setLong(2, p.getCategoryId());
        ps.setLong(3, p.getPriceCents());
        ps.setInt(4, p.isActive() ? 1 : 0);
        ps.setInt(5, p.isFavorite() ? 1 : 0);
        ps.setString(6, p.getIcon());
        ps.setString(7, p.getImagePath());
    }

    private List<Product> query(String sql) {
        try (Connection c = DatabaseConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Product> list = new ArrayList<>();
            while (rs.next()) {
                Product p = new Product();
                p.setId(rs.getLong("id"));
                p.setName(rs.getString("name"));
                p.setCategoryId(rs.getLong("category_id"));
                p.setCategoryName(rs.getString("category_name"));
                p.setPriceCents(rs.getLong("price"));
                p.setActive(JdbcUtil.getBool(rs, "active"));
                p.setFavorite(JdbcUtil.getBool(rs, "favorite"));
                p.setIcon(rs.getString("icon"));
                p.setImagePath(rs.getString("image_path"));
                list.add(p);
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des produits impossible", e);
        }
    }
}

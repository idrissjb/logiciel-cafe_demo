package com.cafemanager.dao;

import com.cafemanager.util.DataAccessException;
import com.cafemanager.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class SettingsDAO {

    public Map<String, String> loadAll() {
        try (Connection c = DatabaseConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT key, value FROM settings")) {
            Map<String, String> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getString(1), rs.getString(2));
            }
            return map;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des paramètres impossible", e);
        }
    }

    public void saveAll(Map<String, String> values) {
        DatabaseConnection.txRun(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO settings(key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value")) {
                for (Map.Entry<String, String> e : values.entrySet()) {
                    ps.setString(1, e.getKey());
                    ps.setString(2, e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        });
    }
}

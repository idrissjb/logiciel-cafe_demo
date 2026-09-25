package com.cafemanager.util;

import com.cafemanager.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/** Création du schéma SQLite et des données de démonstration au premier lancement. */
final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    private static final String[] SCHEMA = {
            "CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT NOT NULL UNIQUE COLLATE NOCASE,"
                    + "password_hash TEXT NOT NULL,"
                    + "salt TEXT NOT NULL,"
                    + "full_name TEXT NOT NULL,"
                    + "role TEXT NOT NULL,"
                    + "active INTEGER NOT NULL DEFAULT 1,"
                    + "created_at TEXT NOT NULL)",

            "CREATE TABLE IF NOT EXISTS categories ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL UNIQUE COLLATE NOCASE,"
                    + "icon TEXT,"
                    + "display_order INTEGER NOT NULL DEFAULT 0,"
                    + "active INTEGER NOT NULL DEFAULT 1)",

            "CREATE TABLE IF NOT EXISTS products ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL,"
                    + "category_id INTEGER REFERENCES categories(id) ON DELETE RESTRICT,"
                    + "price INTEGER NOT NULL CHECK (price >= 0),"          // centimes
                    + "active INTEGER NOT NULL DEFAULT 1,"
                    + "favorite INTEGER NOT NULL DEFAULT 0,"
                    + "icon TEXT,"
                    + "image_path TEXT,"
                    + "created_at TEXT)",

            "CREATE TABLE IF NOT EXISTS tables_cafe ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL UNIQUE COLLATE NOCASE,"
                    + "status TEXT NOT NULL DEFAULT 'LIBRE',"
                    + "display_order INTEGER NOT NULL DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS orders ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "ticket_number TEXT NOT NULL UNIQUE,"
                    + "table_id INTEGER REFERENCES tables_cafe(id) ON DELETE SET NULL,"
                    + "server_id INTEGER REFERENCES users(id) ON DELETE SET NULL,"
                    + "created_at TEXT NOT NULL,"
                    + "paid_at TEXT,"
                    + "status TEXT NOT NULL,"
                    + "subtotal INTEGER NOT NULL DEFAULT 0,"
                    + "discount INTEGER NOT NULL DEFAULT 0,"
                    + "discount_mode TEXT NOT NULL DEFAULT 'NONE',"
                    + "discount_value INTEGER NOT NULL DEFAULT 0,"
                    + "total INTEGER NOT NULL DEFAULT 0,"
                    + "payment_method TEXT)",

            "CREATE TABLE IF NOT EXISTS order_items ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,"
                    + "product_id INTEGER REFERENCES products(id) ON DELETE SET NULL,"
                    + "product_name TEXT NOT NULL,"
                    + "unit_price INTEGER NOT NULL,"
                    + "quantity INTEGER NOT NULL CHECK (quantity > 0),"
                    + "line_total INTEGER NOT NULL)",

            "CREATE TABLE IF NOT EXISTS payments ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,"
                    + "method TEXT NOT NULL,"
                    + "amount_due INTEGER NOT NULL,"
                    + "amount_received INTEGER NOT NULL,"
                    + "change_given INTEGER NOT NULL DEFAULT 0,"
                    + "paid_at TEXT NOT NULL,"
                    + "cashier_id INTEGER REFERENCES users(id) ON DELETE SET NULL)",

            "CREATE TABLE IF NOT EXISTS tickets ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "ticket_number TEXT NOT NULL UNIQUE,"
                    + "order_id INTEGER NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,"
                    + "issued_at TEXT NOT NULL,"
                    + "print_count INTEGER NOT NULL DEFAULT 0,"
                    + "last_printed_at TEXT)",

            "CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)",

            // Compteur de tickets : un seul point d'allocation => jamais deux tickets avec le même numéro.
            "CREATE TABLE IF NOT EXISTS sequences (name TEXT PRIMARY KEY, value INTEGER NOT NULL)",

            "CREATE INDEX IF NOT EXISTS idx_orders_status_paid ON orders(status, paid_at)",
            "CREATE INDEX IF NOT EXISTS idx_orders_table ON orders(table_id, status)",
            "CREATE INDEX IF NOT EXISTS idx_items_order ON order_items(order_id)",
            "CREATE INDEX IF NOT EXISTS idx_products_cat ON products(category_id)"
    };

    static void run() {
        DatabaseConnection.txRun(c -> {
            try (Statement st = c.createStatement()) {
                for (String sql : SCHEMA) {
                    st.execute(sql);
                }
            }
            seedSettings(c);
            seedUsers(c);
            seedCatalog(c);
            seedTables(c);
            try (Statement st = c.createStatement()) {
                st.execute("INSERT OR IGNORE INTO sequences(name, value) VALUES ('ticket', 0)");
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ seeds

    private static boolean isEmpty(Connection c, String table) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private static void seedSettings(Connection c) throws SQLException {
        String[][] defaults = {
                {"cafe_name", "CAFÉ CENTRAL"},
                {"address", "Avenue Mohammed V, Kenitra"},
                {"phone", "05 22 00 00 00"},
                {"logo_path", ""},
                {"currency", "DH"},
                {"vat_rate", "0"},
                {"printer_name", ""},
                {"auto_print", "true"},
                {"ticket_width", "80"},
                {"theme", "dark"},
                {"register_number", "1"},
                {"footer_message", "Merci de votre visite"},
                {"server_can_pay", "false"}
        };
        try (PreparedStatement ps = c.prepareStatement("INSERT OR IGNORE INTO settings(key, value) VALUES (?, ?)")) {
            for (String[] kv : defaults) {
                ps.setString(1, kv[0]);
                ps.setString(2, kv[1]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedUsers(Connection c) throws SQLException {
        if (!isEmpty(c, "users")) {
            return;
        }
        addUser(c, "admin", "admin123", "Administrateur", Role.ADMINISTRATEUR);
        addUser(c, "sara", "1234", "Sara", Role.CAISSIER);
        addUser(c, "yassine", "1234", "Yassine", Role.SERVEUR);
    }

    private static void addUser(Connection c, String username, String password, String fullName, Role role)
            throws SQLException {
        String salt = PasswordUtil.newSalt();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users(username, password_hash, salt, full_name, role, active, created_at) VALUES (?,?,?,?,?,1,?)")) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password, salt));
            ps.setString(3, salt);
            ps.setString(4, fullName);
            ps.setString(5, role.name());
            ps.setString(6, DateUtil.toDb(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    private static void seedCatalog(Connection c) throws SQLException {
        if (!isEmpty(c, "categories")) {
            return;
        }
        String[][] categories = {
                {"Cafés", "☕"}, {"Thés", "🍵"}, {"Boissons", "🥤"}, {"Viennoiseries", "🥐"},
                {"Desserts", "🍰"}, {"Sandwichs", "🥪"}, {"Eau", "💧"}
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO categories(name, icon, display_order, active) VALUES (?,?,?,1)")) {
            for (int i = 0; i < categories.length; i++) {
                ps.setString(1, categories[i][0]);
                ps.setString(2, categories[i][1]);
                ps.setInt(3, i + 1);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // catégorie, nom, prix (DH), icône, favori
        Object[][] products = {
                {"Cafés", "Café normal", 8, "☕", true},
                {"Cafés", "Café allongé", 10, "☕", false},
                {"Cafés", "Café au lait", 12, "🥛", true},
                {"Cafés", "Cappuccino", 15, "☕", true},
                {"Cafés", "Espresso", 10, "☕", false},
                {"Cafés", "Chocolat chaud", 15, "🍫", false},
                {"Thés", "Thé marocain", 10, "🍵", true},
                {"Thés", "Thé noir", 10, "🍵", false},
                {"Thés", "Thé menthe", 10, "🌿", false},
                {"Boissons", "Coca-Cola", 10, "🥤", false},
                {"Boissons", "Sprite", 10, "🥤", false},
                {"Eau", "Eau 50cl", 6, "💧", false},
                {"Eau", "Eau 1.5L", 10, "💧", false},
                {"Viennoiseries", "Croissant", 8, "🥐", true},
                {"Viennoiseries", "Pain au chocolat", 10, "🥐", false},
                {"Desserts", "Gâteau maison", 12, "🍰", false},
                {"Desserts", "Salade de fruits", 15, "🍓", false},
                {"Sandwichs", "Sandwich thon", 20, "🥪", false},
                {"Sandwichs", "Sandwich poulet", 25, "🥪", false}
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO products(name, category_id, price, active, favorite, icon, created_at) "
                        + "VALUES (?, (SELECT id FROM categories WHERE name = ?), ?, 1, ?, ?, ?)")) {
            String now = DateUtil.toDb(LocalDateTime.now());
            for (Object[] p : products) {
                ps.setString(1, (String) p[1]);
                ps.setString(2, (String) p[0]);
                ps.setLong(3, ((Integer) p[2]) * 100L);
                ps.setInt(4, ((Boolean) p[4]) ? 1 : 0);
                ps.setString(5, (String) p[3]);
                ps.setString(6, now);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedTables(Connection c) throws SQLException {
        if (!isEmpty(c, "tables_cafe")) {
            return;
        }
        String[] names = {"Table 1", "Table 2", "Table 3", "Table 4", "Table 5", "Table 6", "Terrasse 1", "Terrasse 2"};
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO tables_cafe(name, status, display_order) VALUES (?, 'LIBRE', ?)")) {
            for (int i = 0; i < names.length; i++) {
                ps.setString(1, names[i]);
                ps.setInt(2, i + 1);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}

package com.cafemanager.util;

import org.sqlite.SQLiteConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Accès JDBC à la base SQLite locale.
 * Chaque appel ouvre une connexion (très peu coûteux avec SQLite) : pas d'état partagé, pas de fuite.
 * Les opérations qui touchent plusieurs tables passent par {@link #tx(TxWork)} (tout ou rien).
 */
public final class DatabaseConnection {

    /** Travail à exécuter dans une transaction. */
    @FunctionalInterface
    public interface TxWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private static String url;
    private static Properties props;

    private DatabaseConnection() {
    }

    /** Ouvre (et crée si nécessaire) la base, le schéma et les données de démarrage. */
    public static synchronized void init() {
        String file = System.getProperty("cafe.db", AppPaths.database().toString());
        init(file);
    }

    public static synchronized void init(String databaseFile) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DataAccessException("Pilote SQLite introuvable", e);
        }
        SQLiteConfig cfg = new SQLiteConfig();
        cfg.enforceForeignKeys(true);
        cfg.setJournalMode(SQLiteConfig.JournalMode.WAL);
        cfg.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        cfg.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        cfg.setBusyTimeout(5000);
        props = cfg.toProperties();
        url = "jdbc:sqlite:" + databaseFile;
        DatabaseInitializer.run();
    }

    public static Connection get() throws SQLException {
        if (url == null) {
            throw new IllegalStateException("DatabaseConnection.init() n'a pas été appelé");
        }
        return DriverManager.getConnection(url, props);
    }

    /** Exécute {@code work} dans une transaction : commit si tout va bien, rollback sinon. */
    public static <T> T tx(TxWork<T> work) {
        try (Connection c = get()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            } catch (SQLException e) {
                c.rollback();
                throw new DataAccessException("Erreur base de données : " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur base de données : " + e.getMessage(), e);
        }
    }

    /** Variante sans valeur de retour. */
    public static void txRun(TxWork<Void> work) {
        tx(work);
    }
}

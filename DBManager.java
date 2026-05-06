package Project.service;

import java.sql.*;
import java.util.concurrent.*;
import javax.swing.*;
import Project.GlassCalculator;

public class DBManager {
    private static final String URL = "jdbc:mysql://localhost:3306/mydb"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true";
    private static final String USER = "root";
    private static final String PASS = "lunapnb1.";

    private final GlassCalculator calculator;
    private Connection conn;
    private final ExecutorService dbExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DB-Worker");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean ready = false;

    public DBManager(GlassCalculator calculator) {
        this.calculator = calculator;
    }

    public void init() {
        dbExec.execute(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                ensureDatabase();
                conn = DriverManager.getConnection(URL, USER, PASS);
                createTable();
                ready = true;
                System.out.println("✅ DB connected");
                SwingUtilities.invokeLater(calculator::refreshStatusBar);
            } catch (ClassNotFoundException cnfe) {
                System.err.println("⚠️ MySQL JDBC Driver not found. Please download mysql-connector-java.jar and add to classpath.");
            } catch (Exception e) {
                System.err.println("⚠️ DB unavailable: " + e.getMessage());
            }
        });
    }

    public void saveAsync(String expression, String result) {
        if (!ready) return;
        dbExec.execute(() -> {
            try {
                reconnectIfNeeded();
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO calculation_history (expression, result) VALUES (?, ?)")) {
                    ps.setString(1, expression);
                    ps.setString(2, result);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                System.err.println("⚠️ DB write failed: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        dbExec.execute(() -> {
            try {
                if (conn != null && !conn.isClosed()) conn.close();
            } catch (SQLException ignored) {
            }
        });
        dbExec.shutdown();
    }

    public boolean isReady() {
        return ready;
    }

    private void reconnectIfNeeded() throws SQLException {
        if (conn == null || !conn.isValid(2)) {
            conn = DriverManager.getConnection(URL, USER, PASS);
        }
    }

    private void ensureDatabase() throws Exception {
        String rootUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try (Connection c = DriverManager.getConnection(rootUrl, USER, PASS);
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS mydb");
        }
    }

    private void createTable() throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS calculation_history (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    expression  VARCHAR(500) NOT NULL,
                    result      VARCHAR(100) NOT NULL,
                    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_created (created_at)
                )
            """);
        }
    }
}
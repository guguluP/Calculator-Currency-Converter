package Project.service;

import java.sql.*;
import java.util.concurrent.*;
import javax.swing.*;
import Project.GlassCalculator;
import Project.config.AppConfig;

/**
 * Database manager with improved security, thread safety, and reconnection logic.
 * Now uses AppConfig for secure credential storage.
 *
 * @author GlassCalculator Team
 * @version 1.0
 */
public class DBManager {
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final int RECONNECT_DELAY_MS = 1000;
    private static final String EXPRESSION_MAX_LENGTH = "500";
    private static final String RESULT_MAX_LENGTH = "100";

    private final GlassCalculator calculator;
    private Connection conn;
    private final ExecutorService dbExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DB-Worker");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean ready = false;
    private volatile boolean driverLoaded = false;
    private int reconnectAttempts = 0;

    // Dynamic JDBC URL based on database type
    private String jdbcUrl;
    private String username;
    private String password;

    public DBManager(GlassCalculator calculator) {
        this.calculator = calculator;
    }

    /**
     * Initializes the database connection using configured credentials.
     * If no credentials are configured, this will fail gracefully.
     */
    public void init() {
        dbExec.execute(() -> {
            try {
                // Check if database is configured
                if (!AppConfig.isDatabaseConfigured()) {
                    System.err.println("⚠️ Database not configured. User should run setup wizard.");
                    driverLoaded = false;
                    ready = false;
                    return;
                }

                // Build JDBC URL based on database type
                String dbType = AppConfig.getDatabaseType().toLowerCase();
                buildJdbcUrl(dbType);

                // Attempt to load driver based on database type
                try {
                    switch (dbType) {
                        case "mysql":
                            Class.forName("com.mysql.cj.jdbc.Driver");
                            break;
                        case "sqlite":
                            Class.forName("org.sqlite.JDBC");
                            break;
                        case "postgresql":
                            Class.forName("org.postgresql.Driver");
                            break;
                        default:
                            Class.forName("com.mysql.cj.jdbc.Driver");
                    }
                    driverLoaded = true;
                    System.out.println("✅ JDBC driver loaded for " + dbType);
                } catch (ClassNotFoundException cnfe) {
                    System.err.println("⚠️ JDBC driver NOT found for " + dbType);
                    System.err.println("   Make sure " + getDriverName(dbType) + " is in your classpath");
                    driverLoaded = false;
                    return;
                }

                // Try to connect and ensure database exists
                ensureDatabase();
                conn = DriverManager.getConnection(jdbcUrl, username, password);
                createTable();
                ready = true;
                System.out.println("✅ Database connected and ready");
                SwingUtilities.invokeLater(calculator::refreshStatusBar);

            } catch (SQLException e) {
                System.err.println("⚠️ Database connection failed: " + e.getMessage());
                ready = false;
            } catch (Exception e) {
                System.err.println("⚠️ Unexpected error: " + e.getMessage());
                ready = false;
            }
        });
    }

    /**
     * Builds JDBC URL based on database type.
     */
    private void buildJdbcUrl(String dbType) {
        this.username = AppConfig.getDatabaseUser();
        this.password = AppConfig.getDatabasePassword();

        String host = AppConfig.getDatabaseHost();
        int port = AppConfig.getDatabasePort();
        String dbName = AppConfig.getDatabaseName();

        switch (dbType) {
            case "mysql":
                this.jdbcUrl = String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true",
                    host, port, dbName);
                break;
            case "sqlite":
                this.jdbcUrl = "jdbc:sqlite:" + dbName + ".db";
                break;
            case "postgresql":
                this.jdbcUrl = String.format(
                    "jdbc:postgresql://%s:%d/%s",
                    host, port, dbName);
                break;
            default:
                // Default to MySQL
                this.jdbcUrl = String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true",
                    host, port, dbName);
        }
    }

    /**
     * Gets the JDBC driver name for a database type.
     */
    private String getDriverName(String dbType) {
        return switch (dbType.toLowerCase()) {
            case "mysql" -> "mysql-connector-java";
            case "sqlite" -> "sqlite-jdbc";
            case "postgresql" -> "postgresql";
            default -> "MySQL JDBC Driver";
        };
    }

    public void saveAsync(String expression, String result, String type) {
        // Don't save error results or if not ready
        if (!ready || !driverLoaded || result.equals("Error")) {
            return;
        }

        // Validate input length
        if (expression.length() > Integer.parseInt(EXPRESSION_MAX_LENGTH)) {
            System.err.println("⚠️ Expression too long, not saving");
            return;
        }
        if (result.length() > Integer.parseInt(RESULT_MAX_LENGTH)) {
            System.err.println("⚠️ Result too long, not saving");
            return;
        }

        dbExec.execute(() -> {
            try {
                reconnectIfNeeded();
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO calculation_history (type, expression, result) VALUES (?, ?, ?)")) {
                    ps.setString(1, type);
                    ps.setString(2, expression);
                    ps.setString(3, result);
                    ps.executeUpdate();
                    System.out.println("✅ " + type + " saved");
                }
            } catch (SQLException e) {
                System.err.println("⚠️ Failed to save: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        dbExec.execute(() -> {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    System.out.println("✅ Database connection closed");
                }
            } catch (SQLException ignored) {
            }
        });

        // Properly shutdown executor
        try {
            dbExec.shutdown();
            if (!dbExec.awaitTermination(3, TimeUnit.SECONDS)) {
                dbExec.shutdownNow();
            }
        } catch (InterruptedException e) {
            dbExec.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isDriverLoaded() {
        return driverLoaded;
    }

    /**
     * Reconnect with retry logic and exponential backoff.
     */
    private void reconnectIfNeeded() throws SQLException {
        if (conn == null || !conn.isValid(2)) {
            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                throw new SQLException("Failed to reconnect after " + MAX_RECONNECT_ATTEMPTS + " attempts");
            }

            try {
                reconnectAttempts++;
                System.out.println("🔄 Reconnecting to database (attempt " + reconnectAttempts + ")...");
                conn = DriverManager.getConnection(jdbcUrl, username, password);
                reconnectAttempts = 0; // Reset on success
                ready = true;
                System.out.println("✅ Reconnected successfully");
            } catch (SQLException e) {
                ready = false;
                if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                    throw e;
                }
                // Exponential backoff before retry
                try {
                    Thread.sleep((long) RECONNECT_DELAY_MS * reconnectAttempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Ensures the database exists before connecting.
     */
    private void ensureDatabase() throws Exception {
        String dbType = AppConfig.getDatabaseType().toLowerCase();

        if ("sqlite".equals(dbType)) {
            // SQLite auto-creates the database file
            return;
        }

        if ("mysql".equals(dbType)) {
            String host = AppConfig.getDatabaseHost();
            int port = AppConfig.getDatabasePort();
            String dbName = AppConfig.getDatabaseName();
            String rootUrl = String.format(
                "jdbc:mysql://%s:%d/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port);

            try (Connection c = DriverManager.getConnection(rootUrl, username, password);
                 Statement s = c.createStatement()) {
                s.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
                System.out.println("✅ Database '" + dbName + "' ensured");
            }
        } else if ("postgresql".equals(dbType)) {
            // PostgreSQL typically requires manual database creation
            System.out.println("ℹ️ PostgreSQL database must be created manually");
        }
    }

    /**
     * Creates the calculation history table if it doesn't exist.
     */
    private void createTable() throws SQLException {
        try (Statement s = conn.createStatement()) {
            // Use ANSI SQL that works across databases
                String createTableSql = """
                CREATE TABLE IF NOT EXISTS calculation_history (
                    id          INTEGER PRIMARY KEY AUTO_INCREMENT,
                    type        VARCHAR(10) NOT NULL,
                    expression  VARCHAR(500) NOT NULL,
                    result      VARCHAR(100) NOT NULL,
                    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;

            // Adjust for SQLite
            String dbType = AppConfig.getDatabaseType().toLowerCase();
            if ("sqlite".equals(dbType)) {
                createTableSql = """
                    CREATE TABLE IF NOT EXISTS calculation_history (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        type        TEXT NOT NULL,
                        expression  TEXT NOT NULL,
                        result      TEXT NOT NULL,
                        created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """;
            }

            s.executeUpdate(createTableSql);
            System.out.println("✅ History table ready");
        }
    }

    /**
     * Loads conversion history from database.
     */
    public void loadConvHistoryAsync(java.util.function.Consumer<java.util.List<String>> callback) {
        dbExec.execute(() -> {
            java.util.List<String> history = new java.util.ArrayList<>();
            try {
                reconnectIfNeeded();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT expression, result FROM calculation_history WHERE type = 'conv' ORDER BY created_at DESC LIMIT 100")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String expr = rs.getString("expression");
                            String res = rs.getString("result");
                            history.add(expr + " = " + res);
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("⚠️ Failed to load conv history: " + e.getMessage());
            }
            SwingUtilities.invokeLater(() -> callback.accept(history));
        });
    }

    /**
     * Clears all calculation history.
     */
    public void clearHistory() {
        dbExec.execute(() -> {
            try {
                reconnectIfNeeded();
                try (Statement s = conn.createStatement()) {
                    s.executeUpdate("DELETE FROM calculation_history");
                    System.out.println("✅ History cleared");
                }
            } catch (SQLException e) {
                System.err.println("⚠️ Failed to clear history: " + e.getMessage());
            }
        });
    }
}
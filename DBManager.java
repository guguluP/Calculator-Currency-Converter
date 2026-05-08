package Project.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.concurrent.*;
import javax.swing.*;
import Project.GlassCalculator;

/**
 * Database manager with improved security, thread safety, and reconnection logic.
 */
public class DBManager {
    private static final Logger logger = LoggerFactory.getLogger(DBManager.class);

    private static final String URL = "jdbc:mysql://localhost:3306/mydb"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true";
    private static final String USER = "root";

    // 🔒 SECURITY FIX: Use environment variable
    private static final String PASS = getPassword();
    


    private final GlassCalculator calculator;
    private HikariDataSource dataSource;
    private final ExecutorService dbExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DB-Worker");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean ready = false;
    private volatile boolean driverLoaded = false;

    public DBManager(GlassCalculator calculator) {
        this.calculator = calculator;
    }

    /**
     * 🔒 Retrieve password from environment variable only
     */
    private static String getPassword() {
        String envPass = System.getenv("MYSQL_PASS");
        if (envPass == null || envPass.trim().isEmpty()) {
            throw new IllegalStateException("MYSQL_PASS environment variable must be set for database connection.");
        }
        return envPass;
    }

    public void init() {
        dbExec.execute(() -> {
            try {
                // Attempt to load MySQL driver
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    driverLoaded = true;
                    logger.info("MySQL JDBC driver loaded");
                } catch (ClassNotFoundException cnfe) {
                    logger.error("MySQL driver NOT found on classpath: {}", cnfe.getMessage());
                    driverLoaded = false;
                    return;
                }

                // If driver loaded, try to connect
                ensureDatabase();

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(URL);
                config.setUsername(USER);
                config.setPassword(PASS);
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
                config.setIdleTimeout(300000);
                config.setMaxLifetime(600000);
                dataSource = new HikariDataSource(config);

                try (Connection testConn = dataSource.getConnection()) {
                    createTable(testConn);
                }
                ready = true;
                logger.info("Database connected and ready");
                SwingUtilities.invokeLater(calculator::refreshStatusBar);
                
            } catch (SQLException e) {
                logger.error("Database connection failed: {}", e.getMessage());
                ready = false;
            } catch (Exception e) {
                logger.error("Unexpected error during DB init: {}", e.getMessage());
                ready = false;
            }
        });
    }

    public void saveAsync(String expression, String result) {
        // 🔧 ENHANCEMENT: Don't save error results
        if (!ready || !driverLoaded || result.equals("Error")) {
            return;
        }
        
        dbExec.execute(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO calculation_history (expression, result) VALUES (?, ?)")) {
                ps.setString(1, expression);
                ps.setString(2, result);
                ps.executeUpdate();
                logger.debug("Calculation saved: {} = {}", expression, result);
            } catch (SQLException e) {
                logger.error("Failed to save calculation: {}", e.getMessage());
            }
        });
    }

    public void shutdown() {
        dbExec.execute(() -> {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.info("Database connection pool closed");
            }
        });
        
        // 🔧 IMPROVEMENT: Properly shutdown executor
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



    private void ensureDatabase() throws Exception {
        String rootUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try (Connection c = DriverManager.getConnection(rootUrl, USER, PASS);
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS mydb");
            logger.info("Database 'mydb' ensured");
        }
    }

    private void createTable(Connection conn) throws SQLException {
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
            logger.info("History table ready");
        }
    }
}
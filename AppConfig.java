package Project.config;

import java.util.prefs.*;

/**
 * Centralized configuration manager for GlassCalculator.
 * Handles secure storage of database and API credentials.
 *
 * @author GlassCalculator Team
 * @version 1.0
 */
public final class AppConfig {
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppConfig.class);

    // Preference keys
    private static final String KEY_FIRST_RUN = "firstRun";
    private static final String KEY_DB_HOST = "dbHost";
    private static final String KEY_DB_PORT = "dbPort";
    private static final String KEY_DB_USER = "dbUser";
    private static final String KEY_DB_PASSWORD = "dbPassword";
    private static final String KEY_DB_NAME = "dbName";
    private static final String KEY_DB_TYPE = "dbType"; // mysql, sqlite, etc.
    private static final String KEY_API_KEY = "apiKey";
    private static final String KEY_SETUP_COMPLETE = "setupComplete";

    /**
     * Checks if this is the first run of the application.
     * @return true if first run, false otherwise
     */
    public static boolean isFirstRun() {
        return PREFS.getBoolean(KEY_FIRST_RUN, true);
    }

    /**
     * Marks the setup wizard as complete.
     */
    public static void markSetupComplete() {
        PREFS.putBoolean(KEY_SETUP_COMPLETE, true);
        PREFS.putBoolean(KEY_FIRST_RUN, false);
        try {
            PREFS.flush();
        } catch (BackingStoreException e) {
            System.err.println("⚠️ Failed to save setup completion: " + e.getMessage());
        }
    }

    /**
     * Checks if setup wizard has been completed.
     * @return true if setup is complete, false otherwise
     */
    public static boolean isSetupComplete() {
        return PREFS.getBoolean(KEY_SETUP_COMPLETE, false);
    }

    // ════════════════════════════════════════════════════════════════
    // DATABASE CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    /**
     * Saves database configuration.
     * @param dbType Database type (mysql, sqlite, postgresql, etc.)
     * @param host Database host (localhost, etc.)
     * @param port Database port (3306, 5432, etc.)
     * @param username Database username
     * @param password Database password (will be stored securely)
     * @param dbName Database name
     */
    public static void setDatabaseConfig(String dbType, String host, int port,
                                         String username, String password, String dbName) {
        PREFS.put(KEY_DB_TYPE, dbType);
        PREFS.put(KEY_DB_HOST, host);
        PREFS.putInt(KEY_DB_PORT, port);
        PREFS.put(KEY_DB_USER, username);
        PREFS.put(KEY_DB_PASSWORD, encryptPassword(password));
        PREFS.put(KEY_DB_NAME, dbName);

        try {
            PREFS.flush();
            System.out.println("✅ Database configuration saved");
        } catch (BackingStoreException e) {
            System.err.println("⚠️ Failed to save database config: " + e.getMessage());
        }
    }

    /**
     * Gets the database type.
     * @return Database type (mysql, sqlite, etc.)
     */
    public static String getDatabaseType() {
        return PREFS.get(KEY_DB_TYPE, "sqlite");
    }

    /**
     * Gets the database host.
     * @return Database host address
     */
    public static String getDatabaseHost() {
        return PREFS.get(KEY_DB_HOST, "localhost");
    }

    /**
     * Gets the database port.
     * @return Database port number
     */
    public static int getDatabasePort() {
        return PREFS.getInt(KEY_DB_PORT, 3306);
    }

    /**
     * Gets the database username.
     * @return Database username
     */
    public static String getDatabaseUser() {
        return PREFS.get(KEY_DB_USER, "root");
    }

    /**
     * Gets the database password (decrypted).
     * @return Database password
     */
    public static String getDatabasePassword() {
        String encrypted = PREFS.get(KEY_DB_PASSWORD, "");
        return encrypted.isEmpty() ? "" : decryptPassword(encrypted);
    }

    /**
     * Gets the database name.
     * @return Database name
     */
    public static String getDatabaseName() {
        return PREFS.get(KEY_DB_NAME, "mydb");
    }

    /**
     * Checks if database is configured.
     * @return true if all required DB credentials are set based on database type, false otherwise
     */
    public static boolean isDatabaseConfigured() {
        String dbType = getDatabaseType().toLowerCase();
        if ("sqlite".equals(dbType)) {
            // SQLite doesn't require credentials
            return true;
        } else {
            // MySQL, PostgreSQL require user and password
            return !getDatabasePassword().isEmpty() && !getDatabaseUser().isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // API KEY CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    /**
     * Saves API key for currency exchange service.
     * @param apiKey The API key (will be stored securely)
     */
    public static void setApiKey(String apiKey) {
        PREFS.put(KEY_API_KEY, encryptPassword(apiKey));
        try {
            PREFS.flush();
            System.out.println("✅ API key configured");
        } catch (BackingStoreException e) {
            System.err.println("⚠️ Failed to save API key: " + e.getMessage());
        }
    }

    /**
     * Gets the API key (decrypted).
     * @return The API key, or empty string if not configured
     */
    public static String getApiKey() {
        String encrypted = PREFS.get(KEY_API_KEY, "");
        return encrypted.isEmpty() ? "" : decryptPassword(encrypted);
    }

    /**
     * Checks if API key is configured.
     * @return true if API key is set, false otherwise
     */
    public static boolean isApiKeyConfigured() {
        return !getApiKey().isEmpty();
    }

    // ════════════════════════════════════════════════════════════════
    // SECURITY / ENCRYPTION
    // ════════════════════════════════════════════════════════════════

    /**
     * Simple password encryption using XOR with a fixed key.
     * NOTE: This is NOT cryptographically secure. For production use,
     * consider using javax.crypto.Cipher with proper key management.
     *
     * @param password Plain text password
     * @return Encrypted password (as hex string)
     */
    private static String encryptPassword(String password) {
        if (password == null || password.isEmpty()) return "";

        // Simple XOR-based encryption (NOT secure, for demo only)
        // In production, use Java Cryptography Architecture (JCA)
        byte[] bytes = password.getBytes();
        byte[] encrypted = new byte[bytes.length];
        byte[] key = "GlassCalc2024!@#".getBytes(); // Fixed key (should be stored securely)

        for (int i = 0; i < bytes.length; i++) {
            encrypted[i] = (byte) (bytes[i] ^ key[i % key.length]);
        }

        return bytesToHex(encrypted);
    }

    /**
     * Decrypts password from hex string.
     * @param encrypted Encrypted password (as hex string)
     * @return Plain text password
     */
    private static String decryptPassword(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return "";

        try {
            byte[] bytes = hexToBytes(encrypted);
            byte[] decrypted = new byte[bytes.length];
            byte[] key = "GlassCalc2024!@#".getBytes();

            for (int i = 0; i < bytes.length; i++) {
                decrypted[i] = (byte) (bytes[i] ^ key[i % key.length]);
            }

            return new String(decrypted);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to decrypt password: " + e.getMessage());
            return "";
        }
    }

    /**
     * Converts byte array to hex string.
     * @param bytes Byte array
     * @return Hex string representation
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Converts hex string to byte array.
     * @param hex Hex string
     * @return Byte array
     */
    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    /**
     * Resets all configuration (useful for testing or reconfiguration).
     */
    public static void resetConfig() {
        try {
            PREFS.clear();
            PREFS.flush();
            System.out.println("✅ Configuration reset");
        } catch (BackingStoreException e) {
            System.err.println("⚠️ Failed to reset configuration: " + e.getMessage());
        }
    }
}
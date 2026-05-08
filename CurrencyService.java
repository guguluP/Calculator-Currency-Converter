package Project.service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import Project.config.AppConfig;

/**
 * Currency exchange service with secure API key management.
 * Fetches real-time exchange rates from exchangerate-api.com.
 * Uses AppConfig for secure API key storage.
 *
 * @author GlassCalculator Team
 * @version 1.0
 */
public final class CurrencyService {
    private static final long CACHE_TTL_MS = 300_000L; // 5 minutes
    private static final int TIMEOUT_MS = 8_000;

    private record CachedRates(Map<String, Double> rates,
                               String updatedUtc,
                               long fetchedAt) {
        boolean isExpired() {
            return System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS;
        }
    }

    private final Map<String, CachedRates> cache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Double>> previousRates = new ConcurrentHashMap<>();
    private final AtomicBoolean fetching = new AtomicBoolean(false);
    private final ScheduledExecutorService debouncer =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Currency-Debounce");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> pending;

    // Flag to track if API is available
    private volatile boolean apiConfigured = false;

    public CurrencyService() {
        this.apiConfigured = AppConfig.isApiKeyConfigured();
        if (!apiConfigured) {
            System.out.println("ℹ️ Currency API not configured. Users can add it later.");
        }
    }

    /**
     * Returns cached rate immediately if fresh; otherwise returns NaN
     * and fires a background fetch that calls onResult when done.
     */
    public double rateOrFetch(String from, String to,
                       Runnable onResult, Runnable onError) {
        // If API not configured, return NaN
        if (!apiConfigured) {
            System.err.println("⚠️ Currency API key not configured");
            SwingUtilities.invokeLater(onError);
            return Double.NaN;
        }

        CachedRates cr = cache.get(from);
        if (cr != null && !cr.isExpired()) {
            return cr.rates.getOrDefault(to, Double.NaN);
        }

        if (fetching.compareAndSet(false, true)) {
            new SwingWorker<CachedRates, Void>() {
                @Override
                protected CachedRates doInBackground() throws Exception {
                    return fetchRates(from);
                }

                @Override
                protected void done() {
                    fetching.set(false);
                    try {
                        CachedRates newRates = get();
                        // Store previous rates before updating cache
                        CachedRates oldRates = cache.get(from);
                        if (oldRates != null) {
                            previousRates.put(from, new HashMap<>(oldRates.rates()));
                        }
                        cache.put(from, newRates);
                        SwingUtilities.invokeLater(onResult);
                     } catch (Exception e) {
                         System.err.println("⚠️ Failed to fetch rates: " + e.getMessage());
                         SwingUtilities.invokeLater(onError);
                     }
                 }
             }.execute();
        }
        return Double.NaN; // signal: not cached yet
    }

    /**
     * Debounce: callback fires 350 ms after last call.
     */
    public void debounce(Runnable callback) {
        if (pending != null) pending.cancel(false);
        pending = debouncer.schedule(
                () -> SwingUtilities.invokeLater(callback), 350, TimeUnit.MILLISECONDS);
    }

    public String lastUpdated(String base) {
        CachedRates cr = cache.get(base);
        return cr != null ? cr.updatedUtc() : "—";
    }

    /**
     * Returns the rate change direction: "↑" for increase, "↓" for decrease, "" for no change
     */
    public String getRateChangeDirection(String from, String to) {
        Map<String, Double> prev = previousRates.get(from);
        CachedRates current = cache.get(from);

        if (prev == null || current == null) return "";

        Double prevRate = prev.get(to);
        Double currRate = current.rates().get(to);

        if (prevRate == null || currRate == null) return "";

        if (currRate > prevRate) return "↑";
        if (currRate < prevRate) return "↓";
        return "";
    }

    public void shutdown() {
        debouncer.shutdown();
    }

    /**
     * Fetches exchange rates from the API using secure credentials.
     */
    private CachedRates fetchRates(String base) throws Exception {
        String apiKey = AppConfig.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API key not configured");
        }

        URL url = new URL("https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/" + base);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();

        try {
            c.setRequestMethod("GET");
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("User-Agent", "GlassCalculator/1.0");

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            return parseJson(sb.toString());
        } finally {
            c.disconnect();
        }
    }

    /**
     * Lightweight JSON parser — no external dependency needed.
     * NOTE: For production, consider using Gson or Jackson library.
     */
    private CachedRates parseJson(String json) {
        Map<String, Double> rates = new LinkedHashMap<>();
        int rStart = json.indexOf("\"conversion_rates\":{");
        if (rStart != -1) {
            int open = json.indexOf('{', rStart);
            int close = json.indexOf('}', open);
            String block = json.substring(open + 1, close);
            for (String kv : block.split(",")) {
                String[] parts = kv.trim().split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].replace("\"", "").trim();
                    try {
                        rates.put(key, Double.parseDouble(parts[1].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        String utc = "Unknown";
        int uIdx = json.indexOf("\"time_last_update_utc\":\"");
        if (uIdx != -1) {
            int vs = uIdx + "\"time_last_update_utc\":\"".length();
            int ve = json.indexOf('"', vs);
            if (ve != -1) utc = json.substring(vs, ve);
        }
        return new CachedRates(Collections.unmodifiableMap(rates), utc,
                System.currentTimeMillis());
    }

    /**
     * Checks if currency service is available.
     * @return true if API key is configured, false otherwise
     */
    public boolean isAvailable() {
        return apiConfigured && AppConfig.isApiKeyConfigured();
    }

    /**
     * Configures the API key and enables the service.
     * @param apiKey The API key from exchangerate-api.com
     */
    public void configureApiKey(String apiKey) {
        if (apiKey != null && !apiKey.isEmpty()) {
            AppConfig.setApiKey(apiKey);
            this.apiConfigured = true;
            System.out.println("✅ Currency API configured");
        }
    }
}
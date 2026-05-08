package Project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import java.util.prefs.*;

public final class CurrencyService {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);

    private static final long CACHE_TTL_MS = 300_000L; // 5 minutes
    private static final int TIMEOUT_MS = 10_000; // Increased timeout
    private static final int DEBOUNCE_DELAY_MS = 350;

    private record CachedRates(Map<String, Double> rates,
                               String updatedUtc,
                               long fetchedAt) {
        boolean isExpired() {
            return System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS;
        }
    }

    private final Map<String, CachedRates> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean fetching = new AtomicBoolean(false);
    // Debouncer: cancels pending update if user keeps typing
    private final ScheduledExecutorService debouncer =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Currency-Debounce");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> pending;

    /**
     * Returns cached rate immediately if fresh; otherwise returns NaN
     * and fires a background fetch that calls onResult when done.
     */
    public double rateOrFetch(String from, String to,
                       Runnable onResult, Runnable onError) {
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
                        cache.put(from, get());
                        SwingUtilities.invokeLater(onResult);
                    } catch (Exception e) {
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

    public void shutdown() {
        debouncer.shutdown();
    }

    private CachedRates fetchRates(String base) throws Exception {
        String apiKey = System.getenv("EXCHANGE_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("EXCHANGE_API_KEY environment variable must be set for currency conversion.");
        }
        URL url = new URL("https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/" + base);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        StringBuilder sb = new StringBuilder();
        try {
            c.setRequestMethod("GET");
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            c.setRequestProperty("Accept", "application/json");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
        } finally {
            c.disconnect();
        }
        return parseJson(sb.toString());
    }

    /**
     * Parse JSON response using Jackson.
     */
    private CachedRates parseJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(json);
            Map<String, Double> rates = new LinkedHashMap<>();
            JsonNode ratesNode = root.get("conversion_rates");
            if (ratesNode != null && ratesNode.isObject()) {
                ratesNode.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    JsonNode valueNode = entry.getValue();
                    if (valueNode.isNumber()) {
                        rates.put(key, valueNode.asDouble());
                    }
                });
            }
            String utc = root.has("time_last_update_utc") ? root.get("time_last_update_utc").asText() : "Unknown";
            return new CachedRates(Collections.unmodifiableMap(rates), utc, System.currentTimeMillis());
        } catch (Exception e) {
            logger.error("Failed to parse JSON: {}", e.getMessage());
            return new CachedRates(Collections.emptyMap(), "Error", System.currentTimeMillis());
        }
    }
}
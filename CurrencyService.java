package Project.service;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import java.util.prefs.*;

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
        String apiKey = Preferences.userRoot().get("calc_api_key", "97ab7ceab50c9baf51e43393");
        URL url = new URL("https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/" + base);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setRequestProperty("Accept", "application/json");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        c.disconnect();
        return parseJson(sb.toString());
    }

    /**
     * Lightweight JSON parser — no external dependency needed.
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
}
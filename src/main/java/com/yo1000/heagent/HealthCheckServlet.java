package com.yo1000.heagent;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Priority by Parameter Sources
 * 1. Agent args
 * 2. System props
 * 3. Env vars
 */
public class HealthCheckServlet extends HttpServlet {
    private static final String ARG_NAME_MAX_USED_HEAP_SIZE = "max-used-heap-size";
    private static final String ARG_NAME_MAX_USED_HEAP_RATIO = "max-used-heap-ratio";
    private static final String ARG_NAME_MIN_AVAILABLE_HEAP_SIZE = "min-available-heap-size";
    private static final String ARG_NAME_MIN_AVAILABLE_HEAP_RATIO = "min-available-heap-ratio";
    private static final String QUERY_NAME_SUSPEND = "suspend";
    private static final String QUERY_VALUE_SUSPEND_ON = "1";
    private static final String QUERY_VALUE_SUSPEND_OFF = "0";
    private static final String METRICS_NAME_HEALTH = "health";

    private long maxUsedHeapSize;
    private long minAvailableHeapSize;
    private double maxUsedHeapRatio;
    private double minAvailableHeapRatio;
    private boolean suspended = false;

    public HealthCheckServlet(ParameterLoader loader) {
        maxUsedHeapSize = loader.loadByName(ARG_NAME_MAX_USED_HEAP_SIZE, 0L, (s, defaultValue) -> {
            try { return Long.parseLong(s); } catch (Exception e) { return defaultValue; }
        });
        maxUsedHeapRatio = loader.loadByName(ARG_NAME_MAX_USED_HEAP_RATIO, 0.0, (s, defaultValue) -> {
            try { return Double.parseDouble(s); } catch (Exception e) { return defaultValue; }
        });
        minAvailableHeapSize = loader.loadByName(ARG_NAME_MIN_AVAILABLE_HEAP_SIZE, 0L, (s, defaultValue) -> {
            try { return Long.parseLong(s); } catch (Exception e) { return defaultValue; }
        });
        minAvailableHeapRatio = loader.loadByName(ARG_NAME_MIN_AVAILABLE_HEAP_RATIO, 0.0, (s, defaultValue) -> {
            try { return Double.parseDouble(s); } catch (Exception e) { return defaultValue; }
        });
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String suspend = request.getParameter(QUERY_NAME_SUSPEND);
        if (suspend != null && suspend.equals(QUERY_VALUE_SUSPEND_ON)) {
            suspended = true;
        }
        if (suspend != null && suspend.equals(QUERY_VALUE_SUSPEND_OFF)) {
            suspended = false;
        }

        Map<String, Object> metrics = collectMetrics();
        Object health = metrics.get(METRICS_NAME_HEALTH);

        if (health instanceof Boolean && (Boolean) health) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }

        response.setContentType("application/json");
        response.getWriter().println(new ObjectMapper().writeValueAsString(metrics));
    }

    protected Map<String, Object> collectMetrics() {
        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memoryUsage = mbean.getHeapMemoryUsage();

        long max = memoryUsage.getMax();
        long committed = memoryUsage.getCommitted();
        long used = memoryUsage.getUsed();
        long available = (max > 0L ? max : committed) - used;
        boolean health = isHealth(max, committed, used, available);

        return new LinkedHashMap<String, Object>() {{
            put(METRICS_NAME_HEALTH, health);
            put("suspended", suspended);
            put("heap", new LinkedHashMap<String, Object>() {{
                put("measured", new LinkedHashMap<String, Object>() {{
                    put("max", max);
                    put("committed", committed);
                    put("used", used);
                    put("available", available);
                }});
                put("threshold", new LinkedHashMap<String, Object>() {{
                    put("used", new LinkedHashMap<String, Object>() {{
                        put("max-size", maxUsedHeapSize);
                        put("max-ratio", maxUsedHeapRatio);
                    }});
                    put("available", new LinkedHashMap<String, Object>() {{
                        put("min-size", minAvailableHeapSize);
                        put("min-ratio", minAvailableHeapRatio);
                    }});
                }});
            }});
        }};
    }

    private boolean isHealth(long max, long committed, long used, long available) {
        if (suspended) {
            return false;
        }

        if (maxUsedHeapSize > 0L && used > maxUsedHeapSize) {
            return false;
        }
        if (minAvailableHeapSize > 0L && available < minAvailableHeapSize) {
            return false;
        }

        if (maxUsedHeapRatio > 0.0) {
            double maxUsedHeapSizeByRatio = (max > 0L ? max : committed) * maxUsedHeapRatio;
            if (used > maxUsedHeapSizeByRatio) return false;
        }
        if (minAvailableHeapRatio > 0.0) {
            double minAvailableSizeByRatio = (max > 0L ? max : committed) * minAvailableHeapRatio;
            if (available < minAvailableSizeByRatio) return false;
        }
        return true;
    }
}

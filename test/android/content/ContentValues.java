package android.content;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal test-only stand-in for android.content.ContentValues.
 * Stores values with full precision (Long is kept as a 64-bit Long).
 */
public class ContentValues {
    private final Map<String, Object> values = new HashMap<String, Object>();

    public void put(final String key, final String value) {
        values.put(key, value);
    }

    public void put(final String key, final Long value) {
        values.put(key, value);
    }

    public void put(final String key, final Float value) {
        values.put(key, value);
    }

    public boolean containsKey(final String key) {
        return values.containsKey(key);
    }

    public Long getAsLong(final String key) {
        final Object v = values.get(key);
        return v == null ? null : ((Number) v).longValue();
    }

    public String getAsString(final String key) {
        final Object v = values.get(key);
        return v == null ? null : v.toString();
    }

    public Float getAsFloat(final String key) {
        final Object v = values.get(key);
        return v == null ? null : ((Number) v).floatValue();
    }
}

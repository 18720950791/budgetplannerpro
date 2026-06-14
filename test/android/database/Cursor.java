package android.database;

import java.util.List;

/**
 * Minimal test-only stand-in for android.database.Cursor.
 *
 * It faithfully reproduces the precision behaviour of a real SQLite cursor:
 * a column may hold a 64-bit value, getLong() returns it intact, while
 * getInt() narrows it to 32 bits exactly like the real implementation. This
 * is what lets the tests distinguish the buggy getInt(0) read from the fixed
 * getLong(0) read.
 */
public class Cursor {
    private final List<Object[]> rows;
    private int position = -1;

    public Cursor(final List<Object[]> rows) {
        this.rows = rows;
    }

    public boolean moveToFirst() {
        if (rows.isEmpty()) {
            return false;
        }
        position = 0;
        return true;
    }

    public boolean moveToNext() {
        position++;
        return position < rows.size();
    }

    public long getLong(final int columnIndex) {
        return ((Number) rows.get(position)[columnIndex]).longValue();
    }

    public int getInt(final int columnIndex) {
        // Faithful to real SQLite/Android: a stored 64-bit value is narrowed
        // to 32 bits, which is the truncation bug under test.
        return (int) ((Number) rows.get(position)[columnIndex]).longValue();
    }

    public String getString(final int columnIndex) {
        return (String) rows.get(position)[columnIndex];
    }

    public float getFloat(final int columnIndex) {
        return ((Number) rows.get(position)[columnIndex]).floatValue();
    }

    public void close() {
        // no-op for the in-memory fake
    }
}

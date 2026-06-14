package android.database.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal in-memory test-only stand-in for android.database.sqlite.SQLiteDatabase.
 *
 * It implements only the operations BudgetDAO actually performs
 * (insertWithOnConflict / query / delete) over a single table whose rows are
 * keyed by their 64-bit budget_id, so the real DAO code runs unchanged.
 * Row layout matches the production schema: [budget_id, budget_name, starting_balance].
 */
public class SQLiteDatabase {
    public static final int CONFLICT_REPLACE = 5;

    private static final String KEY_BUDGET_ID = "budget_id";
    private static final String KEY_BUDGET_NAME = "budget_name";
    private static final String KEY_STARTING_BALANCE = "starting_balance";

    private final Map<Long, Object[]> store = new LinkedHashMap<Long, Object[]>();

    public void execSQL(final String sql) {
        // schema DDL is a no-op for the in-memory fake
    }

    public long insertWithOnConflict(final String table, final String nullColumnHack,
                                     final ContentValues values, final int conflictAlgorithm) {
        final long id = values.containsKey(KEY_BUDGET_ID) ? values.getAsLong(KEY_BUDGET_ID) : nextRowId();
        store.put(id, new Object[]{id, values.getAsString(KEY_BUDGET_NAME), values.getAsFloat(KEY_STARTING_BALANCE)});
        return id;
    }

    public Cursor query(final String table, final String[] columns, final String selection,
                        final String[] selectionArgs, final String groupBy, final String having,
                        final String orderBy) {
        final List<Object[]> result = new ArrayList<Object[]>();
        if (selection != null && selectionArgs != null && selectionArgs.length > 0) {
            final Object[] row = store.get(Long.parseLong(selectionArgs[0]));
            if (row != null) {
                result.add(row);
            }
        } else {
            result.addAll(store.values());
        }
        return new Cursor(result);
    }

    public int delete(final String table, final String whereClause, final String[] whereArgs) {
        if (whereClause != null && whereArgs != null && whereArgs.length > 0) {
            return store.remove(Long.parseLong(whereArgs[0])) != null ? 1 : 0;
        }
        final int removed = store.size();
        store.clear();
        return removed;
    }

    public void close() {
        // no-op: data persists across open/close cycles, like a real on-disk DB
    }

    private long nextRowId() {
        long max = 0;
        for (final Long key : store.keySet()) {
            if (key > max) {
                max = key;
            }
        }
        return max + 1;
    }
}

package android.database.sqlite;

import android.content.Context;

/**
 * Minimal test-only stand-in for android.database.sqlite.SQLiteOpenHelper.
 *
 * Hands out a single shared SQLiteDatabase for both readable and writable
 * access (so writes are visible to subsequent reads) and invokes onCreate
 * exactly once, mirroring the part of the real lifecycle BudgetDAO relies on.
 */
public abstract class SQLiteOpenHelper {
    private final SQLiteDatabase database = new SQLiteDatabase();
    private boolean created = false;

    public SQLiteOpenHelper(final Context context, final String name, final Object factory, final Integer version) {
        // configuration is irrelevant for the in-memory fake
    }

    public abstract void onCreate(SQLiteDatabase db);

    public abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    public SQLiteDatabase getWritableDatabase() {
        ensureCreated();
        return database;
    }

    public SQLiteDatabase getReadableDatabase() {
        ensureCreated();
        return database;
    }

    private void ensureCreated() {
        if (!created) {
            created = true;
            onCreate(database);
        }
    }
}

package com.twansoftware.budgetplannerpro.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.twansoftware.budgetplannerpro.entity.Debit;
import com.twansoftware.budgetplannerpro.iface.CursorConvertable;
import roboguice.inject.ContextSingleton;
import roboguice.util.Ln;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ContextSingleton
public class DebitsDAO extends SQLiteOpenHelper implements CursorConvertable<Debit> {
    public static final String TABLE_NAME = "debits_table";
    private static final String KEY_DEBIT_ID = "debit_id";
    private static final String KEY_DEBIT_DESCRIPTION = "debit_description";
    private static final String KEY_DEBIT_AMOUNT = "debit_amount";
    private static final String KEY_BUDGET_ID = "budget_id";

    private static final String DATABASE_NAME = "debits.db";
    private static final Integer DATABASE_VERSION = 1;

    private static final String CREATE_SQL = "CREATE TABLE "
            + TABLE_NAME
            + "("
            + KEY_DEBIT_ID + " INTEGER primary key autoincrement"
            + ", "
            + KEY_DEBIT_DESCRIPTION + " TEXT not null"
            + ", "
            + KEY_DEBIT_AMOUNT + " REAL not null"
            + ", "
            + KEY_BUDGET_ID + " INTEGER not null"
            + ", "
            + "FOREIGN KEY(" + KEY_BUDGET_ID + ") REFERENCES " + BudgetDAO.TABLE_NAME + "(" + BudgetDAO.KEY_BUDGET_ID + ") ON DELETE CASCADE"
            + ");";

    @Inject
    public DebitsDAO(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public Debit cursorToObject(final Cursor cursor) {
        return new Debit(cursor.getLong(0), cursor.getString(1), cursor.getFloat(2), cursor.getLong(3));
    }

    @Override
    public ContentValues objectToContentValues(final Debit object) {
        final ContentValues contentValues = new ContentValues();
        if (object.getDebitId() != null && object.getDebitId() > 0) {
            contentValues.put(KEY_DEBIT_ID, object.getDebitId());
        }
        contentValues.put(KEY_DEBIT_DESCRIPTION, object.getDescription());
        contentValues.put(KEY_DEBIT_AMOUNT, object.getAmount());
        contentValues.put(KEY_BUDGET_ID, object.getBudgetId());
        return contentValues;
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(CREATE_SQL);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        Ln.d("Upgrade called, but it wasn't needed...");
    }

    public Debit saveDebit(final Debit debit) {
        synchronized (DebitsDAO.class) {
            final SQLiteDatabase writableDatabase = getWritableDatabase();
            writableDatabase.insertWithOnConflict(TABLE_NAME, null, objectToContentValues(debit), SQLiteDatabase.CONFLICT_REPLACE);
            writableDatabase.close();
            return debit;
        }
    }

    public List<Debit> loadDebitsForBudgetId(final Long budgetId) {
        synchronized (DebitsDAO.class) {
            final List<Debit> debits = new ArrayList<Debit>();
            final SQLiteDatabase readableDatabase = getReadableDatabase();
            final Cursor cursor = readableDatabase.query(TABLE_NAME, null, KEY_BUDGET_ID + " = ?", new String[]{String.valueOf(budgetId)}, null, null, null);
            if (cursor.moveToFirst()) {
                debits.add(cursorToObject(cursor));
                while (cursor.moveToNext()) {
                    debits.add(cursorToObject(cursor));
                }
            }
            cursor.close();
            readableDatabase.close();
            return debits;
        }
    }

    public void deleteDebit(final Debit debit) {
        synchronized (DebitsDAO.class) {
            final SQLiteDatabase writableDatabase = getWritableDatabase();
            getWritableDatabase().delete(TABLE_NAME, KEY_DEBIT_ID + " = ?", new String[]{String.valueOf(debit.getDebitId())});
            writableDatabase.close();
        }
    }

    public void deleteDebitsForBudgetId(final Long budgetId) {
        synchronized (DebitsDAO.class) {
            final SQLiteDatabase writableDatabase = getWritableDatabase();
            getWritableDatabase().delete(TABLE_NAME, KEY_BUDGET_ID + " = ?", new String[]{String.valueOf(budgetId)});
            writableDatabase.close();
        }
    }

}

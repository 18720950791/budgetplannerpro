package com.twansoftware.budgetplannerpro.dao;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.twansoftware.budgetplannerpro.entity.Budget;
import com.twansoftware.budgetplannerpro.iface.CursorConvertable;
import roboguice.inject.ContextSingleton;
import roboguice.util.Ln;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ContextSingleton
public class BudgetDAO extends SQLiteOpenHelper implements CursorConvertable<Budget> {
    public static final String TABLE_NAME = "budget_table";
    public static final String KEY_BUDGET_ID = "budget_id";
    private static final String KEY_BUDGET_NAME = "budget_name";
    private static final String KEY_STARTING_BALANCE = "starting_balance";

    private static final String DATABASE_NAME = "budgets.db";
    private static final Integer DATABASE_VERSION = 1;

    private static final String CREATE_SQL = "CREATE TABLE "
            + TABLE_NAME
            + "("
            + KEY_BUDGET_ID + " INTEGER primary key autoincrement"
            + ", "
            + KEY_BUDGET_NAME + " text not null"
            + ", "
            + KEY_STARTING_BALANCE + " real not null"
            + ");";

    private static final String DELETE_SQL = "DELETE FROM " + TABLE_NAME;

    @Inject
    public BudgetDAO(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(CREATE_SQL);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        Ln.d("Upgrade called but no upgrade required...");
    }

    public Budget saveBudget(final Budget budget) {
        synchronized (BudgetDAO.class) {
            final SQLiteDatabase writableDatabase = getWritableDatabase();
            final long budgetId = writableDatabase.insertWithOnConflict(TABLE_NAME, null, objectToContentValues(budget), SQLiteDatabase.CONFLICT_REPLACE);
            if (budget.getBudgetId() == null) {
                budget.setBudgetId(budgetId);
            }
            writableDatabase.close();
            return budget;
        }
    }

    public Budget loadBudgetById(final long budgetId) {
        synchronized (BudgetDAO.class) {
            final SQLiteDatabase readableDatabase = getReadableDatabase();
            final Cursor cursor = readableDatabase.query(TABLE_NAME, null, KEY_BUDGET_ID + " = ?",
                    new String[]{String.valueOf(budgetId)}, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    return cursorToObject(cursor);
                } else {
                    return null;
                }
            } finally {
                cursor.close();
                readableDatabase.close();
            }
        }
    }

    public List<Budget> loadAllBudgets() {
        synchronized (BudgetDAO.class) {
            final List<Budget> budgets = new ArrayList<Budget>();
            final SQLiteDatabase readableDatabase = getReadableDatabase();
            final Cursor cursor = readableDatabase.query(TABLE_NAME, null, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                budgets.add(cursorToObject(cursor));
                while (cursor.moveToNext()) {
                    budgets.add(cursorToObject(cursor));
                }
            }
            cursor.close();
            readableDatabase.close();
            return budgets;
        }
    }


    public void deleteBudgetById(Long budgetId) {
        synchronized (BudgetDAO.class) {
            final SQLiteDatabase writableDatabase = getWritableDatabase();
            writableDatabase.delete(TABLE_NAME, KEY_BUDGET_ID + " = ?", new String[]{String.valueOf(budgetId)});
            writableDatabase.close();
        }
    }

    @Override
    public Budget cursorToObject(final Cursor cursor) {
        return new Budget((long) cursor.getInt(0), cursor.getString(1), cursor.getFloat(2));
    }

    @Override
    public ContentValues objectToContentValues(final Budget budget) {
        final ContentValues contentValues = new ContentValues();
        if (budget.getBudgetId() != null && budget.getBudgetId() > 0) {
            contentValues.put(KEY_BUDGET_ID, budget.getBudgetId());
        }
        contentValues.put(KEY_BUDGET_NAME, budget.getName());
        contentValues.put(KEY_STARTING_BALANCE, budget.getStartingBalance());
        return contentValues;
    }
}

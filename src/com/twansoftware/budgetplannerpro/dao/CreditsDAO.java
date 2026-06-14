package com.twansoftware.budgetplannerpro.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.twansoftware.budgetplannerpro.entity.Credit;
import com.twansoftware.budgetplannerpro.iface.CursorConvertable;
import roboguice.inject.ContextSingleton;
import roboguice.util.Ln;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ContextSingleton
public class CreditsDAO extends SQLiteOpenHelper implements CursorConvertable<Credit> {
    public static final String TABLE_NAME = "credits_table";
    private static final String KEY_CREDIT_ID = "credit_id";
    private static final String KEY_CREDIT_DESCRIPTION = "credit_description";
    private static final String KEY_CREDIT_AMOUNT = "credit_amount";
    private static final String KEY_BUDGET_ID = "budget_id";

    private static final String DATABASE_NAME = "credits.db";
    private static final Integer DATABASE_VERSION = 1;

    private static final String CREATE_SQL = "CREATE TABLE "
            + TABLE_NAME
            + "("
            + KEY_CREDIT_ID + " INTEGER primary key autoincrement"
            + ", "
            + KEY_CREDIT_DESCRIPTION + " TEXT not null"
            + ", "
            + KEY_CREDIT_AMOUNT + " REAL not null"
            + ", "
            + KEY_BUDGET_ID + " INTEGER not null"
            + ", "
            + "FOREIGN KEY(" + KEY_BUDGET_ID + ") REFERENCES " + BudgetDAO.TABLE_NAME + "(" + BudgetDAO.KEY_BUDGET_ID + ") ON DELETE CASCADE"
            + ");";

    @Inject
    public CreditsDAO(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(CREATE_SQL);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        Ln.d("Upgrade called, but it wasn't needed...");
    }

    public Credit saveCredit(final Credit credit) {
        synchronized (CreditsDAO.class) {
            final SQLiteDatabase writableDatabase = getWritableDatabase();
            writableDatabase.insertWithOnConflict(TABLE_NAME, null, objectToContentValues(credit), SQLiteDatabase.CONFLICT_REPLACE);
            writableDatabase.close();
            return credit;
        }
    }

    public List<Credit> loadCreditsForBudgetId(final Long budgetId) {
        synchronized (CreditsDAO.class) {
            final List<Credit> credits = new ArrayList<Credit>();
            final SQLiteDatabase readableDatabase = getReadableDatabase();
            final Cursor cursor = readableDatabase.query(TABLE_NAME, null, KEY_BUDGET_ID + " = ?", new String[]{String.valueOf(budgetId)}, null, null, null);
            if (cursor.moveToFirst()) {
                credits.add(cursorToObject(cursor));
                while (cursor.moveToNext()) {
                    credits.add(cursorToObject(cursor));
                }
            }
            cursor.close();
            readableDatabase.close();
            return credits;
        }
    }

    public void deleteCredit(final Credit credit) {
        synchronized (CreditsDAO.class) {
            final SQLiteDatabase writableDatabase = getWritableDatabase();
            writableDatabase.delete(TABLE_NAME, KEY_CREDIT_ID + " = ?", new String[]{String.valueOf(credit.getCreditId())});
            writableDatabase.close();
        }
    }

    @Override
    public Credit cursorToObject(final Cursor cursor) {
        return new Credit(cursor.getLong(0), cursor.getString(1), cursor.getFloat(2), cursor.getLong(3));
    }

    @Override
    public ContentValues objectToContentValues(final Credit credit) {
        final ContentValues contentValues = new ContentValues();
        if (credit.getCreditId() != null && credit.getCreditId() > 0) {
            contentValues.put(KEY_CREDIT_ID, credit.getCreditId());
        }
        contentValues.put(KEY_CREDIT_AMOUNT, credit.getAmount());
        contentValues.put(KEY_BUDGET_ID, credit.getBudgetId());
        contentValues.put(KEY_CREDIT_DESCRIPTION, credit.getDescription());
        return contentValues;
    }

    public void deleteDebitsForBudgetId(final Long budgetId) {
        final SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.delete(TABLE_NAME, KEY_BUDGET_ID + " = ?", new String[]{String.valueOf(budgetId)});
        writableDatabase.close();
    }
}

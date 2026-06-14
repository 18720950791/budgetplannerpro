package com.twansoftware.budgetplannerpro.dao;

import android.content.ContentValues;
import android.database.Cursor;
import com.twansoftware.budgetplannerpro.entity.Budget;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BudgetDAO#cursorToObject(Cursor)} and related methods.
 *
 * Verifies that the primary key is read as a 64-bit long (via {@code Cursor.getLong})
 * and is NOT truncated to a 32-bit int, which would corrupt any ID that exceeds
 * {@link Integer#MAX_VALUE}.
 *
 * NOTE: We use {@code mock(BudgetDAO.class, CALLS_REAL_METHODS)} to test the real
 * cursorToObject / objectToContentValues logic while bypassing the SQLiteOpenHelper
 * constructor (which requires a non-null Android Context at runtime).
 *
 * Run with:
 *   mvn -f pom-test.xml test -Dtest=BudgetDAOTest
 */
public class BudgetDAOTest {

    /** A "spy-like" BudgetDAO: real method bodies, but no constructor side-effects. */
    private BudgetDAO dao;

    @Mock
    private Cursor cursor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // CALLS_REAL_METHODS lets us exercise cursorToObject() and objectToContentValues()
        // without invoking the SQLiteOpenHelper super-constructor (which needs a Context).
        dao = mock(BudgetDAO.class, CALLS_REAL_METHODS);

        when(cursor.getString(1)).thenReturn("Test Budget");
        when(cursor.getFloat(2)).thenReturn(500.00f);
    }

    /* ------------------------------------------------------------------ */
    /*  cursorToObject — primary-key width tests                           */
    /* ------------------------------------------------------------------ */

    @Test
    public void testCursorToObject_normalId() {
        long id = 42L;
        when(cursor.getLong(0)).thenReturn(id);

        Budget budget = dao.cursorToObject(cursor);

        assertEquals("Normal ID should be preserved", Long.valueOf(42L), budget.getBudgetId());
        assertEquals("Test Budget", budget.getName());
        assertEquals(Float.valueOf(500.00f), budget.getStartingBalance());

        verify(cursor).getLong(0);
        verify(cursor, never()).getInt(0);   // must NOT call getInt
    }

    @Test
    public void testCursorToObject_maxIntId() {
        long id = Integer.MAX_VALUE;   // 2147483647L
        when(cursor.getLong(0)).thenReturn(id);

        Budget budget = dao.cursorToObject(cursor);

        assertEquals("Integer.MAX_VALUE should be preserved",
                Long.valueOf(Integer.MAX_VALUE), budget.getBudgetId());
    }

    @Test
    public void testCursorToObject_largeIdExceedingMaxInt() {
        long id = 3_000_000_000L;   // > Integer.MAX_VALUE
        when(cursor.getLong(0)).thenReturn(id);

        Budget budget = dao.cursorToObject(cursor);

        assertEquals("Large ID must NOT be truncated",
                Long.valueOf(3_000_000_000L), budget.getBudgetId());
        assertTrue("Large ID must remain positive", budget.getBudgetId() > 0);
    }

    @Test
    public void testCursorToObject_veryLargeId() {
        long id = Long.MAX_VALUE - 1L;
        when(cursor.getLong(0)).thenReturn(id);

        Budget budget = dao.cursorToObject(cursor);

        assertEquals("Near-MAX_LONG ID must be preserved",
                Long.valueOf(Long.MAX_VALUE - 1L), budget.getBudgetId());
    }

    @Test
    public void testCursorToObject_zeroId() {
        when(cursor.getLong(0)).thenReturn(0L);

        Budget budget = dao.cursorToObject(cursor);

        assertEquals("Zero ID should be preserved", Long.valueOf(0L), budget.getBudgetId());
    }

    /* ------------------------------------------------------------------ */
    /*  Document the original bug: (long) cursor.getInt(0) truncates       */
    /* ------------------------------------------------------------------ */

    @Test
    public void testGetIntWouldTruncateLargeId() {
        // Reproduces the arithmetic of the original bug:
        //   long id = (long) cursor.getInt(0)
        // where the stored SQLite value exceeds Integer.MAX_VALUE.
        long realId = 3_000_000_000L;
        int  truncatedInt = (int) realId;      // -1294967296
        long buggyResult  = (long) truncatedInt;

        assertNotEquals("Buggy (long)getInt(0) must NOT equal the real ID",
                realId, buggyResult);
        assertTrue("Buggy result should be negative (sign-extended truncation)",
                buggyResult < 0);
    }

    /* ------------------------------------------------------------------ */
    /*  objectToContentValues — ID round-trip                              */
    /*  NOTE: The Android stub jar's ContentValues constructor throws      */
    /*  RuntimeException("Stub!"), so we use mockConstruction() to         */
    /*  intercept the `new ContentValues()` call inside objectToContentValues. */
    /* ------------------------------------------------------------------ */

    @Test
    public void testObjectToContentValues_largeId() {
        long largeId = 3_000_000_000L;
        Budget budget = new Budget(largeId, "Large ID Budget", 100.00f);

        try (org.mockito.MockedConstruction<ContentValues> mocked =
                     mockConstruction(ContentValues.class)) {
            dao.objectToContentValues(budget);

            ContentValues cv = mocked.constructed().get(0);
            // Verify put(KEY_BUDGET_ID, largeId) was called with the full 64-bit long
            verify(cv).put(BudgetDAO.KEY_BUDGET_ID, largeId);
        }
    }

    @Test
    public void testObjectToContentValues_nullId() {
        Budget budget = new Budget(null, "New Budget", 200.00f);

        try (org.mockito.MockedConstruction<ContentValues> mocked =
                     mockConstruction(ContentValues.class)) {
            dao.objectToContentValues(budget);

            ContentValues cv = mocked.constructed().get(0);
            // Null ID must NOT trigger put(KEY_BUDGET_ID, ...)
            verify(cv, never()).put(eq(BudgetDAO.KEY_BUDGET_ID), anyLong());
        }
    }

    @Test
    public void testObjectToContentValues_normalId() {
        Budget budget = new Budget(7L, "Normal Budget", 50.00f);

        try (org.mockito.MockedConstruction<ContentValues> mocked =
                     mockConstruction(ContentValues.class)) {
            dao.objectToContentValues(budget);

            ContentValues cv = mocked.constructed().get(0);
            verify(cv).put(BudgetDAO.KEY_BUDGET_ID, 7L);
            verify(cv).put("budget_name", "Normal Budget");
            verify(cv).put("starting_balance", 50.00f);
        }
    }
}

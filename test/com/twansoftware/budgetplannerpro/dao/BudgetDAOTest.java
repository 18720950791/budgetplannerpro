package com.twansoftware.budgetplannerpro.dao;

import android.content.Context;
import android.database.Cursor;
import com.twansoftware.budgetplannerpro.entity.Budget;

import java.util.Collections;

/**
 * Automated tests for {@link BudgetDAO} focused on the 64-bit primary-key
 * truncation bug: cursorToObject previously read the id with cursor.getInt(0),
 * which silently narrows ids larger than Integer.MAX_VALUE to 32 bits, causing
 * the wrong record to be loaded / updated / deleted.
 *
 * The tests run against faithful in-memory Android fakes (see the test/android
 * package) and exercise the real DAO end to end. They are intentionally free of
 * any third-party test framework so they can run with nothing but javac + java:
 *
 *   javac -d build/test-classes \
 *       src/com/twansoftware/budgetplannerpro/entity/Budget.java \
 *       src/com/twansoftware/budgetplannerpro/entity/Credit.java \
 *       src/com/twansoftware/budgetplannerpro/entity/Debit.java \
 *       src/com/twansoftware/budgetplannerpro/iface/Transaction.java \
 *       src/com/twansoftware/budgetplannerpro/iface/CursorConvertable.java \
 *       src/com/twansoftware/budgetplannerpro/dao/BudgetDAO.java \
 *       test/android/content/Context.java \
 *       test/android/content/ContentValues.java \
 *       test/android/database/Cursor.java \
 *       test/android/database/sqlite/SQLiteDatabase.java \
 *       test/android/database/sqlite/SQLiteOpenHelper.java \
 *       test/roboguice/inject/ContextSingleton.java \
 *       test/roboguice/util/Ln.java \
 *       test/javax/inject/Inject.java \
 *       test/com/twansoftware/budgetplannerpro/dao/BudgetDAOTest.java
 *   java -cp build/test-classes com.twansoftware.budgetplannerpro.dao.BudgetDAOTest
 */
public class BudgetDAOTest {

    /** Larger than Integer.MAX_VALUE (2,147,483,647); narrows to a negative int if truncated. */
    private static final long BIG_ID = 4_000_000_000L;

    private static int passed = 0;
    private static int failed = 0;

    public static void main(final String[] args) {
        testNormalIdRoundTrip();
        testAutoIncrementInsertRoundTrip();
        testLargeIdBeyondIntMaxRoundTrip();
        testQueryByLargeIdReturnsCorrectRecord();
        testDeleteByLargeIdRemovesOnlyThatRecord();
        testCursorToObjectDoesNotTruncateLargeId();

        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testNormalIdRoundTrip() {
        final BudgetDAO dao = new BudgetDAO(new Context());
        final Budget saved = dao.saveBudget(new Budget(42L, "Groceries", 100.0f));
        assertNotNull("saveBudget returns budget for normal id", saved.getBudgetId());
        assertEquals("saveBudget keeps explicit normal id", 42L, saved.getBudgetId());

        final Budget loaded = dao.loadBudgetById(42L);
        assertNotNull("loadBudgetById finds normal id", loaded);
        assertEquals("normal id round-trips", 42L, loaded.getBudgetId());
        assertEqualsObj("normal record name round-trips", "Groceries", loaded.getName());
    }

    private static void testAutoIncrementInsertRoundTrip() {
        final BudgetDAO dao = new BudgetDAO(new Context());
        final Budget saved = dao.saveBudget(new Budget("NoExplicitId", 5.0f));
        assertNotNull("autoincrement assigns an id", saved.getBudgetId());
        assertTrue("autoincrement id is positive", saved.getBudgetId() > 0);

        final Budget loaded = dao.loadBudgetById(saved.getBudgetId());
        assertNotNull("autoincrement id can be loaded", loaded);
        assertEquals("autoincrement id round-trips", saved.getBudgetId(), loaded.getBudgetId());
    }

    private static void testLargeIdBeyondIntMaxRoundTrip() {
        assertTrue("precondition: BIG_ID exceeds Integer.MAX_VALUE", BIG_ID > Integer.MAX_VALUE);
        // Sanity check that this id is exactly the kind that the old getInt(0) mangled.
        assertTrue("precondition: BIG_ID truncates to a different int", ((int) BIG_ID) != BIG_ID);

        final BudgetDAO dao = new BudgetDAO(new Context());
        dao.saveBudget(new Budget(BIG_ID, "BigBudget", 999.0f));

        final Budget loaded = dao.loadBudgetById(BIG_ID);
        assertNotNull("large id can be loaded", loaded);
        assertEquals("large id is not truncated on read", BIG_ID, loaded.getBudgetId());
        assertEqualsObj("large record name round-trips", "BigBudget", loaded.getName());
    }

    private static void testQueryByLargeIdReturnsCorrectRecord() {
        final long otherBig = 5_000_000_000L;
        final BudgetDAO dao = new BudgetDAO(new Context());
        dao.saveBudget(new Budget(otherBig, "QueryBig", 1.0f));
        dao.saveBudget(new Budget(7L, "SmallNeighbour", 2.0f));

        final Budget loaded = dao.loadBudgetById(otherBig);
        assertNotNull("query by large id finds a record", loaded);
        assertEquals("query by large id returns that exact id", otherBig, loaded.getBudgetId());
        assertEqualsObj("query by large id returns the right row", "QueryBig", loaded.getName());
    }

    private static void testDeleteByLargeIdRemovesOnlyThatRecord() {
        final long bigToDelete = 6_000_000_000L;
        final BudgetDAO dao = new BudgetDAO(new Context());
        dao.saveBudget(new Budget(bigToDelete, "ToDelete", 3.0f));
        dao.saveBudget(new Budget(8L, "Keep", 4.0f));
        assertNotNull("large id exists before delete", dao.loadBudgetById(bigToDelete));

        dao.deleteBudgetById(bigToDelete);

        assertNull("large id is gone after delete", dao.loadBudgetById(bigToDelete));
        assertNotNull("delete by large id leaves other records intact", dao.loadBudgetById(8L));
    }

    private static void testCursorToObjectDoesNotTruncateLargeId() {
        final Cursor cursor = new Cursor(Collections.singletonList(
                new Object[]{BIG_ID, "DirectCursor", 12.5f}));
        assertTrue("cursor positioned", cursor.moveToFirst());

        final BudgetDAO dao = new BudgetDAO(new Context());
        final Budget budget = dao.cursorToObject(cursor);
        assertEquals("cursorToObject reads the full 64-bit id", BIG_ID, budget.getBudgetId());
        assertEqualsObj("cursorToObject reads the name", "DirectCursor", budget.getName());
    }

    // --- tiny assertion helpers -------------------------------------------------

    private static void assertEquals(final String message, final long expected, final long actual) {
        if (expected == actual) {
            pass(message);
        } else {
            fail(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    private static void assertEqualsObj(final String message, final Object expected, final Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass(message);
        } else {
            fail(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    private static void assertTrue(final String message, final boolean condition) {
        if (condition) {
            pass(message);
        } else {
            fail(message + " (expected true)");
        }
    }

    private static void assertNotNull(final String message, final Object value) {
        if (value != null) {
            pass(message);
        } else {
            fail(message + " (expected non-null)");
        }
    }

    private static void assertNull(final String message, final Object value) {
        if (value == null) {
            pass(message);
        } else {
            fail(message + " (expected null, actual=" + value + ")");
        }
    }

    private static void pass(final String message) {
        passed++;
        System.out.println("[PASS] " + message);
    }

    private static void fail(final String message) {
        failed++;
        System.out.println("[FAIL] " + message);
    }
}

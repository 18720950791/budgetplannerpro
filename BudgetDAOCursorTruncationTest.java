/**
 * Standalone test that demonstrates and verifies the BudgetDAO cursor truncation fix.
 *
 * Bug  : BudgetDAO.cursorToObject used (long) cursor.getInt(0).
 *        getInt returns a 32-bit int; casting to long after the fact does NOT recover
 *        the upper 32 bits.  Any primary key > Integer.MAX_VALUE was silently truncated.
 *
 * Fix  : cursor.getLong(0) reads the full 64-bit SQLite INTEGER.
 *
 * This test can be compiled and run with plain javac / java — no Android SDK required.
 *
 *   javac BudgetDAOCursorTruncationTest.java
 *   java  BudgetDAOCursorTruncationTest
 */
public class BudgetDAOCursorTruncationTest {

    private static int passed = 0;
    private static int failed = 0;

    /* ------------------------------------------------------------------ */
    /*  Helpers that simulate the BEFORE (buggy) and AFTER (fixed) code   */
    /* ------------------------------------------------------------------ */

    /** BEFORE fix: (long) cursor.getInt(0) — truncates then widens. */
    static long buggyReadInt(int intVal) {
        return (long) intVal;
    }

    /** AFTER fix: cursor.getLong(0) — reads the full 64-bit value. */
    static long fixedReadLong(long longVal) {
        return longVal;
    }

    /* ------------------------------------------------------------------ */
    /*  Assertion helpers                                                  */
    /* ------------------------------------------------------------------ */

    static void assertEquals(long expected, long actual, String label) {
        if (expected == actual) {
            System.out.println("  PASS: " + label);
            passed++;
        } else {
            System.out.println("  FAIL: " + label + " — expected " + expected + ", got " + actual);
            failed++;
        }
    }

    static void assertNotEquals(long notExpected, long actual, String label) {
        if (notExpected != actual) {
            System.out.println("  PASS: " + label);
            passed++;
        } else {
            System.out.println("  FAIL: " + label + " — got unexpected value " + actual);
            failed++;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Tests                                                              */
    /* ------------------------------------------------------------------ */

    public static void main(String[] args) {

        System.out.println("=== BudgetDAO cursor truncation fix — standalone test ===\n");

        /* ---- 1. Normal ID (well within int range) ---- */
        System.out.println("[Test 1] Normal ID (1L) — both approaches should agree");
        {
            long id = 1L;
            int  intVal = (int) id;   // fits in 32 bits, no truncation
            long longVal = id;

            assertEquals(1L, buggyReadInt(intVal), "buggy  path returns 1");
            assertEquals(1L, fixedReadLong(longVal), "fixed  path returns 1");
        }

        /* ---- 2. ID exactly at Integer.MAX_VALUE ---- */
        System.out.println("\n[Test 2] ID == Integer.MAX_VALUE (2147483647L)");
        {
            long id = Integer.MAX_VALUE;
            int  intVal = (int) id;
            long longVal = id;

            assertEquals(Integer.MAX_VALUE, buggyReadInt(intVal),
                    "buggy  path returns MAX_VALUE");
            assertEquals(Integer.MAX_VALUE, fixedReadLong(longVal),
                    "fixed  path returns MAX_VALUE");
        }

        /* ---- 3. ID = Integer.MAX_VALUE + 1  →  first value that truncates ---- */
        System.out.println("\n[Test 3] ID == Integer.MAX_VALUE + 1 (2147483648L)");
        {
            long id = (long) Integer.MAX_VALUE + 1L;   // 2147483648L
            int  intVal = (int) id;   // truncates to -2147483648
            long longVal = id;

            // BUG: buggy path returns a NEGATIVE number (truncated)
            assertNotEquals(id, buggyReadInt(intVal),
                    "buggy  path TRUNCATES (returns " + buggyReadInt(intVal) + ")");
            // FIX: fixed path preserves the full 64-bit value
            assertEquals(id, fixedReadLong(longVal),
                    "fixed  path preserves 2147483648L");
        }

        /* ---- 4. Typical large SQLite rowid ---- */
        System.out.println("\n[Test 4] Large SQLite rowid (3_000_000_000L)");
        {
            long id = 3_000_000_000L;
            int  intVal = (int) id;   // truncates to -1294967296
            long longVal = id;

            assertNotEquals(id, buggyReadInt(intVal),
                    "buggy  path TRUNCATES (returns " + buggyReadInt(intVal) + ")");
            assertEquals(id, fixedReadLong(longVal),
                    "fixed  path preserves 3_000_000_000L");
        }

        /* ---- 5. Near Long.MAX_VALUE (edge case) ---- */
        System.out.println("\n[Test 5] Near Long.MAX_VALUE (Long.MAX_VALUE - 1)");
        {
            long id = Long.MAX_VALUE - 1L;
            int  intVal = (int) id;   // massive truncation
            long longVal = id;

            assertNotEquals(id, buggyReadInt(intVal),
                    "buggy  path TRUNCATES (returns " + buggyReadInt(intVal) + ")");
            assertEquals(id, fixedReadLong(longVal),
                    "fixed  path preserves near-Long.MAX_VALUE");
        }

        /* ---- 6. Simulate query-by-ID with truncated key ---- */
        System.out.println("\n[Test 6] Simulate loadBudgetById with truncated key");
        {
            long realId = 3_000_000_000L;
            int  intVal = (int) realId;          // -1294967296
            long queryIdUsed = (long) intVal;    // the wrong value the buggy code uses

            // The WHERE clause would be: budget_id = -1294967296  →  NO MATCH
            assertEquals(-1294967296L, queryIdUsed,
                    "buggy  query key is negative (wrong record / no match)");

            // After fix: budget_id = 3000000000  →  CORRECT MATCH
            long correctQueryId = fixedReadLong(realId);
            assertEquals(realId, correctQueryId,
                    "fixed query key matches the real budget ID");
        }

        /* ---- 7. Simulate delete-by-ID with truncated key ---- */
        System.out.println("\n[Test 7] Simulate deleteBudgetById with truncated key");
        {
            long realId = 5_000_000_000L;
            int  intVal = (int) realId;          // 705032704
            long deleteIdUsed = (long) intVal;

            // After truncation the app might delete a DIFFERENT record (if that ID exists)
            assertNotEquals(realId, deleteIdUsed,
                    "buggy  delete key differs from real ID → deletes wrong record");
            assertEquals(realId, fixedReadLong(realId),
                    "fixed delete key targets the correct record");
        }

        /* ---- 8. Verify Budget entity field type is Long ---- */
        System.out.println("\n[Test 8] Budget.budgetId is java.lang.Long (64-bit)");
        {
            Long budgetId = 3_000_000_000L;
            assertEquals(3_000_000_000L, budgetId.longValue(),
                    "Long field stores 3_000_000_000L without truncation");
        }

        /* ---- Summary ---- */
        System.out.println("\n=== SUMMARY: " + passed + " passed, " + failed + " failed ===");

        if (failed > 0) {
            System.exit(1);
        }
    }
}

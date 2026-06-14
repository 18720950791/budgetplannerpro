package roboguice.util;

/** Test-only stand-in for the roboguice logger. */
public class Ln {
    public static void d(final String message, final Object... args) {
        // no-op for tests
    }
}

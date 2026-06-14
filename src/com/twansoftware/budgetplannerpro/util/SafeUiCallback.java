package com.twansoftware.budgetplannerpro.util;

/**
 * Guards UI updates from background threads against stale Fragment views.
 *
 * <p>When a background thread loads data and posts the result to the UI thread,
 * the Fragment's view may already have been destroyed (e.g., due to device
 * rotation, back-navigation, or Fragment detachment). Applying the update to
 * the stale view can cause {@code NullPointerException}, dirty data, or
 * crashes.</p>
 *
 * <p>This utility pairs with a {@code volatile int viewGeneration} field in
 * each Fragment. The generation is <b>captured</b> when a Runnable is created
 * via {@link #uiCallback} and <b>re-checked</b> when the Runnable actually
 * executes on the UI thread. If the values differ, the update is silently
 * skipped.</p>
 *
 * <h3>Usage in a Fragment</h3>
 * <pre>
 * // Field
 * private volatile int viewGeneration = 0;
 *
 * // Increment on every view creation
 * {@code @Override}
 * public void onViewCreated(View view, Bundle savedInstanceState) {
 *     super.onViewCreated(view, savedInstanceState);
 *     viewGeneration++;
 * }
 *
 * // Bump generation and interrupt threads on view destroy
 * {@code @Override}
 * public void onDestroyView() {
 *     viewGeneration++;
 *     if (loadThread != null) loadThread.interrupt();
 *     super.onDestroyView();
 * }
 *
 * // Wrap UI updates (capture generation BEFORE starting the thread)
 * final int gen = viewGeneration;
 * new Thread(new Runnable() {
 *     public void run() {
 *         final List&lt;Item&gt; data = service.loadData();
 *         activity.runOnUiThread(SafeUiCallback.uiCallback(gen,
 *                 new SafeUiCallback.GenerationProvider() {
 *                     public int getViewGeneration() { return viewGeneration; }
 *                 },
 *                 new Runnable() {
 *                     public void run() { adapter.updateWith(data); }
 *                 }));
 *     }
 * }).start();
 * </pre>
 */
public final class SafeUiCallback {

    private SafeUiCallback() {
        // utility class
    }

    /**
     * Provides the Fragment's current view generation at callback execution
     * time.
     */
    public interface GenerationProvider {
        int getViewGeneration();
    }

    /**
     * Wraps {@code uiUpdate} in a Runnable that only executes when the
     * captured generation still matches the Fragment's live generation.
     *
     * <p>Call this <b>before</b> starting the background thread so that the
     * current generation value is captured on the UI thread.</p>
     *
     * @param capturedGeneration the {@code viewGeneration} value at the
     *                           moment the background task is started
     * @param provider           a callback that returns the Fragment's live
     *                           {@code viewGeneration} when the Runnable
     *                           executes
     * @param uiUpdate           the UI update logic to run if the view is
     *                           still valid
     * @return a Runnable suitable for {@code Activity.runOnUiThread()}
     */
    public static Runnable uiCallback(
            final int capturedGeneration,
            final GenerationProvider provider,
            final Runnable uiUpdate) {
        return new Runnable() {
            @Override
            public void run() {
                if (provider.getViewGeneration() == capturedGeneration
                        && uiUpdate != null) {
                    uiUpdate.run();
                }
            }
        };
    }
}

package com.twansoftware.budgetplannerpro.util;

/**
 * Tracks the view lifecycle of a {@code Fragment} so that results produced by a
 * background task are only delivered to the UI while the view that started the
 * task is still alive.
 *
 * <p>The fragments in this app load data on a background {@code Thread} and then
 * update the UI when the work finishes. If the fragment has been detached or its
 * view has been destroyed/recreated (for example after a screen rotation or a
 * back navigation) by the time the task completes, applying the result would
 * either throw a {@code NullPointerException}, update a stale {@code Adapter}, or
 * incorrectly toggle the host activity's progress bar.</p>
 *
 * <p>Each time a fragment's view becomes ready it calls {@link #onViewActive()},
 * which marks the guard active and hands back a token identifying that particular
 * view incarnation. A task captures the token when it starts and, on completion,
 * asks {@link #shouldDeliver(int)} whether the result is still relevant. A result
 * is delivered only when the view is still active <em>and</em> the token matches
 * the current view incarnation, so results from a destroyed view are ignored and
 * results from a previous incarnation never overwrite a newly created view.</p>
 *
 * <p>This class is intentionally free of any Android dependency so its behaviour
 * can be unit tested on a plain JVM.</p>
 */
public class FragmentTaskGuard {

    private boolean viewActive;

    private int generation;

    /**
     * Marks the current view as active and advances to a new view incarnation.
     *
     * @return the token that tasks started against this view should capture
     */
    public int onViewActive() {
        viewActive = true;
        generation++;
        return generation;
    }

    /**
     * @return the token of the current view incarnation, for tasks started after
     *         {@link #onViewActive()} (for example a reload triggered by the user)
     */
    public int currentToken() {
        return generation;
    }

    /**
     * Marks the current view as destroyed. Results captured against any previous
     * token are no longer delivered until {@link #onViewActive()} is called again.
     */
    public void onViewDestroyed() {
        viewActive = false;
    }

    /**
     * Decides whether a result tagged with {@code token} may update the UI.
     *
     * @param token the token captured when the task started
     * @return {@code true} only when the view is still active and {@code token}
     *         identifies the current view incarnation
     */
    public boolean shouldDeliver(final int token) {
        return viewActive && token == generation;
    }

    /**
     * @return whether a view is currently active
     */
    public boolean isViewActive() {
        return viewActive;
    }
}

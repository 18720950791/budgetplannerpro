package com.twansoftware.budgetplannerpro.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Lifecycle tests for {@link FragmentTaskGuard}.
 *
 * <p>Each test mirrors what one of the budget fragments does: the view becomes
 * active and a background task captures a token; some lifecycle event may happen
 * (detach, rotation) before the task finishes; finally the task asks the guard
 * whether its result may still touch the UI.</p>
 */
public class FragmentTaskGuardTest {

    private FragmentTaskGuard guard;

    @Before
    public void setUp() {
        guard = new FragmentTaskGuard();
    }

    /** Normal completion: the view that started the task is still showing. */
    @Test
    public void deliversResultWhenViewStillActive() {
        final int token = guard.onViewActive();

        // ...background work runs, then completes while the same view is shown.
        assertTrue(guard.isViewActive());
        assertTrue("a result must be delivered to the live view", guard.shouldDeliver(token));
    }

    /** Task finishes after the fragment has been detached / its view destroyed. */
    @Test
    public void dropsResultWhenViewDestroyedDuringTask() {
        final int token = guard.onViewActive();

        // The user navigates back (or the fragment is detached) before the
        // background work finishes.
        guard.onViewDestroyed();

        assertFalse(guard.isViewActive());
        assertFalse("a result must not touch a destroyed view", guard.shouldDeliver(token));
    }

    /**
     * Rotation while a task is in flight: the old view is destroyed and a new
     * view is created. The stale result must not overwrite the new view, while a
     * task started by the new view is delivered normally.
     */
    @Test
    public void dropsStaleResultButDeliversNewAfterRotation() {
        final int tokenBeforeRotation = guard.onViewActive();

        // Rotation tears down the old view and builds a fresh one.
        guard.onViewDestroyed();
        final int tokenAfterRotation = guard.onViewActive();

        assertNotEquals(tokenBeforeRotation, tokenAfterRotation);
        assertFalse("the pre-rotation result must be ignored",
                guard.shouldDeliver(tokenBeforeRotation));
        assertTrue("the post-rotation result must be delivered",
                guard.shouldDeliver(tokenAfterRotation));
    }

    /**
     * Even if a view-destroyed callback is missed and the view is simply
     * recreated, an old token never matches the new view incarnation.
     */
    @Test
    public void dropsStaleResultWhenViewRecreatedWithoutDestroyCallback() {
        final int oldToken = guard.onViewActive();
        final int newToken = guard.onViewActive();

        assertFalse(guard.shouldDeliver(oldToken));
        assertTrue(guard.shouldDeliver(newToken));
    }

    /** A reload triggered within the same view (e.g. after returning a result). */
    @Test
    public void deliversReloadStartedWithinSameView() {
        guard.onViewActive();

        // A later task started against the live view uses the current token.
        final int reloadToken = guard.currentToken();

        assertTrue(guard.shouldDeliver(reloadToken));
    }

    /** Before any view is active, nothing may be delivered. */
    @Test
    public void dropsResultBeforeAnyViewIsActive() {
        assertFalse(guard.isViewActive());
        assertFalse(guard.shouldDeliver(guard.currentToken()));
    }

    /** The token returned by activation is the one reported as current. */
    @Test
    public void activationTokenMatchesCurrentToken() {
        final int token = guard.onViewActive();
        assertEquals(token, guard.currentToken());
    }
}

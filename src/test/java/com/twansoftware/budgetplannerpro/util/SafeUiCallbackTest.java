package com.twansoftware.budgetplannerpro.util;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests for {@link SafeUiCallback}, verifying that UI update callbacks
 * are properly guarded against stale Fragment views.
 *
 * <p>These tests simulate the lifecycle scenarios that cause crashes in
 * the budget app:
 * <ul>
 *   <li>Normal completion: callback executes when view is still valid</li>
 *   <li>Detach during task: callback is skipped when fragment detaches</li>
 *   <li>Rotation during task: callback is skipped when view is recreated</li>
 *   <li>Null callback: no-op when the uiUpdate Runnable is null</li>
 * </ul>
 */
public class SafeUiCallbackTest {

    /**
     * Simulates a Fragment's viewGeneration field for testing.
     */
    private static class FakeGenerationProvider implements SafeUiCallback.GenerationProvider {
        private final AtomicInteger generation;

        FakeGenerationProvider(int initial) {
            this.generation = new AtomicInteger(initial);
        }

        @Override
        public int getViewGeneration() {
            return generation.get();
        }

        void setGeneration(int value) {
            generation.set(value);
        }

        void incrementGeneration() {
            generation.incrementAndGet();
        }
    }

    // ---------------------------------------------------------------
    // Normal completion: callback should execute
    // ---------------------------------------------------------------

    @Test
    public void testCallbackExecutesWhenGenerationMatches() {
        FakeGenerationProvider provider = new FakeGenerationProvider(1);
        AtomicBoolean executed = new AtomicBoolean(false);

        Runnable callback = SafeUiCallback.uiCallback(1, provider, new Runnable() {
            @Override
            public void run() {
                executed.set(true);
            }
        });
        callback.run();

        assertTrue("Callback should execute when generation matches", executed.get());
    }

    @Test
    public void testCallbackExecutesWithZeroGeneration() {
        FakeGenerationProvider provider = new FakeGenerationProvider(0);
        AtomicBoolean executed = new AtomicBoolean(false);

        Runnable callback = SafeUiCallback.uiCallback(0, provider, new Runnable() {
            @Override
            public void run() {
                executed.set(true);
            }
        });
        callback.run();

        assertTrue("Callback should execute when both generations are 0", executed.get());
    }

    // ---------------------------------------------------------------
    // Rotation scenario: callback should be skipped
    // ---------------------------------------------------------------

    @Test
    public void testCallbackSkippedAfterRotation() {
        // Simulate: onViewCreated sets generation to 1
        FakeGenerationProvider provider = new FakeGenerationProvider(1);
        int capturedGen = 1;
        AtomicBoolean executed = new AtomicBoolean(false);

        Runnable callback = SafeUiCallback.uiCallback(capturedGen, provider, new Runnable() {
            @Override
            public void run() {
                executed.set(true);
            }
        });

        // Simulate rotation: onDestroyView increments generation to 2
        provider.incrementGeneration();
        // onViewCreated increments again to 3
        provider.incrementGeneration();

        // Old callback runs after rotation
        callback.run();

        assertFalse("Callback should be skipped after rotation changes generation", executed.get());
    }

    @Test
    public void testCallbackSkippedAfterSingleRotation() {
        FakeGenerationProvider provider = new FakeGenerationProvider(1);
        int capturedGen = 1;
        AtomicBoolean executed = new AtomicBoolean(false);

        Runnable callback = SafeUiCallback.uiCallback(capturedGen, provider, new Runnable() {
            @Override
            public void run() {
                executed.set(true);
            }
        });

        // Single rotation: onDestroyView bumps to 2, onViewCreated bumps to 3
        provider.setGeneration(3);
        callback.run();

        assertFalse("Callback should be skipped when generation changed due to rotation",
                executed.get());
    }

    // ---------------------------------------------------------------
    // Detach scenario: callback should be skipped
    // ---------------------------------------------------------------

    @Test
    public void testCallbackSkippedAfterDetach() {
        FakeGenerationProvider provider = new FakeGenerationProvider(1);
        int capturedGen = 1;
        AtomicBoolean executed = new AtomicBoolean(false);

        Runnable callback = SafeUiCallback.uiCallback(capturedGen, provider, new Runnable() {
            @Override
            public void run() {
                executed.set(true);
            }
        });

        // Simulate detach: onDestroyView bumps generation
        provider.incrementGeneration();

        callback.run();

        assertFalse("Callback should be skipped after fragment detach", executed.get());
    }

    // ---------------------------------------------------------------
    // New callback after rotation: should execute
    // ---------------------------------------------------------------

    @Test
    public void testNewCallbackAfterRotationExecutes() {
        // Simulate: after rotation, new view is created with generation 3
        FakeGenerationProvider provider = new FakeGenerationProvider(3);
        int capturedGen = 3;
        AtomicBoolean executed = new AtomicBoolean(false);

        Runnable callback = SafeUiCallback.uiCallback(capturedGen, provider, new Runnable() {
            @Override
            public void run() {
                executed.set(true);
            }
        });
        callback.run();

        assertTrue("New callback after rotation should execute", executed.get());
    }

    // ---------------------------------------------------------------
    // Null safety
    // ---------------------------------------------------------------

    @Test
    public void testNullUiUpdateDoesNotThrow() {
        FakeGenerationProvider provider = new FakeGenerationProvider(1);

        Runnable callback = SafeUiCallback.uiCallback(1, provider, null);
        // Should not throw
        callback.run();
    }

    @Test
    public void testNullUiUpdateSkippedWhenGenerationMismatch() {
        FakeGenerationProvider provider = new FakeGenerationProvider(2);

        Runnable callback = SafeUiCallback.uiCallback(1, provider, null);
        // Should not throw
        callback.run();
    }

    // ---------------------------------------------------------------
    // Thread safety: verify volatile-like behavior
    // ---------------------------------------------------------------

    @Test
    public void testConcurrentGenerationChange() throws InterruptedException {
        final FakeGenerationProvider provider = new FakeGenerationProvider(1);
        final int capturedGen = 1;
        final AtomicBoolean executed = new AtomicBoolean(false);

        final Runnable callback = SafeUiCallback.uiCallback(capturedGen, provider, new Runnable() {
            @Override
            public void run() {
                executed.set(true);
            }
        });

        // Simulate another thread changing generation (e.g., rotation on UI thread)
        Thread rotationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                provider.incrementGeneration();
                provider.incrementGeneration();
            }
        });
        rotationThread.start();
        rotationThread.join();

        // Now the callback runs on "UI thread"
        callback.run();

        assertFalse("Callback should be skipped when generation was changed by another thread",
                executed.get());
    }

    // ---------------------------------------------------------------
    // Multiple rapid rotations
    // ---------------------------------------------------------------

    @Test
    public void testMultipleRapidRotations() {
        FakeGenerationProvider provider = new FakeGenerationProvider(1);
        AtomicBoolean callback1Executed = new AtomicBoolean(false);
        AtomicBoolean callback2Executed = new AtomicBoolean(false);
        AtomicBoolean callback3Executed = new AtomicBoolean(false);

        // Capture generation for first task
        int gen1 = 1;
        Runnable callback1 = SafeUiCallback.uiCallback(gen1, provider, new Runnable() {
            @Override
            public void run() {
                callback1Executed.set(true);
            }
        });

        // First rotation
        provider.setGeneration(3);
        int gen2 = 3;
        Runnable callback2 = SafeUiCallback.uiCallback(gen2, provider, new Runnable() {
            @Override
            public void run() {
                callback2Executed.set(true);
            }
        });

        // Second rotation
        provider.setGeneration(5);
        int gen3 = 5;
        Runnable callback3 = SafeUiCallback.uiCallback(gen3, provider, new Runnable() {
            @Override
            public void run() {
                callback3Executed.set(true);
            }
        });

        // All callbacks fire in order
        callback1.run();
        callback2.run();
        callback3.run();

        assertFalse("First callback should be skipped (stale generation)", callback1Executed.get());
        assertFalse("Second callback should be skipped (stale generation)", callback2Executed.get());
        assertTrue("Third (current) callback should execute", callback3Executed.get());
    }
}

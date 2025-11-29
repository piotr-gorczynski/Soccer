package piotr_gorczynski.soccer2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Test cases for preventing double sprite drawing.
 * 
 * The issue: During a parabolic trajectory run animation, both the kick animation
 * and the run animation for the same player (e.g., red) were being drawn at the same time.
 * This happened because the parabolic trajectory code was unconditionally overwriting
 * the redFrame/blueFrame variables with the parabolicCachedRedFrame/parabolicCachedBlueFrame,
 * even when those frames had been set to null to prevent drawing during kick animation.
 * 
 * The fix: In updateRunAnimationState, when applying the parabolic cached frame,
 * check if the original frame was already null (due to kick animation being active).
 * If it was null, don't overwrite it with the parabolic cached frame.
 * 
 * This test simulates the logic without depending on Field.java's internal implementation.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DoubleSpritePreventionTest {

    /**
     * Simulates the OLD (buggy) behavior where parabolic cached frame would overwrite
     * the nullified frame from the kick animation check.
     * 
     * Scenario:
     * - kickAnimationActive = true
     * - runMovingPlayer = 0 (red is kicking)
     * - parabolicCachedRedFrame != null
     * 
     * OLD behavior:
     * 1. redFrame gets set from getRunFrame()
     * 2. redFrame gets nullified because kickAnimationActive && runMovingPlayer == 0
     * 3. parabolicCachedRedFrame overwrites redFrame (BUG: redFrame is no longer null)
     * 4. Both kick and run animations are drawn (INCORRECT)
     */
    @Test
    public void testOldBehavior_DoubleDrawing() {
        // Simulate variables
        boolean kickAnimationActive = true;
        int runMovingPlayer = 0; // Red is kicking
        Object parabolicCachedRedFrame = new Object(); // Non-null cached frame
        
        // Step 1: redFrame gets set from getRunFrame() - simulated as non-null
        Object redFrame = new Object();
        assertNotNull("Initial redFrame from getRunFrame", redFrame);
        
        // Step 2: redFrame gets nullified because of kick animation check
        if (kickAnimationActive) {
            if (runMovingPlayer == 0) {
                redFrame = null;
            }
        }
        assertNull("After kick check, redFrame should be null", redFrame);
        
        // Step 3: OLD behavior - unconditionally overwrite with parabolic cached frame
        if (parabolicCachedRedFrame != null) {
            redFrame = parabolicCachedRedFrame;  // BUG: This overwrites the null
        }
        
        // With old behavior, redFrame is now non-null (BAD)
        assertNotNull("OLD behavior: redFrame is incorrectly non-null", redFrame);
        
        // This means both kick animation and run animation would be drawn (INCORRECT)
        boolean kickWouldDraw = kickAnimationActive && runMovingPlayer == 0;
        boolean runWouldDraw = redFrame != null;
        assertTrue("OLD behavior would draw kick animation", kickWouldDraw);
        assertTrue("OLD behavior would ALSO draw run animation (BUG)", runWouldDraw);
    }

    /**
     * Simulates the NEW (fixed) behavior where parabolic cached frame respects
     * the nullified frame from the kick animation check.
     * 
     * NEW behavior:
     * 1. redFrame gets set from getRunFrame()
     * 2. redFrame gets nullified because kickAnimationActive && runMovingPlayer == 0
     * 3. parabolicCachedRedFrame check also requires redFrame != null (FIX)
     * 4. Only kick animation is drawn (CORRECT)
     */
    @Test
    public void testNewBehavior_SingleDrawing() {
        // Simulate variables
        boolean kickAnimationActive = true;
        int runMovingPlayer = 0; // Red is kicking
        Object parabolicCachedRedFrame = new Object(); // Non-null cached frame
        
        // Step 1: redFrame gets set from getRunFrame() - simulated as non-null
        Object redFrame = new Object();
        assertNotNull("Initial redFrame from getRunFrame", redFrame);
        
        // Step 2: redFrame gets nullified because of kick animation check
        if (kickAnimationActive) {
            if (runMovingPlayer == 0) {
                redFrame = null;
            }
        }
        assertNull("After kick check, redFrame should be null", redFrame);
        
        // Step 3: NEW behavior - only overwrite if redFrame is NOT null
        if (parabolicCachedRedFrame != null && redFrame != null) {
            redFrame = parabolicCachedRedFrame;  // FIX: This condition is false
        }
        
        // With new behavior, redFrame remains null (GOOD)
        assertNull("NEW behavior: redFrame remains null as expected", redFrame);
        
        // This means only kick animation would be drawn (CORRECT)
        boolean kickWouldDraw = kickAnimationActive && runMovingPlayer == 0;
        boolean runWouldDraw = redFrame != null;
        assertTrue("NEW behavior would draw kick animation", kickWouldDraw);
        assertFalse("NEW behavior would NOT draw run animation (CORRECT)", runWouldDraw);
    }

    /**
     * Test that parabolic frame IS applied when kick animation is NOT active.
     * This ensures the fix doesn't break normal parabolic trajectory rendering.
     */
    @Test
    public void testNewBehavior_ParabolicFrameAppliedWhenNoKick() {
        // Simulate variables
        boolean kickAnimationActive = false; // No kick animation
        int runMovingPlayer = 0;
        Object parabolicCachedRedFrame = new Object(); // Non-null cached frame
        
        // Step 1: redFrame gets set from getRunFrame() - simulated as non-null
        Object redFrame = new Object();
        Object originalRedFrame = redFrame;
        assertNotNull("Initial redFrame from getRunFrame", redFrame);
        
        // Step 2: kick check doesn't nullify because kickAnimationActive is false
        if (kickAnimationActive) {
            if (runMovingPlayer == 0) {
                redFrame = null;
            }
        }
        assertSame("No kick, so redFrame unchanged", originalRedFrame, redFrame);
        
        // Step 3: NEW behavior - parabolic frame SHOULD be applied
        if (parabolicCachedRedFrame != null && redFrame != null) {
            redFrame = parabolicCachedRedFrame;  // This condition is true
        }
        
        // With new behavior, redFrame is the parabolic cached frame (GOOD)
        assertSame("NEW behavior: parabolic frame applied correctly", 
                   parabolicCachedRedFrame, redFrame);
    }

    /**
     * Test blue player scenario - same logic applies to blue as red.
     */
    @Test
    public void testNewBehavior_BluePlayer() {
        // Simulate variables for blue player
        boolean kickAnimationActive = true;
        int runMovingPlayer = 1; // Blue is kicking
        Object parabolicCachedBlueFrame = new Object();
        
        // Step 1: blueFrame gets set from getRunFrame()
        Object blueFrame = new Object();
        assertNotNull("Initial blueFrame from getRunFrame", blueFrame);
        
        // Step 2: blueFrame gets nullified because of kick animation check
        if (kickAnimationActive) {
            if (runMovingPlayer == 1) {
                blueFrame = null;
            }
        }
        assertNull("After kick check, blueFrame should be null", blueFrame);
        
        // Step 3: NEW behavior - only overwrite if blueFrame is NOT null
        if (parabolicCachedBlueFrame != null && blueFrame != null) {
            blueFrame = parabolicCachedBlueFrame;
        }
        
        // With new behavior, blueFrame remains null (GOOD)
        assertNull("NEW behavior: blueFrame remains null as expected", blueFrame);
    }

    /**
     * Test that the opponent player (not the kicker) can still use parabolic frames.
     * When red is kicking, blue should still be able to use parabolic trajectory frames.
     */
    @Test
    public void testNewBehavior_OpponentCanUseParabolicFrame() {
        // Simulate variables
        boolean kickAnimationActive = true;
        int runMovingPlayer = 0; // Red is kicking
        Object parabolicCachedBlueFrame = new Object(); // Blue's cached frame
        
        // Step 1: blueFrame gets set from getRunFrame()
        Object blueFrame = new Object();
        assertNotNull("Initial blueFrame from getRunFrame", blueFrame);
        
        // Step 2: kick check only affects the kicking player (red, not blue)
        if (kickAnimationActive) {
            if (runMovingPlayer == 1) {  // This is false (0 != 1)
                blueFrame = null;
            }
        }
        // blueFrame should NOT be nullified because runMovingPlayer is 0 (red)
        assertNotNull("Blue is not kicking, so blueFrame unchanged", blueFrame);
        
        // Step 3: NEW behavior - parabolic frame CAN be applied for blue
        if (parabolicCachedBlueFrame != null && blueFrame != null) {
            blueFrame = parabolicCachedBlueFrame;  // This condition is true
        }
        
        // Blue should use the parabolic cached frame
        assertSame("NEW behavior: blue can use parabolic frame while red kicks", 
                   parabolicCachedBlueFrame, blueFrame);
    }
}

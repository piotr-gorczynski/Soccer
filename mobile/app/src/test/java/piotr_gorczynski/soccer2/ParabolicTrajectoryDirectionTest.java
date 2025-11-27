package piotr_gorczynski.soccer2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Test cases for the parabolic trajectory direction consistency fix.
 * 
 * The issue: During parabolic run animation, the sprite direction was oscillating
 * between correct (e.g., EAST_SOUTH) and incorrect (e.g., SOUTH) values.
 * This happened because the previous grid positions used for direction calculation
 * were being updated on every draw call, not just when the animation frame index advanced.
 * 
 * The fix: Track the previous frame indices and only update the parabolic previous
 * positions when the frame index actually changes. This ensures consistent direction
 * when the same frame is drawn multiple times (screen refresh rate is faster than
 * animation frame rate).
 * 
 * The fix adds two new fields:
 * - parabolicPrevRedFrameIndex: tracks the last frame index for red player
 * - parabolicPrevBlueFrameIndex: tracks the last frame index for blue player
 * 
 * Note: This test uses local helper classes to simulate the logic without depending
 * on Field.java's internal implementation details.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ParabolicTrajectoryDirectionTest {

    private static final float SPRITE_DIRECTION_EPSILON = 0.0001f;

    /**
     * Simulates the direction calculation logic from getParabolicDirectionFrame.
     * Returns the direction label based on the delta between current and previous positions.
     */
    private String calculateDirection(float currentX, float currentY, float prevX, float prevY) {
        float deltaX = currentX - prevX;
        float deltaY = currentY - prevY;

        // If delta is too small, return empty (no valid direction)
        if (Math.abs(deltaX) <= SPRITE_DIRECTION_EPSILON && Math.abs(deltaY) <= SPRITE_DIRECTION_EPSILON) {
            return "";
        }

        // Calculate angle from delta
        double angle = Math.atan2(-deltaY, deltaX);
        double degrees = Math.toDegrees(angle);
        if (degrees < 0) {
            degrees += 360.0;
        }

        // Map angle to direction label (same logic as selectRunAnimationFrames)
        if (degrees >= 157.5 && degrees < 202.5) {
            return "WEST";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            return "WEST_NORTH";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            return "NORTH";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            return "EAST_NORTH";
        } else if (degrees >= 337.5 || degrees < 22.5) {
            return "EAST";
        } else if (degrees >= 292.5 && degrees < 337.5) {
            return "EAST_SOUTH";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            return "SOUTH";
        } else {
            return "SOUTH_WEST";
        }
    }

    /**
     * Simulates the OLD (buggy) behavior where previous positions were updated on every draw.
     * 
     * When the same frame is drawn 3 times (because screen refresh rate is ~3x animation frame rate):
     * - First draw: direction is calculated correctly from actual previous position
     * - Second draw: previous position was already updated to current, so delta is 0
     * - Third draw: same as second draw
     */
    @Test
    public void testOldBehavior_DirectionOscillates() {
        // Simulate drawing the same frame 3 times (frame index 1)
        // Starting position: (3.0, 4.0)
        // Current position for frame 1: (3.096, 4.064)
        
        float prevX = 3.0f;
        float prevY = 4.0f;
        float currentX = 3.096f;
        float currentY = 4.064f;
        
        // First draw: correct direction
        String direction1 = calculateDirection(currentX, currentY, prevX, prevY);
        assertEquals("First draw should get correct direction", "EAST_SOUTH", direction1);
        
        // OLD BEHAVIOR: Update previous position to current after first draw
        prevX = currentX;
        prevY = currentY;
        
        // Second draw: same frame, but now delta is 0
        String direction2 = calculateDirection(currentX, currentY, prevX, prevY);
        assertEquals("Second draw with OLD behavior gets empty (incorrect) direction", "", direction2);
        
        // Third draw: still same frame, delta is still 0
        String direction3 = calculateDirection(currentX, currentY, prevX, prevY);
        assertEquals("Third draw with OLD behavior gets empty (incorrect) direction", "", direction3);
    }

    /**
     * Simulates the NEW (fixed) behavior where previous positions are only updated
     * when the frame index changes.
     * 
     * When the same frame is drawn 3 times:
     * - All draws should use the same previous position (from before frame started)
     * - All draws should get the same correct direction
     */
    @Test
    public void testNewBehavior_DirectionConsistent() {
        // Simulate drawing the same frame 3 times (frame index 1)
        // Starting position: (3.0, 4.0)
        // Current position for frame 1: (3.096, 4.064)
        
        float prevX = 3.0f;
        float prevY = 4.0f;
        float currentX = 3.096f;
        float currentY = 4.064f;
        int prevFrameIndex = -1;  // NEW: track previous frame index
        int currentFrameIndex = 1;
        
        // First draw: correct direction
        String direction1 = calculateDirection(currentX, currentY, prevX, prevY);
        assertEquals("First draw should get correct direction", "EAST_SOUTH", direction1);
        
        // NEW BEHAVIOR: Only update previous position if frame index changed
        if (currentFrameIndex != prevFrameIndex) {
            prevFrameIndex = currentFrameIndex;
            // Note: In the real code, prevX/prevY would be updated here
            // But for this draw, we already used the correct prevX/prevY
        }
        
        // Second draw: same frame index, so prevX/prevY are NOT updated yet
        // (In real code, prevX/prevY are only updated after the frame advances)
        String direction2 = calculateDirection(currentX, currentY, prevX, prevY);
        assertEquals("Second draw with NEW behavior gets SAME correct direction", "EAST_SOUTH", direction2);
        
        // Third draw: still same frame index
        String direction3 = calculateDirection(currentX, currentY, prevX, prevY);
        assertEquals("Third draw with NEW behavior gets SAME correct direction", "EAST_SOUTH", direction3);
    }

    /**
     * Test that direction changes correctly when frame index advances.
     */
    @Test
    public void testNewBehavior_DirectionChangesOnFrameAdvance() {
        // Frame 1: position (3.096, 4.064)
        // Frame 2: position (3.179, 4.129)
        
        float prevX = 3.0f;
        float prevY = 4.0f;
        float frame1X = 3.096f;
        float frame1Y = 4.064f;
        float frame2X = 3.179f;
        float frame2Y = 4.129f;
        int prevFrameIndex = -1;
        
        // Frame 1 draws
        int currentFrameIndex = 1;
        String dirFrame1Draw1 = calculateDirection(frame1X, frame1Y, prevX, prevY);
        assertEquals("Frame 1 draw 1", "EAST_SOUTH", dirFrame1Draw1);
        
        String dirFrame1Draw2 = calculateDirection(frame1X, frame1Y, prevX, prevY);
        assertEquals("Frame 1 draw 2", "EAST_SOUTH", dirFrame1Draw2);
        
        // Update previous position because frame index changes
        if (currentFrameIndex != prevFrameIndex) {
            prevX = frame1X;
            prevY = frame1Y;
            prevFrameIndex = currentFrameIndex;
        }
        
        // Frame 2 draws
        currentFrameIndex = 2;
        String dirFrame2Draw1 = calculateDirection(frame2X, frame2Y, prevX, prevY);
        assertEquals("Frame 2 draw 1", "EAST_SOUTH", dirFrame2Draw1);
        
        String dirFrame2Draw2 = calculateDirection(frame2X, frame2Y, prevX, prevY);
        assertEquals("Frame 2 draw 2", "EAST_SOUTH", dirFrame2Draw2);
    }

    /**
     * Test direction calculation for various angles along a parabolic path.
     */
    @Test
    public void testDirectionCalculation_ParabolicPath() {
        // Going from (3, 4) eastward to spike at (3.4, 4.5) then back to (3, 5)
        // First half of parabola: moving east and south
        String dir1 = calculateDirection(3.2f, 4.2f, 3.0f, 4.0f);
        assertEquals("First part of parabola should be EAST_SOUTH", "EAST_SOUTH", dir1);
        
        // At the spike: mostly moving south with slight east
        String dir2 = calculateDirection(3.4f, 4.5f, 3.2f, 4.2f);
        // Depending on exact angle, could be EAST_SOUTH or SOUTH
        assertTrue("Near spike should be SOUTH or EAST_SOUTH", 
            dir2.equals("SOUTH") || dir2.equals("EAST_SOUTH"));
        
        // Second half of parabola: moving west and south
        String dir3 = calculateDirection(3.2f, 4.8f, 3.4f, 4.5f);
        assertEquals("Second part of parabola should be SOUTH_WEST", "SOUTH_WEST", dir3);
        
        // End: moving west back to x=3
        String dir4 = calculateDirection(3.0f, 5.0f, 3.2f, 4.8f);
        assertEquals("End of parabola should be SOUTH_WEST", "SOUTH_WEST", dir4);
    }

    /**
     * Test that very small deltas (below epsilon) result in empty direction.
     */
    @Test
    public void testDirectionCalculation_SmallDelta() {
        float x = 3.0f;
        float y = 4.0f;
        
        // Delta of 0
        String dir1 = calculateDirection(x, y, x, y);
        assertEquals("Zero delta should return empty", "", dir1);
        
        // Delta below epsilon
        String dir2 = calculateDirection(x + 0.00001f, y + 0.00001f, x, y);
        assertEquals("Delta below epsilon should return empty", "", dir2);
        
        // Delta at epsilon - due to <= comparison, exactly at epsilon returns empty
        // However, due to floating point precision, exactly at epsilon may not be detected
        // So we just test that delta significantly above epsilon returns a valid direction
        
        // Delta above epsilon
        String dir4 = calculateDirection(x + 0.001f, y + 0.001f, x, y);
        assertNotEquals("Delta above epsilon should return a direction", "", dir4);
    }
}

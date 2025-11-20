package piotr_gorczynski.soccer2;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for KickPlayerSprite class
 */
public class KickPlayerSpriteTest {

    @Test
    public void testFrameCount() {
        assertEquals("Kick sprite should have 16 frames", 16, KickPlayerSprite.FRAME_COUNT);
    }

    @Test
    public void testFrameDuration() {
        assertEquals("Kick sprite frame duration should match run sprite", 
                RunPlayerSprite.FRAME_DURATION_MS, KickPlayerSprite.FRAME_DURATION_MS);
    }

    @Test
    public void testGetFrames_NullContext() {
        // KickPlayerSprite should handle null context gracefully
        try {
            KickPlayerSprite.getFrames(null, 0);
            // Should not throw exception
        } catch (Exception e) {
            fail("getFrames should handle null context without throwing exception: " + e.getMessage());
        }
    }
}

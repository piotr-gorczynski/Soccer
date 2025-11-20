package piotr_gorczynski.soccer2;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for KickPlayerSprite class constants
 */
public class KickPlayerSpriteTest {

    @Test
    public void testFrameCount_IsCorrect() {
        // KickPlayerSprite should have 16 frames as specified in requirements
        assertEquals(16, KickPlayerSprite.FRAME_COUNT);
    }

    @Test
    public void testFrameDuration_IsCorrect() {
        // KickPlayerSprite should use same frame duration as RunPlayerSprite
        assertEquals(35L, KickPlayerSprite.FRAME_DURATION_MS);
    }

    @Test
    public void testFrameDuration_MatchesRunPlayerSprite() {
        // Verify both sprites use the same frame duration for smooth animation
        assertEquals(RunPlayerSprite.FRAME_DURATION_MS, KickPlayerSprite.FRAME_DURATION_MS);
    }
}

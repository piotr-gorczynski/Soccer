package piotr_gorczynski.soccer2;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for SpriteSheetMetadata class
 */
public class SpriteSheetMetadataTest {

    @Test
    public void testBuilder_CreatesValidMetadata() {
        SpriteSheetMetadata metadata = new SpriteSheetMetadata.Builder(123)
                .frameWidth(64)
                .frameHeight(64)
                .frameCount(10)
                .startX(0)
                .startY(0)
                .columns(5)
                .rows(2)
                .frameDurationMs(100L)
                .build();

        assertEquals(123, metadata.getResourceId());
        assertEquals(64, metadata.getFrameWidth());
        assertEquals(64, metadata.getFrameHeight());
        assertEquals(10, metadata.getFrameCount());
        assertEquals(0, metadata.getStartX());
        assertEquals(0, metadata.getStartY());
        assertEquals(5, metadata.getColumns());
        assertEquals(2, metadata.getRows());
        assertEquals(100L, metadata.getFrameDurationMs());
    }

    @Test
    public void testBuilder_DefaultValues() {
        SpriteSheetMetadata metadata = new SpriteSheetMetadata.Builder(123)
                .frameWidth(64)
                .frameHeight(64)
                .frameCount(10)
                .build();

        assertEquals(0, metadata.getStartX());
        assertEquals(0, metadata.getStartY());
        assertEquals(1, metadata.getColumns());
        assertEquals(1, metadata.getRows());
        assertEquals(100L, metadata.getFrameDurationMs());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_InvalidFrameWidth() {
        new SpriteSheetMetadata.Builder(123)
                .frameWidth(0)
                .frameHeight(64)
                .frameCount(10)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_InvalidFrameHeight() {
        new SpriteSheetMetadata.Builder(123)
                .frameWidth(64)
                .frameHeight(-1)
                .frameCount(10)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_InvalidFrameCount() {
        new SpriteSheetMetadata.Builder(123)
                .frameWidth(64)
                .frameHeight(64)
                .frameCount(0)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_InvalidFrameDuration() {
        new SpriteSheetMetadata.Builder(123)
                .frameWidth(64)
                .frameHeight(64)
                .frameCount(10)
                .frameDurationMs(-1L)
                .build();
    }

    @Test
    public void testBuilder_IdleSpriteConfiguration() {
        // Test realistic configuration for idle sprite (from RunningPlayerSprite)
        SpriteSheetMetadata metadata = new SpriteSheetMetadata.Builder(456)
                .frameWidth(128)
                .frameHeight(128)
                .frameCount(35)
                .startX(0)
                .startY(129)
                .columns(35)
                .rows(1)
                .frameDurationMs(250L)
                .build();

        assertEquals(456, metadata.getResourceId());
        assertEquals(128, metadata.getFrameWidth());
        assertEquals(128, metadata.getFrameHeight());
        assertEquals(35, metadata.getFrameCount());
        assertEquals(0, metadata.getStartX());
        assertEquals(129, metadata.getStartY());
        assertEquals(35, metadata.getColumns());
        assertEquals(1, metadata.getRows());
        assertEquals(250L, metadata.getFrameDurationMs());
    }

    @Test
    public void testBuilder_MultiRowConfiguration() {
        // Test configuration for multi-row sprite sheet
        SpriteSheetMetadata metadata = new SpriteSheetMetadata.Builder(789)
                .frameWidth(100)
                .frameHeight(100)
                .frameCount(20)
                .startX(10)
                .startY(10)
                .columns(5)
                .rows(4)
                .frameDurationMs(150L)
                .build();

        assertEquals(789, metadata.getResourceId());
        assertEquals(100, metadata.getFrameWidth());
        assertEquals(100, metadata.getFrameHeight());
        assertEquals(20, metadata.getFrameCount());
        assertEquals(10, metadata.getStartX());
        assertEquals(10, metadata.getStartY());
        assertEquals(5, metadata.getColumns());
        assertEquals(4, metadata.getRows());
        assertEquals(150L, metadata.getFrameDurationMs());
    }
}

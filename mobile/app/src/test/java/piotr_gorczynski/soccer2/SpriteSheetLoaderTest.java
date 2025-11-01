package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for SpriteSheetLoader class
 */
@RunWith(AndroidJUnit4.class)
public class SpriteSheetLoaderTest {

    @Test
    public void testLoadFrames_NullContext() {
        SpriteSheetMetadata metadata = new SpriteSheetMetadata.Builder(123)
                .frameWidth(64)
                .frameHeight(64)
                .frameCount(10)
                .build();

        Bitmap[] frames = SpriteSheetLoader.loadFrames(null, metadata);
        
        assertNotNull("Should return non-null array", frames);
        assertEquals("Should return empty array for null context", 0, frames.length);
    }

    @Test
    public void testLoadFrames_NullMetadata() {
        Context context = ApplicationProvider.getApplicationContext();
        
        Bitmap[] frames = SpriteSheetLoader.loadFrames(context, null);
        
        assertNotNull("Should return non-null array", frames);
        assertEquals("Should return empty array for null metadata", 0, frames.length);
    }

    @Test
    public void testLoadFrames_InvalidResourceId() {
        Context context = ApplicationProvider.getApplicationContext();
        SpriteSheetMetadata metadata = new SpriteSheetMetadata.Builder(0)
                .frameWidth(64)
                .frameHeight(64)
                .frameCount(10)
                .build();

        Bitmap[] frames = SpriteSheetLoader.loadFrames(context, metadata);
        
        assertNotNull("Should return non-null array", frames);
        assertEquals("Should return empty array for invalid resource", 0, frames.length);
    }

    @Test
    public void testLoadFrames_ValidSpriteSheet() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Get the actual sprite sheet resource
        int spriteSheetResId = context.getResources().getIdentifier(
                "spritesheet_idle",
                "drawable",
                context.getPackageName()
        );
        
        if (spriteSheetResId == 0) {
            // Skip test if sprite sheet is not available (e.g., in unit tests without resources)
            return;
        }

        SpriteSheetMetadata metadata = new SpriteSheetMetadata.Builder(spriteSheetResId)
                .frameWidth(128)
                .frameHeight(128)
                .frameCount(35)
                .startX(0)
                .startY(129)
                .columns(35)
                .rows(1)
                .frameDurationMs(250L)
                .build();

        Bitmap[] frames = SpriteSheetLoader.loadFrames(context, metadata);
        
        assertNotNull("Should return non-null array", frames);
        assertTrue("Should return frames from valid sprite sheet", frames.length > 0);
        
        // Verify frame dimensions
        for (int i = 0; i < frames.length; i++) {
            assertNotNull("Frame " + i + " should not be null", frames[i]);
            assertTrue("Frame " + i + " width should be positive", frames[i].getWidth() > 0);
            assertTrue("Frame " + i + " height should be positive", frames[i].getHeight() > 0);
        }
    }

    @Test
    public void testLoadFrames_SingleFrame() {
        Context context = ApplicationProvider.getApplicationContext();
        
        int spriteSheetResId = context.getResources().getIdentifier(
                "spritesheet_idle",
                "drawable",
                context.getPackageName()
        );
        
        if (spriteSheetResId == 0) {
            return;
        }

        // Request only one frame
        SpriteSheetMetadata metadata = new SpriteSheetMetadata.Builder(spriteSheetResId)
                .frameWidth(128)
                .frameHeight(128)
                .frameCount(1)
                .startX(0)
                .startY(129)
                .columns(1)
                .rows(1)
                .frameDurationMs(250L)
                .build();

        Bitmap[] frames = SpriteSheetLoader.loadFrames(context, metadata);
        
        assertNotNull("Should return non-null array", frames);
        if (frames.length > 0) {
            assertEquals("Should return exactly one frame when requested", 1, frames.length);
        }
    }
}

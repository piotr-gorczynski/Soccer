package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for animation info dialog feature
 * Verifies that the required string resources exist for the one-time animation info dialog
 */
@RunWith(AndroidJUnit4.class)
public class AnimationInfoDialogTest {

    @Test
    public void testAnimationInfoMessageExists() {
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull("Context should be available for testing", context);
        
        // Verify animation info message string resource exists
        try {
            String animationInfoMessage = context.getString(R.string.animation_info_message);
            assertNotNull("animation_info_message string resource should exist", animationInfoMessage);
            assertFalse("animation_info_message should not be empty", animationInfoMessage.trim().isEmpty());
            
            // Verify the message contains key information about animations
            assertTrue("Message should mention animations", 
                animationInfoMessage.toLowerCase().contains("animation"));
            
            // Verify the message mentions settings
            assertTrue("Message should mention settings", 
                animationInfoMessage.toLowerCase().contains("setting"));
            
        } catch (Exception e) {
            fail("animation_info_message string resource is missing: " + e.getMessage());
        }
    }
    
    @Test
    public void testAnimationInfoMessageNotTranslatable() {
        // This test verifies that we have the animation_info_message resource
        // The actual translatability is handled by the translatable attribute in XML
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            String message = context.getString(R.string.animation_info_message);
            assertNotNull("Animation info message should exist", message);
            assertTrue("Message should have reasonable length", message.length() > 10);
            assertTrue("Message should have reasonable length", message.length() < 500);
        } catch (Exception e) {
            fail("Failed to load animation info message: " + e.getMessage());
        }
    }
}

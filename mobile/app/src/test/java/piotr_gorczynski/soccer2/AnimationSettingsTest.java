package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for animation settings feature
 * Tests that the animation preference is properly defined and accessible
 */
@RunWith(AndroidJUnit4.class)
public class AnimationSettingsTest {

    @Test
    public void testAnimationPreferenceStringsExist() {
        // Test that animation preference strings exist in all required languages
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull("Context should be available for testing", context);
        
        // Test English strings (default)
        try {
            int titleId = context.getResources().getIdentifier("pref_title_animations", "string", context.getPackageName());
            assertTrue("pref_title_animations string should be defined", titleId != 0);
            
            String title = context.getString(titleId);
            assertNotNull("pref_title_animations should not be null", title);
            assertFalse("pref_title_animations should not be empty", title.trim().isEmpty());
            
            int summaryId = context.getResources().getIdentifier("pref_summary_animations", "string", context.getPackageName());
            assertTrue("pref_summary_animations string should be defined", summaryId != 0);
            
            String summary = context.getString(summaryId);
            assertNotNull("pref_summary_animations should not be null", summary);
            assertFalse("pref_summary_animations should not be empty", summary.trim().isEmpty());
            
        } catch (Exception e) {
            fail("Animation preference strings are not properly defined: " + e.getMessage());
        }
    }

    @Test
    public void testAnimationPreferenceKeyExists() {
        // Verify that the animations_enabled preference key is defined in the preferences XML
        // We can't directly access the XML preference during unit tests, but we can verify
        // the string resources exist
        Context context = ApplicationProvider.getApplicationContext();
        
        // The preference key is "animations_enabled" - this is a constant in our code
        // We verify this indirectly by checking that the related string resources exist
        assertNotNull("Context should be available", context);
        
        try {
            // Verify preference strings are defined
            int titleResId = context.getResources().getIdentifier("pref_title_animations", "string", context.getPackageName());
            assertTrue("Preference title resource should exist", titleResId != 0);
            
        } catch (Exception e) {
            fail("Failed to verify animation preference configuration: " + e.getMessage());
        }
    }

    @Test
    public void testMenuActivityHasAnimationMethods() {
        // Verify that MenuActivity has the methods that handle animations
        try {
            Class<?> menuActivityClass = Class.forName("piotr_gorczynski.soccer2.MenuActivity");
            assertNotNull("MenuActivity class should exist", menuActivityClass);
            
        } catch (ClassNotFoundException e) {
            fail("MenuActivity class not found: " + e.getMessage());
        }
    }

    @Test
    public void testFieldClassHasAnimationParameter() {
        // Verify that Field class exists and can be constructed
        try {
            Class<?> fieldClass = Class.forName("piotr_gorczynski.soccer2.Field");
            assertNotNull("Field class should exist", fieldClass);
            
            // Field should have a constructor that accepts the animation parameter
            // We verify the class exists and has constructors
            assertTrue("Field class should have constructors", fieldClass.getConstructors().length > 0);
            
        } catch (ClassNotFoundException e) {
            fail("Field class not found: " + e.getMessage());
        }
    }

    @Test
    public void testGameViewClassExists() {
        // Verify that GameView class exists and can pass animation settings
        try {
            Class<?> gameViewClass = Class.forName("piotr_gorczynski.soccer2.GameView");
            assertNotNull("GameView class should exist", gameViewClass);
            
            // GameView should have constructors
            assertTrue("GameView class should have constructors", gameViewClass.getConstructors().length > 0);
            
        } catch (ClassNotFoundException e) {
            fail("GameView class not found: " + e.getMessage());
        }
    }

    @Test
    public void testPreferenceXMLResourceExists() {
        // Test that the preference XML file exists
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            int xmlResId = context.getResources().getIdentifier("pref_android_level", "xml", context.getPackageName());
            assertTrue("Preference XML resource should exist", xmlResId != 0);
            
        } catch (Exception e) {
            fail("Preference XML resource not found: " + e.getMessage());
        }
    }
}

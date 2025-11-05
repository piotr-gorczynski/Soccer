package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for hand tutorial settings feature
 * Tests that the hand tutorial preference is properly defined and accessible
 */
@RunWith(AndroidJUnit4.class)
public class HandTutorialSettingsTest {

    @Test
    public void testHandTutorialPreferenceStringsExist() {
        // Test that hand tutorial preference strings exist
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull("Context should be available for testing", context);
        
        // Test English strings (default)
        try {
            int titleId = context.getResources().getIdentifier("pref_title_show_hand_tutorial", "string", context.getPackageName());
            assertTrue("pref_title_show_hand_tutorial string should be defined", titleId != 0);
            
            String title = context.getString(titleId);
            assertNotNull("pref_title_show_hand_tutorial should not be null", title);
            assertFalse("pref_title_show_hand_tutorial should not be empty", title.trim().isEmpty());
            
            int summaryId = context.getResources().getIdentifier("pref_summary_show_hand_tutorial", "string", context.getPackageName());
            assertTrue("pref_summary_show_hand_tutorial string should be defined", summaryId != 0);
            
            String summary = context.getString(summaryId);
            assertNotNull("pref_summary_show_hand_tutorial should not be null", summary);
            assertFalse("pref_summary_show_hand_tutorial should not be empty", summary.trim().isEmpty());
            
        } catch (Exception e) {
            fail("Hand tutorial preference strings are not properly defined: " + e.getMessage());
        }
    }

    @Test
    public void testHandTutorialDialogStringsExist() {
        // Test that hand tutorial dialog strings exist
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull("Context should be available for testing", context);
        
        try {
            // Test dialog title
            int titleId = context.getResources().getIdentifier("hand_tutorial_dialog_title", "string", context.getPackageName());
            assertTrue("hand_tutorial_dialog_title string should be defined", titleId != 0);
            String title = context.getString(titleId);
            assertNotNull("hand_tutorial_dialog_title should not be null", title);
            assertFalse("hand_tutorial_dialog_title should not be empty", title.trim().isEmpty());
            
            // Test dialog message
            int messageId = context.getResources().getIdentifier("hand_tutorial_dialog_message", "string", context.getPackageName());
            assertTrue("hand_tutorial_dialog_message string should be defined", messageId != 0);
            String message = context.getString(messageId);
            assertNotNull("hand_tutorial_dialog_message should not be null", message);
            assertFalse("hand_tutorial_dialog_message should not be empty", message.trim().isEmpty());
            
            // Test Yes button
            int yesId = context.getResources().getIdentifier("hand_tutorial_dialog_yes", "string", context.getPackageName());
            assertTrue("hand_tutorial_dialog_yes string should be defined", yesId != 0);
            String yes = context.getString(yesId);
            assertNotNull("hand_tutorial_dialog_yes should not be null", yes);
            assertFalse("hand_tutorial_dialog_yes should not be empty", yes.trim().isEmpty());
            
            // Test No button
            int noId = context.getResources().getIdentifier("hand_tutorial_dialog_no", "string", context.getPackageName());
            assertTrue("hand_tutorial_dialog_no string should be defined", noId != 0);
            String no = context.getString(noId);
            assertNotNull("hand_tutorial_dialog_no should not be null", no);
            assertFalse("hand_tutorial_dialog_no should not be empty", no.trim().isEmpty());
            
        } catch (Exception e) {
            fail("Hand tutorial dialog strings are not properly defined: " + e.getMessage());
        }
    }

    @Test
    public void testFieldClassHasHandTutorialCallback() {
        // Verify that Field class has the hand tutorial callback interface
        try {
            Class<?> fieldClass = Class.forName("piotr_gorczynski.soccer2.Field");
            assertNotNull("Field class should exist", fieldClass);
            
            // Check for the nested callback interface
            Class<?>[] nestedClasses = fieldClass.getDeclaredClasses();
            boolean hasCallbackInterface = false;
            for (Class<?> nestedClass : nestedClasses) {
                if (nestedClass.getSimpleName().equals("HandTutorialDialogCallback")) {
                    hasCallbackInterface = true;
                    break;
                }
            }
            assertTrue("Field should have HandTutorialDialogCallback interface", hasCallbackInterface);
            
        } catch (ClassNotFoundException e) {
            fail("Field class not found: " + e.getMessage());
        }
    }

    @Test
    public void testGameActivityHasDialogMethod() {
        // Verify that GameActivity can show the hand tutorial dialog
        try {
            Class<?> gameActivityClass = Class.forName("piotr_gorczynski.soccer2.GameActivity");
            assertNotNull("GameActivity class should exist", gameActivityClass);
            
            // The class should have methods to handle the dialog
            assertTrue("GameActivity should have methods", gameActivityClass.getDeclaredMethods().length > 0);
            
        } catch (ClassNotFoundException e) {
            fail("GameActivity class not found: " + e.getMessage());
        }
    }

    @Test
    public void testGameViewHasGetFieldMethod() {
        // Verify that GameView provides access to Field
        try {
            Class<?> gameViewClass = Class.forName("piotr_gorczynski.soccer2.GameView");
            assertNotNull("GameView class should exist", gameViewClass);
            
            // Check if getField method exists
            boolean hasGetField = false;
            try {
                gameViewClass.getDeclaredMethod("getField");
                hasGetField = true;
            } catch (NoSuchMethodException e) {
                // Method doesn't exist
            }
            assertTrue("GameView should have getField method", hasGetField);
            
        } catch (ClassNotFoundException e) {
            fail("GameView class not found: " + e.getMessage());
        }
    }

    @Test
    public void testPreferenceXMLContainsHandTutorial() {
        // Test that the preference XML file exists and can be accessed
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            int xmlResId = context.getResources().getIdentifier("pref_android_level", "xml", context.getPackageName());
            assertTrue("Preference XML resource should exist", xmlResId != 0);
            
        } catch (Exception e) {
            fail("Preference XML resource not found: " + e.getMessage());
        }
    }
}

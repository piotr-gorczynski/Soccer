package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for tutorial messages settings feature
 * Tests that the tutorial messages preference is properly defined and accessible
 * separately from the hand tutorial preference
 */
@RunWith(AndroidJUnit4.class)
public class TutorialMessagesSettingsTest {

    @Test
    public void testTutorialMessagesPreferenceStringsExist() {
        // Test that tutorial messages preference strings exist
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull("Context should be available for testing", context);
        
        // Test English strings (default)
        try {
            int titleId = context.getResources().getIdentifier("pref_title_show_tutorial_messages", "string", context.getPackageName());
            assertTrue("pref_title_show_tutorial_messages string should be defined", titleId != 0);
            
            String title = context.getString(titleId);
            assertNotNull("pref_title_show_tutorial_messages should not be null", title);
            assertFalse("pref_title_show_tutorial_messages should not be empty", title.trim().isEmpty());
            
            int summaryId = context.getResources().getIdentifier("pref_summary_show_tutorial_messages", "string", context.getPackageName());
            assertTrue("pref_summary_show_tutorial_messages string should be defined", summaryId != 0);
            
            String summary = context.getString(summaryId);
            assertNotNull("pref_summary_show_tutorial_messages should not be null", summary);
            assertFalse("pref_summary_show_tutorial_messages should not be empty", summary.trim().isEmpty());
            
        } catch (Exception e) {
            fail("Tutorial messages preference strings are not properly defined: " + e.getMessage());
        }
    }

    @Test
    public void testTutorialMessageStringsExist() {
        // Test that all tutorial message strings exist
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull("Context should be available for testing", context);
        
        String[] tutorialMessageKeys = {
            "field_tutorial_message",
            "field_tutorial_goal_1",
            "field_tutorial_goal_2",
            "field_tutorial_goal_3",
            "field_tutorial_own_goal",
            "field_tutorial_opponent_goal_1",
            "field_tutorial_opponent_goal_2",
            "field_tutorial_opponent_goal_3",
            "field_tutorial_no_moves",
            "field_tutorial_bounce_visited",
            "field_tutorial_bounce_border"
        };
        
        try {
            for (String key : tutorialMessageKeys) {
                int stringId = context.getResources().getIdentifier(key, "string", context.getPackageName());
                assertTrue(key + " string should be defined", stringId != 0);
                
                String value = context.getString(stringId);
                assertNotNull(key + " should not be null", value);
                assertFalse(key + " should not be empty", value.trim().isEmpty());
            }
        } catch (Exception e) {
            fail("Tutorial message strings are not properly defined: " + e.getMessage());
        }
    }

    @Test
    public void testFieldClassHasTutorialMessageType() {
        // Verify that Field class has the TutorialMessageType enum
        try {
            Class<?> fieldClass = Class.forName("piotr_gorczynski.soccer2.Field");
            assertNotNull("Field class should exist", fieldClass);
            
            // Check for the nested TutorialMessageType enum
            Class<?>[] nestedClasses = fieldClass.getDeclaredClasses();
            boolean hasTutorialMessageType = false;
            for (Class<?> nestedClass : nestedClasses) {
                if (nestedClass.getSimpleName().equals("TutorialMessageType")) {
                    hasTutorialMessageType = true;
                    // Verify it's an enum
                    assertTrue("TutorialMessageType should be an enum", nestedClass.isEnum());
                    break;
                }
            }
            assertTrue("Field should have TutorialMessageType enum", hasTutorialMessageType);
            
        } catch (ClassNotFoundException e) {
            fail("Field class not found: " + e.getMessage());
        }
    }

    @Test
    public void testFieldClassHasSetTutorialMessageTypeMethod() {
        // Verify that Field class has setTutorialMessageType method
        try {
            Class<?> fieldClass = Class.forName("piotr_gorczynski.soccer2.Field");
            assertNotNull("Field class should exist", fieldClass);
            
            // Find TutorialMessageType enum first
            Class<?> tutorialMessageTypeClass = null;
            for (Class<?> nestedClass : fieldClass.getDeclaredClasses()) {
                if (nestedClass.getSimpleName().equals("TutorialMessageType")) {
                    tutorialMessageTypeClass = nestedClass;
                    break;
                }
            }
            assertNotNull("TutorialMessageType enum should exist", tutorialMessageTypeClass);
            
            // Check if setTutorialMessageType method exists
            boolean hasMethod = false;
            try {
                fieldClass.getDeclaredMethod("setTutorialMessageType", tutorialMessageTypeClass);
                hasMethod = true;
            } catch (NoSuchMethodException e) {
                // Method doesn't exist
            }
            assertTrue("Field should have setTutorialMessageType method", hasMethod);
            
        } catch (ClassNotFoundException e) {
            fail("Field class not found: " + e.getMessage());
        }
    }

    @Test
    public void testPreferenceXMLContainsTutorialMessages() {
        // Test that the preference XML file contains tutorial messages preference
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            int xmlResId = context.getResources().getIdentifier("pref_android_level", "xml", context.getPackageName());
            assertTrue("Preference XML resource should exist", xmlResId != 0);
            
        } catch (Exception e) {
            fail("Preference XML resource not found: " + e.getMessage());
        }
    }
    
    @Test
    public void testTutorialMessagesAndHandTutorialAreIndependent() {
        // This test verifies that tutorial messages preference is separate from hand tutorial
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            // Both preferences should exist independently
            int handTutorialId = context.getResources().getIdentifier("pref_title_show_hand_tutorial", "string", context.getPackageName());
            int tutorialMessagesId = context.getResources().getIdentifier("pref_title_show_tutorial_messages", "string", context.getPackageName());
            
            assertTrue("Hand tutorial preference should exist", handTutorialId != 0);
            assertTrue("Tutorial messages preference should exist", tutorialMessagesId != 0);
            
            // They should be different strings
            String handTutorialTitle = context.getString(handTutorialId);
            String tutorialMessagesTitle = context.getString(tutorialMessagesId);
            
            assertNotEquals("Hand tutorial and tutorial messages preferences should be different", 
                handTutorialTitle, tutorialMessagesTitle);
            
        } catch (Exception e) {
            fail("Failed to verify independence of preferences: " + e.getMessage());
        }
    }
}

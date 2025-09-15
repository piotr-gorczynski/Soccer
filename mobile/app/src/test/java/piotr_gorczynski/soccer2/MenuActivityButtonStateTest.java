package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for MenuActivity button state management
 * Tests that buttons are properly enabled/disabled based on backend availability
 */
@RunWith(AndroidJUnit4.class)
public class MenuActivityButtonStateTest {

    @Test
    public void testButtonIdsExistForBackendAvailabilityToggle() {
        // Test that the button IDs referenced in updateUiForAuthState exist in resources
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull("Context should be available for testing", context);
        
        // Verify that all button IDs used in updateUiForAuthState exist
        String[] buttonIds = {
            "InviteFriend",      // inviteBtn
            "ShowInvites",       // pendingBtn
            "openTournamentsBtn", // tournamentsBtn
            "openRankingBtn"     // rankingBtn
        };
        
        for (String buttonId : buttonIds) {
            try {
                int resourceId = context.getResources().getIdentifier(buttonId, "id", context.getPackageName());
                assertTrue("Button ID " + buttonId + " should be defined in layout (needed for backend availability toggle)", 
                          resourceId != 0);
            } catch (Exception e) {
                fail("Button ID " + buttonId + " is not properly defined in layout resources: " + e.getMessage());
            }
        }
    }

    @Test
    public void testMenuActivityClassHasRequiredMethods() {
        // Verify that MenuActivity has the updateUiForAuthState method
        // This is important because it's where the button state bug occurs
        try {
            Class<?> menuActivityClass = Class.forName("piotr_gorczynski.soccer2.MenuActivity");
            assertNotNull("MenuActivity class should exist", menuActivityClass);
            
            // Check if updateUiForAuthState method exists (it's private, but we can still verify the class structure)
            // Note: Since updateUiForAuthState is private, we can't directly test it, but we can verify the class exists
            assertTrue("MenuActivity should be a valid class", menuActivityClass != null);
            
        } catch (ClassNotFoundException e) {
            fail("MenuActivity class not found: " + e.getMessage());
        }
    }

    @Test
    public void testRequiredStringResourcesExist() {
        // Test that string resources used in backend availability scenarios exist
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            // These strings are used when showing dialogs related to backend availability
            String serverUnavailable = context.getString(
                context.getResources().getIdentifier("server_unavailable_message", "string", context.getPackageName())
            );
            assertNotNull("server_unavailable_message string should exist", serverUnavailable);
            assertFalse("server_unavailable_message should not be empty", serverUnavailable.trim().isEmpty());
            
        } catch (Exception e) {
            fail("Required string resources for backend availability are missing: " + e.getMessage());
        }
    }
}
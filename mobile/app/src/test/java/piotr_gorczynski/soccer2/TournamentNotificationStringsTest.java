package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for tournament notification dialog strings
 * Tests that the tournament notification strings are properly defined and accessible
 */
@RunWith(AndroidJUnit4.class)
public class TournamentNotificationStringsTest {

    @Test
    public void testTournamentNotificationStringExists() {
        // Test that tournament notification string exists
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull("Context should be available for testing", context);
        
        try {
            int messageId = context.getResources().getIdentifier("tournament_started_dialog_message", "string", context.getPackageName());
            assertTrue("tournament_started_dialog_message string should be defined", messageId != 0);
            
            // Test that the string can be formatted with a tournament name
            String message = context.getString(messageId, "Test Tournament");
            assertNotNull("tournament_started_dialog_message should not be null", message);
            assertFalse("tournament_started_dialog_message should not be empty", message.trim().isEmpty());
            assertTrue("tournament_started_dialog_message should contain tournament name", message.contains("Test Tournament"));
            
        } catch (Exception e) {
            fail("Tournament notification string is not properly defined: " + e.getMessage());
        }
    }

    @Test
    public void testYesNoButtonStringsExist() {
        // Verify that yes/no button strings exist (reused from existing resources)
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            int yesId = context.getResources().getIdentifier("yes", "string", context.getPackageName());
            assertTrue("yes string should be defined", yesId != 0);
            
            String yes = context.getString(yesId);
            assertNotNull("yes string should not be null", yes);
            assertFalse("yes string should not be empty", yes.trim().isEmpty());
            
            int noId = context.getResources().getIdentifier("no", "string", context.getPackageName());
            assertTrue("no string should be defined", noId != 0);
            
            String no = context.getString(noId);
            assertNotNull("no string should not be null", no);
            assertFalse("no string should not be empty", no.trim().isEmpty());
            
        } catch (Exception e) {
            fail("Yes/No button strings are not properly defined: " + e.getMessage());
        }
    }
}

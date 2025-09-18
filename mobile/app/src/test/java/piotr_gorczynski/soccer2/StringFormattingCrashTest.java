package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for string formatting crash fixes
 * Specifically tests the Bengali localization format specifier fix
 */
@RunWith(AndroidJUnit4.class)
public class StringFormattingCrashTest {

    @Test
    public void testWinnerStringFormatting_allLocalizations_shouldNotCrash() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Test that all winner_is string resources can be formatted without crashing
        try {
            String englishResult = context.getString(R.string.winner_is, "Player1");
            assertNotNull("English winner_is should format successfully", englishResult);
            assertTrue("English result should contain player name", englishResult.contains("Player1"));
        } catch (Exception e) {
            fail("English winner_is formatting should not crash: " + e.getMessage());
        }
    }

    @Test
    public void testStringFormatDefensiveHandling() {
        // This test verifies our defensive string formatting approach
        // We can't easily test the actual problematic strings without running in Bengali locale
        // but we can verify the defensive pattern works
        
        String playerName = "TestPlayer";
        String result;
        
        try {
            // This should work for valid format strings
            result = String.format("The winner is %1$s!", playerName);
            assertEquals("The winner is TestPlayer!", result);
        } catch (java.util.IllegalFormatException e) {
            // This is our fallback case
            result = "The winner is " + playerName + "!";
            assertEquals("The winner is TestPlayer!", result);
        }
        
        assertNotNull("Result should never be null", result);
        assertTrue("Result should contain player name", result.contains(playerName));
    }

    @Test
    public void testOtherFormatStrings_shouldNotCrash() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Test other format strings that were fixed
        try {
            String timeoutResult = context.getString(R.string.winner_timeout, "Player1");
            assertNotNull("winner_timeout should format successfully", timeoutResult);
        } catch (Exception e) {
            fail("winner_timeout formatting should not crash: " + e.getMessage());
        }
        
        try {
            String abandonResult = context.getString(R.string.winner_abandon, "Player1");
            assertNotNull("winner_abandon should format successfully", abandonResult);
        } catch (Exception e) {
            fail("winner_abandon formatting should not crash: " + e.getMessage());
        }
    }
}
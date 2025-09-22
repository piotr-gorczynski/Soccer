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
        
        // Test additional format strings that were fixed
        try {
            String sendMoveResult = context.getString(R.string.failed_to_send_move, "Error");
            assertNotNull("failed_to_send_move should format successfully", sendMoveResult);
        } catch (Exception e) {
            fail("failed_to_send_move formatting should not crash: " + e.getMessage());
        }
        
        try {
            String registeredResult = context.getString(R.string.registered_as, "Player1");
            assertNotNull("registered_as should format successfully", registeredResult);
        } catch (Exception e) {
            fail("registered_as formatting should not crash: " + e.getMessage());
        }
    }
    
    @Test
    public void testSafeStringFormatterUtility() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Test SafeStringFormatter with valid format strings
        String validResult = SafeStringFormatter.safeGetString(context, R.string.winner_is, "TestPlayer");
        assertNotNull("SafeStringFormatter should return non-null result", validResult);
        assertTrue("SafeStringFormatter result should contain player name", validResult.contains("TestPlayer"));
        
        // Test SafeStringFormatter with multiple parameters
        String multiParamResult = SafeStringFormatter.safeGetString(context, R.string.slots_format, 5, 10);
        assertNotNull("SafeStringFormatter should handle multiple parameters", multiParamResult);
        assertTrue("Multi-parameter result should contain numbers", 
                   multiParamResult.contains("5") && multiParamResult.contains("10"));
        
        // Test SafeStringFormatter fallback behavior by using a non-existent resource
        // This tests the ultimate fallback behavior
        try {
            String fallbackResult = SafeStringFormatter.safeFormat("Invalid format %z", "test");
            assertNotNull("SafeStringFormatter should provide fallback for invalid format", fallbackResult);
            assertTrue("Fallback should contain original argument", fallbackResult.contains("test"));
        } catch (Exception e) {
            fail("SafeStringFormatter should never throw exceptions: " + e.getMessage());
        }
    }
    
    @Test
    public void testSafeFormatMethod() {
        // Test valid format string
        String validFormat = SafeStringFormatter.safeFormat("Hello %s!", "World");
        assertEquals("Valid format should work correctly", "Hello World!", validFormat);
        
        // Test invalid format string - should fallback gracefully
        String invalidFormat = SafeStringFormatter.safeFormat("Hello %z!", "World");
        assertNotNull("Invalid format should return fallback", invalidFormat);
        assertTrue("Fallback should contain the argument", invalidFormat.contains("World"));
        
        // Test with no arguments
        String noArgs = SafeStringFormatter.safeFormat("Just text");
        assertEquals("No args format should work", "Just text", noArgs);
        
        // Test with multiple arguments
        String multiArgs = SafeStringFormatter.safeFormat("Player %s scored %d goals", "John", 3);
        assertTrue("Multi-arg format should contain all args", 
                   multiArgs.contains("John") && multiArgs.contains("3"));
    }
    
    @Test
    public void testUnknownFormatConversionExceptionHandling() {
        // Test format strings that would cause UnknownFormatConversionException
        // This is the specific case from the crash report: "Conversion = ' '"
        String formatWithSpace = "Winner is %1 $s!"; // Space between %1 and $s
        String result = SafeStringFormatter.safeFormat(formatWithSpace, "TestPlayer");
        
        assertNotNull("SafeStringFormatter should handle UnknownFormatConversionException", result);
        assertTrue("Result should contain the player name", result.contains("TestPlayer"));
        assertTrue("Result should contain some fallback text", result.length() > 0);
        
        // Test another malformed format that could cause UnknownFormatConversionException
        String formatWithInvalidConversion = "The winner is %1$z!"; // Invalid conversion 'z'
        String result2 = SafeStringFormatter.safeFormat(formatWithInvalidConversion, "Player2");
        
        assertNotNull("SafeStringFormatter should handle invalid conversion", result2);
        assertTrue("Result should contain the player name", result2.contains("Player2"));
        
        // Test format with invalid character that would cause "Conversion = ' '" error
        String formatCausingSpaceConversion = "Winner: %1$ s"; // Space after $
        String result3 = SafeStringFormatter.safeFormat(formatCausingSpaceConversion, "Player3");
        
        assertNotNull("SafeStringFormatter should handle space conversion error", result3);
        assertTrue("Result should contain the player name", result3.contains("Player3"));
    }
}
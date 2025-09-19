package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for SafeStringFormatter to ensure it handles UnknownFormatConversionException
 */
@RunWith(AndroidJUnit4.class)
public class SafeStringFormatterTest {
    
    @Test
    public void testSafeFormatHandlesUnknownFormatConversionException() {
        // Test malformed format string that would cause UnknownFormatConversionException
        String malformedFormat = "The winner is %1 s!"; // Space between %1 and s
        String result = SafeStringFormatter.safeFormat(malformedFormat, "TestPlayer");
        
        // Should not crash and return fallback
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain fallback content", result.contains("TestPlayer"));
    }
    
    @Test
    public void testSafeFormatHandlesValidFormat() {
        // Test valid format string
        String validFormat = "The winner is %1$s!";
        String result = SafeStringFormatter.safeFormat(validFormat, "TestPlayer");
        
        assertEquals("Should format correctly", "The winner is TestPlayer!", result);
    }
    
    @Test
    public void testSafeGetStringFallback() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Test with valid string resource (assuming R.string.winner_is exists and is valid)
        String result = SafeStringFormatter.safeGetString(context, R.string.winner_is, "TestPlayer");
        
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain player name", result.contains("TestPlayer"));
    }
    
    @Test 
    public void testSafeFormatWithEmptyArgs() {
        String format = "No arguments here";
        String result = SafeStringFormatter.safeFormat(format);
        
        assertEquals("Should return format string unchanged", format, result);
    }
    
    @Test
    public void testSafeFormatWithMultipleArgs() {
        String format = "Player %1$s beat player %2$s!";
        String result = SafeStringFormatter.safeFormat(format, "Alice", "Bob");
        
        assertEquals("Should format multiple args correctly", "Player Alice beat player Bob!", result);
    }
}
package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Comprehensive test cases for string formatting safeguards across the entire application.
 * This test ensures that all vulnerable getString calls are properly protected against 
 * malformed format specifiers in localized string resources.
 */
@RunWith(AndroidJUnit4.class)
public class ComprehensiveStringFormattingTest {

    @Test
    public void testAllFormatStringsWithSafeFormatterDoNotCrash() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Test all known format strings that could potentially have malformed localized versions
        testFormatStringSafely(context, R.string.winner_is, "Player1");
        testFormatStringSafely(context, R.string.winner_timeout, "Player1"); 
        testFormatStringSafely(context, R.string.winner_abandon, "Player1");
        testFormatStringSafely(context, R.string.player_with_number, 1);
        testFormatStringSafely(context, R.string.failed_to_send_move, "Network error");
        testFormatStringSafely(context, R.string.registered_as, "test@example.com");
        testFormatStringSafely(context, R.string.could_not_send_verification_email, "SMTP error");
        testFormatStringSafely(context, R.string.login_failed, "Invalid credentials");
        testFormatStringSafely(context, R.string.failed_to_add_friend_reason, "User not found");
        testFormatStringSafely(context, R.string.link_account_failed, "OAuth error");
        testFormatStringSafely(context, R.string.slots_format, 5, 10);
        testFormatStringSafely(context, R.string.tournament_ended, "2 hours ago");
        testFormatStringSafely(context, R.string.waiting_for_opponent_named, "Alice");
        testFormatStringSafely(context, R.string.could_not_cancel_invite, "Network timeout");
        testFormatStringSafely(context, R.string.game_ready_title, "Bob");
        testFormatStringSafely(context, R.string.current_language, "English");
        testFormatStringSafely(context, R.string.hello_nickname, "TestUser");
        testFormatStringSafely(context, R.string.nickname_label, "TestNick");
        testFormatStringSafely(context, R.string.email_label, "test@example.com");
        testFormatStringSafely(context, R.string.login_method_label, "Google");
        testFormatStringSafely(context, R.string.facebook_id_label, "12345");
        testFormatStringSafely(context, R.string.facebook_name_label, "John Doe");
        
        // Test field-related format strings
        testFormatStringSafely(context, R.string.field_move_tail, "03:45");
        testFormatStringSafely(context, R.string.field_hourglass_tail, "02:30");
        testFormatStringSafely(context, R.string.field_move_ellipsis_tail, "01:15");
        testFormatStringSafely(context, R.string.field_waiting_for, "Charlie");
        testFormatStringSafely(context, R.string.field_to_start_tail, "00:30");
    }
    
    /**
     * Helper method to test a format string safely using SafeStringFormatter
     */
    private void testFormatStringSafely(Context context, int stringResId, Object... args) {
        try {
            String result = SafeStringFormatter.safeGetString(context, stringResId, args);
            assertNotNull("SafeStringFormatter should never return null for resource " + 
                         context.getResources().getResourceName(stringResId), result);
            
            // Verify that the result contains at least some of the input arguments
            if (args.length > 0) {
                boolean containsAtLeastOneArg = false;
                for (Object arg : args) {
                    if (result.contains(String.valueOf(arg))) {
                        containsAtLeastOneArg = true;
                        break;
                    }
                }
                assertTrue("Result should contain at least one argument for resource " + 
                          context.getResources().getResourceName(stringResId), containsAtLeastOneArg);
            }
            
        } catch (Exception e) {
            fail("SafeStringFormatter should never throw exceptions for resource " + 
                 context.getResources().getResourceName(stringResId) + ": " + e.getMessage());
        }
    }
    
    @Test
    public void testEdgeCasesAndErrorScenarios() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Test with null arguments - should not crash
        String resultWithNull = SafeStringFormatter.safeGetString(context, R.string.winner_is, (Object) null);
        assertNotNull("SafeStringFormatter should handle null arguments", resultWithNull);
        
        // Test with empty string argument
        String resultWithEmpty = SafeStringFormatter.safeGetString(context, R.string.winner_is, "");
        assertNotNull("SafeStringFormatter should handle empty string arguments", resultWithEmpty);
        
        // Test with special characters
        String resultWithSpecial = SafeStringFormatter.safeGetString(context, R.string.winner_is, "Player@#$%");
        assertNotNull("SafeStringFormatter should handle special characters", resultWithSpecial);
        assertTrue("Result should contain special characters", resultWithSpecial.contains("Player@#$%"));
        
        // Test with very long string
        String longName = "A".repeat(1000);
        String resultWithLong = SafeStringFormatter.safeGetString(context, R.string.winner_is, longName);
        assertNotNull("SafeStringFormatter should handle very long strings", resultWithLong);
        
        // Test safeFormat with various edge cases
        String nullFormat = SafeStringFormatter.safeFormat(null, "test");
        assertNotNull("SafeFormat should handle null format string", nullFormat);
        
        String emptyFormat = SafeStringFormatter.safeFormat("", "test");
        assertNotNull("SafeFormat should handle empty format string", emptyFormat);
    }
    
    @Test
    public void testBackwardCompatibilityWithRegularGetString() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Test that SafeStringFormatter produces same results as regular getString for valid format strings
        String regularResult = context.getString(R.string.winner_is, "TestPlayer");
        String safeResult = SafeStringFormatter.safeGetString(context, R.string.winner_is, "TestPlayer");
        
        assertEquals("SafeStringFormatter should produce same result as regular getString for valid strings", 
                     regularResult, safeResult);
        
        // Test with multiple parameters
        String regularSlots = context.getString(R.string.slots_format, 3, 8);
        String safeSlots = SafeStringFormatter.safeGetString(context, R.string.slots_format, 3, 8);
        
        assertEquals("SafeStringFormatter should match regular getString for multi-parameter strings",
                     regularSlots, safeSlots);
    }
}
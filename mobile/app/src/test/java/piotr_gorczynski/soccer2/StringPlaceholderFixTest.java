package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases to verify that the string placeholder fix correctly handles 
 * malformed {str} placeholders that cause "Invalid unicode escape sequence" errors
 * during Android resource compilation.
 * 
 * This addresses issue #521 where {str} placeholders were being generated during
 * the build process and causing compilation failures.
 */
@RunWith(AndroidJUnit4.class)
public class StringPlaceholderFixTest {

    @Test
    public void testAffectedStringResourcesExistAndAreValid() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Test that all previously problematic string resources now exist and are valid
        String[] affectedStrings = {
            "pref_title_block_invite_friend",
            "pref_summary_block_invite_friend", 
            "decline_reason_dont_use_providers",
            "friend_s_nickname"
        };
        
        for (String stringName : affectedStrings) {
            try {
                int resourceId = context.getResources().getIdentifier(stringName, "string", context.getPackageName());
                assertTrue("String resource " + stringName + " should exist", resourceId != 0);
                
                String value = context.getString(resourceId);
                assertNotNull("String " + stringName + " should not be null", value);
                assertFalse("String " + stringName + " should not be empty", value.trim().isEmpty());
                assertFalse("String " + stringName + " should not contain {str} placeholder", value.contains("{str}"));
                
            } catch (Exception e) {
                fail("String resource " + stringName + " caused exception: " + e.getMessage());
            }
        }
    }
    
    @Test
    public void testStringResourcesHaveProperContent() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Verify specific content to ensure proper replacement occurred
        String blockTitle = context.getString(R.string.pref_title_block_invite_friend);
        assertTrue("pref_title_block_invite_friend should contain 'Block'", blockTitle.contains("Block"));
        assertTrue("pref_title_block_invite_friend should contain 'Invite a Friend'", blockTitle.contains("Invite a Friend"));
        
        String blockSummary = context.getString(R.string.pref_summary_block_invite_friend);
        assertTrue("pref_summary_block_invite_friend should contain 'Other players'", blockSummary.contains("Other players"));
        assertTrue("pref_summary_block_invite_friend should contain 'tournaments not affected'", blockSummary.contains("tournaments not affected"));
        
        String declineReason = context.getString(R.string.decline_reason_dont_use_providers);
        assertTrue("decline_reason_dont_use_providers should contain 'Google/Facebook/Email'", declineReason.contains("Google/Facebook/Email"));
        
        String friendNickname = context.getString(R.string.friend_s_nickname);
        assertTrue("friend_s_nickname should contain 'Friend'", friendNickname.contains("Friend"));
        assertTrue("friend_s_nickname should contain 'nickname'", friendNickname.contains("nickname"));
    }
    
    @Test
    public void testNormalStringResourcesUnaffected() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Verify that normal strings were not modified by the fix
        String appName = context.getString(R.string.app_name);
        assertEquals("App name should be unchanged", "Soccer 2", appName);
        
        // Test formatted strings work correctly
        String currentLang = context.getString(R.string.current_language, "Test");
        assertTrue("Formatted strings should work", currentLang.contains("Test"));
        assertFalse("Formatted strings should not contain placeholder", currentLang.contains("{str}"));
    }
}
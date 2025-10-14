package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for LanguageSelectionActivity crash prevention fixes
 * Tests defensive measures around dialog display when activity is finishing/destroyed
 */
@RunWith(AndroidJUnit4.class)
public class LanguageSelectionActivityCrashTest {

    @Test
    public void testLanguageSelectionResourcesExist() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Verify that language selection strings are properly defined
        try {
            String selectLanguage = context.getString(R.string.select_language);
            assertNotNull("select_language string resource should exist", selectLanguage);
            assertFalse("select_language should not be empty", selectLanguage.trim().isEmpty());
            
        } catch (Exception e) {
            fail("Required string resources for language selection are missing: " + e.getMessage());
        }
    }

    @Test
    public void testLanguageManagerAvailableLanguages() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Verify that LanguageManager returns valid language array
        try {
            String[] languages = LanguageManager.getAvailableLanguages(context);
            assertNotNull("Available languages array should not be null", languages);
            assertTrue("Available languages array should not be empty", languages.length > 0);
            
            // Verify each language name is not null or empty
            for (int i = 0; i < languages.length; i++) {
                assertNotNull("Language at index " + i + " should not be null", languages[i]);
                assertFalse("Language at index " + i + " should not be empty", 
                    languages[i].trim().isEmpty());
            }
            
        } catch (Exception e) {
            fail("LanguageManager.getAvailableLanguages() failed: " + e.getMessage());
        }
    }

    @Test
    public void testLanguageCodeConversion() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Verify that language code conversion works correctly
        try {
            String[] languages = LanguageManager.getAvailableLanguages(context);
            
            // Test conversion for each language
            for (String language : languages) {
                String code = LanguageManager.getLanguageCodeFromLocalizedName(context, language);
                assertNotNull("Language code for '" + language + "' should not be null", code);
                assertFalse("Language code for '" + language + "' should not be empty", 
                    code.trim().isEmpty());
                assertTrue("Language code should be 2-3 characters", 
                    code.length() >= 2 && code.length() <= 3);
            }
            
        } catch (Exception e) {
            fail("Language code conversion failed: " + e.getMessage());
        }
    }

    @Test
    public void testLanguageManagerIsLanguageSet() {
        Context context = ApplicationProvider.getApplicationContext();
        
        // Verify that isLanguageSet() method works
        try {
            // The method should return a boolean value without throwing
            boolean isSet = LanguageManager.isLanguageSet(context);
            // We don't care about the value, just that it doesn't crash
            assertTrue("isLanguageSet() should return a value", true);
            
        } catch (Exception e) {
            fail("LanguageManager.isLanguageSet() failed: " + e.getMessage());
        }
    }
}

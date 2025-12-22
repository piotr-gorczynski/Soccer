package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for Android level preference handling
 * Tests that the android level setting is correctly stored and retrieved
 */
@RunWith(AndroidJUnit4.class)
public class AndroidLevelPreferenceTest {

    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Clear any existing preferences
        prefs.edit().clear().commit();
    }

    @Test
    public void testDefaultAndroidLevel() {
        // When no preference is set, should default to "0" (Easy)
        String defaultLevel = prefs.getString("android_level", "0");
        assertEquals("0", defaultLevel);
        assertEquals(0.0, Double.parseDouble(defaultLevel), 0.001);
    }

    @Test
    public void testSetAndroidLevelEasy() {
        // Test setting Easy level (0 seconds)
        prefs.edit().putString("android_level", "0").commit();
        
        String level = prefs.getString("android_level", "0");
        assertEquals("0", level);
        assertEquals(0.0, Double.parseDouble(level), 0.001);
    }

    @Test
    public void testSetAndroidLevelMedium() {
        // Test setting Medium level (3 seconds)
        prefs.edit().putString("android_level", "3").commit();
        
        String level = prefs.getString("android_level", "0");
        assertEquals("3", level);
        assertEquals(3.0, Double.parseDouble(level), 0.001);
    }

    @Test
    public void testSetAndroidLevelHard() {
        // Test setting Hard level (10 seconds)
        prefs.edit().putString("android_level", "10").commit();
        
        String level = prefs.getString("android_level", "0");
        assertEquals("10", level);
        assertEquals(10.0, Double.parseDouble(level), 0.001);
    }

    @Test
    public void testAndroidLevelPersistence() {
        // Test that the preference persists across multiple reads
        prefs.edit().putString("android_level", "3").commit();
        
        // First read
        String level1 = prefs.getString("android_level", "0");
        assertEquals("3", level1);
        
        // Second read without modifying
        String level2 = prefs.getString("android_level", "0");
        assertEquals("3", level2);
        
        // Both should be the same
        assertEquals(level1, level2);
    }

    @Test
    public void testAndroidLevelUpdate() {
        // Test updating from one level to another
        prefs.edit().putString("android_level", "0").commit();
        assertEquals("0", prefs.getString("android_level", "0"));
        
        // Update to medium
        prefs.edit().putString("android_level", "3").commit();
        assertEquals("3", prefs.getString("android_level", "0"));
        
        // Update to hard
        prefs.edit().putString("android_level", "10").commit();
        assertEquals("10", prefs.getString("android_level", "0"));
    }

    @Test
    public void testInvalidAndroidLevelHandling() {
        // Test that invalid values can be detected
        prefs.edit().putString("android_level", "invalid").commit();
        
        String level = prefs.getString("android_level", "0");
        assertEquals("invalid", level);
        
        // Verify that parseDouble would throw an exception (as handled in GameActivity)
        try {
            Double.parseDouble(level);
            fail("Expected NumberFormatException for invalid level");
        } catch (NumberFormatException e) {
            // Expected - this is what GameActivity catches
            assertTrue(true);
        }
    }

    @Test
    public void testPreferenceStringResourceExists() {
        // Verify that the preference string resources exist
        try {
            int titleId = context.getResources().getIdentifier(
                "pref_title_level", "string", context.getPackageName());
            assertTrue("pref_title_level should be defined", titleId != 0);
            
            String title = context.getString(titleId);
            assertNotNull("pref_title_level should not be null", title);
            assertFalse("pref_title_level should not be empty", title.trim().isEmpty());
        } catch (Exception e) {
            fail("Android level preference strings are not properly defined: " + e.getMessage());
        }
    }

    @Test
    public void testPreferenceLevelValuesExist() {
        // Verify that the level values array exists
        try {
            int arrayId = context.getResources().getIdentifier(
                "pref_level_values", "array", context.getPackageName());
            assertTrue("pref_level_values array should be defined", arrayId != 0);
            
            String[] values = context.getResources().getStringArray(arrayId);
            assertNotNull("pref_level_values should not be null", values);
            assertEquals("Should have 3 level values", 3, values.length);
            assertEquals("First level should be 0", "0", values[0]);
            assertEquals("Second level should be 3", "3", values[1]);
            assertEquals("Third level should be 10", "10", values[2]);
        } catch (Exception e) {
            fail("Android level values array is not properly defined: " + e.getMessage());
        }
    }

    @Test
    public void testPreferenceLevelTitlesExist() {
        // Verify that the level titles array exists
        try {
            int arrayId = context.getResources().getIdentifier(
                "pref_level_titles", "array", context.getPackageName());
            assertTrue("pref_level_titles array should be defined", arrayId != 0);
            
            String[] titles = context.getResources().getStringArray(arrayId);
            assertNotNull("pref_level_titles should not be null", titles);
            assertEquals("Should have 3 level titles", 3, titles.length);
            assertFalse("First title (Easy) should not be empty", titles[0].trim().isEmpty());
            assertFalse("Second title (Medium) should not be empty", titles[1].trim().isEmpty());
            assertFalse("Third title (Hard) should not be empty", titles[2].trim().isEmpty());
        } catch (Exception e) {
            fail("Android level titles array is not properly defined: " + e.getMessage());
        }
    }
}

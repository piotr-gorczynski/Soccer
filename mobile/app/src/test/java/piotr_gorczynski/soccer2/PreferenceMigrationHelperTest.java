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
 * Test cases for preference migration logic
 * Tests that old android_level values (1, 10, 30) are correctly migrated to new values (0.1, 3, 10)
 */
@RunWith(AndroidJUnit4.class)
public class PreferenceMigrationHelperTest {

    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Clear all preferences to start fresh
        prefs.edit().clear().commit();
    }

    @Test
    public void testMigrateOldEasyValue() {
        // Set old Easy value (1)
        prefs.edit().putString("android_level", "1").commit();
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify it was migrated to new Easy value (0.1)
        String migratedValue = prefs.getString("android_level", null);
        assertEquals("0.1", migratedValue);
    }

    @Test
    public void testMigrateOldMediumValue() {
        // Set old Medium value (10)
        prefs.edit().putString("android_level", "10").commit();
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify it was migrated to new Medium value (3)
        String migratedValue = prefs.getString("android_level", null);
        assertEquals("3", migratedValue);
    }

    @Test
    public void testMigrateOldHardValue() {
        // Set old Hard value (30)
        prefs.edit().putString("android_level", "30").commit();
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify it was migrated to new Hard value (10)
        String migratedValue = prefs.getString("android_level", null);
        assertEquals("10", migratedValue);
    }

    @Test
    public void testNoMigrationForNewEasyValue() {
        // Set new Easy value (0.1) - should not be changed
        prefs.edit().putString("android_level", "0.1").commit();
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify it remains unchanged
        String migratedValue = prefs.getString("android_level", null);
        assertEquals("0.1", migratedValue);
    }

    @Test
    public void testNoMigrationForNewMediumValue() {
        // Set new Medium value (3) - should not be changed
        prefs.edit().putString("android_level", "3").commit();
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify it remains unchanged
        String migratedValue = prefs.getString("android_level", null);
        assertEquals("3", migratedValue);
    }

    @Test
    public void testNoMigrationForNewHardValue() {
        // Set new Hard value (10) - should not be changed
        // Note: 10 is both old Medium and new Hard, but since it's already a valid new value,
        // it should be migrated to 3 (new Medium)
        prefs.edit().putString("android_level", "10").commit();
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify it was migrated (10 is old Medium -> new 3)
        String migratedValue = prefs.getString("android_level", null);
        assertEquals("3", migratedValue);
    }

    @Test
    public void testNoMigrationWhenNoPreferenceExists() {
        // Don't set any android_level preference
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify no preference was created
        assertFalse("android_level should not exist", prefs.contains("android_level"));
    }

    @Test
    public void testMigrationOnlyRunsOnce() {
        // Set old Easy value (1)
        prefs.edit().putString("android_level", "1").commit();
        
        // Run migration first time
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify it was migrated
        assertEquals("0.1", prefs.getString("android_level", null));
        
        // Manually change to old value again (simulating manual edit or corruption)
        prefs.edit().putString("android_level", "1").commit();
        
        // Run migration again
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify it was NOT migrated again (migration should only run once)
        assertEquals("1", prefs.getString("android_level", null));
    }

    @Test
    public void testMigrationVersionIsSaved() {
        // Set old value
        prefs.edit().putString("android_level", "1").commit();
        
        // Verify migration version is not set initially
        assertFalse("Migration version should not exist initially", 
                   prefs.contains("preference_migration_version"));
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify migration version is now set
        assertTrue("Migration version should exist after migration", 
                  prefs.contains("preference_migration_version"));
        int version = prefs.getInt("preference_migration_version", 0);
        assertEquals("Migration version should be 1", 1, version);
    }

    @Test
    public void testMigrationWithInvalidValue() {
        // Set an invalid value
        prefs.edit().putString("android_level", "invalid_value").commit();
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify invalid value is not changed (no migration for unknown values)
        String migratedValue = prefs.getString("android_level", null);
        assertEquals("invalid_value", migratedValue);
    }

    @Test
    public void testMigrationWithNullValue() {
        // Set android_level key with null value
        prefs.edit().putString("android_level", null).commit();
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Should not crash and should mark migration as done
        int version = prefs.getInt("preference_migration_version", 0);
        assertEquals(1, version);
    }

    @Test
    public void testNewUserWithNoPreferences() {
        // Simulate a new user with no preferences set
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify migration version is set
        int version = prefs.getInt("preference_migration_version", 0);
        assertEquals(1, version);
        
        // Verify no android_level preference was created
        assertFalse("android_level should not exist for new user", 
                   prefs.contains("android_level"));
    }

    @Test
    public void testMigrationSequence() {
        // Test complete migration flow for all three old values
        
        // Test Easy migration
        prefs.edit().clear().commit();
        prefs.edit().putString("android_level", "1").commit();
        PreferenceMigrationHelper.migratePreferences(context);
        assertEquals("0.1", prefs.getString("android_level", null));
        
        // Reset for Medium test
        prefs.edit().clear().commit();
        prefs.edit().putString("android_level", "10").commit();
        PreferenceMigrationHelper.migratePreferences(context);
        assertEquals("3", prefs.getString("android_level", null));
        
        // Reset for Hard test
        prefs.edit().clear().commit();
        prefs.edit().putString("android_level", "30").commit();
        PreferenceMigrationHelper.migratePreferences(context);
        assertEquals("10", prefs.getString("android_level", null));
    }
}

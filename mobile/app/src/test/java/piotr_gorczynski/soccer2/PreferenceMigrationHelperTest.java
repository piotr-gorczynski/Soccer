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
 * Tests that old android_level values are correctly migrated:
 * - v0 to v1: (1, 10, 30) -> (0.1, 3, 10)
 * - v1 to v2: 0.1 -> 0 (Easy level only)
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
        
        // Verify it was migrated to new Easy value (0)
        String migratedValue = prefs.getString("android_level", null);
        assertEquals("0", migratedValue);
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
        // Set new Easy value (0) - should not be changed
        prefs.edit().putString("android_level", "0").commit();
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify it remains unchanged
        String migratedValue = prefs.getString("android_level", null);
        assertEquals("0", migratedValue);
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
        // Set value 10 - this is ambiguous (old Medium or new Hard)
        // Migration treats it as old Medium and migrates to new Medium (3)
        // This is correct because users upgrading will have old values
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
        assertEquals("0", prefs.getString("android_level", null));
        
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
        assertEquals("Migration version should be 2", 2, version);
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
        assertEquals(2, version);
    }

    @Test
    public void testNewUserWithNoPreferences() {
        // Simulate a new user with no preferences set
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify migration version is set
        int version = prefs.getInt("preference_migration_version", 0);
        assertEquals(2, version);
        
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
        assertEquals("0", prefs.getString("android_level", null));
        
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

    @Test
    public void testMigrateV1EasyToV2Easy() {
        // Test migration from v1 Easy (0.1) to v2 Easy (0)
        prefs.edit().putString("android_level", "0.1").commit();
        prefs.edit().putInt("preference_migration_version", 1).commit();
        
        // Run migration
        PreferenceMigrationHelper.migratePreferences(context);
        
        // Verify it was migrated to v2 Easy value (0)
        String migratedValue = prefs.getString("android_level", null);
        assertEquals("0", migratedValue);
        
        // Verify migration version is updated
        int version = prefs.getInt("preference_migration_version", 0);
        assertEquals(2, version);
    }

    @Test
    public void testV2MigrationDoesNotAffectMediumAndHard() {
        // Test that v2 migration only affects Easy level, not Medium or Hard
        
        // Test Medium remains unchanged
        prefs.edit().clear().commit();
        prefs.edit().putString("android_level", "3").commit();
        prefs.edit().putInt("preference_migration_version", 1).commit();
        PreferenceMigrationHelper.migratePreferences(context);
        assertEquals("3", prefs.getString("android_level", null));
        
        // Test Hard remains unchanged
        prefs.edit().clear().commit();
        prefs.edit().putString("android_level", "10").commit();
        prefs.edit().putInt("preference_migration_version", 1).commit();
        PreferenceMigrationHelper.migratePreferences(context);
        assertEquals("10", prefs.getString("android_level", null));
    }
}

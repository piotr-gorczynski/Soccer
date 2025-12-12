package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.preference.PreferenceManager;

/**
 * Helper class to migrate old preference values to new ones.
 * 
 * Version 1: Handles migration from old android_level values (1, 10, 30) to new values (0.1, 3, 10).
 * Version 2: Handles migration from v1 Easy value (0.1) to v2 Easy value (0).
 * This ensures users who upgrade from an older version see the correct difficulty level
 * selected in the settings and the game uses the correct time values.
 */
public class PreferenceMigrationHelper {
    
    private static final String TAG = "TAG_Soccer";
    private static final String PREF_MIGRATION_VERSION = "preference_migration_version";
    private static final int CURRENT_MIGRATION_VERSION = 2;
    
    // Old values: Easy=1, Medium=10, Hard=30
    // New values (v1): Easy=0.1, Medium=3, Hard=10
    // New values (v2): Easy=0, Medium=3, Hard=10
    private static final String OLD_EASY = "1";
    private static final String OLD_MEDIUM = "10";
    private static final String OLD_HARD = "30";
    
    private static final String V1_EASY = "0.1";
    private static final String NEW_EASY = "0";
    private static final String NEW_MEDIUM = "3";
    private static final String NEW_HARD = "10";
    
    /**
     * Migrate old preference values to new ones if needed.
     * This method checks if migration is needed and performs it once per version.
     * 
     * @param context Application context
     */
    public static void migratePreferences(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Check if migration has already been done for this version
        int migrationVersion = prefs.getInt(PREF_MIGRATION_VERSION, 0);
        if (migrationVersion >= CURRENT_MIGRATION_VERSION) {
            Log.d(TAG, "PreferenceMigrationHelper.migratePreferences: Already migrated to version " + migrationVersion);
            return;
        }
        
        Log.d(TAG, "PreferenceMigrationHelper.migratePreferences: Starting migration from version " + migrationVersion + " to " + CURRENT_MIGRATION_VERSION);
        
        // Migrate android_level preference
        if (migrationVersion < 1) {
            migrateAndroidLevelV1(prefs);
        }
        if (migrationVersion < 2) {
            migrateAndroidLevelV2(prefs);
        }
        
        // Mark migration as complete
        prefs.edit().putInt(PREF_MIGRATION_VERSION, CURRENT_MIGRATION_VERSION).apply();
        
        Log.d(TAG, "PreferenceMigrationHelper.migratePreferences: Migration complete");
    }
    
    /**
     * Migrate android_level preference from old values to new values (v1).
     * Old: Easy=1, Medium=10, Hard=30
     * New: Easy=0.1, Medium=3, Hard=10
     * 
     * @param prefs SharedPreferences instance
     */
    private static void migrateAndroidLevelV1(SharedPreferences prefs) {
        if (!prefs.contains("android_level")) {
            Log.d(TAG, "PreferenceMigrationHelper.migrateAndroidLevelV1: No android_level preference found, skipping migration");
            return;
        }
        
        String currentValue = prefs.getString("android_level", null);
        if (currentValue == null) {
            Log.d(TAG, "PreferenceMigrationHelper.migrateAndroidLevelV1: android_level is null, skipping migration");
            return;
        }
        
        String newValue = null;
        switch (currentValue) {
            case OLD_EASY:
                newValue = V1_EASY;
                Log.d(TAG, "PreferenceMigrationHelper.migrateAndroidLevelV1: Migrating Easy level from " + OLD_EASY + " to " + V1_EASY);
                break;
            case OLD_MEDIUM:
                newValue = NEW_MEDIUM;
                Log.d(TAG, "PreferenceMigrationHelper.migrateAndroidLevelV1: Migrating Medium level from " + OLD_MEDIUM + " to " + NEW_MEDIUM);
                break;
            case OLD_HARD:
                newValue = NEW_HARD;
                Log.d(TAG, "PreferenceMigrationHelper.migrateAndroidLevelV1: Migrating Hard level from " + OLD_HARD + " to " + NEW_HARD);
                break;
            default:
                Log.d(TAG, "PreferenceMigrationHelper.migrateAndroidLevelV1: Current value '" + currentValue + "' does not need migration");
                break;
        }
        
        if (newValue != null) {
            prefs.edit().putString("android_level", newValue).apply();
            Log.d(TAG, "PreferenceMigrationHelper.migrateAndroidLevelV1: Successfully migrated android_level to " + newValue);
        }
    }
    
    /**
     * Migrate android_level preference from v1 Easy value to v2 Easy value.
     * Old: Easy=0.1
     * New: Easy=0
     * 
     * @param prefs SharedPreferences instance
     */
    private static void migrateAndroidLevelV2(SharedPreferences prefs) {
        if (!prefs.contains("android_level")) {
            Log.d(TAG, "PreferenceMigrationHelper.migrateAndroidLevelV2: No android_level preference found, skipping migration");
            return;
        }
        
        String currentValue = prefs.getString("android_level", null);
        if (currentValue == null) {
            Log.d(TAG, "PreferenceMigrationHelper.migrateAndroidLevelV2: android_level is null, skipping migration");
            return;
        }
        
        if (V1_EASY.equals(currentValue)) {
            prefs.edit().putString("android_level", NEW_EASY).apply();
            Log.d(TAG, "PreferenceMigrationHelper.migrateAndroidLevelV2: Migrated Easy level from " + V1_EASY + " to " + NEW_EASY);
        } else {
            Log.d(TAG, "PreferenceMigrationHelper.migrateAndroidLevelV2: Current value '" + currentValue + "' does not need migration");
        }
    }
}

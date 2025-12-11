# Manual Verification Test for Preference Migration

## Test Scenario: Upgrading User with Old Preferences

### Setup
To manually verify the migration works correctly, you can simulate an upgrade scenario:

1. **Install old version** (or manually set old preference values)
2. **Set old preference value** using adb:
   ```bash
   # Set Easy level with old value (1)
   adb shell "run-as piotr_gorczynski.soccer2 sh -c 'echo \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\" standalone=\\\"yes\\\" ?><map><string name=\\\"android_level\\\">1</string></map>\" > /data/data/piotr_gorczynski.soccer2/shared_prefs/piotr_gorczynski.soccer2_preferences.xml'"
   
   # Or for Medium (10)
   adb shell "run-as piotr_gorczynski.soccer2 sh -c 'echo \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\" standalone=\\\"yes\\\" ?><map><string name=\\\"android_level\\\">10</string></map>\" > /data/data/piotr_gorczynski.soccer2/shared_prefs/piotr_gorczynski.soccer2_preferences.xml'"
   
   # Or for Hard (30)
   adb shell "run-as piotr_gorczynski.soccer2 sh -c 'echo \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\" standalone=\\\"yes\\\" ?><map><string name=\\\"android_level\\\">30</string></map>\" > /data/data/piotr_gorczynski.soccer2/shared_prefs/piotr_gorczynski.soccer2_preferences.xml'"
   ```

3. **Launch the app** with the new version that includes the migration
4. **Check logs** to verify migration occurred:
   ```bash
   adb logcat -s TAG_Soccer:D | grep -i "migration\|android_level"
   ```
   
   Expected log output:
   ```
   PreferenceMigrationHelper.migratePreferences: Starting migration from version 0 to 1
   PreferenceMigrationHelper.migrateAndroidLevel: Migrating Easy level from 1 to 0.1
   PreferenceMigrationHelper.migrateAndroidLevel: Successfully migrated android_level to 0.1
   PreferenceMigrationHelper.migratePreferences: Migration complete
   ```

5. **Open Settings** and verify:
   - The correct difficulty level is selected (Easy/Medium/Hard)
   - The selection matches the user's previous choice

6. **Start a game** against Android and verify:
   - The Android AI uses the correct timeout (0.1, 3, or 10 seconds)
   - Game performance matches the selected difficulty

### Expected Results

| Old Value | Old Level | New Value | New Level | Settings UI | Game Behavior |
|-----------|-----------|-----------|-----------|-------------|---------------|
| 1         | Easy      | 0.1       | Easy      | ✓ Easy selected | AI thinks for 0.1s |
| 10        | Medium    | 3         | Medium    | ✓ Medium selected | AI thinks for 3s |
| 30        | Hard      | 10        | Hard      | ✓ Hard selected | AI thinks for 10s |

### New User Test

1. **Fresh install** (clear app data)
   ```bash
   adb shell pm clear piotr_gorczynski.soccer2
   ```

2. **Launch app**
3. **Check logs** - should see:
   ```
   PreferenceMigrationHelper.migratePreferences: Starting migration from version 0 to 1
   PreferenceMigrationHelper.migrateAndroidLevel: No android_level preference found, skipping migration
   PreferenceMigrationHelper.migratePreferences: Migration complete
   ```

4. **Open Settings** - Easy should be selected by default (0.1)

### Migration Idempotency Test

1. Set old value and launch app (migration runs)
2. Close app
3. Manually change to an old value again
4. Launch app again
5. Check logs - should see:
   ```
   PreferenceMigrationHelper.migratePreferences: Already migrated to version 1
   ```
6. The old value should remain unchanged (migration doesn't run again)

This confirms migration only runs once per user, as intended.

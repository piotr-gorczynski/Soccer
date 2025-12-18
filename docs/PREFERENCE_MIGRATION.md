# Android Level Preference Migration

## Problem
Users upgrading from an older version of the app had difficulty level preferences stored with old values:
- Easy: 1 second
- Medium: 10 seconds  
- Hard: 30 seconds

The new version uses different values:
- Easy: 0 seconds
- Medium: 3 seconds
- Hard: 10 seconds

This caused two issues:
1. **Settings UI**: No difficulty level appeared selected because the stored value (e.g., "1") didn't match any of the new values in the dropdown (0, 3, 10)
2. **Game**: The game loaded the old timeout values, giving users the wrong difficulty experience

## Solution
Implemented a preference migration system that runs once on app startup:

### Components

#### PreferenceMigrationHelper
A helper class that:
- Detects old preference values (1, 10, 30)
- Translates them to new values (0.1, 3, 10)
- Uses a version flag (`preference_migration_version`) to ensure migration only runs once
- Logs all migration activity for debugging

#### Integration in SoccerApp
The migration is called early in `SoccerApp.onCreate()` before any UI is shown, ensuring:
- Values are migrated before GameActivity reads them
- Settings UI displays the correct selection
- New users without old preferences are unaffected

### Migration Logic
```
Old Value → New Value
1         → 0    (Easy: 1 second → 0 seconds)
10        → 3    (Medium: 10 seconds → 3 seconds) 
30        → 10   (Hard: 30 seconds → 10 seconds)
```

### Testing
Comprehensive unit tests verify:
- ✅ Old values are correctly migrated
- ✅ New values are not changed
- ✅ Migration only runs once
- ✅ New users (no preferences) work correctly
- ✅ Invalid values don't crash the migration
- ✅ Migration version is saved correctly

### Files Changed
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/PreferenceMigrationHelper.java` - Migration logic
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/SoccerApp.java` - Integration point
- `mobile/app/src/test/java/piotr_gorczynski/soccer2/PreferenceMigrationHelperTest.java` - Unit tests

### Future Considerations
- If preference values change again in the future, increment `CURRENT_MIGRATION_VERSION` and add new migration logic
- The version flag ensures each migration runs exactly once per user
- Old migration code can be removed after several releases when no users have the old values

# Bangladesh Migration Feature - Testing Guide

## Overview

This document explains how to test the Bangladesh migration feature that was implemented to promote the Bangladesh-specific version of Gridline Soccer to users in Bangladesh.

## Feature Description

The Bangladesh migration promotion feature:
- **Controlled by a global switch** – `BANGLADESH_PROMO_ENABLED` in `BangladeshMigrationHelper.java` must be `true` for any promotion to appear. **It is currently `false`** until `piotr_gorczynski.soccer2.bd` is published on the Play Store.
- **Only shows in the global app flavor** (`piotr_gorczynski.soccer2`)
- **Only shows to users in Bangladesh** (based on device locale)
- Displays an age-neutral dialog informing users about the Bangladesh version
- Provides options to:
  - Learn More - Shows detailed information
  - Install - Opens Play Store to install Bangladesh version
  - Maybe Later - Dismisses for 7 days
- After 3 dismissals, the promotion is permanently hidden
- Clicking "Install" permanently hides the promotion

## Implementation Files

### Core Classes
- `BangladeshMigrationHelper.java` - Main helper class with region detection and promotion logic
- `MenuActivity.java` - Integrated promotion check in `continueOnResumeAfterBackendCheck()`
- `AnalyticsManager.java` - Added analytics tracking methods

### Resources
- `res/values/strings.xml` - English strings
- `res/values-bn/strings.xml` - Bengali translations

### Tests
- `BangladeshMigrationHelperTest.java` - Unit tests for the helper class

## Testing with Poland (or Any Other Country)

Since the feature is designed for Bangladesh users only, you may want to test it from Poland (or your country) during development. Here's how:

### Step 0: Enable the Global Switch

The promotion is disabled by default. Before any other testing step, enable it:

**File:** `mobile/app/src/main/java/piotr_gorczynski/soccer2/BangladeshMigrationHelper.java`

```java
// Change from:
public static final boolean BANGLADESH_PROMO_ENABLED = false;

// To:
public static final boolean BANGLADESH_PROMO_ENABLED = true;
```

**Important:** This must be reverted to `false` before committing to production!

### Option 1: Temporary Code Change (Recommended for Quick Testing)

**File:** `mobile/app/src/main/java/piotr_gorczynski/soccer2/BangladeshMigrationHelper.java`

**Line to change:** Around line 48

```java
// Original (production code):
return "BD".equals(countryCode);

// For testing in Poland, change to:
return "PL".equals(countryCode);

// For testing in any country, change to:
return true;  // Always return true for testing
```

**Important:** Remember to change this back to `"BD"` before committing to production!

### Option 2: Reset Promotion State for Testing

If you've already dismissed the promotion and want to see it again:

Add this temporary code in `MenuActivity.onCreate()`:

```java
// TEMPORARY: Reset Bangladesh promotion for testing
if (BuildConfig.DEBUG) {
    BangladeshMigrationHelper.resetPromotionState(this);
}
```

This will reset all promotion preferences on every app launch in debug builds.

### Option 3: Use ADB to Change Device Locale (Advanced)

You can temporarily change your device's locale to Bangladesh:

```bash
# Change locale to Bangladesh
adb shell "setprop persist.sys.locale bn_BD; setprop ctl.restart zygote"

# Change back to Poland
adb shell "setprop persist.sys.locale pl_PL; setprop ctl.restart zygote"
```

**Warning:** This will restart your device and may affect all apps.

## Testing Checklist

### 1. Test in Global Flavor
- [ ] Build and run the **global** flavor: `_devGlobalDebug`
- [ ] Verify promotion dialog appears on MenuActivity
- [ ] Test "Learn More" button shows info dialog
- [ ] Test "Install" button opens Play Store
- [ ] Test "Maybe Later" dismisses dialog
- [ ] Verify dialog reappears after 7 days (or manipulate timestamp)
- [ ] Verify dialog stops appearing after 3 dismissals

### 2. Test in Bangladesh Flavor
- [ ] Build and run the **bangladesh** flavor: `_devBangladeshDebug`
- [ ] Verify promotion dialog **NEVER** appears
- [ ] This is critical - the Bangladesh app should not promote itself!

### 3. Test Analytics
- [ ] Check Firebase Analytics console for these events:
  - `bd_promo_viewed` - When dialog is shown
  - `bd_promo_clicked` - When user clicks any button
    - Check parameter: `action` = "install", "learn_more", "maybe_later", "install_from_info"

### 4. Test Localization
- [ ] Change device language to Bengali
- [ ] Verify all strings are properly translated
- [ ] Verify Bengali text displays correctly (no encoding issues)

## Play Store Link

The promotion links to the Bangladesh version on Play Store:
```
https://play.google.com/store/apps/details?id=piotr_gorczynski.soccer2.bd
```

When testing, this will:
- Open the Play Store app if installed
- Show "App not found" if the Bangladesh version hasn't been published yet
- This is expected during development

## Dismissal Logic

The promotion can be in these states:

1. **Never Shown** - Will show on next MenuActivity resume
2. **Shown, Not Dismissed** - Currently showing
3. **Dismissed 1-2 Times** - Will show again after 7 days
4. **Dismissed 3 Times** - Permanently hidden
5. **Accepted (Install clicked)** - Permanently hidden (dismiss count = 999)

## Debugging Tips

### Check Current State

To see the current promotion state, check SharedPreferences:

```bash
adb shell "run-as piotr_gorczynski.soccer2 cat /data/data/piotr_gorczynski.soccer2/shared_prefs/piotr_gorczynski.soccer2_preferences.xml"
```

Look for these keys:
- `bd_promo_dismissed` - Boolean, true if dismissed
- `bd_promo_last_shown_ms` - Timestamp of last showing
- `bd_promo_dismiss_count` - Number of times dismissed (0-3, or 999 if accepted)

### Enable Verbose Logging

All Bangladesh migration code logs to LogCat with tag `BangladeshMigration`. Filter by:

```bash
adb logcat | grep BangladeshMigration
```

You'll see logs like:
```
Device country code: PL
Not showing promo: user not in Bangladesh
Should show Bangladesh promotion
Marked promotion as shown
Promotion dismissed (count: 1/3)
```

## Important Notes

⚠️ **Before Committing:**
1. Ensure `BANGLADESH_PROMO_ENABLED` is set back to `false`
2. Ensure `isUserInBangladesh()` returns `"BD".equals(countryCode)` (not hardcoded true or other country)
3. Remove any temporary testing code (reset promotion state, etc.)
4. Verify tests pass
5. Test in **both** global and Bangladesh flavors

✅ **Production Requirements:**
- Feature only visible in global flavor
- Feature only visible to Bangladesh users
- No cash prize mentions (age-neutral messaging)
- Links to Play Store where age gate (18+) is enforced
- Analytics properly tracking user interactions

## Contact

For questions about this feature, refer to:
- Issue: "Implement user migration strategy"
- Design Doc: `docs/USER_MIGRATION_STRATEGY.md`

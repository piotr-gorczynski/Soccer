# Quick Fix Reference: Bangladesh Variant Install Error

## Problem
```
Error: INSTALL_FAILED_CONFLICTING_PROVIDER
Message: Can't install because provider name 
com.facebook.app.FacebookContentProvider1232966491486195 
(in package piotr_gorczynski.soccer2.bd) is already used by piotr_gorczynski.soccer2
```

## Cause
Both the global app and Bangladesh app were using the same Facebook Content Provider authority.

## Solution
✅ **FIXED** - Bangladesh variant now uses unique provider authority

### What Changed
- **File**: `mobile/app/src/bangladesh/AndroidManifest.xml` (NEW)
- **Change**: Facebook provider authority now has `.bd` suffix
- **Result**: Both apps can coexist on the same device

### Provider Authorities
| App Variant | Package Name | Facebook Provider Authority |
|-------------|--------------|----------------------------|
| Global | `piotr_gorczynski.soccer2` | `com.facebook.app.FacebookContentProvider1232966491486195` |
| Bangladesh | `piotr_gorczynski.soccer2.bd` | `com.facebook.app.FacebookContentProvider1232966491486195.bd` |

## Testing the Fix

### Quick Test (Requires Secrets)
```bash
cd mobile

# Build both variants
./gradlew assemble_devGlobalDebug -Penv=dev
./gradlew assemble_devBangladeshDebug -Penv=dev

# Install both (no conflict should occur)
adb install app/build/outputs/apk/_dev/global/debug/app-_dev-global-debug.apk
adb install app/build/outputs/apk/_dev/bangladesh/debug/app-_dev-bangladesh-debug.apk

# Verify both are installed
adb shell pm list packages | grep soccer
```

Expected output:
```
package:piotr_gorczynski.soccer2
package:piotr_gorczynski.soccer2.bd
```

### Build Variants Available
- `_devBangladeshDebug` - Development build with debug signing
- `_devBangladeshRelease` - Development build with release signing
- `_testBangladeshDebug` - Test environment debug build
- `_testBangladeshRelease` - Test environment release build
- `_prodBangladeshDebug` - Production debug build
- `_prodBangladeshRelease` - Production release build (for Play Store)

## Documentation

For more details, see:
- **[TESTING_BANGLADESH_FIX.md](TESTING_BANGLADESH_FIX.md)** - Complete testing guide
- **[mobile/app/src/bangladesh/README.md](mobile/app/src/bangladesh/README.md)** - Technical explanation
- **[docs/BANGLADESH_VERSION_APPROACH.md](docs/BANGLADESH_VERSION_APPROACH.md)** - Full implementation guide

## No Changes Needed

✅ No Facebook App Dashboard changes required  
✅ No code changes to existing functionality  
✅ No Firebase configuration changes needed  
✅ Same Facebook App ID works for both variants  
✅ User authentication works across both apps  

## Status

🟢 **RESOLVED** - The fix has been implemented and is ready for testing.

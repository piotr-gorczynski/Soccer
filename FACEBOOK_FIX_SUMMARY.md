# Facebook Login Key Hash Issue - Solution Summary

## Problem
Facebook login works with `_prodDebug` builds but fails with `_prodRelease` builds, showing this error:
```
Invalid key hash. The key hash [SOME_HASH] does not match any stored key hashes.
```

## Root Cause
- Debug builds use Android's default debug keystore
- Release builds use your production keystore (configured in `secrets/keystore.properties`)
- Facebook requires ALL signing certificate key hashes to be registered
- Most developers only register the debug keystore hash during development

## Solution

### 1. Generate Key Hashes (Automatic)
```bash
cd mobile
./gradlew generateFacebookKeyHashes
```

This will output something like:
```
🔑 Facebook Key Hash Generator
==================================================
✅ DEBUG keystore hash: ABC123def456...
   Location: /home/user/.android/debug.keystore
✅ RELEASE keystore hash: XYZ789ghi012...
   Location: /path/to/secrets/keystore.jks
```

### 2. Add Hashes to Facebook
1. Go to [Facebook App Dashboard](https://developers.facebook.com/apps/1232966491486195)
2. Navigate to **Settings** → **Basic**
3. Scroll down to **Key Hashes** field
4. Add **both** hashes (one per line)
5. Click **Save Changes**

### 3. Test Both Build Types
```bash
# Test debug build
./gradlew assemble_prodDebug
# Install APK and test Facebook login

# Test release build  
./gradlew assemble_prodRelease
# Install APK and test Facebook login
```

## Enhanced Features Added

1. **Automatic Key Hash Generation**: New Gradle task handles the complex crypto operations
2. **Enhanced Error Logging**: Better error messages when key hash issues occur
3. **Runtime Build Detection**: App logs which build type is running for easier debugging
4. **Comprehensive Documentation**: Step-by-step setup guide in `docs/FACEBOOK_SETUP.md`

## Files Modified
- `mobile/app/build.gradle` - Added `generateFacebookKeyHashes` task
- `docs/FACEBOOK_SETUP.md` - Comprehensive Facebook setup guide
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/SoccerApp.java` - Enhanced logging
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/UniversalLoginActivity.java` - Better error handling
- `README.md` - Added quick reference to the fix

## Quick Fix Summary
**For immediate resolution**: Run `./gradlew generateFacebookKeyHashes` and add the output hashes to your Facebook App Dashboard.
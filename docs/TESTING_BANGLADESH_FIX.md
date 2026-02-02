# How to Test the Bangladesh Variant Fix

This guide explains how to test the fix for the `INSTALL_FAILED_CONFLICTING_PROVIDER` error.

## Prerequisites

Before you can build and test the Bangladesh variant, you need:

1. **Secret files** in the `secrets/` directory (not included in this repository):
   - `soccer_secret_key` - Backend API secret key
   - `facebook_client_token` - Facebook App client token
   - `google-services.dev.json` - Firebase configuration for dev environment
   - `google-services.test.json` - Firebase configuration for test environment
   - `google-services.prod.json` - Firebase configuration for prod environment
   - `keystore.properties` - Release signing configuration (optional for debug builds)

2. **Android development environment**:
   - Android Studio or Gradle command line
   - Android SDK installed
   - Java 17 or later

## Testing the Fix

### Option 1: Using an Android Device or Emulator (Recommended)

1. **Build the global app** (if not already installed):
   ```bash
   cd mobile
   ./gradlew assemble_devGlobalDebug -Penv=dev
   ```

2. **Install the global app**:
   ```bash
   adb install app/build/outputs/apk/_dev/global/debug/app-_dev-global-debug.apk
   ```

3. **Build the Bangladesh app**:
   ```bash
   ./gradlew assemble_devBangladeshDebug -Penv=dev
   ```

4. **Install the Bangladesh app** (this should succeed without the conflict error):
   ```bash
   adb install app/build/outputs/apk/_dev/bangladesh/debug/app-_dev-bangladesh-debug.apk
   ```

5. **Verify both apps are installed**:
   ```bash
   adb shell pm list packages | grep soccer
   ```
   
   You should see:
   ```
   package:piotr_gorczynski.soccer2
   package:piotr_gorczynski.soccer2.bd
   ```

### Option 2: Verify Manifest Merge (Without Building)

If you don't have the secret files, you can still verify the manifest merge logic is correct:

1. **Check the Bangladesh AndroidManifest**:
   ```bash
   cat mobile/app/src/bangladesh/AndroidManifest.xml
   ```

2. **Look for the provider declaration** with `.bd` suffix:
   ```xml
   <provider
       android:name="com.facebook.FacebookContentProvider"
       android:authorities="com.facebook.app.FacebookContentProvider1232966491486195.bd"
       android:exported="true"
       tools:replace="android:authorities" />
   ```

3. **Compare with the main AndroidManifest**:
   ```bash
   cat mobile/app/src/main/AndroidManifest.xml | grep FacebookContentProvider
   ```
   
   You should see the original authority without the `.bd` suffix:
   ```xml
   <provider
       android:name="com.facebook.FacebookContentProvider"
       android:authorities="com.facebook.app.FacebookContentProvider1232966491486195"
       android:exported="true" />
   ```

## Expected Results

### Before the Fix
When trying to install the Bangladesh app while the global app is installed:
```
Failure [INSTALL_FAILED_CONFLICTING_PROVIDER: Scanning Failed.: 
Can't install because provider name com.facebook.app.FacebookContentProvider1232966491486195 
(in package piotr_gorczynski.soccer2.bd) is already used by piotr_gorczynski.soccer2]
```

### After the Fix
The Bangladesh app installs successfully, and both apps can coexist on the same device.

## Troubleshooting

### Build Fails with "soccer_secret_key not found"
This is expected if you don't have the secret files. The secret files are stored in a private repository and are not included in the main codebase for security reasons.

**To resolve**: Contact the repository owner to obtain the secret files, or create your own Firebase project and secret files for testing.

### Facebook Login Doesn't Work
If Facebook Login fails in the Bangladesh app, you may need to:

1. **Add the Bangladesh package to Facebook App**:
   - Go to [Facebook App Dashboard](https://developers.facebook.com/apps/1232966491486195)
   - Settings → Basic → Android platform
   - Add package name: `piotr_gorczynski.soccer2.bd`

2. **Generate and add key hashes**:
   ```bash
   cd mobile
   ./gradlew generateFacebookKeyHashes
   ```
   
   Copy the generated hashes and add them to the Facebook App Dashboard.

### Both Apps Still Conflict
If you still see the conflict error after applying the fix:

1. **Uninstall both apps**:
   ```bash
   adb uninstall piotr_gorczynski.soccer2
   adb uninstall piotr_gorczynski.soccer2.bd
   ```

2. **Clean and rebuild**:
   ```bash
   cd mobile
   ./gradlew clean
   ./gradlew assemble_devGlobalDebug -Penv=dev
   ./gradlew assemble_devBangladeshDebug -Penv=dev
   ```

3. **Reinstall both apps** following the steps in Option 1 above.

## Additional Resources

- [Bangladesh Product Flavor README](mobile/app/src/bangladesh/README.md) - Detailed explanation of the fix
- [BANGLADESH_VERSION_APPROACH.md](docs/BANGLADESH_VERSION_APPROACH.md) - Complete implementation guide
- [Facebook Setup Documentation](https://developers.facebook.com/docs/facebook-login/android) - Official Facebook Login guide

## Questions?

If you encounter issues not covered in this guide, please:
1. Check the error message carefully
2. Review the documentation links above
3. Contact the development team for assistance

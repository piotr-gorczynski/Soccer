# Facebook SDK Configuration

To enable Facebook login functionality in the Soccer app, you need to configure both the Facebook Client Token and the Key Hashes for your signing certificates.

## Current Status

The app has been configured with:
- Facebook App ID: `1232966491486195` (already configured)
- Facebook Client Token: `CLIENT_TOKEN_TO_BE_CONFIGURED` (needs to be replaced)
- Key Hashes: **REQUIRED** - Need to be added to Facebook App Dashboard

## Quick Setup Guide

### Step 1: Configure Client Token

#### Option 1: Using Secrets Directory (Recommended)

1. Go to the [Facebook App Dashboard](https://developers.facebook.com/apps/)
2. Select your app (ID: 1232966491486195)
3. Navigate to **Settings** → **Advanced**
4. Copy the **Client Token** value
5. Create a file `facebook_client_token` in the `secrets/` directory at the repository root
6. Put the client token value in this file (one line, no extra whitespace)

#### Option 2: Using strings.xml (Fallback)

1. Replace `CLIENT_TOKEN_TO_BE_CONFIGURED` in `/mobile/app/src/main/res/values/strings.xml`:

```xml
<string name="facebook_client_token" translatable="false">YOUR_ACTUAL_CLIENT_TOKEN_HERE</string>
```

### Step 2: Configure Key Hashes (Critical for Release Builds)

Key hashes are required for Facebook login to work with your signed APK. Different build types use different signing certificates:

- **Debug builds** (`_prodDebug`): Use Android's default debug keystore
- **Release builds** (`_prodRelease`): Use your production keystore

#### Generate Key Hashes

Run this Gradle task to automatically generate key hashes for both debug and release keystores:

```bash
cd mobile
./gradlew generateFacebookKeyHashes
```

This will output something like:
```
🔑 Facebook Key Hash Generator
==================================================
✅ DEBUG keystore hash: ABC123...
   Location: /home/user/.android/debug.keystore
✅ RELEASE keystore hash: XYZ789...
   Location: /path/to/secrets/keystore.jks
```

#### Add Key Hashes to Facebook

1. Go to [Facebook App Dashboard](https://developers.facebook.com/apps/1232966491486195)
2. Navigate to **Settings** → **Basic**
3. Scroll down to **Key Hashes** field
4. Add **both** the debug and release key hashes (one per line)
5. Click **Save Changes**

## Troubleshooting

### Facebook Login Works in Debug but Fails in Release

**Symptoms:**
- Login works with `_prodDebug` flavor
- Login fails with `_prodRelease` flavor showing "Invalid key hash" error

**Solution:**
This happens when only the debug key hash is registered with Facebook. Follow Step 2 above to add your release keystore hash.

### "Invalid key hash" Error

**Error Message:**
```
Invalid key hash. The key hash [SOME_HASH] does not match any stored key hashes.
```

**Solutions:**
1. **Generate current key hash:** Run `./gradlew generateFacebookKeyHashes`
2. **Check Facebook Dashboard:** Ensure the generated hash matches what's in your Facebook app settings
3. **Add missing hash:** If the hash from the error doesn't match any in Facebook, add it to the Key Hashes field

### Key Hash Not Generated

If `generateFacebookKeyHashes` fails:

1. **For debug keystore:** Ensure Android Studio has been run at least once to generate the debug keystore
2. **For release keystore:** Ensure `secrets/keystore.properties` exists and points to a valid keystore file

### Manual Key Hash Generation

If the Gradle task doesn't work, you can generate key hashes manually:

```bash
# For debug keystore (password: android)
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | openssl sha1 -binary | openssl base64

# For release keystore
keytool -exportcert -alias YOUR_KEY_ALIAS -keystore path/to/your/keystore.jks -storepass YOUR_STORE_PASSWORD | openssl sha1 -binary | openssl base64
```

## Verification

After configuration, verify the setup:

1. **Client Token:** Check app logs for:
   - `Using Facebook client token from assets` or `Using Facebook client token from strings.xml`
   - `Facebook SDK initialized`

2. **Key Hashes:** Test Facebook login with both debug and release builds

3. **Full Integration Test:**
   ```bash
   # Test debug build
   ./gradlew assemble_prodDebug
   # Install and test Facebook login
   
   # Test release build  
   ./gradlew assemble_prodRelease
   # Install and test Facebook login
   ```

## Security Notes

- **Client Token:** Considered public information, safe to include in app binary
- **Key Hashes:** Public information derived from signing certificates
- **App Secret:** Never include in client-side code (not used in this setup)
- **Keystore passwords:** Keep in `secrets/keystore.properties`, excluded from Git

## Alternative Configuration

If you prefer not to use Facebook login, you can remove the Facebook initialization code entirely from `SoccerApp.java` without affecting other app functionality.
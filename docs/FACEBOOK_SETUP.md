# Facebook SDK Configuration

To enable Facebook login functionality in the Soccer app, you need to configure the Facebook Client Token.

## Current Status

The app has been configured with:
- Facebook App ID: `1232966491486195` (already configured)
- Facebook Client Token: `CLIENT_TOKEN_TO_BE_CONFIGURED` (needs to be replaced)

## How to Configure

### Option 1: Using Secrets Directory (Recommended for Development)

1. Go to the [Facebook App Dashboard](https://developers.facebook.com/apps/)
2. Select your app (ID: 1232966491486195)
3. Navigate to **Settings** → **Advanced**
4. Copy the **Client Token** value
5. Create a file `facebook_client_token` in the `secrets/` directory at the repository root
6. Put the client token value in this file (one line, no extra whitespace)
7. During build, the token will be automatically copied to the app's assets folder

### Option 2: Using strings.xml (Fallback)

If the secrets file is not available, the app will fall back to using the token from strings.xml:

1. Follow steps 1-4 from Option 1
2. Replace `CLIENT_TOKEN_TO_BE_CONFIGURED` in `/mobile/app/src/main/res/values/strings.xml`:

```xml
<string name="facebook_client_token" translatable="false">YOUR_ACTUAL_CLIENT_TOKEN_HERE</string>
```

## Verification

After configuration, the app logs will show:
- `Using Facebook client token from assets` - if loaded from secrets file
- `Using Facebook client token from strings.xml` - if loaded from strings.xml fallback
- `Facebook SDK initialized` - if successful
- `Facebook SDK not configured; skipping initialization` - if still missing configuration

## Alternative Configuration

If you prefer not to use Facebook login, you can remove the Facebook initialization code entirely from `SoccerApp.java` without affecting other app functionality.

## Security Note

The Client Token is considered public information and is safe to include in the app binary, unlike the App Secret which should never be included in client-side code. However, using the secrets directory approach prevents the token from being exposed in the public repository source code.
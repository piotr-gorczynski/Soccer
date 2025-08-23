# Facebook SDK Configuration

To enable Facebook login functionality in the Soccer app, you need to configure the Facebook Client Token.

## Current Status

The app has been configured with:
- Facebook App ID: `1232966491486195` (already configured)
- Facebook Client Token: `CLIENT_TOKEN_TO_BE_CONFIGURED` (needs to be replaced)

## How to Configure

1. Go to the [Facebook App Dashboard](https://developers.facebook.com/apps/)
2. Select your app (ID: 1232966491486195)
3. Navigate to **Settings** → **Advanced**
4. Copy the **Client Token** value
5. Replace `CLIENT_TOKEN_TO_BE_CONFIGURED` in `/mobile/app/src/main/res/values/strings.xml`:

```xml
<string name="facebook_client_token" translatable="false">YOUR_ACTUAL_CLIENT_TOKEN_HERE</string>
```

## Verification

After configuration, the app logs will show:
- `Facebook SDK initialized` - if successful
- `Facebook SDK not configured; skipping initialization` - if still missing configuration

## Alternative Configuration

If you prefer not to use Facebook login, you can remove the Facebook initialization code entirely from `SoccerApp.java` without affecting other app functionality.

## Security Note

The Client Token is considered public information and is safe to include in the app binary, unlike the App Secret which should never be included in client-side code.
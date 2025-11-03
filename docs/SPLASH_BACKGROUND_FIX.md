# Fix for App Crash on Startup

## Issue
After PR #714, the app was crashing on startup with the following error:

```
Caused by: org.xmlpull.v1.XmlPullParserException: Binary XML file line #7: <bitmap> requires a valid 'src' attribute
```

This error occurred in `res/drawable/splash_background.xml`.

## Root Cause
The `splash_background.xml` file was using `@mipmap/ic_launcher` as the source for a bitmap drawable:

```xml
<bitmap
    android:gravity="center"
    android:src="@mipmap/ic_launcher"/>
```

On Android 8.0+ (API 26+), `@mipmap/ic_launcher` resolves to an **adaptive icon** XML file (defined in `mipmap-anydpi-v26/ic_launcher.xml`), not a bitmap image. The `<bitmap>` drawable cannot use an adaptive icon XML as its source, which caused the crash.

## Solution
Changed the reference from `@mipmap/ic_launcher` to `@mipmap/ic_launcher_foreground`:

```xml
<bitmap
    android:gravity="center"
    android:src="@mipmap/ic_launcher_foreground"/>
```

The `ic_launcher_foreground` is an actual bitmap image (WebP format) that exists in all density folders:
- `mipmap-mdpi/ic_launcher_foreground.webp`
- `mipmap-hdpi/ic_launcher_foreground.webp`
- `mipmap-xhdpi/ic_launcher_foreground.webp`
- `mipmap-xxhdpi/ic_launcher_foreground.webp`
- `mipmap-xxxhdpi/ic_launcher_foreground.webp`

This ensures the splash screen displays correctly on all Android versions and device densities.

## Files Changed
- `mobile/app/src/main/res/drawable/splash_background.xml` - Changed bitmap source reference

## Testing
Created `SplashBackgroundDrawableTest.java` to verify:
1. The splash_background drawable can be loaded without crashes
2. The ic_launcher_foreground resource exists
3. The colorGreenDark background color exists

## Related Issues
- Fixes crash mentioned in issue regarding PR #709/714
- Stack trace: https://github.com/user-attachments/files/23318295/stack.txt

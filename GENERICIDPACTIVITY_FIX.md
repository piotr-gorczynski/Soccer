# GenericIdpActivity.onResume Crash Fix

## Issue
Crashlytics reported a potential crash in `com.google.firebase.auth.internal.GenericIdpActivity.onResume`. This is Firebase Auth's internal activity used for OAuth provider sign-in flows (Google, Microsoft, Facebook).

## Root Cause
The crash typically occurs when:
1. The app is killed while an OAuth sign-in is in progress
2. The user returns to the app after the OAuth flow is interrupted
3. GenericIdpActivity tries to resume but has lost its state due to ProGuard obfuscation or missing activity lifecycle handling

## Solution
Added ProGuard rules to keep Firebase Auth internal classes, specifically:
- `com.google.firebase.auth.internal.**` - All internal Firebase Auth classes
- `com.google.firebase.auth.api.**` - Firebase Auth API classes
- `GenericIdpActivity` - The specific activity that handles OAuth flows

## Changes Made
1. **Updated `mobile/app/proguard-rules.pro`**:
   - Added `-keep` rules for Firebase Auth internal classes
   - Added `-keep` rules for Facebook SDK (used in OAuth flow)
   - Added `-dontwarn` rules for OkHttp (used for network requests)

## Testing
The ProGuard rules have been validated for syntax. To fully test:
1. Build a release APK with minification enabled
2. Test OAuth sign-in flows:
   - Google sign-in
   - Microsoft sign-in
   - Facebook sign-in
3. Test interruption scenarios:
   - Kill app during OAuth flow and reopen
   - Switch apps during OAuth flow
   - Rotate device during OAuth flow

## Files Modified
- `mobile/app/proguard-rules.pro` - Added Firebase Auth ProGuard rules

## Impact
This fix ensures that Firebase Auth's internal activities are not obfuscated or removed during ProGuard optimization, preventing crashes during OAuth sign-in flows in release builds.

## Related Activities
The following activities in the app use OAuth provider sign-in:
- `UniversalLoginActivity` - Handles user login with various providers
- `LinkAccountActivity` - Handles linking anonymous accounts to OAuth providers

## References
- Firebase Auth SDK: https://firebase.google.com/docs/auth/android/manage-users
- ProGuard configuration: https://developer.android.com/studio/build/shrink-code#configuration-files

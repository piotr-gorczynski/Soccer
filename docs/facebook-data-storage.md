# Facebook Data Storage Implementation

This document explains the implementation of Facebook user data storage in the Firestore users collection.

## Overview

When a user logs in using Facebook, the app now extracts and stores Facebook-specific data in the Firestore `users/{id}` document. The following fields are stored:

- `facebookId`: The user's Facebook ID
- `facebookName`: The user's display name from Facebook  
- `facebookPhotoUrl`: The URL to the user's Facebook profile photo

## Implementation Details

### Modified Methods

1. **`FirebaseAuthManager.loginWithFacebookToken()`** - Primary Facebook login method
2. **`FirebaseAuthManager.loginWithProvider()`** - Generic OAuth method enhanced for Facebook

### Data Extraction

The Facebook data is extracted from Firebase's `UserInfo` provider data:

```java
if (authResult.getUser() != null) {
    for (com.google.firebase.auth.UserInfo profile : authResult.getUser().getProviderData()) {
        if ("facebook.com".equals(profile.getProviderId())) {
            facebookId = profile.getUid();           // Facebook ID
            facebookName = profile.getDisplayName(); // Name  
            if (profile.getPhotoUrl() != null) {
                facebookPhotoUrl = profile.getPhotoUrl().toString(); // Profile pic
            }
            break;
        }
    }
}
```

### Firestore Storage

The extracted data is stored in the Firestore document using `SetOptions.merge()`:

```java
Map<String, Object> data = new HashMap<>();
// ... other user data ...

// Add Facebook-specific data
if (finalFacebookId != null) data.put("facebookId", finalFacebookId);
if (finalFacebookName != null) data.put("facebookName", finalFacebookName);
if (finalFacebookPhotoUrl != null) data.put("facebookPhotoUrl", finalFacebookPhotoUrl);

db.collection("users").document(uid).set(data, SetOptions.merge());
```

## Null Handling

The implementation includes proper null checks to ensure only valid data is stored:
- Only non-null Facebook IDs are stored
- Only non-null Facebook names are stored  
- Only non-null profile photo URLs are stored
- Photo URL extraction includes additional null check for `profile.getPhotoUrl()`

## Debugging

Debug logging has been added to track the extracted Facebook data:

```
TAG_Soccer: FirebaseAuthManager.loginWithFacebookToken: extracted Facebook data - 
ID: 123456789, Name: John Doe, Photo: https://example.com/photo.jpg
```

## Flow

1. User clicks Facebook login button
2. Facebook SDK authenticates user
3. Firebase Auth signs in with Facebook credential
4. App extracts Facebook provider data from Firebase User
5. Facebook data is stored in Firestore users/{id} document
6. Login process continues normally

## Backward Compatibility

This implementation is fully backward compatible:
- Existing users without Facebook data are unaffected
- Users who previously logged in with Facebook will have their data extracted on next login
- The implementation uses `SetOptions.merge()` to avoid overwriting existing user data
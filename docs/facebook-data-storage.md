# Facebook Data Storage Implementation

This document explains the implementation of Facebook user data storage in the Firestore users collection.

## Overview

When a user logs in using Facebook, the app now extracts and stores Facebook-specific data in the Firestore `users/{id}` document. The following fields are stored:

- `facebookId`: The user's Facebook ID
- `facebookName`: The user's display name from Facebook  
- `facebookPhotoUrl`: **The real CDN URL** to the user's Facebook profile photo (not the generic graph.facebook.com URL)

## Facebook Photo URL Fix

**Problem**: Previously, the app stored `https://graph.facebook.com/{facebookId}/picture` as the photo URL, which returned a silhouette/blank image for users with non-public profile photos when accessed without authentication.

**Solution**: The app now uses the Facebook access token to fetch the real photo URL via Facebook Graph API:

```
GET https://graph.facebook.com/v20.0/{facebookId}/picture?type=large&redirect=0&access_token={ACCESS_TOKEN}
```

This returns:
```json
{
  "data": {
    "is_silhouette": false,
    "url": "https://scontent.xx.fbcdn.net/v/t1.6435-1/..."
  }
}
```

The app stores `data.url` (the real CDN URL) instead of the generic graph.facebook.com URL.

## Implementation Details

### Modified Methods

1. **`FirebaseAuthManager.loginWithFacebookToken()`** - Primary Facebook login method
2. **`FirebaseAuthManager.fetchRealFacebookPhotoUrl()`** - New method to fetch real photo URL using access token

### Data Extraction

The Facebook data is extracted from Firebase's `UserInfo` provider data:

```java
if (authResult.getUser() != null) {
    for (com.google.firebase.auth.UserInfo profile : authResult.getUser().getProviderData()) {
        if ("facebook.com".equals(profile.getProviderId())) {
            facebookId = profile.getUid();           // Facebook ID
            facebookName = profile.getDisplayName(); // Name  
            // Photo URL is fetched separately using Graph API
            break;
        }
    }
}
```

### Photo URL Fetching

```java
private CompletableFuture<String> fetchRealFacebookPhotoUrl(String facebookId, String accessToken) {
    // Makes HTTP request to Facebook Graph API
    // Parses JSON response to extract real photo URL
    // Returns CompletableFuture for async execution
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

## Error Handling

- If the Graph API call fails, the login process continues without storing a photo URL
- Uses fallback URL if the response indicates a silhouette image
- All errors are logged for debugging
- Asynchronous execution prevents UI blocking

## Null Handling

The implementation includes proper null checks to ensure only valid data is stored:
- Only non-null Facebook IDs are stored
- Only non-null Facebook names are stored  
- Only non-null and non-silhouette profile photo URLs are stored
- Photo URL extraction includes additional null check for access token

## Debugging

Debug logging has been added to track the extracted Facebook data:

```
TAG_Soccer: FirebaseAuthManager.loginWithFacebookToken: extracted Facebook data - 
ID: 123456789, Name: John Doe, Photo: https://scontent.xx.fbcdn.net/v/t1.6435-1/...
```

Additional logging for Graph API calls:
```
TAG_Soccer: FirebaseAuthManager.fetchRealFacebookPhotoUrl: Got real photo URL: https://scontent.xx.fbcdn.net/...
```

## Flow

1. User clicks Facebook login button
2. Facebook SDK authenticates user and provides access token
3. Firebase Auth signs in with Facebook credential
4. App extracts Facebook provider data from Firebase User
5. **App uses access token to fetch real photo URL from Facebook Graph API**
6. Facebook data (including real photo URL) is stored in Firestore users/{id} document
7. Login process continues normally

## Backward Compatibility

This implementation is fully backward compatible:
- Existing users without Facebook data are unaffected
- Users who previously logged in with Facebook will have their real photo URL fetched on next login
- The implementation uses `SetOptions.merge()` to avoid overwriting existing user data
- If Graph API call fails, login still succeeds (just without photo URL)
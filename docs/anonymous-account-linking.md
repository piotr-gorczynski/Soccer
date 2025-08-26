# Anonymous Account Linking Feature

## Overview

This feature allows users with anonymous Firebase accounts to link their account with email, Google, or Microsoft credentials using Firebase's `linkWithCredential` functionality. This ensures that anonymous users can secure their account and preserve their game history.

## Implementation

### UI Changes

1. **AccountActivity.java**: Added a "Link to registered account" button that is only visible for anonymous users
2. **LinkAccountActivity.java**: New activity that provides options to link with Email, Google, Microsoft, or Facebook
3. **Layout files**: Added `activity_link_account.xml` with buttons for different linking options

### Backend Changes

1. **FirebaseAuthManager.java**: Added new linking methods:
   - `linkWithEmailPassword()`: Links anonymous account with email/password credentials
   - `linkWithProvider()`: Links anonymous account with OAuth providers (Google, Microsoft)
   - `updateUserDocumentAfterLink()`: Updates Firestore user document after successful linking

### String Resources

Added translations for the linking functionality in all supported languages:
- English (default)
- Spanish (es)
- German (de)
- French (fr)
- Polish (pl)
- Hindi (hi)
- Bengali (bn)
- Urdu (ur)
- Nepali (ne)

## User Flow

1. Anonymous user goes to Account page
2. Sees "Link to registered account" button
3. Clicks button to open LinkAccountActivity
4. Chooses linking method (Email, Google, Microsoft)
5. Completes authentication with chosen provider
6. Account is linked, method is updated in Firestore
7. User returns to Account page with updated information

## Data Preservation

When linking accounts, the system:
- Preserves existing user data (nickname, game history, etc.)
- Updates the authentication method from "anonymous" to the chosen provider
- Merges data using Firebase Firestore `SetOptions.merge()`
- Updates local SharedPreferences with new account information

## Error Handling

The implementation handles common scenarios:
- Account collision (when linking credentials already exist for another account)
- Network errors during linking process
- Authentication failures
- Firebase Auth errors

## Security Considerations

- Only anonymous users can access the linking functionality
- The original anonymous account UID is preserved
- Existing user data is not lost during the linking process
- Firebase handles the secure credential linking process

## Future Enhancements

- Facebook linking can be implemented with proper Facebook SDK integration
- Additional OAuth providers can be added following the same pattern
- Account merging logic for handling collisions with existing accounts
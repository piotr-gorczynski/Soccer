# Backend Availability Safeguards Implementation

## Issue Summary
When the backend is unavailable, the app was attempting to make Firestore calls with `Source.SERVER`, resulting in `FirebaseFirestoreException` errors. The errors occurred specifically in:
- `MenuActivity.ensureTermsAccepted()` - trying to fetch terms acceptance status
- `MenuActivity.fetchNicknameFromFirestore()` - trying to fetch user nickname

**Root Cause**: Race condition in `onResume()` method where `checkBackendAvailability()` was called asynchronously, but authentication and Firestore operations proceeded immediately before the backend availability check could complete.

## Solution Implemented

### Changes Made

#### 1. Fixed Race Condition in `onResume()` Method

**Before (Problematic)**:
```java
@Override
protected void onResume() {
    // ... language check ...
    
    checkBackendAvailability();  // ← Asynchronous call
    
    // Firestore operations happened immediately here
    ensureTermsAccepted(uid);
    fetchNicknameFromFirestore(uid, prefs, pickNick);
}
```

**After (Fixed)**:
```java
@Override
protected void onResume() {
    // ... language check ...
    
    checkBackendAvailabilityAndContinue();  // ← Will continue with auth logic in callback
    
    // FCM token sync only (no Firestore calls)
    ((SoccerApp) getApplication()).syncFcmTokenIfNeeded();
}
```

#### 2. Created `checkBackendAvailabilityAndContinue()` Method

**Purpose**: Performs backend availability check and continues with authentication logic in the callback to ensure proper sequencing.

```java
private void checkBackendAvailabilityAndContinue() {
    // ... backend availability check ...
    
    serviceChecker.checkServiceAvailability(new BackendServiceChecker.ServiceCheckCallback() {
        @Override
        public void onServiceAvailable() {
            // Set isBackendAvailable = true
            // Continue with authentication logic
            continueOnResumeAfterBackendCheck();
        }

        @Override
        public void onServiceUnavailable(String reason) {
            // Set isBackendAvailable = false  
            // Still continue with authentication logic (safeguards will handle it)
            continueOnResumeAfterBackendCheck();
        }
    });
}
```

#### 3. Created `continueOnResumeAfterBackendCheck()` Method

**Purpose**: Contains all the authentication and UI logic that should only run after the backend availability check is complete.

```java
private void continueOnResumeAfterBackendCheck() {
    // All the original authentication logic from onResume:
    // - Firebase auth checks
    // - ensureTermsAccepted(uid)  ← NOW properly guarded
    // - fetchNicknameFromFirestore(uid, prefs, pickNick)  ← NOW properly guarded
    // - UI updates
    // - checkForActiveMatch()
}
```

#### 4. Existing Safeguards in Backend Methods (Already Present)

**File**: `mobile/app/src/main/java/piotr_gorczynski/soccer2/MenuActivity.java`

```java
private void ensureTermsAccepted(@NonNull String uid) {
    // Skip terms check if backend is unavailable to prevent Firestore errors
    if (!isBackendAvailable) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".ensureTermsAccepted: Skipping terms check - backend unavailable");
        return;
    }
    
    // Original Firestore call continues...
}

private void fetchNicknameFromFirestore(@NonNull String uid,
                                        @NonNull SharedPreferences prefs,
                                        @NonNull Runnable onMissing) {

    // Skip nickname fetch if backend is unavailable to prevent Firestore errors
    if (!isBackendAvailable) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".fetchNicknameFromFirestore: Skipping nickname fetch - backend unavailable");
        // If we have no local nickname and backend is unavailable, run onMissing callback
        String localNickname = prefs.getString("nickname", null);
        if (localNickname == null || localNickname.trim().isEmpty()) {
            onMissing.run();
        }
        return;
    }

    // Original Firestore call continues...
}
```

### Integration with Existing Backend Availability Tracking

The solution leverages the existing backend availability infrastructure:

1. **Backend Availability Flag**: Uses the existing `isBackendAvailable` field in `MenuActivity`
2. **Backend Service Checker**: Integrates with `BackendServiceChecker` class that monitors backend status
3. **SoccerApp Integration**: Works with `SoccerApp.isBackendAvailable()` method

### Impact on App Flow

#### When Backend is Available (Normal Operation)
- No changes to existing behavior
- Terms checking and nickname fetching work as before
- Full server synchronization maintained

#### When Backend is Unavailable (Safeguarded Operation)
- **Terms Check**: Skipped - user can continue using app without terms validation
- **Nickname Fetch**: Skipped - but if no local nickname exists, user is still prompted to create one
- **Error Prevention**: No more `FirebaseFirestoreException` for network unavailability
- **User Experience**: App remains functional in offline/backend-unavailable scenarios

### Error Scenarios Addressed

The following error scenarios from the original log are now prevented:

```
MenuActivity.ensureTermsAccepted: failed
com.google.firebase.firestore.FirebaseFirestoreException: Failed to get document from server.
```

```  
MenuActivity.fetchNicknameFromFirestore: failed
com.google.firebase.firestore.FirebaseFirestoreException: Failed to get document from server.
```

### Testing

#### Test Coverage Added
- Updated `BackendAvailabilitySafeguardTest.java` with validation for sequencing fix
- Created verification script that validates:
  - Required string resources exist
  - Layout resources are properly configured
  - Backend availability infrastructure is in place
  - Firestore classes are available
  - New sequencing methods exist and are properly called
  - Backend availability check happens before authentication operations

#### Manual Testing Scenarios
1. **Backend Available**: Verify normal terms and nickname functionality
2. **Backend Unavailable**: Verify no Firestore exceptions occur
3. **Network Transitions**: Verify app handles backend availability changes gracefully

### Minimal Change Approach

The implementation follows minimal change principles:
- ✅ Only modified the specific problematic methods
- ✅ Preserved all existing functionality when backend is available
- ✅ Added minimal safeguard checks without architectural changes
- ✅ Maintained existing error handling for other scenarios
- ✅ No deletion of working code
- ✅ Fixed race condition with proper sequencing

### Future Enhancements

While this fix addresses the immediate issue, potential future improvements could include:
- Enhanced offline capability with local caching
- User notification when backend becomes available again
- Retry mechanisms for critical operations
- More granular backend service availability checking

## Verification

The changes can be verified by:
1. Running the app when backend is unavailable
2. Checking logs for the safeguard messages
3. Confirming no `FirebaseFirestoreException` errors occur for these methods
4. Ensuring app functionality is preserved when backend is available
5. Running the verification script: `/tmp/verify_safeguards.sh`

## Summary

**Problem**: Race condition causing `FirebaseFirestoreException` when backend unavailable
**Solution**: Proper sequencing of backend availability check before authentication operations
**Result**: No more Firestore exceptions, graceful handling of backend unavailability
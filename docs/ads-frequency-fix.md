# Ads Frequency Fix - Issue #479

## Problem Description
Ads were not showing on some devices due to incorrect frequency logic. The issue was that unauthorized users were using `DEFAULT_AD_FREQUENCY = 10` instead of `FAILSAFE_AD_FREQUENCY = 1`, meaning they would only see ads after 10 actions instead of after every action.

## Root Causes Identified
1. **Incorrect frequency for unauthorized users**: The code used `DEFAULT_AD_FREQUENCY (10)` for both authorized and unauthorized users
2. **Inconsistent consent checking**: Consent was checked in individual action methods but not validated right before showing ads
3. **Poor error handling**: Firebase failures didn't have proper fallbacks for authorized users
4. **Insufficient logging**: Hard to diagnose why ads weren't showing on different devices

## Solution Implemented

### Key Changes
1. **Fixed frequency logic**:
   - Unauthorized users: `FAILSAFE_AD_FREQUENCY = 1` (ads show after every action)
   - Authorized users: Fetch from Firebase or fallback to `DEFAULT_AD_FREQUENCY = 10`

2. **Centralized consent checking**:
   - Moved consent validation to `showAdThenRun` method
   - Removed duplicate consent checks from action methods
   - Double-check consent right before showing ads

3. **Enhanced logging**:
   - Log authorization status, frequency values, and ad display decisions
   - Help diagnose issues on different devices

4. **Improved code structure**:
   - Split complex logic into `showAdThenRun` and `processAdLogic` methods
   - Better error handling for Firebase failures

### Code Flow
```
Action Method (e.g., OpenGamePlayerVsPlayer)
    ↓
showAdThenRun()
    ↓
Check consent (if no consent → show dialog + run action)
    ↓
Determine user authorization status
    ↓
If authorized: Fetch frequency from Firebase
If unauthorized: Use FAILSAFE_AD_FREQUENCY = 1
    ↓
processAdLogic()
    ↓
Check counter vs frequency
    ↓
If counter < frequency: Increment counter + run action
If counter >= frequency: Reset counter + show ad + run action
```

## Expected Behavior After Fix

### For Unauthorized Users
- **Frequency**: `FAILSAFE_AD_FREQUENCY = 1`
- **Behavior**: Ads show after every action (assuming consent is given)
- **Logs**: `"unauthorized user, using failsafe frequency=1"`

### For Authorized Users  
- **Frequency**: Value from Firebase `settings/adsFreuency` document, or fallback to `DEFAULT_AD_FREQUENCY = 10`
- **Behavior**: Ads show based on Firebase configuration
- **Logs**: `"refreshed ads frequency=X"` or `"failed to refresh ads frequency for authorized user, defaulting to 10"`

### Consent Handling
- **No consent**: Show consent dialog + run action without ad
- **Has consent**: Proceed with normal ad logic
- **Lost consent**: Show consent dialog + run action without ad
- **Logs**: `"No ads consent, running action directly"` or `"Lost ads consent, running action directly"`

## Testing Instructions

### Manual Testing
1. **Test unauthorized user**:
   - Launch app without logging in
   - Perform actions (e.g., "Practice with Android") 
   - **Expected**: Ad should show after every action (frequency = 1)
   - **Log to look for**: `"User authorized=false, default frequency=1"`

2. **Test authorized user**:
   - Log in to the app
   - Perform actions multiple times
   - **Expected**: Ad shows based on Firebase frequency (default 10)
   - **Log to look for**: `"User authorized=true, default frequency=10"` and `"refreshed ads frequency=X"`

3. **Test consent scenarios**:
   - Revoke ads consent in settings
   - Perform actions
   - **Expected**: Consent dialog shows, actions execute without ads
   - **Log to look for**: `"No ads consent, running action directly"`

### Debugging Logs
Key logs to monitor in logcat with tag `TAG_Soccer`:
```
MenuActivity.showAdThenRun: User authorized=false, default frequency=1, stored frequency=1
MenuActivity.showAdThenRun: unauthorized user, using failsafe frequency=1
MenuActivity.processAdLogic: counter=1, frequency=1, should show ad=true
MenuActivity.processAdLogic: Ad ready=true, consent=true
MenuActivity.processAdLogic: Showing interstitial ad
```

## Files Changed
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/MenuActivity.java` - Main fix
- `mobile/app/src/test/java/piotr_gorczynski/soccer2/AdsFrequencyTest.java` - Test validation

## Backward Compatibility
- ✅ Existing authorized users: Will continue to use Firebase frequency or default to 10
- ✅ Existing preferences: Preserved and used appropriately
- ✅ Consent settings: Remain unchanged and are properly respected
- ✅ Ad loading: Uses existing ad loading mechanism without changes
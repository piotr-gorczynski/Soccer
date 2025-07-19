# Backend Service Availability Check Implementation

## Overview
This implementation adds backend service availability checking to the Soccer mobile app, ensuring that all buttons are properly enabled/disabled based on the backend service status.

## Components Added

### 1. BackendServiceChecker.java
- Utility class that makes HTTP requests to the `service-check` cloud function
- Handles retries for network failures
- Supports configuration of project ID and secret key
- Default URL pattern: `https://us-central1-{project-id}.cloudfunctions.net/service-check`

### 2. SoccerApp Modifications
- Added backend service checking when app returns to foreground (`onStart` method)
- Maintains global backend availability state
- Configures default project ID on app initialization

### 3. MenuActivity Modifications
- Checks backend availability on activity creation and resume
- Disables ALL buttons when backend is unavailable
- Shows toast message: "We are sorry, but the Soccer server is not available at the moment..."
- Preserves existing login/logout logic when backend is available

## Configuration

### Project ID
- Default: "soccer-dev-1744877837" (matching the Cloud Build environment)
- Can be configured via `BackendServiceChecker.setProjectId(String)`
- Stored in SharedPreferences for persistence

### Secret Key (Optional)
- Can be configured via `BackendServiceChecker.setSecretKey(String)`
- If not set, requests are made without X-Secret-Key header
- Required for full authentication with the service-check function

## UI Behavior

### When Backend is Available
- All buttons follow normal login/logout logic
- Feature buttons (invite, tournaments, pending) enabled only when logged in
- Account button always enabled, shows "Log in" or "Log out" accordingly

### When Backend is Unavailable
- ALL buttons disabled and dimmed (alpha 0.3)
- Account button shows "Server Unavailable" 
- Toast notification displayed to user
- No button clicks are processed

## Testing

### Manual Testing
- Long press the Settings button to trigger a manual backend test
- Check logs for test results
- Look for "TAG_Soccer" logs for detailed information

### Debug Methods
- `SoccerApp.debugTestBackendService()` - Manual test trigger
- `BackendServiceChecker.testServiceCheck()` - Direct service test

## Dependencies Added
- `com.squareup.okhttp3:okhttp:4.12.0` for HTTP requests

## Files Modified
1. `mobile/app/build.gradle` - Added OkHttp dependency
2. `mobile/app/src/main/java/piotr_gorczynski/soccer2/SoccerApp.java` - Added service checking
3. `mobile/app/src/main/java/piotr_gorczynski/soccer2/MenuActivity.java` - Added UI state management
4. `mobile/app/src/main/res/values/strings.xml` - Added "server_unavailable" string
5. `mobile/app/src/main/res/layout/activity_menu.xml` - Added ID to Settings button

## Files Added
1. `mobile/app/src/main/java/piotr_gorczynski/soccer2/BackendServiceChecker.java` - Core service checker

## Configuration Options
The implementation can be enhanced by:
1. Adding UI in Settings activity to configure project ID
2. Adding secure storage for secret keys
3. Adding more sophisticated retry logic
4. Adding periodic background checks
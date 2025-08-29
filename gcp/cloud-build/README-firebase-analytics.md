# Firebase Analytics Deployment

This directory contains scripts to enable and test Firebase Analytics for the Soccer project.

## Files

### `deploy_firebase_analytics.yaml`
Cloud Build script that enables Firebase Analytics for the project.

**Features:**
- Checks if Firebase Analytics is already enabled before attempting to enable it
- Enables required API (`firebase.googleapis.com`)
- Creates Google Analytics property and links it to Firebase
- Provides comprehensive error handling for various scenarios
- Verifies successful enablement

**Usage:**
```bash
gcloud builds submit --config gcp/cloud-build/deploy_firebase_analytics.yaml
```

### `test_firebase_analytics.yaml`
Test script that validates Firebase Analytics configuration.

**Features:**
 - Checks project accessibility
 - Validates required API is enabled
 - Tests Analytics configuration endpoints
 - Provides diagnostic information

**Usage:**
```bash
gcloud builds submit --config gcp/cloud-build/test_firebase_analytics.yaml
```

## Integration with Mobile App

The mobile app includes `AnalyticsManager.java` which uses the Firebase Analytics SDK:

```java
// Example usage in the mobile app
AnalyticsManager analytics = ((SoccerApp) getApplicationContext()).getAnalyticsManager();
analytics.trackLoginScreenOpened();
analytics.trackSignupSuccess("google");
analytics.trackTournamentJoinStart("tournament_123", true);
```

### Analytics Events Tracked

The mobile app tracks various user events including:

#### Authentication Events
- Login screen opens
- Signup success/failure with method tracking
- Anonymous user linking prompts and decisions

#### Tournament Events
- Tournament list views
- Tournament join attempts (start/success/failure)

#### User Properties
- Authentication method
- App version
- Language preference
- Nickname status

## Deployment Process

1. **Enable Analytics**: Run `deploy_firebase_analytics.yaml`
2. **Test Configuration**: Run `test_firebase_analytics.yaml`
3. **Verify in Console**: Check Firebase Console > Analytics section
4. **Mobile App**: Analytics events will automatically be collected once enabled

## Error Handling

The deployment script handles common scenarios:
- Analytics already enabled (HTTP 409/400)
- Missing APIs (automatically enables them)
- Permissions issues (clear error messages)
- Network/authentication failures

## Dependencies

- Firebase project must exist
- Cloud Build service account needs appropriate permissions
- Firebase APIs must be available in the project region

## Troubleshooting

If Analytics enablement fails:
1. Check if the Firebase project exists and is accessible
2. Verify Cloud Build service account has Firebase Admin permissions
3. Ensure billing is enabled for the project
4. Check Firebase Console for any manual setup requirements

The mobile app will continue to work without server-side changes once Analytics is enabled via this script.
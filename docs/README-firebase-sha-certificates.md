# Firebase SHA Certificate Copy Functionality

## Overview

The SHA certificate copy automation now lives in the dedicated Cloud Build config `gcp/cloud-build/sha_copy.yaml`. It copies SHA-1 and SHA-256 fingerprints from the global Firebase Android app (`piotr_gorczynski.soccer2`) to the Bangladesh variant (`piotr_gorczynski.soccer2.bd`).

## Why This Feature Exists

When creating multiple package variants (product flavors) of the same Android app in Firebase, each variant needs the same SHA certificates for features like:
- Google Sign-In
- Facebook Login
- Firebase Authentication
- Google Play Services integration

Manually copying SHA certificates to each package variant is error-prone and time-consuming. This automation ensures all variants have consistent SHA certificates.

## How It Works

The SHA certificate copying functionality is implemented directly in the `gcp/cloud-build/sha_copy.yaml` workflow. The workflow performs the following operations:

1. **Fetch Android Apps**: Retrieves all Android apps registered in the Firebase project
2. **Identify Global App**: Finds the global app (`piotr_gorczynski.soccer2`) and extracts its app ID
3. **Retrieve SHA Certificates**: Gets all SHA certificates from the global app using Firebase Management API
4. **Copy to Other Packages**: Iterates through all package variants and copies SHA certificates to each one
5. **Skip Duplicates**: Checks existing certificates to avoid adding duplicates

## Technical Details

### Firebase Management API Endpoints Used

- **List Android Apps**: `GET /v1beta1/projects/{projectId}/androidApps`
- **Get SHA Certificates**: `GET /v1beta1/projects/{projectId}/androidApps/{appId}/sha`
- **Add SHA Certificate**: `POST /v1beta1/projects/{projectId}/androidApps/{appId}/sha`

### Certificate Types Supported

- SHA-1 fingerprints (`SHA_1`)
- SHA-256 fingerprints (`SHA_256`)

### Error Handling

The workflow gracefully handles various scenarios:

- **Global app not found**: Exits with warning, suggesting manual configuration
- **No SHA certificates in global app**: Exits gracefully with instructions to add certificates first
- **Target app not found**: Skips that package and continues with others
- **Duplicate certificates**: Detects and skips already-existing certificates
- **API failures**: Logs error details but continues processing other packages

## Usage

### Prerequisites

1. The global Firebase Android app (`piotr_gorczynski.soccer2`) must exist
2. SHA certificates must be added to the global app first (manually via Firebase Console)
3. Cloud Build service account needs Firebase Admin permissions

### Running the Workflow

```bash
gcloud builds submit --config gcp/cloud-build/sha_copy.yaml \
  --substitutions=_ENVIRONMENT=dev,_FOLDER_NAME=soccer
```

### Adding SHA Certificates to Global App (First Time)

Before running the workflow, add SHA certificates to the global app:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to Project Settings → Your apps → Select `piotr_gorczynski.soccer2`
4. Scroll down to "SHA certificate fingerprints"
5. Click "Add fingerprint" and paste your SHA-1 or SHA-256 hash

To get your SHA-1 fingerprint from your keystore:

```bash
# For debug keystore
keytool -list -v -alias androiddebugkey \
  -keystore ~/.android/debug.keystore

# For release keystore
keytool -list -v -alias <your-alias> \
  -keystore /path/to/your/keystore.jks
```

### Verifying the Copy

After running the workflow, verify SHA certificates were copied:

1. Check the Cloud Build logs for success messages
2. Go to Firebase Console → Project Settings
3. Check each Android app variant
4. Verify SHA certificates match the global app

## Package Names

The workflow currently processes these packages:

```bash
_GLOBAL_PACKAGE="piotr_gorczynski.soccer2"
_BD_PACKAGE="piotr_gorczynski.soccer2.bd"
```

- `piotr_gorczynski.soccer2` - Global version (source of SHA certificates)
- `piotr_gorczynski.soccer2.bd` - Bangladesh version (receives copied certificates)

## Compatibility

- **JSON Parser**: Uses Python JSON parsing in the Cloud SDK step
- **Cloud SDK**: Compatible with `gcr.io/google.com/cloudsdktool/cloud-sdk:slim`
- **Firebase API**: Uses Firebase Management API v1beta1

## Troubleshooting

### Problem: "Could not find app ID for global package"

**Solution**: Ensure the global app (`piotr_gorczynski.soccer2`) is created in Firebase. The deployment flow creates it, but if it fails, create it manually.

### Problem: "No SHA certificates found in global app"

**Solution**: Add SHA certificates to the global app first using Firebase Console (see "Adding SHA Certificates to Global App" section above).

### Problem: "Failed to add SHA certificate (HTTP 403)"

**Solution**: Check that Cloud Build service account has proper Firebase permissions:
- Firebase Admin role
- Service Usage Consumer role

### Problem: Certificates not appearing in Firebase Console

**Solution**: 
1. Wait a few minutes for Firebase to propagate changes
2. Refresh the Firebase Console page
3. Check Cloud Build logs for any error messages

### Problem: "Could not find app ID for package" with newly created apps

**Solution**: Re-run the SHA copy workflow after the app creation step completes and ensure the package name matches exactly. If the error persists, verify permissions for the Cloud Build service account.

**Debugging**: Review Cloud Build logs for `sha_copy.yaml` to confirm counts and error messages. The workflow logs certificate counts and success/failure summaries without printing SHA values.

## Security Considerations

- SHA certificates are public information and don't need to be treated as secrets
- The workflow uses Google Cloud authentication tokens with appropriate scopes
- All API calls are made over HTTPS
- Certificates are read-only from the global app (source app is not modified)

## Maintenance

### Adding New Package Variants

To add a new package variant (e.g., `piotr_gorczynski.soccer2.in` for India):

1. Update the substitutions in `gcp/cloud-build/sha_copy.yaml`:
   ```bash
   _GLOBAL_PACKAGE="piotr_gorczynski.soccer2"
   _BD_PACKAGE="piotr_gorczynski.soccer2.in"
   ```

2. Run the SHA copy workflow - SHA certificates will be copied to the new variant

### Changing the Global Package

If you need to use a different package as the source of SHA certificates:

1. Update the `_GLOBAL_PACKAGE` substitution:
   ```bash
   _GLOBAL_PACKAGE="your_new_global_package"
   ```

2. Ensure SHA certificates are configured in the new global package first

## Related Files

- `deploy_firebase.yaml` - Main deployment script
- `sha_copy.yaml` - SHA certificate copy workflow
- `deploy_firebase_with_auth.yaml` - Authentication configuration
- `final-instructions.yaml` - Manual setup instructions

## References

- [Firebase Management API](https://firebase.google.com/docs/projects/api)
- [Android App SHA Certificate](https://developers.google.com/android/guides/client-auth)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start-integrating)

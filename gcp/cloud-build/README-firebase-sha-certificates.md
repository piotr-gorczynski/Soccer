# Firebase SHA Certificate Copy Functionality

## Overview

The `deploy_firebase.yaml` script now includes automatic SHA certificate copying functionality. This feature automatically copies SHA-1 and SHA-256 fingerprints from the global Firebase Android app (`piotr_gorczynski.soccer2`) to all other package variants (e.g., `piotr_gorczynski.soccer2.bd`).

## Why This Feature Exists

When creating multiple package variants (product flavors) of the same Android app in Firebase, each variant needs the same SHA certificates for features like:
- Google Sign-In
- Facebook Login
- Firebase Authentication
- Google Play Services integration

Manually copying SHA certificates to each package variant is error-prone and time-consuming. This automation ensures all variants have consistent SHA certificates.

## How It Works

The script (Step 8 in `deploy_firebase.yaml`) performs the following operations:

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

The script gracefully handles various scenarios:

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

### Running the Script

```bash
gcloud builds submit --config gcp/cloud-build/deploy_firebase.yaml \
  --substitutions=_ENVIRONMENT=dev,_FOLDER_NAME=soccer
```

### Adding SHA Certificates to Global App (First Time)

Before running the script, add SHA certificates to the global app:

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

After running the script, verify SHA certificates were copied:

1. Check the Cloud Build logs for success messages
2. Go to Firebase Console → Project Settings
3. Check each Android app variant
4. Verify SHA certificates match the global app

## Package Names

The script currently processes these packages:

```bash
package_names=("piotr_gorczynski.soccer2" "piotr_gorczynski.soccer2.bd")
```

- `piotr_gorczynski.soccer2` - Global version (source of SHA certificates)
- `piotr_gorczynski.soccer2.bd` - Bangladesh version (receives copied certificates)

## Script Output

### Successful Execution

```
🔑 Starting SHA certificate copy process...
📱 Global package: piotr_gorczynski.soccer2
🔍 Fetching list of Android apps...
🔍 Searching for global app ID...
✅ Found global app ID: 1:123456789:android:abc123def456
🔍 Fetching SHA certificates from global app...
✅ Found SHA certificates in global app
📋 Number of SHA certificates found: 2
⏭️  Skipping global package: piotr_gorczynski.soccer2
---
🔍 Processing package: piotr_gorczynski.soccer2.bd
✅ Found target app ID: 1:123456789:android:xyz789uvw012
🔍 Checking existing SHA certificates in target app...
📥 Adding SHA certificate: AA:BB:CC:DD:EE:FF:... (type: SHA_1)
✅ Successfully added SHA certificate
📥 Adding SHA certificate: 11:22:33:44:55:66:... (type: SHA_256)
✅ Successfully added SHA certificate
✅ Completed SHA certificate copy for package: piotr_gorczynski.soccer2.bd
---
✅ SHA certificate copy process completed successfully!
```

### No SHA Certificates Found

```
⚠️  No SHA certificates found in global app
ℹ️  Please add SHA certificates to the global app first in Firebase Console.
ℹ️  Once added, re-run this deployment to copy them to other packages.
```

### Duplicate Detection

```
⏭️  SHA certificate already exists: AA:BB:CC:DD:EE:FF:... (type: SHA_1)
```

## Compatibility

- **JSON Parser**: Script supports both `jq` (preferred) and grep-based parsing
- **Cloud SDK**: Compatible with `gcr.io/google.com/cloudsdktool/cloud-sdk:slim`
- **Firebase API**: Uses Firebase Management API v1beta1

## Troubleshooting

### Problem: "Could not find app ID for global package"

**Solution**: Ensure the global app (`piotr_gorczynski.soccer2`) is created in Firebase. The script should create it automatically in Step 7, but if it fails, create it manually.

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

### Problem: SHA certificates not being copied

**Issue**: SHA certificates were found in the global app but were not being copied to variant apps.

**Root Causes & Solutions**:

1. **Subshell Issue (Fixed in PR #1124)**
   - **Cause**: The script used a pipe with a `while` loop (`echo ... | while`), which runs the loop in a subshell. With `set -e` enabled, if any error occurred during processing, the loop could exit prematurely without processing all certificates.
   - **Solution**: Changed to use process substitution (`while ... done < <(...)`) instead of a pipe. This ensures the while loop runs in the current shell and all certificates are processed even if individual API calls encounter errors.

2. **Silent Failures (Fixed in PR #1126)**
   - **Cause**: JQ errors were suppressed with `2>/dev/null`, making it impossible to debug parsing issues. The script had no way to detect if the certificate copying loop actually executed.
   - **Solution**: 
     - Removed `2>/dev/null` from jq command to expose parsing errors
     - Added counter to track how many certificates were processed
     - Added warning message if loop executes zero times
     - Added debug logging throughout the process

3. **Process Substitution Reliability (Fixed in current PR)**
   - **Cause**: Using `while read` with process substitution (`while ... done < <(...)`) can have reliability issues in certain shell environments, especially in containerized environments like Cloud Build. The loop might silently fail to execute if the process substitution doesn't work properly, resulting in zero certificates processed.
   - **Solution**: 
     - Replaced the `while read` loop with an array-based approach
     - Extract SHA hashes and cert types into arrays using jq
     - Iterate through arrays using index-based for loop
     - This approach is more robust and works consistently across all environments
     - Uses `mapfile` when available for better performance, falls back to `read` otherwise

**Debugging**: The script now provides detailed logging:
- Shows whether jq or grep parsing is used
- Reports number of certificates found and extracted into arrays
- Logs each certificate as it's being added with its SHA hash
- Warns if no certificates were processed (indicates jq failure or empty list)
- Shows HTTP status codes and error responses for failed API calls

## Security Considerations

- SHA certificates are public information and don't need to be treated as secrets
- The script uses Google Cloud authentication tokens with appropriate scopes
- All API calls are made over HTTPS
- Certificates are read-only from the global app (source app is not modified)

## Maintenance

### Adding New Package Variants

To add a new package variant (e.g., `piotr_gorczynski.soccer2.in` for India):

1. Update the `package_names` array in the script:
   ```bash
   package_names=("piotr_gorczynski.soccer2" "piotr_gorczynski.soccer2.bd" "piotr_gorczynski.soccer2.in")
   ```

2. Run the deployment script - SHA certificates will be automatically copied to the new variant

### Changing the Global Package

If you need to use a different package as the source of SHA certificates:

1. Update the `global_package_name` variable:
   ```bash
   global_package_name="your_new_global_package"
   ```

2. Ensure SHA certificates are configured in the new global package first

## Related Files

- `deploy_firebase.yaml` - Main deployment script with SHA copy functionality
- `deploy_firebase_with_auth.yaml` - Authentication configuration
- `final-instructions.yaml` - Manual setup instructions

## References

- [Firebase Management API](https://firebase.google.com/docs/projects/api)
- [Android App SHA Certificate](https://developers.google.com/android/guides/client-auth)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start-integrating)

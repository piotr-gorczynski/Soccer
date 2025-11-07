# Vertex AI Moderation Fix - Summary

## Problem

The backend logs showed that the `checkNickname` function was failing with Vertex AI errors when checking nicknames for inappropriate content. The function was supposed to moderate nicknames using Google's Vertex AI (Gemini model), but the required **Vertex AI API** (`aiplatform.googleapis.com`) was not enabled in the GCP project.

## Root Cause

The `check-nickname` Cloud Function uses the `@google-cloud/vertexai` package to call Google's Gemini 1.5 Flash model for content moderation. However, this requires two things:

1. **Vertex AI API** must be enabled in the Google Cloud project (`aiplatform.googleapis.com`)
2. **IAM Permissions**: The Cloud Functions service account needs the `roles/aiplatform.user` role to call Vertex AI endpoints

Without both of these, the function would fail with permission or authentication errors.

The function already had graceful error handling that allowed nicknames by default when errors occurred (fail-open approach), which prevented users from being blocked but also meant moderation wasn't working.

## Solution

This fix provides four key components:

### 1. Standalone API Enablement Script

**File**: `gcp/cloud-build/enable_vertex_ai.yaml`

A Cloud Build script that can be run independently to enable the Vertex AI API:

```bash
gcloud builds submit \
  --config=gcp/cloud-build/enable_vertex_ai.yaml \
  --substitutions=_ENVIRONMENT=dev
```

This script:
- Finds the project for the specified environment (dev/staging/prod)
- Enables the `aiplatform.googleapis.com` API
- Provides clear error messages if permissions are insufficient
- Requires `Service Usage Admin` or `Service Usage Consumer` role

### 2. IAM Role Grant Script

**File**: `gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml`

A Cloud Build script to grant the Vertex AI User role to the Cloud Functions service account:

```bash
gcloud builds submit \
  --config=gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml \
  --substitutions=_ENVIRONMENT=dev
```

This script:
- Finds the project for the specified environment
- Grants `roles/aiplatform.user` to the App Engine default service account (`PROJECT_ID@appspot.gserviceaccount.com`)
- This role includes the `aiplatform.endpoints.predict` permission required for Vertex AI operations

### 3. Updated Deployment Script

**File**: `gcp/cloud-build/deploy_check_nickname.yaml`

The deployment script now includes a step to automatically enable the Vertex AI API before deploying the function. This ensures the API is available when the function is deployed.

Key improvements:
- Checks if the API is already enabled before attempting to enable it
- Provides clear success/skip messages
- Maintains the existing deployment flow

**Note**: The deployment script only enables the API. You must separately grant the IAM role using the script above.

### 4. Comprehensive Documentation

**File**: `firebase/functions/check-nickname/README.md`

A complete guide covering:
- Prerequisites (Vertex AI API and IAM role requirements)
- How to enable the API (both automated and manual methods)
- How to grant the IAM role (both automated and manual methods)
- Deployment process
- Function behavior (normal operation and error handling)
- Troubleshooting common issues
- Security considerations

## How to Use

### For First-Time Setup

1. **Enable the Vertex AI API** (one-time setup):
   ```bash
   gcloud builds submit \
     --config=gcp/cloud-build/enable_vertex_ai.yaml \
     --substitutions=_ENVIRONMENT=dev
   ```

2. **Grant the Vertex AI User role** (one-time setup):
   ```bash
   gcloud builds submit \
     --config=gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml \
     --substitutions=_ENVIRONMENT=dev
   ```

3. **Deploy the function**:
   ```bash
   gcloud builds submit \
     --config=gcp/cloud-build/deploy_check_nickname.yaml \
     --substitutions=_ENVIRONMENT=dev
   ```

### For Subsequent Deployments

Just deploy the function - the API enablement is now included:
```bash
gcloud builds submit \
  --config=gcp/cloud-build/deploy_check_nickname.yaml \
  --substitutions=_ENVIRONMENT=dev
```

The IAM role only needs to be granted once per environment.

### Manual Verification

To verify the API is enabled:
```bash
gcloud services list --enabled --project=YOUR_PROJECT_ID | grep aiplatform
```

Expected output:
To verify the IAM role is granted:
```bash
gcloud projects get-iam-policy YOUR_PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.role:roles/aiplatform.user AND bindings.members:YOUR_PROJECT_ID@appspot.gserviceaccount.com"
```

## Files Changed

1. **New**: `gcp/cloud-build/enable_vertex_ai.yaml` - Standalone API enablement script
2. **New**: `gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml` - IAM role grant script
3. **Modified**: `gcp/cloud-build/deploy_check_nickname.yaml` - Added Vertex AI API enablement step
4. **New**: `firebase/functions/check-nickname/README.md` - Complete documentation

## Security Considerations

- The function code (`index.js`) was not modified - it already has robust error handling
- The graceful degradation (fail-open) behavior is intentional to avoid blocking users during temporary API issues
- Error logs still capture all failures for debugging
- The API enablement and IAM role granting require appropriate permissions to prevent unauthorized changes
- The `roles/aiplatform.user` role grants minimal permissions needed for Vertex AI operations

## Next Steps

After merging this PR:

1. **For existing deployments**: 
   - Run the `enable_vertex_ai.yaml` script once for each environment (dev/staging/prod)
   - Run the `grant_vertex_ai_user_to_appengine_sa.yaml` script once for each environment
2. **For new deployments**: The API will be automatically enabled during deployment, but you must still grant the IAM role separately
3. **Monitor logs**: Check that nickname moderation is working and errors have stopped

## Testing

To test the fix:

1. Enable the API in your dev environment
2. Grant the IAM role to the service account
3. Deploy the function
4. Try setting a nickname in the app
5. Check the logs - you should see moderation checks succeeding instead of failing
6. Try a potentially inappropriate nickname to verify moderation is working

## References

- [Vertex AI API Documentation](https://cloud.google.com/vertex-ai/docs/start/cloud-environment)
- [Google Cloud Service Enablement](https://cloud.google.com/service-usage/docs/enable-disable)
- Function README: `firebase/functions/check-nickname/README.md`

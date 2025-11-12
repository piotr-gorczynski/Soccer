# Check Nickname Function

This Cloud Function uses Google's Vertex AI (Gemini model) to moderate nicknames for inappropriate content before users can set them in the app.

## Overview

The `checkNickname` function:
- Accepts a nickname as input
- Uses Vertex AI's Gemini 1.5 Flash model to check for inappropriate content
- Returns whether the nickname is allowed or violates content rules
- Falls back to allowing nicknames if the API is unavailable (graceful degradation)

## Prerequisites

Before deploying this function, you must:
1. Enable the **Vertex AI API** in your GCP project
2. Grant the **Vertex AI User** role to the Cloud Functions service account

### Step 1: Enable Vertex AI API

You can enable the API in two ways:

#### Option 1: Using Cloud Build (Recommended)

Run the Cloud Build trigger to enable the Vertex AI API:

```bash
gcloud builds submit \
  --config=gcp/cloud-build/enable_vertex_ai.yaml \
  --substitutions=_ENVIRONMENT=dev
```

Replace `dev` with your target environment (`dev`, `staging`, or `prod`).

#### Option 2: Using gcloud CLI

Manually enable the API using the gcloud command:

```bash
# Set your project
gcloud config set project YOUR_PROJECT_ID

# Enable Vertex AI API
gcloud services enable aiplatform.googleapis.com
```

### Step 2: Grant Vertex AI User Role

The Cloud Functions service account needs the `roles/aiplatform.user` role to call Vertex AI endpoints.

**Note**: Firebase Functions v2 deployed to Cloud Run uses the default Compute Engine service account (`<PROJECT_NUMBER>-compute@developer.gserviceaccount.com`), not the App Engine service account.

#### Option 1: Using Cloud Build (Recommended)

Run the Cloud Build trigger to grant the IAM role:

```bash
gcloud builds submit \
  --config=gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml \
  --substitutions=_ENVIRONMENT=dev
```

Replace `dev` with your target environment (`dev`, `staging`, or `prod`).

This script will:
1. Automatically detect the service account used by the deployed `checkNickname` Cloud Run service
2. Grant the Vertex AI User role to that service account

#### Option 2: Using gcloud CLI

Manually grant the role using the gcloud command:

```bash
# Set your project
gcloud config set project YOUR_PROJECT_ID

# Get the service account used by the checkNickname Cloud Run service
SERVICE_ACCOUNT=$(gcloud run services describe checknickname \
  --region us-central1 \
  --format='value(spec.template.spec.serviceAccountName)')

# Grant Vertex AI User role to the service account
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:$SERVICE_ACCOUNT" \
  --role="roles/aiplatform.user"
```

### Verify Setup

Check if the Vertex AI API is enabled:

```bash
gcloud services list --enabled --project=YOUR_PROJECT_ID | grep aiplatform
```

You should see:
```
aiplatform.googleapis.com       Vertex AI API
```

Check if the IAM role is granted:

```bash
# First, get the service account used by the Cloud Run service
SERVICE_ACCOUNT=$(gcloud run services describe checknickname \
  --region us-central1 \
  --format='value(spec.template.spec.serviceAccountName)')

# Then check if it has the Vertex AI User role
gcloud projects get-iam-policy YOUR_PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.role:roles/aiplatform.user AND bindings.members:serviceAccount:$SERVICE_ACCOUNT"
```

## Deployment

The function is automatically deployed when you run:

```bash
gcloud builds submit \
  --config=gcp/cloud-build/deploy_check_nickname.yaml \
  --substitutions=_ENVIRONMENT=dev
```

The deployment script:
1. Retrieves the project ID for the target environment
2. **Enables the Vertex AI API** (if not already enabled)
3. Installs Firebase CLI
4. Clones the source code
5. Installs dependencies
6. Deploys the function
7. Grants public access to invoke the function

**Note**: The deployment script only enables the API. You must separately grant the Vertex AI User role to the service account (see Prerequisites above).

## Function Behavior

### Normal Operation

When the Vertex AI API is enabled and functioning:
- Nicknames are checked against safety ratings
- If flagged as `HIGH` or `MEDIUM` probability for inappropriate content, returns `{ allowed: false, reason: "Nickname violates content rules." }`
- Otherwise, returns `{ allowed: true }`

### Error Handling (Graceful Degradation)

If the Vertex AI API encounters errors, the function:
- Logs detailed error information for debugging
- **Allows the nickname by default** to avoid blocking users
- Handles specific error types:
  - **Permission Denied**: API not enabled or insufficient permissions
  - **Unavailable**: Temporary service outage or network issues
  - **Authentication**: API key or credentials problems

This ensures users can still set nicknames even if the moderation service has issues.

### Debug Logging

The function now includes comprehensive logging at each stage:
- **Request received**: Logs the incoming nickname being checked
- **Vertex AI call**: Logs when the API is called for moderation
- **Response received**: Logs when Vertex AI responds
- **Safety ratings**: Logs detailed safety ratings for each category with probability levels
- **Decision made**: Logs whether nickname is ALLOWED or BLOCKED with reasons
- **Fallback activated**: Logs clearly when error fallback occurs and which type of error triggered it

All log messages are prefixed with `checkNickname:` for easy filtering in Cloud Logging. When a fallback is activated, the log will include `FALLBACK ACTIVATED` to make it immediately obvious that the nickname was allowed due to an error rather than passing moderation.

## Error Logs

The function now includes comprehensive logging to help diagnose issues. Look for these log patterns:

### Normal Operation Logs
```
checkNickname called with nickname: <nickname>
checkNickname: Calling Vertex AI for nickname moderation: <nickname>
checkNickname: Vertex AI response received for nickname: <nickname>
checkNickname: Safety ratings for nickname: <nickname> { safetyRatings: [...] }
checkNickname: Nickname ALLOWED: <nickname>
```

### Blocked Nickname Logs
```
checkNickname: Nickname BLOCKED due to content violations: <nickname> { flaggedRatings: [...] }
```

### Fallback Logs (Error Occurred)
If you see errors like:
```
checkNickname: Vertex AI moderation FAILED for nickname: <nickname>
checkNickname: FALLBACK ACTIVATED - <error type> - allowing nickname by default: <nickname>
```

This indicates the nickname was allowed due to an error, not because it passed moderation.

Check:
1. **Is the Vertex AI API enabled?** Run: `gcloud services list --enabled --project=YOUR_PROJECT_ID | grep aiplatform`
2. **Does the service account have the Vertex AI User role?** The Cloud Functions service account needs the `roles/aiplatform.user` role. Run the grant script: `gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml`
3. **Is there a quota issue?** Check your Vertex AI quotas in the GCP Console

**Important**: If you consistently see `FALLBACK ACTIVATED` logs, inappropriate nicknames are being allowed through! Fix the Vertex AI configuration immediately.

## Local Testing

To test locally, ensure:
1. You have application default credentials set up: `gcloud auth application-default login`
2. The `GOOGLE_CLOUD_PROJECT` or `GCLOUD_PROJECT` environment variable is set to your project ID
3. The Vertex AI API is enabled in your project
4. Your user account has the Vertex AI User role or equivalent permissions

## Security

The function:
- Is publicly accessible (required for Firebase callable functions)
- Uses Cloud Functions built-in authentication
- Sanitizes and logs errors without exposing sensitive information
- Falls back to allowing nicknames (fail-open) rather than blocking all users (fail-closed)

## Dependencies

See [`package.json`](package.json) for the complete list of dependencies. Key packages:
- `@google-cloud/vertexai` - Vertex AI SDK for Node.js
- `firebase-functions` - Firebase Functions SDK

## Log Analysis

To verify if the `checkNickname` function is working correctly, you can analyze its logs using the provided log analysis tool.

### Downloading Logs

Download logs from Firebase Console or using gcloud CLI:

**Option 1: Firebase Console**
1. Go to Firebase Console > Functions > checkNickname
2. Click on "Logs" tab
3. Click "Download logs" to export as JSON

**Option 2: gcloud CLI**
```bash
# Download logs from the last 24 hours
gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="checknickname"' \
  --limit=1000 \
  --format=json \
  --freshness=24h > downloaded-logs.json
```

### Analyzing Logs

Run the log analyzer script:

```bash
python3 tools/analyze-function-logs.py downloaded-logs.json
```

The analyzer will:
- Count total requests, allowed nicknames, and blocked nicknames
- Detect fallback activations (indicating errors with Vertex AI)
- Identify permission denied, unavailable, or authentication errors
- Provide recommendations for fixing issues
- Show a health status: ✅ Healthy or ⚠️ Needs Attention

### Example Output

```
checkNickname Function Log Analysis Report
================================================================================

SUMMARY STATISTICS
--------------------------------------------------------------------------------
Total Requests:           150
Nicknames Allowed:        145
Nicknames Blocked:        3
Fallback Activations:     2
Errors Detected:          2

HEALTH STATUS
--------------------------------------------------------------------------------
⚠️  WARNING: Function triggered fallback mode 2 times.
   This means errors occurred and nicknames were allowed by default!
   Inappropriate nicknames may have passed through.

FALLBACK REASONS
--------------------------------------------------------------------------------
  Permission Denied: 2 occurrences

RECOMMENDED ACTIONS:
  1. Enable Vertex AI API:
     gcloud services enable aiplatform.googleapis.com
  
  2. Grant Vertex AI User role to the service account:
     Run: gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml
```

## Troubleshooting

### "Permission Denied" errors
**Causes**: 
- Vertex AI API is not enabled
- Service account lacks the Vertex AI User role

**Solution**: 
1. Enable the API: Run `gcp/cloud-build/enable_vertex_ai.yaml`
2. Grant IAM role: Run `gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml`

**Note**: The script automatically detects the correct service account used by the Cloud Run service.

### "API key not valid" errors
**Cause**: Authentication issues
**Solution**: Check service account permissions and ensure both the API is enabled and the IAM role is granted

### "aiplatform.endpoints.predict" permission denied
**Cause**: Service account lacks the Vertex AI User role
**Solution**: Run `gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml` to grant the role to the correct service account

### Function allows all nicknames
**Cause**: This is the expected fallback behavior when errors occur
**Action**: Check logs for specific error messages. Ensure both the API is enabled AND the IAM role is granted. Use the log analyzer tool to get a detailed report.

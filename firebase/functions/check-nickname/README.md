# Check Nickname Function

This Cloud Function uses Google's Vertex AI (Gemini model) to moderate nicknames for inappropriate content before users can set them in the app.

## Overview

The `checkNickname` function:
- Accepts a nickname as input
- Uses Vertex AI's Gemini 1.5 Flash model to check for inappropriate content
- Returns whether the nickname is allowed or violates content rules
- Falls back to allowing nicknames if the API is unavailable (graceful degradation)

## Prerequisites

Before deploying this function, you must enable the **Vertex AI API** in your GCP project.

### Enable Vertex AI API

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

### Verify API is Enabled

Check if the Vertex AI API is enabled:

```bash
gcloud services list --enabled --project=YOUR_PROJECT_ID | grep aiplatform
```

You should see:
```
aiplatform.googleapis.com       Vertex AI API
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

## Error Logs

If you see errors like:
```
Vertex AI moderation failed for nickname check
```

Check:
1. Is the Vertex AI API enabled? Run the verification command above
2. Does the service account have permissions? The Cloud Functions service account needs `Vertex AI User` role
3. Is there a quota issue? Check your Vertex AI quotas in the GCP Console

## Local Testing

To test locally, ensure:
1. You have application default credentials set up: `gcloud auth application-default login`
2. The `GOOGLE_CLOUD_PROJECT` or `GCLOUD_PROJECT` environment variable is set to your project ID
3. The Vertex AI API is enabled in your project

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

## Troubleshooting

### "Permission Denied" errors
**Cause**: Vertex AI API is not enabled
**Solution**: Run the `enable_vertex_ai.yaml` Cloud Build script

### "API key not valid" errors
**Cause**: Authentication issues
**Solution**: Check service account permissions and ensure `aiplatform.googleapis.com` is enabled

### Function allows all nicknames
**Cause**: This is the expected fallback behavior when errors occur
**Action**: Check logs for specific error messages and enable the API if needed

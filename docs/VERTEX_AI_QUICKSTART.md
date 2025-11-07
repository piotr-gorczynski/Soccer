# Quick Start: Fixing Vertex AI Moderation

## The Issue
Backend logs show Vertex AI moderation failures when checking nicknames because the Vertex AI API is not enabled.

## The Fix (3 Simple Steps)

### Step 1: Enable the Vertex AI API

Run this command for your environment (replace `dev` with `staging` or `prod` if needed):

```bash
gcloud builds submit \
  --config=gcp/cloud-build/enable_vertex_ai.yaml \
  --substitutions=_ENVIRONMENT=dev
```

### Step 2: Verify the API is Enabled

```bash
gcloud services list --enabled --project=YOUR_PROJECT_ID | grep aiplatform
```

You should see:
```
aiplatform.googleapis.com       Vertex AI API
```

### Step 3: (Optional) Redeploy the Function

The deployment script now auto-enables the API, but if you want to redeploy:

```bash
gcloud builds submit \
  --config=gcp/cloud-build/deploy_check_nickname.yaml \
  --substitutions=_ENVIRONMENT=dev
```

## That's It!

Nickname moderation should now work properly. Check your backend logs - you should see successful moderation checks instead of errors.

## Need More Info?

- **Detailed Documentation**: See `firebase/functions/check-nickname/README.md`
- **Complete Summary**: See `docs/VERTEX_AI_MODERATION_FIX.md`
- **Troubleshooting**: Check the README for common issues and solutions

## Still Having Issues?

Check:
1. Do you have the `Service Usage Admin` or `Service Usage Consumer` role?
2. Is the API actually enabled? Run the verification command above.
3. Are there any quota limits on your project?

For more help, see the troubleshooting section in the README.

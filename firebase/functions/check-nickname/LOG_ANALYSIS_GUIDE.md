# Log Analysis Guide for checkNickname Function

This guide explains how to analyze logs from the `checkNickname` Cloud Function to verify it's working correctly.

## Quick Start

1. **Download logs** from Firebase Console or using gcloud:
   ```bash
   gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="checknickname"' \
     --limit=1000 --format=json --freshness=24h > downloaded-logs.json
   ```

2. **Run the analyzer**:
   ```bash
   python3 tools/analyze-function-logs.py downloaded-logs.json
   ```

## Understanding the Output

### Healthy Function Example

When the function is working correctly, you'll see:

```
✅ HEALTHY: Function is working correctly with no errors or fallbacks.

SUMMARY STATISTICS
--------------------------------------------------------------------------------
Total Requests:           150
Nicknames Allowed:        145
Nicknames Blocked:        5
Fallback Activations:     0
Errors Detected:          0

FINAL ASSESSMENT
================================================================================
✅ The checkNickname function is operating normally.
   All nicknames are being properly moderated by Vertex AI.
```

**Key indicators of health:**
- ✅ Zero fallback activations
- ✅ Zero errors detected
- ✅ Some blocked nicknames (shows moderation is working)
- ✅ Most nicknames allowed (users can set appropriate nicknames)

### Problematic Function Example

When there are issues, you'll see warnings:

```
⚠️  WARNING: Function triggered fallback mode 12 times.
   This means errors occurred and nicknames were allowed by default!
   Inappropriate nicknames may have passed through.

FALLBACK REASONS
--------------------------------------------------------------------------------
  Permission Denied: 12 occurrences

RECOMMENDED ACTIONS:
  1. Enable Vertex AI API:
     gcloud services enable aiplatform.googleapis.com
  
  2. Grant Vertex AI User role to the service account:
     Run: gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml

FINAL ASSESSMENT
================================================================================
❌ The checkNickname function has issues that need attention!

   CRITICAL: Fallback mode was activated, meaning:
   - Vertex AI moderation is not working properly
   - Inappropriate nicknames are being allowed through
   - Users may set offensive or inappropriate nicknames

   Fix the Vertex AI configuration immediately!
```

**Key indicators of problems:**
- ❌ Fallback activations > 0 (errors occurred)
- ❌ Permission Denied errors (API not enabled or IAM role missing)
- ❌ Zero blocked nicknames (moderation not running)
- ⚠️  All nicknames allowed (no content filtering)

## Common Issues and Solutions

### Issue 1: Permission Denied Errors

**Symptoms:**
```
FALLBACK REASONS
--------------------------------------------------------------------------------
  Permission Denied: XX occurrences
```

**Root Cause:**
- Vertex AI API is not enabled in the GCP project, OR
- Cloud Functions service account lacks the Vertex AI User role

**Solution:**

1. **Enable the Vertex AI API:**
   ```bash
   gcloud builds submit \
     --config=gcp/cloud-build/enable_vertex_ai.yaml \
     --substitutions=_ENVIRONMENT=dev
   ```

2. **Grant IAM role to service account:**
   ```bash
   gcloud builds submit \
     --config=gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml \
     --substitutions=_ENVIRONMENT=dev
   ```

3. **Verify the fix:**
   - Wait 5-10 minutes for IAM changes to propagate
   - Download new logs and run the analyzer again
   - You should see zero fallback activations

### Issue 2: Service Unavailable Errors

**Symptoms:**
```
FALLBACK REASONS
--------------------------------------------------------------------------------
  Service Unavailable: XX occurrences
```

**Root Cause:**
- Temporary Vertex AI service outage
- Network connectivity issues
- Regional availability problems

**Solution:**
- Check [Google Cloud Status Dashboard](https://status.cloud.google.com/) for Vertex AI outages
- Wait and retry - these are usually temporary
- If persistent, consider deploying to a different region

### Issue 3: Authentication Errors

**Symptoms:**
```
FALLBACK REASONS
--------------------------------------------------------------------------------
  Authentication Error: XX occurrences
```

**Root Cause:**
- API key issues
- Service account credentials problems
- IAM policy configuration errors

**Solution:**
1. Verify the service account has correct permissions:
   ```bash
   gcloud projects get-iam-policy YOUR_PROJECT_ID \
     --flatten="bindings[].members" \
     --filter="bindings.role:roles/aiplatform.user"
   ```

2. Re-deploy the function to refresh credentials:
   ```bash
   gcloud builds submit \
     --config=gcp/cloud-build/deploy_check_nickname.yaml \
     --substitutions=_ENVIRONMENT=dev
   ```

### Issue 4: No Nicknames Being Blocked

**Symptoms:**
```
SUMMARY STATISTICS
--------------------------------------------------------------------------------
Nicknames Blocked:        0
```

**What it means:**
- Either all submitted nicknames were appropriate (good!), OR
- Moderation is not working and inappropriate content is passing through (bad!)

**How to verify:**
1. Check if fallback activations > 0:
   - **Yes** → Moderation is broken, inappropriate nicknames are being allowed
   - **No** → Moderation is working, users are being careful with nicknames

2. Check the blocked nicknames section in the output:
   - **Empty** → Either no inappropriate nicknames were submitted, or moderation failed
   - **Has entries** → Moderation is catching inappropriate content

## Best Practices

### Regular Monitoring

Run log analysis regularly to catch issues early:

```bash
# Daily check (last 24 hours)
gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="checknickname"' \
  --format=json --freshness=24h > daily-logs.json
python3 tools/analyze-function-logs.py daily-logs.json

# Weekly check (last 7 days)
gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="checknickname"' \
  --format=json --freshness=168h > weekly-logs.json
python3 tools/analyze-function-logs.py weekly-logs.json
```

### Setting Up Alerts

Consider setting up Cloud Logging alerts for critical patterns:

1. **Alert on fallback activations:**
   - Filter: `textPayload:"FALLBACK ACTIVATED"`
   - Action: Send notification immediately

2. **Alert on permission denied:**
   - Filter: `textPayload:"permission denied" OR textPayload:"Permission Denied"`
   - Action: Send notification to ops team

3. **Alert on high block rate:**
   - Filter: `textPayload:"BLOCKED due to content violations"`
   - Threshold: > 10% of requests
   - Action: Review for false positives

## Interpreting Log Patterns

### Normal Operation Pattern

```
checkNickname called with nickname: JohnDoe
checkNickname: Calling Vertex AI for nickname moderation: JohnDoe
checkNickname: Vertex AI response received for nickname: JohnDoe
checkNickname: Safety ratings for nickname: JohnDoe {...}
checkNickname: Nickname ALLOWED: JohnDoe
```

**Duration:** 1-3 seconds
**Verdict:** ✅ Working correctly

### Blocked Nickname Pattern

```
checkNickname called with nickname: BadWord123
checkNickname: Calling Vertex AI for nickname moderation: BadWord123
checkNickname: Vertex AI response received for nickname: BadWord123
checkNickname: Safety ratings for nickname: BadWord123 {...}
checkNickname: Nickname BLOCKED due to content violations: BadWord123
```

**Duration:** 1-3 seconds
**Verdict:** ✅ Working correctly (moderation caught inappropriate content)

### Error Pattern (Permission Denied)

```
checkNickname called with nickname: TestUser
checkNickname: Vertex AI moderation FAILED for nickname: TestUser
checkNickname: FALLBACK ACTIVATED - Vertex AI permission denied - allowing nickname by default: TestUser
```

**Duration:** < 1 second (fails fast)
**Verdict:** ❌ Configuration issue - Vertex AI API or IAM role problem

## Manual Log Inspection

If the analyzer reports issues, you can manually inspect the logs for more details:

```bash
# View recent error logs
gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="checknickname" AND severity>=ERROR' \
  --limit=50 --format=json

# View fallback activations
gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="checknickname" AND textPayload:"FALLBACK ACTIVATED"' \
  --limit=50 --format=json

# View blocked nicknames
gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="checknickname" AND textPayload:"BLOCKED"' \
  --limit=50 --format=json
```

## Conclusion

The log analyzer tool helps you quickly identify issues with the `checkNickname` function. Key takeaways:

✅ **Healthy function:**
- Zero fallback activations
- Some nicknames blocked (moderation working)
- No permission errors

❌ **Unhealthy function:**
- Fallback activations present
- Permission denied or authentication errors
- All nicknames allowed (no moderation)

When issues are detected, follow the recommended actions in the analyzer output to fix the configuration.

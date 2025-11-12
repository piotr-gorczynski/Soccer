# Example Log Analysis Output

This document shows example outputs from the log analyzer tool for different scenarios.

## Example 1: Healthy Function (No Issues)

**Input:** Logs from a properly configured checkNickname function

**Command:**
```bash
python3 tools/analyze-function-logs.py healthy-logs.json
```

**Output:**
```
Analyzing 47 log entries...

================================================================================
checkNickname Function Log Analysis Report
================================================================================

Time Range: 2025-11-12T10:00:01.123Z to 2025-11-12T15:30:45.901Z

SUMMARY STATISTICS
--------------------------------------------------------------------------------
Total Requests:           12
Nicknames Allowed:        10
Nicknames Blocked:        2
Fallback Activations:     0
Errors Detected:          0

HEALTH STATUS
--------------------------------------------------------------------------------
✅ HEALTHY: Function is working correctly with no errors or fallbacks.

BLOCKED NICKNAMES
--------------------------------------------------------------------------------
Total blocked: 2

Recent blocks:
  1. 2025-11-12T14:22:01.890Z
     Nickname: BadWord123
  2. 2025-11-12T15:15:33.456Z
     Nickname: OffensiveName

================================================================================
FINAL ASSESSMENT
================================================================================
✅ The checkNickname function is operating normally.
   All nicknames are being properly moderated by Vertex AI.
================================================================================
```

**Analysis:**
- ✅ Function is working correctly
- ✅ Vertex AI moderation is active and working
- ✅ 2 inappropriate nicknames were blocked (16.7% block rate)
- ✅ 10 appropriate nicknames were allowed (83.3% approval rate)
- ✅ No errors or fallback activations

**Action Required:** None - system is healthy

---

## Example 2: Permission Denied (Critical Issue)

**Input:** Logs from a function without proper Vertex AI permissions

**Command:**
```bash
python3 tools/analyze-function-logs.py problem-logs.json
```

**Output:**
```
Analyzing 35 log entries...

================================================================================
checkNickname Function Log Analysis Report
================================================================================

Time Range: 2025-11-12T13:00:01.123Z to 2025-11-12T13:26:26.789Z

SUMMARY STATISTICS
--------------------------------------------------------------------------------
Total Requests:           8
Nicknames Allowed:        0
Nicknames Blocked:        0
Fallback Activations:     8
Errors Detected:          8

HEALTH STATUS
--------------------------------------------------------------------------------
⚠️  WARNING: Function triggered fallback mode 8 times.
   This means errors occurred and nicknames were allowed by default!
   Inappropriate nicknames may have passed through.

FALLBACK REASONS
--------------------------------------------------------------------------------
  Permission Denied: 8 occurrences

RECOMMENDED ACTIONS:
  1. Enable Vertex AI API:
     gcloud services enable aiplatform.googleapis.com

  2. Grant Vertex AI User role to the service account:
     Run: gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml

ERROR DETAILS
--------------------------------------------------------------------------------
  1. [ERROR] 2025-11-12T13:05:00.456Z
     checkNickname: Vertex AI moderation FAILED for nickname: User1 { error: 'Permission denied', code: 7 }

  2. [ERROR] 2025-11-12T13:10:15.789Z
     checkNickname: Vertex AI moderation FAILED for nickname: User2 { error: 'Permission denied', code: 7 }

  3. [ERROR] 2025-11-12T13:15:22.123Z
     checkNickname: Vertex AI moderation FAILED for nickname: User3 { error: 'Permission denied', code: 7 }

  ... (5 more errors)

================================================================================
FINAL ASSESSMENT
================================================================================
❌ The checkNickname function has issues that need attention!

   CRITICAL: Fallback mode was activated, meaning:
   - Vertex AI moderation is not working properly
   - Inappropriate nicknames are being allowed through
   - Users may set offensive or inappropriate nicknames

   Fix the Vertex AI configuration immediately!
================================================================================
```

**Analysis:**
- ❌ Function is NOT working correctly
- ❌ Vertex AI API is either not enabled OR service account lacks permissions
- ❌ ALL 8 requests failed and fell back to allowing nicknames
- ❌ Zero nicknames were actually moderated by AI
- ⚠️  ANY nickname (including inappropriate ones) would have been allowed

**Action Required:** IMMEDIATE - Fix Vertex AI configuration

**Steps to Fix:**
1. Enable Vertex AI API:
   ```bash
   gcloud builds submit \
     --config=gcp/cloud-build/enable_vertex_ai.yaml \
     --substitutions=_ENVIRONMENT=dev
   ```

2. Grant IAM role:
   ```bash
   gcloud builds submit \
     --config=gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml \
     --substitutions=_ENVIRONMENT=dev
   ```

3. Wait 5-10 minutes for changes to propagate

4. Download new logs and verify:
   ```bash
   gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="checknickname"' \
     --format=json --freshness=1h > new-logs.json
   python3 tools/analyze-function-logs.py new-logs.json
   ```

5. Expected result after fix:
   - ✅ Zero fallback activations
   - ✅ Zero errors
   - ✅ Nicknames being allowed and blocked normally

---

## Example 3: Service Unavailable (Temporary Issue)

**Input:** Logs during a Vertex AI service outage

**Command:**
```bash
python3 tools/analyze-function-logs.py outage-logs.json
```

**Output:**
```
Analyzing 22 log entries...

================================================================================
checkNickname Function Log Analysis Report
================================================================================

Time Range: 2025-11-12T08:00:01.123Z to 2025-11-12T08:15:45.901Z

SUMMARY STATISTICS
--------------------------------------------------------------------------------
Total Requests:           5
Nicknames Allowed:        3
Nicknames Blocked:        0
Fallback Activations:     2
Errors Detected:          2

HEALTH STATUS
--------------------------------------------------------------------------------
⚠️  WARNING: Function triggered fallback mode 2 times.
   This means errors occurred and nicknames were allowed by default!
   Inappropriate nicknames may have passed through.

FALLBACK REASONS
--------------------------------------------------------------------------------
  Service Unavailable: 2 occurrences

RECOMMENDED ACTIONS:
  - Service Unavailable errors may be temporary. Check Vertex AI status.

ERROR DETAILS
--------------------------------------------------------------------------------
  1. [ERROR] 2025-11-12T08:05:00.456Z
     checkNickname: Vertex AI moderation FAILED for nickname: User1 { error: 'Service unavailable', code: 14 }

  2. [ERROR] 2025-11-12T08:08:15.789Z
     checkNickname: Vertex AI moderation FAILED for nickname: User2 { error: 'Service unavailable', code: 14 }

================================================================================
FINAL ASSESSMENT
================================================================================
❌ The checkNickname function has issues that need attention!

   CRITICAL: Fallback mode was activated, meaning:
   - Vertex AI moderation is not working properly
   - Inappropriate nicknames are being allowed through
   - Users may set offensive or inappropriate nicknames

   Fix the Vertex AI configuration immediately!
================================================================================
```

**Analysis:**
- ⚠️  Function experienced temporary issues
- ⚠️  2 out of 5 requests (40%) failed due to service unavailability
- ⚠️  3 requests succeeded normally (60% success rate)
- ℹ️  This is likely a temporary Vertex AI outage

**Action Required:** Monitor and wait

**Steps:**
1. Check Google Cloud Status Dashboard:
   https://status.cloud.google.com/

2. If no outage is reported, check regional issues:
   ```bash
   gcloud services list --enabled --filter="name:aiplatform.googleapis.com"
   ```

3. Wait 30-60 minutes and check again:
   ```bash
   gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="checknickname"' \
     --format=json --freshness=1h > recent-logs.json
   python3 tools/analyze-function-logs.py recent-logs.json
   ```

4. If issue persists beyond 2 hours, consider:
   - Filing a support ticket with Google Cloud
   - Temporarily using a different region
   - Checking for quota exhaustion

---

## Example 4: Mixed Results (Partial Success)

**Input:** Logs showing recovery from an issue

**Command:**
```bash
python3 tools/analyze-function-logs.py recovery-logs.json
```

**Output:**
```
Analyzing 60 log entries...

================================================================================
checkNickname Function Log Analysis Report
================================================================================

Time Range: 2025-11-12T11:00:01.123Z to 2025-11-12T12:30:45.901Z

SUMMARY STATISTICS
--------------------------------------------------------------------------------
Total Requests:           15
Nicknames Allowed:        13
Nicknames Blocked:        1
Fallback Activations:     1
Errors Detected:          1

HEALTH STATUS
--------------------------------------------------------------------------------
⚠️  WARNING: Function triggered fallback mode 1 times.
   This means errors occurred and nicknames were allowed by default!
   Inappropriate nicknames may have passed through.

FALLBACK REASONS
--------------------------------------------------------------------------------
  Permission Denied: 1 occurrences

RECOMMENDED ACTIONS:
  1. Enable Vertex AI API:
     gcloud services enable aiplatform.googleapis.com

  2. Grant Vertex AI User role to the service account:
     Run: gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml

BLOCKED NICKNAMES
--------------------------------------------------------------------------------
Total blocked: 1

Recent blocks:
  1. 2025-11-12T12:15:33.456Z
     Nickname: TestBad

ERROR DETAILS
--------------------------------------------------------------------------------
  1. [ERROR] 2025-11-12T11:05:00.456Z
     checkNickname: Vertex AI moderation FAILED for nickname: User1 { error: 'Permission denied', code: 7 }

================================================================================
FINAL ASSESSMENT
================================================================================
❌ The checkNickname function has issues that need attention!

   CRITICAL: Fallback mode was activated, meaning:
   - Vertex AI moderation is not working properly
   - Inappropriate nicknames are being allowed through
   - Users may set offensive or inappropriate nicknames

   Fix the Vertex AI configuration immediately!
================================================================================
```

**Analysis:**
- ℹ️  Function mostly working (93% success rate)
- ⚠️  1 error occurred early in the log period
- ✅ All subsequent requests succeeded
- ✅ AI moderation is working (1 nickname blocked)
- ℹ️  Issue appears to have self-resolved or was fixed

**Action Required:** Verify fix is complete

**Interpretation:**
This pattern suggests:
- Configuration was fixed during the log period
- IAM changes took time to propagate
- Or a temporary issue resolved itself

**Next Steps:**
1. Continue monitoring for 24 hours
2. If no new fallbacks occur, issue is resolved
3. If fallbacks continue, follow the recommended actions

---

## Summary

Use the log analyzer to:

1. **Verify initial deployment** - Check that Vertex AI is configured correctly
2. **Monitor production** - Regular checks to catch issues early
3. **Troubleshoot problems** - Identify specific configuration issues
4. **Verify fixes** - Confirm that remediation steps worked

Remember:
- ✅ = Function working normally
- ⚠️ = Issues detected, action needed
- ❌ = Critical issues, immediate action required

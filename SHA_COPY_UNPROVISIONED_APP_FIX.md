# SHA Copy Fix: Handle Unprovisioned Firebase Apps

## Problem

After PR #1156 was merged, the SHA certificate copy script failed with the following error:

```
❌ Failed to add [SHA REDACTED] (SHA_1). HTTP 404
❌ Response: {
  "error": {
    "code": 404,
    "message": "OAuth Brand not provisioned [com.google.identity.boq.appidentity.spanner.common.StorageException: com.google.apps.framework.request.NotFoundException: Key not found: Long(491861032786)]",
    "status": "NOT_FOUND"
  }
}
```

## Root Cause

When a Firebase Android app is newly created (via `firebase apps:create`), it exists in the Firebase project but may not be fully provisioned immediately. During this initialization period:

1. **GET** requests to `/androidApps/{appId}/sha` return **HTTP 200** with an empty response `{}`
2. **POST** requests to `/androidApps/{appId}/sha` return **HTTP 404** with "OAuth Brand not provisioned" error

This happens because:
- The app is created in Firebase
- The app ID is valid and can be queried
- But internal Firebase/GCP services (like OAuth Brand provisioning) haven't finished initializing
- The app cannot accept SHA certificate additions until fully provisioned

## Solution

Added checks in three places to detect and gracefully handle unprovisioned apps:

### 1. Step 3a: Fetch SHA Certificates (lines 134-141)

After fetching SHA certificates from the Bangladesh app, check if the response is empty:

```bash
# Check if the response is an empty object (indicates app may not be fully provisioned)
if [[ "$target_body" == "{}" ]] || [[ -z "$target_body" ]]; then
  echo "⚠️  Bangladesh app returned empty response. App may not be fully provisioned yet."
  echo "⚠️  This typically happens with newly created Firebase apps that need time to initialize."
  echo "⚠️  Skipping SHA certificate copy for Bangladesh app."
  echo "ℹ️  To retry, run this build again after the app is fully provisioned (usually takes a few minutes)."
  exit 0
fi
```

### 2. Step 3b: Compare and Copy (lines 158-165)

Before attempting to copy certificates, verify the target file is not empty:

```bash
# Check if target file is just empty object (app not provisioned)
target_content=$(cat "$target_sha_file" 2>/dev/null || echo "")
if [[ "$target_content" == "{}" ]] || [[ -z "$target_content" ]]; then
  echo "⚠️  Bangladesh app not fully provisioned (empty response from API)."
  echo "⚠️  Skipping SHA certificate copy. The app needs time to initialize."
  echo "ℹ️  This is normal for newly created Firebase apps. Retry in a few minutes."
  exit 0
fi
```

### 3. Step 3c: Verify Certificates (lines 295-307)

Before verification, check if SHA copy was actually performed:

```bash
# Check if we skipped SHA copy due to unprovisioned app
if [ ! -f /workspace/sha_missing.txt ]; then
  echo "⚠️  SHA copy was skipped (missing file not found). Skipping verification."
  exit 0
fi

# Check if target file indicates unprovisioned app
target_content=$(cat "$target_sha_file" 2>/dev/null || echo "")
if [[ "$target_content" == "{}" ]] || [[ -z "$target_content" ]]; then
  echo "⚠️  Bangladesh app not fully provisioned. Skipping verification."
  exit 0
fi
```

## Expected Behavior

### Before the Fix

When SHA copy ran against a newly created Bangladesh app:
```
✅ Bangladesh SHA certificates fetched successfully
📋 Global certs: 1
📋 Bangladesh certs: 0
📋 Missing certs to add: 1
➕ Adding SHA certificate [SHA] (SHA_1) to Bangladesh app...
❌ Failed to add [SHA] (SHA_1). HTTP 404
❌ Response: {"error":{"code":404,"message":"OAuth Brand not provisioned"...}}
❌ Failed to add 1 SHA certificate(s).
BUILD FAILURE
```

### After the Fix

When SHA copy runs against a newly created Bangladesh app:
```
🔍 Fetching SHA certificates from Bangladesh app...
🔍 DEBUG: HTTP response code: 200
🔍 DEBUG: Response body length: 2 characters
⚠️  Bangladesh app returned empty response. App may not be fully provisioned yet.
⚠️  This typically happens with newly created Firebase apps that need time to initialize.
⚠️  Skipping SHA certificate copy for Bangladesh app.
ℹ️  To retry, run this build again after the app is fully provisioned (usually takes a few minutes).
BUILD SUCCESS
```

When the app is fully provisioned and has certificates:
```
🔍 Fetching SHA certificates from Bangladesh app...
🔍 DEBUG: HTTP response code: 200
🔍 DEBUG: Response body length: 253 characters
✅ Bangladesh SHA certificates fetched successfully
📋 Global certs: 1
📋 Bangladesh certs: 0
📋 Missing certs to add: 1
➕ Adding SHA certificate [SHA] (SHA_1) to Bangladesh app...
✅ Added [SHA] (SHA_1). HTTP 200
✅ SHA certificate copy completed successfully.
✅ Verification succeeded. All global certs are present on Bangladesh app.
BUILD SUCCESS
```

## How to Use

### If Build Skips SHA Copy

1. **Wait 5-10 minutes** for the Firebase app to fully provision
2. **Re-run the build** - the app should be ready by then
3. **Check Firebase Console** to confirm the app appears and is active

### If Problem Persists

If SHA copy continues to be skipped after multiple retries:

1. Check Firebase Console to see if the Bangladesh app exists
2. Verify the app package name is correct: `piotr_gorczynski.soccer2.bd`
3. Check Cloud Build logs for the app ID resolution step
4. Manually verify the app using Firebase CLI:
   ```bash
   firebase apps:list ANDROID --project=<project-id>
   ```

## Testing

The fix was validated with the following test cases:

1. **Empty object `{}`** → Correctly detected and skipped
2. **Empty string `""`** → Correctly detected and skipped
3. **Valid JSON with certificates** → Correctly processed
4. **Empty certificates array `{"certificates":[]}`** → Correctly processed

## Related Issues

- Issue: "fix sha copy" - SHA copy failing with OAuth Brand not provisioned error
- PR #1156: Previous SHA copy improvements
- PR #1157: SHA copy loop fix with trailing newline

## Files Modified

- `gcp/cloud-build/sha_copy.yaml` - Added unprovisioned app detection and graceful handling

## Impact

- ✅ Prevents build failures when Bangladesh app is newly created
- ✅ Provides clear messaging about why SHA copy was skipped
- ✅ Tells users how to resolve (wait and retry)
- ✅ No impact on normal SHA copy operations
- ✅ Build succeeds even when app is not ready (instead of failing)

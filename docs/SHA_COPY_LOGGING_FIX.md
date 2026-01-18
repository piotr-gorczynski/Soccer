# SHA Certificate Copy Logging Enhancement

## Problem Statement

The SHA certificate copy operation in `gcp/cloud-build/sha_copy.yaml` was reporting success ("✅ SHA certificate copy completed successfully") but verification showed that certificates were not actually being added to the Bangladesh app (0 certificates present after copy).

The build logs showed:
```
✅ SHA certificate copy completed successfully.
📋 Post-copy Bangladesh certs: 0
📋 Remaining missing certs: 1
❌ Missing certificates after copy
```

The issue requested:
- Increase logging
- Echo commands being executed
- Capture all errors and print them out

## Root Cause

The previous implementation had limited error visibility:
1. Curl errors were not being captured completely (missing `2>&1`)
2. Curl exit codes were not checked explicitly
3. Response bodies were only shown on HTTP errors, not on successful responses
4. The exact curl commands being executed were not visible
5. JSON parsing errors had minimal context

This meant that if the Firebase API returned HTTP 200 but with an error message in the response body, or if curl failed silently, we wouldn't see it.

## Solution

### 1. Enhanced Curl Error Handling

**Before:**
```bash
global_response=$(curl -sS -w '\n%{http_code}' -H "Authorization: Bearer $access_token" "...")
```

**After:**
```bash
global_response=$(curl -sS -w '\n%{http_code}' -H "Authorization: Bearer $access_token" "..." 2>&1)
global_curl_exit=$?
echo "🔍 DEBUG: curl exit code: $global_curl_exit"

if [ $global_curl_exit -ne 0 ]; then
  echo "❌ curl command failed with exit code $global_curl_exit"
  echo "❌ Full output: $global_response"
  exit 1
fi
```

**Benefits:**
- Captures stderr along with stdout (`2>&1`)
- Explicitly checks curl exit code
- Fails fast with full error output if curl command fails

### 2. Command Echo and Debug Logging

**Added for each curl operation:**
```bash
echo "🔍 DEBUG: Executing curl command..."
echo "  URL: $api_url"
echo "  Payload: $payload"
echo "  Command: curl -sS -w '\\n%{http_code}' -X POST -H 'Authorization: Bearer [REDACTED]' ..."
echo "🔍 DEBUG: curl exit code: $curl_exit_code"
echo "🔍 DEBUG: HTTP response code: $http_code"
echo "🔍 DEBUG: Response body length: ${#response_body} characters"
echo "🔍 DEBUG: Full response body:"
echo "$response_body"
```

**Benefits:**
- See exact URL and payload being sent
- See the curl command structure (with redacted token)
- See response codes and body lengths
- See full response bodies even on success (to catch API errors with HTTP 200)

### 3. Response Body Analysis

**Added:**
```bash
if [ -n "$response_body" ]; then
  echo "✅ Response details: $response_body"
else
  echo "⚠️  WARNING: Response body is empty despite HTTP $http_code"
fi
```

**Benefits:**
- Warns if response is unexpectedly empty
- Shows actual API response for successful operations

### 4. Enhanced Python Script Logging

**Before:**
```python
try:
    data = json.loads(path.read_text() or "{}")
except json.JSONDecodeError:
    print(f"❌ Invalid JSON in {path}. Raw content:")
    print(path.read_text())
    return []
```

**After:**
```python
try:
    content = path.read_text()
    print(f"🔍 DEBUG: Content of {path.name} (first 500 chars):")
    print(content[:500])
    data = json.loads(content or "{}")
except json.JSONDecodeError as e:
    print(f"❌ Invalid JSON in {path}. Error: {e}")
    print(f"❌ Raw content:")
    print(path.read_text())
    return []
```

**Benefits:**
- Shows file content during parsing (helps verify API responses are valid)
- Shows specific JSON error message
- Better context for debugging

### 5. Enhanced Verification Logging

**Added:**
```python
if target_certs:
    print(f"🔍 DEBUG: Bangladesh certificates present:")
    for sha, cert_type in target_certs:
        print(f"  - {sha} ({cert_type})")
if missing:
    print("❌ Missing certificates after copy:")
    for sha, cert_type in missing:
        print(f"- {sha} ({cert_type})")
    print(f"🔍 DEBUG: Showing content of {target_file} for troubleshooting:")
    print(Path(target_file).read_text()[:1000])
```

**Benefits:**
- Shows exactly which certificates are present
- Shows exactly which certificates are missing
- Shows raw file content when verification fails

## What We'll See Now

### When SHA Copy Works
```
🔑 Fetching SHA certificates from global app...
🔍 DEBUG: GET https://firebase.googleapis.com/v1beta1/projects/...
🔍 DEBUG: curl exit code: 0
🔍 DEBUG: HTTP response code: 200
🔍 DEBUG: Response body length: 234 characters
✅ Global SHA certificates fetched successfully

🔍 DEBUG: Content of global_sha.json (first 500 chars):
{"certificates":[{"name":"...","shaHash":"XX:XX:...","certType":"SHA_1"}]}

📋 Global certs: 1
📋 Bangladesh certs: 0
📋 Missing certs to add: 1

➕ Adding SHA certificate XX:XX:... (SHA_1) to Bangladesh app...
🔍 DEBUG: Executing curl command...
  URL: https://firebase.googleapis.com/v1beta1/projects/.../sha
  Payload: {"shaHash":"XX:XX:...","certType":"SHA_1"}
🔍 DEBUG: curl exit code: 0
🔍 DEBUG: HTTP response code: 200
🔍 DEBUG: Full response body:
{"name":"...","shaHash":"XX:XX:...","certType":"SHA_1"}
✅ Added XX:XX:... (SHA_1). HTTP 200

🔍 Verifying SHA certificates on Bangladesh app after copy...
🔁 Verification attempt 1/3...
📋 Post-copy Bangladesh certs: 1
🔍 DEBUG: Bangladesh certificates present:
  - XX:XX:... (SHA_1)
✅ Verification succeeded. All global certs are present on Bangladesh app.
```

### When SHA Copy Fails

The logs will now show EXACTLY what's failing:

**If curl fails:**
```
🔍 DEBUG: curl exit code: 6
❌ curl command failed with exit code 6 while adding XX:XX:...
❌ Full curl output/error:
curl: (6) Could not resolve host: firebase.googleapis.com
```

**If API returns an error:**
```
🔍 DEBUG: HTTP response code: 403
🔍 DEBUG: Full response body:
{"error":{"code":403,"message":"Permission denied","status":"PERMISSION_DENIED"}}
❌ Failed to add XX:XX:... (SHA_1). HTTP 403
```

**If response is empty:**
```
🔍 DEBUG: HTTP response code: 200
🔍 DEBUG: Response body length: 0 characters
⚠️  WARNING: Response body is empty despite HTTP 200
```

**If verification fails:**
```
📋 Post-copy Bangladesh certs: 0
🔍 DEBUG: Bangladesh certificates present:
(none listed)
❌ Missing certificates after copy:
- XX:XX:... (SHA_1)
🔍 DEBUG: Showing content of target_sha.json for troubleshooting:
{"certificates":[]}
```

## Security Considerations

### What's Being Logged
- URLs (contain project ID and app ID)
- SHA certificate hashes
- HTTP response codes
- API response bodies

### Why This Is Safe
1. **SHA certificates are PUBLIC keys** - They're the fingerprint of your app's signing certificate and must be registered with Firebase. They're similar to SSH public keys.
2. **Project/App IDs are not secrets** - They're visible in Firebase console and needed for debugging
3. **Access tokens ARE redacted** - We show `[REDACTED]` in command echoes
4. **Cloud Build logs have access controls** - Only authorized users can view them

### What's Redacted
- OAuth access tokens in command echoes

## Testing Recommendations

1. **Run the build** - The enhanced logging is now active
2. **Check the Cloud Build logs** - Look for DEBUG output
3. **Identify the failure point** - The logs will show exactly where and why it fails
4. **Common issues to look for:**
   - Network/DNS issues (curl exit code 6)
   - Permission issues (HTTP 403)
   - Invalid app IDs (HTTP 404)
   - Malformed requests (HTTP 400)
   - Empty responses (warning message)

## Files Modified

- `gcp/cloud-build/sha_copy.yaml` - Enhanced with comprehensive logging and error handling

## Next Steps

When the build fails again (if it does), the logs will now show:
1. The exact curl command being executed
2. The curl exit code
3. The HTTP response code
4. The full API response body
5. The exact certificate data being processed
6. Which certificates are present/missing during verification

This should make it immediately obvious WHY the SHA copy is failing.

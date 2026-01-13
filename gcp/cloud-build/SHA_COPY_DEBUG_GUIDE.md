# SHA Certificate Copy Debugging Guide

## Problem Summary

The SHA certificate copying script was claiming success but not actually copying certificates from the global app (`piotr_gorczynski.soccer2`) to variant apps (e.g., `piotr_gorczynski.soccer2.bd`).

## Root Causes Identified

### 1. **Silent JQ Failures** (CRITICAL)
The script used `|| true` on jq commands:
```bash
sha_hashes=$(echo "$sha_response" | jq -r '.certificates[]? | .shaHash // empty' || true)
```

**Problem**: If jq failed or returned an error, the script would continue with empty strings, resulting in:
- Empty arrays being created
- Zero certificates processed
- Script reporting "success" even though nothing was copied

**Fix**: Removed `|| true` and added explicit error checking:
```bash
sha_hashes=$(echo "$sha_response" | jq -r '.certificates[]? | .shaHash // empty')
jq_exit_code=$?
if [ $jq_exit_code -ne 0 ]; then
  echo "❌ ERROR: jq failed to parse SHA hashes (exit code: $jq_exit_code)"
  exit 1
fi
```

### 2. **No Verification of Certificate Addition**
The script checked HTTP status codes (200/201) but:
- Never verified certificates were actually added to Firebase
- Never re-fetched to confirm changes persisted
- Could silently fail if API accepted request but didn't persist changes

**Fix**: Added verification step that re-fetches certificates and confirms all source SHAs are present.

### 3. **Missing Failure Detection**
The script would:
- Print warning "No certificates were processed" but continue
- Allow all certificate additions to fail without exiting
- Report "✅ SHA certificate copy process completed successfully!" even when nothing was copied

**Fix**: Added critical error exits when:
- Zero certificates processed but certificates exist in source
- All certificate additions fail
- Verification shows missing certificates

### 4. **Insufficient Debugging**
Previous logs didn't show:
- Actual SHA hashes being extracted
- Array contents after parsing
- API response bodies (only HTTP codes)
- Step-by-step progress through the parsing/copying logic

**Fix**: Added comprehensive DEBUG logging throughout.

## How to Interpret the New Debug Output

### Successful Execution

Look for these key indicators:

```
🔍 DEBUG: First 500 chars of SHA response:
{"certificates":[{"name":"projects/.../sha/...","shaHash":"e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b","certType":"SHA_1"}]}

🔍 Extracting source SHA hashes for verification...
🔍 Extracted 1 source SHA hash(es) for verification
🔍 DEBUG: Source SHA hashes for verification:
  - e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b

🔍 DEBUG: SHA hashes extracted successfully
🔍 DEBUG: Raw sha_hashes variable content (showing first 500 chars):
e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b

🔍 Extracted 1 SHA hash(es) from certificates
🔍 DEBUG: SHA array contents:
  [0]: 'e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b' (length: 59)
🔍 DEBUG: Cert type array contents:
  [0]: 'SHA_1'

🔍 Processing certificate 1: e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b
📥 Adding SHA certificate: e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b (type: SHA_1)
🔍 DEBUG: Request URL: https://firebase.googleapis.com/v1beta1/projects/.../androidApps/.../sha
🔍 DEBUG: Request payload: {"shaHash": "e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b", "certType": "SHA_1"}
🔍 DEBUG: HTTP Response Code: 200
🔍 DEBUG: Response Body (first 500 chars): {"name":"projects/.../sha/...","shaHash":"e2:bf:...","certType":"SHA_1"}
✅ Successfully added SHA certificate

📊 SUMMARY for package: piotr_gorczynski.soccer2.bd
  Total certificates processed: 1
  Successfully added: 1
  Skipped (already exist): 0
  Failed: 0

🔍 VERIFICATION: Re-fetching SHA certificates from target app to confirm changes...
🔍 DEBUG: Verification response (first 500 chars):
{"certificates":[{"name":"...","shaHash":"e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b","certType":"SHA_1"}]}
✅ VERIFICATION: Found 1 SHA certificate(s) in target app after copy
🔍 DEBUG: Verified hashes in target app:
  - e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b
  ✅ e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b - PRESENT
✅ VERIFICATION PASSED: All source SHA certificates are present in target app!
```

### Common Failure Scenarios

#### Scenario 1: JQ Parsing Failure

```
❌ ERROR: jq failed to parse SHA hashes (exit code: 4)
🔍 Check /tmp/sha_response.json for the raw API response
```

**What it means**: JQ couldn't parse the JSON response from Firebase
**Action**: Check `/tmp/sha_response.json` to see if the API returned valid JSON

#### Scenario 2: Empty Extraction

```
🔍 DEBUG: SHA hashes extracted successfully
🔍 DEBUG: Raw sha_hashes variable content (showing first 500 chars):

❌ ERROR: SHA hashes extraction resulted in empty string
🔍 This means jq succeeded but found no certificates
🔍 Check /tmp/sha_response.json - the 'certificates' array may be empty or malformed
```

**What it means**: JSON parsed successfully but no certificates found
**Action**: Verify SHA certificates are added to global app in Firebase Console

#### Scenario 3: Extraction Count Mismatch

```
📋 Number of SHA certificates found: 1
🔍 Extracting source SHA hashes for verification...
🔍 Extracted 0 source SHA hash(es) for verification
❌ ERROR: Extracted 0 source SHA hashes but sha_count=1
🔍 This indicates a mismatch in how SHAs are counted vs extracted
```

**What it means**: Counting found certificates but extraction returned nothing
**Action**: Check /tmp/sha_response.json to see if the JSON structure is unexpected

#### Scenario 4: No Certificates Processed

```
❌ CRITICAL ERROR: No certificates were processed!
🔍 This indicates a problem with jq parsing or array initialization.
🔍 Raw SHA response saved to /tmp/sha_response.json for debugging
```

**What it means**: Arrays were created but all entries were empty
**Action**: Check DEBUG output for SHA array contents to see what went wrong

#### Scenario 5: All Additions Failed

```
📊 SUMMARY for package: piotr_gorczynski.soccer2.bd
  Total certificates processed: 1
  Successfully added: 0
  Skipped (already exist): 0
  Failed: 1

❌ CRITICAL ERROR: Expected to add 1 certificate(s) but added 0!
🔍 All certificate additions failed. Check the error messages above.
```

**What it means**: Certificates were parsed correctly but API calls failed
**Action**: Look for HTTP error codes and response bodies in the DEBUG output above

#### Scenario 6: Verification Failed

```
🔍 VERIFICATION: Re-fetching SHA certificates from target app to confirm changes...
🔍 DEBUG: Verification response (first 500 chars):
{"certificates":[{"shaHash":"aa:bb:cc:dd:ee:ff:..."}]}
✅ VERIFICATION: Found 1 SHA certificate(s) in target app after copy
🔍 DEBUG: Verified hashes in target app:
  - aa:bb:cc:dd:ee:ff:...
🔍 VERIFICATION: Checking if all source SHAs are present in target...
  ✅ aa:bb:cc:dd:ee:ff:... - PRESENT
  ❌ e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b - MISSING!
❌ VERIFICATION FAILED: Not all source SHA certificates are present in target app!
🔍 Missing SHAs:
e2:bf:0e:81:34:a8:aa:fe:e2:47:56:0d:5c:66:ce:eb:df:16:86:0b
🔍 Summary of what should have been copied but is missing:
  - Expected to copy: 2 SHA(s)
  - Actually present: 1 SHA(s)
  - Missing: 1 SHA(s)
```

**What it means**: API reported success but certificates aren't actually in Firebase
**Action**: This indicates a Firebase API issue - contact Firebase support or retry

## Debugging Files Created

The script now saves these files in `/tmp` for debugging:

1. `/tmp/sha_response.json` - SHA certificates from global app
2. `/tmp/target_sha_response_<package>.json` - Existing SHAs in target app (before copy)
3. `/tmp/verification_response_<package>.json` - SHA certificates in target app (after copy)

These files help diagnose issues with the Firebase API responses.

## What Changed in This Fix (PR #7)

### New Debugging Features

1. **API Response Preview**
   - Shows first 500 characters of all API responses immediately
   - Applies to: SHA fetch, target SHA fetch, verification fetch
   - Benefit: Quick visibility into what Firebase is actually returning

2. **Enhanced Source Hash Extraction**
   - Now fails immediately if extraction returns 0 hashes when count > 0
   - Shows all source hashes that will be used for verification
   - Validates extracted count matches expected count
   - Benefit: Catches extraction issues before any processing

3. **Raw Variable Content Display**
   - Shows first 500 chars of extracted `sha_hashes` variable
   - Shows first 500 chars of extracted `cert_types` variable
   - Applies to both jq and grep parsing paths
   - Benefit: See exactly what was extracted before array conversion

4. **Detailed Array Inspection**
   - Shows each array element with index and character length
   - Displays for both SHA and cert type arrays
   - Helps identify empty strings in arrays
   - Benefit: Catches the "empty array element" bug

5. **Complete POST Request Logging**
   - Logs exact URL being called
   - Logs exact JSON payload being sent
   - Shows response body (first 500 chars) for ALL requests (not just failures)
   - Shows full error response on failures
   - Benefit: Can verify request format and see API feedback

6. **Mandatory Verification**
   - Verification now fails if source_sha_hashes is not available
   - Shows first 500 chars of verification response
   - Lists all hashes found in target app
   - Provides detailed missing SHA summary with counts
   - Benefit: Ensures we always verify copying worked

7. **Failure Exit Points**
   - Extraction count mismatch → exit
   - Source hash extraction fails → exit
   - Verification parse fails → exit
   - Verification finds missing SHAs → exit with detailed report
   - No certs in target when none skipped → exit
   - Benefit: Script never claims success when copying failed

### Before (Previous Behavior)
```bash
# Silent failures allowed
sha_hashes=$(... | jq ... || true)  # Errors suppressed!

# No verification
if [[ "$http_code" == "200" ]]; then
  echo "✅ Successfully added"  # Trusted blindly
fi

# Warnings only, no exit
if [ "$certs_processed" -eq "0" ]; then
  echo "⚠️ WARNING: No certificates were processed!"
  # Script continues...
fi

# Generic success message
echo "✅ SHA certificate copy process completed successfully!"
# Even if nothing was copied!
```

### After (New Behavior)
```bash
# Catch errors
sha_hashes=$(... | jq ...)
if [ $? -ne 0 ]; then
  echo "❌ ERROR: jq failed"
  exit 1  # Stop immediately
fi

# Verify
verification_response=$(curl ... verify endpoint)
if not_all_present; then
  echo "❌ VERIFICATION FAILED"
  exit 1
fi

# Exit on critical errors
if [ "$certs_processed" -eq "0" ]; then
  echo "❌ CRITICAL ERROR: No certificates were processed!"
  exit 1  # Fail the build
fi

if [ "$expected_to_add" -gt "0" ] && [ "$certs_added" -eq "0" ]; then
  echo "❌ CRITICAL ERROR: Expected to add ... but added 0!"
  exit 1  # Fail the build
fi
```

## Testing the Fix

When you run the deployment next time:

1. **Check Cloud Build logs** for the new DEBUG output
2. **Look for CRITICAL ERROR messages** - these will now cause build failure
3. **Check the SUMMARY section** for each package
4. **Verify the VERIFICATION section** confirms SHAs are present
5. **Check Firebase Console** to confirm certificates appear

## Expected Outcomes

### If SHA Copying Works
- DEBUG output shows SHA hashes extracted
- Summary shows "Successfully added: X"
- Verification shows all SHAs PRESENT
- Build succeeds with ✅ messages

### If SHA Copying Fails
- Script will EXIT with error code
- Cloud Build will show FAILED status
- Error messages will explain exactly what failed
- Debug files in /tmp will have raw API responses

## Next Steps If Issues Persist

If you still see issues after this fix:

1. **Share the full Cloud Build log** - especially the DEBUG sections
2. **Check the files in /tmp** mentioned in error messages
3. **Verify permissions** - Cloud Build service account needs Firebase Admin role
4. **Check Firebase Console** - manually verify global app has SHA certificates
5. **Try running script with a single SHA** first to isolate the issue

## Summary

This fix transforms the script from "fail silently and claim success" to "fail loudly with detailed diagnostics". You'll now see EXACTLY where and why the SHA copying fails, making it possible to fix the actual root cause instead of repeatedly trying the same broken script.

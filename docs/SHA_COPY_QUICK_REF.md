# SHA Certificate Copy - Quick Reference Card

**Note**: The SHA copy automation now runs via the Cloud Build config `gcp/cloud-build/sha_copy.yaml`. The previous `copy-sha-certificates.sh` script has been removed.

## 🎯 What Was Fixed

The script was **failing silently** - reporting "✅ Success" even when SHA certificates were NOT copied. 

**Now it fails LOUDLY** with detailed diagnostics showing exactly what went wrong.

## ✅ Signs of SUCCESS in Logs

Look for these indicators:

```
🔍 Extracted 1 SHA hash(es) from certificates
📊 SUMMARY for package: piotr_gorczynski.soccer2.bd
  Total certificates processed: 1
  Successfully added: 1
  Skipped (already exist): 0
  Failed: 0
✅ VERIFICATION PASSED: All source SHA certificates are present in target app!
✅ Completed SHA certificate copy for package: piotr_gorczynski.soccer2.bd
```

## ❌ Signs of FAILURE in Logs

### If you see "CRITICAL ERROR"
```
❌ CRITICAL ERROR: No certificates were processed!
```
**Meaning**: Arrays were empty despite finding certificates in source
**Action**: Check DEBUG output above to see what jq/grep extracted

### If you see "VERIFICATION FAILED"
```
❌ VERIFICATION FAILED: Not all source SHA certificates are present in target app!
  ❌ XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX - MISSING!
```
**Meaning**: API accepted request but didn't persist changes
**Action**: Check Firebase Console manually, contact Firebase support

### If you see "All certificate additions failed"
```
❌ CRITICAL ERROR: Expected to add 1 certificate(s) but added 0!
```
**Meaning**: All API calls failed
**Action**: Check HTTP response codes and error messages in DEBUG output

## 🔍 Key DEBUG Points

1. **SHA Extraction**
```
🔍 DEBUG: SHA hashes extracted successfully
🔍 Extracted 1 SHA hash(es) from certificates
```

2. **Certificate Processing**
```
🔍 Processing certificate 1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
📥 Adding SHA certificate: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX (type: SHA_1)
🔍 DEBUG: HTTP Response Code: 200
✅ Successfully added SHA certificate
```

3. **Summary Counts**
```
📊 SUMMARY for package: piotr_gorczynski.soccer2.bd
  Total certificates processed: 1
  Successfully added: 1
  Skipped (already exist): 0
  Failed: 0
```

4. **Verification**
```
✅ VERIFICATION PASSED: All source SHA certificates are present in target app!
```

## 📁 Debug Files

If the script fails, check these files in Cloud Build logs:

- `/tmp/sha_response.json` - SHA certificates from global app
- `/tmp/target_sha_response_<package>.json` - Existing SHAs in target (before)
- `/tmp/verification_response_<package>.json` - SHAs in target (after)

## 🚨 What Changed vs Previous Versions

| Before (Failed 10x) | After (This Fix) |
|---------------------|------------------|
| ❌ `\|\| true` masks errors | ✅ Explicit error checking |
| ❌ No verification | ✅ Re-fetch and verify |
| ❌ Warnings only | ✅ Exit with error code |
| ❌ Minimal logging | ✅ 18 DEBUG statements |
| ❌ Silent success | ✅ Loud failure with diagnostics |

## 📋 Checklist After Deployment

- [ ] Check Cloud Build logs for "VERIFICATION PASSED"
- [ ] Verify Firebase Console shows SHAs in variant app
- [ ] If failed, review CRITICAL ERROR messages
- [ ] If failed, check /tmp debug files mentioned in errors
- [ ] If unclear, share full Cloud Build log for analysis

## 🎓 Learn More

See [SHA_COPY_DEBUG_GUIDE.md](./SHA_COPY_DEBUG_GUIDE.md) for:
- Detailed explanation of all changes
- Common failure scenarios with solutions
- How to interpret DEBUG output
- Troubleshooting guide

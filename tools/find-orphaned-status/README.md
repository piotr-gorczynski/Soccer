# Find Orphaned Status Keys

This script identifies orphaned status keys in the Realtime Database (RTDB) that don't have corresponding user documents in Firestore.

## Background

The Soccer application stores user status information in RTDB under the `status` path, with keys corresponding to user IDs. Each status key should have a matching user document in the Firestore `users` collection. Over time, some status keys may become "orphaned" if the corresponding user document is deleted or never created.

This script helps identify these orphaned status keys so they can be cleaned up if needed.

## What it does

The script performs the following operations:

1. **Fetches status keys**: Reads all keys from the `status` path in RTDB
2. **Fetches user IDs**: Reads all document IDs from the `users` collection in Firestore
3. **Compares data**: Identifies status keys that don't have a matching user document
4. **Reports findings**: Displays a list of orphaned status keys

## Prerequisites

1. **Service Account Key**: You need a service account key for the target environment in the `secrets/` directory:
   - `secrets/serviceAccountKey.prod.json` (for PROD)
   - `secrets/serviceAccountKey.test.json` (for TEST)
   - `secrets/serviceAccountKey.dev.json` (for DEV)

2. **Node.js**: Node.js version 18 or higher

3. **Dependencies**: Install dependencies before running:
   ```bash
   cd tools/find-orphaned-status
   npm install
   ```

## Usage

### Run from project root

```bash
node tools/find-orphaned-status/find-orphaned-status.js <PROD|TEST|DEV>
```

### Run from the script directory

```bash
cd tools/find-orphaned-status
node find-orphaned-status.js <PROD|TEST|DEV>
```

### Examples

Check PROD environment:
```bash
node tools/find-orphaned-status/find-orphaned-status.js PROD
```

Check TEST environment:
```bash
node tools/find-orphaned-status/find-orphaned-status.js TEST
```

Check DEV environment:
```bash
node tools/find-orphaned-status/find-orphaned-status.js DEV
```

## Example Output

### When orphaned keys are found

```
🔥 Firebase app initialized
   Environment: PROD
   Project: soccer-prod-123456

📦 Fetching status keys from RTDB...
   📊 Found 96 status key(s) in RTDB

📦 Fetching user IDs from Firestore...
   📊 Found 87 user(s) in Firestore

🔍 Analyzing data...
   📊 Total status keys: 96
   📊 Total user IDs: 87
   📊 Orphaned keys: 9

============================================================
RESULTS
============================================================

⚠️  Found 9 orphaned status key(s):

   1. user_abc123
   2. user_def456
   3. user_ghi789
   4. user_jkl012
   5. user_mno345
   6. user_pqr678
   7. user_stu901
   8. user_vwx234
   9. user_yz567

📝 These 9 status key(s) exist in RTDB but have no
   corresponding user document in Firestore.

✨ Done!
```

### When no orphaned keys are found

```
🔥 Firebase app initialized
   Environment: TEST
   Project: soccer-test-789012

📦 Fetching status keys from RTDB...
   📊 Found 45 status key(s) in RTDB

📦 Fetching user IDs from Firestore...
   📊 Found 45 user(s) in Firestore

🔍 Analyzing data...
   📊 Total status keys: 45
   📊 Total user IDs: 45
   📊 Orphaned keys: 0

============================================================
RESULTS
============================================================

✅ No orphaned status keys found!
   All status keys have corresponding user documents.

✨ Done!
```

## How it works

1. **Initialize Firebase**: Creates a Firebase Admin SDK instance for the specified environment
2. **Fetch Status Keys**: Reads all keys from the `status` path in RTDB
3. **Fetch User IDs**: Reads all document IDs from the `users` Firestore collection
4. **Compare**: Uses a Set data structure for efficient comparison to identify keys in status that don't exist in users
5. **Report**: Displays findings in a clear, readable format

## Safety Features

- **Read-only**: This script only reads data, it doesn't modify anything
- **Environment validation**: Requires a valid environment parameter (PROD, TEST, or DEV)
- **Key validation**: Checks that the service account key exists before attempting to connect
- **Error handling**: Gracefully handles errors and provides clear error messages

## Notes

- This is a **read-only diagnostic tool** - it doesn't delete or modify any data
- The script is safe to run on production environments
- Use the output to decide whether orphaned keys should be cleaned up manually
- Orphaned keys might indicate:
  - Users that were deleted but their status wasn't cleaned up
  - Data migration issues
  - Application bugs in cleanup logic

## Troubleshooting

### "Service account key not found"

Make sure you have the service account JSON file in the `secrets/` directory with the correct name:
- `secrets/serviceAccountKey.prod.json`
- `secrets/serviceAccountKey.test.json`
- `secrets/serviceAccountKey.dev.json`

### Permission errors

Ensure the service account has the following permissions:
- **Firestore**: Read access to the `users` collection
- **Realtime Database**: Read access to the `status` path

### Connection timeouts

For very large datasets, the script might take some time to fetch all data. This is normal and expected.

## Related Scripts

- **copy-prod-to-test**: Script to copy data between environments (found in `tools/copy-prod-to-test/`)
- **create-tournament**: Script to create tournaments (found in `tools/create-tournament/`)

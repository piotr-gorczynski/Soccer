# Find and Delete Orphaned Status Keys

This script identifies and optionally deletes orphaned status keys in the Realtime Database (RTDB) that don't have corresponding user documents in Firestore.

## Background

The Soccer application stores user status information in RTDB under the `status` path, with keys corresponding to user IDs. Each status key should have a matching user document in the Firestore `users` collection. Over time, some status keys may become "orphaned" if the corresponding user document is deleted or never created.

This script helps identify these orphaned status keys so they can be cleaned up if needed.

## What it does

The script performs the following operations:

1. **Fetches status keys**: Reads all keys from the `status` path in RTDB
2. **Fetches user IDs**: Reads all document IDs from the `users` collection in Firestore
3. **Compares data**: Identifies status keys that don't have a matching user document
4. **Reports findings**: Displays a list of orphaned status keys
5. **Deletes orphaned keys** (optional): When `--delete` flag is provided, removes orphaned status keys from RTDB

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

**Read-only mode (list orphaned keys):**
```bash
node tools/find-orphaned-status/find-orphaned-status.js <PROD|TEST|DEV>
```

**Delete mode (remove orphaned keys):**
```bash
node tools/find-orphaned-status/find-orphaned-status.js <PROD|TEST|DEV> --delete
```

### Run from the script directory

**Read-only mode:**
```bash
cd tools/find-orphaned-status
node find-orphaned-status.js <PROD|TEST|DEV>
```

**Delete mode:**
```bash
cd tools/find-orphaned-status
node find-orphaned-status.js <PROD|TEST|DEV> --delete
```

### Examples

**List orphaned keys** in PROD environment (read-only):
```bash
node tools/find-orphaned-status/find-orphaned-status.js PROD
```

**Delete orphaned keys** in TEST environment:
```bash
node tools/find-orphaned-status/find-orphaned-status.js TEST --delete
```

**List orphaned keys** in DEV environment:
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

### When deleting orphaned keys

```
🔥 Firebase app initialized
   Environment: TEST
   Project: soccer-test-789012
   Mode: 🗑️  DELETE

📦 Fetching status keys from RTDB...
   📊 Found 52 status key(s) in RTDB

📦 Fetching user IDs from Firestore...
   📊 Found 48 user(s) in Firestore

🔍 Analyzing data...
   📊 Total status keys: 52
   📊 Total user IDs: 48
   📊 Orphaned keys: 4

============================================================
RESULTS
============================================================

⚠️  Found 4 orphaned status key(s):

   1. user_abc123
   2. user_def456
   3. user_ghi789
   4. user_jkl012

📝 These 4 status key(s) exist in RTDB but have no
   corresponding user document in Firestore.

============================================================
DELETION
============================================================

🗑️  Deleting orphaned status keys...
   ✅ Deleted: user_abc123 (1/4)
   ✅ Deleted: user_def456 (2/4)
   ✅ Deleted: user_ghi789 (3/4)
   ✅ Deleted: user_jkl012 (4/4)

   📊 Deletion complete:
      ✅ Successfully deleted: 4

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

- **Read-only by default**: Without the `--delete` flag, the script only reads data and doesn't modify anything
- **Explicit opt-in for deletion**: Deletion only happens when the `--delete` flag is explicitly provided
- **Environment validation**: Requires a valid environment parameter (PROD, TEST, or DEV)
- **Key validation**: Checks that the service account key exists before attempting to connect
- **Error handling**: Gracefully handles errors and provides clear error messages
- **Detailed logging**: Shows progress for each deletion operation with success/failure status

## Notes

- By default, this is a **read-only diagnostic tool** - it doesn't delete or modify any data
- With the `--delete` flag, the script will permanently remove orphaned status keys from RTDB
- **Always run in read-only mode first** to review what will be deleted
- The script is safe to run on production environments in read-only mode
- **Use caution with `--delete` in production** - ensure you've reviewed the list of orphaned keys first
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
- **Realtime Database**: 
  - Read access to the `status` path
  - Write/delete access to the `status` path (only needed when using `--delete` flag)

### Connection timeouts

For very large datasets, the script might take some time to fetch all data. This is normal and expected.

## Related Scripts

- **copy-prod-to-test**: Script to copy data between environments (found in `tools/copy-prod-to-test/`)
- **create-tournament**: Script to create tournaments (found in `tools/create-tournament/`)

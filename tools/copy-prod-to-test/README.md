# Copy Data from PROD to TEST

This script copies data from the PROD Firebase database to the TEST Firebase database.

## What it does

The script copies data from the following sources:

**Firestore Collections:**
- `invitations`
- `matches`
- `regulations`
- `settings`
- `tournaments`
- `users`

**Realtime Database:**
- `status`

**Authentication:**
- All user accounts with their:
  - Email, displayName, photoURL
  - Email verification status
  - Disabled/enabled status
  - Password hashes (when available)
  - Custom claims
  - Provider data
  - Phone numbers
  - Account metadata (creation and last sign-in times)

## Prerequisites

1. **Service Account Keys**: You need service account keys for both PROD and TEST environments in the `secrets/` directory:
   - `secrets/serviceAccountKey.prod.json`
   - `secrets/serviceAccountKey.test.json`

2. **Node.js**: Node.js version 18 or higher

3. **Dependencies**: Install dependencies before running:
   ```bash
   cd tools/copy-prod-to-test
   npm install
   ```

## Usage

### Run from project root

```bash
node tools/copy-prod-to-test/copy-prod-to-test.js
```

### Run from the script directory

```bash
cd tools/copy-prod-to-test
node copy-prod-to-test.js
```

## Options

### Dry Run Mode

Test the script without actually writing data to TEST:

```bash
node tools/copy-prod-to-test/copy-prod-to-test.js --dry-run
```

This will:
- Connect to both databases
- Read data from PROD
- Show what would be copied
- **NOT** write anything to TEST

### Clear Target Mode

Delete existing data in TEST before copying from PROD:

```bash
node tools/copy-prod-to-test/copy-prod-to-test.js --clear-target
```

⚠️ **Warning**: This will delete all existing data in the TEST collections before copying!

### Combine Options

You can combine options, but note that `--dry-run` takes precedence:

```bash
node tools/copy-prod-to-test/copy-prod-to-test.js --dry-run --clear-target
```

## How it works

1. **Initialize Firebase Apps**: Creates two separate Firebase Admin SDK instances for PROD and TEST
2. **Copy Firestore Collections**: 
   - Reads all documents from each collection in PROD
   - Writes them to TEST using batched writes (for efficiency)
   - Uses merge mode by default (won't overwrite if document already exists)
3. **Copy Realtime Database**: 
   - Reads the entire `status` path from PROD RTDB
   - Writes it to TEST RTDB (replaces existing data at that path)
4. **Copy Authentication Users**:
   - Lists all users from PROD Authentication
   - Exports user data including credentials, metadata, and custom claims
   - Imports users to TEST Authentication in batches (1000 users per batch)
   - Preserves password hashes when available
   - Updates existing users if they already exist in TEST
5. **Summary**: Shows a summary of what was copied

## Notes

- **Batch Operations**: The script uses Firestore batch writes (max 500 operations per batch) for efficiency
- **Merge Mode**: By default, existing documents in TEST are merged with PROD data (not replaced)
- **RTDB Behavior**: The RTDB `status` path is completely replaced (not merged)
- **Authentication Import**: Uses Firebase Admin SDK's `importUsers` API which:
  - Preserves user UIDs
  - Preserves password hashes (users can log in with same passwords)
  - Updates existing users if they already exist in TEST
  - Processes up to 1000 users per batch
  - Does NOT delete users that exist in TEST but not in PROD
- **Large Collections**: The script handles large collections by processing them in batches
- **Error Handling**: Errors are logged but the script continues processing other collections

## Safety Features

- Requires service account keys to be present before running
- Shows project IDs before copying to confirm correct databases
- Dry run mode to preview changes
- Clear summary of all operations performed

## Example Output

```
🔥 Firebase apps initialized
   PROD project: soccer-prod-123456
   TEST project: soccer-test-789012

============================================================
FIRESTORE COLLECTIONS
============================================================

📦 Copying Firestore collection: invitations
   📊 Found 42 document(s) in PROD
   ✅ Committed final batch of 42 documents
   ✅ Successfully copied 42 document(s)

📦 Copying Firestore collection: matches
   📊 Found 156 document(s) in PROD
   ✅ Committed final batch of 156 documents
   ✅ Successfully copied 156 document(s)

...

============================================================
REALTIME DATABASE
============================================================

📦 Copying RTDB path: status
   📊 Found 23 key(s) in PROD
   ✅ Successfully copied 23 key(s)

============================================================
AUTHENTICATION
============================================================

📦 Copying Authentication users
   📊 Counting users in PROD...
   📊 Found 127 user(s) in PROD
   ✅ Imported batch of 127 user(s)
   ✅ Successfully copied 127 user(s)

============================================================
SUMMARY
============================================================

Firestore Collections:
  ✅ invitations: 42 document(s) copied
  ✅ matches: 156 document(s) copied
  ✅ regulations: 3 document(s) copied
  ✅ settings: 1 document(s) copied
  ✅ tournaments: 8 document(s) copied
  ✅ users: 67 document(s) copied

Realtime Database:
  ✅ status: 23 key(s) copied

Authentication:
  ✅ users: 127 user(s) copied

✨ Done!
```

## Troubleshooting

### "Service account key not found"

Make sure you have the service account JSON files in the `secrets/` directory:
- `secrets/serviceAccountKey.prod.json`
- `secrets/serviceAccountKey.test.json`

### Permission errors

Ensure the service accounts have the following permissions:
- **Firestore**: Read access on PROD, Write access on TEST
- **Realtime Database**: Read access on PROD, Write access on TEST
- **Authentication**: Read access on PROD, Write/Import access on TEST

### Connection timeouts

For very large collections, you might need to run the script multiple times or increase Node.js timeout limits.

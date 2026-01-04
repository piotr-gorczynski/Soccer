# Copy Data from TEST to DEV

This script copies data from the TEST Firebase database to the DEV Firebase database.

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

1. **Service Account Keys**: You need service account keys for both TEST and DEV environments in the `secrets/` directory:
   - `secrets/serviceAccountKey.test.json`
   - `secrets/serviceAccountKey.dev.json`

2. **Node.js**: Node.js version 18 or higher

3. **Dependencies**: Install dependencies before running:
   ```bash
   cd tools/copy-test-to-dev
   npm install
   ```

## Usage

### Basic Usage

⚠️ **Important**: This script will **delete all existing data** in DEV before copying from TEST, including:
- All Firestore collections **and their subcollections** (recursively deleted)
- All Realtime Database paths
- All Authentication users

Run from project root:

```bash
node tools/copy-test-to-dev/copy-test-to-dev.js
```

Run from the script directory:

```bash
cd tools/copy-test-to-dev
node copy-test-to-dev.js
```

## Options

### Dry Run Mode

Test the script without actually writing data to DEV:

```bash
node tools/copy-test-to-dev/copy-test-to-dev.js --dry-run
```

This will:
- Connect to both databases
- Read data from TEST
- Show what would be copied
- **NOT** write anything to DEV
- **NOT** delete anything from DEV

### Combine Options

The `--dry-run` flag can be used alone. The `--clear-target` flag has been removed as clearing is now the default behavior.

## How it works

1. **Initialize Firebase Apps**: Creates two separate Firebase Admin SDK instances for TEST and DEV
2. **Clear DEV Data** (always performed unless `--dry-run` is used):
   - Deletes all Firestore collections **and their subcollections recursively**
   - Deletes all Realtime Database paths  
   - Deletes all Authentication users
3. **Copy Firestore Collections**: 
   - Reads all documents from each collection in TEST
   - **Recursively copies all subcollections** (e.g., `matches/{id}/moves`, `tournaments/{id}/matches`)
   - Writes to DEV (will merge with any remaining data)
4. **Copy Realtime Database**: 
   - Reads the entire `status` path from TEST RTDB
   - Writes it to DEV RTDB
5. **Copy Authentication Users**:
   - Lists all users from TEST Authentication
   - Exports user data including credentials, metadata, and custom claims
   - Imports users to DEV Authentication in batches (1000 users per batch)
   - Preserves password hashes when available
6. **Summary**: Shows a summary of what was copied

## Notes

- **Subcollection Handling**: The script recursively copies and deletes all subcollections to any depth
  - All subcollections in DEV are deleted recursively before copying to ensure a clean state
  - All subcollections from TEST are copied recursively
  - This prevents orphaned subcollections (e.g., old `moves` under `matches` that no longer exist in TEST)
- **Clean State**: DEV is always cleared before copying (unless using `--dry-run`) to ensure it's an exact replica of TEST
- **RTDB Behavior**: The RTDB `status` path is completely replaced
- **Authentication Import**: Uses Firebase Admin SDK's `importUsers` API which:
  - Preserves user UIDs
  - Preserves password hashes (users can log in with same passwords)
  - Processes up to 1000 users per batch
  - All existing users in DEV are deleted before importing from TEST
- **Large Collections**: The script handles large collections by processing them one document at a time with progress logging
- **Error Handling**: Errors are logged but the script continues processing other collections

## Safety Features

- Requires service account keys to be present before running
- Shows project IDs before copying to confirm correct databases
- Dry run mode to preview changes
- **Always clears DEV before copying** to ensure clean state and prevent orphaned subcollections
- Clear summary of all operations performed

## Example Output

```
🔥 Firebase apps initialized
   TEST project: soccer-test-789012
   DEV project: soccer-dev-345678

⚠️  CLEAR AND COPY MODE - Existing data in DEV will be deleted before copying

============================================================
FIRESTORE COLLECTIONS
============================================================

🗑️  Clearing DEV collection: invitations
   ✅ Cleared 35 document(s) with subcollections from DEV

📦 Copying Firestore collection: invitations
   📊 Found 42 document(s) in TEST
   ✅ Successfully copied 42 document(s) with subcollections

🗑️  Clearing DEV collection: matches
   ✅ Cleared 120 document(s) with subcollections from DEV

📦 Copying Firestore collection: matches
   📊 Found 156 document(s) in TEST
   ✅ Successfully copied 156 document(s) with subcollections

...

============================================================
REALTIME DATABASE
============================================================

🗑️  Clearing DEV RTDB path: status
   ✅ Cleared path from DEV

📦 Copying RTDB path: status
   📊 Found 23 key(s) in TEST
   ✅ Successfully copied 23 key(s)

============================================================
AUTHENTICATION
============================================================

🗑️  Clearing DEV Authentication users
   📊 Counting users in DEV...
   📊 Found 98 user(s) in DEV
   ✅ Successfully deleted 98 user(s)

📦 Copying Authentication users
   📊 Counting users in TEST...
   📊 Found 127 user(s) in TEST
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
  🗑️  cleared: 98 user(s) deleted
  ✅ users: 127 user(s) copied

✨ Done!
```

## Troubleshooting

### "Service account key not found"

Make sure you have the service account JSON files in the `secrets/` directory:
- `secrets/serviceAccountKey.test.json`
- `secrets/serviceAccountKey.dev.json`

### Permission errors

Ensure the service accounts have the following permissions:
- **Firestore**: Read access on TEST, Write access on DEV
- **Realtime Database**: Read access on TEST, Write access on DEV
- **Authentication**: Read access on TEST, Write/Import access on DEV

### Connection timeouts

For very large collections, you might need to run the script multiple times or increase Node.js timeout limits.

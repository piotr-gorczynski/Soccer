# Find Orphaned Invitations

This script identifies and optionally deletes orphaned invitations from the Firestore database.

## What are Orphaned Invitations?

Orphaned invitations are invitation documents in the `invitations` collection where **BOTH** the sender (`from` field) and receiver (`to` field) users no longer exist in the `users` collection.

The script specifically looks for invitations where:
- The `from` user ID does not exist in the users collection, AND
- The `to` user ID does not exist in the users collection

If either user still exists, the invitation is NOT considered orphaned.

## Prerequisites

- Node.js installed
- Firebase Admin SDK dependencies installed (`npm install` from the root directory)
- Service account key file in the `secrets` directory:
  - `secrets/serviceAccountKey.dev.json` for DEV environment
  - `secrets/serviceAccountKey.test.json` for TEST environment
  - `secrets/serviceAccountKey.prod.json` for PROD environment

## Usage

### Read-Only Mode (Default)

Check for orphaned invitations without deleting them:

```bash
node find-orphaned-invitations.js <ENV>
```

Examples:
```bash
node find-orphaned-invitations.js DEV
node find-orphaned-invitations.js TEST
node find-orphaned-invitations.js PROD
```

### Delete Mode

To actually delete the orphaned invitations, add the `--delete` flag:

```bash
node find-orphaned-invitations.js <ENV> --delete
```

Examples:
```bash
node find-orphaned-invitations.js DEV --delete
node find-orphaned-invitations.js TEST --delete
node find-orphaned-invitations.js PROD --delete
```

## Output

The script provides detailed output including:
- Number of invitations found
- Number of users found
- Number of orphaned invitations identified
- Details of each orphaned invitation (ID, from user, to user)
- Deletion progress and results (when using --delete flag)

## Safety Features

- **Read-only by default**: The script requires explicit `--delete` flag to perform deletions
- **Environment selection**: Must specify DEV, TEST, or PROD environment
- **Detailed logging**: Shows exactly what will be deleted before performing any operations
- **Error handling**: Reports any errors during deletion process

## Example Output

```
🔥 Firebase app initialized
   Environment: PROD
   Project: my-project
   Mode: 👀 READ-ONLY

📦 Fetching invitations from Firestore...
   📊 Found 150 invitation(s) in Firestore

📦 Fetching user IDs from Firestore...
   📊 Found 1000 user(s) in Firestore

🔍 Analyzing data...
   📊 Total invitations: 150
   📊 Total user IDs: 1000
   📊 Orphaned invitations: 3

============================================================
RESULTS
============================================================

⚠️  Found 3 orphaned invitation(s):

   1. ID: abc123
      from: user999 (user does not exist)
      to: user888 (user does not exist)
   2. ID: def456
      from: user777 (user does not exist)
      to: user666 (user does not exist)
   3. ID: ghi789
      from: user555 (user does not exist)
      to: user444 (user does not exist)

📝 These 3 invitation(s) exist in Firestore but both
   the sender (from) and receiver (to) users no longer exist.

💡 To delete these orphaned invitations, run the script with --delete flag:
   node find-orphaned-invitations.js PROD --delete

✨ Done!
```

## Related

This script was created based on the pattern established by `find-orphaned-status.js` (issue #834).

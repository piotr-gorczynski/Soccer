# Create Tournament Tool

This script creates a new tournament in the Soccer app's Firestore database with support for multi-flavour visibility.

## Purpose

Use this tool to create tournaments that can be:
- Visible globally in all app flavours (default)
- Visible only in specific flavours (e.g., Bangladesh-only tournaments)

## Prerequisites

1. **Service Account Keys**: You need a service account key for the target environment in the `secrets/` directory:
   - `secrets/serviceAccountKey.dev.json`
   - `secrets/serviceAccountKey.test.json`
   - `secrets/serviceAccountKey.prod.json`

2. **Node.js**: Node.js version 18 or higher

3. **Dependencies**: Install dependencies before running:
   ```bash
   cd tools/create-tournament
   npm install
   ```

4. **Active Regulation**: The regulation document you reference must exist in Firestore and have `status: "active"`

## Usage

There are two ways to create a tournament:

### Method 1: Command-Line Arguments

```bash
node create-tournament.js <env> <name> <maxParticipants> <registrationDeadline> <matchesDeadline> <regulation>
```

**Parameters:**
- `<env>`: Environment (`dev`, `test`, or `prod`)
- `<name>`: Tournament name (e.g., "Summer Championship")
- `<maxParticipants>`: Maximum number of participants (integer)
- `<registrationDeadline>`: Registration deadline (ISO 8601 date format)
- `<matchesDeadline>`: Matches deadline (ISO 8601 date format)
- `<regulation>`: Document ID of the regulation to use

**Example:**
```bash
node create-tournament.js prod "Summer Championship" 16 "2024-06-01T00:00:00Z" "2024-06-30T23:59:59Z" "standard-rules-v1"
```

**Limitations:**
- This method creates tournaments visible in **all flavours** by default (uses `["global"]`)
- To create flavour-specific tournaments, use Method 2 with a JSON file

### Method 2: JSON Configuration File (Recommended)

Create a JSON file with tournament parameters:

```bash
node create-tournament.js <env> <config-file.json>
```

**Example JSON file (`tournament-config.json`):**
```json
{
  "name": "💰 Bangladesh Championship",
  "maxParticipants": 16,
  "registrationDeadline": "2024-06-01T00:00:00Z",
  "matchesDeadline": "2024-06-30T23:59:59Z",
  "regulation": "standard-rules-v1",
  "visibleInFlavours": ["bangladesh"]
}
```

**Run it:**
```bash
node create-tournament.js prod tournament-config.json
```

## Creating Bangladesh-Only Tournaments

To create a tournament visible **only in the Bangladesh flavour**, use a JSON configuration file with `"visibleInFlavours": ["bangladesh"]`:

**Step 1**: Create a configuration file (e.g., `bangladesh-tournament.json`):
```json
{
  "name": "💰 Bangladesh Summer Cup",
  "maxParticipants": 16,
  "registrationDeadline": "2024-07-01T00:00:00Z",
  "matchesDeadline": "2024-07-31T23:59:59Z",
  "regulation": "bangladesh-rules",
  "visibleInFlavours": ["bangladesh"]
}
```

**Step 2**: Run the script:
```bash
# Navigate to the script directory
cd tools/create-tournament

# Install dependencies (if not already done)
npm install

# Create the tournament in production
node create-tournament.js prod bangladesh-tournament.json
```

**Expected Output:**
```
Tournament created with ID: xyz123abc456
Visible in flavours: bangladesh
```

## Flavour Visibility Options

The `visibleInFlavours` field controls where tournaments appear:

| Configuration | Visibility |
|---------------|------------|
| `["global"]` (default) | Visible in **all app flavours** (Global, Bangladesh, etc.) |
| `["bangladesh"]` | Visible **only in Bangladesh flavour** |
| Not specified | Defaults to `["global"]` (visible everywhere) |

**Note**: `"global"` means "visible globally/everywhere", not "only in the global app variant".

## What the Script Does

1. Validates that the environment is `dev`, `test`, or `prod`
2. Loads the appropriate service account key from `secrets/serviceAccountKey.{env}.json`
3. Initializes Firebase Admin SDK with Firestore access
4. Parses tournament parameters (from command-line or JSON file)
5. Validates that required fields are provided
6. Sets `visibleInFlavours` to `["global"]` if not specified
7. Converts date strings to Firestore Timestamps
8. Validates that the regulation document exists and is active
9. Creates the tournament document with:
   - All provided parameters
   - `format: "RoundRobin"` (default format)
   - `status: "registering"` (initial status)
   - `participantsCount: 0` (starts with zero participants)
   - `createdAt: <current timestamp>`
   - `visibleInFlavours: ["global"]` or custom value
10. Logs the created tournament ID and flavour visibility

## Complete Example: Bangladesh Tournament

**Scenario**: Create a Bangladesh-only tournament with prizes for the test environment.

**1. Create `bd-prize-tournament.json`:**
```json
{
  "name": "🏆 Bangladesh Prize Cup 2024",
  "maxParticipants": 32,
  "registrationDeadline": "2024-08-01T00:00:00Z",
  "matchesDeadline": "2024-08-31T23:59:59Z",
  "regulation": "prize-tournament-rules",
  "visibleInFlavours": ["bangladesh"]
}
```

**2. Run the command:**
```bash
cd tools/create-tournament
node create-tournament.js test bd-prize-tournament.json
```

**3. Verify the output:**
```
Tournament created with ID: abc123def456
Visible in flavours: bangladesh
```

**4. Check in Firebase Console:**
- Navigate to Firestore → `tournaments` collection
- Find the document with ID `abc123def456`
- Verify `visibleInFlavours: ["bangladesh"]`

## Troubleshooting

### "First argument must specify the environment: dev, test or prod"
You forgot to specify the environment. The first argument must be `dev`, `test`, or `prod`.

**Fix:**
```bash
node create-tournament.js prod tournament.json  # ✅ Correct
```

### "Service account key not found"
The script cannot find the service account key file.

**Fix:**
- Ensure the file exists: `secrets/serviceAccountKey.{env}.json`
- Check the file path is correct (two directories up from the script)
- Verify you have the correct environment name (`dev`, `test`, or `prod`)

### "Regulation document not found"
The regulation ID you specified doesn't exist in Firestore.

**Fix:**
- Check the regulation document ID in Firebase Console
- Ensure you're using the correct environment
- Verify the regulation document exists in the `regulations` collection

### "Regulation is not active"
The regulation exists but its `status` field is not set to `"active"`.

**Fix:**
- Update the regulation document in Firestore
- Set `status: "active"` on the regulation document
- Or use a different regulation that is already active

### "Missing required parameters"
One or more required fields are missing.

**Fix:**
Ensure your JSON file or command-line arguments include all required fields:
- `name`
- `maxParticipants`
- `registrationDeadline`
- `matchesDeadline`
- `regulation`

### Invalid Date Format
Dates must be in ISO 8601 format.

**Fix:**
Use format: `YYYY-MM-DDTHH:mm:ssZ`

**Examples:**
- ✅ `"2024-06-01T00:00:00Z"`
- ✅ `"2024-12-31T23:59:59Z"`
- ❌ `"2024-06-01"` (missing time)
- ❌ `"06/01/2024"` (wrong format)

## Examples

### Example 1: Global Tournament (Visible Everywhere)
```bash
# Using JSON
echo '{
  "name": "Global Championship",
  "maxParticipants": 16,
  "registrationDeadline": "2024-09-01T00:00:00Z",
  "matchesDeadline": "2024-09-30T23:59:59Z",
  "regulation": "standard-v1"
}' > global-tournament.json

node create-tournament.js prod global-tournament.json
```

### Example 2: Bangladesh-Only Tournament
```bash
# Using JSON
echo '{
  "name": "💰 Bangladesh Prize League",
  "maxParticipants": 32,
  "registrationDeadline": "2024-10-01T00:00:00Z",
  "matchesDeadline": "2024-10-31T23:59:59Z",
  "regulation": "prize-rules-v1",
  "visibleInFlavours": ["bangladesh"]
}' > bd-tournament.json

node create-tournament.js prod bd-tournament.json
```

### Example 3: Using Command-Line (Global Only)
```bash
# Creates a global tournament (no Bangladesh-only option via command-line)
node create-tournament.js test "Test Tournament" 8 "2024-11-01T00:00:00Z" "2024-11-30T23:59:59Z" "test-rules"
```

## Additional Notes

- **Default Behavior**: Tournaments without `visibleInFlavours` or with `["global"]` are visible in all flavours
- **Command-line Limitation**: The command-line method (Method 1) does not support custom `visibleInFlavours`. Use JSON configuration (Method 2) for flavour-specific tournaments.
- **Date Format**: Always use ISO 8601 format with timezone (e.g., `2024-06-01T00:00:00Z`)
- **Tournament Format**: Currently, all tournaments are created with `format: "RoundRobin"`
- **Initial Status**: All new tournaments start with `status: "registering"`
- **Participants Count**: Starts at 0 and increments as users join
- **Idempotency**: Running the script multiple times creates multiple tournaments (not idempotent)

## Related Tools

- **migrate-tournaments-flavours**: Adds `visibleInFlavours` field to existing tournaments
- See `tools/migrate-tournaments-flavours/README.md` for migration details

## Security

- Keep service account keys secure and never commit them to version control
- The `secrets/` directory is in `.gitignore` to prevent accidental commits
- Use appropriate Firebase security rules to protect tournament data

# Tournament Flavours Migration Script

This script migrates existing tournaments to support multi-flavour visibility by adding a `visibleInFlavours` field.

## Purpose

The Soccer app now supports multiple product flavours (global and Bangladesh). To control which tournaments are visible in each flavour, we need to add a `visibleInFlavours` field to all tournament documents.

## Schema Change

The script adds the following field to each tournament document:

```javascript
{
  // ... existing fields ...
  visibleInFlavours: ["global"]  // "global" means visible in all flavours
}
```

## Default Behavior

By default, all existing tournaments are migrated to be visible in **all flavours** using `["global"]`. This ensures backward compatibility and prevents any tournaments from disappearing after migration.

**Note**: `"global"` in the visibleInFlavours array means "visible globally/everywhere", not "only in the global app variant".

## Usage

```bash
# Navigate to the script directory
cd tools/migrate-tournaments-flavours

# Install dependencies (if not already installed)
npm install

# Run migration for desired environment
node migrate-tournaments-flavours.js dev    # Development environment
node migrate-tournaments-flavours.js test   # Test environment
node migrate-tournaments-flavours.js prod   # Production environment

# Or use npm scripts
npm run migrate:dev
npm run migrate:test
npm run migrate:prod
```

## What the Script Does

1. Connects to Firestore using the service account for the specified environment
2. Fetches all existing tournaments
3. For each tournament:
   - Checks if it already has the `visibleInFlavours` field
   - If not, adds `visibleInFlavours: ["global", "bangladesh"]`
   - Logs the migration status
4. Provides a summary of migrated, skipped, and failed tournaments

## Safety Features

- **Idempotent**: Can be run multiple times safely. Tournaments already migrated are skipped.
- **Non-destructive**: Only adds a new field, doesn't modify existing data
- **Error handling**: Continues processing even if individual tournaments fail
- **Detailed logging**: Shows exactly what happened to each tournament

## Future Tournament Configuration

After migration, you can manually configure specific tournaments:

### Global tournament (visible in all flavours):
```javascript
{
  visibleInFlavours: ["global"]  // Recommended - simple and clear
}
```

### Bangladesh-only tournament (with prizes):
```javascript
{
  visibleInFlavours: ["bangladesh"]
}
```

**Note**: `["global", "bangladesh"]` also means visible everywhere, but `["global"]` alone is simpler and has the same effect.
```

## Example Output

```
🔄 Starting tournament flavour migration for environment: prod

Found 7 tournament(s) to migrate.

✅ Migrated 2VPmEJA6pgoVZGEU9qoK ("🏁 New Year Kickstart League"): added visibleInFlavours: ["global"]
✅ Migrated AYnQgeaeb365kG1v4uLg ("🔥 January Momentum Cup"): added visibleInFlavours: ["global"]
✅ Migrated AvJpQXVHDDT4PBvHyjPb ("🌨️ Winter Final Sprint"): added visibleInFlavours: ["global"]
✅ Migrated HMwZEjzzpn9oZlreYwnn ("🏆 Opening Tournament 🏆"): added visibleInFlavours: ["global"]
✅ Migrated USzUlCRfq5w3W9m2wtWa ("🍾 New Year Countdown Clash"): added visibleInFlavours: ["global"]
✅ Migrated XaOdzq6T3vdcxh4xhEog ("❄️ February Frost Clash"): added visibleInFlavours: ["global"]
✅ Migrated v0E5lHTtYrePb4qIdFMk ("❄️ December Tournament ❄️"): added visibleInFlavours: ["global"]

📊 Migration Summary:
   Total tournaments: 7
   ✅ Migrated: 7
   ⏭️  Skipped (already migrated): 0
   ❌ Errors: 0

✨ Migration completed!
```

## Prerequisites

- Node.js installed
- Firebase Admin SDK access
- Service account key files in `secrets/serviceAccountKey.{env}.json`
- Appropriate permissions to modify Firestore documents

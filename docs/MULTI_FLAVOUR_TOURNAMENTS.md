# Multi-Flavour Tournament Support

**Document Version:** 1.0  
**Last Updated:** 2026-02-01  
**Status:** Implemented

## Overview

The Soccer app now supports multiple product flavours (Global and Bangladesh), with the ability to control which tournaments are visible in each flavour. This allows for:

- **Global tournaments**: Visible to all users across all app flavours
- **Bangladesh-specific tournaments**: Visible only to users of the Bangladesh variant (with cash prizes)
- **Flexible visibility**: Any combination of flavours can be configured per tournament

## Technical Implementation

### Backend Schema Changes

#### Tournament Document Schema

Each tournament document in the `tournaments` Firestore collection now includes a `visibleInFlavours` field:

```javascript
{
  // Existing fields
  name: "🏆 Tournament Name",
  status: "registering",
  format: "RoundRobin",
  maxParticipants: 10,
  registrationDeadline: Timestamp,
  matchesDeadline: Timestamp,
  regulation: "regulationId",
  participantsCount: 0,
  createdAt: Timestamp,
  
  // New field for multi-flavour support
  visibleInFlavours: ["global"]  // "global" means visible in all flavours
}
```

#### Field Specification

- **Field Name**: `visibleInFlavours`
- **Type**: `Array<string>`
- **Values**: 
  - `["global"]` - Visible in all app flavours (recommended for most tournaments)
  - `["bangladesh"]` - Visible only in Bangladesh variant
  - `["global", "bangladesh"]` - Also visible in all flavours (same as `["global"]`)
- **Default**: `["global"]` (visible in all flavours)
- **Required**: No (for backward compatibility)

### Frontend Implementation

#### Flavour Detection

A new utility class `AppFlavourDetector` determines the current app flavour based on the package name:

```java
String currentFlavour = AppFlavourDetector.getCurrentFlavour(context);
// Returns: "global" or "bangladesh"
```

**Detection Logic**:
- Package name ends with `.bd` → `"bangladesh"`
- All other cases → `"global"`

#### Tournament Filtering

The `TournamentsActivity` filters tournaments based on the current flavour:

```java
// Get current flavour
final String currentFlavour = AppFlavourDetector.getCurrentFlavour(this);

// Filter tournaments in snapshot listener
for (DocumentSnapshot doc : snap.getDocuments()) {
    // Check if tournament is visible in current flavour
    List<String> visibleInFlavours = (List<String>) doc.get("visibleInFlavours");
    if (visibleInFlavours != null) {
        // "global" means visible in all flavours
        // "bangladesh" means visible only in bangladesh flavour
        if (visibleInFlavours.contains("global")) {
            // Tournament is global - visible everywhere
        } else if (!visibleInFlavours.contains(currentFlavour)) {
            // Skip this tournament - not visible in current flavour
            continue;
        }
    }
    
    // Process visible tournaments...
}
```

**Backward Compatibility**: If `visibleInFlavours` field is missing, the tournament is shown (assumes visible in all flavours).

**Semantic Meaning**:
- `"global"` in the array means "visible globally/everywhere"
- `"bangladesh"` means "specific to Bangladesh variant only"

### Migration Process

#### Automated Migration Script

A migration script is provided to add the `visibleInFlavours` field to all existing tournaments:

**Location**: `tools/migrate-tournaments-flavours/`

**Usage**:
```bash
cd tools/migrate-tournaments-flavours
npm install
node migrate-tournaments-flavours.js prod
```

**What it does**:
1. Fetches all existing tournaments from Firestore
2. Adds `visibleInFlavours: ["global", "bangladesh"]` to each tournament
3. Skips tournaments that already have the field (idempotent)
4. Provides detailed logging and summary

See `tools/migrate-tournaments-flavours/README.md` for full documentation.

### Creating New Tournaments

#### Using the create-tournament Script

The `create-tournament` script has been updated to support the new field:

**With JSON file**:
```bash
node create-tournament.js prod tournament.json
```

Example `tournament.json`:
```json
{
  "name": "🏆 Bangladesh Championship",
  "maxParticipants": 10,
  "registrationDeadline": "2026-03-01T00:59:00Z",
  "matchesDeadline": "2026-03-15T00:59:00Z",
  "regulation": "regulationId",
  "visibleInFlavours": ["bangladesh"]
}
```

**With command-line arguments**:
```bash
node create-tournament.js prod "Tournament Name" 10 "2026-03-01" "2026-03-15" "regulationId"
```

**Note**: Command-line creation defaults to `["global"]`. Use JSON file for custom visibility.

#### Manually in Firestore Console

When creating tournaments manually:

1. Open Firebase Console → Firestore Database
2. Navigate to `tournaments` collection
3. Create new document with all required fields
4. Add `visibleInFlavours` field:
   - Type: `array`
   - Values: `"global"` (for all flavours) or `"bangladesh"` (Bangladesh only)

### Tournament Visibility Configurations

#### Global Tournament (Visible Everywhere)
```javascript
{
  visibleInFlavours: ["global"]  // Simplest - just use "global"
}
```
**Use Case**: General tournaments accessible to all users regardless of app variant. This is the recommended default.

**Note**: You can also use `["global", "bangladesh"]` but `["global"]` alone has the same effect and is simpler.

#### Bangladesh-Only Tournament (With Cash Prizes)
```javascript
{
  visibleInFlavours: ["bangladesh"]
}
```
**Use Case**: Tournaments with cash prizes, only visible in the Bangladesh variant which has 18+ age rating and prize support.

### Security Considerations

#### Firestore Security Rules

The existing Firestore security rules remain unchanged. Tournaments are readable by all authenticated users:

```javascript
match /tournaments/{tournamentId} {
  allow get, list: if request.auth != null;
  
  match /{subDoc=**} {
    allow get, list: if request.auth != null;
  }
}
```

**Important**: The `visibleInFlavours` field is enforced client-side. While this is appropriate for UI filtering, be aware that:
- Users can technically read all tournament documents via Firestore
- The filtering only controls UI visibility, not data access
- This is acceptable since tournaments are public information

If server-side enforcement is needed in the future, consider:
- Creating separate collections per flavour
- Adding server-side filtering in Cloud Functions
- Implementing more restrictive security rules

## Testing

### Manual Testing Checklist

#### Global Variant Testing
1. **Build and install Global variant**:
   ```bash
   ./gradlew _prodGlobalRelease
   adb install mobile/app/build/outputs/apk/global/release/app-global-release.apk
   ```

2. **Verify tournament visibility**:
   - Open Tournaments screen
   - Verify tournaments with `visibleInFlavours: ["global"]` are visible
   - Verify tournaments with `visibleInFlavours: ["global", "bangladesh"]` are visible
   - Verify tournaments with `visibleInFlavours: ["bangladesh"]` are NOT visible

#### Bangladesh Variant Testing
1. **Build and install Bangladesh variant**:
   ```bash
   ./gradlew _prodBangladeshRelease
   adb install mobile/app/build/outputs/apk/bangladesh/release/app-bangladesh-release.apk
   ```

2. **Verify tournament visibility**:
   - Open Tournaments screen
   - Verify tournaments with `visibleInFlavours: ["bangladesh"]` are visible
   - Verify tournaments with `visibleInFlavours: ["global", "bangladesh"]` are visible
   - Verify tournaments with `visibleInFlavours: ["global"]` are NOT visible

#### Test Scenarios

**Scenario 1: Mixed Tournament List**
- Create 3 tournaments:
  1. Global-only: `["global"]`
  2. Bangladesh-only: `["bangladesh"]`
  3. Both: `["global", "bangladesh"]`
- Expected in Global: Tournaments 1 and 3
- Expected in Bangladesh: Tournaments 2 and 3

**Scenario 2: Backward Compatibility**
- Create tournament without `visibleInFlavours` field
- Expected: Visible in both Global and Bangladesh variants

**Scenario 3: Empty Array**
- Create tournament with `visibleInFlavours: []`
- Expected: Not visible in any variant

### Automated Testing

A unit test has been added to verify flavour detection:

**Location**: `mobile/app/src/test/java/piotr_gorczynski/soccer2/AppFlavourDetectorTest.java`

```bash
# Run tests
./gradlew test
```

## Operational Guidelines

### Creating Bangladesh Prize Tournaments

When creating tournaments with cash prizes for Bangladesh users:

1. **Set visibility to Bangladesh-only**:
   ```json
   {
     "visibleInFlavours": ["bangladesh"]
   }
   ```

2. **Use appropriate naming**:
   - Include prize information in tournament name
   - Example: `"💰 ৳2,000 Championship 💰"`

3. **Set appropriate deadlines**:
   - Registration deadline: At least 2 weeks for promotion
   - Matches deadline: Allow sufficient time for all matches

4. **Document prize details**:
   - Add prize information to tournament regulation document
   - Ensure terms & conditions are clear

### Monitoring and Analytics

Track tournament visibility effectiveness:

1. **Firebase Analytics Events**:
   - `tournament_list_viewed` - includes flavour context
   - `tournament_joined` - track by flavour
   - `tournament_completed` - track by flavour

2. **Firestore Queries** (for analysis):
   ```javascript
   // Count Bangladesh-only tournaments
   db.collection('tournaments')
     .where('visibleInFlavours', 'array-contains', 'bangladesh')
     .get()
   ```

### Troubleshooting

#### Problem: Tournament not appearing in expected flavour

**Check**:
1. Verify `visibleInFlavours` field in Firestore Console
2. Verify field is an array (not string)
3. Check for typos in flavour names (`"global"`, `"bangladesh"`)
4. Ensure user is authenticated
5. Check Firestore security rules

#### Problem: Tournament appearing in wrong flavour

**Check**:
1. Clear app data and restart
2. Verify package name matches expected flavour:
   - Global: `piotr_gorczynski.soccer2`
   - Bangladesh: `piotr_gorczynski.soccer2.bd`
3. Rebuild app to ensure correct variant

#### Problem: Migration script errors

**Check**:
1. Service account key file exists: `secrets/serviceAccountKey.{env}.json`
2. Service account has Firestore write permissions
3. Internet connection is stable
4. Re-run script (it's idempotent)

## Future Enhancements

### Potential Improvements

1. **Server-side filtering**: Move filtering to Cloud Functions for stronger enforcement
2. **Dynamic flavours**: Support for additional flavours without code changes
3. **User-level visibility**: Allow tournaments to target specific user segments
4. **A/B testing**: Randomized tournament visibility for testing
5. **Scheduled visibility**: Tournaments that become visible at specific times

### Extensibility

The current implementation is designed to be easily extensible:

- **Adding new flavours**: Simply add new flavour names to the array
- **Complex filtering**: The client-side filtering logic can be enhanced
- **Admin UI**: Build admin interface for managing tournament visibility

## Related Documentation

- [BANGLADESH_VERSION_APPROACH.md](BANGLADESH_VERSION_APPROACH.md) - Overall Bangladesh variant strategy
- [tools/migrate-tournaments-flavours/README.md](../tools/migrate-tournaments-flavours/README.md) - Migration script documentation
- [tools/create-tournament/](../tools/create-tournament/) - Tournament creation script

## Change Log

- **2026-02-01**: Initial implementation of multi-flavour tournament support
  - Added `visibleInFlavours` schema field
  - Created migration script
  - Updated frontend filtering
  - Updated create-tournament script
  - Added documentation

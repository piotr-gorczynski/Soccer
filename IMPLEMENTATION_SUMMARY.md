# Implementation Summary: Multi-Flavour Tournament Support

## Overview

This implementation adds support for multi-flavour tournament visibility in the Soccer (Gridline Soccer) mobile application. The feature allows tournaments to be selectively visible in different app variants (Global and Bangladesh).

## Changes Made

### 1. Backend Schema Changes

#### Tournament Schema Extension
- **Added field**: `visibleInFlavours` (Array of strings)
- **Possible values**: `["global"]`, `["bangladesh"]`, or `["global", "bangladesh"]`
- **Default**: `["global", "bangladesh"]` (visible in all flavours)
- **Backward compatible**: Tournaments without this field are visible in all flavours

**Example Tournament Document**:
```json
{
  "name": "💰 Bangladesh Championship",
  "status": "registering",
  "format": "RoundRobin",
  "maxParticipants": 10,
  "visibleInFlavours": ["bangladesh"],
  // ... other fields
}
```

### 2. Migration Script

**Location**: `tools/migrate-tournaments-flavours/`

**Features**:
- Adds `visibleInFlavours: ["global", "bangladesh"]` to all existing tournaments
- Idempotent (can be run multiple times safely)
- Detailed logging and error handling
- Non-destructive (only adds new field)

**Usage**:
```bash
cd tools/migrate-tournaments-flavours
npm install
node migrate-tournaments-flavours.js prod
```

### 3. Tournament Creation Tool Update

**Updated**: `tools/create-tournament/create-tournament.js`

**Changes**:
- Accepts `visibleInFlavours` parameter in JSON configuration
- Defaults to `["global", "bangladesh"]` if not specified
- Logs the flavour visibility when creating tournaments

**Usage with custom visibility**:
```bash
node create-tournament.js prod tournament.json
```

Where `tournament.json` contains:
```json
{
  "name": "Tournament Name",
  "maxParticipants": 10,
  "registrationDeadline": "2026-03-01T00:59:00Z",
  "matchesDeadline": "2026-03-15T00:59:00Z",
  "regulation": "regulationId",
  "visibleInFlavours": ["bangladesh"]
}
```

### 4. Mobile App Changes

#### AppFlavourDetector Utility (New)
**Location**: `mobile/app/src/main/java/piotr_gorczynski/soccer2/AppFlavourDetector.java`

**Purpose**: Detects current app flavour based on package name

**API**:
```java
String flavour = AppFlavourDetector.getCurrentFlavour(context);
// Returns: "global" or "bangladesh"

boolean isBD = AppFlavourDetector.isBangladeshFlavour(context);
boolean isGlobal = AppFlavourDetector.isGlobalFlavour(context);
```

**Detection Logic**:
- Package name ends with `.bd` → `"bangladesh"`
- All other cases → `"global"`

#### TournamentsActivity Update
**Location**: `mobile/app/src/main/java/piotr_gorczynski/soccer2/TournamentsActivity.java`

**Changes**:
- Detects current app flavour on activity creation
- Filters tournaments based on `visibleInFlavours` field in snapshot listener
- Maintains backward compatibility (tournaments without field are shown)

**Filtering Logic**:
```java
final String currentFlavour = AppFlavourDetector.getCurrentFlavour(this);

for (DocumentSnapshot doc : snap.getDocuments()) {
    List<String> visibleInFlavours = (List<String>) doc.get("visibleInFlavours");
    if (visibleInFlavours != null && !visibleInFlavours.contains(currentFlavour)) {
        continue; // Skip tournament
    }
    // Process visible tournaments...
}
```

### 5. Testing

#### Unit Tests (New)
**Location**: `mobile/app/src/test/java/piotr_gorczynski/soccer2/AppFlavourDetectorTest.java`

**Coverage**:
- Global package detection
- Bangladesh package detection
- Helper method tests (`isBangladeshFlavour`, `isGlobalFlavour`)
- Edge cases (unknown packages, .bd in middle of name)

### 6. Documentation

#### Comprehensive Technical Documentation (New)
**Location**: `docs/MULTI_FLAVOUR_TOURNAMENTS.md`

**Contents**:
- Overview and technical implementation details
- Backend schema specification
- Frontend filtering logic
- Migration process and tools
- Creating new tournaments
- Security considerations
- Testing guidelines
- Troubleshooting guide
- Future enhancements

#### Bangladesh Approach Document Update
**Location**: `docs/BANGLADESH_VERSION_APPROACH.md`

**Changes**:
- Added reference to new `MULTI_FLAVOUR_TOURNAMENTS.md` documentation
- Updated Tournament Structure section
- Replaced old `region: "BD"` approach with new `visibleInFlavours` approach
- Added implementation status and key features summary

## Technical Approach

### Why Client-Side Filtering?

The implementation uses **client-side filtering** rather than server-side for several reasons:

1. **Simplicity**: No changes to Cloud Functions or Firestore queries needed
2. **Performance**: All tournaments are fetched in a single query (as before)
3. **Flexibility**: Easy to extend with additional flavours or filtering logic
4. **Security**: Tournament data is public information; no sensitive data exposure
5. **Backward Compatibility**: Existing code continues to work

### Security Considerations

- **Firestore Rules**: Unchanged; all authenticated users can read tournaments
- **Data Access**: Users can technically read all tournaments via direct Firestore access
- **UI Filtering**: The `visibleInFlavours` field controls UI visibility, not data access
- **Acceptable**: Tournaments are public information with no sensitive data

**Note**: If server-side enforcement is required in the future, consider:
- Separate collections per flavour
- Cloud Function filtering
- More restrictive security rules

## Use Cases

### 1. Global Tournament (Visible Everywhere)
```json
{
  "visibleInFlavours": ["global", "bangladesh"]
}
```
**When to use**: General tournaments for all users

### 2. Bangladesh-Only Tournament (Cash Prizes)
```json
{
  "visibleInFlavours": ["bangladesh"]
}
```
**When to use**: Tournaments with cash prizes (18+ requirement, Bangladesh-specific)

### 3. Global-Only Tournament
```json
{
  "visibleInFlavours": ["global"]
}
```
**When to use**: Tournaments that should not appear in Bangladesh variant

## Migration Path

1. **Run migration script** to add `visibleInFlavours` to existing tournaments
2. **Deploy updated app** with filtering logic
3. **Create new tournaments** with appropriate visibility settings
4. **Monitor** tournament participation by flavour

## Future Enhancements

### Potential Improvements
- Server-side filtering via Cloud Functions
- Dynamic flavours without code changes
- User-level visibility targeting
- A/B testing capabilities
- Scheduled visibility (time-based)
- Admin UI for managing visibility

### Extensibility
The implementation is designed for easy extension:
- **New flavours**: Just add to the array
- **Complex logic**: Client-side filtering can be enhanced
- **Admin tools**: Build UI for managing tournament visibility

## Testing Recommendations

### Manual Testing
1. **Build both variants** (Global and Bangladesh)
2. **Create test tournaments** with different visibility settings
3. **Verify filtering** in each variant
4. **Test backward compatibility** with tournaments missing the field

### Automated Testing
- Run unit tests: `./gradlew test`
- Verify AppFlavourDetector logic
- Check compilation of new code

## Files Changed

### New Files
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/AppFlavourDetector.java`
- `mobile/app/src/test/java/piotr_gorczynski/soccer2/AppFlavourDetectorTest.java`
- `tools/migrate-tournaments-flavours/migrate-tournaments-flavours.js`
- `tools/migrate-tournaments-flavours/package.json`
- `tools/migrate-tournaments-flavours/README.md`
- `docs/MULTI_FLAVOUR_TOURNAMENTS.md`

### Modified Files
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/TournamentsActivity.java`
- `tools/create-tournament/create-tournament.js`
- `docs/BANGLADESH_VERSION_APPROACH.md`

## Deployment Checklist

- [ ] Review and merge PR
- [ ] Run migration script on dev environment
- [ ] Test in dev environment
- [ ] Run migration script on test environment
- [ ] Test in test environment
- [ ] Deploy updated mobile app to test track
- [ ] Verify filtering works correctly
- [ ] Run migration script on prod environment
- [ ] Deploy updated mobile app to production
- [ ] Monitor analytics for tournament participation

## Support and Maintenance

### Documentation
- Primary: `docs/MULTI_FLAVOUR_TOURNAMENTS.md`
- Context: `docs/BANGLADESH_VERSION_APPROACH.md`
- Migration: `tools/migrate-tournaments-flavours/README.md`

### Common Issues
See troubleshooting section in `docs/MULTI_FLAVOUR_TOURNAMENTS.md`

### Contact
For questions or issues, refer to project documentation or repository issues.

---

**Implementation Date**: 2026-02-01  
**Version**: 1.0  
**Status**: ✅ Complete and Ready for Review

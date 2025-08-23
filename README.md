# Soccer

This is very simple game which originates from so called paper soccer

## Recent Fixes

### Facebook Profile Photo Fix
Fixed an issue where Facebook users saw silhouette/blank profile photos instead of their actual photos. The app now uses Facebook Graph API with the user's access token to fetch the real profile photo URL instead of the generic `graph.facebook.com/{id}/picture` URL which returns a blank image for non-public profiles.

**Technical details**: Uses `GET https://graph.facebook.com/v20.0/{facebookId}/picture?type=large&redirect=0&access_token={token}` to get the real CDN URL and stores that in Firestore.

## Creating tournaments

A helper script is available in `tools/create-tournament/create-tournament.js` for quickly adding tournaments to Firestore. **Run it from the project root** so the relative path is resolved correctly:

```bash
node tools/create-tournament/create-tournament.js dev "Summer Cup" 16 "2024-06-01T12:00:00Z" "2024-07-01T12:00:00Z" "regDocId"
```

If you change into the `tools/create-tournament` directory first, drop the folder prefix:

```bash
node create-tournament.js dev "Summer Cup" 16 "2024-06-01T12:00:00Z" "2024-07-01T12:00:00Z" "regDocId"
```

The script also accepts a path to a JSON file containing the fields `name`, `maxParticipants`, `registrationDeadline`, `matchesDeadline` and `regulation`:

```bash
node tools/create-tournament/create-tournament.js dev params.json
```

## Android release signing

The production keystore and passwords are kept outside of version control.
Create a `keystore.properties` file inside the `secrets/` directory with your
release signing details:

```properties
KEYSTORE_FILE=secrets/keystore.jks
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

`secrets/keystore.properties` is ignored by Git so your credentials remain
private.

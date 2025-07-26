# Soccer

This is very simple game which originates from so called paper soccer

## Creating tournaments

A helper script is available in `tools/create-tournament.js` for quickly adding tournaments to Firestore. **Run it from the project root** so the relative path is resolved correctly:

```bash
node tools/create-tournament.js "Summer Cup" 16 "2024-06-01T12:00:00Z" "2024-07-01T12:00:00Z" "regDocId"
```

If you change into the `tools` directory first, drop the folder prefix:

```bash
node create-tournament.js "Summer Cup" 16 "2024-06-01T12:00:00Z" "2024-07-01T12:00:00Z" "regDocId"
```

The script also accepts a path to a JSON file containing the fields `name`, `maxParticipants`, `registrationDeadline`, `matchesDeadline` and `regulation`:

```bash
node tools/create-tournament.js params.json
```

See [docs/custom-auth-domain.md](docs/custom-auth-domain.md) for instructions on configuring a custom Firebase Authentication domain.

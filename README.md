# Soccer

This is very simple game which originates from so called paper soccer
## Creating tournaments

A helper script is available in `tools/create-tournament.js` for quickly adding tournaments to Firestore.

```
node tools/create-tournament.js "Summer Cup" 16 "2024-06-01T12:00:00Z" "2024-07-01T12:00:00Z"
```

The script also accepts a path to a JSON file containing the fields `name`, `maxParticipants`, `registrationDeadline` and `matchesDeadline`:

```
node tools/create-tournament.js params.json
```

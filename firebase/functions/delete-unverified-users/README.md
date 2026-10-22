# Unverified email cleanup

Runs every 60 minutes. Only Firebase Authentication accounts whose sole provider
is `password`, whose email is present and explicitly unverified, and whose
creation time is more than one hour ago are eligible. The one-hour threshold
is unchanged. Terms acceptance and optional Firestore `method` fields do not
determine eligibility.

Anonymous/custom accounts (no providers), OAuth/phone accounts and accounts linked
to any non-password provider are excluded. Missing/invalid Auth metadata also
excludes an account. Auth is read again immediately before deletion to check for
email verification or provider linking since pagination; Firebase Auth does not
provide an atomic conditional delete, so a narrow read/delete race remains.

Existing Auth deletion cleanup preserves minimized profile/legal history and
deactivates presence. This fix does not restore previously deleted accounts.

Run `npm test` for eligibility and pre-deletion recheck tests. These source changes
must be deployed to each environment before its scheduled function uses them.

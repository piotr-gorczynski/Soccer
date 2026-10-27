# Unverified email cleanup

Runs every 60 minutes. Auth candidates have only the password provider, an
explicitly unverified email and a creation time more than one hour ago.
Before deletion, Firestore is checked: a saved nonblank nickname, accepted Terms
or accepted legacy Global Terms protects an established profile. This includes
guest accounts converted to email while verification is pending. Optional
Firestore method fields do not determine Auth eligibility. Failed profile reads
skip deletion rather than treating the profile as missing.

Anonymous/custom accounts, OAuth/phone accounts and accounts linked to any
non-password provider are excluded. Auth is re-read immediately before deletion.
Firebase Auth has no atomic conditional delete; a narrow read/delete race remains.
Existing Auth deletion cleanup preserves minimized profile/legal history and
deactivates presence. Previously deleted accounts are not restored.

Run npm test. Deploy this function to DEV before testing pending verification,
then TEST/PROD through the normal promotion process. The new app alone does not
change the already deployed scheduled cleanup.

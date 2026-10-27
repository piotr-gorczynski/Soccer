# Linked email verification

Anonymous-to-email linking preserves the same UID, nickname, legal acceptances
and active Firebase session. Successful delivery displays the destination and
keeps the user signed in; ordinary email registration keeps its existing flow.

Account exposes resend, correct email and check verification while an email is
unverified or a corrected address is pending. Correction uses Firebase
verifyBeforeUpdateEmail: the destination is stored locally per UID only after
successful delivery, and Auth changes the address only after link redemption.
Resend uses that pending destination. If Firebase requires recent credentials,
the user reauthenticates without signing out before retrying correction.
Check reloads Auth, confirms verification of the pending destination, refreshes
the ID token, and merges only the confirmed email into the existing profile.
Failures retain the session and pending recovery state. Explicit logout warns
about losing access while verification is pending.

Strings are Android resources with English defaults and Bengali translations.
No email address, password or verification-link token is added to telemetry.

## Deployment and manual DEV test

Deploy the updated delete-unverified-users function to DEV before testing with
an established account. The app build alone does not protect an account from an
older deployed scheduler. Promote that backend change to TEST/PROD before
releasing the new app. Do not run the cleanup manually against real users as a test.

1. Build devBangladeshDebug; Start Playing, accept the required documents and save a test nickname.
2. Link a controlled test mailbox (a typo test must use an address/domain you control).
3. Confirm the mail-sent dialog. Account stays open and the session stays signed in.
4. Resend and confirm delivery, then check verification before opening a link: it stays pending.
5. Correct to a second controlled mailbox; confirm the message names that mailbox.
6. Restart the app; Account still shows the pending corrected destination. Resend targets it.
7. Try logout and cancel its warning; the account remains accessible.
8. Redeem the corrected-address link and tap Check verification. The verified address is saved;
   UID, nickname, Terms and progress stay unchanged. The recovery panel disappears.
9. Log out and sign in with the corrected address and the original password.

Automated tests cover delivery/confirmation without sign-out, pending correction
persistence, resend targeting, old-address verification, reload/write/network
failure, session changes, UI actions/logout warning and cleanup exclusions.

Reference: https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseUser

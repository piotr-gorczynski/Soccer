# Cleanup regression tests

Run `node --test eligibility.test.js` from this directory.

Coverage:
- Only password-only, explicitly unverified Auth accounts older than one hour
  are initial candidates; anonymous, OAuth and phone accounts are excluded.
- Auth eligibility is rechecked just before deletion.
- Saved nicknames, accepted Terms and accepted legacy Terms protect established
  profiles, including anonymous-to-email conversions awaiting verification.
- Missing profiles retain the previous cleanup eligibility.
- Failed Firestore reads never result in account deletion.

These are unit tests with fake Auth/Firestore objects; they do not delete real
accounts. Source changes require deployment before the scheduler uses them.

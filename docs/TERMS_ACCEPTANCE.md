# Versioned Terms acceptance

Implementation originally prepared 2026-10-07. The dated verification and release notes below distinguish repository changes from deployment/publication. This is an implementation record, not legal approval of the text.

## Required documents

`TermsPolicy` is the authoritative Android mapping. Activities do not contain version strings.

| Scope | Required version | Immutable URL |
| --- | --- | --- |
| Bangladesh | `BD-terms-2026-10-06` | `https://piotr-gorczynski.com/bangladesh/terms/BD-terms-2026-10-06.html` |
| Global | `GLOBAL-terms-2026-10-13` | `https://piotr-gorczynski.com/terms/GLOBAL-terms-2026-10-13/{language}.html` |

The current Global documents are dated 2026-10-13 and include tournament participation and fair-play terms. The 2025-07-30 snapshots remain unchanged for historical evidence and older clients. Translations now cover all 20 supported UI languages: am, ar, bn, de, en, es, fa, fr, hi, km, lo, mg, mn, my, ne, pl, si, so, sw, ur. Global falls back to English only for unsupported language codes; Bangladesh displays Bengali for UI language `bn` (`BD-terms-2026-10-06-bn.html`) and English otherwise (`BD-terms-2026-10-06.html`). Its privacy links use `bangladesh/privacy-bn.html` or `bangladesh/privacy-en.html` by the same rule. Both translations share the Terms version; changing UI language does not invalidate an existing valid acceptance or overwrite its original document language. Publish the Bengali document and deploy its Firestore URL/hash allowlist before releasing this app change. The recorded language is the document language, not necessarily the UI language. Existing profile notification-language preferences are preserved.

Exact SHA-256 values are in `firebase-hosting/terms-document-hashes.json`, `TermsPolicy`, and the Firestore allowlist. `.gitattributes` preserves immutable HTML bytes across checkouts. Only HTML is hashed, not CSS. `TermsDocumentLoader` downloads HTTPS HTML without redirects and verifies the exact hash before enabling acceptance. An HTTP error, missing page, captive portal or changed HTML fails closed. The versioned Bangladesh HTML is prepared without a draft banner for future publication but is not effective or public merely because it exists locally. The unversioned `bangladesh/terms-en.html` remains an unpublished editorial draft; it is not an acceptance target.

## Evidence model

Path: `users/{uid}/legalAcceptances/terms_{scope}__{version}`.

Example (illustrative app metadata; timestamp is stored as Firestore Timestamp, not text):

```json
{
  "documentType": "terms",
  "scope": "bangladesh",
  "version": "BD-terms-2026-10-06",
  "acceptedAt": "<server timestamp>",
  "language": "en",
  "documentUrl": "https://piotr-gorczynski.com/bangladesh/terms/BD-terms-2026-10-06.html",
  "documentSha256": "e77d67d705baec188e277fe8c1cfdcbbad4d061e0581a64e6c884ede8c76ebcc",
  "appVersionCode": 100,
  "appVersionName": "example",
  "flavor": "_devBangladesh"
}
```

A Firestore transaction creates the record and merges compatibility fields atomically. Repeated acceptance of an existing valid record is a no-op. It never overwrites its timestamp, language or app metadata. Firestore rules allow only the authenticated owner to read/create, require all and only the ten approved fields, match the ID, approved version/language/URL/hash and `acceptedAt == request.time`, and deny client updates/deletes. Admin SDK access remains governed by IAM. No IP, device identifier, recipient name or payout details are recorded.

## Gating and legacy compatibility

- Bangladesh always requires the exact current Bangladesh record. A Global record or `termsAccepted=true` cannot satisfy it.
- Global requires its own current-version record. The fixed legacy boundary remains `GLOBAL-terms-2025-07-30`; it cannot satisfy the new `GLOBAL-terms-2026-10-13` requirement. Existing users, including legacy profiles with `termsAccepted=true`, must explicitly accept the new version in the updated app.
- When a modern acceptance occurs, `termsAcceptanceModel=2` and `legacyGlobalTermsAccepted` preserve only the pre-existing legacy eligibility. Therefore accepting Bangladesh for a new account does not create Global eligibility via the compatibility boolean. Changing the required Global version disables grandfathering because the fixed boundary is not changed.
- `termsAccepted`, `termsAcceptanceDate` and `language` remain deprecated compatibility fields for older apps. They cannot demonstrate versioned consent. `AddFriendActivity` still uses the other user's legacy flag for search visibility; it is not the current user's Terms gate and must not read another user's private legal records.
- Menu entry checks and authenticated navigation use server-confirmed reads. Notification entry points use `TermsEntryActivity` before opening the destination. Existing clients are not retroactively changed; this is client gating in the new release, not a new backend authorization policy for every gameplay API.
- Read-only `TermsActivity.READ_ONLY=true` hides Accept and neither writes acceptance nor signs out on close. Viewing a website Terms link also creates no record. Privacy links and tournament regulation acceptance remain independent.
- A save failure stays on the Terms screen with retry enabled; only committed success finishes the acceptance flow. Cancel/back signs out in required mode. No offline cache bypass is introduced: checks use `Source.SERVER`, transactions require connectivity, and pending local timestamps do not match. Even previously accepted users need connectivity for these gates in this implementation.

No bulk migration, acceptance reset or backfill is required or permitted. Existing Bangladesh users explicitly accept on their next gated use of the updated app. Historical versioned records are preserved. Account deletion behavior is unchanged; do not add legal-record erasure until retention and accountability requirements have been reviewed.

## Future update / release procedure

1. Review the new Terms and any required notices. Create a new immutable HTML, e.g. `BD-terms-2027-01-15.html`; never edit a published version in place.
2. Compute its exact SHA-256, add it to the manifest, and update the required version/URL/hash in `TermsPolicy`. Do not move the fixed Global legacy boundary. Add/update each required Global translation for a Global version bump.
3. Update the Firestore approved-document allowlist for the new version and tests. Retain old records and approved old versions as needed for supported older clients; an old record never satisfies the new required version.
4. Run Android policy/UI tests, both flavor builds and `npm test` in `tools/test-payout-rules` (Firestore emulator). Validate that manifest, rules and Android hashes agree.
5. After separate publication approval, publish the immutable pages and necessary linked resources, preserving existing live content. Verify deployed bytes against the hashes. Publish documents and deploy rules **before** releasing an app requiring them. Until this happens new acceptance will fail safely; do not release this app build yet.
6. Release the app. Users without the exact scope/version acceptance are prompted. Previous records remain untouched. No backend function or data migration is required for this change.

## Privacy review / release prerequisite

The effective Bangladesh Privacy Policy is `BD-privacy-2026-10-09`, effective 9 October 2026, in English and Bengali. The 9 October finalization changes only status/version labels, not substantive content. The earlier review of `BD-privacy-2026-10-06` identified a need to explain general-Terms evidence (scope/version, immutable URL/hash, accepted document language and app version/flavor). Finalizing the policy status does not itself resolve that content-review finding or establish a legal retention duration. No automatic payout retention period is applied to legal evidence.

## Verification scope

Focused Android tests cover scope isolation, current/old versions, legacy compatibility and future version boundary, metadata, hash mismatch, read-only UI and failed-write retry. Emulator tests cover exact approved documents, owner access, timestamps, immutability, invalid fields and failed atomic writes. Publication/network validation against the new live URLs and an on-device acceptance smoke test remain release steps because these pages must remain unpublished in this task.

Verified locally on 2026-10-07: `_devGlobalDebug` and `_devBangladeshDebug` builds succeeded; 17 focused Terms/filter tests passed in each flavor (34 executions). The payout/retention/Terms Firestore emulator suite passed 19/19. `git diff --check` passed. Gradle reports pre-existing deprecated features incompatible with Gradle 10; no deployment was performed.

Account-deletion follow-up (2026-10-07): `ACCOUNT_DELETION.md` now defines profile replacement without deleting legalAcceptances. A separate unpublished privacy revision documents this design; The approved 10 October retention for legalAcceptances is five calendar years from accountDeletedAt; the scheduled implementation is described in ACCOUNT_DELETION.md. Tournament participant evidence is unchanged. Earlier statements above about the effective policy remain true for the live website.

## Legal acceptance retention — 10 October 2026

Superseding earlier unresolved-retention notes: immutable `legalAcceptances` survive
account deletion for five calendar years from the tombstone's `accountDeletedAt`.
The daily 03:30 UTC `cleanupModerationAndLegalData` then recursively removes that
collection. Acceptance records are never updated to add expiry fields. Active users,
missing deletion timestamps, tombstones and tournament/game history are not deleted.
This implementation is not deployed by this change.

## Global Terms verification — 13 October 2026 (issue #1293)

- At the initial language-coverage change, the required Global version remained `GLOBAL-terms-2025-07-30` (superseded below). Added translations of the same terms for am, ar, fa, km, lo, mg, mn, my, si, so and sw; no existing immutable HTML or Bangladesh Terms were changed. This is language coverage, not a new substantive Terms version or a reset of existing consent.
- Existing text includes nickname impersonation, abuse, discrimination and moderation restrictions. No Bangladesh age/prize provisions are imported into Global. This technical/content consistency check is not a legal certification.
- The website homepage now links directly to the immutable Global documents in all 20 languages. Android chooses the selected UI language and verifies exact HTML bytes before enabling acceptance. The manifest, Android hashes and Firestore approved-document allowlist include every translation.
- PASS: 16 focused Android Terms tests, including the new-user Terms screen and successful acceptance for all 20 Global languages, language changes, outdated version rejection, failed loads/writes, read-only mode and legacy compatibility. `_devGlobalDebug` APK build succeeded.
- PASS: 6 Firestore emulator tests, including acceptance of every translation, server timestamps, owner-only access, immutability and rejection of incorrect document metadata. Language coverage and HTML hashes are checked against the actual Android language registry.
- No Hosting or Firestore deployment was performed. Publication is assigned to the repository owner. Publish the new HTML and deploy the updated Firestore rules before distributing an Android build that selects the new translations. Hosting publication alone does not update already-installed Android clients or the rules allowlist.
- After publication, verify the live new document hashes against `firebase-hosting/terms-document-hashes.json` and smoke-test acceptance with a new account in the released app. These live checks are not claimed as completed by the local tests.

## Global tournament terms — 13 October 2026

Supersedes the earlier same-day language-only verification above. `GLOBAL-terms-2026-10-13` adds the supplied tournament section in all 20 languages: assigned rules and eligibility, gameplay/deadlines/scoring, skill-based results, free entry without purchases/fees/deposits/stakes/wagers/payments, no guaranteed schedule, fair play and consequences of leaving/disconnecting/inactivity. The public `terms-*.html` pages and homepage links use the new version. Bangladesh documents are unchanged.

The current manifest stores the new hashes; `terms-document-hashes-2025-07-30.json` preserves the historical manifest. Firestore accepts both approved versions for supported old clients, while the updated app requires the new version. Historical acceptances are neither overwritten nor migrated. Publish the new HTML and deploy the new rules before releasing the updated app. No publication or deployment is implied by this repository change.

Verification for the tournament revision: PASS — 16 focused Android Terms tests and the _devGlobalDebug build; PASS — 7 Firestore emulator tests, including every current translation and historical-version compatibility. Live publication/device verification remains a release step.

# Versioned Terms acceptance

Implementation prepared 2026-10-07. Not deployed or published; an Android release must wait for the documents and rules below. This is an implementation record, not legal approval of the text.

## Required documents

`TermsPolicy` is the authoritative Android mapping. Activities do not contain version strings.

| Scope | Required version | Immutable URL |
| --- | --- | --- |
| Bangladesh | `BD-terms-2026-10-06` | `https://piotr-gorczynski.com/bangladesh/terms/BD-terms-2026-10-06.html` |
| Global | `GLOBAL-terms-2025-07-30` | `https://piotr-gorczynski.com/terms/GLOBAL-terms-2025-07-30/{language}.html` |

The current Global documents in the repository say 2025-07-30. The snapshots preserve those bytes; this identifies new acceptances only, not the text previously accepted by legacy users. Existing translations: bn, de, en, es, fr, hi, ne, pl, ur. Global falls back to English for other UI languages; Bangladesh currently displays English. The recorded language is the document language, not necessarily the UI language. Existing profile notification-language preferences are preserved.

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
- Global requires its own record, except for the explicit current-release legacy compatibility boundary `GLOBAL-terms-2025-07-30`. An unmarked legacy profile with `termsAccepted=true` continues to work, without creating any acceptance record or asserting which text was previously accepted.
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

The effective `BD-privacy-2026-10-06` describes tournament eligibility confirmations and timestamps, but does not explicitly describe the new general-Terms evidence: scope/version, immutable URL/hash, accepted document language and app version/flavor. It also still describes shared general Terms. A reviewed amendment should explain the legal/accountability purpose, applicable retention/deletion treatment and the separate Bangladesh Terms before this feature is released. No automatic payout retention period is applied to legal evidence. This task does not change the effective Privacy Policy or decide a legal retention duration.

## Verification scope

Focused Android tests cover scope isolation, current/old versions, legacy compatibility and future version boundary, metadata, hash mismatch, read-only UI and failed-write retry. Emulator tests cover exact approved documents, owner access, timestamps, immutability, invalid fields and failed atomic writes. Publication/network validation against the new live URLs and an on-device acceptance smoke test remain release steps because these pages must remain unpublished in this task.

Verified locally on 2026-10-07: `_devGlobalDebug` and `_devBangladeshDebug` builds succeeded; 17 focused Terms/filter tests passed in each flavor (34 executions). The payout/retention/Terms Firestore emulator suite passed 19/19. `git diff --check` passed. Gradle reports pre-existing deprecated features incompatible with Gradle 10; no deployment was performed.

Account-deletion follow-up (2026-10-07): `ACCOUNT_DELETION.md` now defines profile replacement without deleting legalAcceptances. A separate unpublished privacy revision documents this design; retention durations for legal evidence still require approval. Earlier statements above about the effective policy remain true for the live website.

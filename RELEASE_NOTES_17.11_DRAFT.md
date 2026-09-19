# Release 17.11 – Draft

## Regulation management and Bangladesh tournament preparation

- Added `tools/create-regulation`, a JSON-based CLI for creating Firestore regulations in the `dev`, `test`, and `prod` environments.
- Added full input validation and dry-run support before any Firestore write.
- Regulations use native Firestore document IDs and preserve the existing `regulations/{id}/{language}/rules` localization layout.
- Added optional structured regulation metadata for market eligibility, minimum age, and cash-prize payout methods without breaking existing regulation documents.
- Added a generic prize-pool definition with total amount and per-place awards, allowing future prize structures without introducing hard-coded variant identifiers.
- Root and localized regulation documents are created atomically in one Firestore batch.
- Added automated tests and a Bangladesh example regulation.
- Successfully validated the complete import on the `dev` environment, including English and Bengali localized rules.
- Updated `tools/create-tournament` to derive and validate the tournament `prizePool` from the selected regulation, while retaining `firstPlacePrize` compatibility for the current completion function.
- Added automated coverage for single-place, multi-place, disabled, and inconsistent prize configurations.
- Added per-registration eligibility confirmation for cash-prize tournaments: age, tournament rules,
  and possession of an account with at least one payout method dynamically listed by the regulation.
- Eligibility is validated by `joinTournament` and stored atomically in the participant record; no
  concrete payout method or account details are collected before a participant wins.
- Fixed tournament visibility so `visibleInFlavours: ["global"]` is shown in every app flavour,
  while market-specific values such as `["bangladesh"]` remain restricted to that flavour.
- Added a winner-only payout-details form to tournament results. The available payout methods come
  from the tournament regulation, and the winner can securely submit a validated account or wallet number.
- Restricted payout-detail writes in Firestore to the authenticated first-place winner and made a
  successful submission advance the payment from `awaiting_details` to `ready_for_processing`.
- Added the complete Bangladesh payment-status workflow, an audited administrator CLI, status-aware
  winner UI, and FCM notifications for processing, sending, delivery, correction requests, and cancellation.
- Added method-specific account validation and feedback: bKash/Nagad use an 11-digit Bangladesh
  mobile number, Rocket uses its 12-digit account number, and `+880` input is normalized.
- Added payout-form translations for all supported application languages.

## Follow-up work

- Enforce the regulation constraints in the relevant backend and client participation flows.

This release also introduces the winner-facing payout-details workflow for cash-prize tournaments.

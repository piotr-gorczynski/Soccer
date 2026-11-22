# 17.31 / 17.31-BD — versionCode 66

## Global and Bangladesh

- Add `first_game_completed` for the first completed game, with controlled `won`, `game_type` and `is_anonymous` parameters.
- Add `first_win` for the first actual completed victory, with controlled `game_type`, independent of tournament availability.
- Preserve legacy `first_game_win` semantics: first completed local game, including losses. Do not interpret its event count as victories.
- Reuse persistent installation milestones and the existing Bangladesh first-win prompt state. Prevent duplicate milestones across repeated results, Activity recreation and app reopening. Online results require server confirmation; no private data is added.
- Keep game rules, authentication, legal requirements, joining, payouts and Global UI unchanged.

## Bangladesh

- Keep first-win invitation analytics separate from gameplay milestones and preserve `source=first_win` for the existing tournament funnel. Invitation UX and eligibility are unchanged.

## Measurement configuration and deployment

- PROD GA4 `first_win` key event configured on 22 November 2026; existing `login_success` and `tournament_join_success` retained.
- Imported only the Bangladesh stream's three actions into Google Ads 615-948-1090 as Secondary / observation only at approximately 13:56 UTC. Verified Firebase source and Awaiting conversions; existing Installation and First open actions preserved.
- No campaign, budget or bidding changes. No historical Ads conversion backfill.
- This release requires Android distribution to reach PROD users. No Cloud Function deployment, trigger 580/610, Firestore rule or index change is required for this release relative to 17.30. Production publication remains manual.

## Validation

- Focused Android suites: Bangladesh 51 passed; Global 35 passed, 16 Bangladesh-only tests skipped. Both DEV debug APK builds succeeded again after the version bump to 17.31 / code 66.
- Physical SM-A505FN DEV test: first loss emitted completion with `won=0`, later first victory emitted `first_win`, and repeated victories/restart produced no duplicate milestones in SDK logs.
- GA4 DEV DebugView independently confirmed first completion (`won=0`) at 16:29:36 and first victory (`game_type=2`) at 16:32:25 on 22 November 2026, Europe/Warsaw. Each milestone and the separate invitation shown event appeared once in this fresh test.

# 17.30 / 17.30-BD — versionCode 65

## Global and Bangladesh

- Show the existing loading overlay immediately while sending a player invitation, prevent duplicate taps and premature Back/Up navigation, and handle a missing invitation ID.
- Preserve the existing invitation backend and waiting-screen flow.

## Bangladesh

- Offer the active tournament after a first completed win, once per tournament per installation. Not now suppresses further prompts for that tournament; the Menu card remains available.
- Select one result dialog, avoiding a normal win dialog briefly appearing underneath the tournament invitation. Pending promotion/result checks have a bounded fallback.
- Reuse PendingTournamentPromotion for authentication, Terms, nickname and tournament regulation continuation. Attribute the prompt and eventual join using source=first_win.
- Keep eligibility checkboxes below the unchanged regulation. A persistent, tappable scroll hint gently repeats until confirmations are visible, then displays the 0/3–3/3 counter. Preserve state across recreation; respect accessibility/reduced motion.
- Use uppercase Terms and regulation action buttons. Accept still requires all three confirmations and a successfully loaded, valid regulation.
- Recognize approved HTTPS links in support-authored Help Centre messages. Add optional, administrator-initiated post-payout feedback messages with validated tournament-post/community destinations, EN/BN templates and duplicate-send protection. Prize execution is unchanged.
- Update the local Bangladesh tournament creation template to C. November with registration closing November 25; committing this file does not create or modify a live tournament.

## Deployment

The Android invitation progress, first-win prompt and regulation UX changes need only the app release; no new promotion/join endpoint is required.

The complete release also contains support backend changes. Roll out trigger 580 from the matching branch to each environment (dev/dev, test/test, prod/prod) after the usual branch sync. It deploys existing support functions, including first-message notification handling in onSupportTicketCreated and safe feedback metadata in getSupportConversation. The YAML packages the feedback helper/configuration assets.

No new Cloud Function, Firestore rule/index, payout deployment or trigger 610 is needed. Feedback sends remain explicit CLI operations; deploying does not send messages or transfer money. Older apps can still read plain-text replies. No Cloud deployment is initiated by preparing this release.

## Validation

- 33 Node tests passed for support CLI, feedback gating/deduplication, public projections and notifications.
- Focused Android suites: Bangladesh 67 passed, 1 flavor-specific skip; Global 47 passed, 21 flavor-specific skips. Both DEV APK builds succeeded.
- Physical SM-A505FN DEV checks covered first-win Join/Not now, regulation scroll guidance, confirmation counts and Accept gating; Firebase analytics uploads returned HTTP 204.
- Authentication methods, legal content, tournament eligibility, backend joining and payout execution are unchanged. New tournament UX is Bangladesh-only; Global receives invitation progress feedback.

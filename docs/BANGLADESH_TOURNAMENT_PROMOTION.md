# Bangladesh menu tournament card

Implemented 2 November 2026; Global layout and authentication providers are unchanged.

The BD layout shows a compact clickable card below the greeting and above the game
buttons. It uses a scrollable menu on small screens. All four UI strings have
translations in the existing 20 app languages (independent of Play release-note
locale support). The card says **first prize**, not a guaranteed registration award.
Amount, currency, title, minimum age and registration deadline come from backend data.
Title, first-prize text and join action all use 14sp; title and action stay bold.
For signed-in users, a server read of the selected tournament's participant record
must confirm non-membership before the card is displayed. Existing participants
do not see that card; failed membership reads also hide it until a later refresh.
Guests do not perform this authenticated membership read. Returning to the menu
refreshes the card, including after joining. No backend redeployment is needed for
this client-side membership check.

## Public data boundary

Firestore tournament/regulation reads currently require authentication. The app
does not sign in anonymously just to load this card, and no Firestore rules are
relaxed. A new callable `getBangladeshTournamentPromotion` in codebase
`tournament-promotion` returns only:

`id`, `name`, `regulationId`, `firstPrize`, `currency`,
`registrationDeadlineMillis`, `minimumAge`, plus `serverTimeMillis` in the envelope.

Caller input is ignored. No user/profile, participant, wallet or payout records
are queried or returned. Eligible tournaments explicitly include `bangladesh`,
have status `registering`, a future deadline, spare capacity, an enabled BDT cash
pool and a positive first-place award. The assigned regulation must be active,
for market BD, cash-enabled and age 18+. The earliest eligible registration
deadline wins; ties use tournament ID. Missing/invalid data yields no card.

The endpoint has a 60-second per-instance projection cache, coalesces concurrent
refreshes and caps instances at two. Its response filters expired offers again.
The foreground card refreshes every minute and on menu return, hides on error/no
offer, ignores stopped-screen callbacks and hides at the server-relative deadline.
Capacity/status changes may take up to a refresh interval to appear; clicking
revalidates the actual selected Firestore tournament using `Source.SERVER`.

## Click and onboarding

- One `tournament_promo_click` per accepted click, with only public `tournament_id`.
  Existing Analytics consent/User-ID policies apply; no impression/keystroke events.
- Guests use the existing `UniversalLoginActivity`; no additional provider,
  silent account creation, advertisement or automatic tournament registration.
- The selected ID is retained locally across login, Terms, nickname and process
  recreation for at most 24 hours. It binds to the authenticated UID and is
  discarded on a different account; a cancelled login clears it.
- Navigation waits for the same account's server-confirmed Terms and nonblank
  nickname, resolved menu state and a resumed/focused window.
- Closed/full/hidden/deleted tournaments are not opened. Failed server reads leave
  the selection available for retry and show the existing unavailable message.
- An existing participant opens the lobby. Otherwise the existing regulation and
  eligibility screen opens, reusing `tournament_join_start`; joining still requires
  the existing explicit confirmations and backend validation.

## Deployment

`firebase/firebase.json` registers the new codebase. Only this function is needed;
there are no Auth, Firestore rules, payout or existing tournament-function changes.

Prepared Cloud Build configuration:
`gcp/cloud-build/deploy_tournament_promotion.yaml` and manual trigger definition
`gcp/cloud-build/trigger_tournament_promotion.json` (`600-deploy-tournament-promotion`).
The YAML reads function source from the selected environment's branch. Configure
**both** `gitFileSource.revision` and `sourceToBuild.ref` to the target branch and
set `_ENVIRONMENT` to the same value (`dev`, `test`, `prod`). Restore dev/dev after
any manually requested promotions. Merely adding these files does not create a
Cloud Build trigger or deploy TEST/PROD.

Direct DEV-only development command, run from `firebase` after installing this
codebase's dependencies:

```powershell
firebase deploy --only functions:tournament-promotion --project soccer-dev-1744877837 --account admin@piotr-gorczynski.com --non-interactive
```

Without a deployed endpoint (or without an eligible tournament), the menu keeps
working and the card stays hidden; it never substitutes a hard-coded prize.

## Verification

- Node: `npm test` in `firebase/functions/tournament-promotion`.
- Android focused suites: `TournamentPromotionTest`, `MenuTournamentPromotionTest`,
  `AnalyticsFunnelTest`, `MenuLoadingTest`, `MenuAdNavigationTest`,
  `MenuTermsReadFailureTest` for both DEV markets.
- Phone: a fresh guest should see the real open BD event, click without an ad,
  authenticate, complete Terms/nickname, and reach that tournament's regulation.
  Cancelling login must leave the menu without a deferred surprise navigation.
  Repeat for a complete account, an existing participant, closed/full event and
  network failure. Global must contain no card or promotion requests.

Production registration outcomes and device acceptance of this new card have not
been established merely by adding the code/tests.

Local verification on 2 November 2026: six backend projection tests and 42 focused
Android tests per market passed; both DEV debug APK builds passed. The final repeat
of six navigation tests per market, including the guest double-click guard, and
both APK builds also passed. All 20 translation resources parse and compile.

DEV deployment confirmed on 2 November 2026 after administrator CLI reauthentication:
`getBangladeshTournamentPromotion` was created successfully in `us-central1` in
`soccer-dev-1744877837`. An unauthenticated callable HTTP request returned the
eligible DEV tournament `Rcqd3z41uTTV9n9Ttaxj`, first prize 1000 BDT, minimum age 18,
registration deadline 1794333599000, and server time. No user data was returned.
No IAM changes were made and TEST/PROD were not deployed. The prepared Cloud Build
trigger definition has not been registered. Phone acceptance remains to be tested.

# Firebase Analytics consent integration

## Ownership of the four consent types

All four Firebase consent types default to denied in `AndroidManifest.xml`.
Defaults apply before SDK initialization and do not reset persisted choices.

- `ANALYTICS_STORAGE` and `AD_STORAGE`: `ConsentUtils` preserves the existing
  application policy. With resolved UMP status, it reads TCF purposes 1 and 4,
  respectively; without TCF it retains the existing OBTAINED/NOT_REQUIRED fallback.
  UNKNOWN/REQUIRED cannot reuse stale accepted purposes.
- `AD_USER_DATA` and `AD_PERSONALIZATION`: let UMP 3.2+ interpret consent through
  its supported Firebase integration. The application intentionally omits these
  types from its storage-only `setConsent` call, which only updates specified
  types. Do not infer them from purpose 1/4, `canRequestAds()`, or OBTAINED.

The legacy storage mapping is preserved for compatibility with the verified
reject/accept/withdraw behavior; it is not a claim that purpose 4 equals Google's
ad-storage definition or that OBTAINED proves personalized-ad consent. Google's
ConsentStatus documentation explicitly leaves personalization undefined for
OBTAINED. Any future replacement of the storage mapping needs separate testing.

## Required AdMob configuration (not changed by this code update)

In AdMob, open **Privacy & messaging > European regulations > Settings** and
turn on **Consent mode for advertising purposes**. The analytics-purpose option
is available there too; changing analytics ownership requires verifying it
against the application's existing storage policy. These settings affect all
apps/sites configured for European regulations messages in that account.

The app already uses UMP 3.2.0 and initializes Firebase Analytics in
`SoccerApp.onCreate`, before requesting UMP consent. No SDK upgrade is required.
UMP updates Consent Mode where EU regulations apply; outside that scope the
new advertiser types retain denied defaults unless a supported consent source
updates them. Do not treat NOT_REQUIRED as consent to these two purposes.

The device test on 11 October 2026 showed `EnableAdvertiserConsentMode=0`, absent
UMP advertiser-purpose values, and an empty generated TCF consent bundle.
This indicates the automatic advertiser integration was disabled for that test.
Adding speculative grants in Java would not fix that configuration correctly.
This initial failure was resolved by enabling **Consent mode for advertising
purposes** in AdMob. The subsequent real-device verification on 11 October 2026
passed; see the recorded results below.

## Runtime behavior and diagnostics

`ConsentUtils.updateFirebaseAnalyticsConsent` runs after consent-info refresh,
initial form completion, and successful privacy-options completion. Its log now
explicitly describes storage updates, not effective ad personalization.

The existing PERSONALISED log is only the app's legacy purpose-4 preference.
It does not establish effective `AD_PERSONALIZATION` or guarantee personalized
ad serving. `_npa=1` after acceptance does not by itself identify its source;
inspect Firebase's effective consent and account/configuration restrictions.
Do not force `_npa=0` or set a personalization user property to hide the symptom.
Do not read undocumented UMP_CoMo preference keys as an application API.

## Verification

Automated tests run for Global and Bangladesh:
- merged manifest contains explicit denied defaults for all four consent types;
- existing accepted/rejected, partial, UNKNOWN/REQUIRED and NOT_REQUIRED storage
  behavior is unchanged;
- reject -> accept -> withdraw updates the same two storage types and never
  overrides UMP-owned advertiser types or writes a personalization user property;
- startup completion/error handling remains covered by StartupConsentTest.

These are application regression tests, not a simulation of Google's SDK or
proof of delivery to Analytics. For future regression testing, repeat the
real-device reject/accept/withdraw sequence with verbose Firebase logging.
Inspect effective AD_STORAGE, ANALYTICS_STORAGE, AD_USER_DATA and
AD_PERSONALIZATION after each choice (and after restarting). For an unrestricted
all-consent test, investigate any advertiser type that does not transition with
UMP's interpreted choice. Check `_npa` alongside these signals; an application
PERSONALISED log alone is not a pass. Also test a fresh install's denied defaults.

## Real-device verification — PASS (2026-10-11)

User-reported verification on `piotr_gorczynski.soccer2.bd`, after enabling
**Consent mode for advertising purposes** in AdMob:

| Action | ANALYTICS_STORAGE | AD_STORAGE | AD_USER_DATA | AD_PERSONALIZATION | `_npa` |
| --- | --- | --- | --- | --- | --- |
| Reject all | DENIED | DENIED | DENIED | DENIED | 1 |
| Accept all | GRANTED | GRANTED | GRANTED | GRANTED | 0 |
| Withdraw consent | DENIED | DENIED | DENIED | DENIED | 1 |

- [x] Full Reject -> Accept -> Withdraw lifecycle verified on a real device.
- [x] All four effective consent signals transition correctly.
- [x] `_npa` transitions 1 -> 0 -> 1.
- [x] UMP explicitly updates Firebase for AD_STORAGE, AD_USER_DATA and
  AD_PERSONALIZATION; ConsentUtils updates ANALYTICS_STORAGE / AD_STORAGE.

This closes the previously open advertiser-consent verification for the tested
Bangladesh flow. It does not claim additional fresh-install, restart, region,
or Global-flavor device coverage. Automated consent/startup regression tests
passed 20/20 across `_devGlobalDebug` and `_devBangladeshDebug`.

## Official references

- [UMP Consent Mode integration](https://developers.google.com/admob/android/privacy/consent-mode)
- [AdMob Consent Mode settings](https://support.google.com/admob/answer/16053245)
- [Firebase consent defaults and updates](https://developers.google.com/tag-platform/security/guides/app-consent?platform=android)
- [UMP consent status](https://developers.google.com/admob/android/reference/privacy/com/google/android/ump/ConsentInformation.ConsentStatus)
- [Ad serving modes](https://developers.google.com/admob/android/privacy/ad-serving-modes)

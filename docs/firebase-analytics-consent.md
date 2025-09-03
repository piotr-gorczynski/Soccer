# Firebase Analytics Consent Implementation

## Overview

The app integrates Google's User Messaging Platform (UMP) to collect consent for both AdMob ads and Firebase Analytics in compliance with EEA regulations.

## Implementation

### Consent Flow
1. UMP displays consent form to EEA users
2. User choices are stored as IAB TCF (Transparency & Consent Framework) strings
3. `ConsentUtils` reads IAB consent and maps to Firebase Analytics consent
4. Firebase Analytics consent is updated via `setConsent()` API

### IAB Purpose Mapping
- **Purpose 1** ("Store and/or access information") → `ANALYTICS_STORAGE`
- **Purpose 4** ("Select personalised ads") → `AD_STORAGE`

### Code Components

#### ConsentUtils.java
- `isAnalyticsAllowed()`: Checks Purpose 1 consent
- `isPersonalisedAllowed()`: Checks Purpose 4 consent (existing)
- `updateFirebaseAnalyticsConsent()`: Sets Firebase Analytics consent

#### SoccerApp.java
Calls `ConsentUtils.updateFirebaseAnalyticsConsent()` after:
- Initial consent collection (`loadAndShowConsentForm()`)
- Privacy options updates (`showAdsConsentForm()`)

## Debugging

Check logs for consent updates:
```
TAG_Soccer: Updating Firebase Analytics consent: analytics=true, ads_personalization=false
TAG_Soccer: Firebase Analytics consent updated successfully
```

## Compliance

This implementation ensures:
- ✅ EEA users provide explicit consent before Analytics tracking
- ✅ Consent choices are respected for both ads and analytics
- ✅ Google Analytics for Firebase receives proper consent signals
- ✅ Resolves "Consent missing for EEA users" warning in Google Analytics
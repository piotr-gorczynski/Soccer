# Firebase Analytics Consent Implementation

## Overview

The app integrates Google's User Messaging Platform (UMP) to collect consent for both AdMob ads and Firebase Analytics in compliance with both EEA regulations (GDPR) and US state regulations (CCPA, CPRA, etc.).

## Implementation

### Consent Flow
1. UMP displays consent form to users in regulated regions (EEA and US states)
2. For EEA users: User choices are stored as IAB TCF (Transparency & Consent Framework) strings
3. For US users: Consent status is managed by UMP's consent status API
4. `ConsentUtils` reads consent information and maps to Firebase Analytics consent
5. Firebase Analytics consent is updated via `setConsent()` API

### Regulation-Specific Handling

#### EEA Regulations (GDPR)
- Uses IAB TCF (Transparency & Consent Framework) for granular consent
- **Purpose 1** ("Store and/or access information") → `ANALYTICS_STORAGE`
- **Purpose 4** ("Select personalised ads") → `AD_STORAGE`

#### US State Regulations (CCPA, CPRA, etc.)
- Uses UMP consent status API for overall consent determination
- Fallback mechanism when IAB TCF data is not available
- Supports "opt-out" model typical of US privacy laws

### Code Components

#### ConsentUtils.java
- `isAnalyticsAllowed()`: Checks Purpose 1 consent (EEA) or UMP status (US)
- `isPersonalisedAllowed()`: Checks Purpose 4 consent (EEA) or UMP status (US)
- `hasUmpConsent()`: Helper method to check overall UMP consent status
- `updateFirebaseAnalyticsConsent()`: Sets Firebase Analytics consent
- `setDefaultFirebaseAnalyticsConsent()`: Sets default consent to DENIED

The consent reading methods use a fallback approach:
1. First check IAB TCF strings (for EEA regulations)
2. If IAB data is not available, fall back to UMP consent status (for US regulations)

#### SoccerApp.java
Calls `ConsentUtils.updateFirebaseAnalyticsConsent()` after:
- Initial consent collection (`loadAndShowConsentForm()`)
- Privacy options updates (`showAdsConsentForm()`)

## Debugging

Check logs for consent updates:
```
TAG_Soccer: Checking personalized ads consent - IAB TCF string = "1010"
TAG_Soccer: Using IAB TCF consent for personalized ads: false
TAG_Soccer: Updating Firebase Analytics consent: analytics=true, ads_personalization=false
TAG_Soccer: Firebase Analytics consent updated successfully
```

For US regulations (when IAB TCF is not available):
```
TAG_Soccer: Checking personalized ads consent - IAB TCF string = ""
TAG_Soccer: UMP consent status: OBTAINED
TAG_Soccer: Using UMP consent status for personalized ads (US regulations): true
TAG_Soccer: Updating Firebase Analytics consent: analytics=true, ads_personalization=true
TAG_Soccer: Firebase Analytics consent updated successfully
```

## Compliance

This implementation ensures:
- ✅ EEA users provide explicit consent before Analytics tracking (via IAB TCF)
- ✅ US users comply with state regulations like CCPA/CPRA (via UMP consent status)
- ✅ Consent choices are respected for both ads and analytics
- ✅ Google Analytics for Firebase receives proper consent signals
- ✅ Automatic fallback between IAB TCF and UMP consent mechanisms
- ✅ Resolves "Consent missing for EEA users" warning in Google Analytics
- ✅ Supports both "opt-in" (EEA) and "opt-out" (US) consent models
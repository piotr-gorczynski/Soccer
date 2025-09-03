# ConsentUtils Enhanced Framework - Testing Guide

This document demonstrates how the enhanced ConsentUtils framework handles different consent scenarios for both EEA and US state regulations.

## Test Scenario 1: EEA User with IAB TCF Consent

**Setup**: User is in EEA region, UMP populates IAB TCF strings

```
SharedPreferences contains:
IABTCF_PurposeConsents = "1101"
```

**Expected Behavior**:
- `isAnalyticsAllowed()` returns `true` (Purpose 1 = '1' at index 0)
- `isPersonalisedAllowed()` returns `false` (Purpose 4 = '0' at index 3)
- Uses IAB TCF strings, does not fall back to UMP consent status

**Expected Logs**:
```
TAG_Soccer: Checking analytics consent - IAB TCF string = "1101"
TAG_Soccer: Using IAB TCF consent for analytics: true
TAG_Soccer: Checking personalized ads consent - IAB TCF string = "1101"
TAG_Soccer: Using IAB TCF consent for personalized ads: false
TAG_Soccer: Updating Firebase Analytics consent: analytics=true, ads_personalization=false
```

## Test Scenario 2: US User with UMP Consent Status

**Setup**: User is in US state with regulations, no IAB TCF strings available

```
SharedPreferences contains:
IABTCF_PurposeConsents = "" (empty)

UMP ConsentInformation:
getConsentStatus() = OBTAINED
```

**Expected Behavior**:
- `isAnalyticsAllowed()` returns `true` (falls back to UMP consent status)
- `isPersonalisedAllowed()` returns `true` (falls back to UMP consent status)
- Uses UMP consent status API when IAB data is not available

**Expected Logs**:
```
TAG_Soccer: Checking analytics consent - IAB TCF string = ""
TAG_Soccer: UMP consent status: OBTAINED
TAG_Soccer: Using UMP consent status for analytics (US regulations): true
TAG_Soccer: Checking personalized ads consent - IAB TCF string = ""
TAG_Soccer: UMP consent status: OBTAINED
TAG_Soccer: Using UMP consent status for personalized ads (US regulations): true
TAG_Soccer: Updating Firebase Analytics consent: analytics=true, ads_personalization=true
```

## Test Scenario 3: User Not in Regulated Region

**Setup**: User outside EEA and US regulated states

```
SharedPreferences contains:
IABTCF_PurposeConsents = "" (empty)

UMP ConsentInformation:
getConsentStatus() = NOT_REQUIRED
```

**Expected Behavior**:
- `isAnalyticsAllowed()` returns `true` (consent not required)
- `isPersonalisedAllowed()` returns `true` (consent not required)
- Framework treats NOT_REQUIRED as consent granted

## Test Scenario 4: User Denies Consent

**Setup**: User in regulated region denies consent

```
SharedPreferences contains:
IABTCF_PurposeConsents = "" (empty or all zeros)

UMP ConsentInformation:
getConsentStatus() = REQUIRED (consent required but not obtained)
```

**Expected Behavior**:
- `isAnalyticsAllowed()` returns `false` (consent required but not granted)
- `isPersonalisedAllowed()` returns `false` (consent required but not granted)
- Framework respects user's decision to deny consent

## Benefits of Enhanced Framework

1. **Backward Compatibility**: Existing EEA implementation using IAB TCF continues to work
2. **US Regulations Support**: Automatic fallback to UMP consent status for US state laws
3. **Comprehensive Logging**: Clear visibility into which consent mechanism is being used
4. **Flexibility**: Handles both "opt-in" (EEA) and "opt-out" (US) consent models
5. **Future-Proof**: Ready for new privacy regulations that UMP may support

## Manual Testing Steps

To test the enhanced framework:

1. **EEA Testing**: Use VPN to simulate EEA location, verify IAB TCF strings are used
2. **US Testing**: Use VPN to simulate California/Virginia location, verify UMP fallback works
3. **Log Monitoring**: Watch logcat for consent mechanism being used
4. **Firebase Analytics**: Verify consent signals are properly sent to Firebase Analytics
5. **Privacy Options**: Test that privacy options dialog updates consent correctly
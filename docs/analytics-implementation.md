# Analytics and User Research Implementation

This document describes the comprehensive analytics and user research system implemented to understand why users aren't registering for tournaments.

## Overview

The system tracks user behavior through the authentication and tournament funnels, provides A/B testing capabilities, and collects user feedback to identify barriers to registration.

## Key Features

### 1. Analytics Events (GA4 Compatible)

#### Authentication Funnel
- `login` - When login screen is opened
- `sign_up` - Successful signup with method parameter
- `sign_up_error` - Signup failures with error details
- `signup_decline_reason` - User feedback on why they don't want to register

#### Tournament Funnel  
- `tournament_view` - When tournament list is viewed
- `tournament_join_start` - When user starts joining a tournament
- `tournament_join_success` - Successful tournament join
- `tournament_join_error` - Tournament join failures

#### Anonymous User Flow
- `anonymous_link_prompt` - When anonymous users are prompted to link accounts
- `anonymous_link_decision` - User response to linking prompts

### 2. User Feedback Collection

#### Signup Decline Reason Dialog
When users dismiss signup or try to join tournaments without accounts, they see a one-tap dialog with options:
- "Just playing offline"
- "Privacy concerns" 
- "Too complicated"
- "Don't use Google/Email"
- "Other reason"

### 3. A/B Testing with Remote Config

#### Signup Prompt Timing Variants
- `on_open` - Show immediately when app opens
- `after_first_online_click` - After user clicks online features
- `after_1_match` - After user completes first match
- `only_when_joining_tournament` - Only when trying to join tournaments

#### Registration Copy Variants
- `save_nickname_ranking` - "Save your progress & nickname"
- `play_tournaments_global` - "Join tournaments & compete globally"
- `secure_account_benefits` - "Secure your account and never lose progress"

### 4. Anonymous User Linking

Progressive prompts to convert anonymous users:
- After setting nickname
- After winning matches
- When trying to join tournaments

### 5. In-App Messaging

Targeted messages for v7/v8 users who haven't registered:
- Frequency-limited (max 3 messages, 24h between)
- Session-based triggering (after 2+ app sessions)
- Dismissible with tracking

## Implementation Details

### Core Classes

#### `AnalyticsManager`
Central class for all analytics tracking
- Firebase Analytics integration
- Crashlytics breadcrumbs
- User properties for segmentation

#### `RemoteConfigHelper`
A/B testing and feature flags
- Signup prompt timing control
- Registration copy variants
- Feature toggles

#### `SignupDeclineReasonDialog`
User feedback collection
- One-tap reason selection
- Analytics event logging
- Customizable triggers

#### `AnonymousLinkPromptHelper`
Anonymous user conversion
- Context-aware prompting
- Progress-based triggers
- User choice tracking

#### `InAppMessagingHelper`
Targeted user messaging
- Version-based targeting
- Frequency management
- Engagement tracking

### Integration Points

#### UniversalLoginActivity
- Tracks login screen opens
- Records signup success/failure
- Shows decline reason dialogs on cancel

#### TournamentsActivity  
- Tracks tournament list views
- Monitors join attempts
- Shows authentication prompts for unauthenticated users

#### RegulationActivity
- Tracks tournament join success/failure
- Records error details
- Manages tournament funnel completion

#### PickNicknameActivity
- Updates user properties
- Shows anonymous linking prompts
- Tracks nickname completion

#### MenuActivity
- Triggers in-app messages
- Session counting
- User engagement tracking

## Data Collection Strategy

### Event Parameters
All events include relevant context:
- User authentication status
- Tournament IDs for funnel tracking
- Error codes and messages
- Trigger sources
- A/B test variants

### User Properties
For segmentation and analysis:
- `auth_method` - How user signed up
- `app_version` - Current app version
- `language` - User's language preference
- `has_nickname` - Whether user completed profile

### Breadcrumbs
Detailed debugging trails for:
- Authentication flows
- Tournament operations
- Error conditions
- User decisions

## Analysis Recommendations

### Key Metrics to Monitor

1. **Auth Funnel Conversion**
   - Login screen views → Signup attempts → Successful signups
   - Drop-off points identification
   - Method-specific conversion rates

2. **Tournament Funnel Health**  
   - Tournament views → Join attempts → Successful joins
   - Authentication barriers
   - Error rate analysis

3. **Decline Reason Distribution**
   - Most common reasons for not registering
   - Trigger-specific patterns
   - Geographic/language variations

4. **Anonymous User Behavior**
   - Linking prompt effectiveness
   - Trigger optimization
   - Conversion timing

### A/B Test Analysis
- Prompt timing impact on conversion
- Copy variant effectiveness
- Feature toggle performance

### Segmentation Opportunities
- Version 7.0 vs 8.0 users
- Language/region patterns
- Anonymous vs registered behavior
- Session depth correlation

## Usage Examples

### Basic Analytics Tracking
```java
// Get analytics manager
AnalyticsManager analytics = ((SoccerApp) getApplicationContext()).getAnalyticsManager();

// Track events
analytics.trackLoginScreenOpened();
analytics.trackSignupSuccess("google");
analytics.trackTournamentJoinStart("tournament_123", true);
```

### Show Decline Reason Dialog
```java
SignupDeclineReasonDialog.show(this, "tournament_join", analyticsManager, reason -> {
    Log.d("TAG", "User declined: " + reason);
    // Handle user feedback
});
```

### Remote Config A/B Testing
```java
RemoteConfigHelper config = ((SoccerApp) getApplicationContext()).getRemoteConfigHelper();
String variant = config.getSignupPromptVariant();
String message = config.getRegistrationMessage(this);
```

### Anonymous User Prompts
```java
if (AnonymousLinkPromptHelper.shouldShowPrompt("pick_nickname")) {
    AnonymousLinkPromptHelper.showSaveProgressPrompt(this, "pick_nickname", analyticsManager);
}
```

## Next Steps

1. **Firebase Console Setup**
   - Configure custom events in Analytics
   - Set up conversion goals
   - Create audience segments

2. **Remote Config Parameters**
   - Deploy A/B test configurations
   - Set up percentage rollouts
   - Configure targeting rules

3. **Dashboard Creation**
   - Build funnel analysis reports
   - Set up real-time monitoring
   - Create automated alerts

4. **Feedback Analysis**
   - Weekly decline reason reviews
   - Feature request identification
   - UX improvement planning

This system provides comprehensive visibility into user behavior and barriers to registration, enabling data-driven decisions to improve conversion rates.
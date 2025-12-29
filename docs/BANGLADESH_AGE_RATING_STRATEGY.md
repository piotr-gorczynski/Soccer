# Bangladesh Version: Age Rating Strategy Analysis

**Document Version:** 1.0  
**Last Updated:** 2025-12-29  
**Status:** Analysis & Recommendation

## Executive Summary

**Question**: How to handle age ratings when the global app is 13+ but the Bangladesh version with cash prizes must be 18+?

**Answer**: YES, you will have two separate Google Play Store listings with different age ratings coexisting in Bangladesh:
- **Global version** (`piotr_gorczynski.soccer2`): 13+ rating - regular gameplay without cash prizes
- **Bangladesh version** (`piotr_gorczynski.soccer2.bd`): 18+ rating - includes cash prize tournaments

**This is the correct and compliant approach.** Both apps can coexist in the Bangladesh Google Play Store, allowing users to choose based on their age and interest in cash prize tournaments.

---

## Table of Contents

1. [Problem Statement](#problem-statement)
2. [Google Play Store Age Rating System](#google-play-store-age-rating-system)
3. [Recommended Approach](#recommended-approach)
4. [Age Rating Assignment](#age-rating-assignment)
5. [Coexistence Strategy](#coexistence-strategy)
6. [User Experience Implications](#user-experience-implications)
7. [Google Play Store Configuration](#google-play-store-configuration)
8. [Migration Strategy Impact](#migration-strategy-impact)
9. [Legal & Compliance Considerations](#legal--compliance-considerations)
10. [Frequently Asked Questions](#frequently-asked-questions)

---

## Problem Statement

The Soccer (Gridline Soccer) application currently has:
- **Global version**: Rated 13+ (suitable for teenagers)
- **No gambling or cash prizes**: Pure skill-based game

The planned Bangladesh version will introduce:
- **Cash prize tournaments**: ৳2,000 BDT for 1st place winners
- **Legal requirement**: Participants must be 18+ years old
- **Age-restricted content**: Real money prizes

**Key Questions**:
1. Can both versions (13+ and 18+) coexist in Bangladesh Google Play Store?
2. How should age ratings be configured in Google Play Console?
3. Will users under 18 be blocked from the Bangladesh version?
4. What happens to existing 13-17 year old users who migrate?
5. Is this approach compliant with Google Play policies?

---

## Google Play Store Age Rating System

### How Age Ratings Work

Google Play uses two systems for age classification:

#### 1. Content Rating (IARC - International Age Rating Coalition)
- **Questionnaire-based**: Developer fills out content questionnaire
- **Automatic classification**: IARC assigns ratings based on answers
- **Multiple regions**: Generates ratings for different regions (ESRB, PEGI, etc.)
- **Content-driven**: Based on violence, language, sexual content, etc.

#### 2. Target Age (Google Play Family Policy)
- **Developer-selected**: Choose target age groups
- **Determines visibility**: Affects which users see the app
- **Separate from content rating**: Independent classification

### Age Rating Categories

Common IARC ratings:
- **Everyone**: Suitable for all ages
- **Everyone 10+**: Suitable for ages 10 and up
- **Teen**: Suitable for ages 13 and up
- **Mature 17+**: Suitable for ages 17 and up
- **Adults Only 18+**: Suitable for ages 18 and up

### Real Money Gaming Classification

Google Play has specific policies for real money gaming:
- **Gambling apps**: Must be rated 18+ (Adults Only)
- **Cash prize apps**: May require 18+ depending on jurisdiction
- **Skill-based competitions**: Typically rated based on other content
- **Promotional prizes**: Developer-funded prizes may require 18+ in some regions

**Important**: Bangladesh cash prize tournaments are **skill-based with free entry**, NOT gambling. However, they still involve real money prizes which typically requires 18+ rating.

---

## Recommended Approach

### Strategy: Two Separate Apps with Different Age Ratings

**Recommended Configuration**:

| App Variant | Package Name | Age Rating | Target Audience | Available In |
|-------------|--------------|------------|-----------------|--------------|
| **Global** | `piotr_gorczynski.soccer2` | **13+ (Teen)** | General players, no cash prizes | Worldwide including Bangladesh |
| **Bangladesh** | `piotr_gorczynski.soccer2.bd` | **18+ (Adults Only)** | Bangladesh adults, cash prizes | Bangladesh only |

### Why This Works

1. **Different package names**: Completely separate app listings on Google Play Store
2. **Different content**: Bangladesh version has age-restricted content (cash prizes)
3. **Google Play allows coexistence**: Multiple apps from same developer can target same market
4. **User choice**: Users can choose appropriate version based on age and preferences
5. **Compliance**: Meets both Google Play policies and Bangladesh legal requirements

---

## Age Rating Assignment

### Global Version (`piotr_gorczynski.soccer2`)

**Current Rating**: 13+ (Teen)

**IARC Questionnaire Answers**:
- Violence: None (paper soccer, abstract gameplay)
- Sexual content: None
- Language: None (clean game)
- Controlled substances: None
- Gambling/betting: **No** (no cash prizes, no betting)
- User interaction: Yes (online multiplayer)
- Shares location: No
- Purchases digital goods: No
- Unrestricted internet: No

**Result**: Teen (13+) rating is appropriate

**No changes needed** for global version.

---

### Bangladesh Version (`piotr_gorczynski.soccer2.bd`)

**Required Rating**: 18+ (Adults Only)

**IARC Questionnaire Answers**:
- Violence: None
- Sexual content: None
- Language: None
- Controlled substances: None
- **Gambling/betting**: Answer carefully:
  - "Does your app enable users to purchase or earn credits that can be used to gamble?" → **No** (free entry)
  - "Does your app involve real-world prizes or tournaments?" → **Yes** (cash prizes)
  - "Are users 18 years or older required?" → **Yes**
- User interaction: Yes (online multiplayer)
- Shares location: No
- **Purchases digital goods**: No (tournaments are free)
- Unrestricted internet: No

**Additional Questions for Real Money Content**:
- "Does your app offer promotional contests or sweepstakes?" → **Yes**
- "Is entry free?" → **Yes**
- "Are prizes real money/goods?" → **Yes** (cash prizes)
- "Is it skill-based?" → **Yes**
- "Minimum age requirement?" → **18 years**

**Result**: Based on real money prizes and age requirement, IARC will likely assign **Mature 17+** or **Adults Only 18+** rating.

**If IARC assigns 17+**: Manually select **18+** in Google Play Console to ensure compliance with Bangladesh legal requirement.

---

## Coexistence Strategy

### How Both Apps Work Together in Bangladesh

#### Scenario: Bangladesh User on Google Play Store

**User Age: 13-17 years old** (verified via Google account)
```
Google Play Store (Bangladesh region)
├── Search: "Gridline Soccer" or "Soccer"
│   ├── Result 1: Gridline Soccer (13+) ✅ VISIBLE
│   │   - Can install and play
│   │   - Regular tournaments only
│   │   - No cash prizes
│   │
│   └── Result 2: Gridline Soccer Bangladesh (18+) ❌ RESTRICTED
│       - Not visible in search results OR
│       - Visible but with age restriction message
│       - Cannot install (Google Play blocks installation)
```

**User Age: 18+ years old** (verified via Google account)
```
Google Play Store (Bangladesh region)
├── Search: "Gridline Soccer" or "Soccer"
│   ├── Result 1: Gridline Soccer (13+) ✅ VISIBLE
│   │   - Can install and play
│   │   - Regular tournaments only
│   │
│   └── Result 2: Gridline Soccer Bangladesh (18+) ✅ VISIBLE
│       - Can install and play
│       - Cash prize tournaments available
│       - Requires eligibility confirmation in-app
```

### Google Play Store Behavior

**Age Restriction Enforcement**:
1. **Google account age**: Google Play knows user's age from their account
2. **Automatic filtering**: 18+ apps don't appear in search for users under 18 (in most cases)
3. **Installation blocking**: If user tries to install via direct link, Google Play shows:
   ```
   "This app is rated 18+ and is not appropriate for your age group."
   [Cannot Install]
   ```
4. **Family Link users**: Parents can control access to age-restricted apps

**Visibility**:
- Both apps appear in "More by this developer" section (with age restrictions applied)
- Both apps can appear in search results (with age restrictions applied)
- Both apps have separate listings, descriptions, screenshots
- Both apps can have different keywords and metadata

---

## User Experience Implications

### For Users Under 18

**Experience**:
1. User searches for "Gridline Soccer" on Google Play
2. Sees **Gridline Soccer (13+)** in results
3. **Does NOT see** or **cannot install** Gridline Soccer Bangladesh (18+)
4. Installs global version, plays regular tournaments
5. No cash prizes, but full gameplay experience

**What they cannot access**:
- Cash prize tournaments
- Bangladesh-specific promotional events with prizes

**What they CAN access**:
- All regular gameplay features
- Online multiplayer
- Standard tournaments without prizes
- Friends and social features

---

### For Users 18+

**Experience**:
1. User searches for "Gridline Soccer" on Google Play
2. Sees **both versions** in results:
   - Gridline Soccer (13+)
   - Gridline Soccer Bangladesh (18+) 🇧🇩
3. Can choose to install:
   - **Global version**: For regular play, no cash prizes
   - **Bangladesh version**: For cash prize tournaments
   - **Both**: Can have both installed simultaneously

**Choice guidance**:
- If interested in cash prizes → Install Bangladesh version
- If under 18 or not interested in prizes → Install global version
- Both apps share same user account (Firebase) so progress syncs

---

### For Existing Users (13-17) Who Installed Global Version

**Scenario**: Teen user (age 15) has global version installed, sees promotion for Bangladesh version

**What happens**:
1. User sees in-app banner: "Install Bangladesh version for cash prizes!"
2. User clicks banner → Redirected to Google Play
3. **Google Play blocks installation**:
   ```
   "Gridline Soccer Bangladesh is rated 18+ and is not appropriate for your age."
   [Cannot Install]
   ```
4. User continues using global version with regular tournaments

**Important**: This is **automatic** and handled by Google Play. You don't need to implement age checks in the promotion logic.

---

## Google Play Store Configuration

### Step-by-Step: Setting Age Ratings

#### For Global Version (Already Done)
1. Google Play Console → Gridline Soccer (`piotr_gorczynski.soccer2`)
2. **Content Rating** → Complete IARC questionnaire
3. Answer "No" to gambling/betting questions
4. Receive **Teen (13+)** rating
5. Save and publish

#### For Bangladesh Version (New Setup)
1. Google Play Console → Create new app → "Gridline Soccer Bangladesh"
2. Package name: `piotr_gorczynski.soccer2.bd`
3. **Content Rating** → Complete IARC questionnaire
4. **Critical answers**:
   - "Does your app offer contests/sweepstakes?" → **Yes**
   - "Is entry free?" → **Yes**
   - "Real money prizes?" → **Yes**
   - "Skill-based?" → **Yes**
   - "Minimum age requirement?" → **18+**
5. IARC assigns rating (likely **Mature 17+** or **Adults Only 18+**)
6. If IARC assigns **17+**, manually override:
   - Go to **Store Presence** → **Store Settings**
   - Under "Target audience and content" → Select **18+ only**
7. Save and publish

### Additional Settings for Bangladesh Version

**Store Listing**:
- **Title**: "Gridline Soccer Bangladesh" or "Gridline Soccer - Cash Prizes"
- **Short description**: Mention "18+ only" and cash prizes
- **Full description**: Clear age restriction disclosure
- **Screenshots**: Add "18+" badge or indicator
- **App icon**: Consider adding "BD" or "18+" badge

**Distribution**:
- **Countries**: Select **Bangladesh only**
- **Age restriction**: Confirm **18+** is set
- **Category**: Sports or Strategy (not Gambling)

**Content Rating Details**:
- Add note in "Additional Information":
  ```
  "This app offers skill-based tournaments with real cash prizes.
   Entry is free and prizes are developer-funded.
   This is not gambling. Participants must be 18+ as required by Bangladesh law."
  ```

---

## Migration Strategy Impact

### Effect on Original Migration Plan

The age rating difference **DOES affect** the migration strategy outlined in `BANGLADESH_VERSION_APPROACH.md`:

#### Original Migration Plan
- Show in-app banner to all Bangladesh users in global app
- Direct them to install Bangladesh version
- Target: 30% migration in Month 1 (224 users out of 746)

#### Adjusted Plan (Accounting for Age Restrictions)

**Step 1: Determine Age Demographics**

From 746 current Bangladesh users:
- Estimate **% of users 18+**: Unknown (need analytics)
- If 70% are 18+: ~522 eligible users
- If 50% are 18+: ~373 eligible users
- If 30% are 18+: ~224 eligible users

**Action**: Check Google Play Console → User Analytics → Age distribution
- If available, use actual age data
- If not available, estimate conservatively (assume 50%)

**Step 2: Adjust Migration Targets**

**Revised Migration Targets** (assuming 50% are 18+):
- **Eligible users**: ~373 (50% of 746)
- **Month 1 target**: 30% of eligible = **112 users**
- **Month 6 target**: 60% of eligible = **224 users**

**Step 3: Age-Aware Promotion Strategy**

**In-App Banner Logic**:
```kotlin
// In global app (piotr_gorczynski.soccer2)
fun shouldShowBDPromotion(): Boolean {
    // Show to Bangladesh users only
    if (userRegion != "BD") return false
    
    // Show to all users - Google Play will enforce age restriction
    // We don't need to check age in-app
    return !hasUserDismissedPromo()
}
```

**Banner Message** (age-neutral):
```
🎉 NEW: Gridline Soccer Bangladesh!

Win ৳2,000 cash prizes in skill-based tournaments!

✅ Free entry, no payment required
✅ Same account, all your data preserved
✅ For players 18 and above

[Install Now]  [Learn More]  [Maybe Later]

Note: You must be 18+ to participate in cash prize tournaments.
Google Play will verify your age.
```

**Key Points**:
- **Don't hide banner from young users**: They need to know about the requirement
- **Let Google Play enforce**: Google will block installation if user is under 18
- **Clear messaging**: Banner mentions "18 and above" requirement
- **No false hopes**: User understands why they might not be able to install

#### What Happens to Users Under 18?

**Scenario**: 13-17 year old user sees banner and clicks "Install Now"

1. User redirected to Google Play Store
2. Google Play shows Gridline Soccer Bangladesh listing
3. **Google Play displays age restriction message**
4. User cannot install the app
5. User returns to global app and continues playing

**User experience**:
- Clear why they can't install (age restriction)
- Can still enjoy global version
- Will be able to install when they turn 18 (if still interested)

**No negative impact**: Users understand age restrictions (common in gaming)

---

## Legal & Compliance Considerations

### Bangladesh Gaming Law Compliance

**Age Requirement**: Bangladesh skill-based gaming laws require participants to be 18+

**Compliance Approach**:
1. **Google Play age rating**: 18+ (primary enforcement)
2. **In-app confirmation**: User self-declaration checkbox
3. **Terms of Service**: Clear 18+ requirement
4. **Payment verification**: Age verification at payout (optional)

**Multi-Layer Verification**:
```
Layer 1: Google Play Store age restriction (automatic)
    ↓
Layer 2: In-app eligibility confirmation (user declares 18+)
    ↓
Layer 3: Payment account verification (bKash/Nagad requires 18+)
    ↓
Layer 4: Developer can verify at payout if needed
```

This approach provides **robust age verification** while minimizing user friction.

---

### Google Play Policy Compliance

**Real Money Gaming Policy**: Google Play allows real money gaming apps if:
- ✅ Properly age-rated (18+ for cash prizes)
- ✅ Comply with local laws
- ✅ Clear disclosure of terms
- ✅ No deceptive practices

**Your App Complies**:
- ✅ Age-rated 18+
- ✅ Skill-based (not gambling)
- ✅ Free entry (no purchase required)
- ✅ Clear terms and disclosure
- ✅ Developer-funded prizes

**Potential Policy Concerns**: None identified. Your approach is compliant.

---

## Coexistence Best Practices

### How to Manage Two Versions

#### In Global Version (`piotr_gorczynski.soccer2`)

**Add Age-Aware Messaging**:
```kotlin
// When showing Bangladesh promotion
fun getBDPromotionMessage(): String {
    return """
    🎉 NEW: Gridline Soccer Bangladesh!
    
    Win ৳2,000 cash prizes in skill-based tournaments!
    
    ✅ Free entry, no payment required
    ✅ Same account, all your data preserved
    ✅ Bi-monthly cash prize tournaments
    
    ⚠️ REQUIREMENT: You must be 18 years or older to participate
    in cash prize tournaments as required by Bangladesh law.
    
    [Install Bangladesh Version]  [Learn More]  [Maybe Later]
    """.trimIndent()
}
```

**Log Analytics**:
```kotlin
// Track banner clicks (regardless of age)
analytics.logEvent("bd_promotion_clicked", mapOf(
    "user_id" to userId,
    "user_region" to "BD",
    "timestamp" to System.currentTimeMillis()
))

// Google Play will handle age restriction
// No need to track install success here
```

#### In Bangladesh Version (`piotr_gorczynski.soccer2.bd`)

**First Launch Check**:
```kotlin
// On first launch of Bangladesh version
fun onFirstLaunch() {
    // User already passed Google Play age check (18+)
    // Still require in-app confirmation for legal clarity
    
    showEligibilityConfirmationDialog()
}

fun showEligibilityConfirmationDialog() {
    // User must confirm:
    // - They are 18+ years old
    // - They have payment account (bKash/Nagad/Rocket)
    // - They agree to tournament terms
}
```

**Eligibility Confirmation Screen**:
```
🇧🇩 Bangladesh Tournament Eligibility

To participate in cash prize tournaments, you must confirm:

☑ I am 18 years of age or older
   (You've already been verified by Google Play)

☑ I have a valid bKash, Nagad, or Rocket account
   (Required to receive prize payments)

☑ I agree to the tournament terms and conditions
   (View terms)

[Confirm and Continue]

Note: False declarations may result in disqualification
and prize forfeiture.
```

---

## Frequently Asked Questions

### Q1: Will Google Play automatically block users under 18 from installing Bangladesh version?

**A**: Yes. Google Play enforces age restrictions based on the user's Google account age. If the app is rated 18+ and the user is under 18, they will either:
- Not see the app in search results, OR
- See the app but cannot install it (blocked with age restriction message)

No additional implementation needed on your part.

---

### Q2: Can users lie about their age to Google?

**A**: While users can theoretically create Google accounts with false ages, this is:
- Against Google's Terms of Service
- Difficult (requires fake documents for age verification in some cases)
- Not your responsibility to prevent
- Mitigated by in-app eligibility confirmation and payment account verification (bKash/Nagad require age verification)

Your multi-layer approach provides adequate protection.

---

### Q3: What if a 13-17 year old user manages to install Bangladesh version?

**A**: Unlikely due to Google Play enforcement, but if it happens:
1. User must still confirm eligibility in-app (will see 18+ requirement)
2. If they falsely confirm, they won't have valid payment account (bKash/Nagad require 18+)
3. If they somehow win, you can verify age during payout and disqualify if underage
4. Terms of Service clearly state false declarations result in disqualification

**Risk**: Very low. Multiple verification layers prevent this.

---

### Q4: Should I remove the global version from Bangladesh to avoid confusion?

**A**: **No, absolutely not.** Keep both versions available because:
- ✅ Users under 18 need access to the game
- ✅ Users 18+ may prefer non-prize version
- ✅ Gradual migration allows users to choose
- ✅ Fallback option if Bangladesh version has issues
- ✅ No policy requiring removal of one version

**Both versions should coexist indefinitely.**

---

### Q5: How do I market both versions without confusing users?

**A**: Clear differentiation in messaging:

**Global Version Marketing**:
- "Gridline Soccer - Classic Paper Soccer Game"
- "For all ages, play with friends worldwide"
- "No registration required for basic play"

**Bangladesh Version Marketing**:
- "Gridline Soccer Bangladesh - Win Cash Prizes!"
- "18+ only, compete for ৳2,000 prizes"
- "Skill-based tournaments for Bangladesh players"

**In-app cross-promotion**: Only show Bangladesh promotion to users in Bangladesh region. Make age requirement clear in every promotion.

---

### Q6: Will this affect my app's visibility or ranking?

**A**: Minimal impact:
- Each version has separate ranking and visibility
- 18+ restriction reduces potential audience for Bangladesh version
- Global version visibility unchanged
- Both can rank for different keywords

**Optimize separately**:
- Global version: "paper soccer", "multiplayer game", "strategy game"
- Bangladesh version: "cash prizes", "tournament", "skill competition", "Bangladesh"

---

### Q7: What if Google Play changes its age rating policies?

**A**: Monitor policy changes and adapt:
- Subscribe to Google Play developer policy updates
- Review quarterly for any changes
- Be prepared to update age ratings if required
- Have legal counsel review annually

**Likelihood of impact**: Low. Age restrictions for real money gaming are well-established and unlikely to become more lenient.

---

### Q8: Can I migrate users automatically when they turn 18?

**A**: No automatic migration possible because:
- You don't have access to user's exact birthdate from Google account
- Google Play doesn't provide age information to apps
- Age verification is handled by Google Play at install time

**Alternative**: Show promotion to all Bangladesh users. Google Play will allow installation when they turn 18.

---

### Q9: How does this affect Firebase Analytics and user tracking?

**A**: Both apps share same Firebase project:
- Can track users across both apps (same user UID)
- Can see which app variant they're using
- Can measure migration from global to Bangladesh version
- Can segment analytics by app version

**Recommended analytics events**:
```kotlin
// In global app
analytics.logEvent("bd_promotion_shown", mapOf("user_region" to "BD"))

// In Bangladesh app
analytics.logEvent("bd_app_launched", mapOf("user_age_verified" to true))
```

---

### Q10: What about users who have both apps installed?

**A**: Users can have both apps installed simultaneously:
- **Use case 1**: User likes having separate apps for different purposes
- **Use case 2**: User wants global version for casual play, Bangladesh version for tournaments
- **Use case 3**: User testing both versions

**Impact**: None. Both apps use same Firebase backend, same user account, data stays synced.

**Storage**: Each app ~50-100 MB, total ~100-200 MB. Not a significant concern.

---

## Conclusion & Recommendations

### Summary

**Your intuition is correct**: You will have two separate versions of the app coexisting in Bangladesh:
1. **Global version** (13+): Available to all ages, no cash prizes
2. **Bangladesh version** (18+): Adults only, with cash prize tournaments

**This is the proper and compliant approach.**

---

### Final Recommendations

#### 1. Age Rating Configuration
- ✅ Keep global version at **13+ (Teen)** rating
- ✅ Set Bangladesh version to **18+ (Adults Only)** rating
- ✅ Complete IARC questionnaire accurately for both versions
- ✅ Clearly disclose cash prizes in Bangladesh version content rating

#### 2. Migration Strategy
- ✅ Show promotion to all Bangladesh users (age-neutral)
- ✅ Mention 18+ requirement in promotion messaging
- ✅ Let Google Play enforce age restriction automatically
- ✅ Adjust migration targets based on actual age demographics (~50% eligible)
- ✅ Don't try to filter promotions by age (Google handles this)

#### 3. User Communication
- ✅ Clear messaging about age requirements
- ✅ Explain why 18+ is required (Bangladesh law, cash prizes)
- ✅ Provide alternative (continue using global version)
- ✅ No deceptive practices or hidden age restrictions

#### 4. Compliance
- ✅ Multi-layer age verification (Google Play + in-app + payment)
- ✅ Clear Terms of Service with age requirements
- ✅ Document compliance approach for legal review
- ✅ Monitor policy changes and adapt as needed

#### 5. Technical Implementation
- ✅ No age checks needed in promotion logic
- ✅ Eligibility confirmation required in Bangladesh app
- ✅ Track analytics for both versions separately
- ✅ Maintain both versions indefinitely

---

### Next Steps

1. **Before Development**:
   - [ ] Review this strategy with legal counsel
   - [ ] Check Google Play Console for current user age distribution
   - [ ] Adjust migration targets based on actual demographics

2. **During Development**:
   - [ ] Complete IARC questionnaire for Bangladesh version
   - [ ] Configure 18+ age rating in Google Play Console
   - [ ] Update in-app promotion messaging to mention age requirement
   - [ ] Implement eligibility confirmation screen in Bangladesh app

3. **Before Launch**:
   - [ ] Verify age rating is correctly set (18+)
   - [ ] Test installation from Google Play with test accounts (under 18 and 18+)
   - [ ] Confirm Google Play blocks installation for underage users
   - [ ] Review all user-facing messaging for clarity

4. **After Launch**:
   - [ ] Monitor user feedback about age restrictions
   - [ ] Track analytics: promotion clicks vs. actual installs
   - [ ] Adjust messaging if users are confused
   - [ ] Document any issues for future reference

---

**Document Prepared By**: Copilot  
**Review Required**: Legal counsel, Product Owner (Piotr Gorczyński)  
**Approval Required**: Product Owner

---

## Appendix: Age Rating Decision Tree

```
Is user in Bangladesh?
├── No → Show global version only
│        (13+ rating, no cash prizes)
│
└── Yes → Is user 18+ years old?
          ├── No (13-17) → Can only install global version
          │                (Google Play blocks Bangladesh version)
          │                Shows: "Gridline Soccer" (13+)
          │
          └── Yes (18+) → Can install both versions
                          Shows: 
                          - "Gridline Soccer" (13+)
                          - "Gridline Soccer Bangladesh" (18+)
                          
                          User choice:
                          ├── Wants cash prizes → Install Bangladesh version
                          ├── Doesn't want prizes → Install global version
                          └── Wants both → Install both versions
```

---

## Appendix: Sample Google Play Store Listing Comparison

### Global Version

**Title**: Gridline Soccer - Multiplayer Paper Soccer

**Short Description**:
Classic paper soccer game! Play against friends in strategic turn-based matches.

**Age Rating**: Teen (13+)

**Content Rating Details**:
- No violence
- No sexual content
- No gambling
- Multiplayer interaction

---

### Bangladesh Version

**Title**: Gridline Soccer Bangladesh - Win Cash Prizes!

**Short Description**:
Play skill-based tournaments and win ৳2,000! Free entry, 18+ only. Bangladesh players welcome.

**Age Rating**: Adults Only (18+)

**Content Rating Details**:
- Skill-based tournaments with real cash prizes
- Free entry, no purchase required
- Developer-funded prizes
- Participants must be 18 years or older
- Complies with Bangladesh gaming regulations

**Note in Description**:
```
⚠️ AGE REQUIREMENT: You must be 18 years or older to install 
and participate in cash prize tournaments.

This app is exclusively for Bangladesh residents aged 18+.
All tournaments are skill-based with free entry and developer-funded prizes.
This is not gambling.
```

---

This comprehensive strategy ensures compliance with both Google Play policies and Bangladesh legal requirements while providing a clear path for users of all ages.

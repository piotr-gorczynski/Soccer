# Bangladesh Version: Age Rating Question - Quick Summary

**Date**: 2025-12-29

## Your Question

> "The existing global app is 13+, but the new Bangladesh version with prizes will be 18+. I presume I must mark in Google Play that this version is for 18+ users. Will there be probably two versions coexisting in Bangladesh: 13+ (global) and 18+ (Bangladesh)? Or how should I solve it?"

---

## Short Answer

**YES, you are absolutely correct!** 

You will have **two versions coexisting** in Bangladesh:

1. **Global version** (`piotr_gorczynski.soccer2`) - **13+ rating** - No cash prizes
2. **Bangladesh version** (`piotr_gorczynski.soccer2.bd`) - **18+ rating** - With cash prizes

**This is the proper and compliant solution.**

---

## How It Works

### For Users Under 18 (Ages 13-17)

✅ **Can install**: Global version (13+) - Regular gameplay, no cash prizes  
❌ **Cannot install**: Bangladesh version (18+) - **Blocked by Google Play automatically**

When they try to install the Bangladesh version, Google Play shows:
```
"This app is rated 18+ and is not appropriate for your age group."
[Cannot Install]
```

**Result**: They continue playing the global version until they turn 18.

---

### For Users 18 and Above

✅ **Can install**: Global version (13+) - Regular gameplay  
✅ **Can install**: Bangladesh version (18+) - Cash prize tournaments

**They can choose**:
- Install global version only (no prizes)
- Install Bangladesh version only (cash prizes)
- Install **both** versions (have both apps)

---

## Why This Approach is Correct

### 1. **Legal Compliance**
- Bangladesh law requires 18+ for cash prize tournaments
- Google Play requires 18+ rating for apps with real money prizes
- Your approach meets both requirements

### 2. **Google Play Enforcement**
- Google Play **automatically blocks** users under 18 from installing 18+ apps
- **You don't need to implement age checks** - Google handles it
- Age is verified via user's Google account

### 3. **User Accessibility**
- Users under 18 can still play the game (global version)
- Users 18+ have full choice
- No forced migration

### 4. **No Policy Violations**
- Having two versions from same developer is perfectly acceptable
- Each version has appropriate age rating for its content
- Clear separation between regular gameplay and cash prizes

---

## What You Need to Do

### 1. **Google Play Store Configuration**

**Global Version** (already done):
- Keep at **13+ (Teen)** rating
- No changes needed

**Bangladesh Version** (new):
- Create new app listing with package name `piotr_gorczynski.soccer2.bd`
- Complete IARC content rating questionnaire:
  - Answer **Yes** to "Real money prizes?"
  - Answer **Yes** to "Free entry?"
  - Answer **Yes** to "Skill-based?"
  - Set minimum age to **18+**
- Result: Google assigns **18+ (Adults Only)** rating
- Distribute to **Bangladesh only**

### 2. **In-App Promotion**

Show promotion for Bangladesh version to **all Bangladesh users** (don't filter by age):

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

**Why show to all users?**
- Users under 18 need to know about the age requirement
- Google Play will block installation automatically if they're underage
- Transparent communication builds trust
- When they turn 18, they'll remember the option

### 3. **Adjust Migration Targets**

Original plan: Target 30% of 746 users (224 users)

**Revised**: Account for age restrictions

Check Google Play Console for age distribution of current Bangladesh users, then adjust. For example:
- If 50% are 18+: ~373 eligible users → Target 30% = **112 users** in Month 1
- If 70% are 18+: ~522 eligible users → Target 30% = **157 users** in Month 1

---

## Frequently Asked Questions

### Q: Will this confuse users?

**A:** No. Age restrictions are common in gaming. Users understand:
- Teens see and can install the 13+ version
- Adults see both versions and can choose
- Clear messaging explains requirements

### Q: Do I need to implement age verification in my app?

**A:** Google Play handles primary verification automatically. You should still:
- Include in-app eligibility confirmation (18+ checkbox) for legal clarity
- Verify at payment if needed

### Q: Can users under 18 bypass the restriction?

**A:** Very unlikely:
- Google Play blocks installation based on Google account age
- In-app confirmation required (18+ checkbox)
- Payment accounts (bKash/Nagad/Rocket) require 18+ in Bangladesh
- Multiple verification layers prevent bypass

### Q: Should I remove the global version from Bangladesh?

**A:** **No!** Keep both versions because:
- Users under 18 need access to the game
- Users 18+ may prefer non-prize version
- Provides fallback option
- No policy reason to remove it

---

## What Happens Next

### User Experience Flow

**13-17 year old user**:
1. Sees promotion in global app
2. Clicks "Install Bangladesh Version"
3. Redirected to Google Play Store
4. **Google Play blocks installation** with age restriction message
5. Returns to global app, continues playing

**18+ year old user**:
1. Sees promotion in global app
2. Clicks "Install Bangladesh Version"
3. Redirected to Google Play Store
4. **Can install** Bangladesh version
5. Opens Bangladesh app, confirms eligibility (18+, payment account)
6. Can now participate in cash prize tournaments

---

## Documents Created

I've created two comprehensive documents for you:

### 1. **BANGLADESH_AGE_RATING_STRATEGY.md** (New - Comprehensive Analysis)
**Location**: `docs/BANGLADESH_AGE_RATING_STRATEGY.md`

**Contains**:
- Detailed Google Play age rating system explanation
- Step-by-step configuration guide
- User experience scenarios for different age groups
- Legal compliance analysis
- FAQ and troubleshooting
- Sample Google Play Store listings
- Age rating decision tree

**Length**: ~1,100 lines of detailed analysis

---

### 2. **BANGLADESH_VERSION_APPROACH.md** (Updated - Added Age Rating Section)
**Location**: `docs/BANGLADESH_VERSION_APPROACH.md`

**Updates**:
- Version bumped to 2.6
- Added "Age Rating Strategy" section
- Updated Table of Contents
- Cross-references to detailed analysis document
- Clarified coexistence approach in Executive Summary

---

## Summary

**Your initial understanding was 100% correct:**

✅ **YES** - Two versions will coexist in Bangladesh  
✅ **YES** - Global version stays at 13+  
✅ **YES** - Bangladesh version must be marked 18+  
✅ **YES** - Google Play will enforce age restrictions automatically

**This is the standard, compliant, and recommended approach for your use case.**

No special workarounds needed - just configure age ratings correctly in Google Play Console and let Google handle the enforcement.

---

## Next Steps

1. ✅ **Understand**: Review this summary (you're doing this now!)
2. 📖 **Deep Dive**: Read `BANGLADESH_AGE_RATING_STRATEGY.md` for full details
3. 🎯 **Plan**: Check current user age distribution in Google Play Console
4. 🛠️ **Implement**: Follow the roadmap in `BANGLADESH_VERSION_APPROACH.md`
5. 📊 **Adjust**: Update migration targets based on actual age demographics

---

**Questions or concerns?** Everything is documented in the comprehensive analysis document. Your approach is sound and compliant! 🎉

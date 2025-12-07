# User Retention Strategy - Quick Start Guide

## 🚨 The Problem
Your app has **98% user churn** by Day 14. Users install, play once, and never come back.

## ✅ The Solution
Implement these 3 features **in the next 2 weeks** to see immediate improvement:

---

## Week 1: Push Notifications

### What It Does
Reminds users when friends send game invites or when it's their turn to play.

### Why It Matters
**Primary cause of churn:** Users forget about pending invites.

### Files to Modify
```
1. mobile/app/src/main/java/piotr_gorczynski/soccer2/notifications/MyFirebaseMessagingService.java
   - Add handler for "game_invite" notification type
   - Add handler for "your_turn" notification type

2. mobile/app/src/main/java/piotr_gorczynski/soccer2/SettingsActivity.java
   - Add notification preferences toggle

3. gcp/cloud-functions/ (NEW)
   - Create send-game-invite-notification.js
   - Trigger: onCreate in invitations collection
```

### Implementation Steps
1. Add notification handling in MyFirebaseMessagingService
2. Create Cloud Function to send notifications on new invites
3. Add settings toggle for notification preferences
4. Test with 2 devices

### Expected Impact
📈 **+25% Day 1 retention** (350 → 440 users per 1000 installs)

---

## Week 2: Daily Login Rewards

### What It Does
Gives users a small reward (badge, cosmetic) for logging in each day.

### Why It Matters
Creates daily habit and gives users a reason to check the app.

### Database Schema
```javascript
// Add to users/{userId} document
{
  loginStreak: 0,
  lastLoginDate: "2025-12-07",
  rewardsClaimed: [],
  inventory: {
    badge_day1: true,
    badge_day7: false,
    // ... more rewards
  }
}
```

### Files to Create/Modify
```
1. mobile/app/src/main/java/piotr_gorczynski/soccer2/DailyRewardsManager.java (NEW)
   - Check login streak
   - Award rewards
   - Update Firestore

2. mobile/app/src/main/java/piotr_gorczynski/soccer2/MenuActivity.java
   - Call DailyRewardsManager on app open
   - Show reward dialog if eligible

3. mobile/app/src/main/res/layout/dialog_daily_reward.xml (NEW)
   - UI for showing today's reward

4. firebase/seed/daily_rewards.json (NEW)
   - Define reward schedule
```

### Reward Schedule
```
Day 1: "Welcome Badge"
Day 2: "Streak Starter Badge"  
Day 3: "Committed Player Badge"
Day 7: "Weekly Warrior Badge" + Tournament Ticket
Day 14: "Dedicated Player Badge"
Day 30: "Veteran Badge" + Special Ball Skin
```

### Expected Impact
📈 **+30% Day 2-7 retention** (50 → 65 users at Day 7 per 1000 installs)

---

## Week 2: Rematch Button

### What It Does
After a match ends, shows "Play Again?" button that creates instant rematch.

### Why It Matters
Extends session length when users are already engaged.

### Files to Modify
```
1. mobile/app/src/main/java/piotr_gorczynski/soccer2/GameActivity.java
   - Add rematch button to end-of-game UI
   - Send rematch invite to opponent
   - Handle rematch acceptance

2. mobile/app/src/main/res/layout/activity_game.xml
   - Add rematch button (hidden until game ends)
```

### Implementation Steps
1. Add "Rematch?" button that appears on game end
2. Create invite in Firestore with type="rematch"
3. Auto-navigate both players to new game if accepted
4. Track rematch acceptance rate in AnalyticsManager

### Expected Impact
📈 **+20% session length** (3 min → 3.6 min average)

---

## Testing Checklist

### Push Notifications
- [ ] Notification appears when invite received
- [ ] Tapping notification opens app to correct screen
- [ ] Can toggle notifications on/off in settings
- [ ] Respects Android notification settings
- [ ] Works when app is closed/background

### Daily Rewards
- [ ] Shows reward dialog on first daily open
- [ ] Tracks login streak correctly
- [ ] Doesn't show reward twice in same day
- [ ] Resets streak if user misses a day
- [ ] Rewards stored in user inventory

### Rematch
- [ ] Button appears after match ends
- [ ] Sends notification to opponent
- [ ] Both players navigate to new game
- [ ] Works for both win/loss scenarios
- [ ] Analytics tracks rematch rate

---

## Deployment Order

### Day 1-2: Infrastructure
- Set up Cloud Functions project structure
- Configure Firebase Cloud Messaging
- Test notification delivery

### Day 3-5: Push Notifications
- Implement notification handlers
- Deploy Cloud Functions
- Test end-to-end flow

### Day 6-8: Daily Rewards
- Create reward system
- Design reward dialog UI
- Implement streak tracking

### Day 9-10: Rematch Feature
- Add rematch button
- Implement invite logic
- Polish UX

### Day 11-14: Testing & Polish
- Full QA pass on all features
- Fix any bugs found
- Prepare for production release

---

## Measuring Success

### Before Launch (Baseline)
Track for 7 days before deploying:
```
Day 1 Retention: _____%
Day 7 Retention: _____%
Avg Session Length: _____ minutes
```

### After Launch (Week 1)
```
Day 1 Retention: _____%  (target: +20%)
Push Notif Opt-in: _____%  (target: 60%+)
Reward Claim Rate: _____%  (target: 70%+)
```

### After Launch (Week 2)
```
Day 7 Retention: _____%  (target: +30%)
Rematch Rate: _____%  (target: 40%+)
Session Length: _____ min  (target: +20%)
```

---

## Analytics Events to Track

Add to AnalyticsManager:
```java
// Push notifications
trackNotificationReceived(String type);
trackNotificationClicked(String type);
trackNotificationPermissionChanged(boolean enabled);

// Daily rewards
trackDailyRewardShown(int day);
trackDailyRewardClaimed(String rewardId, int streak);
trackLoginStreakBroken(int lastStreak);

// Rematch
trackRematchOffered();
trackRematchAccepted();
trackRematchDeclined();
```

---

## Common Pitfalls to Avoid

### ❌ Don't
- Send too many notifications (max 3-4 per day)
- Make rewards too complicated (keep it simple)
- Force users into new features (always allow dismissal)
- Forget to test on physical devices
- Skip analytics implementation

### ✅ Do
- Respect notification preferences
- Make rewards feel meaningful
- Provide clear visual feedback
- Test edge cases (offline, app killed, etc.)
- Monitor analytics daily after launch

---

## Help & Resources

### Full Documentation
- 📖 [Complete Strategy](./USER_RETENTION_IMPROVEMENT_IDEAS.md) - All 20 ideas
- 🎯 [Priority Roadmap](./RETENTION_IMPLEMENTATION_PRIORITY.md) - 12-week plan
- 📊 [Impact Projections](./RETENTION_IMPACT_VISUALIZATION.md) - Data and ROI
- 📋 [Executive Summary](./RETENTION_STRATEGY_SUMMARY.md) - Overview

### Code References
- Existing analytics: `mobile/app/src/main/java/piotr_gorczynski/soccer2/AnalyticsManager.java`
- Existing notifications: `mobile/app/src/main/java/piotr_gorczynski/soccer2/notifications/`
- Firestore structure: See existing collections in Firebase Console

### Questions?
**"How do I test notifications?"**  
Use Firebase Console > Cloud Messaging to send test notifications to specific devices.

**"Where should I store rewards data?"**  
In the users/{userId} document in Firestore, in an `inventory` map.

**"What if users opt out of notifications?"**  
Respect it! Daily rewards and rematch will still work without notifications.

---

## Next Steps After Week 2

Once these 3 features are live and showing results:

1. **Week 3-4:** Implement Achievement System
2. **Week 5-6:** Add Weekly Challenges
3. **Week 7-8:** Build Friend Activity Feed
4. **Week 9-12:** Enhanced Onboarding + Social Features

See [RETENTION_IMPLEMENTATION_PRIORITY.md](./RETENTION_IMPLEMENTATION_PRIORITY.md) for the complete roadmap.

---

## TL;DR

1. **Week 1:** Add push notifications for game invites
2. **Week 2:** Add daily login rewards + rematch button
3. **Result:** 20-30% improvement in retention within 2 weeks
4. **Monitor:** Analytics to validate impact
5. **Iterate:** Continue with Phase 2 features if successful

**Get started now!** Every day of delay is losing 95%+ of your new users.

---

**Created:** 2025-12-07  
**Priority:** 🔴 URGENT - Start immediately  
**Time to Impact:** 2 weeks  
**Expected ROI:** 3-5x within 3 months

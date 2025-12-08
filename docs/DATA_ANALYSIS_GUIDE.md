# Data Analysis Guide - How to Use Your Firebase and AdMob Reports

## Purpose
This guide helps you analyze your actual Firebase Analytics and AdMob reports to validate and refine the retention strategy recommendations.

---

## Firebase Analytics Data Analysis

### From: Firebase_overview.csv

#### Key Metrics to Extract

1. **Daily Active Users (DAU)**
   - Look for: "Active users" or "Users" column
   - Average DAU over last 30 days
   - Trend: Growing, stable, or declining?

2. **Retention Rates** (Already have from graph)
   - Day 1: ~35%
   - Day 7: ~5%
   - Day 30: ~2%

3. **Engagement Metrics**
   - Average session duration (minutes)
   - Sessions per user per day
   - Screens per session

4. **User Acquisition**
   - New users per day/week/month
   - Acquisition channels (organic, ads, referrals)

5. **Geographic Distribution**
   - Top countries by user count
   - This impacts eCPM (US/EU higher, others lower)

### Example Analysis Template

```
=== FIREBASE ANALYTICS SUMMARY ===

User Base:
- Daily Active Users (DAU): _____ users
- Monthly Active Users (MAU): _____ users
- New Users (last 30 days): _____ users

Retention:
- Day 1: 35% (from graph)
- Day 7: 5% (from graph)
- Day 30: 2% (from graph)

Engagement:
- Average session duration: _____ minutes
- Sessions per user per day: _____
- Screens per session: _____

Geography:
- Top country: _____ (___%)
- Second: _____ (___%)
- Third: _____ (___%)

Drop-off Points:
- Percentage reaching online multiplayer: _____%
- Percentage adding friends: _____%
- Percentage joining tournaments: _____%
```

---

## AdMob Revenue Data Analysis

### From: admob-report.csv

#### Key Metrics to Extract

1. **Revenue Metrics**
   - Total revenue (last 30 days)
   - Revenue per day
   - Trend: Growing, stable, or declining?

2. **Ad Performance**
   - Total impressions (last 30 days)
   - Click-through rate (CTR)
   - Effective CPM (eCPM) by ad type
   - Fill rate (percentage of ad requests filled)

3. **Ad Types Performance**
   - Interstitial ads: Impressions, eCPM, revenue
   - Banner ads: Impressions, eCPM, revenue
   - Rewarded video: Impressions, eCPM, revenue (if any)

4. **Geographic Performance**
   - eCPM by country
   - Revenue by country
   - This validates geographic strategy

### Example Analysis Template

```
=== ADMOB REVENUE SUMMARY ===

Overall Performance (Last 30 days):
- Total Revenue: $_____
- Total Impressions: _____
- Average eCPM: $_____
- Fill Rate: _____%

Ad Types:
Interstitial Ads:
- Impressions: _____
- eCPM: $_____
- Revenue: $_____
- Percentage of total: _____%

Banner Ads (if any):
- Impressions: _____
- eCPM: $_____
- Revenue: $_____
- Percentage of total: _____%

Rewarded Video (if any):
- Impressions: _____
- eCPM: $_____
- Revenue: $_____
- Percentage of total: _____%

Geographic Breakdown:
- Top country: _____ ($_____ eCPM)
- Second: _____ ($_____ eCPM)
- Third: _____ ($_____ eCPM)

User Metrics:
- Impressions per DAU: _____ (Total impressions / DAU)
- Revenue per DAU: $_____ (Total revenue / DAU)
- Sessions per impression: _____ (Sessions / Impressions)
```

---

## Calculating Current Baseline

### Formula: Ad Impressions per User

```
Ad Frequency = Total Impressions / Total Sessions
```

Example:
- 10,000 impressions last month
- 20,000 sessions last month
- Ad Frequency = 0.5 (one ad every 2 sessions)

### Formula: Revenue per User

```
Revenue per DAU = Total Revenue / Average DAU
```

Example:
- $50 revenue last month
- 100 average DAU
- Revenue per DAU = $0.50/month or $6/year

### Formula: Lifetime Value (LTV)

```
LTV = Revenue per DAU × Average Lifetime (days)
```

Example:
- Revenue per DAU = $0.50/month = $0.017/day
- Average lifetime = 7 days (given 5% Day 7 retention)
- LTV = $0.017 × 7 = $0.12 per user

---

## Projecting Retention Impact on Revenue

### Current Baseline (Fill in with your data)

```
Current Metrics:
- DAU: _____ users
- Sessions per DAU: _____
- Impressions per session: _____
- eCPM: $_____

Current Monthly Revenue:
Revenue = DAU × Sessions per DAU × 30 days × Impressions per session × eCPM / 1000
Revenue = _____ × _____ × 30 × _____ × _____ / 1000 = $_____
```

### After Phase 1 (Push Notifications + Daily Rewards + Rematch)

**Expected Changes:**
- DAU: +100% (from improved retention)
- Sessions per DAU: +200% (from daily rewards and rematch)
- Impressions per session: Same (or add rewarded videos)

```
Phase 1 Projection:
- DAU: _____ × 2 = _____ users
- Sessions per DAU: _____ × 3 = _____
- Impressions per session: _____ (same)
- eCPM: $_____ (same or add rewarded at $7)

Phase 1 Monthly Revenue:
Revenue = _____ × _____ × 30 × _____ × _____ / 1000 = $_____

Increase: $_____ (____%)
```

### After Phase 2 (Achievements + Challenges)

**Expected Changes:**
- DAU: +200-300% total (sustained retention)
- Sessions per DAU: +300% (more reasons to play)

```
Phase 2 Projection:
- DAU: _____ × 3 = _____ users
- Sessions per DAU: _____ × 4 = _____
- Impressions per session: _____
- eCPM: $_____

Phase 2 Monthly Revenue:
Revenue = _____ × _____ × 30 × _____ × _____ / 1000 = $_____

Increase: $_____ (____%)
```

---

## ROI Calculation with Your Data

### Investment

**Phase 1 (Weeks 1-2):**
- Development time: 40-60 hours
- Cost if outsourced: $2,000-3,000
- Cost if in-house: Your hourly rate × 50 hours
- Infrastructure: $30-80/month

**Your Investment:**
- Development: $_____ (in-house or outsourced)
- Infrastructure (12 months): $_____ × 12 = $_____
- Total Year 1: $_____

### Return

**Current Annual Revenue:**
- Monthly: $_____
- Annual: $_____ × 12 = $_____

**Projected Annual Revenue (Phase 1):**
- Monthly: $_____
- Annual: $_____ × 12 = $_____
- Increase: $_____

### ROI Analysis

```
Year 1:
- Investment: $_____
- Return: $_____
- Net: $_____ (profit or loss)
- ROI: _____% 

Payback Period: _____ months
```

**Decision:**
- ✅ Proceed if payback < 12-18 months
- ⚠️ Reconsider if payback > 24 months
- ❌ Don't proceed if ROI negative after 3 years

---

## Key Questions to Answer with Your Data

### 1. What's your current ad frequency?
```
Impressions per session = Total Impressions / Total Sessions
Current: _____

Is this optimal?
- Too low (<0.5): Missing revenue opportunity
- About right (0.5-1): Balanced
- Too high (>1): May be annoying users
```

### 2. What's your eCPM by ad type?
```
Interstitial eCPM: $_____
Banner eCPM: $_____
Rewarded Video eCPM: $_____ (if you have it)

Opportunity:
- If no rewarded video: Add it! (eCPM $5-10)
- If low interstitial eCPM: Check geography and ad placement
```

### 3. What's your user acquisition rate?
```
New users per month: _____

This matters because:
- <100/month: Retention helps but growth is bottleneck
- 100-1000/month: Retention is critical leverage point
- >1000/month: Retention improvements have huge impact
```

### 4. Where are your users?
```
Top countries:
1. _____ (___% of users) - eCPM: $_____
2. _____ (___% of users) - eCPM: $_____
3. _____ (___% of users) - eCPM: $_____

Strategy:
- If mostly US/EU: Good eCPM, optimize retention
- If mostly developing countries: Lower eCPM, focus on volume/acquisition
```

### 5. What's your current user lifetime?
```
Average days active = ln(0.02) / ln(Day 1 retention)
Current: ~7 days (with 35% Day 1 retention)

After improvements: ~14-21 days
Impact: 2-3x more lifetime ad impressions per user
```

---

## Recommended Next Steps

### Step 1: Fill in the Templates Above
Use your Firebase and AdMob CSVs to complete all the blanks in this document.

### Step 2: Calculate Your Baseline
- Current monthly revenue
- Current DAU
- Current retention curve
- Current ad performance

### Step 3: Project Retention Impact
- Use the formulas above
- Calculate projected revenue after Phase 1, 2, 3
- Be conservative in estimates

### Step 4: Calculate ROI
- Determine your investment (time × hourly rate OR outsource cost)
- Calculate payback period
- Decide if it's worthwhile

### Step 5: Prioritize Features
Based on your data:

**If revenue is very low (<$20/month):**
- Focus on user acquisition first, not retention
- Add rewarded video ads (quick win)
- Optimize ad placement

**If revenue is moderate ($20-100/month):**
- Implement Phase 1 only (push + rewards + rematch)
- Measure impact before Phase 2
- Add rewarded videos

**If revenue is good (>$100/month):**
- Full retention strategy worth it
- Implement Phase 1-3 over 12 weeks
- Focus on long-term user engagement

---

## Data-Driven Decision Framework

### Scenario A: Low Revenue, Low Users
```
Current: <$20/month, <50 DAU
Problem: Not enough users to monetize
Solution: Focus on acquisition, not retention
Action: Basic push notifications only, invest in marketing
```

### Scenario B: Low Revenue, Good Users
```
Current: <$50/month, 100+ DAU
Problem: Poor monetization (low eCPM or ad frequency)
Solution: Optimize ads first, then retention
Action: Add rewarded videos, adjust ad frequency, then Phase 1
```

### Scenario C: Good Revenue, Low Retention
```
Current: $50-200/month, poor retention curve
Problem: Users churn before lifetime value is realized
Solution: Implement retention strategy
Action: Full Phase 1-3 plan worthwhile
```

### Scenario D: Good Revenue, Good Retention
```
Current: >$200/month, decent retention
Problem: Already optimized, diminishing returns
Solution: Focus on advanced features and growth
Action: Social features, viral mechanics, new game modes
```

---

## Share Your Results

Once you've analyzed your data using this guide, update:
1. **RETENTION_REVISED_ANALYSIS.md** with actual numbers
2. **ROI projections** with real baseline and targets
3. **Feature priorities** based on your scenario (A/B/C/D)

This ensures recommendations are tailored to **your actual data**, not assumptions.

---

**Created:** 2025-12-08  
**Purpose:** Bridge strategy recommendations with actual Firebase/AdMob data  
**Next Step:** Fill in templates with your CSV data

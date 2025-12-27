# Bangladesh Version Approach

**Document Version:** 2.0  
**Last Updated:** 2025-12-27  
**Status:** Planning

**Revision History**:
- v2.0 (2025-12-27): Simplified approach - manual payments, self-declaration age verification, ৳2,000 bi-monthly prizes
- v1.0 (2025-12-27): Initial comprehensive approach with payment gateway integration

## Executive Summary

This document outlines a simplified, cost-effective approach for creating a Bangladesh-specific version of the Soccer (Gridline Soccer) mobile application that enables skill-based tournaments with promotional cash prizes. The implementation complies with Bangladesh gaming regulations, focusing on skill-based competitions with developer-funded prizes for players aged 18 and above.

**Key Simplifications**:
- **Prize Structure**: ৳2,000 BDT (~$18 USD) for 1st place winners only, bi-monthly tournaments
- **Age Verification**: Self-declaration via checkbox + Google Play Store verification (no document upload)
- **Payment Processing**: Manual processing by developer outside the app (no payment gateway API integration)
- **Total Cost**: ~$8,000-$12,000 initial setup, ~$106-$186/month operational (vs. original ~$16,500-$23,000 / ~$900-$1,400)

This streamlined approach significantly reduces development complexity, time to market, user friction, and operational costs while maintaining full compliance with Bangladesh skill-based gaming regulations.

## Table of Contents

1. [Legal & Regulatory Framework](#legal--regulatory-framework)
2. [Key Requirements](#key-requirements)
3. [Technical Implementation Approaches](#technical-implementation-approaches)
4. [Prize & Payment System](#prize--payment-system)
5. [Age Verification System](#age-verification-system)
6. [Tournament Structure](#tournament-structure)
7. [Implementation Roadmap](#implementation-roadmap)
8. [Risk Assessment & Mitigation](#risk-assessment--mitigation)
9. [Cost Estimation](#cost-estimation)
10. [Compliance Checklist](#compliance-checklist)

---

## Legal & Regulatory Framework

### Bangladesh Gaming Regulations

Based on Bangladesh gaming laws and skill-based game regulations:

1. **Skill-Based Games**: Games where outcome depends primarily on player skill (not chance) are permissible
2. **Age Restriction**: Participants must be 18 years or older
3. **Free Entry**: No entry fee or payment required to participate
4. **Developer Funding**: Prizes funded by the game developer/owner (not from player contributions)
5. **Approved Payment Methods**: Use government-approved payment services operating in Bangladesh

### Legal Compliance Requirements

- **Skill Determination**: The game must be demonstrably skill-based
  - ✅ Paper soccer/Gridline Soccer qualifies as it requires strategic thinking, planning, and tactical execution
  - ✅ Outcome determined by player decisions, not random chance
  
- **Transparency**: Clear rules, prize structure, and terms of service
- **Data Protection**: Compliance with Bangladesh data protection regulations
- **Payment Processing**: Use licensed payment processors authorized in Bangladesh

---

## Key Requirements

### 1. Player Requirements
- **Age**: 18+ years mandatory
- **Verification**: Self-declaration via checkbox + Google Play Store account verification
- **Location**: Bangladesh residency (inferred from Google Play Store region)
- **Account**: User declares they have a valid payment account with approved service (bKash, Nagad, or Rocket)

### 2. Tournament Requirements
- **Entry**: 100% free, no payment required
- **Format**: Skill-based, round-robin or elimination bracket
- **Rules**: Clear, published, and transparent
- **Prizes**: Developer-funded cash rewards

### 3. Payment Requirements
- **Services**: Government-approved payment platforms (bKash, Nagad, or Rocket)
- **Processing**: Manual payment processing outside the app by developer
- **Status Tracking**: Payment status updated in Firestore (pending, processing, completed)
- **Timeline**: Prizes distributed within 7 days of tournament completion

---

## Technical Implementation Approaches

### Approach A: Separate Bangladesh APK (Recommended)

**Description**: Create a separate app variant specifically for Bangladesh market.

**Advantages**:
- ✅ Clear separation of features and compliance requirements
- ✅ Different package name (`piotr_gorczynski.soccer2.bd`) for Google Play
- ✅ Easier to manage Bangladesh-specific regulations
- ✅ Can have different branding/marketing
- ✅ Simpler rollback if regulations change

**Disadvantages**:
- ❌ Requires maintaining two codebases (minimal with proper architecture)
- ❌ Two separate Play Store listings

**Implementation**:
```gradle
// mobile/app/build.gradle
android {
    flavorDimensions "market"
    productFlavors {
        global {
            dimension "market"
            applicationIdSuffix ""
        }
        bangladesh {
            dimension "market"
            applicationIdSuffix ".bd"
            versionNameSuffix "-BD"
        }
    }
}
```

### Approach B: Region-Based Configuration

**Description**: Single app with runtime configuration based on user location.

**Advantages**:
- ✅ Single codebase
- ✅ One Play Store listing
- ✅ Easier updates

**Disadvantages**:
- ❌ More complex feature flags
- ❌ Potential compliance risks if geo-detection fails
- ❌ Harder to ensure Bangladesh-only features stay isolated

**Implementation**:
```java
// Config-based approach
if (UserRegion.isBangladesh()) {
    enableCashPrizeTournaments();
    requireAgeVerification();
    enablePaymentIntegration();
}
```

### Approach C: Completely Separate App

**Description**: Fork the entire repository and create a standalone Bangladesh app.

**Advantages**:
- ✅ Complete independence
- ✅ No risk of feature bleeding

**Disadvantages**:
- ❌ High maintenance overhead
- ❌ Difficult to sync bug fixes and improvements

**Recommendation**: **Approach A (Separate APK via Product Flavors)** provides the best balance of code reuse and regulatory compliance.

---

## Prize & Payment System

### Prize Structure

**Promotional Prize Pool**:
```
1st Place: ৳2,000 BDT (approximately $18 USD)

Frequency: Twice per month (bi-monthly tournaments)
Note: USD conversions based on December 2025 rates and subject to change
```

This simplified prize structure:
- Rewards only the tournament winner (1st place)
- Keeps operational complexity minimal
- Provides consistent bi-monthly prize opportunities

### Payment Processing

**Manual Payment Processing**:

Payment processing will be handled **manually outside the Gridline Soccer application** by the developer. No automatic API integration with payment gateways will be implemented.

**Supported Payment Methods** (Bangladesh-approved):

1. **bKash** (Mobile Financial Service)
   - Manual transfer by developer
   - Winner provides bKash account number

2. **Nagad** (Mobile Financial Service)
   - Manual transfer by developer
   - Winner provides Nagad account number

3. **Rocket** (Dutch-Bangla Bank Mobile Banking)
   - Manual transfer by developer
   - Winner provides Rocket account number

### Payment Flow

```
Tournament Completion
    ↓
Winner Determined (1st Place - Firestore: tournaments/{id}/results)
    ↓
Payment Record Created (Firestore: payments/{id}, status: "pending")
    ↓
Winner Notified via App (Push Notification)
    ↓
Winner Provides Payment Account Number (bKash/Nagad/Rocket)
    ↓
Developer Processes Manual Payment Outside App
    ↓
Developer Updates Payment Status in Firestore (status: "completed")
    ↓
Winner Notified of Payment Completion
```

### Firestore Schema Extension

```javascript
// Collection: tournaments
{
  id: "tournament_123",
  name: "Bangladesh Bi-Monthly Championship March 2026",
  region: "BD",
  prizePool: {
    enabled: true,
    currency: "BDT",
    firstPlacePrize: 2000, // Only 1st place winner receives prize
    fundedBy: "developer"
  },
  ageRestriction: 18,
  // ... existing fields
}

// Collection: payments
{
  id: "payment_456",
  userId: "user_789",
  tournamentId: "tournament_123",
  amount: 2000,
  currency: "BDT",
  rank: 1,
  paymentMethod: "bkash", // or "nagad", "rocket" (user-selected)
  recipientInfo: {
    accountNumber: "+8801XXXXXXXXX", // User-provided account number
    accountName: "User Name" // Optional
  },
  status: "pending", // pending, completed, failed (manually updated by developer)
  initiatedAt: Timestamp,
  completedAt: Timestamp,
  transactionId: "TXN_123456",
  notes: "March Championship - 1st Place"
}

// Collection: users (extended)
{
  id: "user_789",
  // ... existing fields
  bangladeshEligibility: {
    ageConfirmed: true, // User confirmed via checkbox they are 18+
    confirmedAt: Timestamp,
    googlePlayVerified: true, // Verified via Google Play Store account
    hasPaymentAccount: true, // User declared they have bKash/Nagad/Rocket account
    preferredPaymentMethod: "bkash" // User's preferred payment method for prizes
  }
}
```

---

## Age Verification System

### Simplified Verification Approach

To minimize barriers to entry while maintaining 18+ age compliance, the verification system relies on:

1. **Google Play Store Account Verification**
   - Users must have a valid Google account registered in Bangladesh
   - Google Play Store enforces regional policies and account verification
   - Age restrictions can be enforced through Play Store's family settings
   
2. **User Self-Declaration**
   - Users confirm they are 18+ via in-app checkbox
   - Legal acknowledgment that false declaration may result in disqualification
   - Simpler user experience than document upload

3. **Payment Account Declaration**
   - Users confirm they have a valid bKash, Nagad, or Rocket account
   - Payment accounts in Bangladesh typically require age verification by the service provider
   - Acts as indirect age verification

### Verification Process

1. **Initial Eligibility Check** (Bangladesh variant only)
   - User creates account (existing flow)
   - System detects Bangladesh region from Google Play Store
   - Prompted for tournament eligibility confirmation
   
2. **Eligibility Confirmation Screen**
   - Checkbox: "I confirm that I am 18 years of age or older"
   - Checkbox: "I have a valid bKash, Nagad, or Rocket account"
   - Checkbox: "I agree to the terms and conditions for cash prize tournaments"
   - Submit button
   
3. **Immediate Approval**
   - Upon confirmation, user is eligible for cash prize tournaments
   - No manual review or waiting period
   - User can immediately register for tournaments
   
4. **Winner Verification (Post-Tournament)**
   - If user wins 1st place, they must provide payment account details
   - Developer may verify account ownership during manual payment process
   - False declarations result in prize forfeiture and account suspension

### UI Flow

```
Menu Activity
    ↓
Tournament List (BD only: Shows cash prize badge)
    ↓
[If not confirmed] → Eligibility Confirmation Screen
    ↓
    - "You must be 18+ to participate in cash prize tournaments"
    - ☑ "I confirm I am 18 years or older"
    - ☑ "I have a valid bKash/Nagad/Rocket account"
    - ☑ "I agree to tournament terms and conditions"
    - [Submit Button]
    ↓
Eligibility Confirmed (Firestore update)
    ↓
Tournament Registration Enabled
    ↓
[If wins 1st place] → Payment Account Details Screen
    ↓
    - Select payment method (bKash/Nagad/Rocket)
    - Enter account number
    - Confirm account name
    ↓
Submit for Manual Processing
```

### Privacy & Security

- **No Document Storage**: No ID documents collected or stored
- **Minimal Data Collection**: Only age confirmation status and payment method preference
- **Google Play Trust**: Leverage Google's existing account verification
- **Post-Win Verification**: Developer verifies account during manual payment
- **Legal Protection**: Terms clearly state false declarations result in disqualification

---

## Tournament Structure

### Bangladesh-Specific Tournaments

**Identification**:
- Tournament documents have `region: "BD"` field
- Only visible to Bangladesh users (Google Play region check)
- Separate tournament listings in app with "Cash Prize" badge

**Tournament Structure**:

**Bi-Monthly Cash Prize Tournaments**
- **Prize**: ৳2,000 BDT (approximately $18 USD) for 1st place only
- **Frequency**: Twice per month (e.g., 1st and 15th of each month)
- **Participants**: 16-64 players (adjustable based on participation)
- **Format**: Round-robin or elimination bracket
- **Entry**: Completely free, no cost to participate

### Tournament Rules Enhancement

Existing tournament rules (from `tournament_rules_bn.json`) remain the same, with additions:

```json
{
  "rules": [
    // ... existing 13 rules ...
    "এই টুর্নামেন্টটি ১৮+ বছর বয়সী খেলোয়াড়দের জন্য এবং প্রথম স্থানের জন্য ৳2,000 পুরস্কার রয়েছে।",
    "পুরস্কার বিতরণ টুর্নামেন্ট সমাপ্তির ৭ দিনের মধ্যে করা হবে।",
    "পুরস্কার bKash, Nagad বা Rocket এর মাধ্যমে প্রদান করা হবে।",
    "খেলোয়াড়দের অবশ্যই ১৮+ বছর বয়সী হতে হবে এবং বৈধ পেমেন্ট অ্যাকাউন্ট থাকতে হবে।"
  ],
  "cashPrizeDisclaimer": "এই টুর্নামেন্ট সম্পূর্ণ দক্ষতা-ভিত্তিক এবং কোনো প্রবেশ ফি নেই। পুরস্কার ডেভেলপার কর্তৃক অর্থায়ন করা হয়।",
  "updatedAt": "2025-12-27T00:00:00Z"
}
```

**English Translation**:
- "This tournament is for players 18+ years old and offers ৳2,000 prize for 1st place."
- "Prize distribution will be completed within 7 days of tournament completion."
- "Prizes will be paid via bKash, Nagad, or Rocket."
- "Players must be 18+ years old and have a valid payment account."
- Disclaimer: "This tournament is purely skill-based and has no entry fee. Prizes are funded by the developer."

---

## Implementation Roadmap

### Phase 1: Planning & Setup (Week 1-2)
- [ ] Finalize legal review (consult Bangladesh legal expert)
- [ ] Register business entity in Bangladesh (if required)
- [ ] Set up personal bKash, Nagad, and/or Rocket accounts for manual prize distribution
- [ ] Define detailed prize structure
- [ ] Create product flavor for Bangladesh variant

### Phase 2: Backend Development (Week 3-4)
- [ ] Extend Firestore schema for Bangladesh features
- [ ] Create Cloud Functions for tournament completion
  - `onTournamentComplete(tournamentId)` - detect winner, create payment record
  - `updatePaymentStatus(paymentId, status)` - admin function to update payment status
- [ ] Implement eligibility confirmation workflow
  - Firestore eligibility records (age confirmation, payment account declaration)
  - No document upload required
- [ ] Create Bangladesh-specific tournament creation logic
- [ ] Add region detection (Google Play Store region)

### Phase 3: Mobile App Development (Week 5-7)
- [ ] Create Bangladesh product flavor
  - Package name: `piotr_gorczynski.soccer2.bd`
  - App name: "Gridline Soccer Bangladesh"
  - Icon badge: "BD" variant
- [ ] Implement eligibility confirmation UI
  - Simple checkbox screen (18+, payment account, terms)
  - No camera or document upload needed
  - Immediate confirmation
- [ ] Implement winner payment details collection UI
  - Payment method selector (bKash/Nagad/Rocket)
  - Account number input
  - Shown only to 1st place winners
- [ ] Update tournament UI for cash prizes
  - "৳2,000 Prize" badge on tournament listings
  - Winner notifications
  - Payment status screen (pending/completed)
- [ ] Add Bengali translations for new features

### Phase 4: Admin Tools (Week 8)
- [ ] Create simple admin interface (Firebase Console functions or web panel)
  - View tournament winners
  - View payment account details
  - Update payment status (pending → completed)
  - Manual payment processing workflow documentation
- [ ] Test complete workflow (tournament → winner → payment details → manual payment)

### Phase 5: Testing & Compliance (Week 9-10)
- [ ] End-to-end testing
  - Tournament creation and registration
  - Eligibility confirmation workflow
  - Match completion and ranking
  - Winner notification and payment details collection
  - Manual prize payment simulation
- [ ] Security audit
  - Payment account data encryption
  - API authentication
  - User data protection
- [ ] Legal compliance verification
  - Review with legal expert
  - Terms of Service update
  - Privacy Policy update
- [ ] Closed beta testing with Bangladesh users

### Phase 6: Launch Preparation (Week 11-12)
- [ ] Create Google Play Store listing (Bangladesh)
- [ ] Prepare marketing materials
- [ ] Set up customer support (Bengali language support)
- [ ] Document manual payment procedures
- [ ] Establish prize fund reserve (৳4,000/month for bi-monthly tournaments)
- [ ] Create operational runbook

### Phase 7: Soft Launch (Week 13-14)
- [ ] Limited release to 100-500 users
- [ ] Run first bi-monthly tournament
- [ ] Process first manual prize payment
- [ ] Gather user feedback
- [ ] Fix critical issues

### Phase 8: Full Launch (Week 15+)
- [ ] Public launch in Bangladesh Google Play Store
- [ ] Marketing campaign
- [ ] Establish bi-monthly tournament schedule
- [ ] Monitor KPIs (participation, payment success, user satisfaction)
- [ ] Iterate based on feedback

---

## Risk Assessment & Mitigation

### Legal Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Regulation change | High | Medium | Monitor legal landscape, maintain flexibility to pause tournaments |
| Misclassification as gambling | High | Low | Ensure skill-based nature, no entry fees, clear documentation |
| Age verification failure | Medium | Low | Robust verification process, manual review option |
| Payment processing issues | Medium | Medium | Multiple payment options, manual fallback process |

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Manual payment processing errors | Medium | Low | Double-check payment details, maintain audit trail |
| False age declarations | Medium | Medium | Legal terms clearly state consequences, post-win verification possible |
| Server costs exceed budget | Low | Low | Minimal infrastructure changes, monitor usage |
| Geo-blocking bypass | Low | Medium | Google Play region verification, terms enforcement |

### Operational Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Insufficient prize fund | Low | Low | Pre-fund reserve, only ৳4,000/month required |
| Customer support overload | Low | Low | Simple process, automated FAQs, clear documentation |
| Payment disputes | Medium | Low | Clear terms, manual verification, responsive support |
| Manual payment delays | Medium | Medium | Set clear timeline (7 days), maintain communication with winners |

---

## Cost Estimation

### Initial Setup Costs
- Legal consultation: $500 - $1,000 (simplified approach requires less legal review)
- Development time: 150-200 hours (estimated at market rate) - significantly reduced due to:
  - No payment gateway API integration
  - No document upload/verification system
  - Simplified user flow
- **Total Initial: ~$8,000 - $12,000** (adjust based on actual development costs)

### Monthly Operational Costs
- Firebase costs (increased usage): $20 - $50/month (minimal increase)
- Prize pool funding:
  - 2 bi-monthly tournaments × ৳2,000 = ৳4,000/month (approximately $36/month)
  - **Total prizes: approximately $36/month** (based on December 2025 exchange rates)
- Manual payment processing time: 1-2 hours/month (developer time)
- Customer support: $50 - $100/month (minimal support needed)
- **Total Monthly: approximately $106 - $186**

### Cost Savings vs. Original Approach
- **No payment gateway API fees**: Saved ~$500-1,000 setup + 2-3% transaction fees
- **No document storage costs**: Saved ~$20-50/month Cloud Storage
- **No verification review costs**: Saved manual review time or third-party service fees
- **Simpler development**: Saved ~150-200 development hours (~$7,500-$10,000)
- **Lower prize pool**: Saved ~$594/month in prize funding

### Annual Cost Projection (Year 1)
- Initial setup: $8,000 - $12,000
- Monthly operational: $106 - $186 × 12 = $1,272 - $2,232
- **Total Year 1: approximately $9,272 - $14,232**

### Revenue Potential (Optional)
While current model is developer-funded with no entry fees, future revenue options:
- In-app advertising (non-intrusive)
- Premium features (cosmetic items)
- Sponsorships for tournaments
- **Note**: Any revenue model must maintain compliance with skill-based gaming regulations

---

## Compliance Checklist

### Pre-Launch Checklist

#### Legal Compliance
- [ ] Consult with Bangladesh legal expert on gaming regulations
- [ ] Verify skill-based classification is valid
- [ ] Confirm simplified age verification approach is acceptable
- [ ] Update Terms of Service with Bangladesh-specific clauses
- [ ] Update Privacy Policy (minimal data collection - no ID documents)
- [ ] Add eligibility confirmation and terms acceptance in app

#### Technical Compliance
- [ ] Implement 18+ eligibility confirmation (checkbox + declaration)
- [ ] Implement geo-restriction (Bangladesh only via Google Play region)
- [ ] Free tournament entry (no payment required)
- [ ] Clear skill-based game mechanics (no randomness in outcomes)
- [ ] Transparent tournament rules
- [ ] Secure payment account data handling (encryption for account numbers)

#### Operational Compliance
- [ ] Establish prize fund reserve (৳4,000/month minimum)
- [ ] Document manual prize payout procedures
- [ ] Create customer support process (Bengali language)
- [ ] Set up payment dispute resolution process
- [ ] Define fraud detection criteria (suspicious accounts)
- [ ] Create audit trail for manual payments (spreadsheet or database)

#### User Communication
- [ ] Clear prize structure disclosure (৳2,000 for 1st place, bi-monthly)
- [ ] Payment timeline communication (within 7 days)
- [ ] Eligibility requirements notification (18+, payment account)
- [ ] Terms and conditions acceptance
- [ ] Manual payment process explanation
- [ ] Bengali language support for all compliance materials

---

## Appendices

### Appendix A: Recommended Package Structure

```
piotr_gorczynski.soccer2/
├── common/              # Shared code
├── tournament/          # Core tournament logic
└── payment/             # Payment data models (no gateway integration)
    └── PaymentInfo.java

bangladesh-specific/
├── BangladeshTournamentManager.java
├── EligibilityConfirmationActivity.java
└── WinnerPaymentDetailsActivity.java
```

### Appendix B: Sample Terms of Service Clause

```
BANGLADESH SKILL-BASED TOURNAMENTS

Eligibility: Cash prize tournaments are available only to users who:
- Are 18 years of age or older (self-declared)
- Are residents of Bangladesh (verified via Google Play Store region)
- Have confirmed they possess a valid bKash, Nagad, or Rocket account
- Have accepted the tournament terms and conditions

Entry: Participation in cash prize tournaments is completely free. No payment, 
purchase, or entry fee is required.

Skill-Based: All tournaments are based purely on player skill. The game mechanics 
involve strategic decision-making, tactical planning, and execution. There is no 
element of chance in determining match outcomes.

Prizes: Cash prizes are awarded to 1st place winners only. Prize amount is ৳2,000 BDT 
per bi-monthly tournament. All prizes are funded by the game developer. Winners will 
be contacted to provide payment account details and will receive payment within 7 
business days of tournament completion via bKash, Nagad, or Rocket (winner's choice).

Verification: The developer reserves the right to verify winner identity and 
eligibility before distributing prizes. False declarations regarding age or payment 
account ownership may result in disqualification, prize forfeiture, and account 
suspension.

Payment Processing: Prizes are processed manually by the developer outside the app. 
Winners must provide accurate payment account information. The developer is not 
responsible for delays caused by incorrect account details.
```

### Appendix C: Technical Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Mobile App (Bangladesh)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Tournament   │  │ Eligibility  │  │ Winner       │     │
│  │ Registration │  │ Confirmation │  │ Payment Info │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
└─────────┼──────────────────┼──────────────────┼────────────┘
          │                  │                  │
          │                  │                  │
┌─────────▼──────────────────▼──────────────────▼────────────┐
│                     Firebase / Firestore                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ tournaments  │  │ users        │  │ payments     │     │
│  │ (region: BD) │  │ (eligibility)│  │ (status)     │     │
│  └──────┬───────┘  └──────────────┘  └──────┬───────┘     │
└─────────┼──────────────────────────────────────┼────────────┘
          │                                      │
          │                                      │
┌─────────▼──────────────────────────────────────▼────────────┐
│                    Cloud Functions                          │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │ onTournamentEnd  │  │ updatePayment    │               │
│  │ - Calculate rank │  │ - Update status  │               │
│  │ - Create payment │  │ (admin function) │               │
│  └──────────────────┘  └──────────────────┘               │
└─────────────────────────────────────────────────────────────┘
                             │
                             │
                   ┌─────────▼────────┐
                   │  Developer       │
                   │  Manual Payment  │
                   │  (bKash/Nagad/   │
                   │   Rocket)        │
                   └──────────────────┘
```

---

## Conclusion

This approach document provides a simplified, cost-effective framework for launching a Bangladesh-specific version of Gridline Soccer with promotional cash prizes. The streamlined implementation minimizes development complexity and operational overhead while maintaining compliance with Bangladesh skill-based gaming regulations.

**Key Success Factors**:
1. **Legal Compliance**: Strict adherence to Bangladesh skill-based gaming regulations
2. **Simplified Eligibility**: Google Play verification + user declaration (no document upload)
3. **Manual Payment Processing**: Developer-controlled prize distribution outside the app
4. **Low Operational Cost**: Only ৳4,000/month (~$36) for bi-monthly tournaments
5. **User Experience**: Minimal friction for players, no complex verification steps

**Simplified Approach Benefits**:
- **Faster time to market**: 10-15 weeks vs. 16+ weeks
- **Lower development cost**: ~$8,000-$12,000 vs. ~$16,500-$23,000
- **Minimal ongoing costs**: ~$106-$186/month vs. ~$900-$1,400/month
- **Reduced complexity**: No payment gateway APIs, no document storage, simpler user flow
- **Lower user friction**: No document upload, immediate eligibility confirmation

**Next Steps**:
1. Review this simplified approach with legal counsel familiar with Bangladesh regulations
2. Confirm that self-declaration age verification is acceptable
3. Begin Phase 1 implementation (planning & setup)
4. Establish bi-monthly tournament schedule
5. Create manual payment processing procedures

---

**Document Prepared By**: Development Team  
**Review Required**: Legal counsel, payment gateway experts, Bangladesh market specialists  
**Approval Required**: Product Owner (Piotr Gorczyński)

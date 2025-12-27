# Bangladesh Version Approach

**Document Version:** 1.0  
**Last Updated:** 2025-12-27  
**Status:** Planning

## Executive Summary

This document outlines the approach for creating a Bangladesh-specific version of the Soccer (Gridline Soccer) mobile application that enables skill-based tournaments with cash prizes. The implementation will comply with Bangladesh gaming regulations, focusing on skill-based competitions with developer-funded prizes for players aged 18 and above.

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
- **Verification**: Government-issued ID verification
- **Location**: Bangladesh residency verification
- **Account**: Valid payment account with approved service

### 2. Tournament Requirements
- **Entry**: 100% free, no payment required
- **Format**: Skill-based, round-robin or elimination bracket
- **Rules**: Clear, published, and transparent
- **Prizes**: Developer-funded cash rewards

### 3. Payment Requirements
- **Services**: Government-approved payment platforms (bKash, Nagad, Rocket, bank transfer)
- **Verification**: KYC (Know Your Customer) compliance
- **Processing**: Secure, auditable transactions
- **Timeline**: Clear payment schedule (e.g., within 7 days of tournament completion)

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

**Example Tournament Prize Pool**:
```
1st Place: ৳5,000 BDT (~$45 USD)
2nd Place: ৳3,000 BDT (~$27 USD)
3rd Place: ৳2,000 BDT (~$18 USD)

Total per tournament: ৳10,000 BDT (~$90 USD)
```

Prizes can be scaled based on:
- Tournament tier (bronze, silver, gold, platinum)
- Number of participants
- Special events (seasonal championships)

### Payment Integration

**Supported Payment Methods** (Bangladesh-approved):

1. **bKash** (Mobile Financial Service)
   - API: bKash Payment Gateway API
   - KYC: Required
   - Settlement: T+1 to T+3 days

2. **Nagad** (Mobile Financial Service)
   - API: Nagad Payment Gateway
   - KYC: Required
   - Settlement: T+1 to T+3 days

3. **Rocket** (Dutch-Bangla Bank Mobile Banking)
   - API: Rocket Merchant API
   - KYC: Required
   - Settlement: T+2 to T+5 days

4. **Bank Transfer** (Fallback)
   - Direct bank account transfer
   - Requires bank account details
   - Settlement: T+3 to T+7 days

### Payment Flow

```
Tournament Completion
    ↓
Winners Determined (Firestore: tournaments/{id}/results)
    ↓
Payment Records Created (Firestore: payments/{id})
    ↓
Manual/Automated Payment Processing
    ↓
Payment Gateway API Call (bKash/Nagad/Rocket)
    ↓
Payment Confirmation
    ↓
Update Payment Status (Firestore)
    ↓
Notify Winner (Push Notification + Email)
    ↓
Record in Payment History
```

### Firestore Schema Extension

```javascript
// Collection: tournaments
{
  id: "tournament_123",
  name: "Bangladesh Championship March 2026",
  region: "BD",
  prizePool: {
    enabled: true,
    currency: "BDT",
    prizes: [
      { rank: 1, amount: 5000 },
      { rank: 2, amount: 3000 },
      { rank: 3, amount: 2000 }
    ],
    fundedBy: "developer",
    totalPool: 10000
  },
  ageRestriction: 18,
  // ... existing fields
}

// Collection: payments
{
  id: "payment_456",
  userId: "user_789",
  tournamentId: "tournament_123",
  amount: 5000,
  currency: "BDT",
  rank: 1,
  paymentMethod: "bkash", // or "nagad", "rocket", "bank"
  recipientInfo: {
    bkashNumber: "+8801XXXXXXXXX", // encrypted
    accountName: "User Name",
    verified: true
  },
  status: "pending", // pending, processing, completed, failed
  initiatedAt: Timestamp,
  completedAt: Timestamp,
  transactionId: "TXN_123456",
  notes: "March Championship - 1st Place"
}

// Collection: users (extended)
{
  id: "user_789",
  // ... existing fields
  bangladeshVerification: {
    ageVerified: true,
    idType: "NID", // National ID or Passport
    idNumber: "XXXX", // last 4 digits only, encrypted storage
    verifiedAt: Timestamp,
    documentUrl: "gs://bucket/verifications/user_789_id.jpg", // Cloud Storage
    verificationStatus: "approved" // pending, approved, rejected
  },
  paymentInfo: {
    preferredMethod: "bkash",
    bkashNumber: "+8801XXXXXXXXX", // encrypted
    nagadNumber: null,
    rocketNumber: null,
    bankAccount: null
  }
}
```

---

## Age Verification System

### Verification Process

1. **Initial Registration** (Bangladesh variant only)
   - User creates account (existing flow)
   - Prompted to verify age for tournament eligibility
   
2. **Document Upload**
   - User selects ID type: National ID (NID) or Passport
   - Takes photo or uploads document image
   - System extracts date of birth (manual review or OCR)
   
3. **Verification Review**
   - **Option A**: Manual review by administrator
   - **Option B**: Third-party verification service (e.g., Jumio, Onfido)
   - **Option C**: Semi-automated (OCR + manual review for edge cases)
   
4. **Approval/Rejection**
   - Approved: User can join cash prize tournaments
   - Rejected: User notified, can re-submit with correct documentation
   
5. **Re-verification**
   - Periodic re-verification (e.g., annually) if required by regulations

### UI Flow

```
Menu Activity
    ↓
Tournament List (BD only: Shows cash prize badge)
    ↓
[If not verified] → Age Verification Screen
    ↓
    - "You must be 18+ to participate"
    - "Upload National ID or Passport"
    - Camera / Gallery picker
    ↓
Upload Document → Firebase Storage
    ↓
Create verification request → Firestore
    ↓
Pending Review Screen
    ↓
[After approval] → Tournament Registration
```

### Privacy & Security

- **Encryption**: ID documents encrypted at rest (Firebase Storage with encryption)
- **Access Control**: Admin-only access to verification documents
- **Data Retention**: Documents deleted after verification (keep only verification status)
- **Compliance**: GDPR-like principles even if not strictly required

---

## Tournament Structure

### Bangladesh-Specific Tournaments

**Identification**:
- Tournament documents have `region: "BD"` field
- Only visible to Bangladesh users (IP + account region check)
- Separate tournament listings in app

**Tournament Types**:

1. **Daily Challenges** (Small prizes)
   - Prize: ৳500-1,000 BDT
   - Frequency: 3-5 per week
   - Participants: 8-16 players

2. **Weekly Tournaments** (Medium prizes)
   - Prize: ৳5,000-10,000 BDT
   - Frequency: Weekly
   - Participants: 32-64 players

3. **Monthly Championships** (Large prizes)
   - Prize: ৳20,000-50,000 BDT
   - Frequency: Monthly
   - Participants: 128-256 players

4. **Special Events** (Premium prizes)
   - Prize: ৳100,000+ BDT
   - Frequency: Quarterly or seasonal
   - Participants: 512+ players

### Tournament Rules Enhancement

Existing tournament rules (from `tournament_rules_bn.json`) remain the same, with additions:

```json
{
  "rules": [
    // ... existing 13 rules ...
    "এই টুর্নামেন্টটি ১৮+ বছর বয়সী খেলোয়াড়দের জন্য এবং নগদ পুরস্কার দেওয়া হবে।",
    "পুরস্কার বিতরণ টুর্নামেন্ট সমাপ্তির ৭ দিনের মধ্যে করা হবে।",
    "পুরস্কার bKash, Nagad, Rocket বা ব্যাংক ট্রান্সফারের মাধ্যমে প্রদান করা হবে।",
    "খেলোয়াড়দের অবশ্যই বৈধ পরিচয়পত্র যাচাই করতে হবে।"
  ],
  "cashPrizeDisclaimer": "এই টুর্নামেন্ট সম্পূর্ণ দক্ষতা-ভিত্তিক এবং কোনো প্রবেশ ফি নেই। পুরস্কার ডেভেলপার কর্তৃক অর্থায়ন করা হয়।",
  "updatedAt": "2025-12-27T00:00:00Z"
}
```

**English Translation**:
- "This tournament is for players 18+ years old and offers cash prizes."
- "Prize distribution will be completed within 7 days of tournament completion."
- "Prizes will be paid via bKash, Nagad, Rocket, or bank transfer."
- "Players must complete valid ID verification."
- Disclaimer: "This tournament is purely skill-based and has no entry fee. Prizes are funded by the developer."

---

## Implementation Roadmap

### Phase 1: Planning & Setup (Week 1-2)
- [ ] Finalize legal review (consult Bangladesh legal expert)
- [ ] Register business entity in Bangladesh (if required)
- [ ] Set up payment gateway accounts (bKash, Nagad, Rocket)
- [ ] Define detailed prize structure
- [ ] Create product flavor for Bangladesh variant

### Phase 2: Backend Development (Week 3-5)
- [ ] Extend Firestore schema for Bangladesh features
- [ ] Create Cloud Functions for payment processing
  - `initiatePrizePayment(tournamentId, userId, rank)`
  - `verifyPaymentStatus(paymentId)`
  - `processPaymentCallback(gatewayResponse)`
- [ ] Implement age verification workflow
  - Document upload to Cloud Storage
  - Firestore verification records
  - Admin approval interface (Firebase Console or custom admin panel)
- [ ] Create Bangladesh-specific tournament creation logic
- [ ] Add region detection and enforcement

### Phase 3: Mobile App Development (Week 6-8)
- [ ] Create Bangladesh product flavor
  - Package name: `piotr_gorczynski.soccer2.bd`
  - App name: "Gridline Soccer Bangladesh"
  - Icon badge: "BD" variant
- [ ] Implement age verification UI
  - Document upload screen
  - Camera integration
  - Verification status screen
- [ ] Implement payment info collection UI
  - bKash/Nagad/Rocket number input
  - Bank account details (optional)
  - Security and encryption
- [ ] Update tournament UI for cash prizes
  - Prize pool display
  - Winner notifications
  - Payment history screen
- [ ] Add Bengali translations for new features

### Phase 4: Payment Gateway Integration (Week 9-10)
- [ ] Integrate bKash Payment Gateway API
  - Merchant authentication
  - Payout API implementation
  - Webhook handling
- [ ] Integrate Nagad Payment Gateway
- [ ] Integrate Rocket Merchant API
- [ ] Implement fallback bank transfer process
- [ ] Test payment flows (sandbox environment)

### Phase 5: Testing & Compliance (Week 11-12)
- [ ] End-to-end testing
  - Tournament creation and registration
  - Age verification workflow
  - Match completion and ranking
  - Prize payout processing
- [ ] Security audit
  - Payment data encryption
  - ID document security
  - API authentication
- [ ] Legal compliance verification
  - Review with legal expert
  - Terms of Service update
  - Privacy Policy update
- [ ] Closed beta testing with Bangladesh users

### Phase 6: Launch Preparation (Week 13-14)
- [ ] Create Google Play Store listing (Bangladesh)
- [ ] Prepare marketing materials
- [ ] Set up customer support (Bengali language support)
- [ ] Create admin dashboard for tournament and payment management
- [ ] Establish prize fund reserve
- [ ] Document operational procedures

### Phase 7: Soft Launch (Week 15-16)
- [ ] Limited release to 100-500 users
- [ ] Monitor first tournaments
- [ ] Process first prize payments
- [ ] Gather user feedback
- [ ] Fix critical issues

### Phase 8: Full Launch (Week 17+)
- [ ] Public launch in Bangladesh Google Play Store
- [ ] Marketing campaign
- [ ] Scale up tournament frequency
- [ ] Monitor KPIs (participation, payment success rate, user satisfaction)
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
| Payment gateway API failures | High | Medium | Retry logic, multiple gateway options, manual processing fallback |
| Fraudulent age verification | Medium | Medium | Manual review, periodic re-verification, pattern detection |
| Server costs exceed budget | Medium | Low | Monitor usage, set limits on concurrent tournaments |
| Geo-blocking bypass | Low | Medium | Multi-layer verification (IP, phone number, ID document) |

### Operational Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Insufficient prize fund | High | Low | Pre-fund reserve, limit concurrent tournaments |
| Customer support overload | Medium | Medium | Automated FAQs, clear documentation, scale support team |
| Payment disputes | Medium | Medium | Clear terms, audit trail, responsive support |

---

## Cost Estimation

### Initial Setup Costs
- Legal consultation: $1,000 - $2,000
- Payment gateway setup fees: $500 - $1,000
- Development time: 300-400 hours @ $50/hr = $15,000 - $20,000
- **Total Initial: ~$16,500 - $23,000**

### Monthly Operational Costs
- Payment gateway transaction fees: ~2-3% of payouts
- Firebase costs (increased usage): $50 - $200/month
- Cloud Storage (ID documents): $20 - $50/month
- Prize pool funding (example):
  - 4 weekly tournaments × ৳10,000 = ৳40,000/month (~$360/month)
  - 1 monthly championship × ৳30,000 = ৳30,000/month (~$270/month)
  - **Total prizes: ~$630/month**
- Customer support: $200 - $500/month (part-time Bengali speaker)
- **Total Monthly: ~$900 - $1,400**

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
- [ ] Confirm payment methods are legally approved
- [ ] Update Terms of Service with Bangladesh-specific clauses
- [ ] Update Privacy Policy with age verification and payment data handling
- [ ] Add age gate and terms acceptance in app

#### Technical Compliance
- [ ] Implement 18+ age verification
- [ ] Implement geo-restriction (Bangladesh only for cash tournaments)
- [ ] Free tournament entry (no payment required)
- [ ] Clear skill-based game mechanics (no randomness in outcomes)
- [ ] Transparent tournament rules
- [ ] Secure payment data handling (encryption, PCI DSS considerations)

#### Operational Compliance
- [ ] Establish prize fund reserve
- [ ] Document prize payout procedures
- [ ] Create customer support process
- [ ] Set up payment dispute resolution process
- [ ] Implement fraud detection and prevention
- [ ] Create audit trail for all transactions

#### User Communication
- [ ] Clear prize structure disclosure
- [ ] Payment timeline communication
- [ ] Age verification requirement notification
- [ ] Terms and conditions acceptance
- [ ] Bengali language support for all compliance materials

---

## Appendices

### Appendix A: Recommended Package Structure

```
piotr_gorczynski.soccer2/
├── common/              # Shared code
├── tournament/          # Core tournament logic
├── payment/             # Payment abstraction
│   ├── PaymentGateway.java
│   ├── BkashGateway.java
│   ├── NagadGateway.java
│   └── RocketGateway.java
└── verification/        # Age verification
    ├── DocumentUploader.java
    └── VerificationStatus.java

bangladesh-specific/
├── BangladeshTournamentManager.java
├── AgeVerificationActivity.java
├── PaymentInfoActivity.java
└── PrizePaymentProcessor.java
```

### Appendix B: Sample Terms of Service Clause

```
BANGLADESH SKILL-BASED TOURNAMENTS

Eligibility: Cash prize tournaments are available only to users who:
- Are 18 years of age or older
- Are residents of Bangladesh
- Have completed age verification with valid government-issued ID
- Have registered valid payment information

Entry: Participation in cash prize tournaments is completely free. No payment, 
purchase, or entry fee is required.

Skill-Based: All tournaments are based purely on player skill. The game mechanics 
involve strategic decision-making, tactical planning, and execution. There is no 
element of chance in determining match outcomes.

Prizes: All prizes are funded by the game developer. Prize amounts are clearly 
displayed before tournament registration. Winners will be paid within 7 business 
days of tournament completion via their registered payment method (bKash, Nagad, 
Rocket, or bank transfer).

Verification: The developer reserves the right to verify winner identity and 
eligibility before distributing prizes. False information may result in 
disqualification and account suspension.
```

### Appendix C: Technical Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Mobile App (Bangladesh)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Tournament   │  │ Age Verify   │  │ Payment Info │     │
│  │ Registration │  │ Screen       │  │ Screen       │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
└─────────┼──────────────────┼──────────────────┼────────────┘
          │                  │                  │
          │                  │                  │
┌─────────▼──────────────────▼──────────────────▼────────────┐
│                     Firebase / Firestore                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ tournaments  │  │ users        │  │ payments     │     │
│  │ (region: BD) │  │ (verification│  │              │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
└─────────┼──────────────────┼──────────────────┼────────────┘
          │                  │                  │
          │                  │                  │
┌─────────▼──────────────────▼──────────────────▼────────────┐
│                    Cloud Functions                          │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │ onTournamentEnd  │  │ processPrize     │               │
│  │ - Calculate rank │  │ - Call gateway   │               │
│  │ - Create payment │  │ - Update status  │               │
│  └──────────────────┘  └─────────┬────────┘               │
└────────────────────────────────────┼────────────────────────┘
                                     │
                                     │
          ┌──────────────────────────┼──────────────────┐
          │                          │                  │
┌─────────▼───────┐  ┌──────────────▼──┐  ┌───────────▼──────┐
│ bKash Gateway   │  │ Nagad Gateway   │  │ Rocket Gateway   │
│ - Payout API    │  │ - Payout API    │  │ - Payout API     │
└─────────────────┘  └─────────────────┘  └──────────────────┘
```

---

## Conclusion

This approach document provides a comprehensive framework for launching a Bangladesh-specific version of Gridline Soccer that enables skill-based tournaments with cash prizes. The recommended implementation uses Android product flavors to create a separate APK variant, ensuring clear separation of features and compliance requirements.

**Key Success Factors**:
1. **Legal Compliance**: Strict adherence to Bangladesh skill-based gaming regulations
2. **Age Verification**: Robust 18+ verification process
3. **Payment Reliability**: Multiple payment gateway integrations with fallback options
4. **User Experience**: Seamless tournament participation and prize redemption
5. **Operational Excellence**: Efficient prize fund management and customer support

**Next Steps**:
1. Review this document with legal counsel familiar with Bangladesh regulations
2. Finalize payment gateway partnerships
3. Begin Phase 1 implementation (planning & setup)
4. Establish development timeline and resource allocation

---

**Document Author**: GitHub Copilot  
**Review Required**: Legal counsel, payment gateway experts, Bangladesh market specialists  
**Approval Required**: Product Owner (Piotr Gorczyński)

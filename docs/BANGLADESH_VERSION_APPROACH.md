# Bangladesh Version Approach

**Document Version:** 2.15  
**Last Updated:** 2026-01-19  
**Status:** Planning - Legal Validation Completed

**Revision History**:
- v2.15 (2026-01-19): Added source documents that confirm Remitly can deliver to bKash and Nagad mobile wallets.
- v2.14 (2026-01-18): **SHA COPY APPROACH VALIDATED** - Documented the successful automated SHA certificate copy approach using `gcp/cloud-build/sha_copy.yaml`. The workflow has been validated (issue #1159) and successfully handles app discovery, certificate comparison, copying, verification, and graceful handling of unprovisioned Firebase apps. Updated documentation to describe the complete working solution with detailed workflow steps, prerequisites, and usage instructions.
- v2.13 (2025-12-31): **SHA COPY YAML MIGRATION** - Moved SHA certificate copy automation into a dedicated Cloud Build config (`gcp/cloud-build/sha_copy.yaml`) and removed the standalone shell script step from the main deploy flow.
- v2.12 (2025-12-31): **FIREBASE APP ID LOOKUP HARDENING** - Updated the SHA copy step documentation to reflect more resilient app ID parsing across `apps`, `androidApps`, or `result` response shapes, plus a safer fallback split that avoids dropping fields in grep-only environments.
- v2.11 (2025-12-31): **FIREBASE APP ID LOOKUP FIX** - Clarified that the SHA copy step now detects Firebase app IDs from API responses that return either `apps` or `result`, preventing false "app ID not found" warnings in Cloud Build.
- v2.10 (2025-12-30): **FIREBASE SHA COPY RELIABILITY** - Documented the SHA certificate copy step for Firebase Android app variants and noted the resilient JSON parsing to ensure the global app ID is discovered before copying SHA certificates to the Bangladesh variant.
- v2.9 (2025-12-29): **LEGAL VALIDATION COMPLETED** - Game assumptions validated with ChatGPT legal consultation. Updated document to reflect that skill-based game structure, free entry, developer-funded prizes, and 18+ age restriction align with typical legal frameworks for promotional contests. Removed "pending legal validation" status.
- v2.8 (2025-12-29): **DOCUMENTATION CONSOLIDATION** - Merged BANGLADESH_PAYMENT_METHODS_VERIFICATION.md into this document. Added detailed payment service verification section with technical details on why bKash/Nagad/Rocket cannot be used from Poland. Added payment service FAQs. All analysis now in single document.
- v2.7 (2025-12-29): **PAYMENT METHOD UPDATE** - Clarified that Wise may have limited mobile wallet support from Poland. Updated recommendations to prioritize Remitly for direct mobile wallet transfers (bKash, Nagad), with Wise as alternative for bank transfers. Updated all payment-related sections, setup guides, and cost estimates to reflect accurate service capabilities.
- v2.6 (2025-12-29): Added comprehensive age rating strategy section - Global (13+) and Bangladesh (18+) versions will coexist. Merged all age rating analysis into main document.
- v2.5 (2025-12-28): **CRITICAL UPDATE** - Identified that bKash/Nagad/Rocket cannot be accessed from Poland. Added international money transfer solutions (Wise, Western Union, Remitly) as the correct approach for prize distribution. Added detailed setup guides and step-by-step payment processing instructions.
- v2.4 (2025-12-28): Added Authentication Integration with Google and Facebook section
- v2.3 (2025-12-28): Removed FCM/push notification strategy (not viable - users lack registered accounts/tokens)
- v2.2 (2025-12-28): Added User Onboarding & Migration Strategy section for migrating existing users
- v2.1 (2025-12-27): Simplified document - removed approach comparison, presenting product flavor approach as the chosen solution
- v2.0 (2025-12-27): Simplified approach - manual payments, self-declaration age verification, ৳2,000 bi-monthly prizes
- v1.0 (2025-12-27): Initial comprehensive approach with payment gateway integration

## Executive Summary

This document outlines a simplified, cost-effective approach for creating a Bangladesh-specific version of the Soccer (Gridline Soccer) mobile application that enables skill-based tournaments with promotional cash prizes. The implementation uses **Android Product Flavors to create a separate APK variant** and is **intended to align** with Bangladesh gaming regulations, focusing on skill-based competitions with developer-funded prizes for players aged 18 and above. **The game assumptions have been validated with ChatGPT legal consultation** (see “Regulatory Verification Notes” below).

**⚠️ CRITICAL FINDING (v2.5)**: The original approach assumed the developer could directly use bKash, Nagad, or Rocket to distribute prizes. **This is NOT possible from Poland** - these services are geo-restricted and require Bangladesh residency, phone number, and National ID. **Solution**: Use international money transfer services (Remitly, Wise, or Western Union) that can send funds to Bangladesh mobile wallets. **Remitly is recommended** for best mobile wallet support (bKash, Nagad). This adds minimal cost (~$0.50-$1.50 per transfer with Remitly Economy) and maintains the simplified approach.

**Key Simplifications**:
- **Technical Approach**: Separate Bangladesh APK using Android Product Flavors (`piotr_gorczynski.soccer2.bd`)
- **Age Rating**: Global version (13+) and Bangladesh version (18+) will coexist in Bangladesh Play Store
- **Prize Structure**: ৳2,000 BDT (~$18 USD) for 1st place winners only, bi-monthly tournaments
- **Age Verification**: Self-declaration via checkbox + Google Play Store verification (no document upload)
- **Payment Processing**: Manual processing by developer using **international transfer services** (Remitly recommended for mobile wallets, Wise for bank transfers, Western Union as backup) that send to winners' Bangladesh mobile wallets or bank accounts - NOT direct bKash/Nagad/Rocket access
- **Total Cost**: ~$8,000-$12,000 initial setup, ~$108-$190/month operational (including transfer fees)

This streamlined approach significantly reduces development complexity, time to market, user friction, and operational costs while maintaining full compliance with Bangladesh skill-based gaming regulations and being **accessible to a Polish developer without Bangladesh residency**.

**⚠️ AGE RATING STRATEGY**: The global app is rated 13+ (suitable for teens), while the Bangladesh version with cash prizes must be rated 18+ (adults only). Both versions will coexist in the Bangladesh Google Play Store. Users under 18 can continue using the global version, while users 18+ can choose to install the Bangladesh version for cash prize tournaments. See the comprehensive "Age Rating Strategy" section below for full details.

## Table of Contents

1. [Legal & Regulatory Framework](#legal--regulatory-framework)
2. [Key Requirements](#key-requirements)
3. [Technical Implementation](#technical-implementation)
4. [Prize & Payment System](#prize--payment-system)
5. [Age Verification System](#age-verification-system)
6. [Age Rating Strategy (13+ vs 18+)](#age-rating-strategy-13-vs-18)
7. [Tournament Structure](#tournament-structure)
8. [User Onboarding & Migration Strategy](#user-onboarding--migration-strategy)
9. [Authentication Integration with Google and Facebook](#authentication-integration-with-google-and-facebook)
10. [Implementation Roadmap](#implementation-roadmap)
11. [Risk Assessment & Mitigation](#risk-assessment--mitigation)
12. [Cost Estimation](#cost-estimation)
13. [Compliance Checklist](#compliance-checklist)

---

## Legal & Regulatory Framework

### Regulatory Verification Notes (Non-Legal Advice)

The approach below is designed to align with common Bangladesh restrictions on gambling and prize promotions (skill-based, free entry, developer-funded prizes, adults only). **The game assumptions have been validated with ChatGPT legal consultation**, which confirmed the approach aligns with skill-based gaming principles. **However, this document is not formal legal advice.** Before launch, consider engaging Bangladesh counsel to confirm:

1. **Applicable gambling/lottery laws** and whether the planned tournaments are clearly classified as **skill-based** promotions with **no consideration** (no entry fee, no purchase requirement).
2. **Prize promotion/contest rules** (if any) for public marketing, disclosures, winner selection transparency, and dispute handling.
3. **Age-gating requirements** (whether self-declaration + Play Store age gate is sufficient or if stronger checks are expected).
4. **Payment/remittance compliance** for inbound transfers to Bangladesh (e.g., Bangladesh Bank rules, mobile wallet receiver requirements) and any **withholding/tax** obligations on prize payouts.
5. **Data protection requirements** for handling personal data (name, phone, wallet/bank details) and cross-border processing/storage.

**Note**: The ChatGPT legal consultation validated that the game structure (skill-based, free entry, developer-funded prizes, 18+ age restriction) aligns with typical legal frameworks for promotional contests. Final compliance should be confirmed with local legal counsel if additional regulatory certainty is required.

### Bangladesh Gaming Regulations

Based on Bangladesh gaming laws and skill-based game regulations:

1. **Skill-Based Games**: Games where outcome depends primarily on player skill (not chance) are permissible
2. **Age Restriction**: Participants must be 18 years or older
3. **Free Entry**: No entry fee or payment required to participate
4. **Developer Funding**: Prizes funded by the game developer/owner (not from player contributions)
5. **Approved Payment Methods**: Use government-approved payment services operating in Bangladesh

### Legal Compliance Requirements (Validated with ChatGPT Legal Consultation)

- **Skill Determination**: The game must be demonstrably skill-based
  - ✅ Paper soccer/Gridline Soccer qualifies as it requires strategic thinking, planning, and tactical execution
  - ✅ Outcome determined by player decisions, not random chance
  
- **Transparency**: Clear rules, prize structure, and terms of service
- **Data Protection**: Compliance with Bangladesh data protection regulations and any cross-border processing requirements
- **Payment Processing**: Use licensed/authorized remittance/payment providers and comply with Bangladesh Bank rules for inbound transfers to mobile wallets/bank accounts

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

## Technical Implementation

The Bangladesh version will be implemented as a **separate APK using Android Product Flavors**. This approach provides clear separation of features and compliance requirements while maintaining code reuse with the main application.

### Product Flavor Configuration

**Note**: The current project uses environment-based flavors (`_dev`, `_test`, `_prod`) with a single `environment` dimension. To add the Bangladesh market variant, we need to introduce a **second flavor dimension** called `market`.

**Recommended approach** - Add a new flavor dimension to the existing build.gradle:

```gradle
// mobile/app/build.gradle
android {
    // Add market dimension alongside existing environment dimension
    flavorDimensions "environment", "market"
    
    productFlavors {
        // Existing environment flavors
        _dev {
            dimension "environment"
            buildConfigField "String", "AD_UNIT_ID", '"ca-app-pub-3940256099942544/1033173712"'
        }
        _test {
            dimension "environment"
            buildConfigField "String", "AD_UNIT_ID", '"ca-app-pub-3940256099942544/1033173712"'
        }
        _prod {
            dimension "environment"
            buildConfigField "String", "AD_UNIT_ID", '"ca-app-pub-9113152787055223/9306362927"'
        }
        
        // New market flavors
        global {
            dimension "market"
            // Base applicationId from defaultConfig: piotr_gorczynski.soccer2
        }
        bangladesh {
            dimension "market"
            applicationIdSuffix ".bd"
            versionNameSuffix "-BD"
        }
    }
}
```

**This creates combined build variants** like:
- `_devGlobalDebug`, `_devGlobalRelease`
- `_devBangladeshDebug`, `_devBangladeshRelease`
- `_testGlobalDebug`, `_testGlobalRelease`
- `_testBangladeshDebug`, `_testBangladeshRelease`
- `_prodGlobalDebug`, `_prodGlobalRelease`
- `_prodBangladeshDebug`, `_prodBangladeshRelease`

**For production releases**:
- Global version: Use `_prodGlobalRelease` → `piotr_gorczynski.soccer2`
- Bangladesh version: Use `_prodBangladeshRelease` → `piotr_gorczynski.soccer2.bd`

**google-services.json placement**:
- A single `google-services.json` file contains configurations for all registered Android apps in the Firebase project
- This includes both `piotr_gorczynski.soccer2` (global) and `piotr_gorczynski.soccer2.bd` (Bangladesh) package names
- Configuration files are stored in the `secrets/` directory: `secrets/google-services.{env}.json` (e.g., `google-services.prod.json`)
- The build system uses the same file for both global and Bangladesh variants
- Firebase automatically includes all package configurations in a single downloaded file

### Firebase App Creation

**Automated Setup**: Firebase Android apps for both package names are automatically created during deployment using the Cloud Build script `gcp/cloud-build/deploy_firebase.yaml`. This script:
- Checks if Firebase apps already exist for the project
- Creates apps for both package names if they don't exist:
  - `piotr_gorczynski.soccer2` (global variant)
  - `piotr_gorczynski.soccer2.bd` (Bangladesh variant)
- Prevents duplicate app creation by checking existing apps first
- Logs all operations for debugging and audit purposes

**SHA Certificate Copy for Variants (AUTOMATED APPROACH - WORKING)**: After app creation, SHA certificates (SHA-1 and SHA-256 fingerprints) need to be copied from the global app to the Bangladesh variant for features like Google Sign-In, Facebook Login, and Firebase Authentication to work properly.

**Successful Approach**: The automated SHA certificate copy is implemented in the dedicated Cloud Build config `gcp/cloud-build/sha_copy.yaml`. This approach has been validated and is working successfully (issue #1159).

**How It Works**:
1. **App Discovery**: Uses Firebase CLI to fetch all Android apps registered in the Firebase project
2. **ID Resolution**: Identifies both the global app (`piotr_gorczynski.soccer2`) and Bangladesh app (`piotr_gorczynski.soccer2.bd`) and extracts their app IDs
3. **Certificate Retrieval**: Fetches all SHA certificates from the global app using the Firebase Management API (`GET /v1beta1/projects/{projectId}/androidApps/{appId}/sha`)
4. **Comparison**: Compares certificates between global and Bangladesh apps to identify missing certificates
5. **Certificate Copy**: Copies only the missing SHA certificates to the Bangladesh app using Firebase Management API (`POST /v1beta1/projects/{projectId}/androidApps/{appId}/sha`)
6. **Verification**: Verifies all certificates were successfully copied with automatic retry logic

**Handles Unprovisioned Apps**: The workflow gracefully handles newly created Firebase apps that are not fully provisioned yet. When a Firebase app is created, it may take a few minutes for internal services (like OAuth Brand) to initialize. The workflow:
- Detects unprovisioned apps by checking for empty JSON object responses (`{}` instead of the expected `{"certificates": []}`)
- Skips SHA copy with clear warning messages explaining the situation
- Exits successfully (build doesn't fail) and provides instructions to retry after a few minutes
- Once the app is provisioned, re-running the workflow will successfully copy certificates

**Running the Workflow**:
```bash
gcloud builds submit --config gcp/cloud-build/sha_copy.yaml \
  --substitutions=_ENVIRONMENT=dev,_FOLDER_NAME=soccer
```

**Prerequisites**:
- Global Firebase app must exist with SHA certificates already configured
- Cloud Build service account needs Firebase Admin permissions

**Documentation**: See `docs/README-firebase-sha-certificates.md` for complete details and troubleshooting guide.

**Automated Configuration Download**: After Firebase apps are created, the configuration file is automatically downloaded using the Cloud Build script `gcp/cloud-build/download_google_services.yaml`. This script:
- Downloads a single `google-services.json` file that contains configurations for all registered Android apps in the project
- Uses the naming convention: `google-services.{env}.json` (e.g., `google-services.dev.json`, `google-services.prod.json`)
- The downloaded file includes client configurations for both package names (`piotr_gorczynski.soccer2` and `piotr_gorczynski.soccer2.bd`)
- Commits the file to the private repository's secrets directory
- Ensures configuration files are kept in sync with Firebase Console

**Manual Alternative**: You can also download the `google-services.json` file manually from Firebase Console if needed, but the automated approach is recommended for consistency and to avoid manual errors.

### Key Benefits

- **Clear separation**: Bangladesh-specific features are isolated from the global version
- **Different package name**: `piotr_gorczynski.soccer2.bd` for separate Google Play listing
- **Code reuse**: Shared core game logic while enabling region-specific features
- **Regulatory compliance**: Easier to manage Bangladesh-specific regulations
- **Flexible deployment**: Independent release cycles for each variant
- **Simple rollback**: Can pause Bangladesh version without affecting global app

### Build Variants

The configuration creates two separate APKs:
- **Global variant**: Standard version without cash prize tournaments
- **Bangladesh variant**: Includes cash prize tournament features with compliance requirements

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

### International Transfer Service Setup Guide (for Polish Developer)

**⚠️ CRITICAL**: As a Polish citizen, you **CANNOT directly use** bKash, Nagad, or Rocket. You must use international money transfer services.

**Recommended Service: Wise (formerly TransferWise)**

**⚠️ IMPORTANT**: Before relying on Wise, verify that it supports mobile wallet transfers to Bangladesh from Poland. If not, use Remitly as your primary service (see below).

**Setup Steps**:
1. **Create Wise Account**:
   - Visit https://wise.com
   - Sign up with email and create password
   - Verify email address
   - Provide personal details (name, address in Poland)
   - Verify identity (upload Polish ID or passport)
   - Setup takes ~15-30 minutes, verification ~1-2 days

2. **Add Funding Source**:
   - Link Polish bank account (via bank transfer)
   - OR add debit/credit card
   - Initial verification transfer may be required (~$1)

3. **Test Transfer to Bangladesh** (MANDATORY - highly recommended):
   - Click "Send money"
   - Select: Poland (PLN or EUR) → Bangladesh (BDT)
   - Amount: ৳500 BDT (approximately $4.50 - for testing)
   - **IMPORTANT**: Check if "Mobile money" or "Mobile wallet" is available as a delivery method
   - If available: Select "Mobile money" → "bKash" (or Nagad/Rocket if listed)
   - If NOT available: You'll need to use bank account transfers instead, or switch to Remitly/Western Union
   - Enter test recipient details:
     - Full name (ask a Bangladesh contact or use test account)
     - Mobile wallet number OR bank account number
   - Review fees and exchange rate
   - Confirm and send
   - Wait for transfer confirmation (typically 1-2 business days)
   - **This test is CRITICAL to verify what delivery methods Wise supports from Poland to Bangladesh**

4. **Save Recipient Templates** (after tournaments):
   - For each winner, save as recipient in Wise
   - Makes future transfers faster
   - Can reuse if same winner wins again

**Alternative Service: Western Union**

**Setup Steps**:
1. **Create Account**:
   - Visit https://www.westernunion.com/pl/en/
   - Create account with email
   - Verify identity (Polish ID)
   - Add payment method (bank account or card)

2. **Send Test Transfer**:
   - Select "Send money online"
   - Destination: Bangladesh
   - Amount: ৳500 BDT (test amount)
   - Delivery method: "Mobile wallet" or "Cash pickup"
   - For mobile wallet: Select "bKash" or "Nagad"
   - Enter recipient details
   - Review fees (typically higher than Wise)
   - Send transfer

3. **Save Recipient** (optional):
   - Western Union allows saving frequent recipients
   - Speeds up future transfers

**Recommended Service for Mobile Wallets: Remitly**

**Why Remitly**: Remitly has excellent support for direct transfers to bKash and Nagad mobile wallets from Poland, making it the most reliable choice for paying tournament winners.

**Source documents (proof of bKash/Nagad delivery support)**:
- Remitly country page for Bangladesh lists mobile money delivery options that include **bKash** and **Nagad** (select destination Bangladesh and delivery method “Mobile money”).  
  - https://www.remitly.com/us/en/bangladesh
- Remitly Help Center article for Bangladesh transfers documents **bKash**/**Nagad** as supported mobile money providers.  
  - https://help.remitly.com/s/article/How-do-I-send-money-to-Bangladesh?language=en_US

**Setup Steps**:
1. **Create Account**:
   - Visit https://www.remitly.com
   - Sign up and verify identity (upload Polish ID or passport)
   - Add payment method (bank account or card)
   - Verification typically takes 1-2 hours

2. **Choose Transfer Speed**:
   - **Express**: Arrives in minutes (higher fees ~$2-3)
   - **Economy**: Arrives in 1-3 days (lower fees ~$0.50-1.50)
   - For tournament prizes, Economy is sufficient and cost-effective

3. **Test Transfer to Mobile Wallet** (MANDATORY - highly recommended):
   - Select Bangladesh as destination
   - Choose "Mobile money" or "Cash pickup to mobile wallet" delivery
   - Select bKash or Nagad (most widely supported)
   - Amount: ৳500 BDT (approximately $4.50 - for testing)
   - Enter test recipient details:
     - Full name (ask a Bangladesh contact)
     - Mobile wallet number (format: +880 1X XXXX XXXX)
   - Review fees and exchange rate
   - Complete transfer
   - **This test verifies the complete process works before tournament prizes**

4. **Save Recipients** (after tournaments):
   - Remitly allows saving recipient details
   - Makes future transfers faster
   - Can reuse for repeat winners

**Alternative Service: Western Union**

**Comparison Table**:

| Service | Transfer Fee | Speed | Mobile Wallet Support | Pros | Cons |
|---------|-------------|-------|----------------------|------|------|
| **Wise** | $0.20-$0.40 | 1-2 days | ⚠️ Limited (mainly bank accounts) | Lowest fees, transparent | May not support direct mobile wallet transfers |
| **Remitly** | $0.50-$1.50 | Express: minutes, Economy: 1-3 days | ✅ Excellent (bKash, Nagad) | Best mobile wallet support, good balance | Mid-range fees |
| **Western Union** | $2-$5 | Minutes-1 day | ✅ Good (bKash, Nagad) | Fast, widely known | Higher fees |
| **PayPal** | ~$0.36-$0.72 | Instant | ❌ N/A (PayPal to PayPal only) | Fast if both have accounts | Limited Bangladesh adoption |

**Recommended**: Start with **Remitly** for mobile wallets (best support for bKash/Nagad), use **Wise** if winners can accept bank transfers, have **Western Union** as backup.

### Payment Processing

**Manual Payment Processing**:

Payment processing will be handled **manually outside the Gridline Soccer application** by the developer. No automatic API integration with payment gateways will be implemented.

**⚠️ IMPORTANT LIMITATION FOR POLISH DEVELOPER**:

The original approach assumed the developer could directly use bKash, Nagad, or Rocket to send prizes. **This is NOT possible from Poland** due to:

1. **Geo-restrictions**: These apps are blocked outside Bangladesh
   - bKash shows "Cannot operate in Poland" when attempting to install
   - Nagad and Rocket have similar geo-locks
   
2. **Account requirements**: All three services require:
   - Bangladesh mobile phone number (mandatory)
   - Bangladesh National ID (NID) for verification
   - Physical presence in Bangladesh for initial setup
   - Connection to Bangladesh banking system

3. **No international access**: These are domestic Bangladesh payment systems, not international transfer services

**SOLUTION**: Use international money transfer services that support Bangladesh mobile wallets as recipients.

**⚠️ CRITICAL LIMITATION IDENTIFIED**: The payment methods listed below **CANNOT be used by a Polish citizen residing in Poland**. These services are geo-restricted to Bangladesh and require:
- Bangladesh phone number (mandatory)
- Bangladesh National ID (NID) for account verification
- Physical presence in Bangladesh for account setup
- Local Bangladesh bank account

**Attempted installation of bKash from Poland results in: "Cannot operate in Poland" error.**

**Originally Proposed Payment Methods** (Bangladesh-approved but **NOT accessible from Poland**):

1. **bKash** (Mobile Financial Service)
   - ❌ **Cannot be used from Poland** - requires Bangladesh phone number and NID
   - Geo-restricted app, won't install/operate outside Bangladesh
   - Winner provides bKash account number

2. **Nagad** (Mobile Financial Service)
   - ❌ **Cannot be used from Poland** - requires Bangladesh phone number and NID
   - Operated by Bangladesh Post Office, geo-restricted
   - Winner provides Nagad account number

3. **Rocket** (Dutch-Bangla Bank Mobile Banking)
   - ❌ **Cannot be used from Poland** - requires Bangladesh phone number and bank account
   - Geo-restricted to Bangladesh banking system
   - Winner provides Rocket account number

**RECOMMENDED ALTERNATIVE PAYMENT METHODS** (Accessible from Poland):

1. **Wise (formerly TransferWise)** - International Money Transfer
   - ✅ **Works from Poland** - supports international transfers to Bangladesh
   - ⚠️ **Mobile Wallet Support**: Wise primarily supports **bank account transfers** to Bangladesh. Direct mobile wallet support (bKash, Nagad, Rocket) may be limited or unavailable from Poland. Winners may need to provide bank account details, or link their mobile wallet to a bank account for receiving funds.
   - ✅ Supports BDT currency with competitive exchange rates
   - ✅ Transparent fees (typically 1-2% for Poland → Bangladesh)
   - ✅ Transfer time: 1-2 business days
   - Winner provides: Name, bank account number (or mobile wallet linked to bank account), phone number
   - Developer needs: Wise account (free), Polish bank account or card
   - Estimated cost per ৳2,000 transfer: ~$0.20-$0.40 in fees
   - **Note**: Verify mobile wallet support during setup. If direct mobile wallet transfers are not available, use Remitly or Western Union as primary methods instead.

2. **Western Union** - International Money Transfer
   - ✅ **Works from Poland** - global service with Bangladesh support
   - ✅ Can send cash for pickup or to mobile wallets (bKash, Nagad)
   - ✅ Available online or at Western Union locations in Poland
   - ✅ Transfer time: Minutes to 1 day
   - Winner provides: Full name, phone number, location for pickup OR mobile wallet number
   - Developer needs: Western Union account, payment method
   - Estimated cost per ৳2,000 transfer: ~$2-$5 in fees

3. **PayPal** (if winner has account)
   - ✅ **Works from Poland** - international transfers supported
   - ⚠️ Limited availability - not all Bangladesh users have PayPal accounts
   - ✅ Instant transfers if both parties have accounts
   - ✅ Currency conversion handled automatically
   - Winner provides: PayPal email address
   - Developer needs: PayPal account with funding source
   - Estimated cost per ৳2,000 transfer: ~$0.36-$0.72 in fees (2% + currency conversion)

4. **Remitly** - Money Transfer Service (RECOMMENDED FOR MOBILE WALLETS)
   - ✅ **Works from Poland** - specializes in remittances to developing countries
   - ✅ **Excellent mobile wallet support**: Direct transfer to bKash, Nagad mobile wallets, or bank accounts in Bangladesh
   - ✅ Competitive rates and low fees for Bangladesh transfers
   - ✅ Transfer time: Express (minutes) or Economy (1-3 days)
   - Winner provides: Mobile wallet number or bank account details
   - Developer needs: Remitly account, Polish payment method
   - Estimated cost per ৳2,000 transfer: ~$0.50-$1.50 in fees
   - **Best choice for direct mobile wallet transfers** if Wise doesn't support them

### Payment Flow

```
Tournament Completion
    ↓
Winner Determined (1st Place - Firestore: tournaments/{id}/results)
    ↓
Payment Record Created (Firestore: payments/{id}, status: "pending")
    ↓
Winner Notified via App (In-app notification)
    ↓
Winner Provides Payment Details:
    - Full legal name
    - bKash/Nagad/Rocket mobile wallet number
    - Phone number
    - Alternative: Bank account details or PayPal (if available)
    ↓
Developer Uses International Transfer Service:
    - Remitly (recommended for mobile wallets): Transfer to bKash/Nagad
    - Wise (if bank transfers accepted): Transfer to bank account
    - Western Union (backup): Transfer to mobile wallet or cash pickup
    - PayPal: Direct transfer (if winner has account)
    ↓
Developer Initiates Transfer Outside App:
    - Log into Remitly (primary), Wise (secondary), or Western Union/PayPal (backup)
    - Enter winner's mobile wallet number, bank account, or PayPal details
    - Send ৳2,000 BDT (service handles currency conversion)
    - Save transaction ID/receipt
    ↓
Winner Receives Money in Their Mobile Wallet:
    - Funds appear in winner's bKash/Nagad/Rocket account
    - Time: Minutes to 2 business days depending on service
    ↓
Developer Updates Payment Status in Firestore:
    - status: "completed"
    - transactionId: (from transfer service)
    - completedAt: timestamp
    ↓
Winner Notified of Payment Completion
```

### Detailed Payment Service Verification

This section provides detailed technical verification of why bKash, Nagad, and Rocket cannot be used directly from Poland, and why international transfer services are required.

#### 1. bKash (Mobile Financial Service)

**Accessibility from Poland**: ❌ **NOT POSSIBLE**

**Requirements**:
- Bangladesh mobile phone number (mandatory)
- Bangladesh National ID card (NID)
- Must be physically present in Bangladesh for registration
- App is geo-locked and shows "Cannot operate in Poland" when accessed from outside Bangladesh

**Why it doesn't work**:
- The app detects your location and blocks installation/operation outside Bangladesh
- Even if you could install it, account registration requires Bangladesh NID verification
- Service is designed exclusively for Bangladesh residents

#### 2. Nagad (Mobile Financial Service)

**Accessibility from Poland**: ❌ **NOT POSSIBLE**

**Requirements**:
- Bangladesh mobile phone number (mandatory)
- Bangladesh National ID card (NID)
- Operated by Bangladesh Post Office
- Geo-restricted to Bangladesh

**Why it doesn't work**:
- Similar geo-restrictions to bKash
- Requires Bangladesh government-issued ID
- No international access available

#### 3. Rocket (Dutch-Bangla Bank Mobile Banking)

**Accessibility from Poland**: ❌ **NOT POSSIBLE**

**Requirements**:
- Bangladesh mobile phone number (mandatory)
- Account with Dutch-Bangla Bank (requires Bangladesh residency)
- Bangladesh National ID card
- Physical presence at bank branch for account opening

**Why it doesn't work**:
- Tied to Bangladesh banking system
- Requires local bank account
- No international access

### Payment Service FAQs

#### Q: Can I visit Bangladesh to set up bKash/Nagad/Rocket accounts?

**A**: Theoretically yes, but this is highly impractical:
- You'd need to get a Bangladesh phone number
- Provide Bangladesh National ID (you'd need to become a resident)
- Or use alternative documentation which may not be accepted
- Accounts may be deactivated if you return to Poland and access from there
- **Cost and effort far exceed the benefit** - international transfer services are the practical solution

#### Q: Can I hire someone in Bangladesh to process payments for me?

**A**: Possible but introduces risks:
- Trust issues (they'd have access to winner information)
- Legal implications (payment processing on your behalf)
- Communication overhead
- Still need to transfer money to that person first
- **Remitly and Wise are more secure, reliable, and cost-effective**

#### Q: What if Remitly doesn't work for a particular winner?

**A**: Multiple fallback options:
1. Try Wise (if winner can accept bank transfer or Wise supports mobile wallets from Poland)
2. Try Western Union (higher fees but very reliable and fast)
3. Offer bank transfer (winner provides bank account instead of mobile wallet)
4. Offer PayPal if winner has account
5. As last resort, cash pickup via Western Union

#### Q: Are there any other barriers I should know about?

**A**: The geo-restriction of Bangladesh payment services is the main barrier. Once you use international transfer services, the process is straightforward. The only other considerations are:
- Exchange rate fluctuations (minor - Remitly and Wise both offer good rates)
- Transfer time (1-4 days is acceptable for prize distribution)
- Winner providing correct details (collect carefully via app)

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
  paymentMethod: "bkash", // or "nagad", "rocket", "paypal", "bank" (user-selected)
  recipientInfo: {
    fullName: "User Full Legal Name", // Required for international transfers
    accountNumber: "+8801XXXXXXXXX", // User-provided mobile wallet number
    phoneNumber: "+8801XXXXXXXXX", // May be same as accountNumber for mobile wallets
    paypalEmail: "user@email.com", // Optional, if PayPal selected
    bankDetails: { // Optional, if bank transfer selected
      accountNumber: "XXXXXXXX",
      bankName: "Bank Name",
      branchName: "Branch Name"
    }
  },
  transferService: "wise", // "wise", "western_union", "remitly", "paypal" - service used by developer
  status: "pending", // pending, processing, completed, failed (manually updated by developer)
  initiatedAt: Timestamp,
  completedAt: Timestamp,
  transactionId: "TXN_123456", // Transaction ID from Wise/WU/Remitly/PayPal
  transferFee: 0.35, // Actual fee charged by transfer service (in USD)
  exchangeRate: 110.5, // Exchange rate used for the transfer
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
[If wins 1st place] → Payment Details Collection Screen
    ↓
    - Select payment method:
      ☐ bKash mobile wallet (recommended)
      ☐ Nagad mobile wallet
      ☐ Rocket mobile wallet
      ☐ PayPal (if available)
      ☐ Bank transfer
    - Enter full legal name (as per NID/passport)
    - Enter mobile wallet number (for bKash/Nagad/Rocket)
      OR PayPal email OR bank account details
    - Confirm phone number
    - Agree to receive funds via international transfer service
    ↓
Submit for Manual Processing
    ↓
Developer processes via Wise/Western Union/Remitly/PayPal
    ↓
Winner receives funds in mobile wallet (1-2 days)
```

### Privacy & Security

- **No Document Storage**: No ID documents collected or stored
- **Minimal Data Collection**: Only age confirmation status and payment method preference
- **Google Play Trust**: Leverage Google's existing account verification
- **Post-Win Verification**: Developer verifies account during manual payment
- **Legal Protection**: Terms clearly state false declarations result in disqualification

---

## Age Rating Strategy (13+ vs 18+)

### Executive Summary

**Question**: How to handle age ratings when the global app is 13+ but the Bangladesh version with cash prizes must be 18+?

**Answer**: YES, you will have two separate Google Play Store listings with different age ratings coexisting in Bangladesh:
- **Global version** (`piotr_gorczynski.soccer2`): 13+ rating - regular gameplay without cash prizes
- **Bangladesh version** (`piotr_gorczynski.soccer2.bd`): 18+ rating - includes cash prize tournaments

**This is the correct and compliant approach.** Both apps can coexist in the Bangladesh Google Play Store, allowing users to choose based on their age and interest in cash prize tournaments.

---

### Google Play Store Age Rating System

#### How Age Ratings Work

Google Play uses two systems for age classification:

##### 1. Content Rating (IARC - International Age Rating Coalition)
- **Questionnaire-based**: Developer fills out content questionnaire
- **Automatic classification**: IARC assigns ratings based on answers
- **Multiple regions**: Generates ratings for different regions (ESRB, PEGI, etc.)
- **Content-driven**: Based on violence, language, sexual content, etc.

##### 2. Target Age (Google Play Family Policy)
- **Developer-selected**: Choose target age groups
- **Determines visibility**: Affects which users see the app
- **Separate from content rating**: Independent classification

#### Age Rating Categories

Common IARC ratings:
- **Everyone**: Suitable for all ages
- **Everyone 10+**: Suitable for ages 10 and up
- **Teen**: Suitable for ages 13 and up
- **Mature 17+**: Suitable for ages 17 and up
- **Adults Only 18+**: Suitable for ages 18 and up

#### Real Money Gaming Classification

Google Play has specific policies for real money gaming:
- **Gambling apps**: Must be rated 18+ (Adults Only)
- **Cash prize apps**: May require 18+ depending on jurisdiction
- **Skill-based competitions**: Typically rated based on other content
- **Promotional prizes**: Developer-funded prizes may require 18+ in some regions

**Important**: Bangladesh cash prize tournaments are **skill-based with free entry**, NOT gambling. However, they still involve real money prizes which typically requires 18+ rating.

---

### Recommended Approach

#### Strategy: Two Separate Apps with Different Age Ratings

**Recommended Configuration**:

| App Variant | Package Name | Age Rating | Target Audience | Available In |
|-------------|--------------|------------|-----------------|--------------|
| **Global** | `piotr_gorczynski.soccer2` | **13+ (Teen)** | General players, no cash prizes | Worldwide including Bangladesh |
| **Bangladesh** | `piotr_gorczynski.soccer2.bd` | **18+ (Adults Only)** | Bangladesh adults, cash prizes | Bangladesh only |

#### Why This Works

1. **Different package names**: Completely separate app listings on Google Play Store
2. **Different content**: Bangladesh version has age-restricted content (cash prizes)
3. **Google Play allows coexistence**: Multiple apps from same developer can target same market
4. **User choice**: Users can choose appropriate version based on age and preferences
5. **Compliance**: Meets both Google Play policies and Bangladesh legal requirements

---

### Age Rating Assignment

#### Global Version (`piotr_gorczynski.soccer2`)

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

#### Bangladesh Version (`piotr_gorczynski.soccer2.bd`)

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

### Coexistence Strategy

#### How Both Apps Work Together in Bangladesh

##### Scenario: Bangladesh User on Google Play Store

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

#### Google Play Store Behavior

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

### User Experience Implications

#### For Users Under 18

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

#### For Users 18+

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

#### For Existing Users (13-17) Who Installed Global Version

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

### Google Play Store Configuration

#### Step-by-Step: Setting Age Ratings

##### For Global Version (Already Done)
1. Google Play Console → Gridline Soccer (`piotr_gorczynski.soccer2`)
2. **Content Rating** → Complete IARC questionnaire
3. Answer "No" to gambling/betting questions
4. Receive **Teen (13+)** rating
5. Save and publish

##### For Bangladesh Version (New Setup)
1. Google Play Console → Create new app → "Gridline Soccer Bangladesh"
2. Package name: `piotr_gorczynski.soccer2.bd`
   - **Note**: This will be generated by the Bangladesh product flavor with `applicationIdSuffix ".bd"` in build.gradle
   - See BANGLADESH_VERSION_APPROACH.md Technical Implementation section for build configuration details
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

#### Additional Settings for Bangladesh Version

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

### Migration Strategy Impact

#### Effect on Original Migration Plan

The age rating difference **DOES affect** the migration strategy outlined in `BANGLADESH_VERSION_APPROACH.md`:

##### Original Migration Plan
- Show in-app banner to all Bangladesh users in global app
- Direct them to install Bangladesh version
- Target: 30% migration in Month 1 (224 users out of 746)

##### Adjusted Plan (Accounting for Age Restrictions)

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

##### What Happens to Users Under 18?

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

### Legal & Compliance Considerations

#### Bangladesh Gaming Law Compliance

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

#### Google Play Policy Compliance

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

### Coexistence Best Practices

#### How to Manage Two Versions

##### In Global Version (`piotr_gorczynski.soccer2`)

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

##### In Bangladesh Version (`piotr_gorczynski.soccer2.bd`)

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

### Frequently Asked Questions

#### Q1: Will Google Play automatically block users under 18 from installing Bangladesh version?

**A**: Yes. Google Play enforces age restrictions based on the user's Google account age. If the app is rated 18+ and the user is under 18, they will either:
- Not see the app in search results, OR
- See the app but cannot install it (blocked with age restriction message)

No additional implementation needed on your part.

---

#### Q2: Can users lie about their age to Google?

**A**: While users can theoretically create Google accounts with false ages, this is:
- Against Google's Terms of Service
- Difficult (requires fake documents for age verification in some cases)
- Not your responsibility to prevent
- Mitigated by in-app eligibility confirmation and payment account verification (bKash/Nagad require age verification)

Your multi-layer approach provides adequate protection.

---

#### Q3: What if a 13-17 year old user manages to install Bangladesh version?

**A**: Unlikely due to Google Play enforcement, but if it happens:
1. User must still confirm eligibility in-app (will see 18+ requirement)
2. If they falsely confirm, they won't have valid payment account (bKash/Nagad require 18+)
3. If they somehow win, you can verify age during payout and disqualify if underage
4. Terms of Service clearly state false declarations result in disqualification

**Risk**: Very low. Multiple verification layers prevent this.

---

#### Q4: Should I remove the global version from Bangladesh to avoid confusion?

**A**: **No, absolutely not.** Keep both versions available because:
- ✅ Users under 18 need access to the game
- ✅ Users 18+ may prefer non-prize version
- ✅ Gradual migration allows users to choose
- ✅ Fallback option if Bangladesh version has issues
- ✅ No policy requiring removal of one version

**Both versions should coexist indefinitely.**

---

#### Q5: How do I market both versions without confusing users?

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

#### Q6: Will this affect my app's visibility or ranking?

**A**: Minimal impact:
- Each version has separate ranking and visibility
- 18+ restriction reduces potential audience for Bangladesh version
- Global version visibility unchanged
- Both can rank for different keywords

**Optimize separately**:
- Global version: "paper soccer", "multiplayer game", "strategy game"
- Bangladesh version: "cash prizes", "tournament", "skill competition", "Bangladesh"

---

#### Q7: What if Google Play changes its age rating policies?

**A**: Monitor policy changes and adapt:
- Subscribe to Google Play developer policy updates
- Review quarterly for any changes
- Be prepared to update age ratings if required
- Have legal counsel review annually

**Likelihood of impact**: Low. Age restrictions for real money gaming are well-established and unlikely to become more lenient.

---

#### Q8: Can I migrate users automatically when they turn 18?

**A**: No automatic migration possible because:
- You don't have access to user's exact birthdate from Google account
- Google Play doesn't provide age information to apps
- Age verification is handled by Google Play at install time

**Alternative**: Show promotion to all Bangladesh users. Google Play will allow installation when they turn 18.

---

#### Q9: How does this affect Firebase Analytics and user tracking?

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

#### Q10: What about users who have both apps installed?

**A**: Users can have both apps installed simultaneously:
- **Use case 1**: User likes having separate apps for different purposes
- **Use case 2**: User wants global version for casual play, Bangladesh version for tournaments
- **Use case 3**: User testing both versions

**Impact**: None. Both apps use same Firebase backend, same user account, data stays synced.

**Storage**: Each app ~50-100 MB, total ~100-200 MB. Not a significant concern.

---


---

### Summary

**Your initial understanding was 100% correct:**

✅ **YES** - Two versions will coexist in Bangladesh
✅ **YES** - Global version stays at 13+
✅ **YES** - Bangladesh version must be marked 18+
✅ **YES** - Google Play will enforce age restrictions automatically

**This is the standard, compliant, and recommended approach for your use case.**

No special workarounds needed - just configure age ratings correctly in Google Play Console and let Google handle the enforcement.

---

## Tournament Structure

> **📖 See [MULTI_FLAVOUR_TOURNAMENTS.md](MULTI_FLAVOUR_TOURNAMENTS.md) for complete technical documentation on multi-flavour tournament support, including schema changes, filtering implementation, and migration instructions.**

### Multi-Flavour Tournament Support

**Implementation Status**: ✅ Implemented (2026-02-01)

Tournaments now support visibility control across app flavours using the `visibleInFlavours` field:

- **Global tournaments**: `visibleInFlavours: ["global"]` - visible in all app variants (recommended)
- **Bangladesh-only tournaments**: `visibleInFlavours: ["bangladesh"]` - visible only in Bangladesh variant (for cash prize tournaments)
- **Backward compatibility**: Tournaments without the field are visible in all flavours

**Semantic Meaning**: `"global"` means "visible globally/everywhere", not "only in global app variant". This simplifies tournament configuration - you don't need to specify `["global", "bangladesh"]`, just `["global"]` is sufficient.

**Key Features**:
- Client-side filtering based on package name detection
- Migration script to update existing tournaments with `["global"]`
- Updated tournament creation tools
- Full documentation in `MULTI_FLAVOUR_TOURNAMENTS.md`

### Bangladesh-Specific Tournaments

**Identification**:
- Tournament documents have `visibleInFlavours: ["bangladesh"]` field (new approach)
- Only visible to users of the Bangladesh app variant (`piotr_gorczynski.soccer2.bd`)
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

## User Onboarding & Migration Strategy

### Migration Challenge

As of December 24, 2024, the current version of Gridline Soccer (`piotr_gorczynski.soccer2`) has **746 active installs in Bangladesh** according to Google Play Console. The new Bangladesh-specific version (`piotr_gorczynski.soccer2.bd`) will be a **separate listing on Google Play Store** with zero initial installs.

**Key Migration Questions**:
1. How to convince existing users to install the new Bangladesh-specific version?
2. Should both apps share the same Firebase backend and authentication?
3. Do we need separate Firebase authentication keys/configurations?
4. How to preserve user data and progress during migration?
5. What incentives can drive user adoption of the new version?

---

### Backend & Authentication Strategy

#### Recommended Approach: Shared Firebase Backend

**Use the same Firebase project for both versions** with the following configuration:

```
Firebase Project: gridline-soccer (existing)
├── App 1: piotr_gorczynski.soccer2 (Global version)
│   ├── Package Name: piotr_gorczynski.soccer2
│   ├── Authentication: Enabled (existing users)
│   └── Firestore Database: Shared with regional filtering
│
└── App 2: piotr_gorczynski.soccer2.bd (Bangladesh version)
    ├── Package Name: piotr_gorczynski.soccer2.bd
    ├── Authentication: Same Firebase Auth (shared users)
    └── Firestore Database: Shared with regional filtering

Firebase Configuration: Single google-services.json per environment
└── Contains client configurations for both package names
```

#### Firebase Configuration Requirements

**Do you need separate authentication keys?**

**Answer: No separate authentication, and no separate configuration files:**

1. **Same Firebase Project**: Both apps connect to the same Firebase project
2. **Different App Registrations**: Each package ID must be registered separately in Firebase Console
3. **Single google-services.json file**: One configuration file per environment contains client entries for both package names

**Setup Steps**:

```bash
# In Firebase Console (https://console.firebase.google.com)

1. Go to Project Settings → Your apps
2. Add Android app: piotr_gorczynski.soccer2 (if not already registered)
   
3. Add another Android app: piotr_gorczynski.soccer2.bd
   
4. Download google-services.json (contains both apps)
   - Place in: secrets/google-services.{env}.json
   - The file includes client configurations for both package names
   
5. Enable Authentication methods (same for both apps):
   - Email/Password
   - Google Sign-In
   - Facebook (if applicable)
   - Anonymous authentication
   
5. Firestore Security Rules (apply regional filtering):
   - Users can access their own data
   - Bangladesh users can access BD tournaments
   - Global users see global tournaments only
```

#### Firebase Authentication Behavior

**Key Points**:
- **Same user accounts work across both apps** (because they share Firebase Authentication)
- User signs in with the same credentials (email/password or Google account)
- User data (profile, stats, friends) is preserved and accessible from both apps
- **No separate login required** when switching between apps

**Authentication Flow**:
```
User installs piotr_gorczynski.soccer2.bd
    ↓
Opens app and clicks "Login"
    ↓
Uses existing credentials from piotr_gorczynski.soccer2
    ↓
Firebase Auth recognizes user (same project, same user UID)
    ↓
User data automatically syncs from Firestore
    ↓
User sees their existing profile, stats, and friend list
```

#### Firestore Data Sharing Strategy

**Shared Collections** (accessible from both apps):
- `users/` - User profiles, statistics, preferences
- `friendships/` - Friend connections and invitations
- `matches/` - Match history and results
- `tournaments/` - All tournaments with regional filtering

**Regional Filtering Logic**:

```javascript
// Firestore Security Rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users can access their own data from any app variant
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Tournament access based on region
    match /tournaments/{tournamentId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                      request.auth.uid == resource.data.createdBy;
      
      // Bangladesh users can only join BD tournaments
      // Global users can only join global tournaments
      match /participants/{participantId} {
        allow create: if request.auth != null && 
                         // Check if user's region matches tournament region
                         (get(/databases/$(database)/documents/tournaments/$(tournamentId)).data.region == 
                          get(/databases/$(database)/documents/users/$(request.auth.uid)).data.region ||
                          get(/databases/$(database)/documents/tournaments/$(tournamentId)).data.region == null);
      }
    }
    
    // Match data accessible to participants
    match /matches/{matchId} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null && 
                               (request.auth.uid == resource.data.player1Id || 
                                request.auth.uid == resource.data.player2Id);
    }
  }
}
```

**User Document Extension**:

```javascript
// Firestore: users/{userId}
{
  id: "user_123",
  email: "user@example.com",
  displayName: "Player Name",
  // ... existing fields ...
  
  // NEW: Regional configuration
  region: "BD", // or null for global users
  appVariant: "bangladesh", // or "global"
  
  // Existing users migrating from global to BD version
  migrationStatus: {
    migratedFromGlobal: true,
    migrationDate: Timestamp,
    eligibilityConfirmedForBD: false // User needs to confirm 18+ in BD app
  },
  
  // Bangladesh-specific fields (only for BD users)
  bangladeshEligibility: {
    ageConfirmed: true,
    confirmedAt: Timestamp,
    googlePlayVerified: true,
    hasPaymentAccount: true,
    preferredPaymentMethod: "bkash"
  }
}
```

---

### Migration Approaches & Recommendation

#### Option 1: In-App Notification with Deep Link (Recommended)

**Description**: Show a prominent notification in the existing `piotr_gorczynski.soccer2` app for Bangladesh users, directing them to install the new Bangladesh version.

**Implementation**:

1. **Detect Bangladesh Users** (in existing global app):
   - Check device region/locale
   - Check Google Play Store country from Firebase
   - Identify users who primarily play in Bangladesh timezone

2. **Show In-App Banner** (one-time or recurring):
   ```
   🎉 NEW: Win Cash Prizes in Bangladesh!
   
   We've launched a special version of Gridline Soccer for Bangladesh 
   with bi-monthly cash prize tournaments!
   
   • Win ৳2,000 for 1st place
   • Free entry, skill-based competition
   • Same account, all your data preserved
   
   [Install Bangladesh Version] [Learn More] [Dismiss]
   ```

3. **Deep Link to Google Play**:
   ```kotlin
   // In existing app (piotr_gorczynski.soccer2)
   val playStoreUrl = "https://play.google.com/store/apps/details?id=piotr_gorczynski.soccer2.bd"
   val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
   startActivity(intent)
   ```

**Pros**:
- Direct communication with existing users through in-app UI
- Clear call-to-action
- Preserves user data automatically (shared Firebase backend)
- Users can keep both apps or uninstall the old one
- No forced migration
- Works for all users regardless of account registration status

**Cons**:
- Requires update to existing app to add notification logic
- Users must take action (install new app)
- Some users may ignore the banner
- Only reaches users who actively use the app

**Note**: Push notifications via FCM are not viable since most Bangladesh users don't have registered accounts and thus no FCM tokens available.

---

#### Option 2: Google Play Store Cross-Promotion

**Description**: Use Google Play Store's own features to promote the Bangladesh version.

**Implementation**:

1. **App Description Update** (existing app):
   ```markdown
   🇧🇩 Bangladesh Users: Check out our new Bangladesh-specific version 
   with cash prize tournaments! Search "Gridline Soccer Bangladesh" 
   or visit: [link]
   ```

2. **Developer Profile Cross-Promotion**:
   - Both apps appear under same developer account
   - Users browsing one app see "More by this developer"
   - Add links in "What's New" section

3. **Similar Apps Recommendation**:
   - Google Play algorithm may recommend BD version to users of global version

**Pros**:
- No code changes required to existing app
- Organic discovery through Play Store
- Professional separation of variants

**Cons**:
- Passive discovery only
- Lower conversion rate
- Relies on users actively searching

---

#### Option 3: Gradual Sunset of Global App in Bangladesh

**Description**: Gradually phase out the global app for Bangladesh users while promoting the new version.

**Implementation**:

**Phase 1: Soft Promotion (Months 1-2)**
- Add in-app banners promoting BD version
- Keep global app fully functional

**Phase 2: Feature Gating (Months 3-4)**
- Disable new tournament creation in global app for BD users
- Show message: "Create tournaments in Bangladesh version for cash prizes"
- Existing functionality still works

**Phase 3: Full Migration (Month 5+)**
- Show full-screen migration prompt in global app for BD users
- Require BD users to switch to new app for tournaments
- Maintain read-only access to old app

**Pros**:
- Ensures complete migration over time
- Gives users time to adapt
- Clear migration timeline

**Cons**:
- More complex implementation
- Risk of user frustration
- May violate Play Store policies if too aggressive

---

### Recommended Migration Strategy

**Best Approach: Combination of Option 1 + Option 2**

**Phase 1: Immediate Actions (Week 1-2)**

1. **Launch BD Version on Play Store**
   - Publish `piotr_gorczynski.soccer2.bd`
   - Clear app description highlighting cash prizes
   - Screenshots showing prize tournaments
   - Localized Bengali description

2. **Update Global App** (piotr_gorczynski.soccer2):
   ```kotlin
   // Add to global app codebase
   if (userRegion == "BD" && !hasSeenBDPromo) {
       showBangladeshVersionPromotionDialog()
   }
   ```

**Phase 2: Ongoing Promotion (Week 3-8)**

1. **In-App Banners**:
   - Show banner on main menu for BD users in global app
   - Allow dismissal but show again after 7 days
   - Track banner impressions and clicks

2. **Play Store Optimization**:
   - Add "Bangladesh" to global app keywords
   - Link to BD version in "What's New" section
   - Use custom Play Store listing experiments

3. **Social Media & Community**:
   - Announce on any existing social media channels
   - Encourage users to share in Bangladesh gaming communities
   - Create viral content about prize winners

**Phase 3: Incentivized Migration (Month 2-3)**

1. **First-Mover Advantage**:
   - Offer bonus entry into special tournament for early adopters
   - "Install by [date] to get entry into ৳5,000 inaugural tournament"

2. **Referral Program**:
   - Users who refer friends to BD version get bonus entries
   - Track referrals via Firebase Dynamic Links

3. **Email Campaign** (if you have email addresses):
   - Direct email to Bangladesh users
   - Personalized message about cash prizes

---

### Data Migration & Continuity

#### User Experience During Migration

**Seamless Transition**:

1. **Install Bangladesh Version**:
   ```
   User: [Clicks "Install Bangladesh Version" in global app]
       ↓
   Google Play: [Opens piotr_gorczynski.soccer2.bd listing]
       ↓
   User: [Installs app]
       ↓
   Bangladesh App: [Opens for first time]
   ```

2. **Automatic Data Sync**:
   ```
   Bangladesh App Launch
       ↓
   Firebase Authentication: [Detects existing user via Google account]
       ↓
   Firestore: [Loads user profile with same UID]
       ↓
   User sees:
       ✅ Same username and avatar
       ✅ All friend connections
       ✅ Match history and statistics
       ✅ Preferences and settings
       ↓
   NEW: Prompt for Bangladesh eligibility confirmation (18+, payment account)
       ↓
   User: [Confirms eligibility]
       ↓
   Bangladesh App: [User is now eligible for cash prize tournaments]
   ```

3. **What's Preserved**:
   - User profile (name, avatar, bio)
   - Friend list and pending invitations
   - Match history and win/loss record
   - Player statistics (ELO rating, games played)
   - Tournament participation history (non-cash tournaments)
   - Preferences (language, notifications)

4. **What's New/Different**:
   - Bangladesh eligibility status (requires confirmation)
   - Access to cash prize tournaments (BD only)
   - Payment account information (required for prize winners)

#### Handling Edge Cases

**Scenario 1: User has both apps installed**
- Both apps work independently
- Same user account in both
- User can play regular tournaments in global app
- User can play cash prize tournaments in BD app
- No conflicts, data stays in sync

**Scenario 2: User uninstalls global app**
- No data loss (all data in Firebase)
- Can still access everything in BD app
- Can reinstall global app later if needed

**Scenario 3: User only wants global app**
- Completely fine, no forced migration
- User can dismiss BD promotion banner
- Global functionality unchanged

---

### Migration Success Metrics

**Target Conversion Rates**:
- **Week 1**: 10-15% of 746 users (75-112 installs)
- **Month 1**: 30-40% of users (224-298 installs)
- **Month 3**: 50-60% of users (373-448 installs)
- **Month 6**: 60-70% of users (448-522 installs)

**Tracking Metrics**:

```javascript
// Firebase Analytics Events

// In global app
logEvent("bd_promotion_shown", {
  user_id: userId,
  region: "BD",
  timestamp: Date.now()
});

logEvent("bd_promotion_clicked", {
  user_id: userId,
  destination: "play_store",
  timestamp: Date.now()
});

// In Bangladesh app
logEvent("bd_app_first_launch", {
  user_id: userId,
  migrated_from_global: true, // Check if user exists in Firestore
  timestamp: Date.now()
});

logEvent("bd_eligibility_confirmed", {
  user_id: userId,
  age_confirmed: true,
  payment_method: "bkash",
  timestamp: Date.now()
});
```

**Success Indicators**:
1. **Install Rate**: % of global app BD users who install BD app
2. **Activation Rate**: % of BD app installs who confirm eligibility
3. **Tournament Registration**: % of eligible users who join cash prize tournaments
4. **Retention Rate**: % of migrated users still active after 30 days

---

### Communication Templates

#### In-App Banner (English)

```
🎉 NEW: Gridline Soccer Bangladesh!

Win ৳2,000 cash prizes in skill-based tournaments!

✅ Free entry, no payment required
✅ Same account, all your data preserved
✅ Bi-monthly cash prize tournaments

[Install Now]  [Learn More]  [Maybe Later]
```

#### In-App Banner (Bengali)

```
🎉 নতুন: গ্রিডলাইন সকার বাংলাদেশ!

দক্ষতা-ভিত্তিক টুর্নামেন্টে ৳২,০০০ নগদ পুরস্কার জিতুন!

✅ বিনামূল্যে প্রবেশ, কোন পেমেন্ট প্রয়োজন নেই
✅ একই অ্যাকাউন্ট, আপনার সমস্ত ডেটা সংরক্ষিত
✅ দ্বি-মাসিক নগদ পুরস্কার টুর্নামেন্ট

[এখনই ইনস্টল করুন]  [আরও জানুন]  [পরে হয়তো]
```

#### Push Notification

**Title**: 🏆 Win ৳2,000 in Gridline Soccer Bangladesh!

**Body**: New Bangladesh version with cash prize tournaments. Same account, all data preserved. Install now!

**Action**: Deep link to Play Store

#### Google Play Store Description (Bangladesh Version)

```markdown
# Gridline Soccer Bangladesh - Win Cash Prizes! 🏆

Play the classic paper soccer game and compete in skill-based tournaments 
to win real cash prizes!

## 💰 Cash Prize Tournaments
• Win ৳2,000 BDT for 1st place
• Bi-monthly tournaments (twice per month)
• 100% FREE entry - no payment required
• Prizes funded by developer

## 🎮 Game Features
• Classic paper soccer / Gridline Soccer gameplay
• Skill-based strategy game (no chance/gambling)
• Play against friends or compete in tournaments
• Bengali language support
• Same great game you already know and love

## 📋 Eligibility Requirements
• Must be 18+ years old
• Residents of Bangladesh
• Have a valid bKash, Nagad, or Rocket account
• No entry fees or payments required

## 🔐 Safe & Compliant
• Fully compliant with Bangladesh gaming regulations
• Skill-based competition (not gambling)
• Secure payment processing
• Your data is protected

## 🔄 Existing Users
Already playing Gridline Soccer? Your account works here too!
• Same login credentials
• All your friends and statistics preserved
• Seamless transition to cash prize tournaments

Download now and start competing for real prizes!

---

প্রশ্ন বা সহায়তার জন্য যোগাযোগ করুন: [support email]
```

---

### Technical Implementation Checklist

#### Global App Updates (piotr_gorczynski.soccer2)

- [ ] Add Bangladesh user detection logic
  ```kotlin
  fun isBangladeshUser(): Boolean {
      val locale = Locale.getDefault()
      val playStoreCountry = getPlayStoreCountry() // From Firebase Config
      return locale.country == "BD" || playStoreCountry == "BD"
  }
  ```

- [ ] Create promotion banner UI component
  ```kotlin
  class BangladeshPromotionBanner : Fragment() {
      fun showPromotion() {
          // Show banner with "Install Bangladesh Version" CTA
      }
      
      fun onInstallClicked() {
          openPlayStore("piotr_gorczynski.soccer2.bd")
          logAnalyticsEvent("bd_promotion_clicked")
      }
  }
  ```

- [ ] Implement banner dismissal tracking
  ```kotlin
  SharedPreferences.edit {
      putBoolean("bd_promo_dismissed", true)
      putLong("bd_promo_dismissed_time", System.currentTimeMillis())
  }
  ```

#### Bangladesh App Development (piotr_gorczynski.soccer2.bd)

- [ ] Use shared google-services.json
  ```bash
  # File location: secrets/google-services.{env}.json
  # This file contains client configurations for both package IDs:
  # - piotr_gorczynski.soccer2 (global)
  # - piotr_gorczynski.soccer2.bd (bangladesh)
  ```

- [ ] Detect migrated users on first launch
  ```kotlin
  suspend fun detectMigratedUser(): Boolean {
      val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
      val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
      return userDoc.exists() && userDoc.data?.get("region") != "BD"
  }
  ```

- [ ] Show migration welcome message
  ```kotlin
  if (isMigratedUser) {
      showWelcomeDialog(
          title = "Welcome to Gridline Soccer Bangladesh!",
          message = "All your data has been preserved. Confirm your eligibility to start playing for cash prizes!"
      )
  }
  ```

- [ ] Update user document with BD region
  ```kotlin
  suspend fun updateUserRegion(userId: String) {
      firestore.collection("users").document(userId).update(
          mapOf(
              "region" to "BD",
              "appVariant" to "bangladesh",
              "migrationStatus" to mapOf(
                  "migratedFromGlobal" to true,
                  "migrationDate" to FieldValue.serverTimestamp()
              )
          )
      )
  }
  ```

#### Firebase Backend Configuration

- [ ] Register both package IDs in Firebase Console
  - `piotr_gorczynski.soccer2` (existing)
  - `piotr_gorczynski.soccer2.bd` (new)

- [ ] Update Firestore security rules
  - Add regional tournament access rules
  - Allow cross-app user data access
  - Implement Bangladesh eligibility checks

- [ ] Create Cloud Function for migration tracking
  ```javascript
  exports.onUserMigration = functions.firestore
      .document('users/{userId}')
      .onUpdate(async (change, context) => {
          const before = change.before.data();
          const after = change.after.data();
          
          // Detect migration from global to BD
          if (!before.region && after.region === 'BD') {
              await admin.firestore().collection('analytics').add({
                  event: 'user_migrated_to_bd',
                  userId: context.params.userId,
                  timestamp: admin.firestore.FieldValue.serverTimestamp()
              });
          }
      });
  ```

---

### Cost & Resource Implications

**Additional Costs for Migration**:

1. **Development Time**:
   - Global app update (promotion banner): 8-16 hours
   - Firebase configuration (dual app setup): 4-8 hours
   - Testing migration flow: 8-12 hours
   - **Total: 20-36 hours (~$1,000-$1,800)**

2. **Marketing Costs** (optional):
   - Graphic design for promotional materials: $100-$300
   - Social media advertising (Facebook/Instagram): $200-$500/month
   - Influencer partnerships in Bangladesh: $100-$500
   - **Total: $400-$1,300** (one-time or ongoing)

3. **Incentive Costs** (optional):
   - Inaugural tournament larger prize pool: ৳5,000 (~$45)
   - Referral bonus prizes: ৳10,000-20,000 (~$90-$180)
   - **Total: ~$135-$225**

**Total Migration Investment**: ~$1,535-$3,325

---

### Timeline for Migration

**Week 1-2: Development**
- Update global app with promotion banner
- Configure Bangladesh app in Firebase
- Test cross-app authentication

**Week 3: Launch**
- Publish Bangladesh app to Play Store
- Update global app with promotion banner
- Monitor initial user response

**Week 4-8: Active Promotion**
- Monitor conversion metrics
- Adjust promotion messaging
- Engage with early adopters

**Month 3+: Ongoing Optimization**
- Analyze migration success
- Continue periodic reminders
- Build BD-specific community

---

### Success Criteria

**Minimum Viable Success** (3 months):
- ✅ 30% of existing users (224+ users) install BD version
- ✅ 50% of BD app installs (112+ users) confirm eligibility
- ✅ 25% of eligible users (28+ users) participate in first tournament
- ✅ 90%+ user satisfaction (no major complaints about migration)

**Ideal Success** (6 months):
- ✅ 60% of existing users (448+ users) install BD version
- ✅ 70% of BD app installs (313+ users) confirm eligibility
- ✅ 40% of eligible users (125+ users) participate in tournaments
- ✅ Positive ROI (tournament participation generates engagement/retention value)

---

## Authentication Integration with Google and Facebook

### Overview

This section addresses the authentication integration approach for the Bangladesh version (`piotr_gorczynski.soccer2.bd`) and whether separate authentication instances or configurations are required for Google Sign-In and Facebook Login services.

### Executive Summary

**Answer: You DO NOT need separate authentication instances, but you DO need separate app registrations.**

- **Firebase Authentication**: Same Firebase project, same authentication system, different app registration
- **Google Sign-In**: Automatically configured via Firebase, no additional setup required
- **Facebook Login**: Requires separate Facebook app registration or adding new package ID to existing app

The Bangladesh APK will share the same user authentication database with the global version, enabling seamless user migration while maintaining separate app identities on Google Play Store.

---

### Firebase Authentication Configuration

#### Single Firebase Project, Multiple Apps

**Recommended Architecture:**
```
Firebase Project: gridline-soccer (existing)
├── Authentication (shared across all apps)
│   ├── Sign-in Methods:
│   │   ├── Email/Password ✓
│   │   ├── Google ✓
│   │   ├── Facebook ✓
│   │   ├── Microsoft ✓
│   │   └── Anonymous ✓
│   └── User Database (shared)
│
├── App 1: piotr_gorczynski.soccer2 (Global)
│   └── Android Package Name: piotr_gorczynski.soccer2
│
└── App 2: piotr_gorczynski.soccer2.bd (Bangladesh)
    └── Android Package Name: piotr_gorczynski.soccer2.bd

google-services.json (shared configuration)
└── Contains client entries for both package names
```

#### Why Same Firebase Project?

**Benefits:**
1. **Shared User Database**: Users can sign in with same credentials across both apps
2. **Unified Authentication**: Single authentication system, one set of security rules
3. **No Additional Cost**: Firebase charges by usage, not by number of registered apps
4. **Seamless Migration**: Users automatically authenticated when switching apps
5. **Centralized Management**: Manage all users in one Firebase console

**No Drawbacks**: There are no significant disadvantages to using the same Firebase project for both app variants.

---

### Google Sign-In Configuration

#### Current Status

The global app currently uses Google Sign-In via Firebase Authentication with the following configuration:

**Dependencies (mobile/app/build.gradle):**
```gradle
implementation 'com.google.firebase:firebase-auth:24.0.1'
implementation 'com.google.android.gms:play-services-auth:21.4.0'
```

#### Bangladesh Version Configuration

**Do you need separate Google API credentials?**

**Answer: No, but you need to register the new package ID.**

**Steps Required:**

1. **Register Bangladesh App in Firebase Console**
   ```
   Firebase Console → Project Settings → Your apps → Add app → Android
   
   Package name: piotr_gorczynski.soccer2.bd
   App nickname: Gridline Soccer Bangladesh
   SHA-1 certificate fingerprint: [Your release keystore SHA-1]
   ```

2. **Download google-services.json**
   ```
   After registering both apps, download the google-services.json file
   
   File location: secrets/google-services.{env}.json
   Note: This single file contains client configurations for both package names
   ```

3. **Google Sign-In Automatic Configuration**
   ```
   Google Sign-In OAuth client IDs are automatically created by Firebase
   when you register the Android app with your package ID and SHA-1 fingerprint.
   
   No manual Google Cloud Console configuration needed!
   ```

4. **Verify OAuth Client in Google Cloud Console** (Optional)
   ```
   Google Cloud Console → APIs & Services → Credentials
   
   You should see two Android OAuth clients:
   - piotr_gorczynski.soccer2 (existing)
   - piotr_gorczynski.soccer2.bd (new)
   
   Both automatically configured with package names and SHA-1 fingerprints
   ```

#### Implementation Changes Required

**Answer: Zero code changes needed for Google Sign-In!**

The existing Google Sign-In implementation will work automatically with the Bangladesh version because:
- Firebase SDK reads the appropriate package name from the client configurations in `google-services.json`
- OAuth credentials are automatically matched by Firebase based on package name
- Same authentication flow works for both apps

**Gradle Configuration:**
```gradle
// mobile/app/build.gradle

android {
    flavorDimensions "environment", "market"
    productFlavors {
        global {
            dimension "market"
            // Base applicationId: piotr_gorczynski.soccer2
        }
        bangladesh {
            dimension "market"
            applicationIdSuffix ".bd"
            // Final applicationId: piotr_gorczynski.soccer2.bd
        }
    }
}
// Both variants use the same google-services.json file
// Firebase automatically selects the correct client configuration based on package name
}
```

The Google Services Gradle plugin uses the single `google-services.json` file, and Firebase automatically selects the correct client configuration based on the application's package name at runtime.

---

### Facebook Login Configuration

#### Current Status

The global app currently integrates Facebook Login SDK with the following configuration:

**Dependencies (mobile/app/build.gradle):**
```gradle
implementation 'com.facebook.android:facebook-android-sdk:18.1.3'
```

**AndroidManifest.xml:**
```xml
<meta-data
    android:name="com.facebook.sdk.ApplicationId"
    android:value="@string/facebook_app_id" />
<meta-data
    android:name="com.facebook.sdk.ClientToken"
    android:value="@string/facebook_client_token" />
```

**Current Facebook App ID:** `1232966491486195`

#### Bangladesh Version Configuration

**Do you need a separate Facebook app?**

**Answer: No, but you have two options:**

##### Option 1: Add Bangladesh Package ID to Existing Facebook App (Recommended)

**Steps:**

1. **Add Android Platform Configuration**
   ```
   Facebook App Dashboard → Settings → Basic → Add Platform → Android
   
   OR if Android platform exists:
   Facebook App Dashboard → Settings → Basic → Android section
   ```

2. **Add Bangladesh Package Name**
   ```
   Google Play Package Name: piotr_gorczynski.soccer2.bd
   Class Name: piotr_gorczynski.soccer2.UniversalLoginActivity
   ```

3. **Generate and Add Key Hashes**
   ```bash
   # In mobile directory, run:
   ./gradlew generateFacebookKeyHashes
   
   # This generates key hashes for both debug and release builds
   # Add ALL generated hashes to Facebook App Dashboard
   ```

4. **Facebook Configuration (values/strings.xml)**
   ```xml
   <!-- No changes needed, use same App ID and Client Token -->
   <string name="facebook_app_id" translatable="false">1232966491486195</string>
   <string name="facebook_client_token" translatable="false">YOUR_CLIENT_TOKEN</string>
   ```

**Advantages:**
- ✅ Single Facebook app to manage
- ✅ Same App ID and Client Token for both variants
- ✅ Easier maintenance and monitoring
- ✅ No additional Facebook app review required
- ✅ Users can link same Facebook account across both apps

**Disadvantages:**
- ⚠️ Both apps share same Facebook app settings
- ⚠️ Cannot have different Facebook branding per variant

##### Option 2: Create Separate Facebook App for Bangladesh

**Steps:**

1. **Create New Facebook App**
   ```
   Facebook Developers → My Apps → Create App
   App Type: Consumer
   App Name: Gridline Soccer Bangladesh
   ```

2. **Configure Android Platform**
   ```
   Package Name: piotr_gorczynski.soccer2.bd
   Add key hashes for both debug and release keystores
   ```

3. **Create Bangladesh-Specific Configuration**
   ```
   File: mobile/app/src/bangladesh/res/values/strings.xml
   
   <resources>
       <string name="facebook_app_id" translatable="false">NEW_BD_APP_ID</string>
       <string name="facebook_client_token" translatable="false">NEW_BD_CLIENT_TOKEN</string>
   </resources>
   ```

4. **Submit for Facebook App Review**
   ```
   Required permissions:
   - public_profile (default, no review needed)
   - email (default, no review needed)
   ```

**Advantages:**
- ✅ Independent Facebook app settings per variant
- ✅ Can customize Facebook branding for Bangladesh
- ✅ Separate analytics and monitoring

**Disadvantages:**
- ❌ Requires managing two Facebook apps
- ❌ Potential app review required
- ❌ More complex configuration maintenance
- ❌ Users cannot link same Facebook account across apps (different app scopes)

#### Recommended Approach: Option 1

**Use the existing Facebook app and add the Bangladesh package ID.** This is simpler, requires less maintenance, and provides a better user experience for migration.

---

### Implementation Checklist

#### Phase 1: Firebase Configuration

- [x] **Register Bangladesh App in Firebase Console** (AUTOMATED)
  - **Note**: Firebase Android apps are automatically created by the `gcp/cloud-build/deploy_firebase.yaml` Cloud Build script
  - The script creates apps for both package names:
    - `piotr_gorczynski.soccer2` (global variant)
    - `piotr_gorczynski.soccer2.bd` (Bangladesh variant)
  - The script checks if apps already exist before creating them to avoid duplicates
  - To run the script: Execute the Cloud Build deployment which includes this step

- [ ] **Download Configuration File** (AUTOMATED)
  - **Note**: A single configuration file containing both app configurations is automatically downloaded by the `gcp/cloud-build/download_google_services.yaml` Cloud Build script
  - The script downloads one `google-services.json` file per environment that includes configurations for all registered Android apps:
    - The file contains client entries for both `piotr_gorczynski.soccer2` and `piotr_gorczynski.soccer2.bd`
  - Naming convention: `google-services.{env}.json` (e.g., `google-services.dev.json`, `google-services.prod.json`)
  - The file is automatically committed to the private repository's secrets directory
  - **Manual Alternative**: You can also download the `google-services.json` file manually from Firebase Console if needed, but the automated approach is recommended for consistency

- [ ] **Place Configuration Files**
  ```
  secrets/
  ├── google-services.dev.json (contains both global and Bangladesh configs)
  ├── google-services.test.json (contains both global and Bangladesh configs)
  └── google-services.prod.json (contains both global and Bangladesh configs)
  ```
  **Note**: Each file contains client configurations for all package names. The build system uses the same file for both global and Bangladesh variants.

- [ ] **Add SHA-1 Fingerprints in Firebase Console**
  - Note the SHA-1 fingerprint from your release keystore
  - Add to Firebase Console for both apps to enable Google Sign-In

- [ ] **Verify Firebase Auth Methods Enabled**
  - Email/Password: ✓
  - Google: ✓
  - Facebook: ✓
  - Microsoft: ✓ (if used)
  - Anonymous: ✓

#### Phase 2: Google Sign-In Setup

- [ ] **Verify OAuth Client Created**
  ```
  Firebase automatically creates Android OAuth client when you:
  - Register the app with package ID
  - Provide SHA-1 certificate fingerprint
  ```

- [ ] **Test Google Sign-In**
  ```bash
  # Build Bangladesh variant
  ./gradlew assembleBangladeshDebug
  
  # Install and test Google Sign-In flow
  # Should work identically to global version
  ```

**No code changes required for Google Sign-In!**

#### Phase 3: Facebook Login Setup

##### If Using Option 1 (Recommended): Add to Existing Facebook App

- [ ] **Add Bangladesh Package ID to Facebook App**
  - Go to [Facebook App Dashboard](https://developers.facebook.com/apps/1232966491486195)
  - Settings → Basic → Android platform
  - Add package name: `piotr_gorczynski.soccer2.bd`

- [ ] **Generate Key Hashes**
  ```bash
  cd mobile
  ./gradlew generateFacebookKeyHashes
  ```

- [ ] **Add All Key Hashes to Facebook**
  - Copy both debug and release key hashes
  - Add to Facebook App Dashboard → Settings → Basic → Key Hashes
  - Add hashes for both `piotr_gorczynski.soccer2` and `piotr_gorczynski.soccer2.bd`

- [ ] **Update AndroidManifest for Bangladesh Flavor (COMPLETED)**
  
  **File**: `mobile/app/src/bangladesh/AndroidManifest.xml`
  
  ✅ **Already implemented** - The Bangladesh-specific manifest has been created to prevent Facebook Content Provider conflicts:
  
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <manifest xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:tools="http://schemas.android.com/tools">

      <application>
          <!-- Override Facebook Content Provider authority for Bangladesh variant -->
          <!-- This prevents conflict when both global and Bangladesh apps are installed -->
          <provider
              android:name="com.facebook.FacebookContentProvider"
              android:authorities="com.facebook.app.FacebookContentProvider1232966491486195.bd"
              android:exported="true"
              tools:replace="android:authorities" />
      </application>

  </manifest>
  ```
  
  **Why this is needed**: When both the global app (`piotr_gorczynski.soccer2`) and Bangladesh app (`piotr_gorczynski.soccer2.bd`) are installed on the same device, they cannot share the same Facebook Content Provider authority. The Bangladesh variant uses a unique authority with `.bd` suffix to prevent the `INSTALL_FAILED_CONFLICTING_PROVIDER` error.
  
  **No action required** - This fix has already been implemented and is ready to use.

- [ ] **Test Facebook Login**
  ```bash
  # Build and test both debug and release builds
  ./gradlew assembleBangladeshDebug
  ./gradlew assembleBangladeshRelease
  
  # Verify Facebook login works in both builds
  ```

##### If Using Option 2: Create Separate Facebook App

- [ ] **Create New Facebook App**
  - App name: "Gridline Soccer Bangladesh"
  - App type: Consumer

- [ ] **Configure Android Platform**
  - Package name: `piotr_gorczynski.soccer2.bd`
  - Class name: `piotr_gorczynski.soccer2.UniversalLoginActivity`

- [ ] **Create Bangladesh-Specific Strings**
  ```
  File: mobile/app/src/bangladesh/res/values/strings.xml
  
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string name="facebook_app_id" translatable="false">NEW_BD_APP_ID</string>
      <string name="facebook_client_token" translatable="false">NEW_BD_CLIENT_TOKEN</string>
  </resources>
  ```

- [ ] **Update AndroidManifest for Bangladesh Flavor**
  ```
  File: mobile/app/src/bangladesh/AndroidManifest.xml
  
  <manifest xmlns:android="http://schemas.android.com/apk/res/android">
      <application>
          <provider
              android:name="com.facebook.FacebookContentProvider"
              android:authorities="com.facebook.app.FacebookContentProviderNEW_BD_APP_ID"
              android:exported="true" />
      </application>
  </manifest>
  ```

- [ ] **Add Key Hashes and Test**

#### Phase 4: Testing & Validation

- [ ] **Test Authentication Flows**
  - [ ] Email/Password login works in Bangladesh app
  - [ ] Google Sign-In works in Bangladesh app
  - [ ] Facebook Login works in Bangladesh app
  - [ ] Anonymous authentication works in Bangladesh app

- [ ] **Test User Migration**
  - [ ] User logs in with Google on global app
  - [ ] User installs Bangladesh app
  - [ ] User logs in with same Google account on Bangladesh app
  - [ ] Verify user data syncs (same Firebase UID, same Firestore documents)

- [ ] **Test Cross-App Authentication**
  - [ ] Verify same user can be authenticated in both apps simultaneously
  - [ ] Confirm Firestore security rules allow cross-app access
  - [ ] Test friend connections work across apps

- [ ] **Test Build Variants**
  - [ ] `bangladeshDebug` build with Google Sign-In
  - [ ] `bangladeshDebug` build with Facebook Login
  - [ ] `bangladeshRelease` build with Google Sign-In
  - [ ] `bangladeshRelease` build with Facebook Login

---

### Configuration Summary

#### What You Need

| Service | Separate Instance? | Configuration Required |
|---------|-------------------|------------------------|
| **Firebase Project** | ❌ No | Register Bangladesh app, download `google-services.json` |
| **Firebase Authentication** | ❌ No | No changes, automatically shared |
| **Google Sign-In** | ❌ No | Automatic via Firebase app registration |
| **Google OAuth Client** | ✅ Yes (auto-created) | Automatic when registering app with SHA-1 |
| **Facebook App** | ⚠️ Optional | Option 1: Add package ID to existing app (recommended)<br>Option 2: Create separate app |
| **Facebook App ID** | ⚠️ Optional | Same if Option 1, different if Option 2 |

#### What Stays the Same

- ✅ Firebase project ID
- ✅ Firebase Authentication database
- ✅ User UIDs and authentication tokens
- ✅ Firestore database and security rules
- ✅ Firebase Authentication sign-in methods configuration
- ✅ Code for handling authentication (no changes needed)

#### What's Different

- 📦 Package ID: `piotr_gorczynski.soccer2.bd` (vs `piotr_gorczynski.soccer2`)
- 📄 Configuration file: `google-services.json` (Bangladesh-specific)
- 🔑 Google OAuth client: Separate Android client (auto-created by Firebase)
- 🔐 Key hashes: Additional Facebook key hashes for Bangladesh package
- 🏪 Play Store listing: Completely separate app on Google Play Store

---

### Security Considerations

#### Firebase Security Rules

**Critical**: Ensure Firestore security rules allow cross-app access for the same user:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users can access their own data from any app variant
    match /users/{userId} {
      allow read, write: if request.auth != null && 
                           request.auth.uid == userId;
      // No restriction based on app package name
    }
    
    // Friends can be accessed from any app variant
    match /friendships/{friendshipId} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null;
    }
    
    // Matches accessible from any app variant
    match /matches/{matchId} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null && 
                               (request.auth.uid == resource.data.player1Id || 
                                request.auth.uid == resource.data.player2Id);
    }
  }
}
```

**Important**: Do NOT add package name restrictions to security rules, as this would prevent cross-app access.

#### Key Hash Security

**Best Practice:**
- Generate separate key hashes for debug and release builds
- Add ALL key hashes to Facebook app (both global and Bangladesh)
- Never commit keystores or signing credentials to version control
- Store keystore passwords in `secrets/keystore.properties` (gitignored)

#### Google OAuth Security

**Automatic Security:**
- Google uses package name + SHA-1 fingerprint for app verification
- Each OAuth client is automatically scoped to its package name
- No manual security configuration needed
- Firebase handles token validation automatically

---

### Cost Implications

#### Firebase Costs

**No additional cost for authentication:**
- Firebase Authentication is free for unlimited users
- Adding a second app to same project: $0
- OAuth client creation: $0 (automatic via Firebase)

**Potential increase in usage-based costs:**
- Firestore reads/writes: May increase if Bangladesh app gains traction
- Cloud Functions invocations: Proportional to user activity
- Cloud Storage: Only if file uploads increase

**Estimated Impact:** 
- If Bangladesh app reaches 500 active users: +10-20% Firebase costs (~$2-$10/month)
- Still well within Firebase free tier initially

#### Facebook Costs

**Free for standard authentication:**
- Facebook Login SDK: Free
- Basic permissions (public_profile, email): Free
- No per-user charges

**Potential costs:**
- Advanced permissions requiring app review: Free but requires time investment
- Facebook Analytics: Free

**Cost: $0**

#### Google Cloud Costs

**OAuth client creation: Free**
- Android OAuth clients: Free (unlimited)
- No per-authentication charges
- Included with Firebase

**Cost: $0**

#### Developer Time Investment

**Option 1 (Recommended - Same Facebook App):**
- Firebase setup: 2-3 hours
- Facebook key hash generation and configuration: 1-2 hours
- Testing: 2-3 hours
- **Total: 5-8 hours (~$250-$400)**

**Option 2 (Separate Facebook App):**
- Firebase setup: 2-3 hours
- Create new Facebook app: 1-2 hours
- Configure Facebook for Bangladesh: 1-2 hours
- Create flavor-specific resources: 1-2 hours
- Testing: 3-4 hours
- **Total: 8-13 hours (~$400-$650)**

**Recommended**: Use Option 1 to save 3-5 hours of development time.

---

### Common Questions & Troubleshooting

#### Q: Will users need to log in again on the Bangladesh app?

**A:** No, if they use the same authentication method (Google, Facebook, email/password). Firebase recognizes the same user across both apps because they share the same Firebase project and authentication database.

**Example:**
1. User logs in with Google on global app → User ID: `abc123`
2. User installs Bangladesh app
3. User clicks "Sign in with Google" on Bangladesh app → Same User ID: `abc123`
4. All user data automatically synced (same Firestore user document)

#### Q: Can a user be signed in to both apps at the same time?

**A:** Yes! Both apps can have active sessions simultaneously because they use the same Firebase Authentication. The user will see their same profile, friends, and data in both apps.

#### Q: What happens if I use different Firebase projects?

**A:** **Not recommended.** Different Firebase projects mean:
- ❌ Different user databases → Users must create separate accounts
- ❌ Different UIDs → Cannot share data between apps
- ❌ Complex migration → Must manually copy user data
- ❌ Double management overhead → Manage two separate backends

**Always use the same Firebase project for both app variants.**

#### Q: How do I test that authentication works correctly?

**A:** Follow this test plan:

```bash
# 1. Build Bangladesh debug variant
./gradlew assembleBangladeshDebug

# 2. Install on test device
adb install mobile/app/build/outputs/apk/bangladesh/debug/app-bangladesh-debug.apk

# 3. Test each authentication method:
# - Email/Password: Create account, sign in, sign out
# - Google Sign-In: Click Google button, select account, verify sign-in
# - Facebook Login: Click Facebook button, authorize, verify sign-in
# - Anonymous: Click anonymous button, verify sign-in

# 4. Verify user data appears (if user exists from global app)

# 5. Build release variant and repeat
./gradlew assembleBangladeshRelease
```

#### Q: What if Google Sign-In fails with "Developer Error"?

**A:** This usually means the OAuth client configuration is incorrect.

**Solution:**
1. Verify you registered the Bangladesh app in Firebase Console
2. Ensure you provided the correct SHA-1 fingerprint for your release keystore
3. Check that `google-services.json` is in the correct location: `mobile/app/src/bangladesh/`
4. Wait 5-10 minutes after registration for OAuth client propagation
5. Try signing in with a Google account not previously used for testing

**Debug command:**
```bash
# Verify SHA-1 fingerprint of your keystore
keytool -list -v -keystore path/to/your/keystore.jks -alias your_key_alias
```

#### Q: What if Facebook Login fails with "Invalid key hash" error?

**A:** This means the key hash you're using doesn't match what's registered in Facebook.

**Solution:**
```bash
# 1. Generate current key hash
cd mobile
./gradlew generateFacebookKeyHashes

# 2. Copy ALL generated key hashes

# 3. Add to Facebook App Dashboard
#    Settings → Basic → Key Hashes
#    Paste all hashes (one per line)

# 4. Save and retry Facebook login after 2-3 minutes
```

**Common mistake:** Forgetting to add key hashes for BOTH debug and release keystores.

#### Q: Can I migrate from separate Facebook app back to shared?

**A:** Yes, but requires users to re-authenticate with Facebook.

**Migration steps:**
1. Remove Bangladesh-specific Facebook app configuration
2. Add Bangladesh package ID to existing Facebook app
3. Update `strings.xml` to use original Facebook App ID
4. Users will need to re-authorize Facebook login (one-time inconvenience)

---

### Recommended Configuration Strategy

**For optimal user experience and minimal maintenance:**

1. ✅ **Use same Firebase project** for both app variants
2. ✅ **Register both package IDs** in Firebase Console
3. ✅ **Use existing Facebook app** and add Bangladesh package ID (Option 1)
4. ✅ **Share authentication configuration** (same App IDs, different `google-services.json`)
5. ✅ **Test thoroughly** with both debug and release builds

**This approach provides:**
- Seamless user migration between apps
- Minimal configuration overhead
- Single authentication system to manage
- Shared user database and consistent experience
- Lowest development and maintenance cost

---

## Implementation Roadmap

### Phase 1: Planning & Setup (Week 1-2)
- [x] Game assumptions validated with ChatGPT legal consultation
- [ ] Consider additional legal review with Bangladesh legal expert (optional for regulatory certainty)
- [ ] Register business entity in Bangladesh (if required)
- [ ] **Set up international money transfer service accounts**:
  - [ ] **Remitly** (PRIMARY - recommended for mobile wallet transfers to bKash/Nagad)
  - [ ] **Wise** (SECONDARY - for bank transfers if needed, verify mobile wallet support)
  - [ ] **Western Union** (BACKUP - for urgent or failed transfers)
- [ ] **Test small transfer to Bangladesh mobile wallet** (MANDATORY):
  - [ ] Test Remitly Economy transfer of ৳500 to bKash or Nagad
  - [ ] Verify delivery time and fees
  - [ ] Document the exact process for future reference
  - [ ] If Wise supports mobile wallets from Poland, test that as well
- [ ] ~~Set up personal bKash, Nagad, and/or Rocket accounts for manual prize distribution~~ ❌ NOT POSSIBLE from Poland
- [x] Define detailed prize structure (see [PRIZE_STRATEGY.md](PRIZE_STRATEGY.md))
- [x] Create product flavor for Bangladesh variant
- [ ] **Migration Planning**:
  - [x] Define user migration strategy and communication plan (see [USER_MIGRATION_STRATEGY.md](USER_MIGRATION_STRATEGY.md))
  - [ ] Prepare promotional materials (banners, notifications, Play Store assets)
  - [ ] Design Firebase dual-app configuration (shared authentication)

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
- [ ] **Migration Backend Setup**:
  - [ ] Register both package IDs in Firebase Console (`piotr_gorczynski.soccer2` and `.bd`)
  - [ ] Configure separate `google-services.json` files for each flavor
  - [ ] Update Firestore security rules for cross-app data access
  - [ ] Create Cloud Function for tracking user migrations
  - [ ] Extend user schema with migration tracking fields
- [ ] **Authentication Integration Setup**:
  - [ ] Register Bangladesh app in Firebase Console with package ID `piotr_gorczynski.soccer2.bd`
  - [ ] Provide SHA-1 fingerprint from release keystore for Google Sign-In
  - [ ] Download Bangladesh-specific `google-services.json`
  - [ ] Place `google-services.json` in `mobile/app/src/bangladesh/` directory
  - [ ] Verify Firebase Authentication methods enabled (Email, Google, Facebook, Microsoft, Anonymous)
  - [ ] Add Bangladesh package ID to existing Facebook app (Option 1 - Recommended)
    - OR create new Facebook app for Bangladesh (Option 2)
  - [ ] Generate Facebook key hashes for both debug and release keystores
  - [ ] Add all key hashes to Facebook App Dashboard
  - [ ] Verify Firestore security rules allow cross-app user data access (no package restrictions)

### Phase 3: Mobile App Development (Week 5-7)
- [x] Create Bangladesh product flavor
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
- [ ] **Migration UI Development**:
  - [ ] Add Bangladesh user detection in global app
  - [ ] Create promotion banner component for global app
  - [ ] Implement Play Store deep linking from global to BD app
  - [ ] Add banner dismissal and tracking logic
  - [ ] Create migrated user welcome flow in BD app
  - [ ] Implement auto-detection of existing users in BD app
  - [ ] Add Firebase Analytics events for migration tracking

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
- [x] Legal compliance verification
  - Game assumptions validated with ChatGPT legal consultation
  - Terms of Service update
  - Privacy Policy update
- [ ] Closed beta testing with Bangladesh users
- [ ] **Migration Testing**:
  - [ ] Test cross-app authentication (same user in both apps)
  - [ ] Verify data sync between global and BD apps
  - [ ] Test promotion banner in global app
  - [ ] Verify Play Store deep linking
  - [ ] Test migrated user welcome flow
  - [ ] Validate Firebase Analytics tracking
- [ ] **Authentication Testing**:
  - [ ] Test Email/Password authentication in Bangladesh app
  - [ ] Test Google Sign-In in Bangladesh debug build
  - [ ] Test Google Sign-In in Bangladesh release build
  - [ ] Test Facebook Login in Bangladesh debug build
  - [ ] Test Facebook Login in Bangladesh release build
  - [ ] Test Anonymous authentication in Bangladesh app
  - [ ] Verify same user can authenticate in both global and Bangladesh apps
  - [ ] Confirm user data syncs correctly (same UID, same Firestore documents)
  - [ ] Test friend connections work across apps
  - [ ] Verify authentication with existing global app users

### Phase 6: Launch Preparation (Week 11-12)
- [ ] Create Google Play Store listing (Bangladesh)
- [ ] Prepare marketing materials
- [ ] Set up customer support (Bengali language support)
- [ ] Document manual payment procedures
- [ ] Establish prize fund reserve (৳4,000/month for bi-monthly tournaments)
- [ ] Create operational runbook
- [ ] **Migration Campaign Preparation**:
  - [ ] Finalize promotional banner designs (English + Bengali)
  - [ ] Prepare social media announcements
  - [ ] Create migration FAQ and support documentation
  - [ ] Design Play Store listing with clear migration benefits

### Phase 7: Soft Launch (Week 13-14)
- [ ] Limited release to 100-500 users
- [ ] Run first bi-monthly tournament
- [ ] Process first manual prize payment
- [ ] Gather user feedback
- [ ] Fix critical issues
- [ ] **Initial Migration Campaign**:
  - [ ] Deploy updated global app with promotion banner
  - [ ] Monitor installation metrics (target: 75-112 installs in Week 1)
  - [ ] Track banner impressions and click-through rates
  - [ ] Respond to user questions about migration
  - [ ] Adjust messaging based on early feedback
  - [ ] Update Play Store listing based on user feedback

### Phase 8: Full Launch & Ongoing Migration (Week 15+)
> ⚠️ **REMINDER**: Before or at launch, release a new global app version with the promo flag set to **TRUE** so the promotion banner is displayed to global users (see #1240).

- [ ] Public launch in Bangladesh Google Play Store
- [ ] Marketing campaign
- [ ] Establish bi-monthly tournament schedule
- [ ] Monitor KPIs (participation, payment success, user satisfaction)
- [ ] Iterate based on feedback
- [ ] **Ongoing Migration Activities**:
  - [ ] Monitor migration conversion rates (target: 30% Month 1, 60% Month 6)
  - [ ] A/B test different promotion messages in banner
  - [ ] Engage with user community in Bangladesh
  - [ ] Share success stories from prize winners
  - [ ] Periodic promotion banner refresh in global app
  - [ ] Track migration success metrics and adjust strategy
  - [ ] Continue Play Store optimization and keyword updates

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

### Migration-Specific Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Low user migration rate (<30%) | High | Medium | Multi-channel promotion, incentives, clear value proposition |
| User confusion about two apps | Medium | Medium | Clear messaging, FAQ, in-app explanations |
| Data sync issues between apps | High | Low | Thorough testing, shared Firebase backend, monitoring |
| Firebase dual-app configuration errors | Medium | Low | Careful setup, testing with test accounts first |
| Existing users angry about migration | Medium | Low | Optional migration, maintain global app, clear communication |
| Play Store policy violation | High | Low | Review Play Store policies, avoid aggressive migration tactics |

---

## Cost Estimation

### Initial Setup Costs
- Legal consultation: $500 - $1,000 (simplified approach requires less legal review)
- Development time: 150-200 hours (estimated at market rate) - significantly reduced due to:
  - No payment gateway API integration
  - No document upload/verification system
  - Simplified user flow
- **Migration development**: 20-36 hours (~$1,000-$1,800)
  - Global app promotion banner
  - Firebase dual-app configuration
  - Migration tracking and analytics
- **Authentication integration setup**: 5-8 hours (~$250-$400)
  - Firebase app registration for Bangladesh variant
  - Facebook key hash generation and configuration
  - Testing authentication flows across both apps
  - (Note: Using Option 1 - same Facebook app. Option 2 would add 3-5 hours)
- **Marketing & promotion**: $400 - $1,300 (optional)
  - Promotional materials design
  - Social media advertising
  - Influencer partnerships
- **Migration incentives**: ~$135 - $225 (optional)
  - Inaugural tournament bonus prizes
  - Referral rewards
- **Total Initial: ~$9,285 - $15,725** (including migration and authentication setup costs)

### Monthly Operational Costs
- Firebase costs (increased usage): $20 - $50/month (minimal increase)
- Prize pool funding:
  - 2 bi-monthly tournaments × ৳2,000 = ৳4,000/month (approximately $36/month)
  - International transfer fees: 
    - Using Remitly Economy (recommended): 2 transfers × $0.50-$1.50 = $1.00-$3.00/month
    - Using Wise (if bank transfers): 2 transfers × $0.20-$0.40 = $0.40-$0.80/month
  - **Total prizes + fees: approximately $37.00-$39.00/month** (with Remitly) or $36.40-$36.80/month (with Wise)
- Manual payment processing time: 1-2 hours/month (developer time, not monetized)
- Customer support: $50 - $100/month (minimal support needed)
- **Total Monthly: approximately $107-$189** (using Remitly) or $106.40-$186.80 (using Wise)

**Breakdown**:
- Minimum: $20 (Firebase) + $36.40 (prizes) + $50 (support) = $106.40
- Maximum: $50 (Firebase) + $36.80 (prizes) + $100 (support) = $186.80

### Cost Savings vs. Original Approach
- **No payment gateway API fees**: Saved ~$500-1,000 setup + 2-3% transaction fees
- **No document storage costs**: Saved ~$20-50/month Cloud Storage
- **No verification review costs**: Saved manual review time or third-party service fees
- **Simpler development**: Saved ~150-200 development hours (~$7,500-$10,000)
- **Lower prize pool**: Saved ~$594/month in prize funding

### Annual Cost Projection (Year 1)
- Initial setup: $9,285 - $15,725 (including migration and authentication setup costs)
- Monthly operational: $107 - $187 × 12 = $1,284 - $2,244
- **Total Year 1: approximately $10,569 - $17,969**

### Migration ROI Analysis
**Investment**: ~$1,535 - $3,325 in migration-specific costs
**Potential Return**:
- Higher user retention (engaged users stay longer)
- Stronger community in Bangladesh market
- Potential future revenue from engaged user base
- Brand presence in emerging market

**Break-even Scenario**:
- If 224 users migrate (30% conversion), cost per acquired user: ~$6.86-$14.84
- If 448 users migrate (60% conversion), cost per acquired user: ~$3.43-$7.42
- Comparable to typical mobile app user acquisition costs ($5-$15 per user)

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
- [x] Game assumptions validated with ChatGPT legal consultation on gaming/promotions regulations
- [x] Verified skill-based classification is valid and that **no consideration** is required
- [x] Confirmed simplified age verification approach aligns with legal frameworks
- [ ] Confirm whether any prize withholding/tax reporting obligations apply (optional local counsel review)
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
- [ ] Confirm remittance provider rules for payout descriptions/purpose codes (if required)
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

#### Migration Compliance
- [ ] Verify Google Play Store policies allow cross-promotion between apps
- [ ] Ensure migration messaging is not misleading or deceptive
- [ ] Privacy policy addresses data sharing between app variants
- [ ] User consent for migration tracking analytics
- [ ] Clear communication that migration is optional
- [ ] Respect user choice to stay on global app
- [ ] No degradation of global app experience for Bangladesh users
- [ ] Transparent about separate app installations (not an update)

#### Authentication Integration Compliance
- [ ] Register both Android apps in Firebase Console (global and Bangladesh)
- [ ] Download and configure separate `google-services.json` files for each variant
- [ ] Verify Firebase Authentication methods are enabled for both apps
- [ ] Add Bangladesh package ID to Facebook app settings (Option 1 recommended)
- [ ] Generate and add Facebook key hashes for both debug and release keystores
- [ ] Ensure Firestore security rules allow cross-app user data access
- [ ] Test authentication works in both global and Bangladesh apps
- [ ] Verify same user can authenticate in both apps simultaneously
- [ ] Confirm OAuth client auto-created for Bangladesh app in Google Cloud Console
- [ ] Privacy policy mentions shared authentication across app variants

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
- Have confirmed they possess a valid bKash, Nagad, Rocket account, PayPal account, or Bangladesh bank account
- Have accepted the tournament terms and conditions

Entry: Participation in cash prize tournaments is completely free. No payment, 
purchase, or entry fee is required.

Skill-Based: All tournaments are based purely on player skill. The game mechanics 
involve strategic decision-making, tactical planning, and execution. There is no 
element of chance in determining match outcomes.

Prizes: Cash prizes are awarded to 1st place winners only. Prize amount is ৳2,000 BDT 
per bi-monthly tournament. All prizes are funded by the game developer. Winners will 
be contacted to provide payment details (full name and mobile wallet number or bank 
account) and will receive payment within 7 business days of tournament completion 
via international money transfer service (Wise, Western Union, or Remitly) to their 
bKash, Nagad, Rocket, PayPal, or bank account (winner's choice).

Verification: The developer reserves the right to verify winner identity and 
eligibility before distributing prizes. False declarations regarding age or payment 
account ownership may result in disqualification, prize forfeiture, and account 
suspension.

Payment Processing: Prizes are processed manually by the developer using international 
money transfer services. Winners must provide accurate payment details including full 
legal name and mobile wallet number or bank account information. The developer is not 
responsible for delays caused by incorrect details. Transfer fees are covered by the 
developer.
```

### Appendix C: Step-by-Step Payment Processing Guide (for Developer)

**When a Winner is Determined:**

**Step 1: Receive Winner Information from App**
- Check Firestore `tournaments/{tournamentId}/results` for 1st place winner
- Check `payments/{paymentId}` collection for payment record
- Winner's submitted information should include:
  - Full legal name
  - Mobile wallet number (bKash/Nagad/Rocket) OR PayPal email OR bank details
  - Phone number
  - Preferred payment method

**Step 2: Choose Transfer Service and Log In**

**Option A: Remitly (RECOMMENDED for mobile wallets)**
- Go to https://www.remitly.com and log in
- Click "Send Money" button

**Option B: Wise (if mobile wallets not supported, use for bank transfers)**
- Go to https://wise.com and log in
- Click "Send money" button

**Option C: Western Union (backup for urgent transfers)**
- Go to https://www.westernunion.com/pl/en/ and log in
- Click "Send money online"

**Step 3: Configure Transfer**

**If using Remitly:**
- **Send from**: Poland (PLN or EUR)
- **Send to**: Bangladesh
- **Amount to send**: Calculate equivalent of ৳2,000 BDT (approximately $18 USD)
- **Delivery speed**: Choose "Economy" (1-3 days, lower fees ~$0.50-$1.50)
- **Delivery method**: Select "Mobile Money" or "Cash Pickup to Mobile Wallet"
- **Service provider**: Select "bKash" or "Nagad" (based on winner's preference)
- Remitly will show exchange rate and total fees
- Verify total cost (should be ~$18.50-$19.50 USD equivalent)

**If using Wise:**
- **You send**: Enter amount in PLN or EUR (your funding currency)
- **Recipient gets**: ৳2,000 BDT
- Wise will show exchange rate and fees
- Verify total cost (should be ~$18-$19 USD equivalent)

**Step 4: Enter Recipient Details**

**If using Remitly:**
- **Full name**: Copy exactly as winner provided (must match their mobile wallet/NID)
- **Mobile wallet number** OR **Phone number**: Enter with country code +880
  - Example: +8801712345678 (11 digits after +880)
- **Delivery method**: Confirm "Mobile Money - bKash" or "Mobile Money - Nagad"
- Remitly may ask for purpose of transfer: Select "Family support" or "Gift"

**If using Wise:**
- **Delivery method**: Choose "Mobile money" or "Mobile wallet" (if available)
  - If mobile wallet not available: Choose "Bank account" and winner provides bank details
- **Full name**: Copy exactly as winner provided (must match their NID/ID)
- **Mobile wallet number** OR **Bank account**: Enter with country code +880
  - Example: +8801712345678 (for mobile wallet)
- **Phone number**: Same as mobile wallet for mobile money

**Step 5: Review and Confirm**
- Verify all details are correct
- Check exchange rate and fees
- Confirm transfer
- **Save transaction ID** (e.g., "REMITLY-123456789" or "WISE-123456789")

**Step 6: Update Firestore**
```javascript
// Update payment record in Firestore
{
  status: "processing", // Change from "pending" to "processing"
  transferService: "remitly", // or "wise" or "western_union"
  transactionId: "REMITLY-123456789",
  transferFee: 0.85, // Actual fee (Remitly: ~$0.50-$1.50, Wise: ~$0.20-$0.40)
  exchangeRate: 110.5, // Rate used
  processedAt: new Date()
}
```

**Step 7: Monitor Transfer Status**
- **If using Remitly**: Check Remitly dashboard or app for transfer status
  - Economy transfers: 1-3 business days
  - You'll receive email when transfer completes
- **If using Wise**: Check Wise dashboard for transfer status
  - Typical timeline: 1-2 business days
  - You'll receive email when transfer completes

**Step 8: Confirm Completion**
- Once service confirms delivery, update Firestore:
```javascript
{
  status: "completed",
  completedAt: new Date()
}
```

**Step 9: Winner Notification**
- App automatically notifies winner when status changes to "completed"
- Winner checks their bKash/Nagad/Rocket app and sees ৳2,000

**Troubleshooting Common Issues:**

**Problem**: "Recipient name doesn't match account"
- **Solution**: Contact winner to verify exact name on their mobile wallet account
- Names must match exactly as registered with bKash/Nagad/Rocket

**Problem**: "Mobile wallet number invalid"
- **Solution**: Verify format +8801XXXXXXXXX (11 digits after +880)
- Bangladesh mobile numbers: +880 1X XX XXX XXX

**Problem**: "Transfer delayed"
- **Solution**: Check Wise status. May need additional verification for first transfer
- Contact Wise support if delayed >3 days
- Keep winner informed via app

**Problem**: "Winner wants different payment method"
- **Solution**: Wise supports multiple delivery options
- Can switch between mobile wallets if first option fails
- Can fall back to bank transfer or Western Union cash pickup

**Record Keeping:**
- Save all transaction IDs in Firestore
- Keep Wise/WU transaction receipts
- Create monthly spreadsheet of all prize payments for tax purposes:
  ```
  Date | Tournament ID | Winner Name | Amount BDT | Amount USD | Service | Transaction ID | Fee
  ```

### Appendix D: Technical Architecture Diagram

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
                             │ Winner info:
                             │ - Name
                             │ - Mobile wallet #
                ┌────────────▼─────────────┐
                │  Developer (Poland)      │
                │  Manual Transfer via:    │
                │  - Wise (recommended)    │
                │  - Western Union         │
                │  - Remitly              │
                └────────────┬─────────────┘
                             │
                             │ International
                             │ Money Transfer
                ┌────────────▼─────────────┐
                │  Bangladesh Mobile       │
                │  Wallet Service:         │
                │  - bKash                 │
                │  - Nagad                 │
                │  - Rocket                │
                └────────────┬─────────────┘
                             │
                             │ ৳2,000 BDT
                ┌────────────▼─────────────┐
                │  Winner's Mobile Wallet  │
                │  Funds Received          │
                └──────────────────────────┘
```

**Key Points**:
- Developer in Poland **cannot directly access** bKash/Nagad/Rocket
- Developer uses **Wise/Western Union/Remitly** to send money internationally
- These services deliver to winner's Bangladesh mobile wallet
- Winner receives funds in their local bKash/Nagad/Rocket account
- Process takes 1-2 business days typically

---

## Conclusion

This approach document provides a simplified, cost-effective framework for launching a Bangladesh-specific version of Gridline Soccer with promotional cash prizes. The streamlined implementation minimizes development complexity and operational overhead while maintaining compliance with Bangladesh skill-based gaming regulations.

**⚠️ CRITICAL UPDATE**: The original document assumed the developer could directly use bKash, Nagad, or Rocket payment services. **This is not possible from Poland** due to geo-restrictions. The solution is to use **international money transfer services**. **Remitly is recommended** as the primary service for direct mobile wallet transfers (bKash, Nagad), with Wise as an alternative for bank transfers and Western Union as backup.

**Key Success Factors**:
1. **Legal Compliance**: Strict adherence to Bangladesh skill-based gaming regulations
2. **Simplified Eligibility**: Google Play verification + user declaration (no document upload)
3. **International Payment Processing**: Developer uses Remitly (primary), Wise (bank transfers), or Western Union (backup) to send prizes to winners' Bangladesh mobile wallets or bank accounts
4. **Low Operational Cost**: ৳4,000/month (~$36) + transfer fees (~$1-$3/month with Remitly) for bi-monthly tournaments
5. **User Experience**: Minimal friction for players, no complex verification steps
6. **Seamless Migration**: Shared Firebase backend ensures existing users preserve all data

**Simplified Approach Benefits**:
- **Faster time to market**: 10-15 weeks vs. 16+ weeks
- **Lower development cost**: ~$9,285-$15,725 vs. ~$16,500-$23,000 (including migration)
- **Minimal ongoing costs**: ~$107-$189/month (including transfer fees with Remitly)
- **Reduced complexity**: No payment gateway APIs, no document storage, simpler user flow
- **Lower user friction**: No document upload, immediate eligibility confirmation
- **Data continuity**: Users keep all progress, friends, and stats when migrating
- **International accessibility**: Polish developer can send prizes without Bangladesh residency
- **Reliable mobile wallet support**: Remitly provides excellent direct transfer capability to bKash and Nagad

**Migration Strategy Highlights**:
- **Current user base**: 746 active Bangladesh users on `piotr_gorczynski.soccer2`
- **Target conversion**: 30% (224 users) in Month 1, 60% (448 users) by Month 6
- **Shared Firebase**: Same authentication and database for seamless transition
- **No separate keys needed**: Same Firebase project, different app registrations
- **Multi-channel promotion**: In-app banners, Play Store optimization, social media
- **User-friendly approach**: Optional migration, data preservation, clear incentives
- **Note**: Push notifications not viable as most users don't have registered accounts

**Next Steps**:
1. Game assumptions validated with ChatGPT legal consultation - skill-based framework confirmed
2. Age verification approach validated - self-declaration + Google Play verification aligns with legal frameworks
3. **Set up Remitly account (PRIMARY) and test transfer to Bangladesh mobile wallet** (CRITICAL for Polish developer)
4. **Set up Wise account (SECONDARY) and verify mobile wallet support from Poland** (or use for bank transfers)
5. Set up dual-app Firebase configuration (register both package IDs)
6. Begin Phase 1 implementation (planning & setup)
7. Develop migration promotion materials and messaging
8. Establish bi-monthly tournament schedule
9. Create manual payment processing procedures using Remitly/Wise/Western Union
10. Launch migration campaign to existing 746 Bangladesh users

**Important for Polish Developer**:
- **DO NOT attempt to create bKash/Nagad/Rocket accounts** - these services are geo-locked to Bangladesh
- **DO create a Remitly account** (https://www.remitly.com) - RECOMMENDED for best mobile wallet support (bKash, Nagad)
- **DO create a Wise account** (https://wise.com) - works from Poland, verify mobile wallet support or use for bank transfers
- **DO test the transfer process** before launching tournaments - send ৳500 via Remitly Economy to test bKash/Nagad delivery
- **DO budget for transfer fees** - ~$0.50-$1.50 per prize payment with Remitly Economy (covered by developer)

---

**Document Prepared By**: Development Team  
**Legal Validation**: Game assumptions validated with ChatGPT legal consultation  
**Review Required**: Payment gateway experts, Bangladesh market specialists  
**Approval Required**: Product Owner (Piotr Gorczyński)

---

## Implementation Status

### ✅ Phase 1: Product Flavor Setup (COMPLETED - 2025-12-29)

The Bangladesh product flavor has been successfully implemented with the following configuration:

#### Product Flavor Configuration
- **Flavor Dimensions**: Added "market" dimension alongside existing "environment" dimension
- **Product Flavors**:
  - `global` - Base variant (package: `piotr_gorczynski.soccer2`)
  - `bangladesh` - Bangladesh variant (package: `piotr_gorczynski.soccer2.bd`)
- **Version Suffix**: Bangladesh variant includes `-BD` suffix (e.g., `17.3-BD`)

#### Build Variants Created
The configuration generates 12 build variants (3 environments × 2 markets × 2 build types):
- `_devGlobalDebug`, `_devGlobalRelease`
- `_devBangladeshDebug`, `_devBangladeshRelease`
- `_testGlobalDebug`, `_testGlobalRelease`
- `_testBangladeshDebug`, `_testBangladeshRelease`
- `_prodGlobalDebug`, `_prodGlobalRelease`
- `_prodBangladeshDebug`, `_prodBangladeshRelease`

#### Firebase Configuration
A single `google-services.json` file per environment contains configurations for all registered Android apps in the Firebase project:

**Configuration files in `secrets/` directory:**
- `secrets/google-services.dev.json` (contains both global and Bangladesh configs)
- `secrets/google-services.test.json` (contains both global and Bangladesh configs)
- `secrets/google-services.prod.json` (contains both global and Bangladesh configs)

The build system uses the same file for both global and Bangladesh variants. Firebase automatically selects the correct client configuration based on the application's package name at runtime.

#### Build Commands
```bash
# Build global production release
./gradlew assemble_prodGlobalRelease

# Build Bangladesh production release
./gradlew assemble_prodBangladeshRelease
```

#### Documentation
- `mobile/app/PRODUCT_FLAVOR_README.md` - Complete setup and usage guide

#### Facebook Content Provider Configuration
To prevent conflicts when both global and Bangladesh apps are installed on the same device, a Bangladesh-specific AndroidManifest.xml has been created:

**File**: `mobile/app/src/bangladesh/AndroidManifest.xml`

This manifest overrides the Facebook Content Provider authority to use a unique value for the Bangladesh variant:
- **Global app**: `com.facebook.app.FacebookContentProvider1232966491486195`
- **Bangladesh app**: `com.facebook.app.FacebookContentProvider1232966491486195.bd`

This allows both apps to coexist on the same device without Android provider conflicts.

#### Troubleshooting

**Error**: `INSTALL_FAILED_CONFLICTING_PROVIDER`
```
Can't install because provider name com.facebook.app.FacebookContentProvider1232966491486195 
(in package piotr_gorczynski.soccer2.bd) is already used by piotr_gorczynski.soccer2
```

**Solution**: This error occurs when both the global and Bangladesh apps use the same Facebook Content Provider authority. The fix has been implemented in `mobile/app/src/bangladesh/AndroidManifest.xml` which overrides the provider authority with a `.bd` suffix for the Bangladesh variant.

**To verify the fix**:
```bash
# Build the Bangladesh debug variant
cd mobile
./gradlew assemble_devBangladeshDebug

# The APK should now install without conflicts
```

#### Next Steps
1. Register Bangladesh app in Firebase Console (package: `piotr_gorczynski.soccer2.bd`)
2. Download and place google-services.json file in secrets/ directory
3. Continue with Phase 2: Backend Development (Firestore schema, Cloud Functions)
4. Then Phase 3: Mobile App Development (UI, eligibility, payment flows)

---

**Status**: ✅ Product Flavor Setup Complete (with Facebook provider conflict fix)  
**Next Phase**: Backend Development (Phase 2)  
**Last Updated**: 2026-01-10

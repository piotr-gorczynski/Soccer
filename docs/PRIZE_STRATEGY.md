# Tournament Prize Strategy Specification

## Document Purpose

This document defines the complete prize allocation strategy for the Bangladesh variant tournament system. It establishes clear, deterministic, and legally compliant rules for determining tournament rankings and distributing financial rewards.

---

## 1. Scope

This specification defines how 1st, 2nd, and 3rd place must be determined and how financial rewards must be allocated in a skill-based tournament, including tie handling and eligibility rules.

### Goals

- **Fairness**: Equal treatment for all participants based on objective performance
- **Determinism**: Identical inputs always produce identical outputs
- **Regulatory Safety**: Compliance with skill-based gaming regulations
- **Resistance to Abuse**: Clear rules that prevent exploitation
- **Predictable Payouts**: Transparent and calculable reward distribution

---

## 2. Definitions

| Term | Definition |
|------|------------|
| **Participant** | A registered tournament player |
| **Final Score** | Numeric score calculated by the existing tournament algorithm |
| **Eligible Participant** | A participant whose final score is greater than 0 |
| **Prize Positions** | 1st, 2nd, and 3rd place only |
| **Prize Pool** | Total amount available for distribution: 3,500 BDT |
| **Tie** | Two or more eligible participants with identical final scores |

### Prize Pool Breakdown

- **1st Place**: 2,000 BDT
- **2nd Place**: 1,000 BDT
- **3rd Place**: 500 BDT
- **Total**: 3,500 BDT

---

## 3. Eligibility Rules (MANDATORY)

### R1 — Minimum Score Requirement

**Rule**: A participant MUST have `final score > 0` to be eligible for any financial reward.

**Rationale**: Ensures only participants who demonstrated skill receive prizes.

### R2 — Zero-Score Exclusion

Participants with `final score = 0`:

- **MUST NOT** receive any prize
- **MUST NOT** be considered when determining 1st, 2nd, or 3rd place
- **MUST NOT** participate in tie calculations

**Rationale**: Prevents participation-based rewards, maintaining skill-based classification.

### R3 — Fewer Eligible Players

If fewer than 3 eligible participants exist:

- Only corresponding prize positions **MAY** be paid
- Unused prize amounts **MUST NOT** be redistributed

**Example**: If only 2 eligible participants exist:
- 1st place receives 2,000 BDT
- 2nd place receives 1,000 BDT
- 3rd place prize (500 BDT) remains unpaid

---

## 4. Ranking Rules

### R4 — Ranking Input

- Only **eligible participants** are used for ranking
- Ranking is based **solely** on final score
- Higher score = higher rank

### R5 — Determinism

The ranking algorithm **MUST**:

- Produce the same output for the same input data
- Contain **NO** randomness in ranking or payout
- Be fully reproducible and auditable

---

## 5. Tie Handling Rules (CRITICAL)

### R6 — Tie Grouping

Eligible participants with identical final scores **MUST**:

- Be grouped together
- Occupy consecutive rank positions

### R7 — Prize Combination

When a tie occurs at a prize-winning position:

1. **Sum** all prizes for the occupied prize positions
2. **Divide equally** the combined amount among tied eligible participants
3. Apply rounding rules (see Section 7)

### R8 — Skipped Positions

Prize positions covered by a tie **MUST** be skipped for subsequent participants.

**Example**: If 2 players tie for 1st place:
- Both receive share of (1st + 2nd)
- Next player is ranked 3rd
- 2nd place is skipped

---

## 6. Tie Examples (For Validation)

### Example A — Two Players Tied for 1st

**Eligible Players:**
- Player A: 100 points (tie for 1st)
- Player B: 100 points (tie for 1st)
- Player C: 50 points (next)

**Calculation:**
```
Combined prizes = 1st + 2nd = 2,000 + 1,000 = 3,000 BDT
Tied players: 2
Payout per tied player = 3,000 ÷ 2 = 1,500 BDT
```

**Result:**
- Player A: 1,500 BDT (1st place, tied)
- Player B: 1,500 BDT (1st place, tied)
- Player C: 500 BDT (3rd place)
- **Total paid**: 3,500 BDT ✓

---

### Example B — Four Players Tied for 1st

**Eligible Players:**
- Player A: 100 points (tie for 1st)
- Player B: 100 points (tie for 1st)
- Player C: 100 points (tie for 1st)
- Player D: 100 points (tie for 1st)

**Calculation:**
```
Combined prizes = 1st + 2nd + 3rd = 2,000 + 1,000 + 500 = 3,500 BDT
Tied players: 4
Payout per player = 3,500 ÷ 4 = 875 BDT
```

**Result:**
- Player A: 875 BDT
- Player B: 875 BDT
- Player C: 875 BDT
- Player D: 875 BDT
- **Total paid**: 3,500 BDT ✓

---

### Example C — Zero-Score Crowd

**Scores:**
- Player A: 10 points
- Player B: 5 points
- Players C–Z: 0 points (20+ players)

**Calculation:**
```
Eligible players: A, B (only players with score > 0)
Zero-score players excluded from ranking
```

**Result:**
- Player A: 2,000 BDT (1st place)
- Player B: 1,000 BDT (2nd place)
- Players C–Z: 0 BDT (ineligible)
- **Total paid**: 3,000 BDT
- **Unpaid**: 500 BDT (no 3rd place)

---

### Example D — Three-Way Tie for 2nd Place

**Eligible Players:**
- Player A: 100 points (1st)
- Player B: 50 points (tie for 2nd)
- Player C: 50 points (tie for 2nd)
- Player D: 50 points (tie for 2nd)

**Calculation:**
```
Player A: 1st place = 2,000 BDT
Combined prizes for tied 2nd = 2nd + 3rd = 1,000 + 500 = 1,500 BDT
Tied players: 3
Payout per tied player = 1,500 ÷ 3 = 500 BDT
```

**Result:**
- Player A: 2,000 BDT (1st place)
- Player B: 500 BDT (2nd place, tied)
- Player C: 500 BDT (2nd place, tied)
- Player D: 500 BDT (2nd place, tied)
- **Total paid**: 3,500 BDT ✓

---

## 7. Rounding Rules

### R9 — Currency Rounding

All payouts **MUST**:

- Be rounded **down** to whole BDT
- Use floor rounding (e.g., 875.67 → 875)

Remaining fractional amounts **MUST NOT**:

- Be paid to any participant
- Be redistributed in any manner
- Be carried over to future tournaments

**Example:**
```
Total: 3,500 BDT
Tied players: 3
Division: 3,500 ÷ 3 = 1,166.666...
Payout per player: 1,166 BDT (rounded down)
Total paid: 3,498 BDT
Remaining: 2 BDT (retained, not distributed)
```

---

## 8. Prize Cap Rule

### R10 — Maximum Payout

**Rule**: Total payouts **MUST NOT** exceed the predefined total prize pool (3,500 BDT).

The algorithm **MUST NOT**:

- Generate additional payouts under any circumstance
- Exceed the prize pool through calculation errors
- Create fractional payments that sum to more than available

**Verification**: Before finalizing payouts, the system must verify:
```
SUM(all_payouts) ≤ 3,500 BDT
```

---

## 9. Output Requirements (For Implementation)

For each tournament, the system **MUST** be able to produce:

### 9.1 Ordered List of Participants

- All participants ordered by final score (descending)
- Eligibility status clearly marked
- Zero-score participants listed separately

### 9.2 Rank Assignment

Each eligible participant assigned to:
- 1st place (tied or sole)
- 2nd place (tied or sole)
- 3rd place (tied or sole)
- No prize position

### 9.3 Payout Calculation

For each eligible participant:
- Exact payout amount (integer BDT)
- Calculation method used
- Prize positions involved in tie (if applicable)

### 9.4 Verification Proof

The output must include proof that:
- ✓ Zero-score participants are excluded from prizes
- ✓ Total payout ≤ 3,500 BDT
- ✓ Tie logic was applied correctly
- ✓ No randomness was used
- ✓ Rounding rules were followed

---

## 10. Explicit Non-Requirements (Clarity)

The following are **explicitly excluded** from this specification:

❌ **No prizes beyond 3rd place**
- Only top 3 positions receive financial rewards

❌ **No participation prizes**
- All prizes are performance-based (score > 0)

❌ **No random tie-breakers**
- All ties are resolved through equal prize splitting

❌ **No manual overrides**
- The algorithm operates deterministically without human intervention

❌ **No redistribution of unused prizes**
- Unclaimed prize money is not redistributed to other participants

---

## 11. Compliance Goals

The algorithm **MUST**:

✓ **Qualify as skill-based**
- Rewards based solely on performance (final score)
- No luck or chance elements in prize distribution

✓ **Avoid gambling characteristics**
- No random elements in prize allocation
- Transparent, predictable outcomes

✓ **Be transparent and auditable**
- All calculations can be independently verified
- Complete audit trail of ranking and payout decisions

✓ **Be safe for Google Play distribution**
- Complies with Google Play policies on real-money tournaments
- Meets skill-based game requirements

✓ **Minimize disputes and support claims**
- Clear, unambiguous rules
- Verifiable calculations
- Consistent application across all tournaments

---

## 12. Implementation Algorithm (Pseudocode)

```pseudocode
function calculateTournamentPrizes(participants):
    // Step 1: Filter eligible participants
    eligible = participants.filter(p => p.finalScore > 0)
    
    // Step 2: Sort by score descending
    eligible.sort((a, b) => b.finalScore - a.finalScore)
    
    // Step 3: Group by score to identify ties
    scoreGroups = groupByScore(eligible)
    
    // Step 4: Assign prize positions
    prizePositions = [
        {position: 1, amount: 2000},
        {position: 2, amount: 1000},
        {position: 3, amount: 500}
    ]
    
    payouts = []
    currentPosition = 0
    
    for each scoreGroup in scoreGroups:
        if currentPosition >= 3:
            break  // No more prizes
        
        // Calculate how many prize positions this group occupies
        groupSize = scoreGroup.length
        positionsToConsume = min(groupSize, 3 - currentPosition)
        
        // Sum prizes for occupied positions
        combinedPrize = sum(prizePositions[currentPosition...currentPosition+positionsToConsume].amount)
        
        // Divide equally among tied players (round down)
        payoutPerPlayer = floor(combinedPrize / groupSize)
        
        // Assign payouts
        for each player in scoreGroup:
            payouts.add({
                player: player,
                position: currentPosition + 1,
                amount: payoutPerPlayer,
                tied: groupSize > 1
            })
        
        // Move to next available position
        currentPosition += groupSize
    
    // Step 5: Verify total payout
    totalPayout = sum(payouts.amount)
    assert(totalPayout <= 3500)
    
    return payouts

function groupByScore(participants):
    groups = []
    currentScore = null
    currentGroup = []
    
    for each participant in participants:
        if participant.score != currentScore:
            if currentGroup.length > 0:
                groups.add(currentGroup)
            currentGroup = [participant]
            currentScore = participant.score
        else:
            currentGroup.add(participant)
    
    if currentGroup.length > 0:
        groups.add(currentGroup)
    
    return groups
```

---

## 13. Testing Requirements

### 13.1 Unit Tests

The implementation must pass all of the following test scenarios:

1. **Single winner, no ties**
   - 3+ eligible participants with unique scores
   - Verify correct prize allocation (2000, 1000, 500)

2. **Two-way tie for 1st**
   - As per Example A above
   - Verify equal splitting (1500, 1500, 500)

3. **Four-way tie for 1st**
   - As per Example B above
   - Verify equal splitting (875, 875, 875, 875)

4. **Zero-score exclusion**
   - As per Example C above
   - Verify zero-score players receive nothing

5. **Three-way tie for 2nd**
   - As per Example D above
   - Verify correct splitting (2000, 500, 500, 500)

6. **Rounding test**
   - 3-way tie for 1st (3500 ÷ 3 = 1166.66...)
   - Verify floor rounding (1166, 1166, 1166)

7. **Fewer than 3 eligible participants**
   - Test with 1 and 2 eligible participants
   - Verify unused prizes are not redistributed

8. **All zero scores**
   - All participants have score = 0
   - Verify no prizes awarded

9. **Determinism test**
   - Run same input data multiple times
   - Verify identical output every time

10. **Maximum payout verification**
    - For all test cases, verify total ≤ 3500 BDT

### 13.2 Edge Cases

- Exactly 3 participants, all with different scores
- Exactly 3 participants, all with same score
- 100+ participants with various tie scenarios
- Maximum possible score values
- Minimum eligible score (score = 1)

---

## 14. Audit Trail Requirements

For regulatory compliance, each tournament must maintain:

1. **Input Data**
   - List of all participants
   - Final scores for each participant
   - Timestamp of tournament completion

2. **Calculation Log**
   - Eligibility filtering results
   - Identified tie groups
   - Prize calculation steps
   - Rounding operations

3. **Output Data**
   - Final rankings
   - Prize allocations
   - Total payout amount
   - Verification checksums

4. **Retention**
   - Audit trail must be retained for minimum 2 years
   - Must be retrievable for regulatory inspection

---

## 15. Future Considerations

This specification is designed to be extensible. Potential future enhancements (not currently in scope):

- **Additional prize tiers** (4th, 5th place)
- **Variable prize pools** based on entry fees
- **Tournament size adjustments** (different rules for small vs. large tournaments)
- **Regional variations** (different currencies/amounts)

Any changes to this specification must:
- Maintain determinism
- Preserve skill-based nature
- Pass all existing test cases
- Include updated compliance review

---

## 16. Approval and Sign-off

| Role | Name | Date | Status |
|------|------|------|--------|
| Product Owner | | | ⏳ Pending |
| Technical Lead | | | ⏳ Pending |
| Legal Advisor | | | ⏳ Pending |
| Compliance Officer | | | ⏳ Pending |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-31 | GitHub Copilot | Initial specification based on requirements |

---

## References

1. Bangladesh Gaming Regulations (skill-based gaming)
2. Google Play Real-Money Gaming Policies
3. BANGLADESH_VERSION_APPROACH.md - Overall project approach
4. Tournament scoring algorithm documentation (to be linked)

---

**Document Status**: ✅ Complete and Ready for Implementation

This specification provides a complete, production-ready definition of the prize allocation strategy that is fair, transparent, and compliant with regulatory requirements.

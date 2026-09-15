const assert = require("node:assert/strict");
const test = require("node:test");

const {
  getCashEligibilityRequirements,
  buildEligibilityConfirmation
} = require("./eligibility");

const regulation = {
  market: "BD",
  minimumAge: 18,
  prizeRules: {
    cashPrizesEnabled: true,
    payoutMethods: ["bkash", "nagad"]
  }
};

test("requires confirmation for a cash-prize tournament", () => {
  assert.deepEqual(
    getCashEligibilityRequirements({ prizePool: { enabled: true } }, regulation),
    { market: "BD", minimumAge: 18, payoutMethods: ["bkash", "nagad"] }
  );
});

test("does not require confirmation for a legacy non-cash tournament", () => {
  assert.equal(getCashEligibilityRequirements({}, {}), null);
});

test("stores every confirmation without selecting a payout method", () => {
  const confirmedAt = { serverTimestamp: true };
  const result = buildEligibilityConfirmation({
    ageConfirmed: true,
    hasSupportedPayoutAccount: true,
    termsAccepted: true
  }, "regulation-1", {
    market: "BD",
    minimumAge: 18,
    payoutMethods: ["bkash", "nagad"]
  }, confirmedAt);

  assert.deepEqual(result, {
    regulationId: "regulation-1",
    market: "BD",
    minimumAge: 18,
    ageConfirmed: true,
    hasSupportedPayoutAccount: true,
    termsAccepted: true,
    payoutMethodsOffered: ["bkash", "nagad"],
    confirmedAt
  });
  assert.equal("selectedPayoutMethod" in result, false);
});

test("rejects a missing confirmation on every join attempt", () => {
  assert.throws(() => buildEligibilityConfirmation({
    ageConfirmed: true,
    hasSupportedPayoutAccount: false,
    termsAccepted: true
  }, "regulation-1", {
    market: "BD",
    minimumAge: 18,
    payoutMethods: ["bkash"]
  }, {}), /All eligibility confirmations/);
});

test("rejects cash regulations without payout methods", () => {
  assert.throws(() => getCashEligibilityRequirements(
    { prizePool: { enabled: true } },
    { minimumAge: 18, prizeRules: { cashPrizesEnabled: true, payoutMethods: [] } }
  ), /payout methods/);
});

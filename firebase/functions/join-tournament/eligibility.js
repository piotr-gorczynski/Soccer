function getCashEligibilityRequirements(tournament, regulation) {
  const prizeRules = regulation && regulation.prizeRules;
  if (!tournament?.prizePool?.enabled && prizeRules?.cashPrizesEnabled !== true) {
    return null;
  }

  if (!regulation || prizeRules?.cashPrizesEnabled !== true) {
    throw new Error("Cash-prize tournament regulation is missing prize rules.");
  }
  if (!Number.isInteger(regulation.minimumAge) || regulation.minimumAge < 1) {
    throw new Error("Cash-prize tournament regulation has an invalid minimum age.");
  }
  if (!Array.isArray(prizeRules.payoutMethods)
      || prizeRules.payoutMethods.length === 0
      || prizeRules.payoutMethods.some(method => typeof method !== "string" || !method.trim())) {
    throw new Error("Cash-prize tournament regulation has no valid payout methods.");
  }

  return {
    market: regulation.market || null,
    minimumAge: regulation.minimumAge,
    payoutMethods: [...prizeRules.payoutMethods]
  };
}

function buildEligibilityConfirmation(eligibility, regulationId, requirements, confirmedAt) {
  if (!eligibility
      || eligibility.ageConfirmed !== true
      || eligibility.hasSupportedPayoutAccount !== true
      || eligibility.termsAccepted !== true) {
    throw new Error("All eligibility confirmations are required.");
  }

  return {
    regulationId,
    market: requirements.market,
    minimumAge: requirements.minimumAge,
    ageConfirmed: true,
    hasSupportedPayoutAccount: true,
    termsAccepted: true,
    payoutMethodsOffered: requirements.payoutMethods,
    confirmedAt
  };
}

module.exports = {
  getCashEligibilityRequirements,
  buildEligibilityConfirmation
};

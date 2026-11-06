// Public projection only: never return Firestore documents or participant data.
function projectTournament(id, data, now) {
  const pool = data?.prizePool;
  const deadline = data?.registrationDeadline?.toMillis?.();
  const first = Array.isArray(pool?.awards) ? pool.awards.find(a => a?.place === 1) : null;
  if (data?.status !== "registering" || !Array.isArray(data.visibleInFlavours)
      || !data.visibleInFlavours.includes("bangladesh") || pool?.enabled !== true
      || pool.currency !== "BDT" || !Number.isSafeInteger(first?.amount) || first.amount <= 0
      || !Number.isSafeInteger(deadline) || deadline <= now
      || !Number.isSafeInteger(data.maxParticipants) || data.maxParticipants <= 0
      || !Number.isSafeInteger(data.participantsCount) || data.participantsCount < 0
      || data.participantsCount >= data.maxParticipants
      || typeof data.name !== "string" || !data.name.trim() || data.name.length > 200
      || typeof data.regulation !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(data.regulation)
      || !/^[A-Za-z0-9_-]{1,128}$/.test(id)) return null;
  return {
    id, name: data.name.trim(), regulationId: data.regulation,
    firstPrize: first.amount, currency: pool.currency, registrationDeadlineMillis: deadline
  };
}

async function findPromotion(db, now) {
  // Explicit market membership prevents a public listing of Global-only tournaments.
  const snap = await db.collection("tournaments").where("visibleInFlavours", "array-contains", "bangladesh").get();
  const eligible = snap.docs.map(d => projectTournament(d.id, d.data(), now)).filter(Boolean)
      .sort((a, b) => a.registrationDeadlineMillis - b.registrationDeadlineMillis || a.id.localeCompare(b.id));
  const promotions=[];
  const regulations=new Map();
  for (const candidate of eligible) {
    if (!regulations.has(candidate.regulationId)) {
      regulations.set(candidate.regulationId,await db.collection("regulations").doc(candidate.regulationId).get());
    }
    const regulation = regulations.get(candidate.regulationId);
    const rules = regulation.data();
    if (rules?.status === "active" && rules.market === "BD"
        && Number.isSafeInteger(rules.minimumAge) && rules.minimumAge >= 18
        && rules.prizeRules?.cashPrizesEnabled === true) {
      // Match the supported Bangladesh methods in join-tournament/eligibility.js.
      // The regulation decides which of those methods this tournament offers.
      const configured = rules.prizeRules.payoutMethods;
      const payoutMethods = Array.isArray(configured)
        ? [...new Set(configured.filter(method => method === "bkash" || method === "rocket"))]
        : [];
      promotions.push({ ...candidate, minimumAge: rules.minimumAge, payoutMethods });
    }
  }
  return promotions;
}
module.exports = { projectTournament, findPromotion };

const ONE_HOUR_MS = 60 * 60 * 1000;

// Auth is authoritative. Never infer providers from optional Firestore profile fields.
function shouldDeleteUnverifiedUser(user, now) {
  const providers = user?.providerData;
  if (!Array.isArray(providers) || providers.length === 0
      || !providers.every(provider => provider?.providerId === 'password')
      || typeof user.email !== 'string' || !user.email.trim()
      || user.emailVerified !== false) return false;

  const createdAt = Date.parse(user.metadata?.creationTime);
  return Number.isFinite(now) && Number.isFinite(createdAt)
    && now - createdAt > ONE_HOUR_MS;
}

async function deleteIfStillEligible(auth, uid, now) {
  // A user may verify email or link an OAuth provider after listUsers was read.
  const current = await auth.getUser(uid);
  if (!shouldDeleteUnverifiedUser(current, now)) return false;
  await auth.deleteUser(uid);
  return true;
}

module.exports = { shouldDeleteUnverifiedUser, deleteIfStillEligible };

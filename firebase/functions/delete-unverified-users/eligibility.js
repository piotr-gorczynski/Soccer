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

function hasEstablishedProfile(profile) {
  return !!profile && ((typeof profile.nickname === 'string' && profile.nickname.trim().length > 0)
    || profile.termsAccepted === true || profile.legacyGlobalTermsAccepted === true);
}

async function deleteIfStillEligible(auth, uid, now, firestore) {
  // Completed guest profiles must survive linking to an unverified email.
  // Read errors fail closed: the caller logs the error and skips deletion.
  const profile = await firestore.collection('users').doc(uid).get();
  if (hasEstablishedProfile(profile.exists ? profile.data() : null)) return false;
  // A user may verify email or link an OAuth provider after listUsers was read.
  const current = await auth.getUser(uid);
  if (!shouldDeleteUnverifiedUser(current, now)) return false;
  await auth.deleteUser(uid);
  return true;
}

module.exports = { shouldDeleteUnverifiedUser, deleteIfStillEligible, hasEstablishedProfile };

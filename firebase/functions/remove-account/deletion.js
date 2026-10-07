// Historical UID references and legal/payout subcollections are deliberately untouched.
const TOMBSTONE_NAME = '(Account removed)';

async function minimizeProfile(db, uid, FieldValue) {
  const ref = db.collection('users').doc(uid);
  await db.runTransaction(async tx => {
    const snapshot = await tx.get(ref);
    const old = snapshot.data() || {};
    // Preserve legacy consent evidence without inventing a document version or scope.
    const legacyRef = ref.collection('legalAcceptances').doc('legacy_terms_unversioned');
    const hasLegacy = (old.termsAccepted === true && old.termsAcceptanceModel !== 2)
      || old.legacyGlobalTermsAccepted === true;
    const legacy = hasLegacy ? await tx.get(legacyRef) : null;
    if (hasLegacy && !legacy.exists) {
      tx.set(legacyRef, {
        documentType: 'terms', legacy: true, scope: 'unknown', version: null,
        accepted: true,
        // A modern acceptance may have overwritten the legacy timestamp; do not reuse it.
        acceptedAt: old.termsAcceptanceModel !== 2 && typeof old.termsAcceptanceDate?.toMillis === 'function'
          ? old.termsAcceptanceDate : null,
        language: old.termsAcceptanceModel !== 2 && typeof old.language === 'string'
          && /^[a-z]{2,3}(-[A-Za-z]{2,4})?$/.test(old.language) ? old.language : null,
      });
    }
    const tombstone = {
      accountDeleted: true,
      nickname: TOMBSTONE_NAME,
      nicknameLowercase: '(account removed)',
      accountDeletedAt: typeof old.accountDeletedAt?.toMillis === 'function'
        ? old.accountDeletedAt : FieldValue.serverTimestamp(),
    };
    if (old.accountDeleted === true && old.nickname === TOMBSTONE_NAME
        && old.nicknameLowercase === tombstone.nicknameLowercase
        && typeof old.accountDeletedAt?.toMillis === 'function'
        && Object.keys(old).length === 4) return;
    // Replacement, not merge: also removes unknown/future provider and profile fields.
    // Setting a document does not remove its legalAcceptances or other subcollections.
    tx.set(ref, tombstone);
  });
}

async function deletePages(db, collection) {
  for (;;) {
    const page = await collection.limit(300).get();
    if (page.empty) return;
    const batch = db.batch();
    page.docs.forEach(doc => batch.delete(doc.ref));
    await batch.commit();
  }
}

async function removePendingInvitations(db, uid, field) {
  let last;
  for (;;) {
    let query = db.collection('invitations').where(field, '==', uid).orderBy('__name__').limit(200);
    if (last) query = query.startAfter(last);
    const page = await query.get();
    if (page.empty) return;
    for (const doc of page.docs) {
      if (doc.get('status') !== 'pending') continue;
      // Recheck under transaction so an invitation accepted concurrently is preserved.
      await db.runTransaction(async tx => {
        const current = await tx.get(doc.ref);
        if (current.get('status') === 'pending') tx.delete(doc.ref);
      });
    }
    last = page.docs[page.docs.length - 1];
  }
}

async function cleanupDeletedAccount(db, rtdb, uid, FieldValue) {
  await minimizeProfile(db, uid, FieldValue);
  // A minimal offline marker blocks stale ID tokens/onDisconnect from recreating presence.
  // It contains no activity timestamp, device information or notification target.
  await rtdb.ref('status').child(uid).set({ accountDeleted: true, state: 'offline' });
  await removePendingInvitations(db, uid, 'from');
  await removePendingInvitations(db, uid, 'to');
  await deletePages(db, db.collection('users').doc(uid).collection('friends'));
  // Friend IDs are document IDs, not queryable fields; page user IDs only, without profiles.
  let last;
  for (;;) {
    let query = db.collection('users').orderBy('__name__').select().limit(300);
    if (last) query = query.startAfter(last);
    const page = await query.get();
    if (page.empty) break;
    const batch = db.batch();
    page.docs.forEach(user => batch.delete(user.ref.collection('friends').doc(uid)));
    await batch.commit();
    last = page.docs[page.docs.length - 1];
  }
}

module.exports = { minimizeProfile, cleanupDeletedAccount };

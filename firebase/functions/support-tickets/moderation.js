"use strict";

// Caller-controlled context is a bounded screen label, never an authoritative identity.
function validate(data, reporting) {
  const allowed = reporting ? ['reportedUid', 'reason', 'context'] : ['reportedUid', 'blocked'];
  if (!data || typeof data !== 'object' || Object.keys(data).some(k => !allowed.includes(k))
      || typeof data.reportedUid !== 'string' || !data.reportedUid.trim()
      || data.reportedUid.length > 128 || data.reportedUid.includes('/')) throw new Error('Invalid player.');
  if (reporting && (typeof data.reason !== 'string' || !data.reason.trim() || data.reason.length > 1000
      || !['friends', 'search', 'ranking', 'invitation', 'tournament'].includes(data.context))) {
    throw new Error('A reason (1–1000 characters) and valid screen context are required.');
  }
  if (!reporting && typeof data.blocked !== 'boolean') throw new Error('Invalid block state.');
  return data;
}

function handlers(db, FieldValue, HttpsError) {
  async function act(data, context, reporting) {
    const uid = context.auth?.uid;
    if (!uid) throw new HttpsError('unauthenticated', 'Login required.');
    try { validate(data, reporting); } catch (e) { throw new HttpsError('invalid-argument', e.message); }
    if (uid === data.reportedUid) throw new HttpsError('invalid-argument', 'Cannot moderate yourself.');
    return db.runTransaction(async tx => {
      const ownRef = db.collection('users').doc(uid);
      const own = await tx.get(ownRef);
      const target = await tx.get(db.collection('users').doc(data.reportedUid));
      if (!own.exists || own.get('accountDeleted') === true) throw new HttpsError('permission-denied', 'Account unavailable.');
      if ((!target.exists || target.get('accountDeleted') === true) && (reporting || data.blocked)) {
        throw new HttpsError('not-found', 'Player unavailable.');
      }
      const now = FieldValue.serverTimestamp();
      if (!reporting) {
        const ref = ownRef.collection('blocks').doc(data.reportedUid);
        const existing = await tx.get(ref);
        if (data.blocked && !existing.exists) tx.set(ref, { createdAt: now });
        if (!data.blocked && existing.exists) tx.delete(ref);
        return { blocked: data.blocked };
      }
      // Shared per-reporter document serializes concurrent submissions and enforces the cap.
      const gate = ownRef.collection('moderationState').doc('reports');
      await tx.get(gate);
      const reports = await tx.get(db.collection('supportTickets').where('reporterUid', '==', uid));
      const active = reports.docs.filter(d => !['resolved', 'closed'].includes(d.get('status')));
      const duplicate = active.find(d => d.get('reportedUid') === data.reportedUid);
      if (duplicate) return { reference: duplicate.get('reference') };
      if (active.length >= 10) throw new HttpsError('resource-exhausted', 'Too many open reports.');
      const ticket = db.collection('supportTickets').doc();
      const reference = 'MOD-' + ticket.id.slice(-12).toUpperCase();
      tx.set(gate, { updatedAt: now });
      tx.set(ticket, { reference, type: 'moderation', category: 'player_report', status: 'open',
        userId: uid, reporterUid: uid, reportedUid: target.id,
        nickname: target.get('nickname') || '', reason: data.reason.trim(), message: data.reason.trim(),
        appContext: { screen: data.context, source: 'client-reported' },
        createdAt: now, updatedAt: now, statusUpdatedAt: now });
      tx.set(ticket.collection('statusHistory').doc('created'), { eventType: 'ticket_created',
        from: null, to: 'open', changedAt: now, changedBy: uid, actorType: 'user', source: 'reportPlayer' });
      return { reference };
    });
  }
  return { reportPlayer: (data, context) => act(data, context, true),
    setPlayerBlock: (data, context) => act(data, context, false) };
}
module.exports = { validate, handlers };

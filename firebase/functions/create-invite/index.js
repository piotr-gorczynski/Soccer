// functions/create-invite/index.js  (CommonJS)
const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();

const TTL_MIN = 5;                       // invite lifetime in minutes

exports.createInvite = functions
  .region('us-central1')
  .https.onCall(async (data, context) => {

    /* ── guards ────────────────────────────────────────────── */
    const from = context.auth?.uid;
    const to   = data?.toUid;

    if (!from)
      throw new functions.https.HttpsError('unauthenticated', 'Login required.');
    if (!to)
      throw new functions.https.HttpsError('invalid-argument', 'Missing toUid.');
    if (from === to)
      throw new functions.https.HttpsError('failed-precondition', 'Cannot invite yourself.');

    const tournamentId = data?.tournamentId || null;
    const matchPath    = data?.matchPath    || null;
    const invitesCol   = admin.firestore().collection('invitations');

    /* ── transaction guarantees one active invite per sender ─ */
    const result = await admin.firestore().runTransaction(async tx => {

      const now      = admin.firestore.Timestamp.now();          // fresh each call
      const expireAt = admin.firestore.Timestamp.fromMillis(
                         now.toMillis() + TTL_MIN * 60_000);

      const conflict = await tx.get(
        invitesCol.where('from', '==', from)
                  .where('status', '==', 'pending')
                  .where('expireAt', '>', now)   // ignore expired
                  .limit(1)
      );

      if (!conflict.empty)
        throw new functions.https.HttpsError(
          'failed-precondition',
          'sender_busy'          /* ← caller already has a pending invite */
        );

      /* 2️⃣  target player must be “free” (no pending IN or OUT invites) ---- */
      const [incoming, outgoing] = await Promise.all([
        tx.get(
          invitesCol.where('to', '==',  to)
                    .where('status', '==', 'pending')
                    .where('expireAt', '>', now)
                    .limit(1)
        ),
        tx.get(
          invitesCol.where('from', '==', to)
                    .where('status', '==', 'pending')
                    .where('expireAt', '>', now)
                    .limit(1)
        )
      ]);
      if (!incoming.empty || !outgoing.empty)
        throw new functions.https.HttpsError(
          'failed-precondition',
          'target_busy'          /* ← target is waiting or being invited */
        );

      const ref = invitesCol.doc();
      tx.set(ref, {
        from, to,
        tournamentId,
        matchPath,
        status   : 'pending',
        expireAt : expireAt,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });

      return { inviteId: ref.id };
    });

    return result;        // { inviteId: 'abc123' }
});

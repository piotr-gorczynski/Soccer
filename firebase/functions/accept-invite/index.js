// functions/accept-invite/index.js  (CommonJS style)
const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();

const db                  = admin.firestore();
const initialTimeSeconds  = 300;   // 5-minute chess clock

exports.acceptInvite = functions
  .region('us-central1')
  .https.onCall(async (data, context) => {

    /* ── basic guards (outside the tx) ───────────────────────── */
    const uid          = context.auth?.uid;
    const invitationId = data?.invitationId;

    if (!uid)
      throw new functions.https.HttpsError('unauthenticated', 'Login required');

    if (!invitationId)
      throw new functions.https.HttpsError('invalid-argument', 'Invitation ID missing');

    const inviteRef = db.collection('invitations').doc(invitationId);

    /* ── ONE transaction does all the work ───────────────────── */
    const { matchPath } = await db.runTransaction(async tx => {

      const now       = admin.firestore.Timestamp.now();   // server time
      const inviteDoc = await tx.get(inviteRef);

      if (!inviteDoc.exists)
        throw new functions.https.HttpsError('not-found', 'Invitation not found');

      const invite = inviteDoc.data();

      /* ownership */
      if (invite.to !== uid)
        throw new functions.https.HttpsError(
          'permission-denied', 'This invitation isn’t addressed to you');

      /* status still pending */
      if (invite.status !== 'pending')
        throw new functions.https.HttpsError(
          'failed-precondition', 'Invitation is no longer available');

      /* not past TTL */
      if (invite.expireAt && invite.expireAt.toMillis() <= now.toMillis())
        throw new functions.https.HttpsError(
          'deadline-exceeded', 'Invitation has expired');

      /* ---------- choose or create match doc ------------------ */
      let matchRef;
      if (invite.matchPath) {                 // tournament – doc already exists
        matchRef = db.doc(invite.matchPath);
        const mSnap = await tx.get(matchRef);
        if (!mSnap.exists)
          throw new functions.https.HttpsError(
            'not-found', 'Scheduled match document is missing');
      } else {                                // friendly
        matchRef = db.collection('matches').doc();
        tx.set(matchRef, {
          player0       : invite.from,
          player1       : invite.to,
          status        : 'active',
          turn          : 0,
          winner        : null,
          invitationId,
          tournamentId  : invite.tournamentId || null,
          createdAt     : admin.firestore.FieldValue.serverTimestamp(),
          updatedAt     : admin.firestore.FieldValue.serverTimestamp(),
          remainingTime0: initialTimeSeconds,
          remainingTime1: initialTimeSeconds,
          turnStartTime : null
        });

        /* initial move at board centre */
        tx.set(
          matchRef.collection('moves').doc(),
          {
            x: 3, y: 4, p: 0,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
          }
        );
      }

      /* mark invitation as accepted */
      tx.update(inviteRef, { status: 'accepted' });

      return { matchPath: matchRef.path };    // ← returned outside tx
    });

    /* ── send push to the inviter (best-effort) ──────────────── */
    (async () => {
      try {
        const inviteDoc = await db.collection('invitations').doc(invitationId).get();
        const { from, to } = inviteDoc.data();
        const fromUserSnap = await db.collection('users').doc(from).get();
        const fcmToken     = fromUserSnap.get('fcmToken');

        if (fcmToken) {
          await admin.messaging().send({
            token: fcmToken,
            data : {
              type     : 'start',
              matchPath: matchPath,
              fromNickname: 'Your opponent'
            },
            notification: {
              title: 'Game started!',
              body : 'Your opponent accepted your invitation.'
            }
          });
        }
      } catch (err) {
        console.warn('⚠️  Failed to send FCM start push', err);
      }
    })();

    /* ── success payload to the client ───────────────────────── */
    return { matchPath };
  });

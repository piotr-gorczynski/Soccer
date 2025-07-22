// functions/accept-invite/index.js        (CommonJS)
const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();

const db                 = admin.firestore();
const initialTimeSeconds = 300;   // 5-minute chess clock

exports.acceptInvite = functions
  .region('us-central1')
  .https.onCall(async (data, context) => {

    /* ─────────────────── basic guards ─────────────────────── */
    const uid          = context.auth?.uid;
    const invitationId = data?.invitationId;

    if (!uid)
      throw new functions.https.HttpsError('unauthenticated', 'Login required');

    if (!invitationId)
      throw new functions.https.HttpsError('invalid-argument', 'Invitation ID missing');

    const inviteRef = db.collection('invitations').doc(invitationId);

    /* ── ONE Firestore transaction does all the work ───────── */
    const { matchPath } = await db.runTransaction(async tx => {

      const now       = admin.firestore.Timestamp.now();   // server time
      const inviteDoc = await tx.get(inviteRef);

      if (!inviteDoc.exists)
        throw new functions.https.HttpsError('not-found', 'Invitation not found');

      const invite = inviteDoc.data();

      /* 1 Ownership */
      if (invite.to !== uid)
        throw new functions.https.HttpsError(
          'permission-denied', 'This invitation isn’t addressed to you');

      /* 2 Still pending */
      if (invite.status !== 'pending')
        throw new functions.https.HttpsError(
          'failed-precondition', 'Invitation already handled');

      /* 3 TTL guard */
      if (invite.expireAt && invite.expireAt.toMillis() <= now.toMillis())
        throw new functions.https.HttpsError(
          'deadline-exceeded', 'Invitation has expired');

      /* 4 Choose or create the match document */
      let matchRef;
      if (invite.matchPath) {                     // tournament — doc exists
        matchRef = db.doc(invite.matchPath);
        const mSnap = await tx.get(matchRef);
        if (!mSnap.exists)
          throw new functions.https.HttpsError(
            'not-found', 'Scheduled match document is missing');

        const mData = mSnap.data();
        if (mData.status === 'completed' || mData.winner)
          throw new functions.https.HttpsError(
            'failed-precondition', 'match_already_completed');

        /* patch existing doc */
        tx.update(matchRef, {
          status        : 'active',
          invitationId  : invitationId,
          updatedAt     : admin.firestore.FieldValue.serverTimestamp(),
          remainingTime0: initialTimeSeconds,
          remainingTime1: initialTimeSeconds,
          turn          : 0,
          turnStartTime : null
        });
        /* ensure first move exists */
        tx.set(
          matchRef.collection('moves').doc(),
          {
            x: 3, y: 4, p: 0,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
          }
        );
      } else {                                    // friendly — create new doc
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

        /* initial move at field centre */
        tx.set(
          matchRef.collection('moves').doc(),
          {
            x: 3, y: 4, p: 0,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
          }
        );
      }

      /* 5 Mark invitation as accepted */
      tx.update(inviteRef, { status: 'accepted' });

      return { matchPath: matchRef.path };        // ← returned outside tx
    });

    /* ───────── success response to the client ─────────────── */
    return { matchPath };
  });

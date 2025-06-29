// functions/index.ts
import * as functions from 'firebase-functions';
import * as admin     from 'firebase-admin';
admin.initializeApp();

/**
 *  data = { toUid: string,
 *           tournamentId?: string,
 *           matchPath?   ?: string }
 */
export const createInvite = functions.https.onCall(async (data, context) => {
  /* ── sanity checks ────────────────────────────────────────────── */
  const from = context.auth?.uid;
  const to   = data?.toUid as string | undefined;

  if (!from) throw new functions.https.HttpsError('unauthenticated', 'Login required.');
  if (!to  ) throw new functions.https.HttpsError('invalid-argument', 'Missing toUid.');
  if (from === to)
      throw new functions.https.HttpsError('failed-precondition', 'You cannot invite yourself.');

  /* optional extras coming from MatchAdapter */
  const tournamentId = data?.tournamentId as string | undefined;
  const matchPath    = data?.matchPath    as string | undefined;

  const invites = admin.firestore().collection('invitations');

  /* ── one-active-invite guarantee ──────────────────────────────── */
  const result = await admin.firestore().runTransaction(async tx => {
    const clash = await tx.get(
      invites.where('from', '==', from)
             .where('status', '==', 'pending')
             .limit(1)
    );
    if (!clash.empty) {
      throw new functions.https.HttpsError(
        'failed-precondition', 'You already have an active invite.');
    }

    const ref = invites.doc();          // auto-ID
    tx.set(ref, {
      from, to,
      tournamentId  : tournamentId ?? null,
      matchPath     : matchPath    ?? null,
      status        : 'pending',
      createdAt     : admin.firestore.FieldValue.serverTimestamp(),
    });

    /* value returned from runTransaction is sent to the caller */
    return { inviteId: ref.id };
  });

  return result;          // { inviteId: 'abc123' }
});

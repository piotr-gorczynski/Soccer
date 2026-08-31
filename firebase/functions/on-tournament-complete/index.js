// functions/on-tournament-complete/index.js
const functions = require('firebase-functions/v1');
const admin     = require('firebase-admin');

// Only initialize if not already initialized
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const { Timestamp } = admin.firestore;

/**
 * Localized "You won the tournament!" messages for FCM push notifications.
 * Keys match the `language` field stored on the user document.
 */
const WINNER_MESSAGES = {
  'en': 'You won the tournament! 🏆',
  'am': 'ውድድሩን አሸነፉ! 🏆',
  'ar': 'لقد فزت بالبطولة! 🏆',
  'bn': 'আপনি টুর্নামেন্ট জিতেছেন! 🏆',
  'de': 'Du hast das Turnier gewonnen! 🏆',
  'es': '¡Ganaste el torneo! 🏆',
  'fa': 'شما تورنمنت را بردید! 🏆',
  'fr': 'Vous avez remporté le tournoi ! 🏆',
  'hi': 'आपने टूर्नामेंट जीत लिया! 🏆',
  'km': 'អ្នកឈ្នះការប្រកួត! 🏆',
  'lo': 'ທ່ານຊະນະການແຂ່ງຂັນ! 🏆',
  'mg': 'Menaka ny fifaninanana ianao! 🏆',
  'mn': 'Та тэмцээнд тэргүүллээ! 🏆',
  'my': 'သင် ပြိုင်ပွဲကို အနိုင်ရသည်! 🏆',
  'ne': 'तपाईंले प्रतियोगिता जित्नुभयो! 🏆',
  'pl': 'Wygrałeś turniej! 🏆',
  'si': 'ඔබ තරඟාවලිය ජය ගත්තා! 🏆',
  'so': 'Tartanka ayaad ku guuleysatay! 🏆',
  'sw': 'Umeshinda mashindano! 🏆',
  'ur': 'آپ نے ٹورنامنٹ جیت لیا! 🏆',
};

/* ─────────────────────────────────────────────────────────────── *
 *  Firestore trigger: fires whenever a tournament document is     *
 *  updated.  When the status transitions to "ended" this         *
 *  function:                                                      *
 *    1. Computes Round-Robin standings from completed matches.    *
 *    2. Writes per-rank documents to tournaments/{id}/results.   *
 *    3. If the tournament has a prize pool, creates a pending     *
 *       payment record for the 1st-place winner.                 *
 *    4. Sends an FCM notification to the winner.                 *
 * ─────────────────────────────────────────────────────────────── */
exports.onTournamentComplete = functions.firestore
  .document('tournaments/{tournamentId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after  = change.after.data();
    const tournamentId = context.params.tournamentId;

    // Only act when status transitions to 'ended'.
    if (before.status === after.status || after.status !== 'ended') {
      return null;
    }

    console.log(`[onTournamentComplete] Processing tournament ${tournamentId}`);

    const tournamentRef = change.after.ref;

    // Idempotency guard: skip if results have already been written.
    const existingResults = await tournamentRef.collection('results').limit(1).get();
    if (!existingResults.empty) {
      console.log(`[onTournamentComplete] Results already exist for ${tournamentId}, skipping`);
      return null;
    }

    // Step 1: Compute standings.
    const standings = await computeStandings(tournamentRef);

    if (standings.length === 0) {
      console.log(`[onTournamentComplete] No participants found for ${tournamentId}`);
      return null;
    }

    // Step 2: Write results sub-collection (one document per rank).
    const now   = Timestamp.now();
    const batch = db.batch();

    for (const entry of standings) {
      const resultRef = tournamentRef.collection('results').doc(String(entry.rank));
      batch.set(resultRef, {
        rank:        entry.rank,
        userId:      entry.userId,
        wins:        entry.wins,
        losses:      entry.losses,
        points:      entry.points,
        computedAt:  now,
      });
    }

    await batch.commit();
    console.log(`[onTournamentComplete] Results written for ${tournamentId} (${standings.length} entries)`);

    // Step 3: Create a payment record if this is a prize tournament.
    const prizePool = after.prizePool;
    if (!prizePool || !prizePool.enabled) {
      console.log(`[onTournamentComplete] No prize pool for ${tournamentId}, done`);
      return null;
    }

    if (standings.length === 0 || !standings[0]) {
      console.log(`[onTournamentComplete] No winner could be determined for ${tournamentId}`);
      return null;
    }

    const winner = standings[0]; // rank 1

    // Idempotency guard: skip if a payment record already exists for this tournament.
    const existingPayment = await db.collection('payments')
      .where('tournamentId', '==', tournamentId)
      .where('rank', '==', 1)
      .get();

    if (!existingPayment.empty) {
      console.log(`[onTournamentComplete] Payment record already exists for ${tournamentId}`);
      return null;
    }

    const paymentRef = db.collection('payments').doc();
    await paymentRef.set({
      userId:       winner.userId,
      tournamentId: tournamentId,
      amount:       prizePool.firstPlacePrize,
      currency:     prizePool.currency || 'BDT',
      rank:         1,
      status:       'pending',
      createdAt:    now,
    });

    console.log(
      `[onTournamentComplete] Payment record ${paymentRef.id} created for winner ${winner.userId}`
    );

    // Step 4: Notify the winner via FCM (best-effort; failure does not abort the function).
    await notifyWinner(winner.userId, tournamentId, after.name || '');

    return null;
  });

/* ─────────────────────────────────────────────────────────────── *
 *  Helper: compute Round-Robin standings from completed matches.  *
 *                                                                 *
 *  Points scheme: win = 3 pts, draw = 1 pt (each), loss = 0.    *
 *  Tie-break: total wins descending, then userId ascending.       *
 * ─────────────────────────────────────────────────────────────── */
async function computeStandings(tournamentRef) {
  // Fetch all participants to include players with no completed matches.
  const participantsSnap = await tournamentRef.collection('participants').get();
  if (participantsSnap.empty) return [];

  const playerIds = participantsSnap.docs.map(d => d.id);

  // Initialize counters for every participant.
  const wins   = {};
  const draws  = {};
  const losses = {};
  for (const uid of playerIds) {
    wins[uid]   = 0;
    draws[uid]  = 0;
    losses[uid] = 0;
  }

  // Tally results from all completed matches.
  const matchesSnap = await tournamentRef.collection('matches')
    .where('status', '==', 'completed')
    .get();

  for (const matchDoc of matchesSnap.docs) {
    const match = matchDoc.data();

    if (match.winner) {
      // Decisive result.
      const winnerUid = match.winner;
      const loserUid  = winnerUid === match.player0 ? match.player1 : match.player0;

      if (winnerUid in wins)  wins[winnerUid]    += 1;
      if (loserUid  in losses) losses[loserUid]  += 1;
    } else if (match.player0 && match.player1) {
      // Drawn match (no winner set).
      const p0 = match.player0;
      const p1 = match.player1;

      if (p0 in draws) draws[p0] += 1;
      if (p1 in draws) draws[p1] += 1;
    }
  }

  // Build and sort the standings array.
  const standings = playerIds.map(uid => ({
    userId: uid,
    wins:   wins[uid],
    draws:  draws[uid],
    losses: losses[uid],
    points: wins[uid] * 3 + draws[uid],
  }));

  standings.sort((a, b) => {
    if (b.points !== a.points) return b.points - a.points;
    if (b.wins   !== a.wins)   return b.wins   - a.wins;
    return a.userId.localeCompare(b.userId);  // deterministic tie-break
  });

  standings.forEach((entry, index) => {
    entry.rank = index + 1;
  });

  return standings;
}

/* ─────────────────────────────────────────────────────────────── *
 *  Helper: send an FCM push notification to the tournament winner.*
 * ─────────────────────────────────────────────────────────────── */
async function notifyWinner(userId, tournamentId, tournamentName) {
  try {
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) {
      console.warn(`[notifyWinner] User ${userId} not found`);
      return;
    }

    const userData = userDoc.data();
    if (userData.accountDeleted === true || (!userData.fcmInstallationId && !userData.fcmToken)) {
      console.log(`[notifyWinner] Skipping notification for user ${userId} (deleted or no FCM target)`);
      return;
    }

    const language = userData.language || 'en';
    const localizedMessage = WINNER_MESSAGES[language] || WINNER_MESSAGES['en'];

    const message = {
      ...(userData.fcmInstallationId
        ? { fid: userData.fcmInstallationId }
        : { token: userData.fcmToken }),
      data: {
        type: 'tournament_winner',
        tournamentId: tournamentId,
        tournamentName: tournamentName,
        title: localizedMessage,
        body: tournamentName,
      },
      android: { priority: 'high' },
    };

    await admin.messaging().send(message);
    console.log(`[notifyWinner] Notification sent to user ${userId}`);
  } catch (error) {
    // Notification failure must never prevent payment record creation.
    if (error.code === 'messaging/registration-token-not-registered' ||
        error.code === 'messaging/invalid-registration-token') {
      console.warn(`[notifyWinner] Invalid FCM token for user ${userId}`);

      try {
        const fcmErrorType = error.code === 'messaging/registration-token-not-registered'
          ? 'NotRegistered'
          : 'InvalidRegistration';

        await db.collection('users').doc(userId).update({
          fcmErrorType: fcmErrorType,
          fcmErrorDate: admin.firestore.FieldValue.serverTimestamp(),
        });
      } catch (updateError) {
        console.error(`[notifyWinner] Failed to update FCM error for user ${userId}: ${updateError.message}`);
      }
    } else {
      console.error(`[notifyWinner] Failed to notify user ${userId}: ${error.message}`);
    }
  }
}

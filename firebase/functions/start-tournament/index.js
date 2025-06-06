const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();
const db  = admin.firestore();
const { Timestamp } = admin.firestore;

/* ────────────────────────────────────────────────────────────── *
 *  Scheduled task: runs every hour.                              *
 *  1. Reads all tournaments whose status == "registering".       *
 *  2. Starts those whose registrationDeadline has already passed *
 *     (according to server time).                                *
 *  3. For now only Round Robin is implemented; other formats     *
 *     are stubbed.                                               *
 * ────────────────────────────────────────────────────────────── */
exports.startTournament = functions.pubsub
  .schedule('every 1 hours')
  .timeZone('Europe/Warsaw')                // choose your local TZ
  .onRun(async () => {

    const now = Timestamp.now();

    // 1️⃣  Fetch only tournaments that are still in "registering".
    //     This query needs no composite index.
    const regSnap = await db.collection('tournaments')
        .where('status', '==', 'registering')
        .get();

    if (regSnap.empty) {
      console.log('No tournaments to check.');
      return null;
    }

    // 2️⃣  Keep only those whose registration deadline is past.
    const toStart = regSnap.docs.filter(doc => {
      const deadline = doc.get('registrationDeadline');   // Timestamp
      return deadline && deadline.toMillis() <= now.toMillis();
    });

    if (toStart.length === 0) {
      console.log('No tournaments ready to start.');
      return null;
    }

    console.log(`Starting ${toStart.length} tournament(s)…`);

    // 3️⃣  Launch the correct pairing generator
    for (const doc of toStart) {
      const data = doc.data();
      const ref  = doc.ref;

      switch (data.format) {
        case 'RoundRobin':
          await generateRoundRobin(ref);          // implemented below
          break;

        case 'DoubleElim':
          console.log(`TODO: DoubleElim for ${doc.id}`);
          await ref.update({ status: 'running', startedAt: now });
          break;

        case 'Swiss':
          console.log(`TODO: Swiss for ${doc.id}`);
          await ref.update({ status: 'running', startedAt: now });
          break;

        default:
          console.warn(`Unknown format ${data.format} in ${doc.id}`);
      }
    }
    return null;
  });

/* ────────────────────────────────────────────────────────────── *
 * Helper: create one match for every unique pair of players      *
 * inside tournaments/{tid}/matches.                              *
 * ────────────────────────────────────────────────────────────── */
async function generateRoundRobin(tournamentRef) {
  const now   = Timestamp.now();
  const parts = await tournamentRef.collection('participants').get();
  const ids   = parts.docs.map(d => d.id);

  // Skip if matches already exist (idempotent behaviour).
  const existing = await tournamentRef.collection('matches')
                     .limit(1).get();
  if (!existing.empty) {
    console.log(`Matches already exist for ${tournamentRef.id}, skipping`);
    return;
  }

  const batch = db.batch();
  for (let i = 0; i < ids.length; i++) {
    for (let j = i + 1; j < ids.length; j++) {
      const mRef = tournamentRef.collection('matches').doc();
      batch.set(mRef, {
        playerA:   ids[i],
        playerB:   ids[j],
        status:    'scheduled',
        createdAt: now
      });
    }
  }

  // Mark tournament as running and save the start timestamp.
  batch.update(tournamentRef, { status: 'running', startedAt: now });
  await batch.commit();

  console.log(`Round-robin pairs created for ${tournamentRef.id}`);
}

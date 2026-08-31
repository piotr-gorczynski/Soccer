const functions = require('firebase-functions/v1');
const admin     = require('firebase-admin');
admin.initializeApp();
const db  = admin.firestore();
const { Timestamp } = admin.firestore;

/**
 * Localized messages for "Tournament started!" in all supported languages.
 * These match the "lobby_started" strings in the mobile app.
 */
const TOURNAMENT_STARTED_MESSAGES = {
  'en': 'Tournament started!',
  'am': 'ውድድሩ ተጀመረ!',
  'ar': 'بدأت البطولة!',
  'bn': 'টুর্নামেন্ট শুরু!',
  'de': 'Turnier gestartet!',
  'es': '¡Torneo iniciado!',
  'fa': 'تورنمنت شروع شد!',
  'fr': 'Tournoi commencé !',
  'hi': 'टूर्नामेंट शुरू हुआ!',
  'km': 'ការប្រកួតបានចាប់ផ្តើមហើយ!',
  'lo': 'ເລີ່ມການແຂ່ງຂັນແລ້ວ!',
  'mg': 'Nanomboka ny fifaninanana!',
  'mn': 'Тэмцээн эхэллээ!',
  'my': 'ပြိုင်ပွဲ စတင်ပြီးပါပြီ!',
  'ne': 'प्रतियोगिता सुरु भयो!',
  'pl': 'Turniej rozpoczęty!',
  'si': 'තරඟාවලිය ආරම්භ විය!',
  'so': 'Tartanku wuu bilaabmay!',
  'sw': 'Mashindano yameanza!',
  'ur': 'ٹورنامنٹ شروع ہو گیا!'
};

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

    // 3️⃣  Launch the correct pairing generator and send notifications
    for (const doc of toStart) {
      const data = doc.data();
      const ref  = doc.ref;
      const tournamentName = data.name || '';

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

      // Send notifications to participants after tournament starts
      await sendNotificationsToParticipants(ref, tournamentName);
    }
    return null;
  });

/* ────────────────────────────────────────────────────────────── *
 * Helper: Send FCM notifications to tournament participants.     *
 * ────────────────────────────────────────────────────────────── */
async function sendNotificationsToParticipants(tournamentRef, tournamentName) {
  const tournamentId = tournamentRef.id;
  console.log(`[sendNotifications] Processing tournament ${tournamentId}: "${tournamentName}"`);

  try {
    // Get all participants
    const participantsSnapshot = await tournamentRef.collection('participants').get();

    if (participantsSnapshot.empty) {
      console.log(`[sendNotifications] No participants for tournament ${tournamentId}`);
      return;
    }

    const participantIds = participantsSnapshot.docs.map(doc => doc.id);
    console.log(`[sendNotifications] Found ${participantIds.length} participants`);

    // Get user data for all participants
    const userDocs = await Promise.all(
      participantIds.map(uid => db.collection('users').doc(uid).get())
    );

    // Filter users who should receive notifications
    const eligibleUsers = userDocs
      .filter(doc => {
        if (!doc.exists) return false;

        const userData = doc.data();

        // Check if account is deleted
        if (userData.accountDeleted === true) return false;

        // Prefer the current FID registration, but keep legacy tokens working
        // while users migrate from app version 17.6.
        if (!userData.fcmInstallationId && !userData.fcmToken) return false;

        return true;
      })
      .map(doc => ({
        uid: doc.id,
        fcmInstallationId: doc.data().fcmInstallationId,
        fcmToken: doc.data().fcmToken,
        language: doc.data().language || 'en'
      }));

    console.log(`[sendNotifications] ${eligibleUsers.length} eligible users to notify`);

    if (eligibleUsers.length === 0) return;

    // Send notifications
    let sentCount = 0;
    let failedCount = 0;

    for (const user of eligibleUsers) {
      try {
        const localizedMessage = TOURNAMENT_STARTED_MESSAGES[user.language] || TOURNAMENT_STARTED_MESSAGES['en'];
        
        const message = {
          ...(user.fcmInstallationId
            ? { fid: user.fcmInstallationId }
            : { token: user.fcmToken }),
          data: {
            type: 'tournament_started',
            tournamentId: tournamentId,
            tournamentName: tournamentName,
            title: localizedMessage,
            body: tournamentName
          },
          android: { priority: 'high' }
        };

        await admin.messaging().send(message);
        sentCount++;
        console.log(`[sendNotifications] Sent to user ${user.uid} (${user.language}, ${user.fcmInstallationId ? 'fid' : 'legacy-token'})`);
      } catch (error) {
        failedCount++;
        
        // Handle invalid/expired FCM tokens
        if (error.code === 'messaging/registration-token-not-registered' ||
            error.code === 'messaging/invalid-registration-token') {
          console.warn(`[sendNotifications] Invalid FCM token for user ${user.uid}`);
          
          // Mark the token as invalid in Firestore
          try {
            const fcmErrorType = error.code === 'messaging/registration-token-not-registered' 
              ? 'NotRegistered' 
              : 'InvalidRegistration';
            
            await db.collection('users').doc(user.uid).update({
              fcmErrorType: fcmErrorType,
              fcmErrorDate: admin.firestore.FieldValue.serverTimestamp()
            });
          } catch (updateError) {
            console.error(`[sendNotifications] Failed to update FCM error for user ${user.uid}: ${updateError.message}`);
          }
        } else {
          console.error(`[sendNotifications] Failed to send to user ${user.uid}: ${error.message}`);
        }
      }
    }

    console.log(`[sendNotifications] Completed: sent=${sentCount}, failed=${failedCount}`);
  } catch (error) {
    console.error(`[sendNotifications] Error for tournament ${tournamentId}: ${error.message}`);
    // Don't throw - we don't want to fail tournament start if notifications fail
  }
}

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
        player0:   ids[i],
        player1:   ids[j],
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

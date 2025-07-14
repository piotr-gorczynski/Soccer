const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();

exports.expireStaleInvites = functions
  .region('us-central1')
  .pubsub.schedule('every 1 minutes')
  .timeZone('Europe/Warsaw')
  .onRun(async () => {
    const db = admin.firestore();
    const now = admin.firestore.Timestamp.now();

    const snapshot = await db.collection('invitations')
      .where('status', '==', 'pending')
      .where('expireAt', '<=', now)
      .get();

    if (snapshot.empty) {
      console.log('👌 No expired invites found this run');
      return null;
    }

    const batch = db.batch();
    snapshot.forEach(doc => {
      batch.update(doc.ref, { status: 'expired' });
    });

    await batch.commit();
    console.log(`✅ Expired ${snapshot.size} stale invitation(s)`);

    return null;
  });

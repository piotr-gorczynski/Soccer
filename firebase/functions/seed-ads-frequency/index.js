const admin = require('firebase-admin');
admin.initializeApp();
const db = admin.firestore();

async function main() {
  await db.collection('settings').doc('adsFreuency').set({
    value: 10,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  console.log('adsFreuency document created.');
}

main().catch(err => { console.error(err); process.exit(1); });

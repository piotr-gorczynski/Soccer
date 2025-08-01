const admin = require('firebase-admin');
admin.initializeApp();
const db = admin.firestore();

async function main() {
  const status = process.argv[2];
  if (!status) {
    console.error('Usage: node index.js <status>');
    process.exit(1);
  }
  await db.collection('settings').doc('serviceStatus').set({
    status,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  console.log('serviceStatus document created.');
}

main().catch(err => { console.error(err); process.exit(1); });

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

admin.initializeApp();

const db = admin.firestore();

async function main() {
  const rulesPath = path.join(__dirname, '..', '..', 'seed', 'tournament_rules.json');
  const body = fs.readFileSync(rulesPath, 'utf8');

  await db.collection('regulations').add({
    name: 'General Tournament Game Rules',
    body,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    status: 'active'
  });

  console.log('Regulations document uploaded.');
}

main().catch(err => { console.error(err); process.exit(1); });

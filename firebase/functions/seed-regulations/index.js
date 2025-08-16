const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

admin.initializeApp();

const db = admin.firestore();

async function main() {
  const seedDir = path.join(__dirname, '..', '..', 'seed');
  const files = fs.readdirSync(seedDir).filter(f => /^tournament_rules_.*\.json$/.test(f));
  const enRulesPath = path.join(seedDir, 'tournament_rules_en.json');
  let body = '';
  if (fs.existsSync(enRulesPath)) {
    const enContent = JSON.parse(fs.readFileSync(enRulesPath, 'utf8'));
    if (Array.isArray(enContent.rules)) {
      body = enContent.rules.map(r => `• ${r}`).join('\n\n');
    }
  }

  if (files.length === 0) {
    console.error('No tournament rules files found.');
    return;
  }

  const docRef = await db.collection('regulations').add({
    name: 'General Tournament Game Rules',
    body,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    status: 'active'
  });

  for (const file of files) {
    const iso = file.replace('tournament_rules_', '').replace('.json', '');
    const content = JSON.parse(fs.readFileSync(path.join(seedDir, file), 'utf8'));
    await docRef.collection(iso).doc('rules').set(content);
  }

  console.log('Regulations documents uploaded.');
}

main().catch(err => { console.error(err); process.exit(1); });

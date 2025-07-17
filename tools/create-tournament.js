// tools/create-tournament.js
const fs   = require('fs');
const path = require('path');
const admin = require('firebase-admin');

// ────────────────────────────────────────────────────────────────
// 1. Load service-account credentials from the secrets folder
//    (../secrets relative to this script’s directory)
const serviceAccount = require(path.join(__dirname, '..', 'secrets', 'serviceAccountKey.json'));

// 2. Initialise Firebase Admin with that key
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
// ────────────────────────────────────────────────────────────────

// (rest of your original code stays unchanged)

async function main () {
  const args = process.argv.slice(2);
  let params = {};

  if (args.length === 1 && args[0].endsWith('.json')) {
    params = JSON.parse(fs.readFileSync(path.resolve(args[0]), 'utf8'));
  } else if (args.length === 4) {
    params = {
      name: args[0],
      maxParticipants: Number(args[1]),
      registrationDeadline: args[2],
      matchesDeadline: args[3]
    };
  } else {
    console.error('Usage: node create-tournament.js name maxParticipants registrationDeadline matchesDeadline');
    console.error('   or: node create-tournament.js params.json');
    process.exit(1);
  }

  const { name, maxParticipants, registrationDeadline, matchesDeadline } = params;
  if (!name || !maxParticipants || !registrationDeadline || !matchesDeadline) {
    console.error('Missing required parameters.');
    process.exit(1);
  }

  const { Timestamp } = admin.firestore;
  const regDeadline   = Timestamp.fromDate(new Date(registrationDeadline));
  const matchDeadline = Timestamp.fromDate(new Date(matchesDeadline));

  try {
    const doc = await db.collection('tournaments').add({
      name,
      maxParticipants,
      registrationDeadline: regDeadline,
      matchesDeadline: matchDeadline,
      format: 'RoundRobin',
      status: 'registering',
      participantsCount: 0,
      createdAt: Timestamp.now()
    });
    console.log('Tournament created with ID:', doc.id);
  } catch (err) {
    console.error('Failed to create tournament:', err.message);
    process.exit(1);
  }
}

main();

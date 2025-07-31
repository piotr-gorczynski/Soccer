// tools/create-tournament/create-tournament.js
const fs   = require('fs');
const path = require('path');
const admin = require('firebase-admin');

// ────────────────────────────────────────────────────────────────
// Service account loading happens after reading the desired environment
// from the command line. The key files are stored two directories up
// under `secrets/serviceAccountKey.{env}.json`.
// ────────────────────────────────────────────────────────────────

// (rest of your original code stays unchanged)

async function main () {
  const args = process.argv.slice(2);

  const env = args.shift();
  if (!['dev', 'test', 'prod'].includes(env)) {
    console.error('First argument must specify the environment: dev, test or prod');
    process.exit(1);
  }

  const serviceAccountPath = path.join(__dirname, '..', '..', 'secrets', `serviceAccountKey.${env}.json`);
  const serviceAccount     = require(serviceAccountPath);

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  const db = admin.firestore();

  let params = {};

  if (args.length === 1 && args[0].endsWith('.json')) {
    params = JSON.parse(fs.readFileSync(path.resolve(args[0]), 'utf8'));
  } else if (args.length === 5) {
    params = {
      name: args[0],
      maxParticipants: Number(args[1]),
      registrationDeadline: args[2],
      matchesDeadline: args[3],
      regulation: args[4]
    };
  } else {
    console.error('Usage: node create-tournament.js <env> name maxParticipants registrationDeadline matchesDeadline regulation');
    console.error('   or: node create-tournament.js <env> params.json');
    console.error('Where <env> is one of: dev, test, prod');
    process.exit(1);
  }

  const { name, maxParticipants, registrationDeadline, matchesDeadline, regulation } = params;
  if (!name || !maxParticipants || !registrationDeadline || !matchesDeadline || !regulation) {
    console.error('Missing required parameters.');
    process.exit(1);
  }

  const { Timestamp } = admin.firestore;
  const regDeadline   = Timestamp.fromDate(new Date(registrationDeadline));
  const matchDeadline = Timestamp.fromDate(new Date(matchesDeadline));

  const regSnap = await db.collection('regulations').doc(regulation).get();
  if (!regSnap.exists) {
    console.error('Regulation document not found:', regulation);
    process.exit(1);
  }
  if (regSnap.data().status !== 'active') {
    console.error('Regulation is not active:', regulation);
    process.exit(1);
  }

  try {
    const doc = await db.collection('tournaments').add({
      name,
      maxParticipants,
      registrationDeadline: regDeadline,
      matchesDeadline: matchDeadline,
      regulation,
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

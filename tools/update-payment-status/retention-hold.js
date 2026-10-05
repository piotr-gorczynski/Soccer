'use strict';
const { setHold } = require('../../firebase/functions/update-payment-status/retention');
async function main() {
  const [env, paymentId, action, reasonCode] = process.argv.slice(2);
  if (!['dev', 'test', 'prod'].includes(env) || !paymentId || paymentId.includes('/') ||
      !['hold', 'release'].includes(action)) {
    throw new Error('Usage: node retention-hold.js <dev|test|prod> <paymentId> hold <dispute|legal_obligation> OR ... release');
  }
  const admin = require('firebase-admin');
  const key = require(require('path').join(__dirname, '../../secrets', `serviceAccountKey.${env}.json`));
  const app = admin.initializeApp({ credential: admin.credential.cert(key) });
  try {
    await setHold(app.firestore(), paymentId, action === 'hold', action === 'release' ? 'released' : reasonCode,
      key.client_email, admin.firestore.Timestamp);
    console.log(`Retention ${action} recorded in ${env}.`);
  } finally { await app.delete(); }
}
if (require.main === module) main().catch(error => { console.error(error.message); process.exitCode = 1; });

'use strict';

const path = require('path');
const { assertTransition, validateTransitionData } = require('../../firebase/functions/update-payment-status/payment-workflow');

const ENVIRONMENTS = new Set(['dev', 'test', 'prod']);

function parseArgs(argv) {
  const [env, paymentId, status, ...rest] = argv;
  if (!ENVIRONMENTS.has(env) || !paymentId || !status) {
    throw new Error('Usage: node update-payment-status.js <dev|test|prod> <paymentId> <status> [options]');
  }
  const data = {};
  let dryRun = false;
  for (let index = 0; index < rest.length; index += 1) {
    const arg = rest[index];
    if (arg === '--dry-run') {
      dryRun = true;
      continue;
    }
    const valueOptions = {
      '--provider': 'provider',
      '--reference': 'providerReference',
      '--issue-code': 'issueCode',
      '--user-message': 'userMessage',
      '--notes': 'notes',
    };
    const key = valueOptions[arg];
    if (!key || index + 1 >= rest.length) throw new Error(`Invalid or incomplete option: ${arg}`);
    data[key] = rest[++index];
  }
  return { env, paymentId, status, data, dryRun };
}

function buildUpdate(status, data, FieldValue) {
  const update = {
    status,
    statusUpdatedAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  };
  if (status === 'processing') update.processingAt = FieldValue.serverTimestamp();
  if (status === 'sent') {
    update['transfer.provider'] = data.provider.trim();
    update['transfer.providerReference'] = data.providerReference.trim();
    update['transfer.sentAt'] = FieldValue.serverTimestamp();
  }
  if (status === 'completed') update['transfer.completedAt'] = FieldValue.serverTimestamp();
  if (status === 'action_required') {
    update.issue = {
      code: data.issueCode.trim(),
      userMessage: data.userMessage.trim(),
      createdAt: FieldValue.serverTimestamp(),
    };
  } else if (status === 'processing') {
    update.issue = FieldValue.delete();
  }
  if (data.notes) update.adminNotes = data.notes.trim();
  return update;
}

async function main() {
  const admin = require('firebase-admin');
  let args;
  try {
    args = parseArgs(process.argv.slice(2));
  } catch (error) {
    console.error(error.message);
    process.exit(1);
  }

  const serviceAccount = require(path.join(
    __dirname, '..', '..', 'secrets', `serviceAccountKey.${args.env}.json`));
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  const db = admin.firestore();
  const paymentRef = db.collection('payments').doc(args.paymentId);
  const paymentSnap = await paymentRef.get();
  if (!paymentSnap.exists) throw new Error(`Payment ${args.paymentId} does not exist.`);

  const currentStatus = paymentSnap.get('status');
  assertTransition(currentStatus, args.status);
  validateTransitionData(args.status, args.data);
  console.log(`Payment: ${args.paymentId}`);
  console.log(`Environment: ${args.env}`);
  console.log(`Transition: ${currentStatus} -> ${args.status}`);
  if (args.dryRun) {
    console.log('Dry run complete; no data was changed.');
    return;
  }

  const historyRef = paymentRef.collection('statusHistory').doc();
  await db.runTransaction(async transaction => {
    const latest = await transaction.get(paymentRef);
    const latestStatus = latest.get('status');
    assertTransition(latestStatus, args.status);
    transaction.update(paymentRef, buildUpdate(args.status, args.data, admin.firestore.FieldValue));
    transaction.set(historyRef, {
      from: latestStatus,
      to: args.status,
      changedAt: admin.firestore.FieldValue.serverTimestamp(),
      changedBy: 'admin-cli',
      source: 'tools/update-payment-status',
      ...(args.data.issueCode ? { reasonCode: args.data.issueCode } : {}),
    });
  });
  console.log('Payment status updated. A deployed onPaymentStatusChanged trigger will notify the winner.');
}

if (require.main === module) {
  main().catch(error => {
    console.error(`Failed to update payment: ${error.message}`);
    process.exit(1);
  });
}

module.exports = { parseArgs, buildUpdate };

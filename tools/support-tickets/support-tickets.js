'use strict';

const path = require('path');
const admin = require('firebase-admin');

const ENVIRONMENTS = new Set(['dev', 'test', 'prod']);

function usage() {
  return [
    'Usage:',
    '  node support-tickets.js <env> list [--status open]',
    '  node support-tickets.js <env> reply <ticketId> <message>',
    '  node support-tickets.js <env> resolve <ticketId> [message]',
  ].join('\n');
}

async function main() {
  const [env, command, ...args] = process.argv.slice(2);
  if (!ENVIRONMENTS.has(env) || !command) throw new Error(usage());
  const serviceAccount = require(path.join(
    __dirname, '..', '..', 'secrets', `serviceAccountKey.${env}.json`));
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  const db = admin.firestore();

  if (command === 'list') {
    const statusIndex = args.indexOf('--status');
    const requestedStatus = statusIndex >= 0 ? args[statusIndex + 1] : null;
    let query = db.collection('supportTickets');
    if (requestedStatus) query = query.where('status', '==', requestedStatus);
    const snapshot = await query.get();
    const tickets = snapshot.docs
      .sort((a, b) => (b.get('createdAt')?.toMillis() || 0) - (a.get('createdAt')?.toMillis() || 0));
    for (const ticket of tickets) {
      console.log(`${ticket.id}\t${ticket.get('reference')}\t${ticket.get('status')}\t` +
        `${ticket.get('category')}\tpayment=${ticket.get('paymentId')}`);
    }
    console.log(`${tickets.length} ticket(s).`);
    return;
  }

  const [ticketId, ...messageParts] = args;
  if (!ticketId || !['reply', 'resolve'].includes(command)) throw new Error(usage());
  const message = messageParts.join(' ').trim();
  if (command === 'reply' && !message) throw new Error('A reply message is required.');
  if (message.length > 2000) throw new Error('Reply is too long (maximum 2000 characters).');

  const ticketRef = db.collection('supportTickets').doc(ticketId);
  const ticket = await ticketRef.get();
  if (!ticket.exists) throw new Error(`Ticket ${ticketId} does not exist.`);
  const status = command === 'resolve' ? 'resolved' : 'waiting_for_user';
  const now = admin.firestore.FieldValue.serverTimestamp();
  const update = { status, updatedAt: now };
  if (message) {
    update.latestSupportReply = message;
    update.latestSupportReplyAt = now;
  }
  const messageRef = ticketRef.collection('messages').doc();
  const batch = db.batch();
  batch.update(ticketRef, update);
  if (message) batch.set(messageRef, {
    authorType: 'support', message, createdAt: now, source: 'tools/support-tickets',
  });
  await batch.commit();
  console.log(`${ticket.get('reference') || ticketId} updated to ${status}.`);
  console.log('The deployed support notification trigger will notify the user.');
}

main().catch(error => {
  console.error(`Support ticket operation failed: ${error.message}`);
  process.exit(1);
});

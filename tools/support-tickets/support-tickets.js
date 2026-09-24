'use strict';

const path = require('path');

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
  const admin = require('firebase-admin');
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
  const message = messageParts.join(' ');
  if (command === 'reply' && !message.trim()) throw new Error('A reply message is required.');
  if (message.length > 2000) throw new Error('Reply is too long (maximum 2000 characters).');

  const ticketRef = db.collection('supportTickets').doc(ticketId);
  const result = await updateTicket(db, ticketRef, command, message,
    serviceAccount.client_email, admin.firestore.FieldValue);
  console.log(`${result.reference || ticketId} updated to ${result.status}.`);
  console.log('The deployed support notification trigger will notify the user.');
}

async function updateTicket(db, ticketRef, command, message, actor, FieldValue) {
  if (!['reply', 'resolve'].includes(command)) throw new Error('Unknown support command.');
  if (typeof message !== 'string' || message.length > 2000
      || (command === 'reply' && !message.trim())) throw new Error('Invalid support message.');
  const status = command === 'resolve' ? 'resolved' : 'waiting_for_user';
  const now = FieldValue.serverTimestamp();
  const historyRef = ticketRef.collection('statusHistory').doc();
  const messageRef = ticketRef.collection('messages').doc();
  return db.runTransaction(async transaction => {
    const ticket = await transaction.get(ticketRef);
    if (!ticket.exists) throw new Error(`Ticket ${ticketRef.id} does not exist.`);
    const previousStatus = ticket.get('status');
    const update = { status, updatedAt: now };
    if (previousStatus !== status) update.statusUpdatedAt = now;
    if (command === 'resolve') update.resolvedAt = now;
    else if (previousStatus === 'resolved' || previousStatus === 'closed') {
      update.resolvedAt = FieldValue.delete();
    }
    if (message.trim()) {
      update.latestSupportReply = message.trim();
      update.latestSupportReplyAt = now;
      transaction.set(messageRef, {
        authorType: 'support', authorId: actor, message, createdAt: now,
        source: 'tools/support-tickets', historyId: historyRef.id,
      });
    }
    transaction.update(ticketRef, update);
    transaction.set(historyRef, {
      eventType: command === 'reply' ? 'support_reply' : 'ticket_resolved',
      from: previousStatus, to: status, changedAt: now,
      changedBy: actor, actorType: 'admin', source: 'tools/support-tickets',
      command, message, messageId: message.trim() ? messageRef.id : null,
    });
    return { status, reference: ticket.get('reference') };
  });
}

if (require.main === module) {
  main().catch(error => {
    console.error(`Support ticket operation failed: ${error.message}`);
    process.exit(1);
  });
}

module.exports = { updateTicket };

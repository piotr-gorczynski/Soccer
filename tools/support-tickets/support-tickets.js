'use strict';

const path = require('path');

const ENVIRONMENTS = new Set(['dev', 'test', 'prod']);

function formatReply(text, columns = process.stdout.columns || 80) {
  const width = Math.max(1, Math.min(80, columns) - 2);
  return String(text).split(/\r?\n/).map(paragraph => {
    const lines = [];
    let line = '';
    for (const word of paragraph.match(/\S+/gu) || []) {
      const letters = Array.from(word);
      if (line && Array.from(line).length + 1 + letters.length > width) {
        lines.push(line);
        line = '';
      }
      while (letters.length > width) lines.push(letters.splice(0, width).join(''));
      const remainder = letters.join('');
      if (remainder) line += (line ? ' ' : '') + remainder;
    }
    lines.push(line);
    return lines.map(value => `  ${value}`).join('\n');
  }).join('\n');
}

function usage() {
  return [
    'Usage:',
    '  node support-tickets.js <env> list [--status open]',
    '  node support-tickets.js <env> view <ticketId>',
    '  node support-tickets.js <env> reply <ticketId> <message>',
    '  node support-tickets.js <env> resolve <ticketId> [message]',
    '  node support-tickets.js <env> moderation-hold <ticketId> <on|off> <reason>',
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

  if (command === 'moderation-hold') {
    const [id, state, ...reason] = args;
    if (!id || !['on', 'off'].includes(state)) throw Error(usage());
    await require('../../firebase/functions/support-tickets/retention').setModerationHold(
      db, db.collection('supportTickets').doc(id), state === 'on', reason.join(' '),
      serviceAccount.client_email, admin.firestore.FieldValue);
    console.log('Moderation retention hold updated. Original resolution deadline unchanged.');
    return;
  }

  if (command === 'list') {
    const statusIndex = args.indexOf('--status');
    const requestedStatus = statusIndex >= 0 ? args[statusIndex + 1] : null;
    let query = db.collection('supportTickets');
    if (requestedStatus) query = query.where('status', '==', requestedStatus);
    const snapshot = await query.get();
    const tickets = snapshot.docs
      .sort((a, b) => (b.get('createdAt')?.toMillis() || 0) - (a.get('createdAt')?.toMillis() || 0));
    console.table(tickets.map(ticket => ({
      ticketId: ticket.id,
      reference: ticket.get('reference') || '',
      status: ticket.get('status') || '',
      category: ticket.get('category') || '',
      reporterUid: ticket.get('reporterUid') || '',
      paymentId: ticket.get('paymentId') || '',
      reportedUid: ticket.get('reportedUid') || '',
      nickname: ticket.get('nickname') || '',
      message: ticket.get('message') || '',
    })));
    // Keep long replies below the table so they remain readable in a terminal.
    for (const ticket of tickets) {
      const reply = ticket.get('latestSupportReply');
      if (!reply) continue;
      const sentAt = ticket.get('latestSupportReplyAt')?.toDate().toISOString();
      console.log(`\n${ticket.get('reference') || ticket.id}`);
      console.log(`Latest support reply${sentAt ? ` (${sentAt})` : ''}:`);
      console.log(formatReply(reply));
    }
    console.log(`${tickets.length} ticket(s).`);
    return;
  }

  const [ticketId, ...messageParts] = args;
  if (command === 'view') {
    if (!ticketId || !/^[A-Za-z0-9_-]{1,128}$/.test(ticketId)) throw new Error(usage());
    const { publicMessage } = require('../../firebase/functions/support-tickets/conversation');
    const ticket = await db.collection('supportTickets').doc(ticketId).get();
    if (!ticket.exists) throw new Error('Ticket not found.');
    const snapshots = await ticket.ref.collection('messages').orderBy('createdAt').get();
    const messages = snapshots.docs.map(publicMessage).filter(Boolean);
    if (!snapshots.docs.some(doc => doc.id === 'initial') && ticket.get('message')) {
      messages.unshift({ authorType: 'user', message: ticket.get('message'),
        createdAt: ticket.get('createdAt')?.toDate().toISOString() });
    }
    const latest = ticket.get('latestSupportReply');
    const latestTime = ticket.get('latestSupportReplyAt')?.toDate().toISOString();
    if (latest && !messages.some(message => message.authorType === 'support'
        && message.message.trim() === latest.trim() && message.createdAt === latestTime)) {
      messages.push({ authorType: 'support', message: latest, createdAt: latestTime });
    }
    messages.sort((a, b) => (Date.parse(a.createdAt) || 0) - (Date.parse(b.createdAt) || 0));
    console.log(`${ticket.get('reference') || ticketId} [${ticket.get('status')}]`);
    for (const message of messages) {
      console.log(`\n${message.authorType} (${message.createdAt || 'date unknown'}):`);
      console.log(formatReply(message.message));
    }
    return;
  }
  if (!ticketId || !['reply', 'resolve'].includes(command)) throw new Error(usage());
  const message = messageParts.join(' ');
  if (command === 'reply' && !message.trim()) throw new Error('A reply message is required.');
  if (message.length > 2000) throw new Error('Reply is too long (maximum 2000 characters).');

  const ticketRef = db.collection('supportTickets').doc(ticketId);
  const result = await updateTicket(db, ticketRef, command, message,
    serviceAccount.client_email, admin.firestore.FieldValue);
  console.log(`${result.reference || ticketId} updated to ${result.status}.`);
  console.log(result.type === 'moderation'
    ? 'Ticket updated. Player-report notifications require the Help Centre app update.'
    : 'The deployed support notification trigger will attempt to notify the user.');
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
    const paymentId = ticket.get('paymentId');
    const paymentRef = paymentId ? db.collection('payments').doc(paymentId) : null;
    const payment = paymentRef ? await transaction.get(paymentRef) : null;
    if (paymentRef && !payment.exists) throw new Error('Payment no longer exists.');
    if (ticket.get('retentionDeletionStartedAt')) throw Error('Report deletion already started.');
    const previousStatus = ticket.get('status');
    if (['resolved', 'closed'].includes(previousStatus)) {
      throw new Error(`Ticket ${ticketRef.id} is ${previousStatus}; reply/resolve cannot modify a finished ticket.`);
    }
    const legacyReply = ticket.get('latestSupportReply');
    let preserveLegacyReply = false;
    if (message.trim() && legacyReply && !Number.isSafeInteger(ticket.get('supportReplyVersion'))) {
      const timestamp = ticket.get('latestSupportReplyAt');
      const stored = timestamp ? await transaction.get(ticketRef.collection('messages')
        .where('createdAt', '==', timestamp)) : null;
      preserveLegacyReply = !stored?.docs.some(doc => doc.get('authorType') === 'support'
        && doc.get('message').trim() === legacyReply.trim());
    }
    const update = { status, updatedAt: now };
    if (previousStatus !== status) update.statusUpdatedAt = now;
    if (command === 'resolve') update.resolvedAt = now;
    if (message.trim()) {
      // Preserve the last old response before replacing the summary, without
      // inventing an author or timestamp or rewriting an existing message.
      if (preserveLegacyReply) transaction.set(ticketRef.collection('messages').doc('legacy_reply'), {
        authorType: 'support', message: legacyReply,
        createdAt: ticket.get('latestSupportReplyAt') || null, source: 'legacy_support_reply',
      });
      update.latestSupportReply = message.trim();
      update.latestSupportReplyAt = now;
      const { replyVersion } = require('../../firebase/functions/support-tickets/conversation');
      update.supportReplyVersion = replyVersion({
        supportReplyVersion: ticket.get('supportReplyVersion'), latestSupportReply: ticket.get('latestSupportReply'),
      }) + 1;
      transaction.set(messageRef, {
        authorType: 'support', authorId: actor, message, createdAt: now,
        supportReplyVersion: update.supportReplyVersion,
        source: 'tools/support-tickets', historyId: historyRef.id,
      });
    }
    if (paymentRef) transaction.update(paymentRef, { supportActivityAt: now });
    transaction.update(ticketRef, update);
    transaction.set(historyRef, {
      eventType: command === 'reply' ? 'support_reply' : 'ticket_resolved',
      from: previousStatus, to: status, changedAt: now,
      changedBy: actor, actorType: 'admin', source: 'tools/support-tickets',
      command, messageId: message.trim() ? messageRef.id : null,
    });
    return { status, reference: ticket.get('reference'), type: ticket.get('type') };
  });
}

if (require.main === module) {
  main().catch(error => {
    console.error(`Support ticket operation failed: ${error.message}`);
    process.exit(1);
  });
}

module.exports = { updateTicket, formatReply };

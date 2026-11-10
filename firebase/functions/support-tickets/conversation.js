'use strict';

const STATUSES = new Set(['open', 'waiting_for_user', 'resolved', 'closed']);
const id = value => typeof value === 'string' && /^[A-Za-z0-9_-]{1,128}$/.test(value);
const millis = value => typeof value?.toMillis === 'function' ? value.toMillis() : 0;
const iso = value => millis(value) ? new Date(millis(value)).toISOString() : null;

// Old tickets can have a last reply but no version field. Treat that reply as
// one unread message until explicitly read; no production migration is needed.
function replyVersion(data) {
  return Number.isSafeInteger(data.supportReplyVersion) && data.supportReplyVersion >= 0
    ? data.supportReplyVersion : (data.latestSupportReply ? 1 : 0);
}
function readVersion(data) {
  return Math.min(replyVersion(data), Number.isSafeInteger(data.userReadReplyVersion)
    && data.userReadReplyVersion >= 0 ? data.userReadReplyVersion : 0);
}
function publicTicket(snapshot) {
  const data = snapshot.data();
  const version = replyVersion(data);
  return {
    ticketId: snapshot.id, reference: data.reference || snapshot.id,
    category: data.category || 'other', status: data.status,
    createdAt: iso(data.createdAt), updatedAt: iso(data.updatedAt),
    latestSupportReply: data.latestSupportReply || '',
    latestSupportReplyAt: iso(data.latestSupportReplyAt),
    supportReplyVersion: version, unreadCount: version - readVersion(data),
    canReply: data.status !== 'closed' && !data.retentionDeletionStartedAt,
  };
}
function publicMessage(snapshot) {
  const data = snapshot.data();
  if (!['user', 'support'].includes(data.authorType) || typeof data.message !== 'string') return null;
  return { messageId: snapshot.id, authorType: data.authorType,
    message: data.message, createdAt: iso(data.createdAt) };
}

function handlers(db, FieldValue, HttpsError, clock = Date.now) {
  const fail = (code, message) => { throw new HttpsError(code, message); };
  function input(data, allowed) {
    if (!data || typeof data !== 'object' || Array.isArray(data)
        || Object.keys(data).some(key => !allowed.includes(key))) {
      fail('invalid-argument', 'Invalid request.');
    }
  }
  function caller(context) {
    if (!context.auth?.uid) fail('unauthenticated', 'Login required.');
    return context.auth.uid;
  }
  function active(user) {
    if (!user.exists || user.get('accountDeleted') === true) fail('permission-denied', 'Account unavailable.');
  }
  function owned(ticket, uid) {
    // Same response for missing tickets and another player's ticket.
    if (!ticket.exists || ticket.get('userId') !== uid) fail('not-found', 'Conversation unavailable.');
    if (ticket.get('retentionDeletionStartedAt')) fail('failed-precondition', 'Conversation unavailable.');
  }
  function ticketRef(data) {
    if (!id(data.ticketId)) fail('invalid-argument', 'Invalid conversation.');
    return db.collection('supportTickets').doc(data.ticketId);
  }
  async function paymentForWrite(tx, ticket) {
    const paymentId = ticket.get('paymentId');
    if (!paymentId) return null;
    const ref = db.collection('payments').doc(paymentId);
    const payment = await tx.get(ref);
    if (!payment.exists || payment.get('userId') !== ticket.get('userId')) {
      fail('failed-precondition', 'Payment unavailable.');
    }
    return ref;
  }

  async function listSupportConversations(data, context) {
    const uid = caller(context);
    input(data, ['cursor']);
    if (data.cursor != null && !id(data.cursor)) fail('invalid-argument', 'Invalid cursor.');
    active(await db.collection('users').doc(uid).get());
    // The owner index already exists. Read all summaries to keep the badge exact,
    // including legacy tickets; return only 25. Revisit aggregation at higher volume.
    const snapshot = await db.collection('supportTickets').where('userId', '==', uid).get();
    const docs = snapshot.docs.filter(doc => !doc.get('retentionDeletionStartedAt')
      && STATUSES.has(doc.get('status'))).sort((a, b) =>
      millis(b.get('updatedAt')) - millis(a.get('updatedAt')) || a.id.localeCompare(b.id));
    const unreadCount = docs.reduce((sum, doc) => sum + replyVersion(doc.data()) - readVersion(doc.data()), 0);
    const position = data.cursor ? docs.findIndex(doc => doc.id === data.cursor) : -1;
    if (data.cursor && position < 0) fail('invalid-argument', 'Invalid cursor.');
    const page = docs.slice(position + 1, position + 26);
    return { conversations: page.map(publicTicket), unreadCount,
      nextCursor: position + 26 < docs.length ? page[page.length - 1].id : null };
  }

  async function getSupportConversation(data, context) {
    const uid = caller(context);
    input(data, ['ticketId', 'cursor']);
    const ref = ticketRef(data);
    if (data.cursor != null && !id(data.cursor)) fail('invalid-argument', 'Invalid cursor.');
    active(await db.collection('users').doc(uid).get());
    const ticket = await ref.get();
    owned(ticket, uid);
    let query = ref.collection('messages').orderBy('createdAt', 'desc').orderBy('__name__', 'desc');
    if (data.cursor) {
      const cursor = await ref.collection('messages').doc(data.cursor).get();
      if (!cursor.exists) fail('invalid-argument', 'Invalid cursor.');
      query = query.startAfter(cursor);
    }
    const snapshot = await query.limit(51).get();
    const page = snapshot.docs.slice(0, 50);
    const messages = page.map(publicMessage).filter(Boolean);
    // Existing moderation reports did not create an initial message document.
    const initial = await ref.collection('messages').doc('initial').get();
    const initialMessage = !initial.exists && ticket.get('message') ? {
      messageId: 'legacy_initial', authorType: 'user', message: ticket.get('message'),
      createdAt: iso(ticket.get('createdAt')),
    } : null;
    // Some old administrator responses predate the messages collection.
    if (!data.cursor && ticket.get('latestSupportReply')) {
      const timestamp = ticket.get('latestSupportReplyAt');
      const candidates = timestamp ? await ref.collection('messages').where('createdAt', '==', timestamp).get() : null;
      if (!candidates?.docs.some(doc => doc.get('authorType') === 'support'
          && doc.get('message').trim() === ticket.get('latestSupportReply').trim())) {
        messages.push({ messageId: 'legacy_latest_reply', authorType: 'support',
          message: ticket.get('latestSupportReply'), createdAt: iso(timestamp) });
      }
    }
    messages.sort((a, b) => (Date.parse(a.createdAt) || 0) - (Date.parse(b.createdAt) || 0)
      || a.messageId.localeCompare(b.messageId));
    return { conversation: publicTicket(ticket), messages, initialMessage,
      nextCursor: snapshot.docs.length > 50 ? page[page.length - 1].id : null };
  }

  async function sendSupportMessage(data, context) {
    const uid = caller(context);
    input(data, ['ticketId', 'message', 'requestId', 'reopen']);
    const ref = ticketRef(data);
    if (typeof data.message !== 'string' || !data.message.trim() || data.message.length > 2000
        || !id(data.requestId) || data.requestId.length < 16 || data.requestId.length > 64
        || (data.reopen != null && typeof data.reopen !== 'boolean')) {
      fail('invalid-argument', 'A message (1–2000 characters) and request ID are required.');
    }
    const text = data.message.trim();
    return db.runTransaction(async tx => {
      const ownRef = db.collection('users').doc(uid);
      active(await tx.get(ownRef));
      const ticket = await tx.get(ref);
      owned(ticket, uid);
      const messageRef = ref.collection('messages').doc(`user_${data.requestId}`);
      const existing = await tx.get(messageRef);
      // A network retry never creates another message or reopens a later resolution.
      if (existing.exists) {
        if (existing.get('authorId') !== uid || existing.get('message') !== text
            || existing.get('reopen') !== (data.reopen === true)) {
          fail('invalid-argument', 'Request ID already used.');
        }
        return { ok: true, messageId: messageRef.id, alreadyExists: true };
      }
      const previous = ticket.get('status');
      if (previous === 'closed' || !STATUSES.has(previous)) fail('failed-precondition', 'Conversation is closed.');
      if (previous === 'resolved' && data.reopen !== true) fail('failed-precondition', 'Confirm reopening first.');
      const gateRef = ownRef.collection('supportState').doc('messageRate');
      const gate = await tx.get(gateRef);
      const paymentRef = await paymentForWrite(tx, ticket);
      const nowMs = clock();
      const hour = Math.floor(nowMs / 3600000), day = Math.floor(nowMs / 86400000);
      const hourly = gate.get('hour') === hour ? gate.get('hourlyCount') || 0 : 0;
      const daily = gate.get('day') === day ? gate.get('dailyCount') || 0 : 0;
      if (hourly >= 10 || daily >= 30 || nowMs - (gate.get('lastMessageAtMs') ?? -Infinity) < 5000) {
        fail('resource-exhausted', 'Please wait before sending another message.');
      }
      const now = FieldValue.serverTimestamp();
      const history = ref.collection('statusHistory').doc();
      tx.set(messageRef, { authorType: 'user', authorId: uid, message: text,
        createdAt: now, source: 'sendSupportMessage', historyId: history.id, reopen: data.reopen === true });
      tx.update(ref, { status: 'open', updatedAt: now,
        ...(previous !== 'open' ? { statusUpdatedAt: now } : {}) });
      tx.set(history, { eventType: previous === 'resolved' ? 'ticket_reopened' : 'user_reply',
        from: previous, to: 'open', changedAt: now, changedBy: uid, actorType: 'user',
        source: 'sendSupportMessage', messageId: messageRef.id });
      tx.set(gateRef, { hour, day, hourlyCount: hourly + 1, dailyCount: daily + 1, lastMessageAtMs: nowMs });
      if (paymentRef) tx.update(paymentRef, { supportActivityAt: now });
      return { ok: true, messageId: messageRef.id, alreadyExists: false };
    });
  }

  async function markSupportConversationRead(data, context) {
    const uid = caller(context);
    input(data, ['ticketId', 'seenReplyVersion']);
    const ref = ticketRef(data);
    if (!Number.isSafeInteger(data.seenReplyVersion) || data.seenReplyVersion < 0) {
      fail('invalid-argument', 'Invalid reply version.');
    }
    return db.runTransaction(async tx => {
      active(await tx.get(db.collection('users').doc(uid)));
      const ticket = await tx.get(ref);
      owned(ticket, uid);
      const version = replyVersion(ticket.data());
      if (data.seenReplyVersion > version) fail('invalid-argument', 'Invalid reply version.');
      const read = Math.max(readVersion(ticket.data()), data.seenReplyVersion);
      if (read > readVersion(ticket.data())) tx.update(ref, {
        userReadReplyVersion: read, userLastReadAt: FieldValue.serverTimestamp(),
      });
      return { ok: true, unreadCount: version - read };
    });
  }
  return { listSupportConversations, getSupportConversation, sendSupportMessage, markSupportConversationRead };
}

module.exports = { handlers, replyVersion, readVersion, publicTicket, publicMessage };

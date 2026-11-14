'use strict';

const functions = require('firebase-functions/v1');
const { getApps, initializeApp } = require('firebase-admin/app');
const { FieldValue, getFirestore } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const { validateCreateRequest, payoutContext } = require('./support-ticket');

if (!getApps().length) initializeApp();
const db = getFirestore();
const messaging = getMessaging();

const conversation = require('./conversation').handlers(db, FieldValue, functions.https.HttpsError);
for (const name of ['listSupportConversations', 'getSupportConversation',
  'sendSupportMessage', 'markSupportConversationRead']) {
  exports[name] = functions.region('us-central1').https.onCall(conversation[name]);
}

const attention = require('./attention').handlers(require('firebase-functions/logger'));
exports.onSupportTicketCreated = functions.region('us-central1').firestore
  .document('supportTickets/{ticketId}').onCreate(attention.ticketCreated);
exports.onSupportMessageCreated = functions.region('us-central1').firestore
  .document('supportTickets/{ticketId}/messages/{messageId}').onCreate(attention.messageCreated);

exports.createSupportTicket = functions.region('us-central1').https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Authentication required.');
  let input;
  try {
    input = validateCreateRequest(data);
  } catch (error) {
    throw new functions.https.HttpsError('invalid-argument', error.message);
  }

  return db.runTransaction(async transaction => {
    const paymentRef = db.collection('payments').doc(input.paymentId);
    const payment = await transaction.get(paymentRef);
    if (!payment.exists || payment.get('userId') !== context.auth.uid) {
      throw new functions.https.HttpsError('not-found', 'Payment was not found.');
    }

    const recipient = await transaction.get(paymentRef.collection('private').doc('recipient'));
    const tournament = payment.get('tournamentId')
      ? await transaction.get(db.collection('tournaments').doc(payment.get('tournamentId'))) : null;
    const { market, payoutMethod } = payoutContext(payment.data(), recipient.data(), tournament?.data());

    const existing = await transaction.get(db.collection('supportTickets')
      .where('userId', '==', context.auth.uid)
      .where('paymentId', '==', input.paymentId));
    const activeCount = existing.docs.filter(ticket =>
      !['resolved', 'closed'].includes(ticket.get('status'))).length;
    if (activeCount >= 3) {
      throw new functions.https.HttpsError(
        'resource-exhausted', 'You already have open support requests for this payment.');
    }

    const ticketRef = db.collection('supportTickets').doc();
    const reference = `SUP-${ticketRef.id.slice(0, 8).toUpperCase()}`;
    const now = FieldValue.serverTimestamp();
    const batch = transaction;
    batch.set(ticketRef, {
      reference,
      userId: context.auth.uid,
      paymentId: payment.id,
      tournamentId: payment.get('tournamentId') || '',
      market,
      category: input.category,
      message: input.message,
      status: 'open',
      createdAt: now,
      updatedAt: now,
      statusUpdatedAt: now,
      appContext: {
        appVersion: input.appVersion,
        appVariant: input.appVariant,
        locale: input.locale,
        validationErrorCode: input.validationErrorCode,
        paymentStatus: payment.get('status') || '',
        payoutMethod,
      },
    });
    batch.set(ticketRef.collection('statusHistory').doc('created'), {
      eventType: 'ticket_created', from: null, to: 'open', changedAt: now,
      changedBy: context.auth.uid, actorType: 'user', source: 'createSupportTicket',
      category: input.category, messageId: input.message ? 'initial' : null,
      paymentStatus: payment.get('status') || '',
    });
    if (input.message) batch.set(ticketRef.collection('messages').doc('initial'), {
      authorType: 'user', authorId: context.auth.uid, message: input.message,
      createdAt: now, source: 'createSupportTicket', historyId: 'created',
    });
    transaction.update(paymentRef, { supportActivityAt: now });
    return { ok: true, ticketId: ticketRef.id, reference };
  });
});

exports.onSupportTicketUpdated = functions.firestore
  .document('supportTickets/{ticketId}')
  .onUpdate(require('./notifications').handler(db, messaging, FieldValue,
    require('firebase-functions/logger')));

const moderation = require('./moderation').handlers(db, FieldValue, functions.https.HttpsError);
exports.reportPlayer = functions.region('us-central1').https.onCall(moderation.reportPlayer);
exports.setPlayerBlock = functions.region('us-central1').https.onCall(moderation.setPlayerBlock);

// Independent of payout retention.
exports.cleanupModerationAndLegalData = functions.region('us-central1')
  .runWith({ timeoutSeconds: 540, memory: '512MB' })
  .pubsub.schedule('30 3 * * *').timeZone('UTC').onRun(async () => {
    const summary = await require('./retention').sweep(db, new Date(), FieldValue);
    console.log('cleanupModerationAndLegalData summary', summary);
    return summary;
  });

'use strict';

const functions = require('firebase-functions/v1');
const { getApps, initializeApp } = require('firebase-admin/app');
const { FieldValue, getFirestore } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const { validateCreateRequest } = require('./support-ticket');

if (!getApps().length) initializeApp();
const db = getFirestore();
const messaging = getMessaging();

const SUPPORT_MESSAGES = {
  en: {
    reply: ['Support replied', 'You have a new reply to your payout support request.'],
    resolved: ['Support request resolved', 'Your payout support request has been resolved.'],
  },
  bn: {
    reply: ['সহায়তা দল উত্তর দিয়েছে', 'আপনার পেআউট সহায়তা অনুরোধে একটি নতুন উত্তর এসেছে।'],
    resolved: ['সহায়তা অনুরোধ সমাধান হয়েছে', 'আপনার পেআউট সহায়তা অনুরোধ সমাধান করা হয়েছে।'],
  },
  pl: {
    reply: ['Odpowiedź pomocy', 'Masz nową odpowiedź dotyczącą zgłoszenia wypłaty.'],
    resolved: ['Zgłoszenie rozwiązane', 'Twoje zgłoszenie dotyczące wypłaty zostało rozwiązane.'],
  },
};

exports.createSupportTicket = functions.region('us-central1').https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Authentication required.');
  let input;
  try {
    input = validateCreateRequest(data);
  } catch (error) {
    throw new functions.https.HttpsError('invalid-argument', error.message);
  }

  const paymentRef = db.collection('payments').doc(input.paymentId);
  const payment = await paymentRef.get();
  if (!payment.exists || payment.get('userId') !== context.auth.uid) {
    throw new functions.https.HttpsError('not-found', 'Payment was not found.');
  }

  const existing = await db.collection('supportTickets')
    .where('userId', '==', context.auth.uid)
    .where('paymentId', '==', input.paymentId)
    .get();
  const activeCount = existing.docs.filter(ticket =>
    !['resolved', 'closed'].includes(ticket.get('status'))).length;
  if (activeCount >= 3) {
    throw new functions.https.HttpsError(
      'resource-exhausted', 'You already have open support requests for this payment.');
  }

  const ticketRef = db.collection('supportTickets').doc();
  const reference = `SUP-${ticketRef.id.slice(0, 8).toUpperCase()}`;
  const now = FieldValue.serverTimestamp();
  const batch = db.batch();
  batch.set(ticketRef, {
    reference,
    userId: context.auth.uid,
    paymentId: payment.id,
    tournamentId: payment.get('tournamentId') || '',
    market: payment.get('market') || '',
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
      payoutMethod: payment.get('recipientInfo.method') || '',
    },
  });
  batch.set(ticketRef.collection('statusHistory').doc('created'), {
    eventType: 'ticket_created', from: null, to: 'open', changedAt: now,
    changedBy: context.auth.uid, actorType: 'user', source: 'createSupportTicket',
    category: input.category, message: input.message,
    paymentStatus: payment.get('status') || '',
  });
  if (input.message) batch.set(ticketRef.collection('messages').doc('initial'), {
    authorType: 'user', authorId: context.auth.uid, message: input.message,
    createdAt: now, source: 'createSupportTicket', historyId: 'created',
  });
  await batch.commit();
  return { ok: true, ticketId: ticketRef.id, reference };
});

exports.onSupportTicketUpdated = functions.firestore
  .document('supportTickets/{ticketId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    const hasNewReply = after.latestSupportReply &&
      after.latestSupportReply !== before.latestSupportReply;
    const wasResolved = ['resolved', 'closed'].includes(after.status) &&
      after.status !== before.status;
    if (!hasNewReply && !wasResolved) return null;

    const eventType = wasResolved ? 'resolved' : 'reply';
    const eventRef = change.after.ref.collection('notificationEvents')
      .doc(`${context.eventId}-${eventType}`);
    if ((await eventRef.get()).exists) return null;

    const userSnap = await db.collection('users').doc(after.userId).get();
    if (!userSnap.exists || userSnap.get('accountDeleted') === true) return null;
    const targetField = userSnap.get('fcmInstallationId') ? 'fcmInstallationId' : 'fcmToken';
    const targetValue = userSnap.get(targetField);
    if (!targetValue) return null;
    const language = userSnap.get('language') || 'en';
    const [title, body] = (SUPPORT_MESSAGES[language] || SUPPORT_MESSAGES.en)[eventType];

    try {
      await messaging.send({
        token: targetValue,
        data: {
          type: 'support_ticket_updated',
          ticketId: context.params.ticketId,
          paymentId: after.paymentId || '',
          tournamentId: after.tournamentId || '',
          status: after.status || '',
          title,
          body,
        },
        android: { priority: 'high' },
      });
      await eventRef.set({ type: eventType, sent: true, sentAt: FieldValue.serverTimestamp() });
    } catch (error) {
      if (['messaging/registration-token-not-registered',
        'messaging/installation-id-not-registered',
        'messaging/invalid-registration-token'].includes(error.code)) {
        await userSnap.ref.update({
          [targetField]: FieldValue.delete(),
          fcmErrorType: error.code,
          fcmErrorDate: FieldValue.serverTimestamp(),
        });
        return null;
      }
      throw error;
    }
    return null;
  });

'use strict';

const functions = require('firebase-functions/v1');
const { getApps, initializeApp } = require('firebase-admin/app');
const { FieldValue, getFirestore } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const { VALID_STATUSES, NOTIFIABLE_STATUSES, assertTransition, validateTransitionData } = require('./payment-workflow');

if (!getApps().length) initializeApp();
const db = getFirestore();
const messaging = getMessaging();

const PAYMENT_MESSAGES = {
  en: {
    processing: ['Prize payment update', 'Your prize payment is now being processed.'],
    sent: ['Prize payment update', 'Your prize money has been sent and is on its way.'],
    completed: ['Prize payment delivered', 'Your prize payment has been delivered.'],
    action_required: ['Payment details need attention', 'Open the app and correct your payout details.'],
    cancelled: ['Prize payment cancelled', 'Your prize payment has been cancelled.'],
  },
  bn: {
    processing: ['পুরস্কারের পেমেন্ট আপডেট', 'আপনার পুরস্কারের পেমেন্ট এখন প্রক্রিয়াধীন।'],
    sent: ['পুরস্কারের পেমেন্ট আপডেট', 'আপনার পুরস্কারের অর্থ পাঠানো হয়েছে এবং পৌঁছানোর পথে রয়েছে।'],
    completed: ['পুরস্কারের পেমেন্ট পৌঁছেছে', 'আপনার পুরস্কারের পেমেন্ট পৌঁছে দেওয়া হয়েছে।'],
    action_required: ['পেমেন্টের তথ্যে সংশোধন প্রয়োজন', 'অ্যাপ খুলে আপনার পেমেন্টের তথ্য সংশোধন করুন।'],
    cancelled: ['পুরস্কারের পেমেন্ট বাতিল', 'আপনার পুরস্কারের পেমেন্ট বাতিল করা হয়েছে।'],
  },
  pl: {
    processing: ['Aktualizacja wypłaty nagrody', 'Twoja wypłata nagrody jest teraz realizowana.'],
    sent: ['Aktualizacja wypłaty nagrody', 'Pieniądze zostały wysłane i są w drodze.'],
    completed: ['Nagroda została wypłacona', 'Wypłata nagrody została dostarczona.'],
    action_required: ['Dane do wypłaty wymagają poprawy', 'Otwórz aplikację i popraw dane do wypłaty.'],
    cancelled: ['Wypłata nagrody anulowana', 'Twoja wypłata nagrody została anulowana.'],
  },
};

function buildPaymentUpdate(status, data) {
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
  } else if (data.clearIssue === true || status === 'processing') {
    update.issue = FieldValue.delete();
  }
  if (typeof data.notes === 'string' && data.notes.trim()) update.adminNotes = data.notes.trim();
  return update;
}

async function updatePayment(paymentId, status, data, actor) {
  const paymentRef = db.collection('payments').doc(paymentId);
  const historyRef = paymentRef.collection('statusHistory').doc();
  return db.runTransaction(async transaction => {
    const paymentSnap = await transaction.get(paymentRef);
    if (!paymentSnap.exists) throw new Error(`Payment record ${paymentId} not found.`);
    const currentStatus = paymentSnap.get('status');
    assertTransition(currentStatus, status);
    validateTransitionData(status, data);
    transaction.update(paymentRef, buildPaymentUpdate(status, data));
    transaction.set(historyRef, {
      from: currentStatus,
      to: status,
      changedAt: FieldValue.serverTimestamp(),
      changedBy: actor,
      source: 'updatePaymentStatus',
      ...(data.issueCode ? { reasonCode: data.issueCode.trim() } : {}),
    });
    return { previousStatus: currentStatus };
  });
}

exports.updatePaymentStatus = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Authentication required.');
  if (context.auth.token.admin !== true) {
    throw new functions.https.HttpsError('permission-denied', 'Caller must have admin privileges.');
  }
  const { paymentId, status } = data || {};
  if (typeof paymentId !== 'string' || !paymentId.trim()) {
    throw new functions.https.HttpsError('invalid-argument', '`paymentId` is required.');
  }
  if (!VALID_STATUSES.has(status)) {
    throw new functions.https.HttpsError('invalid-argument', `Unknown payment status: ${status}.`);
  }
  try {
    const result = await updatePayment(paymentId.trim(), status, data, context.auth.uid);
    console.log(`[updatePaymentStatus] ${paymentId}: ${result.previousStatus} -> ${status}`);
    return { ok: true, paymentId, status };
  } catch (error) {
    const code = /not found/.test(error.message) ? 'not-found'
      : /Cannot transition/.test(error.message) ? 'failed-precondition' : 'invalid-argument';
    throw new functions.https.HttpsError(code, error.message);
  }
});

exports.onPaymentStatusChanged = functions.firestore
  .document('payments/{paymentId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    if (before.status === after.status || !NOTIFIABLE_STATUSES.has(after.status)) return null;

    const eventRef = change.after.ref.collection('notificationEvents').doc(context.eventId);
    const eventSnap = await eventRef.get();
    if (eventSnap.exists && eventSnap.get('sent') === true) return null;

    const userSnap = await db.collection('users').doc(after.userId).get();
    if (!userSnap.exists) return null;
    const user = userSnap.data();
    const targetField = user.fcmInstallationId ? 'fcmInstallationId' : 'fcmToken';
    const targetValue = user[targetField];
    if (user.accountDeleted === true || !targetValue) {
      console.log(`[onPaymentStatusChanged] Skipped ${after.userId}: no FCM target`);
      return null;
    }

    const messages = PAYMENT_MESSAGES[user.language] || PAYMENT_MESSAGES.en;
    const [title, body] = messages[after.status] || PAYMENT_MESSAGES.en[after.status];
    const message = {
      // The Admin SDK uses the `token` transport field for both legacy
      // registration tokens and FID-based per-installation targets.
      token: targetValue,
      data: {
        type: 'payment_status_changed',
        paymentId: context.params.paymentId,
        tournamentId: after.tournamentId || '',
        status: after.status,
        title,
        body,
      },
      android: { priority: 'high' },
    };

    try {
      await messaging.send(message);
      await eventRef.set({ status: after.status, sent: true, sentAt: FieldValue.serverTimestamp() });
      console.log(`[onPaymentStatusChanged] Sent ${after.status} to ${after.userId}`);
    } catch (error) {
      if (error.code === 'messaging/registration-token-not-registered' ||
          error.code === 'messaging/installation-id-not-registered' ||
          error.code === 'messaging/invalid-registration-token') {
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

exports._test = { buildPaymentUpdate, updatePayment };

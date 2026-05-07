// functions/update-payment-status/index.js
const functions = require('firebase-functions');
const admin     = require('firebase-admin');

// Only initialize if not already initialized
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

/**
 * Valid payment status values and the transitions allowed.
 * pending → processing → completed | failed
 */
const VALID_STATUSES = new Set(['pending', 'processing', 'completed', 'failed']);
const ALLOWED_TRANSITIONS = {
  pending:    new Set(['processing', 'failed']),
  processing: new Set(['completed', 'failed']),
  completed:  new Set(),   // terminal state
  failed:     new Set(['pending']),  // allow retry
};

/**
 * updatePaymentStatus – admin callable function.
 *
 * Expected data:
 *   {
 *     paymentId:       string   (required)
 *     status:          string   (required) – "processing" | "completed" | "failed"
 *     transferService: string   (optional) – "remitly" | "wise" | "western_union" | "paypal"
 *     transactionId:   string   (optional) – ID from the transfer service
 *     transferFee:     number   (optional) – fee in USD
 *     exchangeRate:    number   (optional) – exchange rate used
 *     notes:           string   (optional) – free-text notes for internal use
 *   }
 *
 * This function is intended to be called by the developer/admin only.
 * It requires authentication (any signed-in user) as a baseline guard;
 * additional access control should be enforced at the deployment level.
 */
exports.updatePaymentStatus = functions.https.onCall(async (data, context) => {
  // Require authentication.
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Authentication required.'
    );
  }

  // Restrict to admin users only (custom claim: admin === true).
  if (context.auth.token.admin !== true) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Caller must have admin privileges.'
    );
  }

  const { paymentId, status, transferService, transactionId, transferFee, exchangeRate, notes } = data || {};

  // Validate required fields.
  if (!paymentId || typeof paymentId !== 'string') {
    throw new functions.https.HttpsError(
      'invalid-argument',
      '`paymentId` is required and must be a string.'
    );
  }

  if (!status || !VALID_STATUSES.has(status)) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      `\`status\` must be one of: ${[...VALID_STATUSES].join(', ')}.`
    );
  }

  // Validate optional numeric fields.
  if (transferFee !== undefined && (typeof transferFee !== 'number' || transferFee < 0)) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      '`transferFee` must be a non-negative number.'
    );
  }

  if (exchangeRate !== undefined && (typeof exchangeRate !== 'number' || exchangeRate <= 0)) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      '`exchangeRate` must be a positive number.'
    );
  }

  const paymentRef = db.collection('payments').doc(paymentId);
  const paymentSnap = await paymentRef.get();

  if (!paymentSnap.exists) {
    throw new functions.https.HttpsError(
      'not-found',
      `Payment record ${paymentId} not found.`
    );
  }

  const currentStatus = paymentSnap.data().status;

  // Enforce valid status transitions.
  const allowed = ALLOWED_TRANSITIONS[currentStatus];
  if (!allowed || !allowed.has(status)) {
    throw new functions.https.HttpsError(
      'failed-precondition',
      `Cannot transition payment from "${currentStatus}" to "${status}".`
    );
  }

  // Build the update payload.
  const update = {
    status,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  if (transferService !== undefined) update.transferService = transferService;
  if (transactionId   !== undefined) update.transactionId   = transactionId;
  if (transferFee     !== undefined) update.transferFee     = transferFee;
  if (exchangeRate    !== undefined) update.exchangeRate    = exchangeRate;
  if (notes           !== undefined) update.notes           = notes;

  // Set timestamp fields based on target status.
  if (status === 'processing') {
    update.processedAt = admin.firestore.FieldValue.serverTimestamp();
  } else if (status === 'completed') {
    update.completedAt = admin.firestore.FieldValue.serverTimestamp();
  }

  await paymentRef.update(update);

  console.log(
    `[updatePaymentStatus] Payment ${paymentId} updated: ${currentStatus} → ${status}`
  );

  return { ok: true, paymentId, status };
});

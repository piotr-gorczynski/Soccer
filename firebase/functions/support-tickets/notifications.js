'use strict';

const { replyVersion } = require('./conversation');
const INVALID_TOKENS = new Set(['messaging/registration-token-not-registered',
  'messaging/installation-id-not-registered', 'messaging/invalid-registration-token']);

// No ticket body, identity or payout details belong in a lock-screen payload.
function handler(db, messaging, FieldValue, logger) {
  return async (change, context) => {
    const before = change.before.data(), after = change.after.data();
    const version = replyVersion(after);
    if (!after.latestSupportReply || (version === replyVersion(before)
      && after.latestSupportReply === before.latestSupportReply)) return null;
    const eventRef = change.after.ref.collection('notificationEvents').doc(`reply-${version}`);
    const claimed = await db.runTransaction(async tx => {
      const event = await tx.get(eventRef);
      if (event.exists && (event.get('sent') || event.get('terminal')
        || event.get('leaseUntil') > Date.now())) return false;
      tx.set(eventRef, { sent: false, leaseUntil: Date.now() + 300000,
        type: 'reply', replyVersion: version }, { merge: true });
      return true;
    });
    if (!claimed) return null;
    const finish = async (outcome, extra = {}) => {
      await eventRef.set({ outcome, leaseUntil: 0, ...extra }, { merge: true });
      logger.info('Support reply notification', { event: 'support_reply_notification', outcome, ticketId: context.params.ticketId, replyVersion: version });
    };
    let attemptedField, attemptedToken;
    try {
      const user = await db.collection('users').doc(after.userId).get();
      if (!user.exists || user.get('accountDeleted') === true) {
        await finish('account_unavailable', { terminal: true }); return null;
      }
      const field = user.get('fcmInstallationId') ? 'fcmInstallationId' : 'fcmToken';
      const token = user.get(field);
      if (!token) { await finish('no_token', { terminal: true }); return null; }
      attemptedField = field; attemptedToken = token;
      await messaging.send({ token, data: {
        type: 'support_ticket_updated', destination: 'help_centre',
        ticketId: context.params.ticketId, replyVersion: String(version),
        // Preserve routing for already released payout clients. Old moderation
        // clients safely ignore a push without tournamentId.
        paymentId: after.paymentId || '', tournamentId: after.tournamentId || '',
        title: 'Help Centre', body: 'You have a new reply in the Help Centre.',
      }, android: { priority: 'high' } });
      await finish('sent', { sent: true, sentAt: FieldValue.serverTimestamp() });
    } catch (error) {
      if (INVALID_TOKENS.has(error.code)) {
        // Compare against the attempted token, never remove a replacement token.
        await db.runTransaction(async tx => {
          const userRef = db.collection('users').doc(after.userId);
          const current = await tx.get(userRef);
          if (current.exists && current.get('accountDeleted') !== true
            && current.get(attemptedField) === attemptedToken) tx.update(userRef, {
              [attemptedField]: FieldValue.delete(), fcmErrorType: error.code,
              fcmErrorDate: FieldValue.serverTimestamp(),
            });
        });
        await finish('invalid_token', { terminal: true }); return null;
      }
      await finish('send_failed');
      throw error;
    }
    return null;
  };
}
module.exports = { handler };

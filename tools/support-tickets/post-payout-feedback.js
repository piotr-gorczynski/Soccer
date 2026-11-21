'use strict';
const { destination, facebookUrl } = require('../../firebase/functions/support-tickets/feedback-links');
const templates = require('../../firebase/functions/support-tickets/feedback-templates.json');
const validId = value => typeof value === 'string' && /^[A-Za-z0-9_-]{1,128}$/.test(value);
function messageFor(tournament, language, link) {
  const name = typeof tournament.name === 'string'
    ? tournament.name.replace(/[\r\n\u0000-\u001f]/g, ' ').trim().slice(0, 150) : 'the tournament';
  return templates[language].replace('{tournament}', name || 'the tournament').replace('{url}', link.url);
}
async function send(db, paymentId, actor, FieldValue, { force = false, dryRun = false, language } = {}) {
  if (!validId(paymentId) || (language && !['en', 'bn'].includes(language))) throw Error('Invalid payment ID or language.');
  // Stable conversation ID gives concurrent admin commands the same transaction conflict key.
  const key = require('crypto').createHash('sha256').update(paymentId).digest('hex');
  const ticketRef = db.collection('supportTickets').doc(`feedback_${key}`);
  const messageRef = ticketRef.collection('messages').doc();
  return db.runTransaction(async tx => {
    const payment = await tx.get(db.collection('payments').doc(paymentId));
    if (!payment.exists || payment.get('status') !== 'completed') throw Error('Payout must be completed.');
    const uid = payment.get('userId'), tournamentId = payment.get('tournamentId');
    if (!validId(uid) || !validId(tournamentId)) throw Error('Payout has no valid player/tournament.');
    const tournament = await tx.get(db.collection('tournaments').doc(tournamentId));
    const user = await tx.get(db.collection('users').doc(uid));
    const ticket = await tx.get(ticketRef);
    if (!tournament.exists || !tournament.get('visibleInFlavours')?.includes('bangladesh')
        || payment.get('currency') !== 'BDT' || (payment.get('market') && payment.get('market') !== 'BD'))
      throw Error('Bangladesh BDT payouts only.');
    if (!user.exists || user.get('accountDeleted') === true || user.get('accountRemoved') === true)
      throw Error('Player unavailable.');
    if (payment.get('retention')?.auditDeletedAt || ticket.get('retentionDeletionStartedAt')) throw Error('Record unavailable.');
    if (ticket.exists && (ticket.get('userId') !== uid || ticket.get('paymentId') !== paymentId
        || ticket.get('type') !== 'post_payout_feedback')) throw Error('Conversation mismatch.');
    if (ticket.exists && !force) throw Error('Feedback message already sent; use --force only for an intentional repeat.');
    const link = destination(tournament.data());
    const locale = language || (/^bn(?:[-_]|$)/i.test(user.get('language') || user.get('languageCode') || '') ? 'bn' : 'en');
    const message = messageFor(tournament.data(), locale, link);
    if (message.length > 2000) throw Error('Message too long.');
    if (dryRun) return { dryRun: true, message, destinationType: link.type, language: locale };
    const version = require('../../firebase/functions/support-tickets/conversation').replyVersion(ticket.data() || {}) + 1;
    const now = FieldValue.serverTimestamp();
    const summary = { latestSupportReply: message, latestSupportReplyAt: now,
      supportReplyVersion: version, updatedAt: now };
    if (ticket.exists) tx.update(ticketRef, summary);
    else tx.set(ticketRef, { ...summary, userId: uid, paymentId, tournamentId, market: 'BD',
      type: 'post_payout_feedback', category: 'other', reference: `SUP-${ticketRef.id.slice(-8).toUpperCase()}`,
      message: '', status: 'resolved', createdAt: now, resolvedAt: now, statusUpdatedAt: now, userReadReplyVersion: 0 });
    tx.set(messageRef, { authorType: 'support', authorId: actor, message, createdAt: now,
      supportReplyVersion: version, source: 'tools/post-payout-feedback', kind: 'post_payout_feedback',
      feedbackUrl: link.url, feedbackDestinationType: link.type, templateLanguage: locale, templateVersion: 1 });
    tx.set(ticketRef.collection('statusHistory').doc(), { eventType: 'post_payout_feedback_sent',
      changedAt: now, changedBy: actor, actorType: 'admin', source: 'tools/post-payout-feedback',
      messageId: messageRef.id, supportReplyVersion: version });
    return { ticketId: ticketRef.id, version, destinationType: link.type, language: locale };
  });
}
async function configure(db, tournamentId, url, actor, FieldValue) {
  if (!validId(tournamentId) || (url !== 'clear' && !facebookUrl(url, true))) throw Error('Invalid tournament or Facebook post URL.');
  const ref = db.collection('tournaments').doc(tournamentId);
  return db.runTransaction(async tx => {
    const tournament = await tx.get(ref);
    if (!tournament.exists || !tournament.get('visibleInFlavours')?.includes('bangladesh')) throw Error('Bangladesh tournament required.');
    tx.update(ref, { postPayoutFacebookPostUrl: url === 'clear' ? FieldValue.delete() : facebookUrl(url, true) });
    tx.set(ref.collection('adminHistory').doc(), { eventType: 'feedback_destination_configured',
      changedBy: actor, changedAt: FieldValue.serverTimestamp(), destinationType: url === 'clear' ? 'community' : 'tournament_post' });
  });
}
module.exports = { send, configure, messageFor };

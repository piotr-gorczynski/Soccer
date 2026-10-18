'use strict';

// Firestore creation events run after commit; never log caller-controlled content.
function handlers(logger) {
  function emit(reason, context) {
    logger.info('Support requires attention', {
      event: 'support_attention_required',
      reason,
      ticketId: context.params.ticketId,
      eventId: context.eventId,
    });
    return null;
  }

  return {
    ticketCreated: async (snapshot, context) => {
      if (!snapshot.exists || ['resolved', 'closed'].includes(snapshot.get('status'))) return null;
      return emit('ticket_created', context);
    },
    messageCreated: async (snapshot, context) => {
      // The initial payout message is already covered by ticketCreated.
      if (!snapshot.exists || context.params.messageId === 'initial'
          || snapshot.get('authorType') !== 'user') return null;
      const ticket = await snapshot.ref.parent.parent.get();
      if (!ticket.exists || ['resolved', 'closed'].includes(ticket.get('status'))
          || !ticket.get('userId') || snapshot.get('authorId') !== ticket.get('userId')) return null;
      return emit('user_reply', context);
    },
  };
}

module.exports = { handlers };

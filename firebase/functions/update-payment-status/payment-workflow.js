'use strict';

const VALID_STATUSES = new Set([
  'awaiting_details',
  'ready_for_processing',
  'processing',
  'sent',
  'completed',
  'action_required',
  'cancelled',
]);

const ALLOWED_TRANSITIONS = {
  awaiting_details: new Set(['cancelled']),
  ready_for_processing: new Set(['processing', 'action_required', 'cancelled']),
  processing: new Set(['sent', 'action_required', 'cancelled']),
  sent: new Set(['completed', 'action_required']),
  completed: new Set(),
  action_required: new Set(['cancelled']),
  cancelled: new Set(),
};

const NOTIFIABLE_STATUSES = new Set([
  'processing',
  'sent',
  'completed',
  'action_required',
  'cancelled',
]);

function assertTransition(currentStatus, targetStatus) {
  if (!VALID_STATUSES.has(targetStatus)) {
    throw new Error(`Unknown target payment status: ${targetStatus}.`);
  }
  const allowed = ALLOWED_TRANSITIONS[currentStatus];
  if (!allowed || !allowed.has(targetStatus)) {
    throw new Error(`Cannot transition payment from "${currentStatus}" to "${targetStatus}".`);
  }
}

function validateTransitionData(targetStatus, data = {}) {
  if (targetStatus === 'sent') {
    if (typeof data.provider !== 'string' || !data.provider.trim()) {
      throw new Error('`provider` is required when marking a payment as sent.');
    }
    if (typeof data.providerReference !== 'string' || !data.providerReference.trim()) {
      throw new Error('`providerReference` is required when marking a payment as sent.');
    }
  }
  if (targetStatus === 'action_required') {
    if (typeof data.issueCode !== 'string' || !data.issueCode.trim()) {
      throw new Error('`issueCode` is required when action is required.');
    }
    if (typeof data.userMessage !== 'string' || !data.userMessage.trim()) {
      throw new Error('`userMessage` is required when action is required.');
    }
  }
}

module.exports = {
  VALID_STATUSES,
  ALLOWED_TRANSITIONS,
  NOTIFIABLE_STATUSES,
  assertTransition,
  validateTransitionData,
};

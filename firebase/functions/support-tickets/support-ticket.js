'use strict';

const VALID_CATEGORIES = new Set([
  'validation_rejected',
  'payout_method_unavailable',
  'payment_not_received',
  'other',
]);

function cleanOptionalText(value, maxLength, fieldName) {
  if (value == null) return '';
  if (typeof value !== 'string') throw new Error(`\`${fieldName}\` must be text.`);
  const clean = value.trim();
  if (clean.length > maxLength) throw new Error(`\`${fieldName}\` is too long.`);
  return clean;
}

function validateCreateRequest(data) {
  if (!data || typeof data.paymentId !== 'string' || !data.paymentId.trim()) {
    throw new Error('`paymentId` is required.');
  }
  if (!VALID_CATEGORIES.has(data.category)) throw new Error('Unknown support category.');
  return {
    paymentId: data.paymentId.trim(),
    category: data.category,
    message: cleanOptionalText(data.message, 1000, 'message'),
    appVersion: cleanOptionalText(data.appVersion, 50, 'appVersion'),
    appVariant: cleanOptionalText(data.appVariant, 80, 'appVariant'),
    locale: cleanOptionalText(data.locale, 35, 'locale'),
    validationErrorCode: cleanOptionalText(data.validationErrorCode, 100, 'validationErrorCode'),
  };
}

module.exports = { VALID_CATEGORIES, validateCreateRequest };

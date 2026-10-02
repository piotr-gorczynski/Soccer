const { test, before, after, beforeEach } = require('node:test');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { doc, setDoc, updateDoc, getDoc, writeBatch, serverTimestamp, deleteField } = require('firebase/firestore');
const fs = require('node:fs');
let env;
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-payout', firestore: {
    host: '127.0.0.1', port: 8188, rules: fs.readFileSync('../../firebase/firestore.rules', 'utf8'),
  }});
});
after(async () => { if (env) await env.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'regulations/r'), { prizeRules: { payoutMethods: ['bkash', 'rocket', 'nagad'] }});
    await setDoc(doc(db, 'tournaments/t'), { regulation: 'r' });
    await setDoc(doc(db, 'payments/p'), { userId: 'winner', rank: 1, tournamentId: 't', status: 'awaiting_details' });
  });
});
const details = () => ({ firstName: 'Test', lastName: 'Recipient', walletProvider: 'BKASH', walletNumber: '01712345678', submittedAt: serverTimestamp() });
function submit(info, user = 'winner') {
  const db = env.authenticatedContext(user).firestore();
  const batch = writeBatch(db);
  batch.set(doc(db, 'payments/p/private/recipient'), info);
  batch.update(doc(db, 'payments/p'), { status: 'ready_for_processing', recipientDetailsVersion: 2,
    statusUpdatedAt: serverTimestamp(), updatedAt: serverTimestamp(), recipientInfo: deleteField(), issue: deleteField() });
  return batch.commit();
}
test('accepts bKash and Rocket atomic submissions', async () => {
  await assertSucceeds(submit(details()));
  await env.withSecurityRulesDisabled(ctx => updateDoc(doc(ctx.firestore(), 'payments/p'), { status: 'action_required' }));
  await assertSucceeds(submit({ ...details(), walletProvider: 'ROCKET', walletNumber: '017123456789' }));
});
for (const field of ['firstName', 'lastName', 'walletProvider', 'walletNumber']) {
  test('rejects missing or blank ' + field, async () => {
    const missing = details(); delete missing[field];
    await assertFails(submit(missing));
    await assertFails(submit({ ...details(), [field]: ' ' }));
  });
}
test('rejects Nagad even when legacy regulation offers it', async () => {
  await assertFails(submit({ ...details(), walletProvider: 'NAGAD' }));
});
test('rejects unsupported method, invalid number, and extra recipient fields', async () => {
  await assertFails(submit({ ...details(), walletProvider: 'BANK' }));
  await assertFails(submit({ ...details(), walletNumber: '01212345678' }));
  await assertFails(submit({ ...details(), name: 'Combined name' }));
});
test('requires regulation support', async () => {
  await env.withSecurityRulesDisabled(ctx => updateDoc(doc(ctx.firestore(), 'regulations/r'), { 'prizeRules.payoutMethods': ['rocket'] }));
  await assertFails(submit(details()));
});
test('enforces owner access and denies anonymous reads', async () => {
  await assertFails(submit(details(), 'other'));
  await assertSucceeds(submit(details()));
  await assertSucceeds(getDoc(doc(env.authenticatedContext('winner').firestore(), 'payments/p/private/recipient')));
  await assertFails(getDoc(doc(env.authenticatedContext('other').firestore(), 'payments/p/private/recipient')));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'payments/p/private/recipient')));
});
test('rejects standalone details, legacy submissions, and edits after submission', async () => {
  const db = env.authenticatedContext('winner').firestore();
  await assertFails(setDoc(doc(db, 'payments/p/private/recipient'), details()));
  await assertFails(updateDoc(doc(db, 'payments/p'), { status: 'ready_for_processing', recipientInfo: { method: 'bkash', accountNumber: '01712345678', submittedAt: serverTimestamp() }, statusUpdatedAt: serverTimestamp() }));
  await assertSucceeds(submit(details()));
  await assertFails(submit(details()));
});

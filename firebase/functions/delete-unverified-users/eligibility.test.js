const test = require('node:test');
const assert = require('node:assert/strict');
const { shouldDeleteUnverifiedUser, deleteIfStillEligible } = require('./eligibility');

const firestore = profile => ({ collection: () => ({ doc: () => ({ get: async () => ({ exists: !!profile, data: () => profile }) }) }) });
const now = Date.parse('2026-10-22T10:00:00Z');
const user = overrides => ({
  uid: 'test-user', email: 'test@example.invalid', emailVerified: false,
  providerData: [{ providerId: 'password' }],
  metadata: { creationTime: '2026-10-22T08:00:00Z' }, ...overrides
});

test('only old unverified password-only accounts qualify, without a Firestore profile', () => {
  assert.equal(shouldDeleteUnverifiedUser(user(), now), true);
  assert.equal(shouldDeleteUnverifiedUser(user({ emailVerified: true }), now), false);
});

test('anonymous accounts never qualify, even with missing or misleading profile method', () => {
  for (const method of [undefined, 'password', 'anonymous']) {
    assert.equal(shouldDeleteUnverifiedUser(user({ providerData: [], email: undefined, method }), now), false);
    assert.equal(shouldDeleteUnverifiedUser(user({ providerData: [], method }), now), false);
  }
});

test('OAuth, phone, custom and linked accounts do not qualify', () => {
  for (const providerId of ['google.com', 'facebook.com', 'microsoft.com', 'phone']) {
    assert.equal(shouldDeleteUnverifiedUser(user({ providerData: [{ providerId }] }), now), false);
    assert.equal(shouldDeleteUnverifiedUser(user({ providerData: [{ providerId: 'password' }, { providerId }] }), now), false);
  }
  assert.equal(shouldDeleteUnverifiedUser(user({ providerData: undefined }), now), false);
});

test('missing email, unknown verification/provider data and invalid dates fail closed', () => {
  for (const overrides of [
    { email: '' }, { email: undefined }, { emailVerified: undefined },
    { providerData: [null] }, { metadata: {} },
    { metadata: { creationTime: 'invalid' } },
    { metadata: { creationTime: '2026-10-22T11:00:00Z' } }
  ]) assert.equal(shouldDeleteUnverifiedUser(user(overrides), now), false);
});

test('one hour boundary and younger accounts are retained', () => {
  assert.equal(shouldDeleteUnverifiedUser(user({ metadata: { creationTime: '2026-10-22T09:00:00Z' } }), now), false);
  assert.equal(shouldDeleteUnverifiedUser(user({ metadata: { creationTime: '2026-10-22T09:30:00Z' } }), now), false);
});

test('rechecks Auth before deletion and preserves newly verified or linked accounts', async () => {
  const deleted = [];
  for (const current of [user({ emailVerified: true }), user({ providerData: [{ providerId: 'google.com' }] }), user({ providerData: [] })]) {
    const auth = { getUser: async () => current, deleteUser: async uid => deleted.push(uid) };
    assert.equal(await deleteIfStillEligible(auth, current.uid, now, firestore(null)), false);
  }
  assert.deepEqual(deleted, []);
  const auth = { getUser: async () => user(), deleteUser: async uid => deleted.push(uid) };
  assert.equal(await deleteIfStillEligible(auth, 'test-user', now, firestore(null)), true);
  assert.deepEqual(deleted, ['test-user']);
});

test('established profiles survive email linking regardless of the original Auth creation date', async () => {
  const deleted=[];
  const auth={ getUser: async () => user(), deleteUser: async uid => deleted.push(uid) };
  for(const profile of [{nickname:'guest-player'}, {termsAccepted:true}, {legacyGlobalTermsAccepted:true}]) {
    assert.equal(await deleteIfStillEligible(auth,'test-user',now,firestore(profile)),false);
  }
  assert.deepEqual(deleted,[]);
});

test('profile read failure never deletes an account', async () => {
  let deleted=false;
  const auth={ getUser: async () => user(), deleteUser: async () => { deleted=true; } };
  const db={ collection: () => ({doc: () => ({get: async () => {throw new Error('unavailable');}})}) };
  await assert.rejects(deleteIfStillEligible(auth,'test-user',now,db));
  assert.equal(deleted,false);
});

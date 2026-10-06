'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { Timestamp } = require('firebase-admin/firestore');
const { dates, minimized, processPayment, setHold } = require('./retention');
const at = text => Timestamp.fromDate(new Date(text));

test('180 UTC days and five calendar years, including leap day', () => {
  const expiry = dates(new Date('2024-02-29T12:34:56.789Z'));
  assert.equal(expiry.raw.toISOString(), '2024-08-27T12:34:56.789Z');
  assert.equal(expiry.audit.toISOString(), '2029-02-28T12:34:56.789Z');
  assert.equal(dates(new Date('2026-10-04T03:00:00Z')).audit.toISOString(), '2031-10-04T03:00:00.000Z');
});
function fixture(status = 'completed') {
  const values = new Map([
    ['payments/p', { status, userId: 'u', tournamentId: 't', amount: 100, currency: 'BDT',
      statusUpdatedAt: at('2026-01-01T00:00:00Z'), adminNotes: 'PRIVATE-NAME 01712345678',
      transfer: { provider: 'remitly', providerReference: 'REF-1', completedAt: at('2026-01-01T00:00:00Z') } }],
    ['tournaments/t', { visibleInFlavours: ['bangladesh'] }],
    ['payments/p/private/recipient', { firstName: 'PRIVATE-NAME', lastName: 'PRIVATE-LAST',
      walletNumber: '01712345678', walletProvider: 'BKASH' }],
    ['payments/p/statusHistory/legacy', { recipientInfo: { accountNumber: 'legacy-preserved' } }],
    ['payments/p/statusHistory/new', { retentionPolicyVersion: 1, from: 'sent', to: 'completed' }],
  ]);
  function ref(path) { return { path, id: path.split('/').pop(), collection: name => query(path+'/'+name) }; }
  function snap(path) { const data = values.get(path); return { exists: data !== undefined,
    ref: ref(path), data: () => data, get: key => data?.[key] }; }
  function query(path, filters = [], max = Infinity) { return { path, filters, max,
    doc: id => ref(path+'/'+id), where: (k, op, v) => query(path, [...filters, [k,v]], max),
    limit: n => query(path, filters, n) }; }
  const writes = [];
  const db = { collection: name => query(name), runTransaction: async callback => {
    const operations = [];
    const result = await callback({ get: async target => {
      if (!target.filters) return snap(target.path);
      const docs = [...values.keys()].filter(p => p.startsWith(target.path+'/') &&
        p.split('/').length === target.path.split('/').length+1 && target.filters.every(([k,v]) => values.get(p)[k] === v))
        .slice(0,target.max).map(snap);
      return { docs, size: docs.length };
    }, update: (r,d) => operations.push(['update',r.path,d]), set: (r,d) => operations.push(['set',r.path,d]),
    delete: r => operations.push(['delete',r.path]) });
    for (const [op,p,d] of operations) {
      writes.push([op,p]);
      if (op === 'delete') values.delete(p);
      else values.set(p, op === 'update' ? {...values.get(p),...d} : d);
    }
    return result;
  } };
  return { values, writes, db, ref: ref('payments/p') };
}

test('raw boundary, repeat runs, safe minimized audit and unchanged legacy history', async () => {
  const f = fixture();
  await processPayment(f.db,f.ref,new Date('2026-06-29T23:59:59.999Z'),Timestamp);
  assert.ok(f.values.has('payments/p/private/recipient'));
  assert.equal(await processPayment(f.db,f.ref,new Date('2026-06-30T00:00:00Z'),Timestamp), 'raw-deleted');
  assert.ok(!f.values.has('payments/p/private/recipient'));
  const record = f.values.get('payments/p');
  assert.equal(record.walletProvider,'BKASH');
  assert.equal(record.transfer.providerReference,'REF-1');
  const json = JSON.stringify(record);
  for (const secret of ['firstName','lastName','walletNumber','01712345678','PRIVATE-NAME','PRIVATE-LAST']) assert.ok(!json.includes(secret),secret);
  const writeCount = f.writes.length;
  assert.equal(await processPayment(f.db,f.ref,new Date('2026-07-01T00:00:00Z'),Timestamp), 'already-raw-deleted');
  assert.equal(f.writes.length, writeCount);
  assert.equal(record.retention.rawDeletedAt.toMillis(), at('2026-06-30T00:00:00Z').toMillis());
  assert.equal(f.values.get('payments/p').retention.rawDeletedAt.toMillis(), record.retention.rawDeletedAt.toMillis());
  assert.ok(f.values.has('payments/p/statusHistory/legacy'));
});

test('cancelled is terminal; sent and non-Bangladesh payments are excluded', async () => {
  for (const [status, expected] of [['cancelled','raw-deleted'], ['sent','active']]) {
    const f=fixture(status);
    assert.equal(await processPayment(f.db,f.ref,new Date('2027-01-01Z'),Timestamp),expected);
  }
  const f=fixture();f.values.get('tournaments/t').visibleInFlavours=['global'];
  assert.equal(await processPayment(f.db,f.ref,new Date('2027-01-01Z'),Timestamp),'other-market');
});

test('manual hold and any unresolved ticket block both deadlines; releasing does not restart clock', async () => {
  const f=fixture();
  await setHold(f.db,'p',true,'legal_obligation','admin',Timestamp);
  assert.equal(await processPayment(f.db,f.ref,new Date('2032-01-01Z'),Timestamp),'held');
  assert.ok(f.values.has('payments/p/private/recipient'));
  await setHold(f.db,'p',false,'released','admin',Timestamp);
  f.values.set('supportTickets/s',{paymentId:'p',status:'waiting_for_user'});
  assert.equal(await processPayment(f.db,f.ref,new Date('2032-01-01Z'),Timestamp),'held');
  f.values.get('supportTickets/s').status='resolved';
  assert.equal(await processPayment(f.db,f.ref,new Date('2032-01-01Z'),Timestamp),'audit-deleted');
  assert.ok(!f.values.has('payments/p'));
  assert.ok(!f.values.has('payments/p/statusHistory/new'));
  assert.ok(f.values.has('payments/p/statusHistory/legacy'));
});

test('five-year boundary deletes new audit but never existing history', async () => {
  const f=fixture();
  await processPayment(f.db,f.ref,new Date('2030-12-31T23:59:59.999Z'),Timestamp);
  assert.ok(f.values.has('payments/p'));
  assert.ok(f.values.get('payments/p').retention.rawDeletedAt);
  assert.equal(await processPayment(f.db,f.ref,new Date('2031-01-01T00:00:00Z'),Timestamp), 'audit-deleted');
  assert.ok(!f.values.has('payments/p'));
  assert.ok(f.values.has('payments/p/statusHistory/legacy'));
});

test('free-text transfer fields cannot preserve known names or full numbers', () => {
  const data=minimized({status:'completed',recipientInfo:{accountNumber:'01712345678'},
    transfer:{provider:'PRIVATE-NAME',providerReference:'ref-01712345678'}},
    {firstName:'PRIVATE-NAME',lastName:'PRIVATE-LAST',walletNumber:'01712345678'});
  assert.deepEqual(data.transfer,{});
  assert.ok(!JSON.stringify(data).includes('01712345678'));
});

test('missing terminal timestamp fails closed without deleting recipient', async () => {
  const f=fixture();delete f.values.get('payments/p').statusUpdatedAt;
  assert.equal(await processPayment(f.db,f.ref,new Date('2032-01-01Z'),Timestamp),'missing-terminal-date');
  assert.equal(f.writes.length,0);
});


test('large new history is paginated while raw data is removed on the first pass', async () => {
  const f=fixture();
  for (let i=0;i<401;i++) f.values.set('payments/p/statusHistory/page-'+i,{retentionPolicyVersion:1});
  assert.equal(await processPayment(f.db,f.ref,new Date('2032-01-01Z'),Timestamp),'history-page');
  assert.ok(!f.values.has('payments/p/private/recipient'));
  assert.ok(f.values.has('payments/p'));
  assert.equal(await processPayment(f.db,f.ref,new Date('2032-01-02Z'),Timestamp),'audit-deleted');
  assert.ok(f.values.has('payments/p/statusHistory/legacy'));
});

test('support context reads private recipient and server-side tournament flavour', () => {
 const { payoutContext } = require('../support-tickets/support-ticket');
 assert.deepEqual(payoutContext({}, {walletProvider:'BKASH'}, {visibleInFlavours:['bangladesh']}),
  {market:'BD',payoutMethod:'bkash'});
 assert.deepEqual(payoutContext({walletProvider:'ROCKET'}, undefined, {visibleInFlavours:['bangladesh']}),
  {market:'BD',payoutMethod:'rocket'});
 assert.deepEqual(payoutContext({},undefined,{}),{market:'',payoutMethod:''});
});
test('many private attempts are paginated and legacy history is left unchanged', async () => {
 const f=fixture();
 for(let i=0;i<205;i++) f.values.set('payments/p/private/transfer-'+i,{recordType:'transfer_attempt',providerReference:'ref-'+i});
 assert.equal(await processPayment(f.db,f.ref,new Date('2032-01-01Z'),Timestamp),'history-page');
 assert.ok(f.values.has('payments/p'));
 assert.equal(await processPayment(f.db,f.ref,new Date('2032-01-01Z'),Timestamp),'history-page');
 assert.equal(await processPayment(f.db,f.ref,new Date('2032-01-01Z'),Timestamp),'audit-deleted');
 assert.ok(![...f.values.keys()].some(k=>k.includes('/private/')));
 assert.ok(f.values.has('payments/p/statusHistory/legacy'));
});

test('raw idempotency does not strand paginated private attempts before audit expiry', async () => {
 const f=fixture();
 for(let i=0;i<105;i++) f.values.set('payments/p/private/transfer-'+i,{recordType:'transfer_attempt'});
 assert.equal(await processPayment(f.db,f.ref,new Date('2027-01-01Z'),Timestamp),'raw-deleted');
 const deletedAt=f.values.get('payments/p').retention.rawDeletedAt.toMillis();
 assert.equal(await processPayment(f.db,f.ref,new Date('2027-01-02Z'),Timestamp),'raw-deleted');
 assert.ok(![...f.values.keys()].some(k=>k.includes('/private/')));
 const writes=f.writes.length;
 assert.equal(await processPayment(f.db,f.ref,new Date('2027-01-03Z'),Timestamp),'already-raw-deleted');
 assert.equal(f.writes.length,writes);
 assert.equal(f.values.get('payments/p').retention.rawDeletedAt.toMillis(),deletedAt);
});

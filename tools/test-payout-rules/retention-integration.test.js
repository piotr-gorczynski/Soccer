const { test, after } = require('node:test');
const assert = require('node:assert/strict');
const { initializeApp, deleteApp } = require('firebase-admin/app');
const { getFirestore, Timestamp, FieldValue } = require('firebase-admin/firestore');
const { processPayment, setHold, sweep } = require('../../firebase/functions/update-payment-status/retention');
if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error('Emulator required');
const { updateTicket } = require('../support-tickets/support-tickets');
const app=initializeApp({projectId:'demo-retention'},'retention-tests');
const db=getFirestore(app);
after(() => deleteApp(app));
test('Admin transactions preserve legacy history, honor holds and remove expired raw/audit data', async () => {
 const ref=db.collection('payments').doc('retention-test');
 await db.doc('tournaments/t').set({visibleInFlavours:['bangladesh']});
 await ref.set({status:'completed',statusUpdatedAt:Timestamp.fromDate(new Date('2026-01-01Z')),
  tournamentId:'t',userId:'winner',amount:100,currency:'BDT',transfer:{provider:'remitly',providerReference:'REF-1'}});
 const raw=ref.collection('private').doc('recipient');
 await raw.set({firstName:'Jane',lastName:'Recipient',walletProvider:'BKASH',walletNumber:'01712345678'});
 const legacy=ref.collection('statusHistory').doc('old');
 await legacy.set({recipientInfo:{accountNumber:'legacy-untouched'}});
 await ref.collection('statusHistory').doc('new').set({retentionPolicyVersion:1,from:'sent',to:'completed'});
 await db.doc('supportTickets/s').set({paymentId:ref.id,status:'open'});
 assert.equal(await processPayment(db,ref,new Date('2027-01-01Z'),Timestamp),'held');
 await updateTicket(db, db.doc('supportTickets/s'), 'resolve', '', 'admin', FieldValue);
 await setHold(db,ref.id,true,'legal_obligation','admin',Timestamp);
 assert.equal(await processPayment(db,ref,new Date('2027-01-01Z'),Timestamp),'held');
 await setHold(db,ref.id,false,'released','admin',Timestamp);
 assert.deepEqual(await sweep(db,new Date('2027-01-01Z'),Timestamp), {'raw-deleted':1});
 assert.equal((await raw.get()).exists,false);
 const record=(await ref.get()).data();
 assert.equal(record.walletProvider,'BKASH');
 assert.ok(!JSON.stringify(record).includes('01712345678'));
 assert.ok(!JSON.stringify(record).includes('firstName'));
 const snapshotBeforeRepeat=await ref.get();
 assert.deepEqual(await sweep(db,new Date('2027-01-02Z'),Timestamp), {'already-raw-deleted':1});
 const snapshotAfterRepeat=await ref.get();
 assert.ok(snapshotAfterRepeat.updateTime.isEqual(snapshotBeforeRepeat.updateTime));
 assert.deepEqual(snapshotAfterRepeat.data(),snapshotBeforeRepeat.data());
 assert.deepEqual(await sweep(db,new Date('2031-01-01Z'),Timestamp), {'audit-deleted':1});
 assert.equal((await ref.get()).exists,false);
 await assert.rejects(updateTicket(db, db.doc('supportTickets/s'), 'reply', 'Follow-up', 'admin', FieldValue), /no longer exists/);
 assert.deepEqual((await legacy.get()).data(),{recipientInfo:{accountNumber:'legacy-untouched'}});
 assert.equal((await ref.collection('statusHistory').doc('new').get()).exists,false);
});

const { persistTransition, buildAdminHistory } = require('../../firebase/functions/update-payment-status/payment-workflow');
test('atomic transitions retain separate attempts, set expiry, resolve only technical tickets and honor holds', async () => {
 const ref=db.doc('payments/lifecycle');
 await db.doc('tournaments/lifecycle').set({visibleInFlavours:['bangladesh']});
 await ref.set({status:'processing',userId:'winner',tournamentId:'lifecycle'});
 const technical=db.doc('supportTickets/technical');
 const dispute=db.doc('supportTickets/dispute');
 await technical.set({paymentId:ref.id,status:'waiting_for_user',category:'validation_rejected'});
 await dispute.set({paymentId:ref.id,status:'open',category:'payment_not_received'});
 let n=0;
 async function transition(status,data={}) {
  const history=ref.collection('statusHistory').doc('step-'+(++n));
  await db.runTransaction(async tx=>{
   const snap=await tx.get(ref);
   const update={status};
   if(status==='sent') {update['transfer.provider']=data.provider;update['transfer.providerReference']=data.providerReference;}
   await persistTransition(tx,db,ref,snap,status,data,history,update,
    buildAdminHistory(snap.get('status'),status,data,'admin','test',null),Timestamp);
  });
 }
 await transition('sent',{provider:'Remitly',providerReference:'REF-FIRST'});
 await transition('action_required');
 await transition('sent',{provider:'Remitly',providerReference:'REF-SECOND'});
 const first=ref.collection('private').doc('transfer-step-1');
 const second=ref.collection('private').doc('transfer-step-3');
 assert.equal((await first.get()).get('providerReference'),'REF-FIRST');
 assert.equal((await second.get()).get('providerReference'),'REF-SECOND');
 await transition('completed');
 const completed=(await ref.get()).data();
 assert.equal(completed.retention.terminalAt.toMillis(),completed.statusUpdatedAt.toMillis());
 assert.equal(completed.retention.rawExpiresAt.toMillis()-completed.statusUpdatedAt.toMillis(),180*86400000);
 assert.equal((await technical.get()).get('status'),'resolved');
 assert.equal((await dispute.get()).get('status'),'open');
 assert.equal((await technical.collection('statusHistory').get()).size,1);
 const history=(await ref.collection('statusHistory').get()).docs.map(d=>d.data());
 assert.ok(!JSON.stringify(history).includes('REF-FIRST'));
 assert.ok(!JSON.stringify(history).includes('REF-SECOND'));
 assert.equal(history[3].details.attemptId,'step-3');
 const expiry=completed.retention.rawExpiresAt.toDate();
 assert.equal(await processPayment(db,ref,expiry,Timestamp),'held');
 assert.ok((await first.get()).exists);
 await dispute.update({status:'resolved'});
 await processPayment(db,ref,expiry,Timestamp);
 assert.equal((await first.get()).exists,false);
 assert.equal((await second.get()).exists,false);
 assert.equal((await ref.collection('statusHistory').get()).size,4);
 // Cancellation and legal holds must not resolve a user's complaint.
 await ref.set({status:'sent',userId:'winner',tournamentId:'lifecycle',retentionHold:{active:true}});
 await technical.update({status:'open'});
 await transition('completed');
 assert.equal((await technical.get()).get('status'),'open');
 assert.ok((await ref.get()).get('retention'));
 await ref.set({status:'processing',userId:'winner',tournamentId:'lifecycle'});
 await transition('cancelled');
 assert.ok((await ref.get()).get('retention'));
 assert.equal((await technical.get()).get('status'),'open');
});

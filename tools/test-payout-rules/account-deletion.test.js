const {test,before,beforeEach,after}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {initializeTestEnvironment,assertFails,assertSucceeds}=require('@firebase/rules-unit-testing');
const {initializeApp,deleteApp}=require('firebase-admin/app');
const {getFirestore,FieldValue,Timestamp}=require('firebase-admin/firestore');
const {getDatabase}=require('firebase-admin/database');
const {doc,setDoc,updateDoc}=require('firebase/firestore');
const {ref,set,get}=require('firebase/database');
const {cleanupDeletedAccount}=require('../../firebase/functions/remove-account/deletion');
if(!process.env.FIRESTORE_EMULATOR_HOST||!process.env.FIREBASE_DATABASE_EMULATOR_HOST)throw Error('Local emulators required');
const projectId='demo-account-deletion';
const app=initializeApp({projectId,databaseURL:'https://'+projectId+'.firebaseio.com'},'account-deletion');
const db=getFirestore(app),rtdb=getDatabase(app);
let env;
before(async()=>{env=await initializeTestEnvironment({projectId,firestore:{host:'127.0.0.1',port:8188,rules:fs.readFileSync('../../firebase/firestore.rules','utf8')},database:{host:'127.0.0.1',port:9005,rules:fs.readFileSync('../../gcp/cloud-build/database.rules.json','utf8')}});});
beforeEach(async()=>{await env.clearFirestore();await env.clearDatabase();});
after(async()=>{await env.cleanup();await deleteApp(app);});
const cleanup=()=>cleanupDeletedAccount(db,rtdb,'gone',FieldValue);

test('minimizes root, preserves historical UIDs/legal/payout data, removes only active account state',async()=>{
 await db.doc('users/gone').set({email:'fake@example.invalid',nickname:'OLD TEST NAME',facebookId:'TEST',facebookName:'TEST',facebookPhotoUrl:'TEST',providerData:{name:'TEST'},googleProfile:{name:'TEST'},fcmToken:'TEST',fcmInstallationId:'TEST',method:'TEST',appVariants:{global:{}},termsAccepted:true,termsAcceptanceDate:Timestamp.fromMillis(123456),language:'pl',unknownFutureProfileField:'TEST'});
 await db.doc('users/other').set({nickname:'Other'});
 await db.doc('users/gone/friends/other').set({addedAt:Timestamp.now()});
 await db.doc('users/other/friends/gone').set({addedAt:Timestamp.now()});
 await db.doc('users/other/friends/keep').set({test:true});
 await db.doc('invitations/out').set({from:'gone',to:'other',status:'pending'});
 await db.doc('invitations/in').set({from:'other',to:'gone',status:'pending'});
 const retained={
  'matches/m':{player0:'gone',player1:'other',winner:'gone'},
  'tournaments/t/participants/gone':{uid:'gone',eligibilityConfirmation:{regulationId:'r'}},
  'tournaments/t/results/gone':{userId:'gone',rank:1},
  'rankings/gone':{userId:'gone',wins:1},
  'invitations/historical':{from:'gone',to:'other',status:'accepted'},
  'users/gone/legalAcceptances/terms_test':{scope:'bangladesh',version:'TEST'},
  'payments/p':{userId:'gone',status:'completed',retention:{rawExpiresAt:Timestamp.fromMillis(100000),auditExpiresAt:Timestamp.fromMillis(500000)}},
  'payments/p/private/recipient':{firstName:'TEST',walletNumber:'TEST_ONLY'},
  'payments/p/statusHistory/event':{userId:'gone',status:'completed'},
  'supportTickets/s':{userId:'gone',status:'open'}
 };
 for(const [path,value] of Object.entries(retained))await db.doc(path).set(value);
 await rtdb.ref('status/gone').set({state:'online',device:'TEST',last_changed:123});
 await cleanup();
 const snapshot=await db.doc('users/gone').get(), tombstone=snapshot.data();
 assert.deepEqual(Object.keys(tombstone).sort(),['accountDeleted','accountDeletedAt','nickname','nicknameLowercase'].sort());
 assert.equal(tombstone.accountDeleted,true);assert.equal(tombstone.nickname,'(Account removed)');assert.equal(tombstone.nicknameLowercase,'(account removed)');assert.ok(tombstone.accountDeletedAt instanceof Timestamp);
 const legacy=(await db.doc('users/gone/legalAcceptances/legacy_terms_unversioned').get()).data();
 assert.equal(legacy.version,null);assert.equal(legacy.scope,'unknown');assert.equal(legacy.acceptedAt.toMillis(),123456);assert.equal(legacy.language,'pl');
 for(const path of ['users/gone/friends/other','users/other/friends/gone','invitations/out','invitations/in'])assert.equal((await db.doc(path).get()).exists,false,path);
 for(const [path,value] of Object.entries(retained))assert.deepEqual((await db.doc(path).get()).data(),value,path);
 assert.equal((await db.doc('users/other/friends/keep').get()).exists,true);
 assert.deepEqual((await rtdb.ref('status/gone').get()).val(),{accountDeleted:true,state:'offline'});
 await cleanup();assert.deepEqual((await db.doc('users/gone').get()).data(),tombstone);
 assert.ok((await db.doc('users/gone').get()).updateTime.isEqual(snapshot.updateTime));
});

test('partial failure can retry without losing tombstone or legal records',async()=>{
 await db.doc('users/gone').set({email:'fake@example.invalid'});
 await db.doc('users/gone/legalAcceptances/test').set({test:true});
 const failedRtdb={ref:()=>({child:()=>({set:async()=>{throw Error('simulated outage');}})})};
 await assert.rejects(cleanupDeletedAccount(db,failedRtdb,'gone',FieldValue),/simulated outage/);
 assert.equal((await db.doc('users/gone').get()).get('accountDeleted'),true);
 await cleanup();assert.equal((await db.doc('users/gone/legalAcceptances/test').get()).exists,true);
});

test('missing profile becomes tombstone without deleting orphaned legal subcollections',async()=>{
 await db.doc('users/gone/legalAcceptances/test').set({test:true});
 await cleanup();assert.equal((await db.doc('users/gone').get()).exists,true);
 assert.equal((await db.doc('users/gone/legalAcceptances/test').get()).exists,true);
});

test('stale client cannot restore profile, tokens, friendships or presence',async()=>{
 await cleanup();await db.doc('users/other').set({nickname:'Other'});
 const own=env.authenticatedContext('gone'),other=env.authenticatedContext('other');
 await assertFails(updateDoc(doc(own.firestore(),'users/gone'),{fcmToken:'TEST',email:'fake@example.invalid'}));
 await assertFails(setDoc(doc(own.firestore(),'users/gone'),{nickname:'RESTORED'}));
 await assertFails(setDoc(doc(own.firestore(),'users/gone/friends/other'),{test:true}));
 await assertFails(setDoc(doc(other.firestore(),'users/other/friends/gone'),{test:true}));
 assert.deepEqual((await get(ref(own.database(),'status/gone'))).val(),{accountDeleted:true,state:'offline'});
 await assertFails(set(ref(own.database(),'status/gone'),{state:'online'}));
 await assertFails(set(ref(own.database(),'status/gone'),null));
 await assertSucceeds(set(ref(other.database(),'status/other'),{state:'online'}));
 await assertFails(set(ref(other.database(),'status/other'),{accountDeleted:true,state:'offline'}));
});

test('incoming friend cleanup paginates beyond a single batch and keeps unrelated links',async()=>{
 const batch=db.batch();
 for(let i=0;i<305;i++)batch.set(db.doc('users/u'+String(i).padStart(3,'0')),{nickname:'TEST'});
 await batch.commit();
 const friends=db.batch();
 for(let i=0;i<305;i++)friends.set(db.doc('users/u'+String(i).padStart(3,'0')+'/friends/gone'),{test:true});
 await friends.commit();
 await cleanup();
 assert.equal((await db.collectionGroup('friends').get()).size,0);
 assert.equal((await db.collection('users').get()).size,306);
});

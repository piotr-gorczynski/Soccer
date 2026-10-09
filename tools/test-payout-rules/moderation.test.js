const {test,before,after} = require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {initializeTestEnvironment,assertFails,assertSucceeds}=require('@firebase/rules-unit-testing');
const {doc,setDoc,getDoc,deleteDoc,updateDoc}=require('firebase/firestore');
const {initializeApp,deleteApp}=require('firebase-admin/app');
const {getFirestore,FieldValue,Timestamp}=require('firebase-admin/firestore');
if(!process.env.FIRESTORE_EMULATOR_HOST) throw Error('Emulator required');
let env, app, db, handlers;
class HttpsError extends Error { constructor(code,message){super(message);this.code=code;} }
const auth=uid=>({auth:{uid}});
before(async()=>{
 env=await initializeTestEnvironment({projectId:'demo-moderation',firestore:{host:'127.0.0.1',port:8188,rules:fs.readFileSync('../../firebase/firestore.rules','utf8')}});
 app=initializeApp({projectId:'demo-moderation'},'moderation');db=getFirestore(app);
 handlers=require('../../firebase/functions/support-tickets/moderation').handlers(db,FieldValue,HttpsError);
 await db.doc('users/a').set({nickname:'Alice'});await db.doc('users/b').set({nickname:'Bob'});
});
after(async()=>{await env?.cleanup();if(app)await deleteApp(app);});
function load(name,exportName){
 const functions={https:{HttpsError,onCall:fn=>fn},region:()=>functions};
 const firestore=()=>db;firestore.Timestamp=Timestamp;firestore.FieldValue=FieldValue;
 const sandbox={exports:{},require:id=>id.startsWith('firebase-functions')?functions:{initializeApp(){},firestore}};
 new Function('require', 'exports', fs.readFileSync('../../firebase/functions/'+name+'/index.js','utf8'))(sandbox.require, sandbox.exports);
 return sandbox.exports[exportName];
}
test('report derives identity/nickname/time on server; rejects forged fields, unauthenticated/self/invalid/deleted targets',async()=>{
 const input={reportedUid:'b',reason:'Offensive name',context:'ranking'};
 await assert.rejects(handlers.reportPlayer(input,{}),{code:'unauthenticated'});
 for(const extra of [{reporterUid:'b'},{nickname:'fake'},{createdAt:123},{context:'arbitrary'},{reason:''}]) {
  await assert.rejects(handlers.reportPlayer({...input,...extra},auth('a')),{code:'invalid-argument'});
 }
 await assert.rejects(handlers.reportPlayer({...input,reportedUid:'a'},auth('a')));
 await assert.rejects(handlers.reportPlayer({...input,reportedUid:'missing'},auth('a')));
 await db.doc('users/deleted').set({accountDeleted:true});
 await assert.rejects(handlers.reportPlayer({...input,reportedUid:'deleted'},auth('a')));
 const result=await handlers.reportPlayer(input,auth('a'));
 assert.deepEqual(await handlers.reportPlayer(input,auth('a')),result);
 const reports=await db.collection('supportTickets').get();assert.equal(reports.size,1);
 const r=reports.docs[0].data();assert.equal(r.reporterUid,'a');assert.equal(r.reportedUid,'b');assert.equal(r.nickname,'Bob');assert.ok(r.createdAt instanceof Timestamp);
 const a=env.authenticatedContext('a').firestore(),b=env.authenticatedContext('b').firestore();
 await assertSucceeds(getDoc(doc(a,reports.docs[0].ref.path)));
 await assertFails(getDoc(doc(b,reports.docs[0].ref.path)));
 await assertFails(setDoc(doc(a,'supportTickets/forged'),{...r,createdAt:new Date(),updatedAt:new Date(),statusUpdatedAt:new Date()}));
 await assertFails(updateDoc(doc(a,reports.docs[0].ref.path),{status:'resolved'}));
});
test('private block list cannot be forged; blocks both directions, pending acceptance, tournament invites and direct friend/match writes; unblock restores invites',async()=>{
 const create=load('create-invite','createInvite'),accept=load('accept-invite','acceptInvite'),add=load('add-friend','addFriend');
 await db.doc('tournaments/t').set({status:'running'});
 const pending=await create({toUid:'b'},auth('a'));
 await assert.rejects(handlers.setPlayerBlock({reportedUid:'a',blocked:true},auth('a')));
 await assert.rejects(handlers.setPlayerBlock({reportedUid:'b',blocked:true,userId:'b'},auth('a')));
 await handlers.setPlayerBlock({reportedUid:'a',blocked:true},auth('b'));
 const a=env.authenticatedContext('a').firestore(),b=env.authenticatedContext('b').firestore();
 await assertSucceeds(getDoc(doc(b,'users/b/blocks/a')));
 await assertFails(getDoc(doc(a,'users/b/blocks/a')));
 await assertFails(setDoc(doc(a,'users/b/blocks/a'),{}));
 await assertFails(deleteDoc(doc(a,'users/b/blocks/a')));
 await assertFails(setDoc(doc(b,'users/b/blocks/a'),{}));
 await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(),'users/b/blocks/a')));
 for(const [from,to] of [['a','b'],['b','a']]) {
  await assert.rejects(create({toUid:to},auth(from)),{code:'permission-denied'});
  await assert.rejects(create({toUid:to,tournamentId:'t'},auth(from)),{code:'permission-denied'});
  await assert.rejects(add({userId:from,friendId:to},auth(from)),{code:'permission-denied'});
 }
 await assert.rejects(accept({invitationId:pending.inviteId},auth('b')),{code:'permission-denied'});
 await assertFails(setDoc(doc(a,'users/a/friends/b'),{}));
 await assertFails(setDoc(doc(a,'matches/forged'),{player0:'a',player1:'b'}));
 await assertFails(setDoc(doc(a,'tournaments/t/matches/forged'),{player0:'a',player1:'b'}));
 await handlers.setPlayerBlock({reportedUid:'a',blocked:false},auth('b'));
 const accepted=await accept({invitationId:pending.inviteId},auth('b'));assert.ok(accepted.matchPath);
 await add({userId:'a',friendId:'b'},auth('a'));
 await create({toUid:'b'},auth('a'));
});

test('concurrent reports deduplicate, limit open reports and preserve resolved history', async () => {
 await db.doc('users/c').set({nickname:'Carol'});
 const input={reportedUid:'c',reason:'Harassment',context:'search'};
 const results=await Promise.all(Array.from({length:3},()=>handlers.reportPlayer(input,auth('a'))));
 assert.equal(new Set(results.map(r=>r.reference)).size,1);
 const reports=await db.collection('supportTickets').where('reportedUid','==','c').get();
 assert.equal(reports.size,1);
 await reports.docs[0].ref.update({status:'resolved'});
 const next=await handlers.reportPlayer(input,auth('a'));
 assert.notEqual(next.reference,results[0].reference);
 assert.equal((await reports.docs[0].ref.get()).get('status'),'resolved');
 const a=env.authenticatedContext('a').firestore();
 await assertFails(setDoc(doc(a,'users/a/moderationState/reports'),{}));
 for(let i=0;i<8;i++) await db.doc('supportTickets/limit'+i).set({reporterUid:'a',reportedUid:'limit'+i,status:'open'});
 await db.doc('users/d').set({nickname:'Dave'});
 await assert.rejects(handlers.reportPlayer({...input,reportedUid:'d'},auth('a')),{code:'resource-exhausted'});
});
test('block retries preserve timestamp, deleted accounts cannot act, missing targets can be unblocked', async () => {
 await assert.rejects(handlers.setPlayerBlock({reportedUid:'b',blocked:true},{}),{code:'unauthenticated'});
 await assert.rejects(handlers.setPlayerBlock({reportedUid:'b',blocked:true},auth('deleted')),{code:'permission-denied'});
 await handlers.setPlayerBlock({reportedUid:'b',blocked:true},auth('a'));
 const ref=db.doc('users/a/blocks/b'),before=await ref.get();
 await handlers.setPlayerBlock({reportedUid:'b',blocked:true},auth('a'));
 assert.ok((await ref.get()).updateTime.isEqual(before.updateTime));
 await db.doc('users/a/blocks/missing').set({createdAt:FieldValue.serverTimestamp()});
 await handlers.setPlayerBlock({reportedUid:'missing',blocked:false},auth('a'));
 assert.equal((await db.doc('users/a/blocks/missing').get()).exists,false);
});
test('queued invitation notification is suppressed when either player has blocked the other', async () => {
 let sent=0;
 const functions={firestore:{document:()=>({onCreate:fn=>fn})}};
 const exports={};
 const dependencies={
  'firebase-functions/v1':functions,
  'firebase-admin/app':{initializeApp(){}},
  'firebase-admin/firestore':{FieldValue,getFirestore:()=>db},
  'firebase-admin/messaging':{getMessaging:()=>({send:async()=>{sent++;}})},
  'firebase-admin/database':{getDatabase:()=>({})}
 };
 new Function('require','exports',fs.readFileSync('../../firebase/functions/send-invite-notification/sendInviteNotification.js','utf8'))(id=>dependencies[id],exports);
 const ref=db.doc('invitations/queued');
 await ref.set({from:'a',to:'b',status:'pending'});
 await exports.sendInviteNotification(await ref.get(),{params:{inviteId:'queued'}});
 assert.equal(sent,0);
});

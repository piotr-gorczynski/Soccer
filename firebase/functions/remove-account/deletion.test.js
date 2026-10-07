const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
function load({authError,cleanupError}={}) {
 const events=[],options=[];
 class HttpsError extends Error {constructor(code,message){super(message);this.code=code;}}
 const builder={https:{onCall:fn=>fn,HttpsError},auth:{user:()=>({onDelete:fn=>fn})},region:()=>builder,runWith:opt=>{options.push(opt);return builder;}};
 const firestore=()=>({});firestore.FieldValue={};
 const admin={initializeApp:()=>{},firestore,database:()=>({}),auth:()=>({deleteUser:async uid=>{events.push('auth:'+uid);if(authError)throw {code:authError};}})};
 const exports={};
 vm.runInNewContext(fs.readFileSync(__dirname+'/index.js','utf8'),{exports,console:{error:()=>{}},require:name=>{
  if(name==='firebase-functions')return builder;
  if(name==='firebase-admin')return admin;
  if(name==='./deletion')return {cleanupDeletedAccount:async(db,rtdb,uid)=>{events.push('cleanup:'+uid);if(cleanupError)throw Error('outage');}};
  throw Error(name);
 }});
 return {exports,events,options};
}
test('unauthenticated request cannot delete anything',async()=>{const x=load();await assert.rejects(x.exports.removeAccount({},{}),{code:'unauthenticated'});assert.deepEqual(x.events,[]);});
test('deletes only authenticated UID before cleanup',async()=>{const x=load();const result=await x.exports.removeAccount({uid:'someone-else'},{auth:{uid:'test'}});assert.equal(result.uid,'test');assert.deepEqual(x.events,['auth:test','cleanup:test']);});
test('Auth failure prevents cleanup but user-not-found allows retry',async()=>{
 const failed=load({authError:'auth/internal-error'});await assert.rejects(failed.exports.removeAccount({},{auth:{uid:'test'}}),{code:'internal'});assert.deepEqual(failed.events,['auth:test']);
 const retry=load({authError:'auth/user-not-found'});await retry.exports.removeAccount({},{auth:{uid:'test'}});assert.deepEqual(retry.events,['auth:test','cleanup:test']);
});
test('cleanup failure is not reported as success; deletion event retries same UID',async()=>{
 const failed=load({cleanupError:true});await assert.rejects(failed.exports.removeAccount({},{auth:{uid:'test'}}),/account-cleanup-pending/);
 const retry=load();await retry.exports.onAccountDeleted({uid:'test'});assert.deepEqual(retry.events,['cleanup:test']);assert.equal(retry.options[0].failurePolicy,true);
});

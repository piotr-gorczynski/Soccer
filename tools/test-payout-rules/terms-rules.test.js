const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { doc, setDoc, getDoc, updateDoc, deleteDoc, serverTimestamp, Timestamp, writeBatch } = require('firebase/firestore');
const fs = require('node:fs');
const crypto = require('node:crypto');
const hashes = require('../../firebase-hosting/terms-document-hashes.json');
let env;
before(async () => { env = await initializeTestEnvironment({ projectId:'demo-terms', firestore: {
  host:'127.0.0.1',port:8188,rules:fs.readFileSync('../../firebase/firestore.rules','utf8')
}}); });
after(async () => { if(env) await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });
function record(scope='bangladesh',language='en') {
  const bd=scope==='bangladesh', version=bd?'BD-terms-2026-10-06':'GLOBAL-terms-2026-10-13';
  return {documentType:'terms',scope,version,acceptedAt:serverTimestamp(),language,
    documentUrl:'https://piotr-gorczynski.com/'+(bd?'bangladesh/terms/'+version+(language==='bn'?'-bn':'')+'.html':'terms/'+version+'/'+language+'.html'),
    documentSha256:hashes[bd?(language==='bn'?'bangladesh-bn':'bangladesh'):language],appVersionCode:100,appVersionName:'TEST',flavor:bd?'_devBangladesh':'_devGlobal'};
}
function ref(db,r=record(),owner='owner') {return doc(db,'users/'+owner+'/legalAcceptances/terms_'+r.scope+'__'+r.version);}
function db(user='owner') {return env.authenticatedContext(user).firestore();}
test('exact approved HTML bytes match manifest, app and rules',()=>{
  const manager=fs.readFileSync('../../mobile/app/src/main/java/piotr_gorczynski/soccer2/LanguageManager.java','utf8');
  const supported=[...manager.matchAll(/LANGUAGE_NAME_RES_IDS\.put\("([a-z]+)"/g)].map(m=>m[1]).sort();
  assert.deepEqual(Object.keys(hashes).filter(k=>!k.startsWith('bangladesh')).sort(),supported);
  const app=fs.readFileSync('../../mobile/app/src/main/java/piotr_gorczynski/soccer2/TermsPolicy.java','utf8');
  const rules=fs.readFileSync('../../firebase/firestore.rules','utf8');
  for(const [lang,hash] of Object.entries(hashes)) {
    const path=lang.startsWith('bangladesh')?'bangladesh/terms/BD-terms-2026-10-06'+(lang==='bangladesh-bn'?'-bn':'')+'.html':'terms/GLOBAL-terms-2026-10-13/'+lang+'.html';
    assert.equal(crypto.createHash('sha256').update(fs.readFileSync('../../firebase-hosting/public/'+path)).digest('hex'),hash);
    assert.ok(app.includes(hash));assert.ok(rules.includes(hash));
    if(!lang.startsWith('bangladesh')) {
      const html=fs.readFileSync('../../firebase-hosting/public/'+path,'utf8');
      assert.ok(html.includes('lang="'+lang+'"'));
      assert.equal((html.match(/<h2(?: id="tournaments")?>/g)||[]).length,7);
      assert.ok(html.includes('id="tournaments"'));
      const publicHtml=fs.readFileSync('../../firebase-hosting/public/terms-'+lang+'.html','utf8');
      const content=page=>page.match(/<div id="content">([\s\S]*?)<\/div>/)[1];
      assert.equal(content(publicHtml),content(html));
      assert.equal((html.match(/<li>/g)||[]).length,6);
      assert.ok(html.includes('2026-10-13'));
      if(['ar','fa','ur'].includes(lang)) assert.ok(html.includes('dir="rtl"'));
      assert.ok(fs.readFileSync('../../firebase-hosting/public/index.html','utf8').includes('href="terms-'+lang+'.html"'));
    }
  }
});
test('BD and all Global language acceptances store server timestamp and independent records',async()=>{
  for(const lang of Object.keys(hashes)) {
    const r=lang.startsWith('bangladesh')?record('bangladesh',lang==='bangladesh-bn'?'bn':'en'):record('global',lang), store=db('user-'+lang);
    const target=ref(store,r,'user-'+lang);
    await assertSucceeds(setDoc(target,r));
    const stored=(await getDoc(target)).data();
    assert.ok(stored.acceptedAt instanceof Timestamp);
    for(const k of Object.keys(r).filter(k=>k!=='acceptedAt')) assert.equal(stored[k],r[k]);
  }
  const store=db(); await setDoc(ref(store),record());
  assert.equal((await getDoc(ref(store,record('global')))).exists(),false);
  await setDoc(ref(store,record('global')),record('global'));
  assert.equal((await getDoc(ref(store))).exists(),true);
});
test('owner can read but never update/delete or overwrite acceptance',async()=>{
  const target=ref(db()); await setDoc(target,record());
  await assertSucceeds(getDoc(target));
  await assertFails(updateDoc(target,{appVersionName:'changed'}));
  await assertFails(setDoc(target,record()));
  await assertFails(deleteDoc(target));
});
test('another user and unauthenticated clients cannot read or create',async()=>{
  await setDoc(ref(db()),record());
  await assertFails(getDoc(ref(db('other'))));
  await assertFails(setDoc(ref(db('other'),record('global')),record('global')));
  const anon=env.unauthenticatedContext().firestore();
  await assertFails(getDoc(ref(anon)));await assertFails(setDoc(ref(anon,record('global')),record('global')));
});
test('rejects extra/missing fields, wrong version/hash/url/scope/language/time',async()=>{
  for(const change of [{firstName:'not allowed'},{documentType:'privacy'},{version:'BD-terms-old'},
      {scope:'other'},{language:'pl'},{documentSha256:'wrong'},{documentUrl:'https://example.com'},
      {acceptedAt:Timestamp.fromMillis(1)},{appVersionCode:'100'}]) {
    await assertFails(setDoc(ref(db()),{...record(),...change}));
  }
  const missing=record();delete missing.flavor;await assertFails(setDoc(ref(db()),missing));
  await assertFails(setDoc(doc(db(),'users/owner/legalAcceptances/wrong-id'),record()));
});
test('view alone creates nothing; denied atomic write leaves no acceptance or legacy update',async()=>{
  const store=db();const target=ref(store);
  assert.equal((await getDoc(target)).exists(),false);
  const batch=writeBatch(store);
  batch.set(target,{...record(),documentSha256:'wrong'});
  batch.set(doc(store,'users/owner'),{termsAccepted:true});
  await assertFails(batch.commit());
  assert.equal((await getDoc(target)).exists(),false);
  assert.equal((await getDoc(doc(store,'users/owner'))).exists(),false);
});

test('historical Global acceptances remain valid for older clients and HTML is unchanged',async()=>{
  const old=require('../../firebase-hosting/terms-document-hashes-2025-07-30.json');
  for(const [language,hash] of Object.entries(old).filter(([k])=>!k.startsWith('bangladesh'))) {
    const version='GLOBAL-terms-2025-07-30';
    const path='terms/'+version+'/'+language+'.html';
    assert.equal(crypto.createHash('sha256').update(fs.readFileSync('../../firebase-hosting/public/'+path)).digest('hex'),hash);
    const r={...record('global',language),version,documentUrl:'https://piotr-gorczynski.com/'+path,documentSha256:hash};
    await assertSucceeds(setDoc(ref(db('old-'+language),r,'old-'+language),r));
  }
});

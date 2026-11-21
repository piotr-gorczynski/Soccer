'use strict';
const test = require('node:test'), assert = require('node:assert/strict');
const { send, configure } = require('./post-payout-feedback');
const { destination, facebookUrl, publicFeedback } = require('../../firebase/functions/support-tickets/feedback-links');
const ticketPath = 'supportTickets/feedback_' + require('crypto').createHash('sha256').update('p').digest('hex');
const community = require('../../firebase/functions/support-tickets/community.json').facebookCommunityUrl;
function fixture() {
  let next=0, chain=Promise.resolve();
  const rows=new Map([
    ['payments/p', {status:'completed', userId:'u', tournamentId:'t', currency:'BDT', market:'BD', private:'secret'}],
    ['tournaments/t', {name:'Cup', visibleInFlavours:['bangladesh']}],
    ['users/u', {language:'bn', accountNumber:'SECRET'}],
  ]);
  const ref=path=>({path,id:path.split('/').pop(),collection:name=>({doc:(id=String(++next))=>ref(`${path}/${name}/${id}`)})});
  const db={collection:name=>({doc:id=>ref(`${name}/${id}`)}),runTransaction:callback=>{
    const result=chain.then(async()=>{
      const writes=[];
      const result=await callback({get:async r=>({exists:rows.has(r.path),get:k=>rows.get(r.path)?.[k],data:()=>rows.get(r.path)}),
        set:(r,d)=>writes.push(()=>rows.set(r.path,d)),update:(r,d)=>writes.push(()=>rows.set(r.path,{...rows.get(r.path),...d}))});
      writes.forEach(w=>w());return result;
    });chain=result.catch(()=>{});return result;
  }};
  return {db,rows,fields:{serverTimestamp:()=>123,delete:()=>null}};
}
test('only completed BD payouts; unavailable users and missing records cannot send',async()=>{
  for (const modify of [f=>f.rows.get('payments/p').status='sent',f=>f.rows.delete('payments/p'),
    f=>f.rows.get('tournaments/t').visibleInFlavours=['global'],f=>f.rows.get('payments/p').currency='USD',
    f=>f.rows.get('users/u').accountDeleted=true,f=>f.rows.get('payments/p').userId='../bad']) {
    const f=fixture();modify(f);await assert.rejects(send(f.db,'p','admin',f.fields));
    assert(!f.rows.has(ticketPath));
  }
});
test('atomic outbound support conversation, private data omitted, payout unchanged, BN and EN templates',async()=>{
  const f=fixture(),payment=JSON.stringify(f.rows.get('payments/p'));
  const result=await send(f.db,'p','admin',f.fields);
  assert.equal(result.language,'bn');assert.equal(result.version,1);
  const ticket=f.rows.get(ticketPath);
  assert.equal(ticket.status,'resolved');assert.equal(ticket.supportReplyVersion,1);assert.equal(ticket.userReadReplyVersion,0);
  const m=[...f.rows].find(([p])=>p.includes('/messages/'))[1];
  assert.equal(m.authorType,'support');assert(m.message.includes('সম্পূর্ণ ঐচ্ছিক'));assert(m.message.includes(community));
  assert(!JSON.stringify(m).includes('SECRET'));assert(!JSON.stringify(ticket).includes('secret'));
  assert.equal(JSON.stringify(f.rows.get('payments/p')),payment);
  const en=await send(f.db,'p','admin',f.fields,{force:true,language:'en'});
  assert.equal(en.version,2);assert(f.rows.get(ticketPath).latestSupportReply.includes('completely optional'));
});
test('duplicate/concurrent sends prevented; explicit force preserves thread/read version; dry run has no writes',async()=>{
  const f=fixture();const preview=await send(f.db,'p','admin',f.fields,{dryRun:true});assert(preview.dryRun);assert.equal(f.rows.size,3);
  const result=await Promise.allSettled([send(f.db,'p','a',f.fields),send(f.db,'p','b',f.fields)]);
  assert.equal(result.filter(r=>r.status==='fulfilled').length,1);
  await assert.rejects(send(f.db,'p','a',f.fields),/already sent/);
  f.rows.get(ticketPath).userReadReplyVersion=1;
  await send(f.db,'p','a',f.fields,{force:true});
  assert.equal(f.rows.get(ticketPath).userReadReplyVersion,1);
  assert.equal([...f.rows.keys()].filter(x=>x.includes('/messages/')).length,2);
});
test('single configured fallback; explicit valid tournament posts only and safe config writes',async()=>{
  assert.deepEqual(destination({}),{url:community,type:'community'});
  const url='https://www.facebook.com/Gridline/posts/123';assert.equal(destination({postPayoutFacebookPostUrl:url}).type,'tournament_post');
  for(const url of ['http://www.facebook.com/x/posts/1','https://facebook.com.evil.test/x/posts/1',
    'https://evil.test','https://admin@www.facebook.com/x/posts/1','https://www.facebook.com:444/x/posts/1',
    'javascript:alert(1)','https://www.facebook.com/otherpage']) {
    assert.equal(facebookUrl(url,true),null);assert.throws(()=>destination({postPayoutFacebookPostUrl:url}));
  }
  const f=fixture();await configure(f.db,'t',url,'admin',f.fields);
  assert.equal(f.rows.get('tournaments/t').postPayoutFacebookPostUrl,url);
  assert.equal((await send(f.db,'p','admin',f.fields,{dryRun:true})).destinationType,'tournament_post');
});
test('public projection allowlists feedback fields, rejects forged/user metadata and keeps old messages compatible',()=>{
  assert.deepEqual(publicFeedback({}),{});
  assert.deepEqual(publicFeedback({authorType:'user',kind:'post_payout_feedback',feedbackUrl:community,feedbackDestinationType:'community'}),{});
  const data={authorType:'support',kind:'post_payout_feedback',feedbackUrl:community,feedbackDestinationType:'community',accountNumber:'SECRET'};
  assert.deepEqual(publicFeedback(data),{kind:'post_payout_feedback',feedbackUrl:community,destinationType:'community'});
  assert.deepEqual(publicFeedback({...data,feedbackUrl:'https://evil.test'}),{});
});

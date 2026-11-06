const test = require("node:test");
const assert = require("node:assert/strict");
const { projectTournament, findPromotion } = require("./promotion");
const now=1_000_000;
function tournament(changes={}) {
  return { name:"Kickoff Cup", status:"registering", visibleInFlavours:["bangladesh"],
    maxParticipants:20, participantsCount:1, registrationDeadline:{toMillis:()=>now+1000},
    regulation:"rules", prizePool:{enabled:true,currency:"BDT",awards:[{place:1,amount:1000}]}, ...changes };
}
test("public projection contains only allowlisted tournament metadata", () => {
  const value=projectTournament("cup",tournament({email:"private",participants:["private"],wallet:"private"}),now);
  assert.deepEqual(Object.keys(value).sort(),["id","name","regulationId","firstPrize","currency","registrationDeadlineMillis"].sort());
  assert.equal(value.firstPrize,1000);
  assert.ok(!JSON.stringify(value).includes("private"));
});
test("amount is derived from first-place award, not total pool or a constant", () => {
  const value=projectTournament("cup",tournament({prizePool:{enabled:true,currency:"BDT",totalAmount:9000,awards:[{place:2,amount:1000},{place:1,amount:2300}]}}),now);
  assert.equal(value.firstPrize,2300);
});
test("expired, full, ended, missing or Global-only offers are hidden", () => {
  for(const change of [{status:"running"},{status:"ended"},{visibleInFlavours:["global"]},
    {registrationDeadline:{toMillis:()=>now}},{registrationDeadline:null},{participantsCount:20},
    {participantsCount:-1},{maxParticipants:0},{regulation:"bad/path"},{name:""}]) {
    assert.equal(projectTournament("cup",tournament(change),now),null);
  }
});
test("no cash prize, invalid currency and missing first-place award cannot advertise cash", () => {
  for(const pool of [null,{enabled:false,currency:"BDT",awards:[{place:1,amount:1000}]},
    {enabled:true,currency:"USD",awards:[{place:1,amount:1000}]},
    {enabled:true,currency:"BDT",awards:[{place:2,amount:1000}]},
    {enabled:true,currency:"BDT",awards:[{place:1,amount:0}]}]) {
    assert.equal(projectTournament("cup",tournament({prizePool:pool}),now),null);
  }
});
test("all eligible tournaments are returned in deadline order and invalid regulation is skipped", async () => {
  const docs=[{id:"later",data:()=>tournament({regulation:"good",registrationDeadline:{toMillis:()=>now+3000}})},
    {id:"earlier",data:()=>tournament({regulation:"invalid"})},
    {id:"first",data:()=>tournament({regulation:"good",registrationDeadline:{toMillis:()=>now+2000}})}];
  const db={collection:name=>name==="tournaments" ? {where:(field,op,value)=>{
    assert.deepEqual([field,op,value],["visibleInFlavours","array-contains","bangladesh"]);
    return {get:async()=>({docs})};
  }} : {doc:id=>({get:async()=>({data:()=>({status:"active",market:id==="good"?"BD":"US",minimumAge:18,prizeRules:{cashPrizesEnabled:true}})})})}};
  assert.deepEqual((await findPromotion(db,now)).map(t=>t.id),["first","later"]);
});
test("empty collection returns no promotion", async () => {
  const db={collection:()=>({where:()=>({get:async()=>({docs:[]})})})};
  assert.deepEqual(await findPromotion(db,now),[]);
});

test("payout projection returns only configured supported identifiers without changing eligibility", async () => {
  for (const [configured, expected] of [
    [["bkash"], ["bkash"]],
    [["rocket", "bkash", "rocket", "nagad", { account: "private" }], ["rocket", "bkash"]],
    [["nagad"], []],
    [undefined, []]
  ]) {
    const db = { collection: name => name === "tournaments"
      ? { where: () => ({ get: async () => ({ docs: [{ id: "cup", data: tournament }] }) }) }
      : { doc: () => ({ get: async () => ({ data: () => ({
        status: "active", market: "BD", minimumAge: 18,
        prizeRules: { cashPrizesEnabled: true, payoutMethods: configured, wallet: "private" }
      }) }) }) } };
    assert.deepEqual(await findPromotion(db, now), [{
      ...projectTournament("cup", tournament(), now), minimumAge: 18, payoutMethods: expected
    }]);
  }
});

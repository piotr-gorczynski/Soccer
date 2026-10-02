# Local payout rules tests

Run `npm ci` then `npm test` from this directory, with Java 21+ on PATH.
The tests start Firestore on port 8188 with the fictitious `demo-payout` project. No live Firebase
credentials or deployment are used. The pretest script copies the canonical rules locally because
Firebase CLI does not accept a rules path outside its configuration directory; that copy is ignored.

Coverage: bKash/Rocket, each required field, Nagad rejection under legacy regulation, private reads,
owner-only writes, regulation provider constraints, atomic submission, legacy client rejection and
read-only submitted state. Node module/cache/emulator logs are ignored.

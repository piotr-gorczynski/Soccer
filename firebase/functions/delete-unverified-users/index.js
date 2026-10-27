const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { shouldDeleteUnverifiedUser, deleteIfStillEligible } = require('./eligibility');

admin.initializeApp();

exports.deleteUnverifiedUsers = functions.pubsub
  .schedule("every 60 minutes")
  .onRun(async (context) => {
    const auth = admin.auth();
    const rtdb = admin.database();
    let nextPageToken = undefined;

    const now = Date.now();
    let deletedCount = 0;

    do {
      const listUsersResult = await auth.listUsers(1000, nextPageToken);
      for (const user of listUsersResult.users) {
        if (shouldDeleteUnverifiedUser(user, now)) {
          try {
            if (!await deleteIfStillEligible(auth, user.uid, Date.now(), admin.firestore())) continue;
            console.log(`🧹 Deleted unverified user from Auth: ${user.email}`);

            // Retain the UID anchor; never delete historical references.
            // onAccountDeleted owns profile minimization and legacy-consent preservation.
            // Do not delete/replace the profile here before that handler reads its legal evidence.
            console.log(`🧹 Queued profile minimization via Auth deletion: users/${user.uid}`);

            // Deactivate presence without allowing stale clients to recreate it
            try {
              await rtdb.ref('status').child(user.uid).set({ accountDeleted: true, state: 'offline' });
              console.log(`🧹 Deactivated user status in RTDB: status/${user.uid}`);
            } catch (rtdbErr) {
              console.error(`⚠️ Failed to delete RTDB status for ${user.uid}: ${rtdbErr.message}`);
              // Don't throw - main deletion is complete
            }

            deletedCount++;
          } catch (err) {
            console.error(`❌ Failed to delete user ${user.uid}: ${err.message}`);
          }
        }
      }
      nextPageToken = listUsersResult.pageToken;
    } while (nextPageToken);

    console.log(`✅ Cleanup complete. Deleted ${deletedCount} unverified users.`);
    return null;
  });

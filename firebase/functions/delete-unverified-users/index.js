const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.deleteUnverifiedUsers = functions.pubsub
  .schedule("every 60 minutes")
  .onRun(async (context) => {
    const auth = admin.auth();
    const db = admin.firestore();
    const rtdb = admin.database();
    let nextPageToken = undefined;

    const now = Date.now();
    const oneHourMillis = 60 * 60 * 1000;
    let deletedCount = 0;

    do {
      const listUsersResult = await auth.listUsers(1000, nextPageToken);
      for (const user of listUsersResult.users) {
        if (
          !user.emailVerified &&
          user.metadata.creationTime &&
          now - new Date(user.metadata.creationTime).getTime() > oneHourMillis
        ) {
          try {
            // Check user's login method in Firestore before deleting
            const userDoc = await db.collection("users").doc(user.uid).get();
            const method = userDoc.exists ? userDoc.data().method : null;
            
            // Skip deletion for Facebook users as they don't require email verification
            if (method === "facebook.com") {
              console.log(`⏭️ Skipping Facebook user: ${user.uid} (no email verification required)`);
              continue;
            }

            // Skip deletion for anonymous users as they don't have email verification
            if (method === "anonymous") {
              console.log(`⏭️ Skipping anonymous user: ${user.uid} (no email verification required)`);
              continue;
            }

            await auth.deleteUser(user.uid);
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

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.deleteUnverifiedUsers = functions.pubsub
  .schedule("every 60 minutes")
  .onRun(async (context) => {
    const auth = admin.auth();
    const db = admin.firestore();
    let nextPageToken = undefined;

    const now = Date.now();
    const oneDayMillis = 24 * 60 * 60 * 1000;
    let deletedCount = 0;

    do {
      const listUsersResult = await auth.listUsers(1000, nextPageToken);
      for (const user of listUsersResult.users) {
        if (
          !user.emailVerified &&
          user.metadata.creationTime &&
          now - new Date(user.metadata.creationTime).getTime() > oneDayMillis
        ) {
          try {
            await auth.deleteUser(user.uid);
            console.log(`🧹 Deleted unverified user from Auth: ${user.email}`);

            // Also delete from Firestore
            await db.collection("users").doc(user.uid).delete();
            console.log(`🧹 Deleted user document from Firestore: users/${user.uid}`);

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

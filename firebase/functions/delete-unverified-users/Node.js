const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.deleteUnverifiedUsers = functions.pubsub
  .schedule("every 60 minutes")
  .onRun(async (context) => {
    const auth = admin.auth();
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
          await auth.deleteUser(user.uid);
          console.log(`Deleted unverified user: ${user.email}`);
          deletedCount++;
        }
      }
      nextPageToken = listUsersResult.pageToken;
    } while (nextPageToken);

    console.log(`Cleanup complete. Deleted ${deletedCount} unverified users.`);
    return null;
  });

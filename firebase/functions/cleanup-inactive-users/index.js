const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.cleanupInactiveUsers = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async (context) => {
    const auth = admin.auth();
    const db = admin.firestore();
    const rtdb = admin.database();
    let nextPageToken = undefined;

    const now = Date.now();
    const oneMonthMillis = 30 * 24 * 60 * 60 * 1000; // 30 days in milliseconds
    let deletedCount = 0;
    const deletedUsers = [];

    console.log(`🧹 Starting cleanup of inactive users without terms acceptance...`);

    do {
      const listUsersResult = await auth.listUsers(1000, nextPageToken);
      
      for (const user of listUsersResult.users) {
        // Check if user has been inactive for more than 1 month
        const lastSignInTime = user.metadata.lastSignInTime 
          ? new Date(user.metadata.lastSignInTime).getTime()
          : new Date(user.metadata.creationTime).getTime(); // Fallback to creation time if never signed in
        
        if (now - lastSignInTime > oneMonthMillis) {
          try {
            // Check if user has accepted terms in Firestore
            const userDoc = await db.collection("users").doc(user.uid).get();
            
            if (userDoc.exists) {
              const userData = userDoc.data();
              const termsAccepted = userData.termsAccepted;
              
              // Only delete if terms are not accepted (missing or false)
              if (termsAccepted !== true) {
                await deleteUserCompletely(auth, db, rtdb, user.uid, user.email);
                deletedCount++;
                deletedUsers.push({
                  uid: user.uid,
                  email: user.email,
                  lastSignInTime: user.metadata.lastSignInTime || user.metadata.creationTime,
                  termsAccepted: termsAccepted
                });
                
                console.log(`🧹 Deleted inactive user: ${user.email} (UID: ${user.uid})`);
              } else {
                console.log(`⏭️ Skipping user with accepted terms: ${user.email} (UID: ${user.uid})`);
              }
            } else {
              // User document doesn't exist in Firestore, but exists in Auth - delete
              await deleteUserCompletely(auth, db, rtdb, user.uid, user.email);
              deletedCount++;
              deletedUsers.push({
                uid: user.uid,
                email: user.email,
                lastSignInTime: user.metadata.lastSignInTime || user.metadata.creationTime,
                termsAccepted: null
              });
              
              console.log(`🧹 Deleted orphaned user (no Firestore doc): ${user.email} (UID: ${user.uid})`);
            }
          } catch (err) {
            console.error(`❌ Failed to process user ${user.uid}: ${err.message}`);
          }
        }
      }
      
      nextPageToken = listUsersResult.pageToken;
    } while (nextPageToken);

    // Log summary
    console.log(`✅ Cleanup complete. Deleted ${deletedCount} inactive users without terms acceptance.`);
    
    if (deletedUsers.length > 0) {
      console.log(`📋 Deleted users summary:`);
      deletedUsers.forEach(user => {
        console.log(`   - ${user.email} (UID: ${user.uid}, Last Sign In: ${user.lastSignInTime}, Terms: ${user.termsAccepted})`);
      });
    }

    return null;
  });

/**
 * Completely delete a user from all systems
 */
async function deleteUserCompletely(auth, db, rtdb, uid, email) {
  // 1. Delete from Firebase Authentication
  try {
    await auth.deleteUser(uid);
    console.log(`   🔐 Deleted from Auth: ${email}`);
  } catch (err) {
    console.error(`   ❌ Failed to delete from Auth: ${uid}: ${err.message}`);
    throw err; // Re-throw to prevent partial cleanup
  }

  // 2. Delete from Firestore users collection
  try {
    await db.collection("users").doc(uid).delete();
    console.log(`   📄 Deleted from Firestore users: ${uid}`);
  } catch (err) {
    console.error(`   ❌ Failed to delete from Firestore users: ${uid}: ${err.message}`);
    // Don't throw - user is already deleted from Auth, continue cleanup
  }

  // 3. Delete from realtime database status
  try {
    await rtdb.ref('status').child(uid).remove();
    console.log(`   🔄 Deleted from realtime database status: ${uid}`);
  } catch (err) {
    console.error(`   ❌ Failed to delete from realtime database status: ${uid}: ${err.message}`);
    // Don't throw - continue with friends cleanup
  }

  // 4. Remove from all users' friends collections
  try {
    await removeFromAllFriendsCollections(db, uid);
    console.log(`   👥 Removed from all friends collections: ${uid}`);
  } catch (err) {
    console.error(`   ❌ Failed to cleanup friends collections: ${uid}: ${err.message}`);
    // Don't throw - main deletion is complete
  }
}

/**
 * Remove a user ID from all other users' friends subcollections
 */
async function removeFromAllFriendsCollections(db, uidToRemove) {
  // Query all users to find friends subcollections that contain this user
  const usersSnapshot = await db.collection('users').get();
  
  const batch = db.batch();
  let friendsRemovalCount = 0;

  for (const userDoc of usersSnapshot.docs) {
    const userUid = userDoc.id;
    
    // Skip the user being deleted (though their doc should already be gone)
    if (userUid === uidToRemove) {
      continue;
    }
    
    // Check if this user has the deleted user as a friend
    const friendRef = db.collection('users').doc(userUid).collection('friends').doc(uidToRemove);
    const friendDoc = await friendRef.get();
    
    if (friendDoc.exists) {
      batch.delete(friendRef);
      friendsRemovalCount++;
    }
  }
  
  if (friendsRemovalCount > 0) {
    await batch.commit();
    console.log(`   👥 Removed ${uidToRemove} from ${friendsRemovalCount} friends collections`);
  }
}
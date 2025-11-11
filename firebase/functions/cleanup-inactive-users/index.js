const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.cleanupInactiveUsers = functions
  .region('us-central1')
  .pubsub
  .schedule("every 24 hours")
  .timeZone('Europe/Warsaw')
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
        try {
          // First, check if user has accepted terms in Firestore and get method
          const userDoc = await db.collection("users").doc(user.uid).get();
          let method = null;
          
          if (userDoc.exists) {
            method = userDoc.data().method;
          }
          
          // For anonymous users, use creationTime since their lastSignInTime 
          // is automatically updated by Firebase Auth token refresh
          // For other users, use lastSignInTime or fallback to creationTime
          const isAnonymous = method === "anonymous";
          const lastSignInTime = isAnonymous
            ? new Date(user.metadata.creationTime).getTime()
            : (user.metadata.lastSignInTime 
                ? new Date(user.metadata.lastSignInTime).getTime()
                : new Date(user.metadata.creationTime).getTime());
          
          const daysSinceLastActivity = Math.floor((now - lastSignInTime) / (24 * 60 * 60 * 1000));
          
          if (now - lastSignInTime > oneMonthMillis) {
            if (userDoc.exists) {
              const userData = userDoc.data();
              const termsAccepted = userData.termsAccepted;
              
              // Only delete if terms are not accepted (missing or false)
              if (termsAccepted !== true) {
                // Check for active involvement before deletion
                const canDelete = await checkUserCanBeDeleted(db, user.uid, user.email);
                
                if (canDelete) {
                  await deleteUserCompletely(auth, db, rtdb, user.uid, user.email);
                  deletedCount++;
                  deletedUsers.push({
                    uid: user.uid,
                    email: user.email,
                    creationTime: user.metadata.creationTime,
                    lastSignInTime: user.metadata.lastSignInTime,
                    daysSinceLastActivity: daysSinceLastActivity,
                    termsAccepted: termsAccepted,
                    method: method
                  });
                  
                  const activityNote = isAnonymous ? " (anonymous user - using creation time)" : "";
                  console.log(`🧹 Deleted inactive user: ${user.email || 'anonymous'} (UID: ${user.uid}, inactive for ${daysSinceLastActivity} days${activityNote})`);
                } else {
                  console.log(`⏭️ Skipping user with active involvement: ${user.email || 'anonymous'} (UID: ${user.uid}, inactive for ${daysSinceLastActivity} days)`);
                }
              } else {
                console.log(`⏭️ Skipping user with accepted terms: ${user.email || 'anonymous'} (UID: ${user.uid}, inactive for ${daysSinceLastActivity} days)`);
              }
            } else {
              // User document doesn't exist in Firestore, but exists in Auth
              // Still check for active involvement before deletion
              const canDelete = await checkUserCanBeDeleted(db, user.uid, user.email);
              
              if (canDelete) {
                await deleteUserCompletely(auth, db, rtdb, user.uid, user.email);
                deletedCount++;
                deletedUsers.push({
                  uid: user.uid,
                  email: user.email,
                  creationTime: user.metadata.creationTime,
                  lastSignInTime: user.metadata.lastSignInTime,
                  daysSinceLastActivity: daysSinceLastActivity,
                  termsAccepted: null,
                  method: null
                });
                
                console.log(`🧹 Deleted orphaned user (no Firestore doc): ${user.email || 'anonymous'} (UID: ${user.uid}, inactive for ${daysSinceLastActivity} days)`);
              } else {
                console.log(`⏭️ Skipping orphaned user with active involvement: ${user.email || 'anonymous'} (UID: ${user.uid}, inactive for ${daysSinceLastActivity} days)`);
              }
            }
          }
        } catch (err) {
          console.error(`❌ Failed to process user ${user.uid}: ${err.message}`);
        }
      }
      
      nextPageToken = listUsersResult.pageToken;
    } while (nextPageToken);

    // Log summary
    console.log(`✅ Cleanup complete. Deleted ${deletedCount} inactive users without terms acceptance.`);
    
    if (deletedUsers.length > 0) {
      console.log(`📋 Deleted users summary:`);
      deletedUsers.forEach(user => {
        const timeUsed = user.method === 'anonymous' ? 'creation' : 'lastSignIn';
        console.log(`   - ${user.email || 'anonymous'} (UID: ${user.uid}, inactive: ${user.daysSinceLastActivity} days, terms: ${user.termsAccepted}, method: ${user.method}, time used: ${timeUsed})`);
      });
    }

    return null;
  });

/**
 * Check if a user can be safely deleted by verifying they don't have active involvement
 * in invitations, matches, tournaments, or tournament participation
 */
async function checkUserCanBeDeleted(db, uid, email) {
  console.log(`   🔍 Checking if user ${email} (${uid}) can be safely deleted...`);
  
  try {
    // 1. Check for invitations sent by this user
    const invitationsQuery = db.collection('invitations')
      .where('from', '==', uid)
      .where('status', '==', 'pending');
    
    const invitationsSnapshot = await invitationsQuery.get();
    if (!invitationsSnapshot.empty) {
      console.log(`   ⚠️ User ${email} cannot be deleted: has ${invitationsSnapshot.size} pending invitation(s) sent`);
      return false;
    }
    
    // 2. Check for matches where user is player0 or player1
    const matchesQuery1 = db.collection('matches').where('player0', '==', uid);
    const matchesQuery2 = db.collection('matches').where('player1', '==', uid);
    
    const [matchesSnapshot1, matchesSnapshot2] = await Promise.all([
      matchesQuery1.get(),
      matchesQuery2.get()
    ]);
    
    if (!matchesSnapshot1.empty || !matchesSnapshot2.empty) {
      const totalMatches = matchesSnapshot1.size + matchesSnapshot2.size;
      console.log(`   ⚠️ User ${email} cannot be deleted: involved in ${totalMatches} match(es)`);
      return false;
    }
    
    // 3. Check for tournament matches where user is player0 or player1
    const tournamentMatchesQuery1 = db.collectionGroup('matches').where('player0', '==', uid);
    const tournamentMatchesQuery2 = db.collectionGroup('matches').where('player1', '==', uid);
    
    const [tournamentMatchesSnapshot1, tournamentMatchesSnapshot2] = await Promise.all([
      tournamentMatchesQuery1.get(),
      tournamentMatchesQuery2.get()
    ]);
    
    if (!tournamentMatchesSnapshot1.empty || !tournamentMatchesSnapshot2.empty) {
      const totalTournamentMatches = tournamentMatchesSnapshot1.size + tournamentMatchesSnapshot2.size;
      console.log(`   ⚠️ User ${email} cannot be deleted: involved in ${totalTournamentMatches} tournament match(es)`);
      return false;
    }
    
    // 4. Check for tournament participation
    const participantsQuery = db.collectionGroup('participants').where(admin.firestore.FieldPath.documentId(), '==', uid);
    const participantsSnapshot = await participantsQuery.get();
    
    if (!participantsSnapshot.empty) {
      console.log(`   ⚠️ User ${email} cannot be deleted: participating in ${participantsSnapshot.size} tournament(s)`);
      return false;
    }
    
    console.log(`   ✅ User ${email} can be safely deleted - no active involvement found`);
    return true;
    
  } catch (err) {
    console.error(`   ❌ Error checking user involvement for ${email} (${uid}): ${err.message}`);
    // In case of error, err on the side of caution and don't delete
    return false;
  }
}

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
  // Use collectionGroup query to find all friend documents with this UID
  // This is more efficient than querying all users individually
  const friendsQuery = db.collectionGroup('friends').where(admin.firestore.FieldPath.documentId(), '==', uidToRemove);
  const friendsSnapshot = await friendsQuery.get();
  
  if (friendsSnapshot.empty) {
    console.log(`   👥 No friend relationships found for ${uidToRemove}`);
    return;
  }

  const batch = db.batch();
  let friendsRemovalCount = 0;

  friendsSnapshot.forEach(doc => {
    batch.delete(doc.ref);
    friendsRemovalCount++;
  });
  
  if (friendsRemovalCount > 0) {
    await batch.commit();
    console.log(`   👥 Removed ${uidToRemove} from ${friendsRemovalCount} friends collections`);
  }
}
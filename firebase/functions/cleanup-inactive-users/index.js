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
        // Check if user has been inactive for more than 1 month
        const lastSignInTime = user.metadata.lastSignInTime 
          ? new Date(user.metadata.lastSignInTime).getTime()
          : new Date(user.metadata.creationTime).getTime(); // Fallback to creation time if never signed in
        
        const daysSinceLastActivity = Math.floor((now - lastSignInTime) / (24 * 60 * 60 * 1000));
        
        if (now - lastSignInTime > oneMonthMillis) {
          try {
            // Check if user has accepted terms in Firestore
            const userDoc = await db.collection("users").doc(user.uid).get();
            
            if (userDoc.exists) {
              const userData = userDoc.data();
              const termsAccepted = userData.termsAccepted;
              const method = userData.method; // Also check login method for additional context
              const nickname = userData.nickname || null; // Get user's nickname
              
              // Only delete if terms are not accepted (missing or false)
              if (termsAccepted !== true) {
                // Check for active involvement before deletion
                const canDelete = await checkUserCanBeDeleted(db, user.uid, user.email, nickname);
                
                if (canDelete) {
                  await deleteUserCompletely(auth, db, rtdb, user.uid, user.email, nickname);
                  deletedCount++;
                  deletedUsers.push({
                    uid: user.uid,
                    email: user.email,
                    nickname: nickname,
                    lastSignInTime: user.metadata.lastSignInTime || user.metadata.creationTime,
                    daysSinceLastActivity: daysSinceLastActivity,
                    termsAccepted: termsAccepted,
                    method: method
                  });
                  
                  console.log(`🧹 Deleted inactive user: ${user.email || user.uid} (UID: ${user.uid}, nickname: ${nickname || 'N/A'}, inactive for ${daysSinceLastActivity} days)`);
                } else {
                  console.log(`⏭️ SKIPPED - Active involvement detected for: ${user.email || user.uid} (UID: ${user.uid}, nickname: ${nickname || 'N/A'}, inactive for ${daysSinceLastActivity} days) - See detailed checks above`);
                }
              } else {
                // Terms accepted - check for fcmErrorType="NotRegistered"
                const fcmErrorType = userData.fcmErrorType;
                
                if (fcmErrorType === "NotRegistered") {
                  console.log(`🔍 User with accepted terms has fcmErrorType="NotRegistered": ${user.email || user.uid} (UID: ${user.uid}, nickname: ${nickname || 'N/A'}, method: ${method})`);
                  
                  if (method === "anonymous") {
                    // For anonymous users, try to delete first
                    const canDelete = await checkUserCanBeDeleted(db, user.uid, user.email, nickname);
                    
                    if (canDelete) {
                      await deleteUserCompletely(auth, db, rtdb, user.uid, user.email, nickname);
                      deletedCount++;
                      deletedUsers.push({
                        uid: user.uid,
                        email: user.email,
                        nickname: nickname,
                        lastSignInTime: user.metadata.lastSignInTime || user.metadata.creationTime,
                        daysSinceLastActivity: daysSinceLastActivity,
                        termsAccepted: termsAccepted,
                        method: method,
                        fcmErrorType: fcmErrorType
                      });
                      
                      console.log(`🧹 Deleted anonymous user with fcmErrorType="NotRegistered": ${user.email || user.uid} (UID: ${user.uid}, nickname: ${nickname || 'N/A'}, inactive for ${daysSinceLastActivity} days)`);
                    } else {
                      // If deletion not possible, force logoff instead
                      await forceUserLogoff(rtdb, user.uid, user.email, nickname);
                      console.log(`🔓 Forced logoff for anonymous user with fcmErrorType="NotRegistered" (active involvement detected): ${user.email || user.uid} (UID: ${user.uid}, nickname: ${nickname || 'N/A'}, method: ${method})`);
                    }
                  } else {
                    // For non-anonymous users, force logoff by setting state to "offline" in RTDB
                    await forceUserLogoff(rtdb, user.uid, user.email, nickname);
                    console.log(`🔓 Forced logoff for user with fcmErrorType="NotRegistered": ${user.email || user.uid} (UID: ${user.uid}, nickname: ${nickname || 'N/A'}, method: ${method})`);
                  }
                } else {
                  console.log(`⏭️ Skipping user with accepted terms (no fcmErrorType="NotRegistered"): ${user.email || user.uid} (UID: ${user.uid}, nickname: ${nickname || 'N/A'}, inactive for ${daysSinceLastActivity} days)`);
                }
              }
            } else {
              // User document doesn't exist in Firestore, but exists in Auth
              // Still check for active involvement before deletion
              const canDelete = await checkUserCanBeDeleted(db, user.uid, user.email, null);
              
              if (canDelete) {
                await deleteUserCompletely(auth, db, rtdb, user.uid, user.email, null);
                deletedCount++;
                deletedUsers.push({
                  uid: user.uid,
                  email: user.email,
                  nickname: null,
                  lastSignInTime: user.metadata.lastSignInTime || user.metadata.creationTime,
                  daysSinceLastActivity: daysSinceLastActivity,
                  termsAccepted: null,
                  method: null
                });
                
                console.log(`🧹 Deleted orphaned user (no Firestore doc): ${user.email || user.uid} (UID: ${user.uid}, nickname: ${'N/A'}, inactive for ${daysSinceLastActivity} days)`);
              } else {
                console.log(`⏭️ SKIPPED - Active involvement detected for orphaned user: ${user.email || user.uid} (UID: ${user.uid}, nickname: ${'N/A'}, inactive for ${daysSinceLastActivity} days) - See detailed checks above`);
              }
            }
          } catch (err) {
            console.error(`❌ Failed to process user ${user.uid} (${user.email || 'no email'}): ${err.message}`, err);
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
        console.log(`   - ${user.email} (UID: ${user.uid}, nickname: ${user.nickname || 'N/A'}, inactive: ${user.daysSinceLastActivity} days, terms: ${user.termsAccepted}, method: ${user.method})`);
      });
    }

    return null;
  });

/**
 * Force a user to logoff by setting their state to "offline" and last_heartbeat to 0 in realtime database
 */
async function forceUserLogoff(rtdb, uid, email, nickname) {
  const displayName = email || uid;
  console.log(`   🔓 Forcing logoff for user ${displayName} (UID: ${uid}, nickname: ${nickname || 'N/A'})...`);
  
  try {
    await rtdb.ref('status').child(uid).update({
      state: "offline",
      last_heartbeat: 0
    });
    console.log(`   ✅ Successfully set state to "offline" and last_heartbeat to 0 for ${displayName} (nickname: ${nickname || 'N/A'})`);
  } catch (err) {
    console.error(`   ❌ Failed to force logoff for ${displayName} (UID: ${uid}, nickname: ${nickname || 'N/A'}): ${err.message}`, err);
    throw err;
  }
}

/**
 * Check if a user can be safely deleted by verifying they don't have active involvement
 * in invitations, matches, tournaments, or tournament participation
 */
async function checkUserCanBeDeleted(db, uid, email, nickname) {
  const displayName = email || uid;
  console.log(`   🔍 Checking if user ${displayName} (UID: ${uid}, nickname: ${nickname || 'N/A'}) can be safely deleted...`);
  
  try {
    // 1. Check for invitations involving this user (both sent and received)
    console.log(`   📨 Checking invitations for ${displayName} (nickname: ${nickname || 'N/A'})...`);
    const invitationsFromQuery = db.collection('invitations')
      .where('from', '==', uid);
    const invitationsToQuery = db.collection('invitations')
      .where('to', '==', uid);
    
    const [invitationsFromSnapshot, invitationsToSnapshot] = await Promise.all([
      invitationsFromQuery.get(),
      invitationsToQuery.get()
    ]);
    
    const totalInvitations = invitationsFromSnapshot.size + invitationsToSnapshot.size;
    if (totalInvitations > 0) {
      console.log(`   ⚠️ BLOCKED: User ${displayName} (nickname: ${nickname || 'N/A'}) has ${invitationsFromSnapshot.size} invitation(s) sent and ${invitationsToSnapshot.size} invitation(s) received`);
      return false;
    }
    console.log(`   ✓ No invitations found`);
    
    // 2. Check for matches where user is player0 or player1
    console.log(`   ⚽ Checking matches for ${displayName} (nickname: ${nickname || 'N/A'})...`);
    const matchesQuery1 = db.collection('matches').where('player0', '==', uid);
    const matchesQuery2 = db.collection('matches').where('player1', '==', uid);
    
    const [matchesSnapshot1, matchesSnapshot2] = await Promise.all([
      matchesQuery1.get(),
      matchesQuery2.get()
    ]);
    
    if (!matchesSnapshot1.empty || !matchesSnapshot2.empty) {
      const totalMatches = matchesSnapshot1.size + matchesSnapshot2.size;
      console.log(`   ⚠️ BLOCKED: User ${displayName} (nickname: ${nickname || 'N/A'}) is involved in ${totalMatches} match(es)`);
      return false;
    }
    console.log(`   ✓ No matches found`);
    
    // 3. Check for tournament matches where user is player0 or player1
    console.log(`   🏆 Checking tournament matches for ${displayName} (nickname: ${nickname || 'N/A'})...`);
    const tournamentMatchesQuery1 = db.collectionGroup('matches').where('player0', '==', uid);
    const tournamentMatchesQuery2 = db.collectionGroup('matches').where('player1', '==', uid);
    
    const [tournamentMatchesSnapshot1, tournamentMatchesSnapshot2] = await Promise.all([
      tournamentMatchesQuery1.get(),
      tournamentMatchesQuery2.get()
    ]);
    
    if (!tournamentMatchesSnapshot1.empty || !tournamentMatchesSnapshot2.empty) {
      const totalTournamentMatches = tournamentMatchesSnapshot1.size + tournamentMatchesSnapshot2.size;
      console.log(`   ⚠️ BLOCKED: User ${displayName} (nickname: ${nickname || 'N/A'}) is involved in ${totalTournamentMatches} tournament match(es)`);
      return false;
    }
    console.log(`   ✓ No tournament matches found`);
    
    // 4. Check for tournament participation
    console.log(`   🎖️ Checking tournament participation for ${displayName} (nickname: ${nickname || 'N/A'})...`);
    // Note: We can't use collectionGroup with FieldPath.documentId() and a simple UID
    // because Firestore requires a full document path (odd number of segments error).
    // Instead, we query all tournaments and check if user is a participant.
    const tournamentsSnapshot = await db.collection('tournaments').get();
    let participantCount = 0;
    
    for (const tournamentDoc of tournamentsSnapshot.docs) {
      const participantDoc = await tournamentDoc.ref.collection('participants').doc(uid).get();
      if (participantDoc.exists) {
        participantCount++;
      }
    }
    
    if (participantCount > 0) {
      console.log(`   ⚠️ BLOCKED: User ${displayName} (nickname: ${nickname || 'N/A'}) is participating in ${participantCount} tournament(s)`);
      return false;
    }
    console.log(`   ✓ No tournament participation found`);
    
    // 5. Check if user exists in any friends collections
    console.log(`   👥 Checking if user exists in friends collections...`);
    const usersSnapshot = await db.collection('users').get();
    const friendsOfUsers = [];
    
    for (const userDoc of usersSnapshot.docs) {
      const friendDoc = await userDoc.ref.collection('friends').doc(uid).get();
      if (friendDoc.exists) {
        friendsOfUsers.push(userDoc.id);
      }
    }
    
    if (friendsOfUsers.length > 0) {
      console.log(`   ⚠️ BLOCKED: User ${displayName} (nickname: ${nickname || 'N/A'}) exists in friend collection of user(s): ${friendsOfUsers.join(', ')}`);
      return false;
    }
    console.log(`   ✓ User not found in any friends collections`);
    
    console.log(`   ✅ User ${displayName} (nickname: ${nickname || 'N/A'}) CAN be safely deleted - no active involvement found`);
    return true;
    
  } catch (err) {
    console.error(`   ❌ ERROR checking user involvement for ${displayName} (UID: ${uid}, nickname: ${nickname || 'N/A'}): ${err.message}`, err);
    // In case of error, err on the side of caution and don't delete
    return false;
  }
}

/**
 * Completely delete a user from all systems
 */
async function deleteUserCompletely(auth, db, rtdb, uid, email, nickname) {
  // 1. Delete from Firebase Authentication
  try {
    await auth.deleteUser(uid);
    console.log(`   🔐 Deleted from Auth: ${email || uid} (nickname: ${nickname || 'N/A'})`);
  } catch (err) {
    console.error(`   ❌ Failed to delete from Auth: ${uid} (nickname: ${nickname || 'N/A'}): ${err.message}`, err);
    throw err; // Re-throw to prevent partial cleanup
  }

  // 2. Delete from Firestore users collection
  try {
    await db.collection("users").doc(uid).delete();
    console.log(`   📄 Deleted from Firestore users: ${uid} (nickname: ${nickname || 'N/A'})`);
  } catch (err) {
    console.error(`   ❌ Failed to delete from Firestore users: ${uid} (nickname: ${nickname || 'N/A'}): ${err.message}`, err);
    // Don't throw - user is already deleted from Auth, continue cleanup
  }

  // 3. Delete from realtime database status
  try {
    await rtdb.ref('status').child(uid).remove();
    console.log(`   🔄 Deleted from realtime database status: ${uid} (nickname: ${nickname || 'N/A'})`);
  } catch (err) {
    console.error(`   ❌ Failed to delete from realtime database status: ${uid} (nickname: ${nickname || 'N/A'}): ${err.message}`, err);
    // Don't throw - continue with friends cleanup
  }

  // 4. Remove from all users' friends collections
  try {
    await removeFromAllFriendsCollections(db, uid, nickname);
    console.log(`   👥 Removed from all friends collections: ${uid} (nickname: ${nickname || 'N/A'})`);
  } catch (err) {
    console.error(`   ❌ Failed to cleanup friends collections: ${uid} (nickname: ${nickname || 'N/A'}): ${err.message}`, err);
    // Don't throw - main deletion is complete
  }
}

/**
 * Remove a user ID from all other users' friends subcollections
 */
async function removeFromAllFriendsCollections(db, uidToRemove, nickname) {
  // Query all users and check their friends subcollections for this UID
  // We can't use collectionGroup with FieldPath.documentId() and a simple UID
  // because Firestore requires full document paths, not just document IDs
  const usersSnapshot = await db.collection('users').get();
  
  const batch = db.batch();
  let friendsRemovalCount = 0;

  for (const userDoc of usersSnapshot.docs) {
    const friendDoc = await userDoc.ref.collection('friends').doc(uidToRemove).get();
    if (friendDoc.exists) {
      batch.delete(friendDoc.ref);
      friendsRemovalCount++;
    }
  }
  
  if (friendsRemovalCount > 0) {
    await batch.commit();
    console.log(`   👥 Removed ${uidToRemove} (nickname: ${nickname || 'N/A'}) from ${friendsRemovalCount} friends collections`);
  } else {
    console.log(`   👥 No friend relationships found for ${uidToRemove} (nickname: ${nickname || 'N/A'})`);
  }
}
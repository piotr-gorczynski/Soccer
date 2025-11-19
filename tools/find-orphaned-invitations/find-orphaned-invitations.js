// tools/find-orphaned-invitations/find-orphaned-invitations.js
const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

/**
 * Fetch all invitations from Firestore
 */
async function fetchInvitations(db) {
  console.log('\n📦 Fetching invitations from Firestore...');
  
  try {
    const snapshot = await db.collection('invitations').get();
    
    if (snapshot.empty) {
      console.log('   ℹ️  No invitations found in Firestore');
      return [];
    }
    
    const invitations = snapshot.docs.map(doc => ({
      id: doc.id,
      from: doc.data().from,
      to: doc.data().to
    }));
    
    console.log(`   📊 Found ${invitations.length} invitation(s) in Firestore`);
    
    return invitations;
  } catch (error) {
    console.error('   ❌ Error fetching invitations:', error.message);
    throw error;
  }
}

/**
 * Fetch all user IDs from Firestore
 */
async function fetchUserIds(db) {
  console.log('\n📦 Fetching user IDs from Firestore...');
  
  try {
    const snapshot = await db.collection('users').get();
    
    if (snapshot.empty) {
      console.log('   ℹ️  No users found in Firestore');
      return [];
    }
    
    const userIds = snapshot.docs.map(doc => doc.id);
    console.log(`   📊 Found ${userIds.length} user(s) in Firestore`);
    
    return userIds;
  } catch (error) {
    console.error('   ❌ Error fetching user IDs:', error.message);
    throw error;
  }
}

/**
 * Find orphaned invitations (where 'from' or 'to' users don't exist)
 * Returns object with:
 * - reported: invitations where EITHER from or to user doesn't exist (for reporting)
 * - toDelete: invitations where BOTH from and to users don't exist (safe to delete)
 */
function findOrphanedInvitations(invitations, userIds) {
  console.log('\n🔍 Analyzing data...');
  
  const userIdSet = new Set(userIds);
  
  // Find all invitations where EITHER user doesn't exist (for reporting)
  const reportedInvitations = invitations.filter(invitation => {
    const fromExists = userIdSet.has(invitation.from);
    const toExists = userIdSet.has(invitation.to);
    return !fromExists || !toExists;
  }).map(invitation => {
    const fromExists = userIdSet.has(invitation.from);
    const toExists = userIdSet.has(invitation.to);
    return {
      ...invitation,
      fromExists,
      toExists
    };
  });
  
  // Find invitations where BOTH users don't exist (safe to delete)
  const toDeleteInvitations = reportedInvitations.filter(invitation => {
    return !invitation.fromExists && !invitation.toExists;
  });
  
  console.log(`   📊 Total invitations: ${invitations.length}`);
  console.log(`   📊 Total user IDs: ${userIds.length}`);
  console.log(`   📊 Invitations with missing user(s): ${reportedInvitations.length}`);
  console.log(`   📊 Invitations with both users missing: ${toDeleteInvitations.length}`);
  
  return {
    reported: reportedInvitations,
    toDelete: toDeleteInvitations
  };
}

/**
 * Delete orphaned invitations from Firestore
 */
async function deleteOrphanedInvitations(db, orphanedInvitations) {
  console.log('\n🗑️  Deleting orphaned invitations...');
  
  if (orphanedInvitations.length === 0) {
    console.log('   ℹ️  No invitations to delete');
    return;
  }
  
  let successCount = 0;
  let errorCount = 0;
  
  for (const invitation of orphanedInvitations) {
    try {
      await db.collection('invitations').doc(invitation.id).delete();
      successCount++;
      console.log(`   ✅ Deleted: ${invitation.id} (from: ${invitation.from}, to: ${invitation.to}) - ${successCount}/${orphanedInvitations.length}`);
    } catch (error) {
      errorCount++;
      console.error(`   ❌ Failed to delete ${invitation.id}: ${error.message}`);
    }
  }
  
  console.log(`\n   📊 Deletion complete:`);
  console.log(`      ✅ Successfully deleted: ${successCount}`);
  if (errorCount > 0) {
    console.log(`      ❌ Failed: ${errorCount}`);
  }
}

/**
 * Main function
 */
async function main() {
  const args = process.argv.slice(2);
  
  // Check for --delete flag
  const deleteMode = args.includes('--delete');
  const argsWithoutFlags = args.filter(arg => !arg.startsWith('--'));
  
  // Get environment from arguments
  const env = argsWithoutFlags[0];
  if (!env || !['dev', 'test', 'prod'].includes(env.toLowerCase())) {
    console.error('❌ Error: Environment parameter is required');
    console.error('   Usage: node find-orphaned-invitations.js <PROD|TEST|DEV> [--delete]');
    console.error('   Example: node find-orphaned-invitations.js PROD');
    console.error('   Example: node find-orphaned-invitations.js PROD --delete');
    process.exit(1);
  }
  
  const envLower = env.toLowerCase();
  
  // Load service account key
  const keyPath = path.join(__dirname, '..', '..', 'secrets', `serviceAccountKey.${envLower}.json`);
  
  if (!fs.existsSync(keyPath)) {
    console.error(`❌ Service account key not found: ${keyPath}`);
    console.error('   Please ensure the file exists at the expected location.');
    process.exit(1);
  }
  
  const serviceAccount = require(keyPath);
  
  // Initialize Firebase app
  const app = admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: serviceAccount.database_url || `https://${serviceAccount.project_id}-default-rtdb.firebaseio.com`
  });
  
  const db = app.firestore();
  
  console.log('🔥 Firebase app initialized');
  console.log(`   Environment: ${env.toUpperCase()}`);
  console.log(`   Project: ${serviceAccount.project_id}`);
  console.log(`   Mode: ${deleteMode ? '🗑️  DELETE' : '👀 READ-ONLY'}`);
  
  try {
    // Fetch data
    const invitations = await fetchInvitations(db);
    const userIds = await fetchUserIds(db);
    
    // Find orphaned invitations
    const result = findOrphanedInvitations(invitations, userIds);
    const reportedInvitations = result.reported;
    const toDeleteInvitations = result.toDelete;
    
    // Display results
    console.log('\n' + '='.repeat(60));
    console.log('RESULTS');
    console.log('='.repeat(60));
    
    if (reportedInvitations.length === 0) {
      console.log('\n✅ No orphaned invitations found!');
      console.log('   All invitations reference existing users.');
    } else {
      console.log(`\n⚠️  Found ${reportedInvitations.length} invitation(s) with missing user(s):\n`);
      reportedInvitations.forEach((invitation, index) => {
        console.log(`   ${index + 1}. ID: ${invitation.id}`);
        console.log(`      from: ${invitation.from} ${!invitation.fromExists ? '(user does not exist)' : '(user exists)'}`);
        console.log(`      to: ${invitation.to} ${!invitation.toExists ? '(user does not exist)' : '(user exists)'}`);
      });
      
      console.log(`\n📝 These ${reportedInvitations.length} invitation(s) exist in Firestore but`);
      console.log('   the sender (from) and/or receiver (to) users no longer exist.');
      
      // Show deletion info
      if (toDeleteInvitations.length > 0) {
        console.log(`\n🗑️  ${toDeleteInvitations.length} of these invitation(s) have BOTH users missing`);
        console.log('   and are safe to delete.');
      }
      
      // Delete orphaned invitations if --delete flag is provided
      if (deleteMode) {
        if (toDeleteInvitations.length > 0) {
          console.log('\n' + '='.repeat(60));
          console.log('DELETION');
          console.log('='.repeat(60));
          
          await deleteOrphanedInvitations(db, toDeleteInvitations);
        } else {
          console.log('\n   ℹ️  No invitations to delete (none have both users missing).');
        }
      } else {
        if (toDeleteInvitations.length > 0) {
          console.log('\n💡 To delete invitations with both users missing, run the script with --delete flag:');
          console.log(`   node find-orphaned-invitations.js ${env.toUpperCase()} --delete`);
        }
      }
    }
    
    console.log('\n✨ Done!\n');
  } catch (error) {
    console.error('\n💥 Fatal error:', error);
    process.exit(1);
  } finally {
    // Clean up
    await app.delete();
  }
}

// Run the script
main().catch(error => {
  console.error('\n💥 Fatal error:', error);
  process.exit(1);
});

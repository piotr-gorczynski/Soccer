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
 * Find orphaned invitations (where both 'from' and 'to' users don't exist)
 */
function findOrphanedInvitations(invitations, userIds) {
  console.log('\n🔍 Analyzing data...');
  
  const userIdSet = new Set(userIds);
  const orphanedInvitations = invitations.filter(invitation => {
    const fromExists = userIdSet.has(invitation.from);
    const toExists = userIdSet.has(invitation.to);
    // Only orphaned if BOTH from and to users don't exist
    return !fromExists && !toExists;
  });
  
  console.log(`   📊 Total invitations: ${invitations.length}`);
  console.log(`   📊 Total user IDs: ${userIds.length}`);
  console.log(`   📊 Orphaned invitations: ${orphanedInvitations.length}`);
  
  return orphanedInvitations;
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
    const orphanedInvitations = findOrphanedInvitations(invitations, userIds);
    
    // Display results
    console.log('\n' + '='.repeat(60));
    console.log('RESULTS');
    console.log('='.repeat(60));
    
    if (orphanedInvitations.length === 0) {
      console.log('\n✅ No orphaned invitations found!');
      console.log('   All invitations reference existing users.');
    } else {
      console.log(`\n⚠️  Found ${orphanedInvitations.length} orphaned invitation(s):\n`);
      orphanedInvitations.forEach((invitation, index) => {
        console.log(`   ${index + 1}. ID: ${invitation.id}`);
        console.log(`      from: ${invitation.from} (user does not exist)`);
        console.log(`      to: ${invitation.to} (user does not exist)`);
      });
      
      console.log(`\n📝 These ${orphanedInvitations.length} invitation(s) exist in Firestore but both`);
      console.log('   the sender (from) and receiver (to) users no longer exist.');
      
      // Delete orphaned invitations if --delete flag is provided
      if (deleteMode) {
        console.log('\n' + '='.repeat(60));
        console.log('DELETION');
        console.log('='.repeat(60));
        
        await deleteOrphanedInvitations(db, orphanedInvitations);
      } else {
        console.log('\n💡 To delete these orphaned invitations, run the script with --delete flag:');
        console.log(`   node find-orphaned-invitations.js ${env.toUpperCase()} --delete`);
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

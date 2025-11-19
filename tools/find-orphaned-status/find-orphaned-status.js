// tools/find-orphaned-status/find-orphaned-status.js
const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

/**
 * Fetch all status keys from RTDB
 */
async function fetchStatusKeys(rtdb) {
  console.log('\n📦 Fetching status keys from RTDB...');
  
  try {
    const snapshot = await rtdb.ref('status').once('value');
    
    if (!snapshot.exists()) {
      console.log('   ℹ️  No status data found in RTDB');
      return [];
    }
    
    const data = snapshot.val();
    const keys = Object.keys(data);
    console.log(`   📊 Found ${keys.length} status key(s) in RTDB`);
    
    return keys;
  } catch (error) {
    console.error('   ❌ Error fetching status keys:', error.message);
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
 * Find orphaned status keys
 */
function findOrphanedKeys(statusKeys, userIds) {
  console.log('\n🔍 Analyzing data...');
  
  const userIdSet = new Set(userIds);
  const orphanedKeys = statusKeys.filter(key => !userIdSet.has(key));
  
  console.log(`   📊 Total status keys: ${statusKeys.length}`);
  console.log(`   📊 Total user IDs: ${userIds.length}`);
  console.log(`   📊 Orphaned keys: ${orphanedKeys.length}`);
  
  return orphanedKeys;
}

/**
 * Main function
 */
async function main() {
  const args = process.argv.slice(2);
  
  // Get environment from arguments
  const env = args[0];
  if (!env || !['dev', 'test', 'prod'].includes(env.toLowerCase())) {
    console.error('❌ Error: Environment parameter is required');
    console.error('   Usage: node find-orphaned-status.js <PROD|TEST|DEV>');
    console.error('   Example: node find-orphaned-status.js PROD');
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
  const rtdb = app.database();
  
  console.log('🔥 Firebase app initialized');
  console.log(`   Environment: ${env.toUpperCase()}`);
  console.log(`   Project: ${serviceAccount.project_id}`);
  
  try {
    // Fetch data
    const statusKeys = await fetchStatusKeys(rtdb);
    const userIds = await fetchUserIds(db);
    
    // Find orphaned keys
    const orphanedKeys = findOrphanedKeys(statusKeys, userIds);
    
    // Display results
    console.log('\n' + '='.repeat(60));
    console.log('RESULTS');
    console.log('='.repeat(60));
    
    if (orphanedKeys.length === 0) {
      console.log('\n✅ No orphaned status keys found!');
      console.log('   All status keys have corresponding user documents.');
    } else {
      console.log(`\n⚠️  Found ${orphanedKeys.length} orphaned status key(s):\n`);
      orphanedKeys.forEach((key, index) => {
        console.log(`   ${index + 1}. ${key}`);
      });
      
      console.log(`\n📝 These ${orphanedKeys.length} status key(s) exist in RTDB but have no`);
      console.log('   corresponding user document in Firestore.');
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

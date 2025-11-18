// tools/copy-prod-to-test/copy-prod-to-test.js
const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

// Collections to copy from Firestore
const FIRESTORE_COLLECTIONS = [
  'invitations',
  'matches',
  'regulations',
  'settings',
  'tournaments',
  'users'
];

// RTDB paths to copy
const RTDB_PATHS = ['status'];

/**
 * Copy all documents from a Firestore collection
 */
async function copyFirestoreCollection(sourceDb, targetDb, collectionName) {
  console.log(`\n📦 Copying Firestore collection: ${collectionName}`);
  
  try {
    const sourceSnapshot = await sourceDb.collection(collectionName).get();
    
    if (sourceSnapshot.empty) {
      console.log(`   ℹ️  Collection '${collectionName}' is empty in PROD`);
      return { success: 0, skipped: 0, failed: 0 };
    }
    
    console.log(`   📊 Found ${sourceSnapshot.size} document(s) in PROD`);
    
    let successCount = 0;
    let failedCount = 0;
    
    let batch = targetDb.batch();
    let batchCount = 0;
    const MAX_BATCH_SIZE = 500; // Firestore limit
    
    for (const doc of sourceSnapshot.docs) {
      try {
        const targetDocRef = targetDb.collection(collectionName).doc(doc.id);
        batch.set(targetDocRef, doc.data(), { merge: true });
        batchCount++;
        
        // Commit batch if we reach the limit
        if (batchCount >= MAX_BATCH_SIZE) {
          await batch.commit();
          successCount += batchCount;
          console.log(`   ✅ Committed batch of ${batchCount} documents`);
          batch = targetDb.batch(); // Create new batch
          batchCount = 0;
        }
      } catch (error) {
        console.error(`   ❌ Error preparing document ${doc.id}:`, error.message);
        failedCount++;
      }
    }
    
    // Commit remaining documents
    if (batchCount > 0) {
      await batch.commit();
      successCount += batchCount;
      console.log(`   ✅ Committed final batch of ${batchCount} documents`);
    }
    
    console.log(`   ✅ Successfully copied ${successCount} document(s)`);
    if (failedCount > 0) {
      console.log(`   ⚠️  Failed to copy ${failedCount} document(s)`);
    }
    
    return { success: successCount, failed: failedCount };
  } catch (error) {
    console.error(`   ❌ Error copying collection '${collectionName}':`, error.message);
    return { success: 0, failed: 0, error: error.message };
  }
}

/**
 * Copy data from RTDB path
 */
async function copyRtdbPath(sourceRtdb, targetRtdb, pathName) {
  console.log(`\n📦 Copying RTDB path: ${pathName}`);
  
  try {
    const sourceSnapshot = await sourceRtdb.ref(pathName).once('value');
    
    if (!sourceSnapshot.exists()) {
      console.log(`   ℹ️  Path '${pathName}' is empty or doesn't exist in PROD`);
      return { success: 0 };
    }
    
    const data = sourceSnapshot.val();
    const keysCount = typeof data === 'object' && data !== null ? Object.keys(data).length : 1;
    console.log(`   📊 Found ${keysCount} key(s) in PROD`);
    
    await targetRtdb.ref(pathName).set(data);
    console.log(`   ✅ Successfully copied ${keysCount} key(s)`);
    
    return { success: keysCount };
  } catch (error) {
    console.error(`   ❌ Error copying RTDB path '${pathName}':`, error.message);
    return { success: 0, error: error.message };
  }
}

/**
 * Main function
 */
async function main() {
  const args = process.argv.slice(2);
  
  // Check for dry-run flag
  const dryRun = args.includes('--dry-run');
  const clearTarget = args.includes('--clear-target');
  
  if (dryRun) {
    console.log('🔍 DRY RUN MODE - No data will be written to TEST\n');
  }
  
  if (clearTarget && !dryRun) {
    console.log('⚠️  CLEAR TARGET MODE - Existing data in TEST will be deleted first\n');
  }
  
  // Load service account keys
  const prodKeyPath = path.join(__dirname, '..', '..', 'secrets', 'serviceAccountKey.prod.json');
  const testKeyPath = path.join(__dirname, '..', '..', 'secrets', 'serviceAccountKey.test.json');
  
  if (!fs.existsSync(prodKeyPath)) {
    console.error('❌ PROD service account key not found:', prodKeyPath);
    console.error('   Please ensure the file exists at the expected location.');
    process.exit(1);
  }
  
  if (!fs.existsSync(testKeyPath)) {
    console.error('❌ TEST service account key not found:', testKeyPath);
    console.error('   Please ensure the file exists at the expected location.');
    process.exit(1);
  }
  
  const prodServiceAccount = require(prodKeyPath);
  const testServiceAccount = require(testKeyPath);
  
  // Initialize PROD Firebase app
  const prodApp = admin.initializeApp({
    credential: admin.credential.cert(prodServiceAccount),
    databaseURL: prodServiceAccount.database_url || `https://${prodServiceAccount.project_id}-default-rtdb.firebaseio.com`
  }, 'prod');
  
  // Initialize TEST Firebase app
  const testApp = admin.initializeApp({
    credential: admin.credential.cert(testServiceAccount),
    databaseURL: testServiceAccount.database_url || `https://${testServiceAccount.project_id}-default-rtdb.firebaseio.com`
  }, 'test');
  
  const prodDb = prodApp.firestore();
  const testDb = testApp.firestore();
  const prodRtdb = prodApp.database();
  const testRtdb = testApp.database();
  
  console.log('🔥 Firebase apps initialized');
  console.log(`   PROD project: ${prodServiceAccount.project_id}`);
  console.log(`   TEST project: ${testServiceAccount.project_id}`);
  
  const results = {
    firestore: {},
    rtdb: {}
  };
  
  // Copy Firestore collections
  console.log('\n' + '='.repeat(60));
  console.log('FIRESTORE COLLECTIONS');
  console.log('='.repeat(60));
  
  for (const collectionName of FIRESTORE_COLLECTIONS) {
    if (dryRun) {
      console.log(`\n📦 [DRY RUN] Would copy collection: ${collectionName}`);
      try {
        const snapshot = await prodDb.collection(collectionName).get();
        console.log(`   📊 Found ${snapshot.size} document(s) in PROD`);
        results.firestore[collectionName] = { success: snapshot.size, dryRun: true };
      } catch (error) {
        console.error(`   ❌ Error reading collection:`, error.message);
        results.firestore[collectionName] = { error: error.message };
      }
    } else {
      if (clearTarget) {
        console.log(`\n🗑️  Clearing TEST collection: ${collectionName}`);
        try {
          const testSnapshot = await testDb.collection(collectionName).get();
          const batch = testDb.batch();
          testSnapshot.docs.forEach(doc => batch.delete(doc.ref));
          await batch.commit();
          console.log(`   ✅ Cleared ${testSnapshot.size} document(s) from TEST`);
        } catch (error) {
          console.error(`   ⚠️  Error clearing collection:`, error.message);
        }
      }
      
      results.firestore[collectionName] = await copyFirestoreCollection(
        prodDb,
        testDb,
        collectionName
      );
    }
  }
  
  // Copy RTDB paths
  console.log('\n' + '='.repeat(60));
  console.log('REALTIME DATABASE');
  console.log('='.repeat(60));
  
  for (const pathName of RTDB_PATHS) {
    if (dryRun) {
      console.log(`\n📦 [DRY RUN] Would copy RTDB path: ${pathName}`);
      try {
        const snapshot = await prodRtdb.ref(pathName).once('value');
        if (snapshot.exists()) {
          const keysCount = Object.keys(snapshot.val()).length;
          console.log(`   📊 Found ${keysCount} key(s) in PROD`);
          results.rtdb[pathName] = { success: keysCount, dryRun: true };
        } else {
          console.log(`   ℹ️  Path is empty or doesn't exist`);
          results.rtdb[pathName] = { success: 0, dryRun: true };
        }
      } catch (error) {
        console.error(`   ❌ Error reading path:`, error.message);
        results.rtdb[pathName] = { error: error.message };
      }
    } else {
      if (clearTarget) {
        console.log(`\n🗑️  Clearing TEST RTDB path: ${pathName}`);
        try {
          await testRtdb.ref(pathName).remove();
          console.log(`   ✅ Cleared path from TEST`);
        } catch (error) {
          console.error(`   ⚠️  Error clearing path:`, error.message);
        }
      }
      
      results.rtdb[pathName] = await copyRtdbPath(prodRtdb, testRtdb, pathName);
    }
  }
  
  // Summary
  console.log('\n' + '='.repeat(60));
  console.log('SUMMARY');
  console.log('='.repeat(60));
  
  console.log('\nFirestore Collections:');
  for (const [collection, result] of Object.entries(results.firestore)) {
    if (result.error) {
      console.log(`  ❌ ${collection}: Error - ${result.error}`);
    } else if (result.dryRun) {
      console.log(`  🔍 ${collection}: ${result.success} document(s) [DRY RUN]`);
    } else {
      console.log(`  ✅ ${collection}: ${result.success} document(s) copied`);
      if (result.failed > 0) {
        console.log(`     ⚠️  ${result.failed} document(s) failed`);
      }
    }
  }
  
  console.log('\nRealtime Database:');
  for (const [path, result] of Object.entries(results.rtdb)) {
    if (result.error) {
      console.log(`  ❌ ${path}: Error - ${result.error}`);
    } else if (result.dryRun) {
      console.log(`  🔍 ${path}: ${result.success} key(s) [DRY RUN]`);
    } else {
      console.log(`  ✅ ${path}: ${result.success} key(s) copied`);
    }
  }
  
  console.log('\n✨ Done!\n');
  
  // Clean up
  await prodApp.delete();
  await testApp.delete();
}

// Run the script
main().catch(error => {
  console.error('\n💥 Fatal error:', error);
  process.exit(1);
});

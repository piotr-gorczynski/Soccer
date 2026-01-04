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

// Batch size for authentication user operations
const AUTH_BATCH_SIZE = 1000; // Firebase Admin SDK limit for listUsers

function formatDuration(ms) {
  if (ms < 1000) {
    return `${ms}ms`;
  }

  const seconds = ms / 1000;
  if (seconds < 60) {
    return `${seconds.toFixed(2)}s`;
  }

  const minutes = Math.floor(seconds / 60);
  const remainder = (seconds % 60).toFixed(2);
  return `${minutes}m ${remainder}s`;
}

function isRetryableFirestoreError(error) {
  if (!error) {
    return false;
  }

  const message = String(error.message || '').toLowerCase();
  const retryableMessages = [
    'total timeout of api',
    'deadline exceeded',
    'deadline-exceeded',
    'etimedout',
    'econnreset',
    'unavailable',
    'resource exhausted',
    'too many requests',
  ];

  if (retryableMessages.some(text => message.includes(text))) {
    return true;
  }

  const retryableCodes = new Set([4, 8, 13, 14, 429]);
  if (retryableCodes.has(error.code)) {
    return true;
  }

  return false;
}

/**
 * Recursively delete a document and all its subcollections using BulkWriter
 */
async function deleteDocumentRecursive(docRef, bulkWriter) {
  let deletedDocs = 1;
  let subcollectionsCount = 0;
  const subcollections = await docRef.listCollections();
  subcollectionsCount += subcollections.length;

  // Process all subcollections
  for (const subcollection of subcollections) {
    const subcollectionDocs = await subcollection.get();

    for (const doc of subcollectionDocs.docs) {
      const result = await deleteDocumentRecursive(doc.ref, bulkWriter);
      deletedDocs += result.documents;
      subcollectionsCount += result.subcollections;
    }
  }

  // Delete the document itself using BulkWriter
  bulkWriter.delete(docRef);

  return { documents: deletedDocs, subcollections: subcollectionsCount };
}

/**
 * Recursively copy a document and all its subcollections using BulkWriter
 */
async function copyDocumentRecursive(sourceDocRef, targetDocRef, bulkWriter) {
  let copiedDocs = 0;
  let subcollectionsCount = 0;
  // Copy the document data
  const sourceDoc = await sourceDocRef.get();
  if (sourceDoc.exists) {
    bulkWriter.set(targetDocRef, sourceDoc.data());
    copiedDocs = 1;
  }

  // Copy all subcollections
  const subcollections = await sourceDocRef.listCollections();
  subcollectionsCount += subcollections.length;
  for (const subcollection of subcollections) {
    const subcollectionDocs = await subcollection.get();
    for (const doc of subcollectionDocs.docs) {
      const targetSubcollectionDocRef = targetDocRef.collection(subcollection.id).doc(doc.id);
      const result = await copyDocumentRecursive(doc.ref, targetSubcollectionDocRef, bulkWriter);
      copiedDocs += result.documents;
      subcollectionsCount += result.subcollections;
    }
  }

  return { documents: copiedDocs, subcollections: subcollectionsCount };
}

/**
 * Process batch results sequentially to avoid race conditions
 * @param {Array} results - Array of result objects from Promise.all
 * @param {number} successCounter - Current success count
 * @param {number} failedCounter - Current failed count
 * @param {Array} failedDocs - Array to store failed document information
 * @param {string} operationType - Type of operation (e.g., 'copying', 'deleting')
 * @returns {Object} Updated counts { successCount, failedCount }
 */
function processBatchResults(results, successCounter, failedCounter, failedDocs, operationType) {
  let newSuccessCount = successCounter;
  let newFailedCount = failedCounter;
  
  for (const result of results) {
    if (result.success) {
      newSuccessCount++;
    } else {
      console.error(`   ❌ Error ${operationType} document ${result.docId}:`, result.error.message);
      failedDocs.push({ id: result.docId, message: result.error.message });
      newFailedCount++;
    }
  }
  
  return { successCount: newSuccessCount, failedCount: newFailedCount };
}

/**
 * Create and configure a BulkWriter with retry logic
 * @param {FirebaseFirestore.Firestore} db - Firestore database instance
 * @returns {FirebaseFirestore.BulkWriter} Configured BulkWriter instance
 */
function createConfiguredBulkWriter(db) {
  const bulkWriter = db.bulkWriter();
  
  // Configure BulkWriter to handle retryable errors
  bulkWriter.onWriteError((error) => {
    if (isRetryableFirestoreError(error.error)) {
      return true; // Retry
    }
    return false; // Don't retry
  });
  
  return bulkWriter;
}

/**
 * Copy all documents from a Firestore collection using BulkWriter
 */
async function copyFirestoreCollection(sourceDb, targetDb, collectionName) {
  console.log(`\n📦 Copying Firestore collection: ${collectionName}`);
  
  try {
    const collectionStart = Date.now();
    const fetchStart = Date.now();
    const sourceSnapshot = await sourceDb.collection(collectionName).get();
    const fetchDuration = Date.now() - fetchStart;
    
    if (sourceSnapshot.empty) {
      console.log(`   ℹ️  Collection '${collectionName}' is empty in PROD (${formatDuration(fetchDuration)} to read)`);
      return { success: 0, skipped: 0, failed: 0 };
    }
    
    console.log(`   📊 Found ${sourceSnapshot.size} document(s) in PROD`);
    console.log(`   ⏱️  Read collection in ${formatDuration(fetchDuration)}`);
    
    // Create a BulkWriter for efficient batch operations
    const bulkWriter = createConfiguredBulkWriter(targetDb);

    let successCount = 0;
    let failedCount = 0;
    const failedDocs = [];

    // Process documents in parallel batches for better performance
    const PARALLEL_BATCH_SIZE = 50;
    const docPromises = [];

    for (let i = 0; i < sourceSnapshot.docs.length; i++) {
      const doc = sourceSnapshot.docs[i];
      const sourceDocRef = sourceDb.collection(collectionName).doc(doc.id);
      const targetDocRef = targetDb.collection(collectionName).doc(doc.id);

      const promise = copyDocumentRecursive(sourceDocRef, targetDocRef, bulkWriter)
        .then(copyResult => {
          return { success: true, copyResult, docId: doc.id };
        })
        .catch(error => {
          return { success: false, error, docId: doc.id };
        });

      docPromises.push(promise);

      // Process in batches to avoid overwhelming the system
      if (docPromises.length >= PARALLEL_BATCH_SIZE || i === sourceSnapshot.docs.length - 1) {
        const results = await Promise.all(docPromises);
        const counts = processBatchResults(results, successCount, failedCount, failedDocs, 'copying');
        successCount = counts.successCount;
        failedCount = counts.failedCount;
        
        // Log progress every PARALLEL_BATCH_SIZE documents
        if (successCount > 0 && (successCount % PARALLEL_BATCH_SIZE === 0 || successCount + failedCount === sourceSnapshot.size)) {
          console.log(`   📝 Copied ${successCount}/${sourceSnapshot.size} document(s)...`);
        }
        
        docPromises.length = 0; // Clear the array
      }
    }

    // Close the BulkWriter and wait for all operations to complete
    console.log(`   🔄 Committing all writes...`);
    await bulkWriter.close();

    console.log(`   ✅ Successfully copied ${successCount} document(s) with subcollections`);
    if (failedCount > 0) {
      console.log(`   ⚠️  Failed to copy ${failedCount} document(s)`);
      failedDocs.forEach(failure => {
        console.log(`      ❌ ${failure.id}: ${failure.message}`);
      });
    }

    const collectionDuration = Date.now() - collectionStart;
    const averageDuration = successCount > 0 ? collectionDuration / successCount : 0;
    console.log(`   ⏱️  Collection copy time: ${formatDuration(collectionDuration)} (avg ${formatDuration(Math.round(averageDuration))}/doc)`);
    
    return { success: successCount, failed: failedCount };
  } catch (error) {
    console.error(`   ❌ Error copying collection '${collectionName}':`, error.message);
    return { success: 0, failed: 0, error: error.message };
  }
}

/**
 * Clear all documents (and their subcollections) from a Firestore collection using BulkWriter
 */
async function clearFirestoreCollection(targetDb, collectionName) {
  console.log(`\n🗑️  Clearing TEST collection: ${collectionName}`);

  const collectionRef = targetDb.collection(collectionName);

  let deletedCount = 0;
  let failedCount = 0;
  const failedDocs = [];
  const clearStart = Date.now();

  // Process documents in parallel batches
  const PARALLEL_BATCH_SIZE = 50;

  // Keep deleting until no more documents are found
  // Always query from the beginning to avoid pagination issues when deleting
  while (true) {
    const query = collectionRef
      .orderBy(admin.firestore.FieldPath.documentId())
      .limit(200);

    const snapshot = await query.get();

    if (snapshot.empty) {
      break;
    }

    // Create a new BulkWriter for each batch to ensure clean state
    const bulkWriter = createConfiguredBulkWriter(targetDb);

    const docPromises = [];
    for (const doc of snapshot.docs) {
      const promise = deleteDocumentRecursive(doc.ref, bulkWriter)
        .then(deleteResult => {
          return { success: true, deleteResult, docId: doc.id };
        })
        .catch(error => {
          return { success: false, error, docId: doc.id };
        });

      docPromises.push(promise);

      // Process in batches to avoid overwhelming the system
      if (docPromises.length >= PARALLEL_BATCH_SIZE) {
        const results = await Promise.all(docPromises);
        const counts = processBatchResults(results, deletedCount, failedCount, failedDocs, 'deleting');
        deletedCount = counts.successCount;
        failedCount = counts.failedCount;
        
        // Log progress every PARALLEL_BATCH_SIZE documents
        if (deletedCount > 0 && deletedCount % PARALLEL_BATCH_SIZE === 0) {
          console.log(`   🗑️  Deleted ${deletedCount} document(s)...`);
        }
        
        docPromises.length = 0; // Clear the array
      }
    }

    // Wait for remaining promises in this batch
    if (docPromises.length > 0) {
      const results = await Promise.all(docPromises);
      const counts = processBatchResults(results, deletedCount, failedCount, failedDocs, 'deleting');
      deletedCount = counts.successCount;
      failedCount = counts.failedCount;
    }

    // Close the BulkWriter and commit all pending deletions before querying again
    console.log(`   🔄 Committing batch deletions...`);
    await bulkWriter.close();
  }

  console.log(`   ✅ Cleared ${deletedCount} document(s) with subcollections from TEST`);
  if (failedCount > 0) {
    console.log(`   ⚠️  Failed to delete ${failedCount} document(s)`);
    failedDocs.forEach(failure => {
      console.log(`      ❌ ${failure.id}: ${failure.message}`);
    });
  }

  const clearDuration = Date.now() - clearStart;
  console.log(`   ⏱️  Collection clear time: ${formatDuration(clearDuration)} (avg ${formatDuration(Math.round(clearDuration / Math.max(1, deletedCount)))} /doc)`);

  return { deleted: deletedCount, failed: failedCount };
}

/**
 * Clear all authentication users from TEST
 */
async function clearAuthenticationUsers(targetAuth) {
  console.log('\n🗑️  Clearing TEST Authentication users');
  
  try {
    let deletedCount = 0;
    let failedCount = 0;
    let pageToken;
    
    // First pass: Count total users
    console.log('   📊 Counting users in TEST...');
    let totalUsers = 0;
    let countPageToken;
    do {
      const listResult = await targetAuth.listUsers(AUTH_BATCH_SIZE, countPageToken);
      totalUsers += listResult.users.length;
      countPageToken = listResult.pageToken;
    } while (countPageToken);
    
    console.log(`   📊 Found ${totalUsers} user(s) in TEST`);
    
    if (totalUsers === 0) {
      console.log('   ℹ️  No users to delete');
      return { deleted: 0, failed: 0 };
    }
    
    // Second pass: Delete users
    // Keep deleting users until none remain
    while (deletedCount + failedCount < totalUsers) {
      const listResult = await targetAuth.listUsers(AUTH_BATCH_SIZE);
      const users = listResult.users;
      
      if (users.length === 0) {
        break; // No more users to delete
      }
      
      for (const user of users) {
        try {
          await targetAuth.deleteUser(user.uid);
          deletedCount++;
          
          // Log progress every 100 users
          if (deletedCount % 100 === 0) {
            console.log(`   🗑️  Deleted ${deletedCount}/${totalUsers} user(s)...`);
          }
        } catch (error) {
          console.error(`   ❌ Failed to delete user ${user.uid}:`, error.message);
          failedCount++;
        }
      }
    }
    
    console.log(`   ✅ Successfully deleted ${deletedCount} user(s)`);
    if (failedCount > 0) {
      console.log(`   ⚠️  Failed to delete ${failedCount} user(s)`);
    }
    
    return { deleted: deletedCount, failed: failedCount };
  } catch (error) {
    console.error(`   ❌ Error clearing authentication users:`, error.message);
    return { deleted: 0, failed: 0, error: error.message };
  }
}

/**
 * Copy authentication users from PROD to TEST
 */
async function copyAuthenticationUsers(sourceAuth, targetAuth) {
  console.log('\n📦 Copying Authentication users');
  
  try {
    let successCount = 0;
    let failedCount = 0;
    let pageToken;
    let totalUsers = 0;
    
    // First pass: Count total users
    console.log('   📊 Counting users in PROD...');
    let countPageToken;
    do {
      const listResult = await sourceAuth.listUsers(AUTH_BATCH_SIZE, countPageToken);
      totalUsers += listResult.users.length;
      countPageToken = listResult.pageToken;
    } while (countPageToken);
    
    console.log(`   📊 Found ${totalUsers} user(s) in PROD`);
    
    if (totalUsers === 0) {
      console.log('   ℹ️  No users to copy');
      return { success: 0, failed: 0 };
    }
    
    // Second pass: Copy users
    do {
      const listResult = await sourceAuth.listUsers(AUTH_BATCH_SIZE, pageToken);
      const users = listResult.users;
      
      if (users.length > 0) {
        const usersToImport = users.map(user => {
          const importUser = {
            uid: user.uid,
            email: user.email,
            emailVerified: user.emailVerified,
            displayName: user.displayName,
            photoURL: user.photoURL,
            disabled: user.disabled,
            metadata: {
              creationTime: user.metadata.creationTime,
              lastSignInTime: user.metadata.lastSignInTime,
            },
            providerData: user.providerData,
          };
          
          // Include password hash if available
          if (user.passwordHash) {
            importUser.passwordHash = user.passwordHash;
          }
          if (user.passwordSalt) {
            importUser.passwordSalt = user.passwordSalt;
          }
          
          // Include custom claims if present
          if (user.customClaims && Object.keys(user.customClaims).length > 0) {
            importUser.customClaims = user.customClaims;
          }
          
          // Include phone number if present
          if (user.phoneNumber) {
            importUser.phoneNumber = user.phoneNumber;
          }
          
          return importUser;
        });
        
        try {
          const importResult = await targetAuth.importUsers(usersToImport);
          successCount += importResult.successCount;
          failedCount += importResult.failureCount;
          
          if (importResult.failureCount > 0) {
            console.log(`   ⚠️  Batch: ${importResult.successCount} succeeded, ${importResult.failureCount} failed`);
            importResult.errors.forEach((error, idx) => {
              console.log(`      ❌ User ${usersToImport[idx].uid}: ${error.error.message}`);
            });
          } else {
            console.log(`   ✅ Imported batch of ${importResult.successCount} user(s)`);
          }
        } catch (error) {
          console.error(`   ❌ Error importing batch:`, error.message);
          failedCount += users.length;
        }
      }
      
      pageToken = listResult.pageToken;
    } while (pageToken);
    
    console.log(`   ✅ Successfully copied ${successCount} user(s)`);
    if (failedCount > 0) {
      console.log(`   ⚠️  Failed to copy ${failedCount} user(s)`);
    }
    
    return { success: successCount, failed: failedCount };
  } catch (error) {
    console.error(`   ❌ Error copying authentication users:`, error.message);
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
  const prodAuth = prodApp.auth();
  const testAuth = testApp.auth();
  
  console.log('🔥 Firebase apps initialized');
  console.log(`   PROD project: ${prodServiceAccount.project_id}`);
  console.log(`   TEST project: ${testServiceAccount.project_id}`);
  
  const results = {
    firestore: {},
    rtdb: {},
    authentication: {}
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
        try {
          await clearFirestoreCollection(testDb, collectionName);
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
          const data = snapshot.val();
          const keysCount = typeof data === 'object' && data !== null ? Object.keys(data).length : 1;
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
  
  // Copy Authentication users
  console.log('\n' + '='.repeat(60));
  console.log('AUTHENTICATION');
  console.log('='.repeat(60));
  
  if (dryRun) {
    console.log('\n📦 [DRY RUN] Would copy Authentication users');
    try {
      const listResult = await prodAuth.listUsers(1);
      let totalUsers = 0;
      let pageToken;
      do {
        const result = await prodAuth.listUsers(AUTH_BATCH_SIZE, pageToken);
        totalUsers += result.users.length;
        pageToken = result.pageToken;
      } while (pageToken);
      console.log(`   📊 Found ${totalUsers} user(s) in PROD`);
      results.authentication.users = { success: totalUsers, dryRun: true };
    } catch (error) {
      console.error(`   ❌ Error reading authentication users:`, error.message);
      results.authentication.users = { error: error.message };
    }
  } else {
    if (clearTarget) {
      results.authentication.cleared = await clearAuthenticationUsers(testAuth);
    }
    
    results.authentication.users = await copyAuthenticationUsers(prodAuth, testAuth);
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
  
  console.log('\nAuthentication:');
  for (const [type, result] of Object.entries(results.authentication)) {
    if (result.error) {
      console.log(`  ❌ ${type}: Error - ${result.error}`);
    } else if (result.dryRun) {
      console.log(`  🔍 ${type}: ${result.success} user(s) [DRY RUN]`);
    } else if (type === 'cleared') {
      console.log(`  🗑️  ${type}: ${result.deleted} user(s) deleted`);
      if (result.failed > 0) {
        console.log(`     ⚠️  ${result.failed} user(s) failed to delete`);
      }
    } else {
      console.log(`  ✅ ${type}: ${result.success} user(s) copied`);
      if (result.failed > 0) {
        console.log(`     ⚠️  ${result.failed} user(s) failed`);
      }
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

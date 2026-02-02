// tools/migrate-tournaments-flavours/migrate-tournaments-flavours.js
// 
// This script migrates existing tournaments to support multi-flavour visibility.
// It adds a `visibleInFlavours` field to all existing tournaments.
//
// Usage:
//   node migrate-tournaments-flavours.js <env>
//
// Where <env> is one of: dev, test, prod
//
// The script will:
// 1. Fetch all existing tournaments
// 2. Add `visibleInFlavours: ["global", "bangladesh"]` to all existing tournaments
//    (making them visible in all app flavours by default)
// 3. Log the migration results

const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

async function main() {
  const args = process.argv.slice(2);

  const env = args.shift();
  if (!['dev', 'test', 'prod'].includes(env)) {
    console.error('First argument must specify the environment: dev, test or prod');
    process.exit(1);
  }

  const serviceAccountPath = path.join(__dirname, '..', '..', 'secrets', `serviceAccountKey.${env}.json`);
  const serviceAccount = require(serviceAccountPath);

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  const db = admin.firestore();

  console.log(`\n🔄 Starting tournament flavour migration for environment: ${env}\n`);

  try {
    // Fetch all tournaments
    const tournamentsSnapshot = await db.collection('tournaments').get();
    
    if (tournamentsSnapshot.empty) {
      console.log('No tournaments found. Nothing to migrate.');
      process.exit(0);
    }

    console.log(`Found ${tournamentsSnapshot.size} tournament(s) to migrate.\n`);

    let migratedCount = 0;
    let skippedCount = 0;
    let errorCount = 0;

    // Process each tournament
    for (const doc of tournamentsSnapshot.docs) {
      const tournamentId = doc.id;
      const tournamentData = doc.data();
      const tournamentName = tournamentData.name || '(unnamed)';

      try {
        // Check if already has visibleInFlavours field
        if (tournamentData.visibleInFlavours) {
          console.log(`⏭️  Skipping ${tournamentId} ("${tournamentName}"): already has visibleInFlavours field`);
          skippedCount++;
          continue;
        }

        // Update tournament with visibleInFlavours field
        // By default, make all existing tournaments visible in all flavours using "global"
        await doc.ref.update({
          visibleInFlavours: ['global']
        });

        console.log(`✅ Migrated ${tournamentId} ("${tournamentName}"): added visibleInFlavours: ["global"]`);
        migratedCount++;

      } catch (error) {
        console.error(`❌ Error migrating ${tournamentId} ("${tournamentName}"):`, error.message);
        errorCount++;
      }
    }

    console.log('\n📊 Migration Summary:');
    console.log(`   Total tournaments: ${tournamentsSnapshot.size}`);
    console.log(`   ✅ Migrated: ${migratedCount}`);
    console.log(`   ⏭️  Skipped (already migrated): ${skippedCount}`);
    console.log(`   ❌ Errors: ${errorCount}`);
    console.log('\n✨ Migration completed!\n');

  } catch (err) {
    console.error('Fatal error during migration:', err.message);
    process.exit(1);
  }
}

main();

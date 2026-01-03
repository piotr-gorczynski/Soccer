const admin = require("firebase-admin");
const serviceAccount = require("../../secrets/serviceAccountKey.dev.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

function inferType(value) {
  if (Array.isArray(value)) return "array";
  if (value === null) return "null";
  if (value instanceof admin.firestore.Timestamp) return "timestamp";
  if (value instanceof admin.firestore.GeoPoint) return "geopoint";
  if (value instanceof admin.firestore.DocumentReference) return "reference";
  if (typeof value === "object") return "map";
  return typeof value; // string, number, boolean
}

async function describeCollectionRecursive(collRef, indent = "") {
  const snapshot = await collRef.limit(2).get();
  console.log(`${indent}📁 ${collRef.path}`);

  if (snapshot.empty) {
    console.log(`${indent}  ⚠️  No documents found`);
    return;
  }

  for (const docSnap of snapshot.docs) {
    console.log(`${indent}  📄 ${docSnap.id}`);
    const data = docSnap.data();

    for (const [key, value] of Object.entries(data)) {
      console.log(`${indent}    🔹 ${key}: ${inferType(value)}`);
    }

    const subCollections = await docSnap.ref.listCollections();
    for (const subColl of subCollections) {
      await describeCollectionRecursive(subColl, indent + "    ");
    }
  }
}

(async () => {
  const topLevelCollections = await db.listCollections();
  for (const collRef of topLevelCollections) {
    await describeCollectionRecursive(collRef);
  }
  console.log("✅ Schema (based on up to 2 docs per collection) extracted.");
})();

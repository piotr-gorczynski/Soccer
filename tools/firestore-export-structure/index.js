const admin = require("firebase-admin");
const path = require("path");

const [envArg, limitArg] = process.argv.slice(2);
const env = (envArg || "").toLowerCase();
const validEnvs = ["dev", "test", "prod"];

if (!validEnvs.includes(env)) {
  console.error("Usage: node index.js <dev|test|prod> <docLimit>");
  process.exit(1);
}

const docLimit = Number.parseInt(limitArg, 10);
if (!Number.isInteger(docLimit) || docLimit <= 0) {
  console.error("docLimit must be a positive integer.");
  process.exit(1);
}

const serviceAccountPath = path.join(
  __dirname,
  "..",
  "..",
  "secrets",
  `serviceAccountKey.${env}.json`
);
const serviceAccount = require(serviceAccountPath);

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
  const snapshot = await collRef.limit(docLimit).get();
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
  console.log(
    `✅ Schema (based on up to ${docLimit} docs per collection) extracted.`
  );
})();

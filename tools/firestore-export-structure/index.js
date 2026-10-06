const admin = require("firebase-admin");
const path = require("path");

function inferType(value) {
  if (Array.isArray(value)) return "array";
  if (value === null) return "null";
  if (value instanceof admin.firestore.Timestamp) return "timestamp";
  if (value instanceof admin.firestore.GeoPoint) return "geopoint";
  if (value instanceof admin.firestore.DocumentReference) return "reference";
  if (Buffer.isBuffer(value) || value instanceof Uint8Array) return "bytes";
  if (typeof value === "object") return "map";
  return typeof value;
}

// Describe types only, never field values. Merge identical array element shapes.
function describeValue(value) {
  const type = inferType(value);
  if (type === "map") {
    return { type, fields: Object.fromEntries(Object.keys(value).sort()
      .map(key => [key, describeValue(value[key])])) };
  }
  if (type === "array") {
    const shapes = [...new Set(value.map(item => JSON.stringify(describeValue(item))))];
    return { type, elements: shapes.sort().map(shape => JSON.parse(shape)) };
  }
  return { type };
}

function printValue(name, shape, indent, log) {
  log(`${indent}${name}: ${shape.type}`);
  if (shape.fields) {
    for (const [key, child] of Object.entries(shape.fields)) {
      printValue(key, child, indent + "  ", log);
    }
  }
  if (shape.elements) {
    for (const child of shape.elements) printValue("[]", child, indent + "  ", log);
  }
}

async function describeCollectionRecursive(collRef, docLimit, indent = "", log = console.log) {
  // Unlike query.get(), listDocuments includes missing parents with subcollections.
  const refs = (await collRef.listDocuments()).sort((a, b) =>
    a.path < b.path ? -1 : a.path > b.path ? 1 : 0);
  log(`${indent}Collection: ${collRef.path}`);
  if (!refs.length) {
    log(`${indent}  No documents found`);
    return;
  }
  const selected = refs.slice(0, docLimit);
  log(`${indent}  Sample: ${selected.length}/${refs.length} document references (including missing parents)`);
  for (const ref of selected) {
    const snapshot = await ref.get();
    log(`${indent}  Document: ${ref.id}${snapshot.exists ? "" : " [missing parent]"}`);
    if (snapshot.exists) {
      const shape = describeValue(snapshot.data());
      for (const [key, child] of Object.entries(shape.fields)) {
        printValue(key, child, indent + "    ", log);
      }
    }
    const subCollections = await ref.listCollections();
    subCollections.sort((a, b) => a.path < b.path ? -1 : a.path > b.path ? 1 : 0);
    for (const subColl of subCollections) {
      await describeCollectionRecursive(subColl, docLimit, indent + "    ", log);
    }
  }
}

async function main(args = process.argv.slice(2)) {
  const [envArg, limitArg] = args;
  const env = (envArg || "").toLowerCase();
  const docLimit = Number(limitArg);
  if (args.length !== 2 || !["dev", "test", "prod"].includes(env) ||
      !/^\d+$/.test(limitArg || "") || !Number.isSafeInteger(docLimit) || docLimit <= 0) {
    throw new Error("Usage: node index.js <dev|test|prod> <positive docLimit>");
  }
  const serviceAccount = require(path.join(__dirname, "..", "..", "secrets", `serviceAccountKey.${env}.json`));
  const app = admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  try {
    console.log(`Project: ${serviceAccount.project_id}; environment: ${env}; READ-ONLY`);
    const collections = await app.firestore().listCollections();
    collections.sort((a, b) => a.path < b.path ? -1 : a.path > b.path ? 1 : 0);
    for (const collection of collections) await describeCollectionRecursive(collection, docLimit);
    console.log(`Schema sample extracted: up to ${docLimit} document references per collection. Subcollections under unselected documents are not inspected. Empty/missing data cannot reveal its schema.`);
  } finally {
    await app.delete();
  }
}

if (require.main === module) {
  main().catch(error => {
    console.error(`Export failed: ${error.message}`);
    process.exitCode = 1;
  });
}

module.exports = { describeValue, describeCollectionRecursive, main };

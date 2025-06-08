const functions = require("firebase-functions");
const admin     = require("firebase-admin");

// pull in whatever little bit of config the CLI set, or start fresh
// then fill in databaseURL dynamically from the project ID
let firebaseConfig;
try {
  firebaseConfig = JSON.parse(process.env.FIREBASE_CONFIG);
} catch(e) {
  firebaseConfig = {};
}

if (!firebaseConfig.databaseURL) {
  // GCLOUD_PROJECT is set by Cloud Functions at runtime
  const pid = process.env.GCLOUD_PROJECT || process.env.FIREBASE_CONFIG && 
              JSON.parse(process.env.FIREBASE_CONFIG).projectId;
  firebaseConfig.databaseURL = `https://${pid}.firebaseio.com`;
}

// now initialize with the fully-populated config
admin.initializeApp(firebaseConfig);

// … your onWrite handler as before …
exports.syncStatusToFirestore = functions.database
  .ref("/status/{uid}")
  .onWrite(async (change, context) => { /* … */ });

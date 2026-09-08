const functions = require("firebase-functions/v1");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { FieldValue, getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { getDatabase } = require("firebase-admin/database");

initializeApp();

const auth = getAuth();
const db = getFirestore();
const messaging = getMessaging();
const rtdb = getDatabase();

const INACTIVITY_DAYS = 30;
const INACTIVITY_MS = INACTIVITY_DAYS * 24 * 60 * 60 * 1000;
const CONCURRENCY = 20;

function createSummary() {
  return {
    authUsersScanned: 0,
    inactiveCandidates: 0,
    skippedRecentlyActive: 0,
    skippedMissingFirestoreUser: 0,
    skippedDeletedAccount: 0,
    skippedMissingFcmTarget: 0,
    probesAcceptedByFcm: 0,
    notRegistered: 0,
    invalidRegistration: 0,
    targetChangedBeforeInvalidation: 0,
    presenceMarkedOffline: 0,
    presencePreservedAfterNewHeartbeat: 0,
    probeFailures: 0
  };
}

function getLastActivityMillis(user) {
  const timestamp = user.metadata.lastSignInTime || user.metadata.creationTime;
  return timestamp ? new Date(timestamp).getTime() : 0;
}

function classifyFcmError(errorCode) {
  if (errorCode === "messaging/registration-token-not-registered" ||
      errorCode === "messaging/installation-id-not-registered") {
    return "NotRegistered";
  }
  if (errorCode === "messaging/invalid-registration-token") {
    return "InvalidRegistration";
  }
  return null;
}

async function invalidateCurrentTarget(uid, targetField, targetValue, errorType, probeStartedAt) {
  const userRef = db.doc(`users/${uid}`);
  const invalidated = await db.runTransaction(async transaction => {
    const currentUser = await transaction.get(userRef);
    if (!currentUser.exists || currentUser.get(targetField) !== targetValue) {
      return false;
    }

    transaction.update(userRef, {
      [targetField]: FieldValue.delete(),
      fcmErrorType: errorType,
      fcmErrorDate: FieldValue.serverTimestamp()
    });
    return true;
  });

  if (!invalidated || errorType !== "NotRegistered") {
    return { invalidated, presenceCommitted: null };
  }

  // Do not overwrite presence refreshed after this probe began. This makes the
  // Firestore target invalidation and the subsequent presence update race-safe.
  const presenceResult = await rtdb.ref(`status/${uid}`).transaction(currentStatus => {
    const lastHeartbeat = Number(currentStatus?.last_heartbeat || 0);
    if (lastHeartbeat > probeStartedAt) {
      return;
    }
    return { ...(currentStatus || {}), state: "offline", last_heartbeat: 0 };
  });

  return { invalidated: true, presenceCommitted: presenceResult.committed };
}

async function probeUser(user, now, summary) {
  const lastActivityMillis = getLastActivityMillis(user);
  const inactivityMillis = now - lastActivityMillis;

  if (inactivityMillis < INACTIVITY_MS) {
    summary.skippedRecentlyActive++;
    return;
  }

  summary.inactiveCandidates++;
  const inactiveDays = Math.floor(inactivityMillis / (24 * 60 * 60 * 1000));
  const logContext = {
    uid: user.uid,
    inactiveDays,
    lastActivity: new Date(lastActivityMillis).toISOString()
  };

  console.log("[verifyInactiveFcmRegistrations] Inspecting inactive user", logContext);

  try {
    const userDoc = await db.doc(`users/${user.uid}`).get();
    if (!userDoc.exists) {
      summary.skippedMissingFirestoreUser++;
      console.log("[verifyInactiveFcmRegistrations] Skipping: Firestore user document is missing", logContext);
      return;
    }

    const userData = userDoc.data();
    const detailedContext = {
      ...logContext,
      nickname: userData.nickname || null,
      method: userData.method || null
    };

    if (userData.accountDeleted === true) {
      summary.skippedDeletedAccount++;
      console.log("[verifyInactiveFcmRegistrations] Skipping: account is marked deleted", detailedContext);
      return;
    }

    const installationId = userData.fcmInstallationId;
    const legacyToken = userData.fcmToken;
    if (!installationId && !legacyToken) {
      summary.skippedMissingFcmTarget++;
      console.log("[verifyInactiveFcmRegistrations] Skipping: no FCM target is stored", detailedContext);
      return;
    }

    const targetField = installationId ? "fcmInstallationId" : "fcmToken";
    const targetValue = installationId || legacyToken;
    const targetType = installationId ? "fid" : "legacy-token";
    const probeStartedAt = Date.now();

    console.log("[verifyInactiveFcmRegistrations] Sending silent registration probe", {
      ...detailedContext,
      targetType
    });

    try {
      const messageId = await messaging.send({
        ...(installationId ? { fid: installationId } : { token: legacyToken }),
        data: {
          // Existing Android clients explicitly ignore this data-only message type.
          type: "start",
          purpose: "fcm_registration_probe"
        },
        android: { priority: "normal" }
      });

      summary.probesAcceptedByFcm++;
      console.log("[verifyInactiveFcmRegistrations] Probe accepted by FCM", {
        ...detailedContext,
        targetType,
        messageId,
        note: "FCM acceptance does not guarantee delivery to the device"
      });
    } catch (error) {
      const errorType = classifyFcmError(error.code);
      console.error("[verifyInactiveFcmRegistrations] Probe rejected", {
        ...detailedContext,
        targetType,
        errorCode: error.code || null,
        errorMessage: error.message
      });

      if (!errorType) {
        summary.probeFailures++;
        return;
      }

      if (errorType === "NotRegistered") summary.notRegistered++;
      if (errorType === "InvalidRegistration") summary.invalidRegistration++;

      const result = await invalidateCurrentTarget(
        user.uid,
        targetField,
        targetValue,
        errorType,
        probeStartedAt
      );

      if (!result.invalidated) {
        summary.targetChangedBeforeInvalidation++;
        console.log("[verifyInactiveFcmRegistrations] FCM target changed during probe; current target preserved", {
          ...detailedContext,
          targetType,
          errorType
        });
        return;
      }

      if (result.presenceCommitted === true) summary.presenceMarkedOffline++;
      if (result.presenceCommitted === false) summary.presencePreservedAfterNewHeartbeat++;

      console.log("[verifyInactiveFcmRegistrations] Invalid FCM target recorded", {
        ...detailedContext,
        targetType,
        errorType,
        presenceMarkedOffline: result.presenceCommitted
      });
    }
  } catch (error) {
    summary.probeFailures++;
    console.error("[verifyInactiveFcmRegistrations] Failed to process inactive user", {
      ...logContext,
      errorCode: error.code || null,
      errorMessage: error.message,
      errorStack: error.stack
    });
  }
}

async function processWithConcurrency(items, worker) {
  let nextIndex = 0;
  const workerCount = Math.min(CONCURRENCY, items.length);
  await Promise.all(Array.from({ length: workerCount }, async () => {
    while (nextIndex < items.length) {
      const index = nextIndex++;
      await worker(items[index]);
    }
  }));
}

exports.verifyInactiveFcmRegistrations = functions
  .region("us-central1")
  .runWith({ timeoutSeconds: 540, memory: "256MB" })
  .pubsub
  .schedule("every 24 hours")
  .timeZone("Europe/Warsaw")
  .onRun(async () => {
    const startedAt = Date.now();
    const summary = createSummary();
    let nextPageToken;
    let pageNumber = 0;

    console.log("[verifyInactiveFcmRegistrations] Starting scheduled FCM registration verification", {
      inactivityThresholdDays: INACTIVITY_DAYS,
      concurrency: CONCURRENCY,
      startedAt: new Date(startedAt).toISOString()
    });

    try {
      do {
        pageNumber++;
        const page = await auth.listUsers(1000, nextPageToken);
        summary.authUsersScanned += page.users.length;
        console.log("[verifyInactiveFcmRegistrations] Processing Auth page", {
          pageNumber,
          usersOnPage: page.users.length,
          totalUsersScanned: summary.authUsersScanned
        });

        await processWithConcurrency(page.users, user => probeUser(user, startedAt, summary));
        nextPageToken = page.pageToken;

        console.log("[verifyInactiveFcmRegistrations] Auth page completed", {
          pageNumber,
          hasNextPage: Boolean(nextPageToken),
          summary: { ...summary }
        });
      } while (nextPageToken);
    } catch (error) {
      console.error("[verifyInactiveFcmRegistrations] Scheduled run failed", {
        errorCode: error.code || null,
        errorMessage: error.message,
        errorStack: error.stack,
        summary: { ...summary }
      });
      throw error;
    }

    console.log("[verifyInactiveFcmRegistrations] Scheduled run completed", {
      durationMs: Date.now() - startedAt,
      summary
    });
    return null;
  });

exports._test = {
  classifyFcmError,
  getLastActivityMillis
};

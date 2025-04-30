# CI/CD Notes

This directory documents issues and solutions encountered during the automation of Google Cloud CI/CD pipelines with Firebase.

---

## Firebase Auth `CONFIGURATION_NOT_FOUND` – Programmatic Fix

**Problem**: Attempts to enable Email/Password sign-in using the Identity Toolkit Admin API resulted in a `CONFIGURATION_NOT_FOUND` error.

**Root Cause**: Firebase Authentication must first be initialized for the project — this usually happens manually in the Firebase Console by clicking "Get Started", but is missing in CI/CD pipelines.

**Solution**: Automate initialization using one of two approaches:

- **Option A**: Add Firebase to the project using `firebase projects:addfirebase` or REST.
- **Option B**: Use the Identity Platform `initializeAuth` endpoint to directly create the auth configuration.

The attached guide includes:
- Required IAM permissions
- Required APIs
- Example `curl` and `gcloud` commands
- CI/CD tips and pitfalls
- How to enable email/password after initialization

📖 [Read the full solution (PDF)](programmatic-solution-to-firebase-auth-configuration-not-found.pdf)

---
## Firebase callable functions → `UNAUTHENTICATED`

**Problem**: `FirebaseFunctionsException: UNAUTHENTICATED` each time the Android app calls an `https.onCall` Cloud Function, even though the user is signed in and Firestore works.

**Root cause**: Organisation enforces **Domain-Restricted Sharing** (`iam.allowedPolicyMemberDomains`). That policy blocks any IAM binding that uses the public principal **`allUsers`**. Callable functions need the binding `roles/cloudfunctions.invoker  ➜ allUsers`; without it the gateway rejects the call before our code runs.

 **Solution**: 
 1. **Override** the constraint in this project only:  ```yaml  constraint: constraints/iam.allowedPolicyMemberDomains  listPolicy:    allValues: ALLOW  ```  ```bash  gcloud beta resource-manager org-policies set-policy /tmp/allow-any-member.yaml --project "$PROJECT_ID"  ```
 2. **Grant** the Invoker role:  ```bash  firebase deploy --only functions:acceptInvite --allow-unauthenticated  # or (manual)  gcloud functions add-iam-policy-binding acceptInvite \     --region us-central1 \     --member allUsers \     --role roles/cloudfunctions.invoker  ```
 3. Re-install / clear-storage on the app and retest – the function now runs and returns `matchId`.
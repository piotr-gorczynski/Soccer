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

# CI/CD Notes

This directory documents issues and solutions encountered during the automation of Google Cloud CI/CD pipelines with Firebase.

---

## 1. Firebase Authentication Initialization (Summary Report)

**Problem**: Attempts to enable Email/Password sign-in via API resulted in a `CONFIGURATION_NOT_FOUND` error.

**Root Cause**: Firebase Authentication must first be initialized by a manual "Get Started" action in the Firebase Console.

**Solution**: Automate this initialization using Terraform's `google_identity_platform_project_default_config`.

📖 [Read the summary report (PDF)](firebase-auth-initialization-issue.pdf)

---

## 2. Firebase Authentication Initialization (Detailed Implementation Guide)

**Overview**: This document explores **two programmatic options** to initialize Firebase Authentication and avoid the `CONFIGURATION_NOT_FOUND` issue in CI/CD pipelines:
- Add Firebase to the GCP project using the **Firebase Management API**
- Use **Identity Platform API** to initialize auth via `initializeAuth`

Includes step-by-step CLI commands, required IAM permissions, API enablements, and Terraform setup.

📖 [Read the full implementation guide (PDF)](programmatic-initialization-of-firebase-authentication-in-gcp.pdf)

---

## 3. Cloud Build IAM Permission Error

**Problem**: Cloud Build failed to enable the Identity Platform API due to a `PERMISSION_DENIED` error, even with broad roles like `owner`.

**Root Cause**: Missing or blocked `servicemanagement.services.bind` permission due to higher-level organizational policies or IAM scope issues.

**Solution**: Investigate org policies and ensure `roles/servicemanagement.admin` and `roles/serviceusage.serviceUsageAdmin` are properly granted to Cloud Build SA.

📄 [Read the full PDF analysis](cloud-build-iam-permission-error.pdf)

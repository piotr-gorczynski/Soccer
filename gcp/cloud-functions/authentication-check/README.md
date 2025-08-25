# Firebase Authentication Cloud Function

This directory contains the Firebase Authentication Cloud Function that provides user creation and token verification services.

## Overview

The Cloud Function exposes two main endpoints:
- `create_user`: Creates a new Firebase user with email and password
- `verify_token`: Verifies Firebase Auth ID tokens

## Recent Improvements (Fix for "Failed to create Firebase user")

### Enhanced Error Handling

The function now provides detailed error diagnostics instead of generic "Failed to create user" messages:

#### Before
```json
{"error": "Failed to create user: Your default credentials were not found"}
```

#### After
```json
{
  "error": "Authentication credentials not found",
  "details": "Firebase Admin SDK cannot find valid service account credentials",
  "suggestion": "Ensure the Cloud Function has proper service account permissions"
}
```

### Error Categories

The function now handles specific error scenarios:

1. **Configuration Issues (HTTP 500)**
   - Firebase Admin SDK not initialized
   - Missing authentication credentials
   - Firebase Authentication not configured for the project

2. **Client Errors (HTTP 400)**
   - Missing JSON body
   - Missing email or password
   - Invalid email format
   - Weak password

3. **Conflict Errors (HTTP 409)**
   - Email already exists

### Common Error Solutions

#### "Authentication credentials not found"
- **Cause**: Cloud Function lacks proper service account credentials
- **Solution**: Ensure the Cloud Function has proper service account permissions
- **Local testing**: Set `GOOGLE_APPLICATION_CREDENTIALS` or run `gcloud auth application-default login`

#### "Firebase Authentication not configured"
- **Cause**: Firebase Authentication is not initialized for the project
- **Solution**: Run the initialization script:
  ```bash
  gcloud builds submit --config gcp/cloud-build/deploy_firebase_with_auth.yaml
  ```
- **Manual**: Go to Firebase Console > Authentication > Get Started

## Files

- `main.py` - Main Cloud Function code with enhanced error handling
- `requirements.txt` - Python dependencies
- `test_auth_errors.py` - Comprehensive unit tests
- `demo_auth_errors.py` - Demo script showing error responses
- `firebase_auth_diagnostic.py` - Diagnostic tool for troubleshooting

## Testing

Run the test suite:
```bash
cd gcp/cloud-functions/authentication-check
pip install -r requirements.txt
python test_auth_errors.py
```

Run the diagnostic tool:
```bash
python firebase_auth_diagnostic.py
```

See error examples:
```bash
python demo_auth_errors.py
```

## Deployment

This function is deployed as part of the CI/CD pipeline. The enhanced error handling helps diagnose configuration issues during deployment.

## Related Documentation

- [CI/CD Firebase Auth Setup](../../../docs/ci-cd/README.md)
- [Firebase Auth Configuration Script](../../cloud-build/deploy_firebase_with_auth.yaml)
- [Authentication Test Script](../../cloud-build/test_firebase_auth.yaml)
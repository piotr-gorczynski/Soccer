# Running Unit Tests Without Real Secrets

## Summary

The Android app requires several secrets for building, including `google-services.json`. Unit tests can run without actual production secrets by using mock/test versions of the required configuration files.

## Required Files

To run unit tests locally without real secrets, you need to create mock configuration files in the `secrets/` directory:

### 1. google-services.json files (Required)

Create `secrets/google-services.{env}.json` files for each environment (dev, test, prod):

**File: `secrets/google-services.test.json`** (minimal valid structure)
```json
{
  "project_info": {
    "project_number": "123456789",
    "firebase_url": "https://soccer-test.firebaseio.com",
    "project_id": "soccer-test",
    "storage_bucket": "soccer-test.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:123456789:android:abcdef123456",
        "android_client_info": {
          "package_name": "piotr_gorczynski.soccer2"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "AIzaSyTest123456789abcdefghijklmnopqrst"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    },
    {
      "client_info": {
        "mobilesdk_app_id": "1:123456789:android:abcdef123456",
        "android_client_info": {
          "package_name": "piotr_gorczynski.soccer2.bd"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "AIzaSyTest123456789abcdefghijklmnopqrst"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
```

### 2. Backend Secret Key (Already present)
- `secrets/soccer_secret_key` - Already in repository

### 3. Facebook Client Token (Already present)
- `secrets/facebook_client_token` - Already in repository

## Gradle Commands to Run Unit Tests

All commands should be run from the `mobile/` directory.

### Run All Unit Tests (All Variants)
```bash
cd mobile
./gradlew test -Penv=test
```

### Run Unit Tests for Specific Environment
```bash
# Test environment
./gradlew test -Penv=test

# Dev environment  
./gradlew test -Penv=dev

# Prod environment
./gradlew test -Penv=prod
```

### Run Unit Tests for Specific Build Variant
The app uses product flavors:
- **Environment**: `_dev`, `_test`, `_prod`
- **Market**: `Global`, `Bangladesh`
- **Build Type**: `Debug`, `Release`

Examples:
```bash
# Test - Global - Debug
./gradlew test_testGlobalDebugUnitTest -Penv=test

# Dev - Global - Debug  
./gradlew test_devGlobalDebugUnitTest -Penv=dev

# Test - Bangladesh - Debug
./gradlew test_testBangladeshDebugUnitTest -Penv=test

# Prod - Global - Debug
./gradlew test_prodGlobalDebugUnitTest -Penv=prod
```

### Run Tests in Quiet Mode (Less Output)
```bash
./gradlew test -Penv=test --quiet
```

## How Tests Work Without Real Secrets

1. **google-services.json**: The build.gradle file has a custom `copyGoogleServicesJson` task that:
   - Expects `secrets/google-services.{env}.json` to exist
   - Copies it to `app/google-services.json`
   - The Google Services Gradle plugin processes this file
   - Unit tests don't actually connect to Firebase, so a mock/test version works fine

2. **Soccer Secret Key**: Already provided in `secrets/soccer_secret_key`
   - Copied to app assets during build
   - Unit tests use the provided test version

3. **Facebook Client Token**: Already provided in `secrets/facebook_client_token`
   - Copied to app assets during build or falls back to strings.xml

## CI/CD Integration

### Current CI/CD Workflows
The repository has these CI/CD workflows in `.github/workflows/`:
- `npm-audit.yml` - Runs npm security audits (JavaScript/Node.js projects)
- `check-translations.yml` - Validates translation completeness

**Note**: There are currently NO Android test workflows configured. To add Android unit tests to CI:

1. Create `.github/workflows/android-unit-tests.yml`
2. Use the gradle command: `./gradlew test -Penv=test`
3. Ensure `google-services.test.json` is included in the repository or created during CI

## Test Configuration in build.gradle

Key test settings from `mobile/app/build.gradle`:

```gradle
testOptions {
    unitTests.returnDefaultValues = true
}

// Test dependencies
testImplementation 'junit:junit:4.13.2'
testImplementation 'androidx.test:core:1.7.0'
testImplementation 'androidx.test.ext:junit:1.3.0'
testImplementation 'org.mockito:mockito-core:5.20.0'
testImplementation 'org.robolectric:robolectric:4.16'
```

The `unitTests.returnDefaultValues = true` setting allows unit tests to run without Android framework fully initialized (Robolectric handles this).

## Test Results

When tests run, Gradle generates an HTML report at:
```
mobile/app/build/reports/tests/{variant_name}/index.html
```

Example: `mobile/app/build/reports/tests/test_testGlobalDebugUnitTest/index.html`

## Troubleshooting

### Error: "google-services.json not found in secrets/"
- Create the missing `secrets/google-services.{env}.json` file
- The file must have valid JSON structure with `project_info` and `client` objects

### Error: "Missing project_info object"
- Ensure the google-services.json has the required structure shown above
- The file must include both package names: `piotr_gorczynski.soccer2` and `piotr_gorczynski.soccer2.bd`

### Tests compile but don't run
- Check that test files exist in `mobile/app/src/test/java/`
- Verify Robolectric is properly configured
- Check test output for specific errors

## Recommended Local Setup

```bash
# 1. Clone the repository
git clone <repo-url>
cd Soccer

# 2. Create test configuration files in secrets/
# Copy the google-services.json template files from this guide

# 3. Run unit tests
cd mobile
./gradlew test -Penv=test
```


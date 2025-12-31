# Bangladesh Product Flavor Configuration

This document describes the product flavor configuration for the Bangladesh variant of the Soccer app.

## Overview

The app now uses two flavor dimensions:
1. **environment**: `_dev`, `_test`, `_prod` (existing)
2. **market**: `global`, `bangladesh` (new)

This creates combined build variants like:
- `_devGlobalDebug`, `_devGlobalRelease`
- `_devBangladeshDebug`, `_devBangladeshRelease`
- `_testGlobalDebug`, `_testGlobalRelease`
- `_testBangladeshDebug`, `_testBangladeshRelease`
- `_prodGlobalDebug`, `_prodGlobalRelease`
- `_prodBangladeshDebug`, `_prodBangladeshRelease`

## Application IDs

- **Global version**: `piotr_gorczynski.soccer2`
- **Bangladesh version**: `piotr_gorczynski.soccer2.bd`

The Bangladesh variant uses `applicationIdSuffix ".bd"` which creates a separate package name, allowing both apps to coexist on the same device and have separate Google Play Store listings.

## Building the App

### Build Global Variant
```bash
# Debug
./gradlew assemble_prodGlobalDebug

# Release
./gradlew assemble_prodGlobalRelease
```

### Build Bangladesh Variant
```bash
# Debug
./gradlew assemble_prodBangladeshDebug

# Release
./gradlew assemble_prodBangladeshRelease
```

### Build All Variants
```bash
./gradlew assemble
```

## Firebase Configuration

The app uses a single `google-services.json` file per environment that contains configurations for all registered Android apps in the Firebase project.

**File locations in `secrets/` directory:**
- `secrets/google-services.dev.json` (contains both global and Bangladesh configs)
- `secrets/google-services.test.json` (contains both global and Bangladesh configs)
- `secrets/google-services.prod.json` (contains both global and Bangladesh configs)

The build system automatically copies the appropriate file based on the environment being built.

At runtime, Firebase automatically selects the correct client configuration based on the application's package name.

### Setting Up Firebase

1. **Register Both Apps in Firebase Console**:
   - Go to Firebase Console → Project Settings → Your apps
   - Ensure both apps are registered:
     - Package name: `piotr_gorczynski.soccer2` (Global)
     - Package name: `piotr_gorczynski.soccer2.bd` (Bangladesh)

2. **Download google-services.json**:
   - Download the `google-services.json` file from Firebase Console
   - This single file contains client configurations for both package names
   - Place it in `secrets/google-services.{env}.json` (e.g., `secrets/google-services.prod.json`)

3. **Add SHA-1 Certificate Fingerprints**:
   - Add your release keystore SHA-1 fingerprint to both apps in Firebase Console
   - This enables Google Sign-In for both variants

## Source Code Organization

```
mobile/app/src/
├── main/                    # Shared code for all variants
├── global/                  # Global variant specific code and resources (optional)
└── bangladesh/              # Bangladesh variant specific code and resources (optional)
```

Flavor-specific source sets can include:
- Java/Kotlin code in `java/` or `kotlin/` subdirectories
- Resources in `res/` subdirectory
- AndroidManifest.xml (merged with main manifest)
- Assets in `assets/` subdirectory

## Key Features of Bangladesh Variant

As per `docs/BANGLADESH_VERSION_APPROACH.md`, the Bangladesh variant includes:

- Package name: `piotr_gorczynski.soccer2.bd`
- Version name suffix: `-BD`
- Separate Google Play Store listing
- 18+ age rating
- Cash prize tournament features
- Bangladesh-specific compliance requirements

## Testing

To test that the variants are correctly configured:

```bash
# List all build variants
./gradlew tasks --all | grep assemble

# Verify Bangladesh variant exists
./gradlew tasks | grep Bangladesh

# Build specific variant to verify configuration
./gradlew assemble_prodBangladeshDebug
```

## Next Steps

1. Register Bangladesh app in Firebase Console (if not already done)
2. Ensure both package names are registered in the same Firebase project
3. Download the `google-services.json` file and place in `secrets/` directory
4. Add Bangladesh-specific features as per the implementation roadmap
5. Configure Facebook authentication for Bangladesh package (see `docs/BANGLADESH_VERSION_APPROACH.md`)
6. Set up Google Play Store listing for Bangladesh variant

## References

- [BANGLADESH_VERSION_APPROACH.md](../../docs/BANGLADESH_VERSION_APPROACH.md) - Complete approach and implementation details
- [Android Product Flavors Documentation](https://developer.android.com/build/build-variants)
- [Firebase Multi-App Configuration](https://firebase.google.com/docs/projects/multiprojects)

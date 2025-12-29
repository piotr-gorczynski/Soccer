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

The app uses flavor-specific `google-services.json` files:

- **Global**: `mobile/app/src/global/google-services.json`
- **Bangladesh**: `mobile/app/src/bangladesh/google-services.json`

The Google Services Gradle plugin automatically selects the correct file based on the build variant.

### Setting Up Firebase

1. **For Global Variant** (if not already done):
   - Go to Firebase Console
   - Ensure the app is registered with package name: `piotr_gorczynski.soccer2`
   - Download `google-services.json`
   - Place it in `mobile/app/src/global/google-services.json`

2. **For Bangladesh Variant** (NEW):
   - Go to Firebase Console → Project Settings → Your apps → Add app → Android
   - Package name: `piotr_gorczynski.soccer2.bd`
   - App nickname: Gridline Soccer Bangladesh
   - SHA-1 certificate fingerprint: [Your release keystore SHA-1]
   - Download `google-services.json`
   - Place it in `mobile/app/src/bangladesh/google-services.json`

## Source Code Organization

```
mobile/app/src/
├── main/                    # Shared code for all variants
├── global/                  # Global variant specific code and resources
│   └── google-services.json
└── bangladesh/              # Bangladesh variant specific code and resources
    └── google-services.json
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

1. Register Bangladesh app in Firebase Console
2. Download and configure `google-services.json` for Bangladesh variant
3. Add Bangladesh-specific features as per the implementation roadmap
4. Configure Facebook authentication for Bangladesh package (see `docs/BANGLADESH_VERSION_APPROACH.md`)
5. Set up Google Play Store listing for Bangladesh variant

## References

- [BANGLADESH_VERSION_APPROACH.md](../../docs/BANGLADESH_VERSION_APPROACH.md) - Complete approach and implementation details
- [Android Product Flavors Documentation](https://developer.android.com/build/build-variants)
- [Firebase Multi-App Configuration](https://firebase.google.com/docs/projects/multiprojects)

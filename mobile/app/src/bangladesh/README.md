# Bangladesh Product Flavor

This directory contains Bangladesh-specific resources and configuration for the `piotr_gorczynski.soccer2.bd` variant.

## Purpose

The Bangladesh variant is a separate APK designed to support cash prize tournaments in Bangladesh while maintaining the ability to coexist with the global app on the same device.

## Package Name

- **Global app**: `piotr_gorczynski.soccer2`
- **Bangladesh app**: `piotr_gorczynski.soccer2.bd`

## Facebook Content Provider Conflict Fix

### Problem

When both the global app and Bangladesh app are installed on the same device, Android throws an `INSTALL_FAILED_CONFLICTING_PROVIDER` error:

```
Can't install because provider name com.facebook.app.FacebookContentProvider1232966491486195 
(in package piotr_gorczynski.soccer2.bd) is already used by piotr_gorczynski.soccer2
```

### Root Cause

Both apps were using the same Facebook Content Provider authority: `com.facebook.app.FacebookContentProvider1232966491486195`

Android requires that content provider authorities be unique across all installed apps. When two apps (even from the same developer) declare the same authority, installation fails.

### Solution

The `AndroidManifest.xml` in this directory overrides the Facebook Content Provider authority for the Bangladesh variant:

- **Global app authority**: `com.facebook.app.FacebookContentProvider1232966491486195`
- **Bangladesh app authority**: `com.facebook.app.FacebookContentProvider1232966491486195.bd`

This is implemented using Android's manifest merger with the `tools:replace` attribute:

```xml
<provider
    android:name="com.facebook.FacebookContentProvider"
    android:authorities="com.facebook.app.FacebookContentProvider1232966491486195.bd"
    android:exported="true"
    tools:replace="android:authorities" />
```

### Verification

To verify the fix works:

1. Build the Bangladesh debug variant:
   ```bash
   cd mobile
   ./gradlew assemble_devBangladeshDebug -Penv=dev
   ```

2. Check the merged manifest in the build output:
   ```bash
   cat app/build/intermediates/merged_manifests/_devBangladeshDebug/AndroidManifest.xml | grep FacebookContentProvider
   ```

3. You should see the `.bd` suffix in the authority for the Bangladesh variant.

### Important Notes

- **No Facebook configuration changes needed**: The same Facebook App ID (1232966491486195) is used for both variants
- **Same authentication**: Both apps share the same Firebase project and authentication
- **User data preserved**: Users can install both apps and their data is synchronized
- **Facebook permissions**: Add the Bangladesh package ID (`piotr_gorczynski.soccer2.bd`) to the Facebook App Dashboard to enable Facebook Login

## Build Variants

The Bangladesh flavor combines with environment flavors to create these variants:

- `_devBangladeshDebug`
- `_devBangladeshRelease`
- `_testBangladeshDebug`
- `_testBangladeshRelease`
- `_prodBangladeshDebug`
- `_prodBangladeshRelease`

## Further Reading

- [BANGLADESH_VERSION_APPROACH.md](../../../docs/BANGLADESH_VERSION_APPROACH.md) - Complete implementation guide
- [PRODUCT_FLAVOR_README.md](../PRODUCT_FLAVOR_README.md) - Product flavor setup documentation

## Changelog

- **2026-01-10**: Created Bangladesh AndroidManifest.xml to fix Facebook Content Provider conflict

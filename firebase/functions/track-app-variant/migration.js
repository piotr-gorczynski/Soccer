const VARIANT_PATTERN = /^[a-z][a-z0-9_-]{1,31}$/;

function buildVariantTrackingUpdate(existingData, appVariant, now) {
  if (typeof appVariant !== "string" || !VARIANT_PATTERN.test(appVariant)) {
    throw new Error("Invalid app variant.");
  }

  const existingVariants = existingData?.appVariants || {};
  const existingVariant = existingVariants[appVariant] || {};
  const appVariants = {
    ...existingVariants,
    [appVariant]: {
      firstSeenAt: existingVariant.firstSeenAt || now,
      lastSeenAt: now
    }
  };

  const update = {
    appVariant,
    appVariants
  };

  if (appVariant !== "global" && existingVariants.global) {
    const existingMigration = existingData?.migrationStatus || {};
    update.migrationStatus = {
      migratedFromGlobal: true,
      sourceVariant: "global",
      targetVariant: appVariant,
      firstDetectedAt: existingMigration.firstDetectedAt || now,
      lastDetectedAt: now
    };
  }

  return update;
}

module.exports = { buildVariantTrackingUpdate };

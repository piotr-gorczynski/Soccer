const assert = require("node:assert/strict");
const test = require("node:test");
const { buildVariantTrackingUpdate } = require("./migration");

test("records the first and latest sighting of a variant", () => {
  const now = { seconds: 1 };
  assert.deepEqual(buildVariantTrackingUpdate({}, "global", now), {
    appVariant: "global",
    appVariants: {
      global: { firstSeenAt: now, lastSeenAt: now }
    }
  });
});

test("preserves firstSeenAt on subsequent sightings", () => {
  const first = { seconds: 1 };
  const now = { seconds: 2 };
  const result = buildVariantTrackingUpdate({
    appVariants: { global: { firstSeenAt: first, lastSeenAt: first } }
  }, "global", now);

  assert.deepEqual(result.appVariants.global, { firstSeenAt: first, lastSeenAt: now });
});

test("detects migration from global to a market variant", () => {
  const first = { seconds: 1 };
  const now = { seconds: 2 };
  const result = buildVariantTrackingUpdate({
    appVariants: { global: { firstSeenAt: first, lastSeenAt: first } }
  }, "bangladesh", now);

  assert.equal(result.migrationStatus.migratedFromGlobal, true);
  assert.equal(result.migrationStatus.sourceVariant, "global");
  assert.equal(result.migrationStatus.targetVariant, "bangladesh");
  assert.equal(result.migrationStatus.firstDetectedAt, now);
});

test("does not infer migration for a new market-only user", () => {
  const result = buildVariantTrackingUpdate({}, "bangladesh", { seconds: 1 });
  assert.equal(result.migrationStatus, undefined);
});

test("rejects invalid variant names", () => {
  assert.throws(() => buildVariantTrackingUpdate({}, "BD", {}), /Invalid app variant/);
  assert.throws(() => buildVariantTrackingUpdate({}, "", {}), /Invalid app variant/);
});

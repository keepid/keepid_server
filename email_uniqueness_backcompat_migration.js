/*
 * Backward-compatible email uniqueness migration for MongoDB.
 *
 * What it does (per target DB):
 * 1) Normalizes non-empty emails to trim().toLowerCase()
 * 2) Converts null/missing email fields to empty string ("") for legacy compatibility
 * 3) Ensures there are no duplicate non-empty emails
 * 4) Recreates unique partial index on email that ignores empty strings
 *
 * Default target DBs: ["prod-db"]
 * You can override by setting TARGET_DBS before loading this script:
 *   mongosh "$MONGO_URI" --eval 'const TARGET_DBS=["test-db"];' email_uniqueness_backcompat_migration.js
 *
 * Using your existing env file via env-cmd:
 *   npx env-cmd -f .env -- mongosh "$MONGO_URI" email_uniqueness_backcompat_migration.js
 *   npx env-cmd -f .env -- mongosh "$MONGO_URI" --eval 'const TARGET_DBS=["test-db"];' email_uniqueness_backcompat_migration.js
 */

const targetDbs =
  typeof TARGET_DBS !== "undefined" && Array.isArray(TARGET_DBS)
    ? TARGET_DBS
    : ["prod-db"];

function collectStats(coll, label) {
  return {
    label,
    totalUsers: coll.countDocuments(),
    emailMissing: coll.countDocuments({ email: { $exists: false } }),
    emailNull: coll.countDocuments({ email: null }),
    emailEmptyString: coll.countDocuments({ email: "" }),
    emailWhitespaceOnly: coll.countDocuments({
      email: { $type: "string", $regex: /^\s+$/ },
    }),
  };
}

function duplicateNonEmptyGroups(coll) {
  return coll
    .aggregate([
      { $match: { email: { $type: "string", $ne: "" } } },
      { $group: { _id: "$email", count: { $sum: 1 }, usernames: { $push: "$username" } } },
      { $match: { count: { $gt: 1 } } },
      { $sort: { count: -1, _id: 1 } },
    ])
    .toArray();
}

const results = [];

for (const dbName of targetDbs) {
  const coll = db.getSiblingDB(dbName).getCollection("user");
  const before = collectStats(coll, "before");

  // Drop existing migration index so updates can run safely.
  try {
    coll.dropIndex("email_unique_non_null");
  } catch (e) {
    // ignore if missing
  }

  // Normalize non-empty emails.
  const normalizeResult = coll.updateMany(
    { email: { $type: "string", $ne: "" } },
    [
      {
        $set: {
          email: {
            $toLower: {
              $trim: { input: "$email" },
            },
          },
        },
      },
    ]
  );

  // Legacy-compatible representation for "no email".
  const nullToEmptyResult = coll.updateMany({ email: null }, { $set: { email: "" } });
  const missingToEmptyResult = coll.updateMany(
    { email: { $exists: false } },
    { $set: { email: "" } }
  );

  const dupes = duplicateNonEmptyGroups(coll);
  if (dupes.length > 0) {
    results.push({
      db: dbName,
      migrated: false,
      reason: "Duplicate non-empty emails exist; index not recreated.",
      before,
      normalizeModifiedCount: normalizeResult.modifiedCount,
      nullToEmptyModifiedCount: nullToEmptyResult.modifiedCount,
      missingToEmptyModifiedCount: missingToEmptyResult.modifiedCount,
      duplicateGroups: dupes.slice(0, 20),
      after: collectStats(coll, "after_without_index"),
      indexes: coll
        .getIndexes()
        .map((i) => ({ name: i.name, key: i.key, unique: !!i.unique, partial: i.partialFilterExpression || null })),
    });
    continue;
  }

  // NOTE: $ne is not supported in partial index expressions on some clusters.
  // Use $gt: "" to exclude empty strings while indexing non-empty strings.
  coll.createIndex(
    { email: 1 },
    {
      name: "email_unique_non_null",
      unique: true,
      partialFilterExpression: { email: { $type: "string", $gt: "" } },
    }
  );

  results.push({
    db: dbName,
    migrated: true,
    before,
    normalizeModifiedCount: normalizeResult.modifiedCount,
    nullToEmptyModifiedCount: nullToEmptyResult.modifiedCount,
    missingToEmptyModifiedCount: missingToEmptyResult.modifiedCount,
    after: collectStats(coll, "after"),
    indexes: coll
      .getIndexes()
      .map((i) => ({ name: i.name, key: i.key, unique: !!i.unique, partial: i.partialFilterExpression || null })),
  });
}

printjson(results);

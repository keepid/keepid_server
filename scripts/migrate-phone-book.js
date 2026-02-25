// All-in-one phone book migration:
//   1. Backfills phoneBook from root phone field
//   2. Strips stale isPrimary / primary fields from phoneBook entries
//   3. Removes root phone field
//
// Usage: npx env-cmd mongosh "$MONGO_URI" --eval 'const targetDb="test-db"' scripts/migrate-phone-book.js
//    or: npx env-cmd mongosh "$MONGO_URI" --eval 'const targetDb="prod-db"' scripts/migrate-phone-book.js

const ALLOWED_DBS = ["test-db", "prod-db"];

if (typeof targetDb === "undefined" || !ALLOWED_DBS.includes(targetDb)) {
  print(`Refused. Pass --eval 'const targetDb="test-db"' or 'const targetDb="prod-db"'`);
  quit(1);
}

const database = db.getSiblingDB(targetDb);
const users = database.getCollection("user");

const totalUsers = users.countDocuments();
print(`Database: ${targetDb} (${totalUsers} users)`);

// --- Step 1: backfill phoneBook from root phone ---
const cursor = users.find({
  $or: [{ phoneBook: { $exists: false } }, { phoneBook: null }, { phoneBook: { $size: 0 } }],
  phone: { $exists: true, $nin: [null, ""] },
});

let migrated = 0;
let skipped = 0;

while (cursor.hasNext()) {
  const user = cursor.next();
  let digits = (user.phone || "").replace(/[^0-9]/g, "");
  if (digits.length === 11 && digits.startsWith("1")) {
    digits = digits.substring(1);
  }
  if (digits.length !== 10) {
    print(`  Skipping ${user.username}: phone "${user.phone}" does not normalize to 10 digits`);
    skipped++;
    continue;
  }

  users.updateOne(
    { _id: user._id },
    { $set: { phoneBook: [{ label: "primary", phoneNumber: digits }] } }
  );
  migrated++;
}
print(`Step 1 - Backfill phoneBook: ${migrated} migrated, ${skipped} skipped`);

// --- Step 2: strip stale isPrimary and primary fields from phoneBook entries ---
const r1 = users.updateMany(
  { "phoneBook.isPrimary": { $exists: true } },
  { $unset: { "phoneBook.$[].isPrimary": "" } }
);
const r2 = users.updateMany(
  { "phoneBook.primary": { $exists: true } },
  { $unset: { "phoneBook.$[].primary": "" } }
);
print(`Step 2 - Strip stale fields: isPrimary (${r1.modifiedCount}), primary (${r2.modifiedCount})`);

// --- Step 3: remove root phone field ---
const r3 = users.updateMany(
  { phone: { $exists: true } },
  { $unset: { phone: "" } }
);
print(`Step 3 - Remove root phone field: ${r3.modifiedCount} user(s)`);

print("\nDone.");

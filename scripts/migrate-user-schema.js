/**
 * MongoDB migration script: User schema simplification
 *
 * Transforms existing user documents from the old schema to the new one:
 *   1. Maps firstName/lastName -> currentName nested doc
 *   2. Maps address/city/state/zipcode -> personalAddress nested doc
 *   3. Maps optionalInformation.basicInfo.mailingAddress -> mailAddress
 *   4. Maps phone -> phoneBook array (if not already migrated)
 *   5. Maps optionalInformation.basicInfo.genderAssignedAtBirth -> sex
 *   6. Drops optionalInformation entirely
 *   7. $unset all removed fields
 *
 * Run with:   mongosh <connection-string> scripts/migrate-user-schema.js
 * Databases:  prod-db, test-db
 */

const dbName = db.getName();
const ALLOWED_DBS = ["staging-db", "prod-db", "test-db"];

if (!ALLOWED_DBS.includes(dbName)) {
  print(`ERROR: connected to "${dbName}" — only ${ALLOWED_DBS.join(", ")} are allowed.`);
  quit(1);
}

print(`\n=== User Schema Migration on "${dbName}" ===\n`);

const users = db.getCollection("user");
const total = users.countDocuments();
print(`Total user documents: ${total}`);

let migrated = 0;
let skipped = 0;
let errors = 0;

users.find().forEach((doc) => {
  try {
    const setFields = {};
    const unsetFields = {};

    // 1. Map firstName/lastName -> currentName
    if (doc.firstName || doc.lastName) {
      const currentName = {
        first: doc.firstName || null,
        middle: null,
        last: doc.lastName || null,
        suffix: null,
        maiden: null,
      };

      // Pull middle name from optionalInformation.person if available
      if (doc.optionalInformation?.person?.middleName) {
        currentName.middle = doc.optionalInformation.person.middleName;
      }

      // Only set if currentName doesn't already exist
      if (!doc.currentName) {
        setFields.currentName = currentName;
      }
    }

    // 2. Map address/city/state/zipcode -> personalAddress
    if ((doc.address || doc.city || doc.state || doc.zipcode) && !doc.personalAddress) {
      setFields.personalAddress = {
        line1: doc.address || null,
        line2: null,
        city: doc.city || null,
        state: doc.state || null,
        zip: doc.zipcode || null,
        county: null,
      };
    }

    // 3. Map optionalInformation.basicInfo.mailingAddress -> mailAddress
    const mailingAddr = doc.optionalInformation?.basicInfo?.mailingAddress;
    if (mailingAddr && !doc.mailAddress) {
      setFields.mailAddress = {
        line1: mailingAddr.streetAddress || null,
        line2: mailingAddr.apartmentNumber || null,
        city: mailingAddr.city || null,
        state: mailingAddr.state || null,
        zip: mailingAddr.zip || null,
        county: null,
      };
    }

    // 4. Map phone -> phoneBook (if phoneBook doesn't exist or is empty)
    if (doc.phone && (!doc.phoneBook || doc.phoneBook.length === 0)) {
      setFields.phoneBook = [{ label: "primary", phoneNumber: doc.phone }];
    }

    // 5. Map genderAssignedAtBirth -> sex
    const gender = doc.optionalInformation?.basicInfo?.genderAssignedAtBirth;
    if (gender && !doc.sex) {
      setFields.sex = gender;
    }

    // 6. Map mother/father names if available from optionalInformation
    // (familyInfo.parents array might have parent entries)
    // This is a best-effort extraction

    // 7. Fields to remove
    const fieldsToRemove = [
      "firstName",
      "lastName",
      "address",
      "city",
      "state",
      "zipcode",
      "phone",
      "optionalInformation",
    ];
    for (const field of fieldsToRemove) {
      if (doc[field] !== undefined) {
        unsetFields[field] = "";
      }
    }

    // Apply changes
    const hasSet = Object.keys(setFields).length > 0;
    const hasUnset = Object.keys(unsetFields).length > 0;

    if (hasSet || hasUnset) {
      const update = {};
      if (hasSet) update.$set = setFields;
      if (hasUnset) update.$unset = unsetFields;

      users.updateOne({ _id: doc._id }, update);
      migrated++;
    } else {
      skipped++;
    }
  } catch (e) {
    print(`ERROR on doc ${doc._id}: ${e.message}`);
    errors++;
  }
});

print(`\nDone. Migrated: ${migrated}, Skipped: ${skipped}, Errors: ${errors}`);

// Verification
print("\n=== Verification ===");
const withOldFields = users.countDocuments({
  $or: [
    { firstName: { $exists: true } },
    { lastName: { $exists: true } },
    { address: { $exists: true } },
    { optionalInformation: { $exists: true } },
  ],
});
print(`Documents still with old fields: ${withOldFields}`);

const withNewFields = users.countDocuments({ currentName: { $exists: true } });
print(`Documents with currentName: ${withNewFields}`);

const withPersonalAddr = users.countDocuments({
  personalAddress: { $exists: true },
});
print(`Documents with personalAddress: ${withPersonalAddr}`);

const withPhoneBook = users.countDocuments({
  phoneBook: { $exists: true, $ne: [] },
});
print(`Documents with non-empty phoneBook: ${withPhoneBook}`);

// ========================================================================
// Organization collection: migrate flat address fields -> nested address doc
// ========================================================================
print(`\n=== Organization Address Migration on "${dbName}" ===\n`);

const orgs = db.getCollection("organization");
const orgTotal = orgs.countDocuments();
print(`Total organization documents: ${orgTotal}`);

let orgMigrated = 0;
let orgSkipped = 0;
let orgErrors = 0;

orgs.find().forEach((doc) => {
  try {
    const setFields = {};
    const unsetFields = {};

    const alreadyMigrated = doc.address != null && typeof doc.address === "object";

    if (!alreadyMigrated) {
      const hasOldFields = typeof doc.address === "string"
          || doc.city !== undefined || doc.state !== undefined || doc.zipcode !== undefined;

      if (hasOldFields) {
        setFields.address = {
          line1: (typeof doc.address === "string" && doc.address) ? doc.address : null,
          line2: null,
          city: doc.city || null,
          state: doc.state || null,
          zip: doc.zipcode || null,
          county: null,
        };
        if (doc.city !== undefined) unsetFields.city = "";
        if (doc.state !== undefined) unsetFields.state = "";
        if (doc.zipcode !== undefined) unsetFields.zipcode = "";
      }
    }

    const hasSet = Object.keys(setFields).length > 0;
    const hasUnset = Object.keys(unsetFields).length > 0;

    if (hasSet || hasUnset) {
      const update = {};
      if (hasSet) update.$set = setFields;
      if (hasUnset) update.$unset = unsetFields;
      orgs.updateOne({ _id: doc._id }, update);
      orgMigrated++;
    } else {
      orgSkipped++;
    }
  } catch (e) {
    print(`ERROR on org ${doc._id}: ${e.message}`);
    orgErrors++;
  }
});

print(`\nDone. Migrated: ${orgMigrated}, Skipped: ${orgSkipped}, Errors: ${orgErrors}`);

print("\n=== Org Verification ===");
const orgsWithOldFields = orgs.countDocuments({
  $or: [
    { city: { $exists: true } },
    { state: { $exists: true } },
    { zipcode: { $exists: true } },
  ],
});
print(`Org documents still with flat city/state/zipcode: ${orgsWithOldFields}`);

const orgsWithNestedAddr = orgs.countDocuments({ "address.line1": { $exists: true } });
print(`Org documents with nested address: ${orgsWithNestedAddr}`);

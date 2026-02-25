/**
 * Rollback script: Reverts the user schema migration
 *
 * Restores:
 *   currentName -> firstName/lastName
 *   personalAddress -> address/city/state/zipcode
 *   phoneBook[0].phoneNumber -> phone
 *   sex -> optionalInformation.basicInfo.genderAssignedAtBirth
 *
 * Run with:   mongosh <connection-string> scripts/rollback-user-schema.js
 * Databases:  prod-db, test-db
 */

const dbName = db.getName();
const ALLOWED_DBS = ["prod-db", "test-db"];

if (!ALLOWED_DBS.includes(dbName)) {
  print(`ERROR: connected to "${dbName}" — only ${ALLOWED_DBS.join(", ")} are allowed.`);
  quit(1);
}

print(`\n=== User Schema ROLLBACK on "${dbName}" ===\n`);

const users = db.getCollection("user");
const total = users.countDocuments();
print(`Total user documents: ${total}`);

let rolledBack = 0;
let skipped = 0;
let errors = 0;

users.find().forEach((doc) => {
  try {
    const setFields = {};
    const unsetFields = {};

    // Restore firstName/lastName from currentName
    if (doc.currentName) {
      if (doc.currentName.first) setFields.firstName = doc.currentName.first;
      if (doc.currentName.last) setFields.lastName = doc.currentName.last;
      unsetFields.currentName = "";
    }

    // Restore address fields from personalAddress
    if (doc.personalAddress) {
      if (doc.personalAddress.line1) setFields.address = doc.personalAddress.line1;
      if (doc.personalAddress.city) setFields.city = doc.personalAddress.city;
      if (doc.personalAddress.state) setFields.state = doc.personalAddress.state;
      if (doc.personalAddress.zip) setFields.zipcode = doc.personalAddress.zip;
      unsetFields.personalAddress = "";
    }

    // Restore phone from phoneBook primary entry
    if (doc.phoneBook && doc.phoneBook.length > 0) {
      const primary = doc.phoneBook.find((e) => e.label === "primary");
      if (primary) {
        setFields.phone = primary.phoneNumber;
      }
    }

    // Restore genderAssignedAtBirth from sex
    if (doc.sex) {
      setFields["optionalInformation.basicInfo.genderAssignedAtBirth"] = doc.sex;
      unsetFields.sex = "";
    }

    // Remove new fields
    if (doc.mailAddress) unsetFields.mailAddress = "";
    if (doc.nameHistory) unsetFields.nameHistory = "";
    if (doc.motherName) unsetFields.motherName = "";
    if (doc.fatherName) unsetFields.fatherName = "";

    const hasSet = Object.keys(setFields).length > 0;
    const hasUnset = Object.keys(unsetFields).length > 0;

    if (hasSet || hasUnset) {
      const update = {};
      if (hasSet) update.$set = setFields;
      if (hasUnset) update.$unset = unsetFields;

      users.updateOne({ _id: doc._id }, update);
      rolledBack++;
    } else {
      skipped++;
    }
  } catch (e) {
    print(`ERROR on doc ${doc._id}: ${e.message}`);
    errors++;
  }
});

print(`\nDone. Rolled back: ${rolledBack}, Skipped: ${skipped}, Errors: ${errors}`);

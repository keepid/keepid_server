/**
 * MongoDB seed script: application_registry collection
 *
 * Migrates all entries from the hardcoded ApplicationRegistry Java enum
 * into the application_registry MongoDB collection.
 *
 * Idempotent: skips entries that already exist (matched by compound key).
 * Creates a unique compound index on (idCategoryType, state, applicationSubtype, pidlSubtype).
 *
 * Run with:   mongosh <connection-string>/<db-name> scripts/seed-application-registry.js
 * Databases:  staging-db, test-db, prod-db
 */

const dbName = db.getName();
const ALLOWED_DBS = ["staging-db", "prod-db", "test-db"];

if (!ALLOWED_DBS.includes(dbName)) {
  print(
    `ERROR: connected to "${dbName}" — only ${ALLOWED_DBS.join(", ")} are allowed.`
  );
  quit(1);
}

print(`\n=== Application Registry Seed on "${dbName}" ===\n`);

const coll = db.getCollection("application_registry");

coll.createIndex(
  {
    idCategoryType: 1,
    state: 1,
    applicationSubtype: 1,
    pidlSubtype: 1,
  },
  { unique: true }
);
print("Compound unique index ensured.\n");

const now = new Date();

const entries = [
  {
    idCategoryType: "Social Security Card",
    state: "Federal",
    applicationSubtype: "INITIAL",
    pidlSubtype: null,
    amount: "0",
    numWeeks: 1,
    orgMappings: [],
  },
  {
    idCategoryType: "Social Security Card",
    state: "Federal",
    applicationSubtype: "DUPLICATE",
    pidlSubtype: null,
    amount: "0",
    numWeeks: 1,
    orgMappings: [
      {
        orgName: "Face to Face",
        fileId: ObjectId("6725da57ebfdb30698fff2eb"),
      },
      {
        orgName: "TSA C.A.T.S Program",
        fileId: ObjectId("672870a32de24d7c8ba75bf4"),
      },
    ],
  },
  {
    idCategoryType: "Birth Certificate",
    state: "Pennsylvania",
    applicationSubtype: "INITIAL",
    pidlSubtype: null,
    amount: "0",
    numWeeks: 1,
    orgMappings: [
      {
        orgName: "TSA C.A.T.S Program",
        fileId: ObjectId("67206bbb17d3b63a60456c48"),
      },
    ],
  },
  {
    idCategoryType: "Birth Certificate",
    state: "Pennsylvania",
    applicationSubtype: "DUPLICATE",
    pidlSubtype: null,
    amount: "20.0",
    numWeeks: 1,
    orgMappings: [],
  },
  {
    idCategoryType: "Birth Certificate",
    state: "Pennsylvania",
    applicationSubtype: "HOMELESS",
    pidlSubtype: null,
    amount: "0",
    numWeeks: 1,
    orgMappings: [],
  },
  {
    idCategoryType: "Birth Certificate",
    state: "Pennsylvania",
    applicationSubtype: "JUVENILE_JUSTICE_INVOLVED",
    pidlSubtype: null,
    amount: "0",
    numWeeks: 1,
    orgMappings: [],
  },
  {
    idCategoryType: "Birth Certificate",
    state: "Pennsylvania",
    applicationSubtype: "VETERANS",
    pidlSubtype: null,
    amount: "0",
    numWeeks: 1,
    orgMappings: [],
  },
  {
    idCategoryType: "Drivers License / Photo ID",
    state: "Pennsylvania",
    applicationSubtype: "INITIAL",
    pidlSubtype: "Photo Id",
    amount: "0",
    numWeeks: 1,
    orgMappings: [
      {
        orgName: "TSA C.A.T.S Program",
        fileId: ObjectId("6737d8ac905dca46f9a0330e"),
      },
    ],
  },
  {
    idCategoryType: "Drivers License / Photo ID",
    state: "Pennsylvania",
    applicationSubtype: "DUPLICATE",
    pidlSubtype: "Photo Id",
    amount: "0",
    numWeeks: 1,
    orgMappings: [
      {
        orgName: "TSA C.A.T.S Program",
        fileId: ObjectId("672870a32de24d7c8ba75bf4"),
      },
    ],
  },
  {
    idCategoryType: "Drivers License / Photo ID",
    state: "Pennsylvania",
    applicationSubtype: "RENEWAL",
    pidlSubtype: "Photo Id",
    amount: "0",
    numWeeks: 1,
    orgMappings: [
      {
        orgName: "TSA C.A.T.S Program",
        fileId: ObjectId("6720705917d3b63a60456d54"),
      },
    ],
  },
  {
    idCategoryType: "Drivers License / Photo ID",
    state: "Pennsylvania",
    applicationSubtype: "CHANGE_OF_ADDRESS",
    pidlSubtype: "Photo Id",
    amount: "0",
    numWeeks: 1,
    orgMappings: [],
  },
  {
    idCategoryType: "Drivers License / Photo ID",
    state: "Pennsylvania",
    applicationSubtype: "INITIAL",
    pidlSubtype: "Driver's License",
    amount: "0",
    numWeeks: 1,
    orgMappings: [],
  },
  {
    idCategoryType: "Drivers License / Photo ID",
    state: "Pennsylvania",
    applicationSubtype: "DUPLICATE",
    pidlSubtype: "Driver's License",
    amount: "0",
    numWeeks: 1,
    orgMappings: [],
  },
  {
    idCategoryType: "Drivers License / Photo ID",
    state: "Pennsylvania",
    applicationSubtype: "RENEWAL",
    pidlSubtype: "Driver's License",
    amount: "0",
    numWeeks: 1,
    orgMappings: [],
  },
  {
    idCategoryType: "Drivers License / Photo ID",
    state: "Pennsylvania",
    applicationSubtype: "CHANGE_OF_ADDRESS",
    pidlSubtype: "Driver's License",
    amount: "0",
    numWeeks: 1,
    orgMappings: [],
  },
  {
    idCategoryType: "Birth Certificate",
    state: "Maryland",
    applicationSubtype: "INITIAL",
    pidlSubtype: null,
    amount: "0",
    numWeeks: 1,
    orgMappings: [
      {
        orgName: "TSA C.A.T.S Program",
        fileId: ObjectId("67206361a4290f111ad4fd3f"),
      },
    ],
  },
  {
    idCategoryType: "Birth Certificate",
    state: "New Jersey",
    applicationSubtype: "INITIAL",
    pidlSubtype: null,
    amount: "0",
    numWeeks: 1,
    orgMappings: [
      {
        orgName: "TSA C.A.T.S Program",
        fileId: ObjectId("672870922de24d7c8ba75bee"),
      },
    ],
  },
];

let inserted = 0;
let skipped = 0;
let errors = 0;

for (const entry of entries) {
  const filter = {
    idCategoryType: entry.idCategoryType,
    state: entry.state,
    applicationSubtype: entry.applicationSubtype,
    pidlSubtype: entry.pidlSubtype,
  };

  const existing = coll.findOne(filter);
  if (existing) {
    const label = `${entry.idCategoryType} / ${entry.state} / ${entry.applicationSubtype}${entry.pidlSubtype ? " / " + entry.pidlSubtype : ""}`;
    print(`  SKIP (exists): ${label}`);
    skipped++;
    continue;
  }

  try {
    coll.insertOne({
      ...entry,
      createdAt: now,
      lastModifiedAt: now,
    });
    const label = `${entry.idCategoryType} / ${entry.state} / ${entry.applicationSubtype}${entry.pidlSubtype ? " / " + entry.pidlSubtype : ""}`;
    print(`  INSERT: ${label} (${entry.orgMappings.length} org mappings)`);
    inserted++;
  } catch (e) {
    const label = `${entry.idCategoryType} / ${entry.state} / ${entry.applicationSubtype}`;
    print(`  ERROR inserting ${label}: ${e.message}`);
    errors++;
  }
}

print(`\n=== Done ===`);
print(`  Inserted: ${inserted}`);
print(`  Skipped:  ${skipped}`);
print(`  Errors:   ${errors}`);
print(`  Total in collection: ${coll.countDocuments()}\n`);

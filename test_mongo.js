const { MongoClient } = require('mongodb');
async function run() {
  const uri = process.env.MONGODB_URI || "mongodb://localhost:27017/keepid"; // Need to find true URI
  console.log("Looking at mongod...");
}
run();

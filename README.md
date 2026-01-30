# keepid_server
[![Build Status](https://travis-ci.org/keepid/keepid_server.svg?branch=master)](https://travis-ci.org/keepid/keepid_server)[![codecov](https://codecov.io/gh/crchong1/keepid_server/branch/master/graph/badge.svg?token=4wI4QFPmQv)](https://codecov.io/gh/crchong1/keepid_server)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/aed15cbdd0a249a69a0439fd7784cb1d)](https://www.codacy.com/gh/keepid/keepid_server?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=keepid/keepid_server&amp;utm_campaign=Badge_Grade)
![Uptime Robot ratio (30 days)](https://img.shields.io/uptimerobot/ratio/m785737816-1b614ce058ee62b1c5d2b6a0)
![Uptime Robot status](https://img.shields.io/uptimerobot/status/m785737816-1b614ce058ee62b1c5d2b6a0)
![GitHub issues](https://img.shields.io/github/issues/keepid/keepid_server)
![Mozilla HTTP Observatory Grade](https://img.shields.io/mozilla-observatory/grade-score/server.keep.id?publish)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/keepid/keepid_server)
![Snyk Vulnerabilities for GitHub Repo](https://img.shields.io/snyk/vulnerabilities/github/keepid/keepid_server)

Keep.id Server Application. Built with Java and Kotlin, this application serves to empower organizations fighting homelessness with tools to help with document storage and other aid.

https://docs.google.com/document/d/1gmrSSzV4gzMEUXjU6bq57TjtAl6zrw1i61FxxzykK94/edit?usp=sharing 


### Docker Setup (Recommended)
This is the easiest way to get started. It works on any device (Mac, Windows, Linux) and does NOT require you to install Java, Maven, or MongoDB on your machine.

**Prerequisites:**
- [Docker Desktop](https://www.docker.com/products/docker-desktop)

**How to run:**
1. Create a `.env` file in the root directory (same level as this README) regarding keys. You will need:
   ```
   MASTERKEYURI=...
   GOOGLE_APPLICATION_CREDENTIALS_CONTENTS={...}
   ```
   *Ask a team member for these values if you don't have them.*

2. Run the application:
   ```bash
   docker-compose up
   ```
   This will start both the Java Server (port 7000) and the MongoDB database.

   > **Note:** If you make changes to the Java code, you must rebuilding the image to see them:
   > 1. Stop the server (`Ctrl+C`)
   > 2. Rebuild and run: `docker-compose up --build`

3. The server is ready when you see "Javalin has started" in the logs.
   > **First Run Note:** You may see a log saying "No key found for staging-db. Generating new key...". This is normal and means the database is being initialized for you.

   Access the server at `http://localhost:8080`.

**How to debug:**
The app exposes port `5005` for remote debugging. In IntelliJ:
1. Run -> Edit Configurations -> + -> Remote JVM Debug.
2. Set Port to `5005` and name it "Docker Debug".
3. Run this configuration *after* `docker-compose up` is running.

---

### Legacy Manual Setup
How to get the backend service running locally (without Docker):

1) make sure you have Java in the JAVA_HOME environment variable
2) make sure you have maven (run "mvn --version" in terminal to check)
3) make sure you have Java 21 JDK/JRE installed. If you wish to have multiple Java versions, look into Java Jabba, which is a Java Version Manager.
4) make sure you install IntelliJ, and install these extensions under File -> Settings -> Plugins:
    - google-java-format (this will help enforce Java code style)
    - Save Actions (this will automatically enforce Java code style on save)
5) make sure that in IntelliJ, maven auto-import is turned on (there will be a pop up usually, or just search up online on how to enable this). You may have to restart IntelliJ multiple times for it to show up
6) under File -> Settings -> Other Settings -> Save Actions, check the first two boxes under General and the first two boxes under Formatting Actions. This will enforce the code style on save
7) under File -> Project Structure -> Modules, set Project SDK and Project Language Level to 21. Additionally, YOU MUST SET YOUR SOURCES, TESTS, TEST RESOURCES, AND EXCLUDED FOLDERS. This step is VERY IMPORTANT, or else nothing will run. Set Source Folders as src/main, Test Source Folders as src/test, Test Resource Folders as src/test/resources, and Excluded Folders as target. Make sure Language level is set to 21. Under the Dependencies tab, make sure the Project SDK is set to 21. 
8) you can run tests or run App
9) check logs in logs/app.log
10) ALSO if you are having a Null Pointer CustomException in the Mongo Config, please follow the instructions here: https://github.com/Ashald/EnvFile. We are deprecating the use of dotenv because it is interfering
with our heroku deployment (also the heroku deployment doesn't see the env file anyway). Therefore, please edit the run configurations in IntelliJ at this link here: https://github.com/Ashald/EnvFile. 

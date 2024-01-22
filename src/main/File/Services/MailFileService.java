package File.Services;

import static Issue.IssueController.issueReportActualURL;
import static io.javalin.Javalin.log;

import Config.Message;
import Config.Service;
import File.FileMessage;
import Issue.IssueReportMessage;
import Security.EncryptionController;
import User.UserType;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import java.time.LocalDateTime;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

public class MailFileService implements Service {

  private ObjectId fileId;
  private String username;
  private UserType privilegeLevel;
  private MongoDatabase db;
  private EncryptionController encryptionController;
  private String description;
  private String mailAddress;
  private String returnAddress;
  private String price;
  private String applicationTitle;

  public MailFileService(
      MongoDatabase db,
      ObjectId fileId,
      String username,
      UserType privilegeLevel,
      String description,
      String price,
      String mailAddress,
      String returnAddress,
      EncryptionController encryptionController) {
    this.db = db;
    this.fileId = fileId;
    this.username = username;
    this.privilegeLevel = privilegeLevel;
    this.description = description;
    this.applicationTitle = this.findApplicationTitle(fileId);
    this.mailAddress = mailAddress;
    this.returnAddress = returnAddress;
    this.price = price;
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    try {
      MongoCollection<Document> mailCollection = db.getCollection("mail");

      //      if (shouldMail(mailCollection, fileId)) {
      if (true) {
        Message slackResponse = sendSlackNotification("File Mailing Request", this.description);
        if (slackResponse != IssueReportMessage.SUCCESS) {
          return slackResponse;
        }

        updateLastMailedTime(mailCollection, fileId);
        return FileMessage.SUCCESS;
      } else {
        return FileMessage.ALREADY_MAILED_RECENTLY;
      }
    } catch (Exception e) {
      log.error("Error in executeAndGetResponse: " + e.getMessage(), e);
      return FileMessage.SERVER_ERROR;
    }
  }

  private String findApplicationTitle(ObjectId fileId) {
    try {
      MongoCollection<Document> completedApplicationsCollection =
          db.getCollection("COMPLETED_APPLICATION.files");

      // Find the document in the collection with the matching fileId
      Document applicationDocument =
          completedApplicationsCollection.find(Filters.eq("_id", fileId)).first();
      log.error(String.valueOf(applicationDocument));

      if (applicationDocument != null) {
        // Extract the applicationTitle field from the document
        String applicationTitle = applicationDocument.getString("filename");
        if (applicationTitle != null) {
          return applicationTitle;
        }
      }
    } catch (MongoException e) {
      log.error("MongoDB error in findApplicationTitle: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error in findApplicationTitle: " + e.getMessage(), e);
    }

    // Return a default value or handle the case when the title is not found
    return "No Title found";
  }

  private void updateLastMailedTime(MongoCollection<Document> mailCollection, ObjectId fileId) {
    Bson filter = Filters.eq("fileId", fileId);
    Bson updateOperation =
        Updates.combine(
            Updates.set("lastMailedAt", LocalDateTime.now().toString()),
            Updates.set("applicationTitle", applicationTitle),
            Updates.set("price", price),
            Updates.set("mailAddress", mailAddress),
            Updates.set("returnAddress", returnAddress));
    mailCollection.updateOne(filter, updateOperation, new UpdateOptions().upsert(true));
  }

  // want to remove this for the future
  private Message sendSlackNotification(String title, String description) {
    JSONArray blocks = new JSONArray();
    JSONObject titleJson = new JSONObject();
    titleJson.put("type", "section");
    titleJson.put(
        "text", new JSONObject().put("type", "mrkdwn").put("text", "*Issue Title: * " + title));
    blocks.put(titleJson);

    JSONObject desJson = new JSONObject();
    desJson.put("type", "section");
    desJson.put(
        "text",
        new JSONObject().put("type", "mrkdwn").put("text", "*Issue Description: * " + description));
    blocks.put(desJson);

    JSONObject input = new JSONObject();
    input.put("blocks", blocks);

    HttpResponse<?> posted =
        Unirest.post(issueReportActualURL)
            .header("accept", "application/json")
            .body(input.toString())
            .asEmpty();

    if (!posted.isSuccess()) {
      return IssueReportMessage.SLACK_FAILED;
    }
    return IssueReportMessage.SUCCESS;
  }

  private boolean shouldMail(MongoCollection<Document> mailCollection, ObjectId fileId) {
    Document mailData = mailCollection.find(Filters.eq("fileId", fileId)).first();

    if (mailData == null) {
      // If no record exists, it means it hasn't been mailed before
      return true;
    }

    String lastMailedAtStr = mailData.getString("lastMailedAt");
    LocalDateTime lastMailedAt =
        lastMailedAtStr != null ? LocalDateTime.parse(lastMailedAtStr) : null;
    return lastMailedAt == null || LocalDateTime.now().isAfter(lastMailedAt.plusDays(1));
  }
}

package User.Services;

import Activity.Activity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.Notification.NotificationDao;
import Database.User.UserDao;
import File.File;
import Form.Form;
import Notification.Notification;
import PDF.PDFType;
import User.User;
import User.UserMessage;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class RemoveOrganizationMemberService implements Service {
  private final MongoDatabase db;
  private final UserDao userDao;
  private final FileDao fileDao;
  private final FormDao formDao;
  private final ActivityDao activityDao;
  private final NotificationDao notificationDao;
  private final String requestingUsername;
  private final String targetUsername;
  private final String requestingOrgName;
  private final UserType requestingUserType;

  public RemoveOrganizationMemberService(
      MongoDatabase db,
      UserDao userDao,
      FileDao fileDao,
      FormDao formDao,
      ActivityDao activityDao,
      NotificationDao notificationDao,
      String requestingUsername,
      String targetUsername,
      String requestingOrgName,
      UserType requestingUserType) {
    this.db = db;
    this.userDao = userDao;
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.activityDao = activityDao;
    this.notificationDao = notificationDao;
    this.requestingUsername = requestingUsername;
    this.targetUsername = targetUsername;
    this.requestingOrgName = requestingOrgName;
    this.requestingUserType = requestingUserType;
  }

  @Override
  public Message executeAndGetResponse() {
    if (requestingUserType != UserType.Admin && requestingUserType != UserType.Director) {
      log.info("User {} lacks privilege to remove members", requestingUsername);
      return UserMessage.INSUFFICIENT_PRIVILEGE;
    }

    if (targetUsername == null || targetUsername.isBlank()) {
      log.info("Target username is empty");
      return UserMessage.INVALID_PARAMETER;
    }

    if (targetUsername.equals(requestingUsername)) {
      log.info("User {} attempted to remove themselves", requestingUsername);
      return UserMessage.INVALID_PARAMETER;
    }

    Optional<User> optionalTarget = userDao.get(targetUsername);
    if (optionalTarget.isEmpty()) {
      log.info("Target user {} not found", targetUsername);
      return UserMessage.USER_NOT_FOUND;
    }

    User targetUser = optionalTarget.get();

    if (targetUser.getUserType() == UserType.Admin || targetUser.getUserType() == UserType.Director) {
      log.info("Cannot remove admin/director {}. Admins cannot be removed from an organization.",
          targetUsername);
      return UserMessage.INSUFFICIENT_PRIVILEGE;
    }

    if (!targetUser.getOrganization().equals(requestingOrgName)) {
      log.info("User {} tried to remove {} from a different organization",
          requestingUsername, targetUsername);
      return UserMessage.CROSS_ORG_ACTION_DENIED;
    }

    String username = targetUser.getUsername();
    deleteGridFsPdfs(username);
    deleteFileRecords(username);
    deleteFormRecords(username);
    deleteActivityRecords(username);
    deleteNotificationRecords(username);

    userDao.delete(username);
    log.info("User {} successfully removed member {} from organization {}",
        requestingUsername, targetUsername, requestingOrgName);
    return UserMessage.SUCCESS;
  }

  private void deleteGridFsPdfs(String username) {
    if (db == null) {
      log.warn("MongoDatabase is null, skipping GridFS deletion for user {}", username);
      return;
    }
    PDFType[] pdfTypes = {
        PDFType.BLANK_FORM,
        PDFType.COMPLETED_APPLICATION,
        PDFType.IDENTIFICATION_DOCUMENT
    };
    for (PDFType pdfType : pdfTypes) {
      GridFSBucket gridBucket = GridFSBuckets.create(db, pdfType.toString());
      List<GridFSFile> files = gridBucket
          .find(eq("metadata.uploader", username))
          .into(new ArrayList<>());
      for (GridFSFile file : files) {
        gridBucket.delete(file.getObjectId());
      }
      log.info("Deleted {} GridFS {} entries for user {}", files.size(), pdfType, username);
    }
  }

  private void deleteFileRecords(String username) {
    List<File> files = fileDao.getAll(username);
    if (files == null) return;
    int count = files.size();
    for (File file : new ArrayList<>(files)) {
      fileDao.delete(file);
    }
    log.info("Deleted {} file records for user {}", count, username);
  }

  private void deleteFormRecords(String username) {
    List<Form> forms = formDao.get(username);
    if (forms == null) return;
    int count = forms.size();
    for (Form form : new ArrayList<>(forms)) {
      formDao.delete(form);
    }
    log.info("Deleted {} form records for user {}", count, username);
  }

  private void deleteActivityRecords(String username) {
    List<Activity> activities = activityDao.getAllFromUser(username);
    if (activities == null) return;
    int count = activities.size();
    for (Activity activity : new ArrayList<>(activities)) {
      activityDao.delete(activity);
    }
    log.info("Deleted {} activity records for user {}", count, username);
  }

  private void deleteNotificationRecords(String username) {
    List<Notification> notifications = notificationDao.getByClientUsername(username);
    if (notifications == null) return;
    int count = notifications.size();
    for (Notification notification : new ArrayList<>(notifications)) {
      notificationDao.delete(notification);
    }
    log.info("Deleted {} notification records for user {}", count, username);
  }
}

package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import PDF.PDFType;
import User.User;
import User.UserMessage;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class RemoveOrganizationMemberService implements Service {
  private final MongoDatabase db;
  private final UserDao userDao;
  private final String requestingUsername;
  private final String targetUsername;
  private final String requestingOrgName;
  private final UserType requestingUserType;

  public RemoveOrganizationMemberService(
      MongoDatabase db,
      UserDao userDao,
      String requestingUsername,
      String targetUsername,
      String requestingOrgName,
      UserType requestingUserType) {
    this.db = db;
    this.userDao = userDao;
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

    deleteUserFiles(targetUser.getUsername());
    userDao.delete(targetUser.getUsername());
    log.info("User {} successfully removed member {} from organization {}",
        requestingUsername, targetUsername, requestingOrgName);
    return UserMessage.SUCCESS;
  }

  private void deleteUserFiles(String username) {
    if (db == null) {
      log.warn("MongoDatabase is null, skipping file deletion for user {}", username);
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
          .find(Filters.eq("metadata.uploader", username))
          .into(new ArrayList<>());
      for (GridFSFile file : files) {
        gridBucket.delete(file.getObjectId());
      }
    }
  }
}

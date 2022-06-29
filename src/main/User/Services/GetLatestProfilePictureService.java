package User.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import File.File;
import File.FileType;
import User.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GetLatestProfilePictureService implements Service {
  private FileDao fileDao;
  private String username;
  private ObjectId fileId;

  public GetLatestProfilePictureService(FileDao fileDao, String username) {
    this.fileDao = fileDao;
    this.username = username;
  }

  @Override
  public Message executeAndGetResponse() {
    List<ObjectId> allFiles =
        fileDao.getAll(this.username).stream()
            .filter(file -> file.getFileType() == FileType.PROFILE_PICTURE)
            //            .sorted(Comparator.comparing(File::getUploadedAt).reversed())
            .map(File::getFileId)
            .collect(Collectors.toList());
    System.out.println("Here are all the profile picture files");
    allFiles.forEach(
        file -> {
          System.out.println(file);
        });
    if (allFiles.isEmpty()) {
      return UserMessage.INVALID_PARAMETER;
      // new message like Empty Profile Picture
    }
    ObjectId latestProfilePicture = allFiles.stream().findFirst().get();
    this.fileId = latestProfilePicture;
    return UserMessage.SUCCESS;
  }

  public ObjectId getFileId() {
    return fileId;
  }
}

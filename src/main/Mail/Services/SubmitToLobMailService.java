package Mail.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Mail.MailDao;
import File.File;
import Mail.FormMailAddress;
import Mail.Mail;
import Mail.MailMessage;
import Mail.MailStatus;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.lob.api.ApiClient;
import com.lob.api.Configuration;
import com.lob.api.auth.*;
import com.lob.api.client.AddressesApi;
import com.lob.api.client.ChecksApi;
import com.lob.api.client.LettersApi;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.openapitools.client.model.*;
import org.threeten.bp.DateTimeUtils;

public class SubmitToLobMailService implements Service {
  private MailDao mailDao;
  private Mail mail;
  private String lobApiKey;
  private FileDao fileDao;
  private String username;
  private boolean isCheck;

  public SubmitToLobMailService(
      FileDao fileDao,
      MailDao mailDao,
      FormMailAddress formMailAddress,
      String fileId,
      String username,
      String loggedInUser,
      String lobApiKey,
      boolean isCheck) {

    Mail mail = new Mail(new ObjectId(fileId), formMailAddress, username, loggedInUser);
    this.mail = mail;
    this.lobApiKey = lobApiKey;
    this.mailDao = mailDao;
    this.fileDao = fileDao;
    this.username = username;
    mailDao.save(mail); // Save created mail
    this.isCheck = isCheck;
  }

  @Override
  public Message executeAndGetResponse() throws Exception {
    ApiClient lobClient = Configuration.getDefaultApiClient();
    HttpBasicAuth basicAuth = (HttpBasicAuth) lobClient.getAuthentication("basicAuth");
    basicAuth.setUsername(this.lobApiKey);
    this.printAllFiles(this.username);

    if (isCheck) {
      ChecksApi checksApi = new ChecksApi(lobClient);
      FormMailAddress mailAddress = mail.getMailingAddress();

      AddressEditable toAddress = new AddressEditable(); // build toAddress
      toAddress.setName(mailAddress.getNameForCheck());
      toAddress.setAddressLine1(mailAddress.getStreet1());
      if (mailAddress.getStreet2() != "") {
        toAddress.setAddressLine2(mailAddress.getStreet2());
      }
      toAddress.setAddressState(mailAddress.getState());
      toAddress.setAddressCity(mailAddress.getCity());
      toAddress.setAddressZip(mailAddress.getZipcode());
      toAddress.setDescription(mailAddress.getDescription());
      toAddress.setCompany(mailAddress.getOffice_name());

      CheckEditable checkEditable = new CheckEditable();
      checkEditable.setBankAccount("bank_8ed776f222c2985");
      checkEditable.setFrom("adr_13508cc9d5747779");
      checkEditable.setAmount(39.5F);
      checkEditable.setMetadata(
          Map.of(
              "Mail Username",
              mail.getTargetUsername(),
              "Mail Requestor",
              mail.getRequesterUsername(),
              "Mail ID:",
              mail.getId().toString()));
      checkEditable.setMemo("Application Fee");
      checkEditable.setTo(toAddress);

      System.out.println("Mail File id: " + mail.getFileId());
      File file = fileDao.get(mail.getFileId()).orElseThrow();
      System.out.println("Filename: " + file.getFilename());
      System.out.println("File id: " + file.getId());
      System.out.println("File file id: " + file.getFileId());

      byte[] pdfData = IOUtils.toByteArray(fileDao.getStream(file.getId()).orElseThrow());
      String uri = uploadFileToGCS(pdfData, mail.getId().toString());

      checkEditable.setAttachment(uri);

      Check checkAndLetter =
          checksApi.create(checkEditable, mail.getTargetUsername() + mail.getId().toString());
      this.mail.setLobId(checkAndLetter.getId());
      this.mail.setMailStatus(MailStatus.MAILED);
      this.mail.setLobCreatedAt(DateTimeUtils.toDate(checkAndLetter.getDateCreated().toInstant()));
      mailDao.update(this.mail);
      return MailMessage.MAIL_SUCCESS;
    } else {
      AddressesApi addressesApi = new AddressesApi(lobClient);
      AddressEditable toAddress = new AddressEditable();
      LettersApi lettersApi = new LettersApi(lobClient);
      FormMailAddress mailAddress = mail.getMailingAddress();
      toAddress.setName(mailAddress.getNameForCheck());
      toAddress.setAddressLine1(mailAddress.getStreet1());
      if (mailAddress.getStreet2() != "") {
        toAddress.setAddressLine2(mailAddress.getStreet2());
      }
      toAddress.setAddressState(mailAddress.getState());
      toAddress.setAddressCity(mailAddress.getCity());
      toAddress.setAddressZip(mailAddress.getZipcode());
      toAddress.setDescription(mailAddress.getDescription());
      toAddress.setCompany(mailAddress.getOffice_name());

      CheckEditable checkEditable = new CheckEditable();
      checkEditable.setBankAccount("bank_8ed776f222c2985");
      checkEditable.setFrom("adr_13508cc9d5747779");
      checkEditable.setAmount(39.5F);
      checkEditable.setMetadata(
          Map.of(
              "Mail Username",
              mail.getTargetUsername(),
              "Mail Requestor",
              mail.getRequesterUsername(),
              "Mail ID:",
              mail.getId().toString()));
      checkEditable.setMemo("Application Fee");
      checkEditable.setTo(toAddress);

      System.out.println("Mail File id: " + mail.getFileId());
      File file = fileDao.get(mail.getFileId()).orElseThrow();
      System.out.println("Filename: " + file.getFilename());
      System.out.println("File id: " + file.getId());
      System.out.println("File file id: " + file.getFileId());

      byte[] pdfData = IOUtils.toByteArray(fileDao.getStream(file.getId()).orElseThrow());
      String uri = uploadFileToGCS(pdfData, mail.getId().toString());

      checkEditable.setAttachment(uri);

      Check checkAndLetter =
          checksApi.create(checkEditable, mail.getTargetUsername() + mail.getId().toString());
      this.mail.setLobId(checkAndLetter.getId());
      this.mail.setMailStatus(MailStatus.MAILED);
      this.mail.setLobCreatedAt(DateTimeUtils.toDate(checkAndLetter.getDateCreated().toInstant()));
      mailDao.update(this.mail);
      return MailMessage.MAIL_SUCCESS;
    }
    LettersApi lettersApi = new LettersApi(lobClient);

    return MailMessage.FAILED_WHEN_SENDING_MAIL;
  }

  private void printAllFiles(String username) {
    for (String filename :
        fileDao.getAll(username).stream()
            .map(x -> x.getFilename() + ", " + x.getFileId().toString())
            .collect(Collectors.toList())) {
      System.out.println("Filename: " + filename);
    }
  }

  private String uploadFileToGCS(byte[] fileData, String fileId) throws Exception {
    String bucketName = "keepid-lob-mail";
    Storage storage =
        StorageOptions.newBuilder()
            .setProjectId("keepid-302503")
            .setCredentials(
                GoogleCredentials.fromStream(
                    new FileInputStream(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"))))
            .build()
            .getService();

    BlobId blobId = BlobId.of(bucketName, fileId);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/pdf").build();
    storage.create(blobInfo, fileData);
    URL signedUrl =
        storage.signUrl(
            blobInfo, 1, TimeUnit.HOURS, Storage.SignUrlOption.httpMethod(HttpMethod.GET));
    return signedUrl.toString();
  }
}

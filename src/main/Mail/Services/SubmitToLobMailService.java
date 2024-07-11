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

  public SubmitToLobMailService(
      FileDao fileDao,
      MailDao mailDao,
      FormMailAddress formMailAddress,
      String username,
      String loggedInUser,
      String lobApiKey) {

    Mail mail =
        new Mail(new ObjectId("668f41c2248acc02d93f157e"), formMailAddress, username, loggedInUser);
    this.mail = mail;
    this.lobApiKey = lobApiKey;
    this.mailDao = mailDao;
    this.fileDao = fileDao;
    mailDao.save(mail); // Save created mail
  }

  @Override
  public Message executeAndGetResponse() throws Exception {
    System.out.println("Here1");
    ApiClient lobClient = Configuration.getDefaultApiClient();
    HttpBasicAuth basicAuth = (HttpBasicAuth) lobClient.getAuthentication("basicAuth");
    basicAuth.setUsername(this.lobApiKey);

    AddressesApi addressesApi = new AddressesApi(lobClient);
    LettersApi lettersApi = new LettersApi(lobClient);
    AddressEditable toAddress = new AddressEditable();
    ChecksApi checksApi = new ChecksApi(lobClient);

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

    Address from = addressesApi.get("adr_13508cc9d5747779"); // sender address id on lob account
    AddressEditable fromEditable = new AddressEditable();
    fromEditable.setName(from.getName());
    fromEditable.setAddressLine1(from.getAddressLine1());
    fromEditable.setAddressLine2(from.getAddressLine2());
    fromEditable.setAddressState(from.getAddressState());
    fromEditable.setAddressCity(from.getAddressCity());
    fromEditable.setAddressZip(from.getAddressZip());
    fromEditable.setDescription(from.getDescription());
    fromEditable.setCompany(from.getCompany());

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
    for (String filename :
        fileDao.getAll("SAMPLE-CLIENT").stream()
            .map(x -> x.getFilename() + ", " + x.getFileId().toString())
            .collect(Collectors.toList())) {
      System.out.println(filename);
    }
    System.out.println("Here3: " + mail.getFileId());
    File file = fileDao.get(new ObjectId("668f41c2248acc02d93f157e")).orElseThrow();
    System.out.println("Filename: " + file.getFilename());

    byte[] pdfData =
        IOUtils.toByteArray(
            fileDao.getStream(new ObjectId("668f41c2248acc02d93f157e")).orElseThrow());
    String uri = uploadFileToGCS(pdfData, mail.getId().toString());
    System.out.println("Here4: " + uri);

    checkEditable.setAttachment(uri);

    Check checkAndLetter =
        checksApi.create(checkEditable, mail.getTargetUsername() + mail.getId().toString());
    this.mail.setLobId(checkAndLetter.getId());
    this.mail.setMailStatus(MailStatus.MAILED);
    this.mail.setLobCreatedAt(DateTimeUtils.toDate(checkAndLetter.getDateCreated().toInstant()));
    mailDao.update(this.mail);
    return MailMessage.MAIL_SUCCESS;
    //      return MailMessage.FAILED_WHEN_SENDING_MAIL;
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

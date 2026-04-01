package Mail;

import Database.File.FileDao;
import File.File;
import Security.EncryptionController;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.lob.api.ApiClient;
import com.lob.api.Configuration;
import com.lob.api.auth.HttpBasicAuth;
import com.lob.api.client.AddressesApi;
import com.lob.api.client.ChecksApi;
import com.lob.api.client.LettersApi;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.openapitools.client.model.*;
import org.threeten.bp.DateTimeUtils;

@Slf4j
public class LobMailSender implements MailSender {
  private final String lobApiKey;

  public static final String TEAM_KEEP_ADDRESS_LOB_ID = "adr_13508cc9d5747779";

  private final String bankAccountId;

  public LobMailSender(String lobApiKey, String bankAccountId) {
    this.lobApiKey = Objects.requireNonNull(lobApiKey, "Lob API key must not be null");
    this.bankAccountId = Objects.requireNonNull(bankAccountId, "Lob bank account ID must not be null");
  }

  @Override
  public MailResult sendMail(
      Mail mail,
      FileDao fileDao,
      EncryptionController encryptionController,
      ReturnAddress returnAddress)
      throws Exception {

    ApiClient lobClient = Configuration.getDefaultApiClient();
    HttpBasicAuth basicAuth = (HttpBasicAuth) lobClient.getAuthentication("basicAuth");
    basicAuth.setUsername(this.lobApiKey);

    String fromAddressId = resolveFromAddress(lobClient, returnAddress);
    FormMailAddress mailAddress = mail.getMailingAddress();

    if (mailAddress.getMaybeCheckAmount().compareTo(BigDecimal.ZERO) > 0) {
      return sendCheck(lobClient, mail, fileDao, encryptionController, mailAddress, fromAddressId);
    } else {
      return sendLetter(
          lobClient, mail, fileDao, encryptionController, mailAddress, fromAddressId);
    }
  }

  @Override
  public MailResult refreshStatus(String lobId, boolean isCheck) throws Exception {
    ApiClient lobClient = Configuration.getDefaultApiClient();
    HttpBasicAuth basicAuth = (HttpBasicAuth) lobClient.getAuthentication("basicAuth");
    basicAuth.setUsername(this.lobApiKey);

    MailResult result = new MailResult();
    result.setLobId(lobId);

    if (isCheck) {
      ChecksApi checksApi = new ChecksApi(lobClient);
      Check check = checksApi.get(lobId);
      result.setLobStatus("processed");
      if (check.getExpectedDeliveryDate() != null) {
        result.setExpectedDeliveryDate(check.getExpectedDeliveryDate().toString());
      }
      result.setTrackingEvents(convertCheckTrackingEvents(check.getTrackingEvents()));
    } else {
      LettersApi lettersApi = new LettersApi(lobClient);
      Letter letter = lettersApi.get(lobId);
      result.setLobStatus("processed");
      if (letter.getExpectedDeliveryDate() != null) {
        result.setExpectedDeliveryDate(letter.getExpectedDeliveryDate().toString());
      }
      result.setTrackingEvents(convertLetterTrackingEvents(letter.getTrackingEvents()));
    }

    return result;
  }

  private MailResult sendCheck(
      ApiClient lobClient,
      Mail mail,
      FileDao fileDao,
      EncryptionController encryptionController,
      FormMailAddress mailAddress,
      String fromAddressId)
      throws Exception {

    ChecksApi checksApi = new ChecksApi(lobClient);
    AddressEditable toAddress = buildToAddress(mailAddress, true);

    CheckEditable checkEditable = new CheckEditable();
    checkEditable.setBankAccount(bankAccountId);
    checkEditable.setFrom(fromAddressId);
    checkEditable.setAmount(mailAddress.getMaybeCheckAmount().floatValue());
    checkEditable.setMetadata(
        Map.of(
            "Mail Username", mail.getTargetUsername(),
            "Mail Requestor", mail.getRequesterUsername(),
            "Mail ID:", mail.getId().toString()));
    checkEditable.setMemo("Application Fee");
    checkEditable.setTo(toAddress);

    File file = fileDao.get(mail.getFileId()).orElseThrow();
    InputStream decryptedStream =
        encryptionController.decryptFile(
            fileDao.getStream(file.getId()).orElseThrow(),
            mail.getTargetUsername());
    byte[] pdfData = IOUtils.toByteArray(decryptedStream);
    String uri = uploadFileToGCS(pdfData, mail.getId().toString());
    checkEditable.setAttachment(uri);

    Check check =
        checksApi.create(checkEditable, mail.getTargetUsername() + mail.getId().toString());

    MailResult result = new MailResult();
    result.setLobId(check.getId());
    result.setLobCreatedAt(DateTimeUtils.toDate(check.getDateCreated().toInstant()));
    result.setLobStatus("processed");
    if (check.getExpectedDeliveryDate() != null) {
      result.setExpectedDeliveryDate(check.getExpectedDeliveryDate().toString());
    }
    result.setMailType("check");
    result.setTrackingEvents(convertCheckTrackingEvents(check.getTrackingEvents()));
    return result;
  }

  private MailResult sendLetter(
      ApiClient lobClient,
      Mail mail,
      FileDao fileDao,
      EncryptionController encryptionController,
      FormMailAddress mailAddress,
      String fromAddressId)
      throws Exception {

    LettersApi lettersApi = new LettersApi(lobClient);
    AddressEditable toAddress = buildToAddress(mailAddress, false);

    LetterEditable letterEditable = new LetterEditable();
    letterEditable.setColor(false);
    letterEditable.setFrom(fromAddressId);
    letterEditable.setMetadata(
        Map.of(
            "Mail Username", mail.getTargetUsername(),
            "Mail Requestor", mail.getRequesterUsername(),
            "Mail ID:", mail.getId().toString()));
    letterEditable.setTo(toAddress);

    File file = fileDao.get(mail.getFileId()).orElseThrow();
    InputStream decryptedStream =
        encryptionController.decryptFile(
            fileDao.getStream(file.getId()).orElseThrow(),
            mail.getTargetUsername());
    byte[] pdfData = IOUtils.toByteArray(decryptedStream);
    String uri = uploadFileToGCS(pdfData, mail.getId().toString());
    letterEditable.setFile(uri);

    Letter letter =
        lettersApi.create(letterEditable, mail.getTargetUsername() + mail.getId().toString());

    MailResult result = new MailResult();
    result.setLobId(letter.getId());
    result.setLobCreatedAt(DateTimeUtils.toDate(letter.getDateCreated().toInstant()));
    result.setLobStatus("processed");
    if (letter.getExpectedDeliveryDate() != null) {
      result.setExpectedDeliveryDate(letter.getExpectedDeliveryDate().toString());
    }
    result.setMailType("letter");
    result.setTrackingEvents(convertLetterTrackingEvents(letter.getTrackingEvents()));
    return result;
  }

  private static final int LOB_FIELD_MAX_LENGTH = 40;

  private AddressEditable buildToAddress(FormMailAddress mailAddress, boolean isCheck) {
    AddressEditable toAddress = new AddressEditable();

    String name = isCheck ? mailAddress.getNameForCheck() : mailAddress.getName();
    String company = mailAddress.getOffice_name();

    if (name != null && name.length() > LOB_FIELD_MAX_LENGTH
        && (company == null || company.isEmpty())) {
      company = name;
      name = null;
    }

    name = truncateToLobLimit(name);
    company = truncateToLobLimit(company);

    if (name != null && !name.isEmpty()) {
      toAddress.setName(name);
    }
    if (company != null && !company.isEmpty()) {
      toAddress.setCompany(company);
    }

    toAddress.setAddressLine1(mailAddress.getStreet1());
    if (mailAddress.getStreet2() != null && !mailAddress.getStreet2().isEmpty()) {
      toAddress.setAddressLine2(mailAddress.getStreet2());
    }
    toAddress.setAddressState(mailAddress.getState());
    toAddress.setAddressCity(mailAddress.getCity());
    toAddress.setAddressZip(mailAddress.getZipcode());
    if (mailAddress.getDescription() != null && !mailAddress.getDescription().isEmpty()) {
      toAddress.setDescription(mailAddress.getDescription());
    }
    return toAddress;
  }

  private String truncateToLobLimit(String value) {
    if (value == null || value.length() <= LOB_FIELD_MAX_LENGTH) return value;
    String truncated = value.substring(0, LOB_FIELD_MAX_LENGTH);
    int lastSpace = truncated.lastIndexOf(' ');
    if (lastSpace > LOB_FIELD_MAX_LENGTH / 2) {
      return truncated.substring(0, lastSpace);
    }
    return truncated;
  }

  private String resolveFromAddress(ApiClient lobClient, ReturnAddress returnAddress)
      throws Exception {
    if (returnAddress == null
        || returnAddress.getStreet1() == null
        || returnAddress.getStreet1().isBlank()) {
      return TEAM_KEEP_ADDRESS_LOB_ID;
    }
    try {
      AddressesApi addressesApi = new AddressesApi(lobClient);
      AddressEditable addressEditable = new AddressEditable();
      addressEditable.setName(
          returnAddress.getName() != null ? returnAddress.getName() : "Return Address");
      addressEditable.setAddressLine1(returnAddress.getStreet1());
      if (returnAddress.getStreet2() != null && !returnAddress.getStreet2().isEmpty()) {
        addressEditable.setAddressLine2(returnAddress.getStreet2());
      }
      addressEditable.setAddressCity(returnAddress.getCity());
      addressEditable.setAddressState(returnAddress.getState());
      addressEditable.setAddressZip(returnAddress.getZipcode());
      Address created = addressesApi.create(addressEditable);
      return created.getId();
    } catch (Exception e) {
      log.warn("Failed to create return address in Lob, falling back to default: {}", e.getMessage());
      return TEAM_KEEP_ADDRESS_LOB_ID;
    }
  }

  private List<TrackingEvent> convertLetterTrackingEvents(
      List<TrackingEventNormal> lobEvents) {
    if (lobEvents == null) return new ArrayList<>();
    List<TrackingEvent> events = new ArrayList<>();
    for (TrackingEventNormal e : lobEvents) {
      events.add(
          new TrackingEvent(
              e.getType() != null ? e.getType().getValue() : null,
              e.getName() != null ? e.getName().getValue() : null,
              e.getTime() != null ? DateTimeUtils.toDate(e.getTime().toInstant()) : null,
              e.getLocation() != null ? e.getLocation() : null));
    }
    return events;
  }

  private List<TrackingEvent> convertCheckTrackingEvents(
      List<TrackingEventNormal> lobEvents) {
    return convertLetterTrackingEvents(lobEvents);
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
            blobInfo, 2, TimeUnit.HOURS, Storage.SignUrlOption.httpMethod(HttpMethod.GET));
    return signedUrl.toString();
  }
}

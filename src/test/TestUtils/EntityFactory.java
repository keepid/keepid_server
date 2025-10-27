package TestUtils;

import Activity.Activity;
import Database.Dao;
import File.File;
import File.FileType;
import Form.*;
import Organization.Organization;
import Security.SecurityUtils;
import Security.Tokens;
import User.IpObject;
import User.User;
import User.UserType;
import Validation.ValidationException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import org.bson.types.ObjectId;

public class EntityFactory {
  public static final long TEST_DATE = 1577862000000L; // Jan 1 2020
  public static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of(2020, 1, 1, 0, 0);

  public static PartialUser createUser() {
    return new PartialUser();
  }

  public static PartialOrganization createOrganization() {
    return new PartialOrganization();
  }

  public static PartialTokens createTokens() {
    return new PartialTokens();
  }

  public static PartialActivity createActivity() {
    return new PartialActivity();
  }

  public static PartialForm createForm() {
    return new PartialForm();
  }

  public static PartialFile createFile() {
    return new PartialFile();
  }

  public static class PartialFile implements PartialObject<File> {

    private ObjectId id = new ObjectId(); // id of file in collection (metadata)
    private String filename = "testFilename";
    private FileType fileType = FileType.MISC;
    private Date uploadedAt = new Date(TEST_DATE);
    private String username = "testUsername";
    private String organizationName = "testOrganizationName";
    private String contentType = "testContentType";

    @Override
    public File build() {
      File newFile =
          new File(id, filename, fileType, uploadedAt, username, organizationName, contentType);
      newFile.setFileStream(InputStream.nullInputStream());
      return newFile;
    }

    @Override
    public File buildAndPersist(Dao<File> dao) {
      File file = this.build();
      dao.save(file);
      return file;
    }

    public PartialFile withId(ObjectId id) {
      this.id = id;
      return this;
    }

    public PartialFile withFilename(String filename) {
      this.filename = filename;
      return this;
    }

    public PartialFile withFileType(FileType fileType) {
      this.fileType = fileType;
      return this;
    }

    public PartialFile withUploadedAt(Date uploadedAt) {
      this.uploadedAt = uploadedAt;
      return this;
    }

    public PartialFile withUsername(String username) {
      this.username = username;
      return this;
    }

    public PartialFile withOrganizationName(String orgName) {
      this.organizationName = orgName;
      return this;
    }

    public PartialFile withContentType(String contentType) {
      this.contentType = contentType;
      return this;
    }
  }

  public static class PartialForm implements PartialObject<Form> {
    private int defaultNumLines = 10;
    private String username = "testFirstName";
    private Optional<String> uploaderUsername = Optional.of("testuploadername");
    private LocalDateTime uploadedAt = TEST_DATE_TIME;
    private ObjectId conditionalFieldId = new ObjectId();
    private String condition = "TEST_CONDITION";
    private Optional<LocalDateTime> lastModifiedAt = Optional.of(TEST_DATE_TIME);
    private FormType formType = FormType.FORM;
    private boolean isTemplate = false;
    private FormMetadata metadata =
        new FormMetadata(
            "title",
            "description",
            "state",
            "county",
            new HashSet<ObjectId>(),
            TEST_DATE_TIME,
            new ArrayList<String>(),
            defaultNumLines);
    private FormSection child =
        new FormSection(
            "child",
            "childDescription",
            new ArrayList<FormSection>(),
            new ArrayList<FormQuestion>());
    List<FormSection> subSections = new ArrayList<>();
    private FormSection body;

    @Override
    public Form build() {
      FormQuestion question =
          new FormQuestion(
              new ObjectId(),
              FieldType.TEXT_FIELD,
              "question",
              "question",
              "question answer",
              new ArrayList<>(),
              "default",
              true,
              10,
              true,
              new ObjectId(),
              "NONE");
      List<FormQuestion> questions = new ArrayList<>();
      questions.add(question);
      subSections.add(child);
      body = new FormSection("title", "description", subSections, questions);
      Form newForm =
          new Form(
              username,
              uploaderUsername,
              uploadedAt,
              lastModifiedAt,
              formType,
              isTemplate,
              metadata,
              body,
              conditionalFieldId,
              condition);
      return newForm;
    }

    @Override
    public Form buildAndPersist(Dao<Form> dao) {
      Form form = this.build();
      dao.save(form);
      return form;
    }

    public PartialForm withUsername(String username) {
      this.username = username;
      return this;
    }

    public PartialForm withUploaderUsername(Optional<String> uploaderUsername) {
      this.uploaderUsername = uploaderUsername;
      return this;
    }

    public PartialForm withUploadedAt(LocalDateTime uploadedAt) {
      this.uploadedAt = uploadedAt;
      return this;
    }

    public PartialForm withLastModifiedAt(Optional<LocalDateTime> lastModifiedAt) {
      this.lastModifiedAt = lastModifiedAt;
      return this;
    }

    public PartialForm withFormType(FormType formType) {
      this.formType = formType;
      return this;
    }

    public PartialForm withIsTemplate(boolean isTemplate) {
      this.isTemplate = isTemplate;
      return this;
    }

    public PartialForm withMetadata(FormMetadata metadata) {
      this.metadata = metadata;
      return this;
    }

    public PartialForm withBody(FormSection body) {
      this.body = body;
      return this;
    }
  }

  public static class PartialUser implements PartialObject<User> {
    private String firstName = "testFirstName";
    private String lastName = "testLastName";
    private String birthDate = "12-14-1997";
    private String email = "testemail@keep.id";
    private String phone = "1231231234";
    private String organization = "testOrganizationName";
    private String address = "123 Test St Av";
    private String city = "Philadelphia";
    private String state = "PA";
    private String zipcode = "19104";
    private String username = "testUser123";
    private String password = "testUser123";
    private HashMap<String, String> defaultIds = new HashMap<String, String>();
    private UserType userType = UserType.Client;
    private boolean twoFactorOn = false;
    private Date creationDate = new Date(TEST_DATE);
    private List<IpObject> logInHistory = new ArrayList<>();
    private List<String> assignedWorkerUsernames = new ArrayList<>();

    @Override
    public User build() {
      try {
        User newUser =
            new User(
                firstName,
                lastName,
                birthDate,
                email,
                phone,
                organization,
                address,
                city,
                state,
                zipcode,
                twoFactorOn,
                username,
                password,
                userType);
        newUser.setLogInHistory(logInHistory);
        newUser.setCreationDate(creationDate);
        newUser.setAssignedWorkerUsernames(assignedWorkerUsernames);
        return newUser;
      } catch (ValidationException e) {
        throw new IllegalArgumentException("Illegal Param: " + e.toString());
      }
    }

    @Override
    public User buildAndPersist(Dao<User> dao) {
      User user = this.build();
      dao.save(user);
      return user;
    }

    public PartialUser withFirstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public PartialUser withLastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public PartialUser withBirthDate(String birthDate) {
      this.birthDate = birthDate;
      return this;
    }

    public PartialUser withEmail(String email) {
      this.email = email;
      return this;
    }

    public PartialUser withPhoneNumber(String phoneNumber) {
      this.phone = phoneNumber;
      return this;
    }

    public PartialUser withOrgName(String orgName) {
      this.organization = orgName;
      return this;
    }

    public PartialUser withAddress(String address) {
      this.address = address;
      return this;
    }

    public PartialUser withCity(String city) {
      this.city = city;
      return this;
    }

    public PartialUser withState(String state) {
      this.state = state;
      return this;
    }

    public PartialUser withZipcode(String zipcode) {
      this.zipcode = zipcode;
      return this;
    }

    public PartialUser withUsername(String username) {
      this.username = username;
      return this;
    }

    public PartialUser withPassword(String password) {
      this.password = password;
      return this;
    }

    public PartialUser withPasswordToHash(String password) {
      this.password = SecurityUtils.hashPassword(password);
      return this;
    }

    public PartialUser withUserType(UserType userType) {
      this.userType = userType;
      return this;
    }

    public PartialUser withTwoFactorState(boolean isTwoFactorOn) {
      this.twoFactorOn = isTwoFactorOn;
      return this;
    }

    public PartialUser withCreationDate(Date creationDate) {
      this.creationDate = creationDate;
      return this;
    }

    public PartialUser withLoginHistory(List<IpObject> logInHistory) {
      this.logInHistory = logInHistory;
      return this;
    }

    public PartialUser withDefaultId(String category, String id) {
      this.defaultIds.put(category, id);
      return this;
    }

    public PartialUser withAssignedWorker(String assignedWorkerUsername) {
      this.assignedWorkerUsernames.add(assignedWorkerUsername);
      return this;
    }
  }

  public static class PartialOrganization implements PartialObject<Organization> {
    private String orgName = "testOrganizationName";
    private String orgWebsite = "https://www.example.org/somethinghere";
    private String orgEIN = "123456789";
    private String orgStreetAddress = "311 Broad Street";
    private String orgCity = "Philadelphia";
    private String orgState = "PA";
    private String orgZipcode = "19104";
    private String orgEmail = "testOrgEmail@keep.id";
    private String orgPhoneNumber = "1234567890";
    private Date creationDate = new Date(TEST_DATE);

    @Override
    public Organization build() {
      try {
        Organization newOrg =
            new Organization(
                orgName,
                orgWebsite,
                orgEIN,
                orgStreetAddress,
                orgCity,
                orgState,
                orgZipcode,
                orgEmail,
                orgPhoneNumber);
        newOrg.setCreationDate(creationDate);
        return newOrg;
      } catch (ValidationException e) {
        throw new IllegalArgumentException("Illegal Param: " + e.toString());
      }
    }

    @Override
    public Organization buildAndPersist(Dao<Organization> dao) {
      Organization organization = this.build();
      dao.save(organization);
      return organization;
    }

    public PartialOrganization withAddress(String address) {
      this.orgStreetAddress = address;
      return this;
    }

    public PartialOrganization withCity(String city) {
      this.orgCity = city;
      return this;
    }

    public PartialOrganization withState(String state) {
      this.orgState = state;
      return this;
    }

    public PartialOrganization withZipcode(String zipcode) {
      this.orgZipcode = zipcode;
      return this;
    }

    public PartialOrganization withEmail(String email) {
      this.orgEmail = email;
      return this;
    }

    public PartialOrganization withEIN(String ein) {
      this.orgEIN = ein;
      return this;
    }

    public PartialOrganization withOrgName(String orgName) {
      this.orgName = orgName;
      return this;
    }

    public PartialOrganization withWebsite(String website) {
      this.orgWebsite = website;
      return this;
    }

    public PartialOrganization withPhoneNumber(String phoneNumber) {
      this.orgPhoneNumber = phoneNumber;
      return this;
    }

    public PartialOrganization withCreationDate(Date creationDate) {
      this.creationDate = creationDate;
      return this;
    }
  }

  public interface PartialObject<T> {
    public T build();

    public T buildAndPersist(Dao<T> dao);
  }

  public static class PartialTokens implements PartialObject<Tokens> {
    private ObjectId id = new ObjectId();
    private String username = "testUser123";
    private String resetJwt =
        SecurityUtils.createJWT(
            id.toString(), "KeepID", username, "Password Reset Confirmation", 72000000);
    private String twoFactorCode = "444555";
    private Date twoFactorExp = new Date(Long.valueOf("3786930000000"));

    @Override
    public Tokens build() {
      Tokens newTokens =
          new Tokens()
              .setId(id)
              .setUsername(username)
              .setResetJwt(resetJwt)
              .setTwoFactorCode(twoFactorCode)
              .setTwoFactorExp(twoFactorExp);
      return newTokens;
    }

    @Override
    public Tokens buildAndPersist(Dao<Tokens> dao) {
      Tokens tokens = this.build();
      dao.save(tokens);
      return tokens;
    }

    public PartialTokens withId(ObjectId id) {
      this.id = id;
      return this;
    }

    public PartialTokens withUsername(String username) {
      this.username = username;
      return this;
    }

    public PartialTokens withResetJwt(String resetJwt) {
      this.resetJwt = resetJwt;
      return this;
    }

    public PartialTokens withTwoFactorCode(String twoFactorCode) {
      this.twoFactorExp = twoFactorExp;
      return this;
    }

    public PartialTokens withTwoFactorExp(Date twoFactorExp) {
      this.twoFactorExp = twoFactorExp;
      return this;
    }
  }

  public static class PartialActivity implements PartialObject<Activity> {
    private ObjectId id = new ObjectId();
    private LocalDateTime occurredAt = LocalDateTime.of(2022, 9, 4, 12, 12, 12);
    private String username = "exampleUsername";
    private List<String> type = Collections.emptyList();

    @Override
    public Activity build() {
      Activity newActivity = new Activity().setOccurredAt(occurredAt).setInvokerUsername(username);
      if (!type.isEmpty()) {
        newActivity.setType(type);
      }
      return newActivity;
    }

    @Override
    public Activity buildAndPersist(Dao<Activity> dao) {
      Activity activity = this.build();
      dao.save(activity);
      return activity;
    }

    public PartialActivity withId(ObjectId id) {
      this.id = id;
      return this;
    }

    public PartialActivity withOccurredAt(LocalDateTime occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    public PartialActivity withUsername(String username) {
      this.username = username;
      return this;
    }

    public PartialActivity withType(List<String> type) {
      this.type = type;
      return this;
    }

    public PartialActivity withType(Activity activity) {
      this.type = activity.getType();
      return this;
    }
  }
}

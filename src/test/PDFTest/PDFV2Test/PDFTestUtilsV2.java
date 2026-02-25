package PDFTest.PDFV2Test;

import static org.junit.Assert.assertEquals;

import Config.Message;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.User.UserDao;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import PDF.Services.V2Services.GetQuestionsPDFServiceV2;
import PDF.Services.V2Services.UploadAnnotatedPDFServiceV2;
import PDF.Services.V2Services.UploadPDFServiceV2;
import PDF.Services.V2Services.UploadSignedPDFServiceV2;
import Security.EncryptionController;
import User.Address;
import User.Name;
import User.User;
import User.UserType;
import Validation.ValidationException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

public class PDFTestUtilsV2 {
  public static String resourcesFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources";

  public static ObjectId uploadBlankSSFormAndGetFileId(
      FileDao fileDao,
      FormDao formDao,
      UserDao userDao,
      UserParams developerUserParams,
      FileParams blankFileParams,
      EncryptionController encryptionController) {
    try {
      userDao.save(
          new User(
              new Name("testFirstName", "testLastName"),
              "12-12-2012",
              "testemail@keep.id",
              "2652623333",
              "org0",
              new Address("1 Keep Ave", "Keep", "PA", "11111"),
              false,
              "dev1",
              "devPass123",
              UserType.Developer));
    } catch (ValidationException e) {
      throw new RuntimeException(e);
    }
    UploadAnnotatedPDFServiceV2 uploadService =
        new UploadAnnotatedPDFServiceV2(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    Message uploadResponse = uploadService.executeAndGetResponse();
    assertEquals("uploadBlankSSForm failed: " + uploadResponse, PdfMessage.SUCCESS, uploadResponse);
    return uploadService.getUploadedFileId();
  }

  public static JSONObject getQuestionsSSForm(
      FormDao formDao, UserDao userDao, UserParams clientUserParams, ObjectId SSFileId) {
    FileParams getQuestionsFileParams = new FileParams().setFileId(SSFileId.toString());
    GetQuestionsPDFServiceV2 getService =
        new GetQuestionsPDFServiceV2(formDao, userDao, clientUserParams, getQuestionsFileParams);
    Message getQuestionsResponse = getService.executeAndGetResponse();
    assertEquals(
        "getQuestionsSSForm failed: " + getQuestionsResponse,
        PdfMessage.SUCCESS,
        getQuestionsResponse);
    return getService.getApplicationInformation();
  }

  public static List<String> getFieldNamesFromFields(JSONArray fields) {
    List<String> fieldNames = new LinkedList<>();
    for (int i = 0; i < fields.length(); i++) {
      JSONObject field = fields.getJSONObject(i);
      if (field.has("fieldName")) {
        fieldNames.add(field.getString("fieldName"));
      }
    }
    return fieldNames;
  }

  public static JSONObject getSampleFormAnswersFromSSFormQuestions(JSONObject formQuestions) {
    JSONObject formAnswers = new JSONObject();
    JSONArray fields = formQuestions.getJSONArray("fields");
    for (int i = 0; i < fields.length(); i++) {
      JSONObject field = fields.getJSONObject(i);
      if (field.getString("fieldType").equals("textField")) {
        if (field.getString("fieldName").equals("currentdate_af_date")) {
          formAnswers.put(field.getString("fieldName"), "7/14/20");
        } else {
          formAnswers.put(field.getString("fieldName"), "1");
        }
      } else if ((field.getString("fieldType").equals("checkBox"))) {
        if (field.getString("fieldName").equals("Ribeye Steaks")) {
          formAnswers.put(field.getString("fieldName"), false);
        } else {
          formAnswers.put(field.getString("fieldName"), true);
        }
      } else if ((field.getString("fieldType").equals("pushButton"))) {
        formAnswers.put(field.getString("fieldName"), true);
      } else if ((field.getString("fieldType").equals("radioButton"))) {
        formAnswers.put(field.getString("fieldName"), "Yes");
      } else if ((field.getString("fieldType").equals("comboBox"))) {
        formAnswers.put(field.getString("fieldName"), "Choice2");
      } else if ((field.getString("fieldType").equals("listBox"))) {
        JSONArray l = new JSONArray();
        l.put("Choice2");
        formAnswers.put(field.getString("fieldName"), l);
      } else if ((field.getString("fieldType").equals("Signature"))) {
        formAnswers.put(field.getString("fieldName"), new PDSignature());
      }
    }
    return formAnswers;
  }

  public static ObjectId uploadAnnotatedSSFormAndGetFileId(
      FileDao fileDao,
      FormDao formDao,
      ActivityDao activityDao,
      UserParams clientUserParams,
      EncryptionController encryptionController,
      InputStream signatureStream,
      JSONObject formAnswers,
      ObjectId uploadedFileId) {
    FileParams fillFileParams =
        new FileParams()
            .setFileId(uploadedFileId.toString())
            .setFormAnswers(formAnswers)
            .setSignatureStream(signatureStream);
    UploadSignedPDFServiceV2 uploadService =
        new UploadSignedPDFServiceV2(
            fileDao, formDao, activityDao, clientUserParams, fillFileParams, encryptionController);
    Message uploadResponse = uploadService.executeAndGetResponse();
    assertEquals(
        "uploadAnnotatedSSForm failed: " + uploadResponse,
        PdfMessage.SUCCESS,
        uploadResponse);
    return uploadService.getFilledFileObjectId();
  }

  public static void uploadSixTestStreams(
      FileDao fileDao,
      FormDao formDao,
      ActivityDao activityDao,
      UserDao userDao,
      InputStream signatureStream,
      UserParams clientOneUserParams,
      UserParams developerUserParams,
      FileParams blankOneFileParams,
      FileParams blankTwoFileParams,
      FileParams uploadFileOneFileParams,
      FileParams uploadFileTwoFileParams,
      FileParams uploadFileThreeFileParams,
      EncryptionController encryptionController) {
    UploadPDFServiceV2 uploadOneService =
        new UploadPDFServiceV2(
            fileDao, clientOneUserParams, uploadFileOneFileParams, encryptionController);
    uploadOneService.executeAndGetResponse();
    UploadPDFServiceV2 uploadTwoService =
        new UploadPDFServiceV2(
            fileDao, clientOneUserParams, uploadFileTwoFileParams, encryptionController);
    uploadTwoService.executeAndGetResponse();
    UploadPDFServiceV2 uploadThreeService =
        new UploadPDFServiceV2(
            fileDao, clientOneUserParams, uploadFileThreeFileParams, encryptionController);
    uploadThreeService.executeAndGetResponse();
    ObjectId blankOneFileObjectId =
        uploadBlankSSFormAndGetFileId(
            fileDao,
            formDao,
            userDao,
            developerUserParams,
            blankOneFileParams,
            encryptionController);

    UploadAnnotatedPDFServiceV2 uploadBlankTwoService =
        new UploadAnnotatedPDFServiceV2(
            fileDao,
            formDao,
            userDao,
            developerUserParams,
            blankTwoFileParams,
            encryptionController);
    uploadBlankTwoService.executeAndGetResponse();

    JSONObject formQuestions =
        getQuestionsSSForm(formDao, userDao, clientOneUserParams, blankOneFileObjectId);
    JSONObject formAnswers = getSampleFormAnswersFromSSFormQuestions(formQuestions);
    uploadAnnotatedSSFormAndGetFileId(
        fileDao,
        formDao,
        activityDao,
        clientOneUserParams,
        encryptionController,
        signatureStream,
        formAnswers,
        blankOneFileObjectId);
  }
}

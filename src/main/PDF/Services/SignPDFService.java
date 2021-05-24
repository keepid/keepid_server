package PDF.Services;

import Config.Message;
import Config.Service;
import PDF.PDFType;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class SignPDFService implements Service {
  public static final int CHUNK_SIZE_BYTES = 100000;

  String uploader;
  String organizationName;
  UserType privilegeLevel;
  String filename;
  String fileContentType;
  InputStream fileStream;
  InputStream signatureFileStream;
  InputStream signedPDF;
  PDFType pdfType;
  MongoDatabase db;
  EncryptionController encryptionController;

  public SignPDFService(
      MongoDatabase db,
      String uploaderUsername,
      String organizationName,
      UserType privilegeLevel,
      PDFType pdfType,
      String filename,
      String fileContentType,
      InputStream fileStream,
      InputStream signatureFileStream,
      EncryptionController encryptionController) {
    this.db = db;
    this.uploader = uploaderUsername;
    this.organizationName = organizationName;
    this.privilegeLevel = privilegeLevel;
    this.pdfType = pdfType;
    this.filename = filename;
    this.fileContentType = fileContentType;
    this.fileStream = fileStream;
    this.signatureFileStream = signatureFileStream;
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    if (pdfType == null) {
      return PdfMessage.INVALID_PDF_TYPE;
    } else if (fileStream == null) {
      return PdfMessage.INVALID_PDF;
    } else if (!fileContentType.equals("application/pdf")
        && !fileContentType.equals("application/octet-stream")) {
      return PdfMessage.INVALID_PDF;
    } else {
      if ((pdfType == PDFType.APPLICATION
              || pdfType == PDFType.IDENTIFICATION
              || pdfType == PDFType.FORM)
          && (privilegeLevel == UserType.Client
              || privilegeLevel == UserType.Worker
              || privilegeLevel == UserType.Director
              || privilegeLevel == UserType.Admin)) {
        try {
          signPDF(uploader, fileStream, signatureFileStream);
          return PdfMessage.SUCCESS;
        } catch (IOException e) {
          return PdfMessage.ENCRYPTION_ERROR;
        }
      } else {
        return PdfMessage.INSUFFICIENT_PRIVILEGE;
      }
    }
  }

  public InputStream getSignedPDF() {
    Objects.requireNonNull(signedPDF);
    return signedPDF;
  }

  public void signPDF(String username, InputStream pdfInputStream, InputStream imageInputStream)
      throws IOException {
    PDDocument pdfDocument = PDDocument.load(pdfInputStream);

    PDVisibleSignDesigner visibleSignDesigner = new PDVisibleSignDesigner(imageInputStream);
    visibleSignDesigner.zoom(0);
    PDVisibleSigProperties visibleSigProperties =
        new PDVisibleSigProperties()
            .visualSignEnabled(true)
            .setPdVisibleSignature(visibleSignDesigner);
    visibleSigProperties.buildSignature();

    SignatureOptions signatureOptions = new SignatureOptions();
    signatureOptions.setVisualSignature(visibleSigProperties.getVisibleSignature());

    PDSignature signature = new PDSignature();
    signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
    signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
    signature.setName(username);
    signature.setSignDate(Calendar.getInstance());

    for (PDSignatureField signatureField : findSignatureFields(pdfDocument)) {
      signatureField.setValue(signature);
    }

    pdfDocument.addSignature(signature, signatureOptions);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    pdfDocument.save(outputStream);
    pdfDocument.close();

    signedPDF = new ByteArrayInputStream(outputStream.toByteArray());
  }

  // Make it so that it can handle different signers
  public static List<PDSignatureField> findSignatureFields(PDDocument pdfDocument) {
    List<PDSignatureField> signatureFields = new LinkedList<>();
    List<PDField> fields = new LinkedList<>();
    fields.addAll(pdfDocument.getDocumentCatalog().getAcroForm().getFields());
    while (!fields.isEmpty()) {
      PDField field = fields.get(0);
      if (field instanceof PDNonTerminalField) {
        List<PDField> childrenFields = ((PDNonTerminalField) field).getChildren();
        fields.addAll(childrenFields);
      } else {
        if (field instanceof PDSignatureField) {
          signatureFields.add((PDSignatureField) field);
        }
      }

      // Remove field just gotten so we do not get it again
      fields.remove(0);
    }
    return signatureFields;
  }
}

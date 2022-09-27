package PDFTest;

import Config.Message;
import PDF.PdfMessage;
import PDF.Services.CrudServices.ImageToPDFService;
import TestUtils.TestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static PDFTest.PDFTestUtils.resourcesFolderPath;
import static TestUtils.TestUtils.assertPDFEquals;
import static org.junit.Assert.assertEquals;

public class ImageToPdfServiceUnitTests {
    @BeforeClass
    public static void startServer() {
        TestUtils.startServer();
    }

    @AfterClass
    public static void teardown() {
        TestUtils.tearDownTestDB();
    }

    @Test
    public void testValidUnrotatedPNGImageToPDF() throws IOException {
        File inputFile = new File(resourcesFolderPath + File.separator + "1.png");
        File expectedOutputFile = new File(resourcesFolderPath + File.separator + "1_converted.pdf");
        InputStream expectedOutputFileStream = FileUtils.openInputStream(expectedOutputFile);

        InputStream fileInputStream = FileUtils.openInputStream(inputFile);
        ImageToPDFService imageToPDFService = new ImageToPDFService(fileInputStream);
        Message response = imageToPDFService.executeAndGetResponse();
        InputStream convertedPDFInputStream = imageToPDFService.getFileStream();

        assertEquals(PdfMessage.SUCCESS, response);
        assertPDFEquals(expectedOutputFileStream, convertedPDFInputStream);
    }

    @Test
    public void testValidRotatedPNGImageToPDF() throws IOException {
        File inputFile = new File(resourcesFolderPath + File.separator + "OFW_ID_Card_sample.png");
        File expectedOutputFile = new File(resourcesFolderPath + File.separator + "OFW_ID_Card_sample_converted.pdf");
        InputStream expectedOutputFileStream = FileUtils.openInputStream(expectedOutputFile);

        InputStream fileInputStream = FileUtils.openInputStream(inputFile);
        ImageToPDFService imageToPDFService = new ImageToPDFService(fileInputStream);
        Message response = imageToPDFService.executeAndGetResponse();
        InputStream convertedPDFInputStream = imageToPDFService.getFileStream();

        assertEquals(PdfMessage.SUCCESS, response);
        assertPDFEquals(expectedOutputFileStream, convertedPDFInputStream);
    }

    @Test
    public void testValidUnrotatedJPEGImageToPDF() throws IOException {
        File inputFile = new File(resourcesFolderPath + File.separator + "icon-vector-check-mark.jpg");
        File expectedOutputFile = new File(resourcesFolderPath + File.separator + "icon-vector-check-mark_converted.pdf");
        InputStream expectedOutputFileStream = FileUtils.openInputStream(expectedOutputFile);

        InputStream fileInputStream = FileUtils.openInputStream(inputFile);
        ImageToPDFService imageToPDFService = new ImageToPDFService(fileInputStream);
        Message response = imageToPDFService.executeAndGetResponse();
        InputStream convertedPDFInputStream = imageToPDFService.getFileStream();

        assertEquals(PdfMessage.SUCCESS, response);
        assertPDFEquals(expectedOutputFileStream, convertedPDFInputStream);
    }

    @Test
    public void testValidRotatedJPEGImageToPDF() throws IOException {
        File inputFile = new File(resourcesFolderPath + File.separator + "veteran-id-card-vic.jpg");
        File expectedOutputFile = new File(resourcesFolderPath + File.separator + "veteran-id-card-vic_converted.pdf");
        InputStream expectedOutputFileStream = FileUtils.openInputStream(expectedOutputFile);

        InputStream fileInputStream = FileUtils.openInputStream(inputFile);
        ImageToPDFService imageToPDFService = new ImageToPDFService(fileInputStream);
        Message response = imageToPDFService.executeAndGetResponse();
        InputStream convertedPDFInputStream = imageToPDFService.getFileStream();

        assertEquals(PdfMessage.SUCCESS, response);
        assertPDFEquals(expectedOutputFileStream, convertedPDFInputStream);
    }

    @Test
    public void testInvalidImageToPDF() throws IOException {
        File inputFile = new File(resourcesFolderPath + File.separator + "Application_for_a_Birth_Certificate.pdf");

        InputStream fileInputStream = FileUtils.openInputStream(inputFile);
        ImageToPDFService imageToPDFService = new ImageToPDFService(fileInputStream);
        Message response = imageToPDFService.executeAndGetResponse();
        InputStream convertedPDFInputStream = imageToPDFService.getFileStream();

        assertEquals(PdfMessage.INVALID_IMAGE, response);
        assertEquals(null, convertedPDFInputStream);
    }

}

package PDF.Services.CrudServices;

import Config.Message;
import Config.Service;
import PDF.PdfMessage;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageToPDFService implements Service {
    public static final int PDF_HEIGHT = 792;
    public static final int PDF_WIDTH = 612;

    InputStream fileStream;

    public ImageToPDFService(InputStream fileStream) {
        this.fileStream = fileStream;
    }

    @Override
    public Message executeAndGetResponse() {
        if (fileStream == null) {
            return PdfMessage.INVALID_PDF;
        } else {
            try {
                fileStream = convertImageToPDF(fileStream);
            } catch (IOException exception) {
                return PdfMessage.INVALID_PDF;
            }
        }
        return PdfMessage.SUCCESS;
    }

    public InputStream convertImageToPDF(InputStream fileStream) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        // Get new dimensions of rotated & scaled image
        BufferedImage bimg = ImageIO.read(fileStream);
        float imageWidth = bimg.getWidth();
        float imageHeight = bimg.getHeight();

        // Rotate Image
        if (imageWidth > imageHeight) {
            bimg = this.rotateImage(bimg);

            float tmp = imageWidth;
            imageWidth = imageHeight;
            imageHeight = tmp;
        }

        // Scale Image
        float finalWidth, finalHeight;
        if (imageWidth / PDF_WIDTH > imageHeight / PDF_HEIGHT) {
            finalWidth = PDF_WIDTH;
            finalHeight = imageHeight * PDF_WIDTH / imageWidth;
        } else {
            finalHeight = PDF_HEIGHT;
            finalWidth = imageWidth * PDF_HEIGHT / imageHeight;
        }

        PDImageXObject imageXObject = LosslessFactory.createFromImage(document, bimg);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.drawImage(imageXObject, 0, 0, finalWidth, finalHeight);
        contentStream.close();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        document.close();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        return inputStream;
    }

    // Source: https://blog.idrsolutions.com/2019/05/image-rotation-in-java/
    public BufferedImage rotateImage(BufferedImage bimg) {
        final double rads = Math.toRadians(90);
        final double sin = Math.abs(Math.sin(rads));
        final double cos = Math.abs(Math.cos(rads));
        final int w = (int) Math.floor(bimg.getWidth() * cos + bimg.getHeight() * sin);
        final int h = (int) Math.floor(bimg.getHeight() * cos + bimg.getWidth() * sin);
        final BufferedImage rotatedImage = new BufferedImage(w, h, bimg.getType());
        final AffineTransform at = new AffineTransform();
        at.translate(w / 2, h / 2);
        at.rotate(rads, 0, 0);
        at.translate(-bimg.getWidth() / 2, -bimg.getHeight() / 2);
        final AffineTransformOp rotateOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        rotateOp.filter(bimg, rotatedImage);
        return rotatedImage;
    }
}

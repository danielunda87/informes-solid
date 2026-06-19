import java.io.FileOutputStream;
import java.io.File;
import java.awt.Color;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

public class TestTemplate {
    public static void main(String[] args) {
        try {
            Document doc = new Document(PageSize.LETTER, 30, 30, 26, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream("temp/test_brand_image.pdf"));
            
            doc.open();
            
            File archivoMarca = new File("images/imagenMarca2.png");
            if (archivoMarca.exists()) {
                System.out.println("Image exists on disk!");
                Image img = Image.getInstance(archivoMarca.getAbsolutePath());
                img.scaleToFit(65, 18);
                doc.add(new Paragraph("Inserting brand image:"));
                doc.add(img);
            } else {
                System.out.println("Image NOT found!");
            }
            
            doc.close();
            System.out.println("Finished test!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

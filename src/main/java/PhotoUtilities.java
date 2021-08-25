import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PhotoUtilities {
    public static File cropImage(double amount, File sourceFile, String destinationFolder) throws IOException {
        BufferedImage originalImage = ImageIO.read(sourceFile);
        int height = originalImage.getHeight();
        int width = originalImage.getWidth();


        // Crop
        BufferedImage croppedImage = originalImage.getSubimage(
                0,
                0,
                width, // widht
                (int) (height - amount) // height
        );
        return cropImageToFile(croppedImage, destinationFolder, sourceFile);
    }

    public static File cropImageToFile(BufferedImage image, String destinationFolder, File sourceFile) throws IOException {
        File outputfile = new File(destinationFolder+"/"+sourceFile.getName());
        ImageIO.write(image, "jpg", outputfile);
        return outputfile;
    }
}

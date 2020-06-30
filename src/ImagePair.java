import java.awt.image.BufferedImage;

/** A DTO used for transporting everything necessary to generate an overlay. */
public class ImagePair {
    public final BufferedImage background, ore;
    public final String name;

    public ImagePair(String name, BufferedImage background, BufferedImage ore) {
        this.background = background;
        this.ore = ore;
        this.name = name;
    }
}
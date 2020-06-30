import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Main {

    private static final File ORES = new File("ores");
    private static final File BACKGROUNDS = new File("backgrounds");
    private static final File OUTPUT = new File("output");

    private static final String INSTRUCTIONS =
        "Place each background sprite inside of /backgrounds." +
        "Then, place ore sprites in directories of the same name in /ores.";

    public static void main(String[] args) {
        final List<ImagePair> images = getAllPairs();
        if (images.size() == 0) {
            System.out.println(INSTRUCTIONS);
        }
        mkdir(OUTPUT);

        for (ImagePair pair : images) {
            final Color[][] bg = getColors(pair.background);
            final Color[][] fg = getColors(pair.ore);
            debugImage(pair.name, bg, fg);
            for (double d = 0.0; d < 1.0; d += 0.05) {
                final String name = pair.name.replace(".png", "");
                final File dir = new File(OUTPUT, name);
                mkdir(dir);

                final File f = new File(dir, d + ".png");
                final Color[][] overlay = Extractor.primary(bg, fg, d);
                writeImage(overlay, f.getPath());
            }
        }
    }

    /** Retrieves all of the BufferedImages matched fg to bg. */
    private static List<ImagePair> getAllPairs() {
        final List<ImagePair> pairs = new ArrayList<>();
        for (File f : listFiles(ORES)) {
            final String name = f.getName();
            final FileArch files = getMatchingDirectories(name);
            final BufferedImage bgImage = loadImage(files.background.getPath())
                .orElseThrow(Main::unreachable);
            for (File ore : files.ores) {
                final BufferedImage oreImage = loadImage(ore.getPath())
                    .orElseThrow(Main::unreachable);
                pairs.add(new ImagePair(ore.getName(), bgImage, oreImage));
            }
        }
        return pairs;
    }

    private static File[] listFiles(File f) {
        mkdir(f);
        return f.listFiles();
    }

    private static void mkdir(File f) {
        if (!(f.exists() || f.mkdirs())) {
            throw new RuntimeException("Error creating directory: " + f);
        }
    }

    /** Ensures that a bg file and ore directory exists with the same name in each input folder. */
    private static FileArch getMatchingDirectories(String name) {
        final File ores = new File(ORES, name);
        final File bg = new File(BACKGROUNDS, name + ".png");
        if (!(ores.exists() && ores.isDirectory() && bg.exists())) {
            throw new RuntimeException("Expected matching directories and files in /ores and /backgrounds.");
        }
        return new FileArch(bg, ores);
    }

    /** Attempts to load an image file. */
    private static Optional<BufferedImage> loadImage(String path) {
        Optional<InputStream> is = locateResource(path);
        if (is.isPresent()) {
            try {
                return Optional.of(ImageIO.read(is.get()));
            } catch (IOException ignored) {}
        }
        return Optional.empty();
    }

    /** Retrieves a resource in or outside of the jar file. */
    private static Optional<InputStream> locateResource(String path) {
        final InputStream relative = Main.class.getResourceAsStream(path);
        if (relative != null) {
            return Optional.of(relative);
        }
        try {
            return Optional.of(new FileInputStream(new File(path)));
        } catch (FileNotFoundException e) {
            return Optional.empty();
        }
    }

    private static RuntimeException unreachable() {
        return new RuntimeException("unreachable");
    }

    /** Reuses any original .mcmeta files for all overlay variants. */
    private static void handleMcMeta(Set<FileSpec> files, String forImage, String... paths) {
        locateResource(forImage + ".mcmeta").ifPresent(mcmeta -> {
            for (String path : paths) {
                files.add(new FileSpec(mcmeta, path + ".mcmeta"));
            }
        });
    }

    /** Generates a matrix of colors from the input BufferedImage. */
    private static Color[][] getColors(BufferedImage image) {
        final int w = image.getWidth(), h = image.getHeight();
        final Color[][] colors = new Color[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                colors[x][y] = new Color(image.getRGB(x, y), true);
            }
        }
        return colors;
    }

    /** Generates a BufferedImage from the input color matrix. */
    private static BufferedImage getImage(Color[][] image) {
        final int w = image.length, h = image[0].length;
        final BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                bi.setRGB(x, y, image[x][y].getRGB());
            }
        }
        return bi;
    }

    /** Generates a faux InputStream from the input color matrix. */
    private static InputStream getStream(Color[][] image) {
        BufferOutputStream os = new BufferOutputStream();
        try {
            ImageIO.write(getImage(image), "png", os);
        } catch (IOException e) {
            throw new RuntimeException("Unable to generate faux InputStream from color matrix", e);
        }
        return os.toInputStream();
    }

    /** Writes a new image to the disk. */
    private static void writeImage(Color[][] image, String path) {
        try {
            final OutputStream os = new FileOutputStream(path);
            ImageIO.write(getImage(image), "png", os);
        } catch (IOException e) {
            throw new RuntimeException("Error writing image: " + path, e);
        }
    }

    /** Returns a clone of the input color matrix. */
    private static Color[][] cloneColors(Color[][] colors) {
        final int w = colors.length, h = colors[0].length;
        final Color[][] newColors = new Color[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                newColors[x][y] = colors[x][y];
            }
        }
        return newColors;
    }

    /** Scales the background to the width of the foreground, repeating it for additional frames. */
    private static Color[][] ensureSizeParity(Color[][] background, Color[][] foreground) {
        final int w = foreground.length, h = foreground[0].length;
        background = getColors(ImageTools.scale(getImage(background), w, h));
        background = ImageTools.addFramesToBackground(background, foreground);
        return background;
    }

    private static void debugImage(String path, Color[][] bg, Color[][] fg) {
        final double diffAB = ImageTools.getAverageDifference(bg, fg);
        final double diffA = ImageTools.getAverageDifference(fg);
        final double diffB = ImageTools.getAverageDifference(bg);
        final double diffD = Math.abs(diffAB - diffA);
        final double diffU = Math.abs(diffA - diffB);
        final double sumU = diffA + diffB;
        final double diffS = Math.abs(diffAB - sumU);
        final double thrsA = Extractor.AVG_DIFF_RATIO * diffAB;
        final double multD = thrsA / diffD;
        final double multU = thrsA / diffU;
        final double multS = thrsA / diffS;

        System.out.println("name:        " + path);
        System.out.println("diff(fg/bg): " + diffAB);
        System.out.println("diff(fg):    " + diffA);
        System.out.println("diff(bg):    " + diffB);
        System.out.println("diffD:       " + diffD);
        System.out.println("diffU:       " + diffU);
        System.out.println("diffS:       " + diffS);
        System.out.println("multD:       " + multD);
        System.out.println("multU:       " + multU);
        System.out.println("multS:       " + multS);
        System.out.println();
    }

    /** For all functions directly related to producing an overlay. */
    private static class Extractor {
        /**
         * The average difference between two textures and their optimal
         * selection threshold are highly correlated (r = 0.9230). This
         * ratio is used to more accurately determine which pixels in a
         * texture belong to the actual ore and not its background.
         */
        private static final double AVG_DIFF_RATIO = 2.7;
        /** The location of the the vignette mask. */
        private static final String MASK_LOCATION =  "mask.png";
        /** The mask used for removing edge pixels from larger textures. */
        private static final BufferedImage VIGNETTE_MASK = loadImage(MASK_LOCATION)
                .orElseThrow(() -> new RuntimeException("Build error: mask path is invalid."));

        /**
         * Uses the average color of the background texture and the average
         * difference between each image to determine a difference threshold
         * used for retaining select pixels from the foreground. Produces an
         * overlay which ideally containing only the ore pixels from the
         * original foreground texture.
         */
        private static Color[][] primary( Color[][] background, Color[][] foreground, double threshold) {
            final int w = foreground.length, h = foreground[0].length;
            final Color[][] overlay = new Color[w][h];

            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    overlay[x][y] = ImageTools.getAdjustedOrePixel(background[x][y], foreground[x][y], threshold);
                }
            }
            return overlay;
        }

        private static Color[][] getEdges(Color[][] foreground) {
            final float[] edgeKernel = {
                0.0f, -1.0f, 0.0f,
                -1.0f, 5.0f, -1.0f,
                0.0f, -1.0f, 0.0f
            };
            BufferedImageOp edge = new ConvolveOp(
                new Kernel(3, 3, edgeKernel),
                ConvolveOp.EDGE_ZERO_FILL,
                null
            );
            return getColors(edge.filter(getImage(foreground), null));
        }

        /**
         * Variant of primary() which applies shading to push and
         * pull the background texture, matching the original ore sprite.
         */
        private static Color[][] shade(Color[][] overlay, Color[][] background, Color[][] foreground) {
            final Color[][] mask = ensureSizeParity(getColors(VIGNETTE_MASK), foreground);
            background = ensureSizeParity(background, foreground);
            // Again, I forget why only one color was used here.
            background = ImageTools.fillColors(background, ImageTools.getAverageColor(background));
            Color[][] texturePixels = ImageTools.convertToPushAndPull(background, foreground);
            texturePixels = ImageTools.removePixels(texturePixels, mask);
            return ImageTools.overlay(texturePixels, overlay);
        }
    }
}
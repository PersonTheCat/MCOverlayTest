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
        "Place each background sprite inside of /backgrounds.\n" +
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
            generate(pair, bg, fg);
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
    public static Optional<BufferedImage> loadImage(String path) {
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
    public static Color[][] getColors(BufferedImage image) {
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
    public static BufferedImage getImage(Color[][] image) {
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

    private static void debugImage(String path, Color[][] bg, Color[][] fg) {
        final double diffAB = ImageTools.getAverageDistance(bg, fg);
        final double diffA = ImageTools.getAverageDistance(fg);
        final double diffB = ImageTools.getAverageDistance(bg);
        final double diffD = Math.abs(diffAB - diffA);
        final double diffE = Math.abs(diffAB - diffB);
        final double diffU = Math.abs(diffA - diffB);
        final double diffW = diffAB / diffU;
        final double sumU = diffA + diffB;
        final double diffS = Math.abs(diffAB - sumU);
        final double thrsA = 2.3 * diffAB;
        final double multD = thrsA / diffD;
        final double multU = thrsA / diffU;
        final double multS = thrsA / diffS;
        final double avgDst = ImageTools.getAverageDistance(bg, fg);
        final double maxDst = ImageTools.getMaxDistance(bg, fg);
        final double ratDst = maxDst / avgDst;
        final double avgDstBg = ImageTools.getAverageDistance(bg);
        final double maxDist = ImageTools.getMaxDistance(bg, fg);
        final double maxRel = ImageTools.getMaxRelDist(bg, fg);

        System.out.println("name:        " + path);
        System.out.println("diff(fg/bg): " + diffAB);
        System.out.println("diff(fg):    " + diffA);
        System.out.println("diff(bg):    " + diffB);
        System.out.println("diffD:       " + diffD);
        System.out.println("diffE:       " + diffE);
        System.out.println("diffU:       " + diffU);
        System.out.println("diffS:       " + diffS);
        System.out.println("diffW:       " + diffW);
        System.out.println("multD:       " + multD);
        System.out.println("multU:       " + multU);
        System.out.println("multS:       " + multS);
        System.out.println("avgDst:      " + avgDst);
        System.out.println("maxDst:      " + maxDst);
        System.out.println("ratDst:      " + ratDst);
        System.out.println("avgDstBg:    " + avgDstBg);
        System.out.println("max rel:     " + maxRel);
        System.out.println("max std:     " + maxDist);
        System.out.println();
    }

    private static void generateLeveled(ImagePair pair, Color[][] bg, Color[][] fg) {
        for (double d = 0.0; d < 3.0; d += 0.05) {
            final String name = pair.name.replace(".png", "");
            final File dir = new File(OUTPUT, name);
            mkdir(dir);

            final File f = new File(dir, d + ".png");
            final Color[][] overlay = ImageTools.getOverlayManual(bg, fg, d);
            writeImage(overlay, f.getPath());
        }
    }

    private static void generate(ImagePair pair, Color[][] bg, Color[][] fg) {
        final File f = new File(OUTPUT, pair.name);
        final Color[][] overlay = ImageTools.getOverlay(bg, fg);
        writeImage(overlay, f.getPath());
        final Color[][] shaded = ImageTools.shadeOverlay(overlay, bg, fg);
        writeImage(shaded, f.getPath().replace(".png", "_shaded.png"));
    }
}
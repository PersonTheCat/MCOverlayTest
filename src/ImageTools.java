import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageTools {
    /** Pixels with higher alpha levels are considered opaque. */
    private static final int OPACITY_THRESHOLD = 50;
    /** Pixels with lower alpha levels are considered transparent. */
    private static final int TRANSPARENCY_THRESHOLD = 17;
    /** A pixel with no color. */
    private static final Color EMPTY_PIXEL = new Color(0, 0, 0, 0);
    /** The maximum "difference" between any two pixels. */
    private static final double MAX_DIFFERENCE = 441.673;
    /** The maximum possible difference between three color channels. */
    private static final double MAX_ADJUSTMENT = 510.0;
    /** Multiplies the alpha levels for push and pull. */
    private static final double TEXTURE_SHARPEN_RATIO = 2.3;
    private static final double IN_THRESH_SCALE = 1.05;

    /** Variant of getAverageColor() which accepts a matrix. */
    public static Color getAverageColor(Color[][] image) {
        return getAverageColor(matrixToArray(image));
    }

    /** Gets the average color from an array of colors. */
    public static Color getAverageColor(Color... colors) {
        int r = 0, g = 0, b = 0;
        int count = 0;
        for (Color color : colors) {
            if (color.getAlpha() > OPACITY_THRESHOLD) {
                r += color.getRed();
                g += color.getGreen();
                b += color.getBlue();
                count++;
            }
        }
        if (count == 0) {
            return EMPTY_PIXEL;
        }
        return new Color(r / count, g / count, b / count);
    }

    /** Converts a 2D array of colors to a 1D array. */
    private static Color[] matrixToArray(Color[][] matrix) {
        final Color[] array = new Color[matrix.length * matrix[0].length];
        int index = 0;
        for (int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < matrix[0].length; y++) {
                array[index++] = matrix[x][y];
            }
        }
        return array;
    }

    /** Determines the average difference from the input color. */
    public static double getAverageDifference(Color[][] image, Color[][] from) {
        double sum = 0.0;
        int count = 0;
        for (int x = 0; x < image.length; x++) {
            for (int y = 0; y < image[0].length; y++) {
                if (image[x][y].getAlpha() > TRANSPARENCY_THRESHOLD) {
                    sum += getDifference(image[x][y], from[x][y]);
                    count++;
                }
            }
        }
        return sum / count;
    }

    /** Determines the average difference from the input color. */
    public static double getAvgAdjustedDiff(Color[][] image, Color[][] from) {
        double sum = 0.0;
        int count = 0;
        for (int x = 0; x < image.length; x++) {
            for (int y = 0; y < image[0].length; y++) {
                if (image[x][y].getAlpha() > TRANSPARENCY_THRESHOLD) {
                    sum += getDifference(image[x][y], from[x][y]);
                    sum += getAdjustment(image[x][y], from[x][y]);
                    count++;
                }
            }
        }
        return sum / count;
    }

    /** Determines the average difference from the input color. */
    public static double getAverageAdjustment(Color[][] image, Color[][] from) {
        double sum = 0.0;
        int count = 0;
        for (int x = 0; x < image.length; x++) {
            for (int y = 0; y < image[0].length; y++) {
                if (image[x][y].getAlpha() > TRANSPARENCY_THRESHOLD) {
                    sum += getAdjustment(image[x][y], from[x][y]);
                    count++;
                }
            }
        }
        return sum / count;
    }

    /** Determines the average difference from the input color. */
    public static double getAverageDifference(Color[][] image) {
        double sum = 0.0;
        for (int x = 0; x < image.length - 1; x++) {
            for (int y = 0; y < image[0].length - 1; y++) {
                sum += getDifference(image[x][y], image[x+1][y]);
                sum += getDifference(image[x][y], image[x][y+1]);
            }
        }
        return sum / (image.length * image[0].length * 2);
    }

    public static Color[][] isolateClusters(Color[][] image) {
        if (image.length < 2 || image[0].length < 2) {
            throw new UnsupportedOperationException("Image too small.");
        }
        final double threshold = 0.2;
//        final Color[][] forward = orCheck(
//            isolateRight(image, threshold),
//            isolateUp(image, threshold)
//        );
//        final Color[][] backward = orCheck(
//            isolateLeft(image, threshold),
//            isolateDown(image, threshold)
//        );
//        return andCheck(forward, backward);
        final Color[][] horizontal = andCheck(
            isolateLeft(image, threshold),
            isolateRight(image, threshold)
        );
        final Color[][] vertical = andCheck(
            isolateUp(image, threshold),
            isolateDown(image, threshold)
        );
        return orCheck(horizontal, vertical);
    }

    private static Color[][] isolateUp(Color[][] image, double threshold) {
        final Color[][] clusters = getEmptyMatrix(image.length, image[0].length);
        for (int x = 0; x < image.length; x++) {
            double comp = threshold;
            boolean inside = false;
            for (int y = 0; y < image[0].length; y++) {
                if (inside) {
                    comp *= IN_THRESH_SCALE;
                    clusters[x][y] = image[x][y];
                }
                if (x == image.length - 1 || y == image[0].length - 1) {
                    continue;
                }
                if (getDifference(image[x][y], image[x][y+1]) > comp) {
                    comp /= IN_THRESH_SCALE;
                    inside = !inside;
                }
            }
        }
        return clusters;
    }

    private static Color[][] isolateRight(Color[][] image, double threshold) {
        final Color[][] clusters = getEmptyMatrix(image.length, image[0].length);
        for (int y = 0; y < image[0].length; y++) {
            double comp = threshold;
            boolean inside = false;
            for (int x = 0; x < image.length; x++) {
                if (inside) {
                    comp *= IN_THRESH_SCALE;
                    clusters[x][y] = image[x][y];
                }
                if (x == image.length - 1 || y == image[0].length - 1) {
                    continue;
                }
                if (getDifference(image[x][y], image[x+1][y]) > comp) {
                    comp /= IN_THRESH_SCALE;
                    inside = !inside;
                }
            }
        }
        return clusters;
    }

    private static Color[][] isolateDown(Color[][] image, double threshold) {
        final Color[][] clusters = getEmptyMatrix(image.length, image[0].length);
        for (int x = image.length - 1; x >= 0; x--) {
            double comp = threshold;
            boolean inside = false;
            for (int y = image[0].length - 1; y >= 0; y--) {
                if (inside) {
                    comp *= IN_THRESH_SCALE;
                    clusters[x][y] = image[x][y];
                }
                if (x == 0 || y == 0) {
                    continue;
                }
                if (getDifference(image[x][y], image[x][y-1]) > comp) {
                    comp /= IN_THRESH_SCALE;
                    inside = !inside;
                }
            }
        }
        return clusters;
    }

    private static Color[][] isolateLeft(Color[][] image, double threshold) {
        final Color[][] clusters = getEmptyMatrix(image.length, image[0].length);
        for (int y = image[0].length - 1; y >= 0; y--) {
            double comp = threshold;
            boolean inside = false;
            for (int x = image.length - 1; x >= 0; x--) {
                if (inside) {
                    comp *= IN_THRESH_SCALE;
                    clusters[x][y] = image[x][y];
                }
                if (x == 0 || y == 0) {
                    continue;
                }
                if (getDifference(image[x][y], image[x-1][y]) > comp) {
                    comp /= IN_THRESH_SCALE;
                    inside = !inside;
                }
            }
        }
        return clusters;
    }

    private static Color[][] andCheck(Color[][] bg, Color[][] fg) {
        final Color[][] clusters = getEmptyMatrix(bg.length, bg.length);
        for (int x = 0; x < bg.length; x++) {
            for (int y = 0; y < bg[0].length; y++) {
                if (bg[x][y] != EMPTY_PIXEL && fg[x][y] != EMPTY_PIXEL) {
                    clusters[x][y] = bg[x][y];
                }
            }
        }
        return clusters;
    }

    private static Color[][] orCheck(Color[][] bg, Color[][] fg) {
        final Color[][] clusters = getEmptyMatrix(bg.length, bg.length);
        for (int x = 0; x < bg.length; x++) {
            for (int y = 0; y < bg[0].length; y++) {
                if (bg[x][y] != EMPTY_PIXEL) {
                    clusters[x][y] = bg[x][y];
                } else if (fg[x][y] != EMPTY_PIXEL) {
                    clusters[x][y] = fg[x][y];
                }
            }
        }
        return clusters;
    }

    /** Generates a blank image. */
    private static Color[][] getEmptyMatrix(int w, int h) {
        return fillColors(new Color[w][h], EMPTY_PIXEL);
    }

    public static double getAdjustment(Color background, Color foreground) {
        // Get individual differences;
        final int r = foreground.getRed() - background.getRed();
        final int g = foreground.getGreen() - background.getGreen();
        final int b = foreground.getBlue() - background.getBlue();
        // Get lowest number.
        final int baseline = getMin(getMin(r, g), b);
        // Get ratings of which channels are the most different.
        final int rS = r - baseline;
        final int gS = g - baseline;
        final int bS = b - baseline;
        // Get the distance between
        return Math.sqrt((rS * rS) + (gS * gS) + (bS * bS)) / MAX_ADJUSTMENT;
    }

    public static double getAdjustedDifference(Color background, Color foreground) {
        // Get individual differences;
        final int r = foreground.getRed() - background.getRed();
        final int g = foreground.getGreen() - background.getGreen();
        final int b = foreground.getBlue() - background.getBlue();
        // Get lowest number.
        final int baseline = getMin(getMin(r, g), b);
        // Get ratings of which channels are the most different.
        final int rS = r - baseline;
        final int gS = g - baseline;
        final int bS = b - baseline;
        // Get the distance between
        final double adjust = Math.sqrt((rS * rS) + (gS * gS) + (bS * bS)) / MAX_ADJUSTMENT;
        final double diff = Math.sqrt((r * r) + (g * g) + (b * b)) / MAX_DIFFERENCE;
        // Adjustment is substantially more significant.
        // Todo: lower avg diff -> diff more important.
        // return diff * 10.0 + adjust;
        return diff + adjust * 10.0;
    }

    public static Color getAdjustedOrePixel(Color background, Color foreground, double threshold) {
        if (getAdjustedDifference(background, foreground) > threshold) {
            return foreground;
        }
        return EMPTY_PIXEL;
    }

    /**
     * Determines whether the foreground is different enough from
     * the background. If so, returns it.
     */
    public static Color getOrePixel(Color background, Color foreground, double threshold) {
        if (getDifference(background, foreground) > threshold) {
            return foreground;
        }
        return EMPTY_PIXEL;
    }

    /** Calculates the distance between two colors. */
    public static double getDifference(Color background, Color foreground) {
        final int r = foreground.getRed() - background.getRed();
        final int g = foreground.getGreen() - background.getGreen();
        final int b = foreground.getBlue() - background.getBlue();
        return Math.sqrt((r * r) + (g * g) + (b * b)) / MAX_DIFFERENCE;
    }

    /** Fills an entire image with a single color. */
    public static Color[][] fillColors(Color[][] image, Color color) {
        for (int x = 0; x < image.length; x++) {
            for (int y = 0; y < image[0].length; y++) {
                image[x][y] = color;
            }
        }
        return image;
    }

    /** Repeats the background image until it is the height of the foreground. */
    public static Color[][] addFramesToBackground(Color[][] background, Color[][] foreground) {
        final int w = background.length, h = background[0].length, nh = foreground.length;
        final int frames = nh / h;
        final Color[][] newBackground = new Color[w][h * frames];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int i = 0; i < frames; i++) {
                    newBackground[x][(h * i) + y] = background[x][y];
                }
            }
        }
        return newBackground;
    }

    /**
     * Uses getDifference() to determine the alpha level for each pixel.
     * Uses isPixelDarker() to determine whether each pixel should be
     * black or white (push or pull).
     */
    public static Color[][] convertToPushAndPull(Color[][] background, Color[][] foreground) {
        final Color[][] image = new Color[foreground.length][foreground[0].length];
        for (int x = 0; x < foreground.length; x++) {
            for (int y = 0; y < foreground[0].length; y++) {
                int alpha = (int) (255 * getDifference(foreground[x][y], background[x][y]));
                if (alpha > 200) {
                    alpha = 200;
                } else if (alpha < 0) {
                    alpha = 0;
                }
                if (isPixelDarker(background[x][y], foreground[x][y])) {
                    image[x][y] = new Color(0, 0, 0, alpha);
                } else {
                    image[x][y] = new Color(255, 255, 255, alpha);
                }
            }
        }
        return image;
    }

    /** Determines whether the foreground is lighter than the background. */
    public static boolean isPixelDarker(Color background, Color foreground) {
        final int fgTotal = Math.abs(foreground.getRed() + foreground.getGreen() + foreground.getBlue());
        final int bgTotal = Math.abs(background.getRed() + background.getGreen() + background.getBlue());
        return fgTotal < bgTotal;
    }

    /** Uses a mask to fade pixels out of an image. */
    public static Color[][] removePixels(Color[][] image, Color[][] mask) {
        for (int x = 0; x < image.length; x++) {
            for (int y = 0; y < image[0].length; y++) {
                final int r = image[x][y].getRed();
                final int g = image[x][y].getGreen();
                final int b = image[x][y].getBlue();
                int a = (int) ((double) image[x][y].getAlpha() * (1.0 - ((double) mask[x][y].getAlpha() / 255)));
                if (a < 0) {
                    a = 0;
                } else if (a > 255) {
                    a = 255;
                }
                image[x][y] = new Color(r, g, b, a);
            }
        }
        return image;
    }

    /** Blends the foreground above the background. */
    public static Color[][] overlay(Color[][] background, Color[][] foreground) {
        for (int x = 0; x < foreground.length; x++) {
            for (int y = 0; y < foreground[0].length; y++) {
                foreground[x][y] = blendPixels(background[x][y], foreground[x][y]);
            }
        }
        return foreground;
    }

    /**
     * Gets the weighted average of each color relative to the foreground's
     * alpha level. Foreground gets alpha * its color, background gets the
     * rest * its color. The final alpha is the sum of both.
     */
    public static Color blendPixels(Color bg, Color fg) {
        final int r, g, b;
        if (fg.getAlpha() > OPACITY_THRESHOLD) {
            r = fg.getRed();
            g = fg.getGreen();
            b = fg.getBlue();
        } else {
            r = ((fg.getRed() * fg.getAlpha()) + (bg.getRed() * (255 - fg.getAlpha()))) / 255;
            g = ((fg.getGreen() * fg.getAlpha()) + (bg.getGreen() * (255 - fg.getAlpha()))) / 255;
            b = ((fg.getBlue() * fg.getAlpha()) + (bg.getBlue() * (255 - fg.getAlpha()))) / 255;
        }
        int a = fg.getAlpha() + bg.getAlpha();
        if (a < TRANSPARENCY_THRESHOLD && r == 255 && g == 255 && b == 255) {
            return EMPTY_PIXEL; // Don't keep white pixels.
        }
        a = limitRange((int) ((double) a * TEXTURE_SHARPEN_RATIO));

        return new Color(r, g, b, a);
    }

    /** Pupnewfster's original algorithm for generating dense ore sprites. */
    public static Color[][] shiftImage(Color[][] image) {
        final int w = image.length, h = image[0].length;
        final Color[][] shifted = new Color[w][h];
        final int frames = h / w;
        assert(1.0 * h / w == frames);
        for (int f = 0; f < frames; f++) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int imageY = f * w + y;
                    shifted[x][imageY] = getAverageColor(
                        image[x][imageY],
                        fromIndex(image, x - 1, imageY, f),
                        fromIndex(image, x + 1, imageY, f),
                        fromIndex(image, x, imageY - 1, f),
                        fromIndex(image, x, imageY + 1, f)
                    );
                }
            }
        }
        return shifted;
    }

    private static Color fromIndex(Color[][] image, int x, int y , int frame) {
        final int w = image.length;
        return ((x < 0) || (y < frame * w) || (x >= w) || (y >= (frame + 1) * w) || (image[x][y].getAlpha() == 34)) ?
            EMPTY_PIXEL : image[x][y];
    }

    public static BufferedImage scale(BufferedImage image, int x, int y) {
        BufferedImage scaled = new BufferedImage(x, y, image.getType());
        Graphics2D graphics = scaled.createGraphics();
        graphics.drawImage(image, 0, 0, x, y, null);
        graphics.dispose();
        return scaled;
    }

    /** Corrects the channel value if it is outside of the accepted range. */
    private static int limitRange(int channel) {
        return channel < 0 ? 0 : channel > 255 ? 255 : channel;
    }

    private static int getMin(int a, int b) {
        return a < b ? a : b;
    }
}
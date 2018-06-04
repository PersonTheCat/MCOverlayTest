package personthecat.overlay32v2;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

public class Main
{
	//Doing my testing in slop.
	public static void main(String[] args)
	{
		//Misc
		String singleColorLocation = System.getProperty("user.dir") + "/generated_color.png";
		String overlayLocation = System.getProperty("user.dir") + "/overlay.png";
		Scanner scanner = new Scanner(System.in);

		//Info
		System.out.println("In Windows, you can shift + right click a file to expose a \"copy as path\" option.\nUse that to copy and paste your images here.\n");

		//Background
		String backgroundLocation = ProgramUsageSimplifier.getBackgroundLocation(scanner);
		BufferedImage originalBackground = getImageFromFile(backgroundLocation);	
		Color[][] backgroundColors = getColorsFromImage(originalBackground);
		
		//Blended overlay
		boolean fancyOverlay = ProgramUsageSimplifier.useFancyTextures(scanner);
		
		System.out.println();
		
		List<String> textureLocations = ProgramUsageSimplifier.getOreTextureLocations(scanner);
		
		scanner.close();
		
		for (String path : textureLocations)
		{			
			if (textureLocations.size() > 1)
			{
				System.out.println("current texture: " + new File(path).getName());
				
				overlayLocation = path.replaceAll("ore_textures", "output").replaceAll(".png", "") + "_overlay.png";
			}
			
			BufferedImage oreTexture = getImageFromFile(path);
			Color[][] oreColors = getColorsFromImage(oreTexture);
			
			BufferedImage newBackground = IMGTools.scaleImage(createImageFromColors(IMGTools.fillColors(backgroundColors, IMGTools.getAverageColor(backgroundColors))), oreColors.length, oreColors.length);
			backgroundColors = getColorsFromImage(newBackground);

			//Create overlay
			BufferedImage overlay = null;
			
			if (fancyOverlay) overlay = createImageFromColors(OverlayExtractor.extractBlendedOverlay(backgroundColors, oreColors));
			else overlay = createImageFromColors(OverlayExtractor.extractNormalOverlay(backgroundColors, oreColors));
			
			IMGTools.getPixelClusters(getColorsFromImage(overlay));
			
			//Write files
			writeImageToFile(newBackground, singleColorLocation);
			writeImageToFile(overlay, overlayLocation);
			
			System.out.println();
		}
	}
	
	/**
	 * To-do improvements (?): 
	 * 
	 *     * Have color matches return an array of colors to test instead of finding the most common
	 *       match. A percent-based number of matches may be useful (?) for determining which matches
	 *       are valid. <- could still be problematic for Conquest textures, which tend to have varying
	 *       shades (and distinct colors) in the background of the ore texture.
	 */
	
	private static BufferedImage getImageFromFile(String input)
	{
		BufferedImage bufferedImage = null;

		try 
		{
			bufferedImage = ImageIO.read(new File(input));
		} 
		
		catch (IOException e) {System.err.println("Error: Could not load image " + input);}

		return bufferedImage;
	}
	
	private static Color[][] getColorsFromImage(BufferedImage image)
	{
		int w = image.getWidth(), h = image.getHeight();
		
		Color[][] colors = new Color[w][h];
		
		for (int i = 0; i < w; i++)
		{
			for (int j = 0; j < h; j++)
			{
				colors[i][j] = new Color(image.getRGB(i, j), true);
			}
		}
		
		return colors;
	}
	
	private static BufferedImage createImageFromColors(Color[][] image)
	{
		int w = image.length, h = image[0].length;
		
		BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		
		for (int i = 0; i < w; i++)
		{
			for (int j = 0; j < h; j++)
			{
				bufferedImage.setRGB(i, j, image[i][j].getRGB());
			}
		}
		
		return bufferedImage;
	}
	
	private static void writeImageToFile(BufferedImage image, String location)
	{
		try
		{
			File png = new File(location);
			
			ImageIO.write(image, "png", png);
		}
		
		catch (IOException e) {System.err.println("Error: Could not create image file.");}
	}
	
	/**
	 * Most of these algorithms accept Color matrices as backgrounds. This is in case
	 * I decide to revert to extracting images based on per-pixel calculations, like before.
	 */
	private static class OverlayExtractor
	{		
		/**
		 * Decides which algorithm to use. Applies no effects.
		 */
		private static Color[][] extractNormalOverlay(Color[][] background, Color[][] image)
		{
			if (IMGTools.getGreatestDifference(image) > 0.30) //number based on data from a collection of overlays generated.
			{
				System.out.println("Using algorithm 2.");
				
				return algorithm2(background, image, getComparisonColors(background, image));
			}
			
			System.out.println("Using algorithm 1.");
			
			return algorithm1NoLoop(image, background);
		}
		
		/**
		 * Retrieves normal algorithm. Applies effects in some cases.
		 */
		private static Color[][] extractBlendedOverlay(Color[][] background, Color[][] image)
		{
			Color[][] orePixels = extractNormalOverlay(background, image);
			Color[][] texturePixels = new Color[image.length][image[0].length];
			Color[][] textureMask = getColorsFromImage(getImageFromFile(System.getProperty("user.dir") + "/mask.png"));

			//Ore texture changes.
			if (orePixels.length > 16)
			{
				for (int i = 0; i < 2; i++) orePixels = IMGTools.removeLonePixels(orePixels);                //Remove all of the single pixels and small lines.			
				for (int i = 0; i < 3; i++) orePixels = IMGTools.removeOffColorPixelsFromBorders(orePixels); //Remove pixels that look too different from everything else.
				for (int i = 0; i < 2; i++) orePixels = IMGTools.reAddSurroundingPixels(orePixels, image);   //The ore textures are usually too small at this point. Expand them.
			}
			
			//Texture texture changes.
			texturePixels = IMGTools.convertToPushAndPull(image, background);   //Add transparency from getDifference() per-pixel using only black and white.
			texturePixels = IMGTools.removePixelsUsingMask(texturePixels, textureMask); //Use a vignette mask to lower the opacity from border pixels.
			
			return IMGTools.overlayImage(orePixels, texturePixels);
		}
		
		/**
		 * Determines which colors to forward into the extractor for comparison.
		 */
		private static Color[] getComparisonColors(Color[][] background, Color[][] image)
		{
			Color mostUniqueColor = IMGTools.getMostUniqueColor(background, image);		
			Color guessedColor = IMGTools.guessOreColor(background, image);
			
			if (guessedColor == null) return new Color[] {mostUniqueColor};
			
			Color blendedColor = IMGTools.getAverageColor(IMGTools.colorsToMatrix(mostUniqueColor,guessedColor));
			
			//Debug stuff
			
				double differenceUniqueGuessed = IMGTools.getDifference(mostUniqueColor, guessedColor);
				System.out.println("Difference between mostUniqueColor, guessedColor: " + differenceUniqueGuessed);
			
				Color[][] mostUniqueColorImage = IMGTools.fillColors(new Color[256][256], mostUniqueColor);
				Color[][] guessedColorImage = IMGTools.fillColors(new Color[256][256], guessedColor);
				Color[][] blendedColorImageFile = IMGTools.fillColors(new Color[256][256], blendedColor);
				
				writeImageToFile(createImageFromColors(mostUniqueColorImage), System.getProperty("user.dir") + "/most_unique_color.png");
				writeImageToFile(createImageFromColors(guessedColorImage), System.getProperty("user.dir") + "/guessed_ore_color.png");
				writeImageToFile(createImageFromColors(blendedColorImageFile), System.getProperty("user.dir") + "/blended_color.png");

			return new Color[] {mostUniqueColor, guessedColor, blendedColor};	
		}
		
		/**
		 * v1 of this algorithm uses frames to correctly align the background with each section of
		 * the ore texture. That is removed here as it isn't necessary when the background is only 
		 * one color. Depending on how things turn out, @background may eventually be a single Color()
		 * for the same reason.
		 */
		private static Color[][] algorithm1NoLoop(Color[][] image, Color[][] background)
		{
			int w = image.length, h = image[0].length, bh = background[0].length;
			
			if (h % bh != 0) return null; //No decimals allowed.
			
			Color[][] overlay = new Color[w][h];

			double threshold = 0.085; //Threshold used for separating ore from background.

			for (int x = 0; x < w; x++)
			{
				for (int y = 0; y < bh; y++)
				{
					overlay[x][y] = IMGTools.getOrePixel2(image[x][y], background[x][y], threshold);	
				}
			}
			
			return overlay;
		}
		
		//The biggest problem with this algorithm is that the colors passed into it are often not geniune.
		//It uses the closest match, and if that match is closer than the background, it is accepted.
		//I have yet to realize a way to verify these matches any further.
		private static Color[][] algorithm2(Color[][] background, Color[][] image, Color... colors)
		{
			Color[][] overlay = new Color[image.length][image[0].length];
			
			for (int x = 0; x < image.length; x++)
			{
				for (int y = 0; y < image[0].length; y++)
				{
					for (Color color : colors)
					{
						double differenceFromBackground = IMGTools.getDifference(image[x][y], background[x][y]);
						double differenceFromColor = IMGTools.getDifference(image[x][y], color);
						
						if (differenceFromColor < differenceFromBackground)
						{
							overlay[x][y] = image[x][y];
							
							break;
						}
					}
					
					if (overlay[x][y] == null) overlay[x][y] = new Color(0, 0, 0, 0);
				}
			}
			
			return overlay;
		}
		
		//This might currently be very slow for large images (>256x or so) due to the number of iterations. Luckily, most aren't >16-32x.
		private static Color[][] algorithm1(double targetAlpha, Color[][] image, Color[][] background)
		{
			int w = image.length, h = image[0].length, bh = background[0].length;
			int frames = h / bh;
			
			if (h % bh != 0) return null; //No decimals allowed.
			
			Color[][] overlay = new Color[w][h];

			double threshold = 0.6; //Threshold used for separating ore from background.
			double percentAlpha  = 100.0; //Percentage of alpha pixels.			
			int iteration = 0;
			
			while (percentAlpha >= targetAlpha && !(Math.abs(percentAlpha - targetAlpha) < 0.75)) 
			{
				percentAlpha = 0.0;
				threshold /= 1.1;

				for (int f = 0; f < frames; f++)
				{
					for (int x = 0; x < w; x++)
					{
						for (int y = 0; y < bh; y++)
						{
							int imageY = (f * bh) + y;
							
							//overlay[x][imageY] = IMGTools.getOrePixel(image[x][imageY], background[x][y], threshold);
							
							overlay[x][imageY] = IMGTools.getOrePixel2(image[x][imageY], background[x][y], threshold);
							
							percentAlpha += overlay[x][imageY].getAlpha();
						}
					}
				}

				percentAlpha /= (frames * w * bh);

				percentAlpha = 100 - (percentAlpha * 100 / 255);
				
				iteration++;
				System.out.println("Iteration #" + iteration + " complete. Current threshold: " + threshold + ". Current alpha percent: " + percentAlpha + "%" + ". Target alpha: " + targetAlpha + "%.");
			}
			
			return overlay;
		}
		

	}
}
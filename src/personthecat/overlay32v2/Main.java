package personthecat.overlay32v2;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
			
			//BufferedImage newBackground = IMGTools.scaleImage(createImageFromColors(IMGTools.fillColors(backgroundColors, IMGTools.getAverageColor(backgroundColors))), oreColors.length, oreColors.length);
			BufferedImage newBackground = IMGTools.scaleImage(createImageFromColors(backgroundColors), oreColors.length, oreColors.length);
			backgroundColors = getColorsFromImage(newBackground);

			//Create overlay
			BufferedImage overlay = null;
			
			if (fancyOverlay) overlay = createImageFromColors(OverlayExtractor.extractBlendedOverlay(backgroundColors, oreColors));
			else overlay = createImageFromColors(OverlayExtractor.extractNormalOverlay(backgroundColors, oreColors));
			
			//overlay = createImageFromColors(OverlayExtractor.algorithm1FromSD(backgroundColors, oreColors, IMGTools.getChannelAverage(IMGTools.getStandardDeviation(oreColors))));
			//overlay = createImageFromColors(OverlayExtractor.algorithm1NoLoop(backgroundColors, oreColors));
			
			System.out.println();
			
			//IMGTools.getPixelClusters(oreColors);
			printImageStats(oreColors);
			
			System.out.println();
			
			//OverlayExtractor.algorithm1MultiThresholdTest(backgroundColors, oreColors, new File(path).getName());
			
			//Write files
			writeImageToFile(newBackground, singleColorLocation);
			writeImageToFile(overlay, overlayLocation);
			
			System.out.println();
			
//			for (File png : new File(System.getProperty("user.dir") + "/output/thresholds").listFiles())
//			{
//				Color[][] colors = getColorsFromImage(getImageFromFile(png.getPath()));
//				int alphaCount = 0, pixelCount = 0;				
//				
//				for (int x = 0; x < colors.length; x++)
//				{
//					for (int y = 0; y < colors[0].length; y++)
//					{
//						pixelCount++;
//						
//						if (colors[x][y].getAlpha() < 20)
//						{
//							alphaCount++;
//						}
//					}
//				}
//				
//				if ((double) alphaCount / (double) pixelCount > 0.96) png.delete();
//			}
			
			Color[] colors = IMGTools.equalizeColorSums(new Color[] {new Color(125, 118, 192), new Color(67, 57, 59)});
			
			System.out.println("difference for equalized values: " + IMGTools.getDifference(colors[0], colors[1]));			
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
	
	public static BufferedImage createImageFromColors(Color[][] image)
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
	
	public static void writeImageToFile(BufferedImage image, String location)
	{
		try
		{
			File png = new File(location);
			
			ImageIO.write(image, "png", png);
		}
		
		catch (IOException e) {System.err.println("Error: Could not create image file.");}
	}
	
	private static void printImageStats(Color[][] image)
	{
		System.out.println("Average Difference: " + IMGTools.getAverageDifference(image));
		System.out.println("Standard Deviation: " + IMGTools.getChannelAverage(IMGTools.getStandardDeviation(image)));
	}
	/**
	 * Most of these algorithms accept Color matrices as backgrounds. This is in case
	 * I decide to revert to extracting images based on per-pixel calculations, like before.
	 */
	private static class OverlayExtractor
	{		
		//A texture's SD and optimal threshold are highly correlated (r = 0.9174)
		//Average difference is slightly higher (r = 0.9230). 
		//Try correlating with the highest channel from SD instead. 
		private static final double SD_THRESHOLD_RATIO = 0.0055; 
		
		/**
		 * Decides which algorithm to use. If one misses good pixels found in the other,
		 * adds those pixels. If any exclusive pixels are clearly bad, removes them.
		 * 
		 * Note: for all overlays included in the mod where normal stone is the background, 
		 * this only fixes two images. I'm not sure if it's actually a good solution, but
		 * it does fix those two overlays.
		 */
		private static Color[][] extractNormalOverlay(Color[][] background, Color[][] image)
		{			
			int w = image.length, h = image[0].length;
			
			//Getting a single color to avoid issues with frames and ores with no equivalent background.
			Color[][] singleColorBG = IMGTools.fillColors(background, IMGTools.getAverageColor(background));
			
			Color[][] algorithm1 = algorithm1FromSD(singleColorBG, image, IMGTools.getChannelAverage(IMGTools.getStandardDeviation(image)));
			Color[][] alg1Exclusives = IMGTools.createBlankImage(w, h);
			
			Color[][] algorithm2 = algorithm2(singleColorBG, image, getComparisonColors(background, image));
			Color[][] alg2Exclusives = IMGTools.createBlankImage(w, h);
			
			for (int x = 0; x < w; x++)
			{
				for (int y = 0; y < h; y++)
				{
					if (algorithm1[x][y].getAlpha() > 127 && algorithm2[x][y].getAlpha() < 127)
					{
						alg1Exclusives[x][y] = algorithm1[x][y];
					}
					
					if (algorithm2[x][y].getAlpha() > 127 && algorithm1[x][y].getAlpha() < 127)
					{
						alg2Exclusives[x][y] = algorithm1[x][y];
					}
				}
			}
			
			//Getting the average color in case the image forwarded in isn't
			//just one color. This actually has to be just one color.
			Color averageColorBG = IMGTools.getAverageColor(background);
			
			Double alg1ExclusivesDifferenceFromBG = new Double(IMGTools.getAverageDifferenceFromColor(averageColorBG, alg1Exclusives));
			Double noDifference = new Double(0.4901960295198231);
			
			/*
			 * The exclusives from algorithm1's output are the most useful;
			 * they tend to either have pixels that algorithm2's output is missing
			 * (as 2's problems are usually the result of not including enough pixels)
			 * or they just have too many pixels (i.e. 1's problems are usually the
			 * result of including too many pixels. The opposite is not true. We can
			 * use this information to decide when to add extra pixels to algorithm2's
			 * output or when to remove pixels from algorithm1's output.
			 */

			if (!alg1ExclusivesDifferenceFromBG.equals(noDifference))
			{
				System.out.println("alg1ExclusivesDifferenceFromBG" + alg1ExclusivesDifferenceFromBG);
				
				if (alg1ExclusivesDifferenceFromBG < 0.1) //These shouldn't be here.
				{
					System.out.println("removing extra pixels from alg 1");
					
					algorithm1 = IMGTools.removePixelsUsingMask(algorithm1, alg1Exclusives);
				}
				
				//This value is too picky. Thus, this entire function is not a good long-term solution.
				if (alg1ExclusivesDifferenceFromBG > 0.27) //These probably should have been kept.
				{
					System.out.println("adding extra pixels to alg 2");
					
					algorithm2 = IMGTools.overlayImage(algorithm2, alg1Exclusives);
				}
			}
			
			//Pixels may sometimes be similar at this point, but this if statement 
			//still helps decide the best algorithm when they aren't.
			
			if (IMGTools.getGreatestDifference(image) > 0.45)
			{				
				System.out.println("algorithm 2");
				
				return algorithm2;
			}

			System.out.println("algorithm 1");
			
			return algorithm1;
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
			
			Color blendedColor = IMGTools.getAverageColor(IMGTools.arrayToMatrix(mostUniqueColor,guessedColor));
			
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
		private static Color[][] algorithm1NoLoop(Color[][] background, Color[][] image)
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
		
		private static Color[][] algorithm1FromSD(Color[][] background, Color[][] image, int SD)
		{
			int w = image.length, h = image[0].length, bh = background[0].length;
			
			if (h % bh != 0) return null; //No decimals allowed.
			
			Color[][] overlay = new Color[w][h];

			double threshold = SD * SD_THRESHOLD_RATIO; //Threshold used for separating ore from background.

			for (int x = 0; x < w; x++)
			{
				for (int y = 0; y < bh; y++)
				{
					overlay[x][y] = IMGTools.getOrePixel2(image[x][y], background[x][y], threshold);	
				}
			}
			
			return overlay;
		}
		
		private static void algorithm1MultiThresholdTest(Color[][] background, Color[][] image, String filename)
		{
			int w = image.length, h = image[0].length, bh = background[0].length;
			
			double threshold = 1.0;
			
			while (threshold > 0)
			{
				threshold -= 0.05;
				
				Color[][] overlay = new Color[w][h];

				for (int x = 0; x < w; x++)
				{
					for (int y = 0; y < bh; y++)
					{
						overlay[x][y] = IMGTools.getOrePixel2(image[x][y], background[x][y], threshold);	
					}
				}
				
				new File(System.getProperty("user.dir") + "/output/thresholds").mkdirs();
				
				BufferedImage bi = createImageFromColors(overlay);
				
				writeImageToFile(bi, System.getProperty("user.dir") + "/output/thresholds/" + filename + "_" + String.valueOf(threshold).replace('.', 'D') + ".png");
			}
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
		private static Color[][] algorithm1(double targetAlpha, Color[][] background, Color[][] image)
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
		
		private static void algorithm3(Color[][] background, Color[][] image)
		{
			List<Color[][]> clusters = getMatchingPixelClusters(background, image, getComparisonColors(background, image));
			
			for (Color[][] cluster : clusters)
			{
				System.out.println("Current cluster: " + clusters.indexOf(cluster));
				
				double averageDifference = 0.0;
				int pixelCount = 0;
				
				for (int x = 0; x < cluster.length; x++)
				{
					for (int y = 0; y < cluster[0].length; y++)
					{						
						if (cluster[x][y].getAlpha() > 17) 
						{
							averageDifference += IMGTools.getDifference(image[x][y], background[x][y]);
							
							pixelCount++;
						}
					}
				}
				
				averageDifference /= pixelCount;
				
				System.out.println("averageDifference " + averageDifference);
			}
		}
		
		private static List<Color[][]> getMatchingPixelClusters(Color[][] background, Color[][] image, Color... colors)
		{
			List<Color[][]> clusters = new ArrayList<>();
			boolean[][] isPixelUsed = new boolean[image.length][image[0].length];
			
			//For each pixel...
			for (int x = 1; x < image.length - 1; x++)
			{
				for (int y = 1; y < image[0].length - 1; y++)
				{
					Color[][] currentCluster = IMGTools.createBlankImage(image.length, image[0].length);
					int clusterSize = 0;
					
					double differenceFromBackground = IMGTools.getDifference(image[x][y], background[x][y]);
					
					//If it's visible and not already used, 
					//create a cluster. Then...
					if (!isPixelUsed[x][y] && image[x][y].getAlpha() > 17 && differenceFromBackground > 0.35)
					{
						currentCluster[x][y] = image[x][y];
						isPixelUsed[x][y] = true;
						clusterSize++;
						
						boolean clusterHasMorePixels = true;
						
						while (clusterHasMorePixels)
						{
							clusterHasMorePixels = false;
							
							//Assuming the cluster has more pixels: for each pixel in the cluster,
							//look for adjacent pixels and add them to the cluster.
							for (int cX = 0; cX < currentCluster.length; cX++)
							{
								for (int cY = 0; cY < currentCluster[0].length; cY++)
								{
									if (currentCluster[cX][cY].getAlpha() > 17)
									{
										//Look through all other pixels and see if it's adjacent.
										for (int x2 = 0; x2 < image.length; x2++)
										{
											for (int y2 = 0; y2 < image[0].length; y2++)
											{
												for (Color color : colors)
												{
													differenceFromBackground = IMGTools.getDifference(image[x2][y2], background[x2][y2]);
													double differenceFromColor = IMGTools.getDifference(image[x2][y2], color);
														
													if (!isPixelUsed[x2][y2] &&                       //Not already part of a cluster
													image[x2][y2].getAlpha() > 17 &&                  //the pixel exists in the image
													differenceFromColor < differenceFromBackground && //Closer to current color than bg
													currentCluster[x2][y2].getAlpha() < 17 &&         //Not already in this cluster
													IMGTools.arePixelsAdjacent(cX, cY, x2, y2))       //Pixels are proximally close
													{
														currentCluster[x2][y2] = image[x2][y2];
														isPixelUsed[x2][y2] = true;
														clusterSize++;
														clusterHasMorePixels = true;
													}	
												}//end comparison colors
											}//end y2
										}//end x2
									}//end if--pixel exists
								}//end cY
							}//end cX
						}//end while(clusterHasMorePixels)
					}//end if--create cluster / finish cluster
					
					//It's a valid cluster if it's larger than the size of one single pixel in 16x. 
					//This is where the lone pixels get removed (but not in 16x).
					if (clusterSize >= Math.pow((image.length / 16), 2))
					{
						clusters.add(currentCluster);
						
						System.out.println("A cluster was found. Size: " + clusterSize);
						
						Main.writeImageToFile(Main.createImageFromColors(currentCluster), System.getProperty("user.dir") + "/cluster_" + clusters.indexOf(currentCluster) + ".png");
					}
				}
			}
			
			return clusters;
		}
	}
}
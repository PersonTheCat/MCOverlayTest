package personthecat.overlay32v2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class IMGTools
{
	private static final double TEXTURE_SHARPEN_RATIO = 2.0;       //Multiplies the alpha levels for push and pull / overlay background textures.
	private static final double COLOR_RANGE_MAX_DIFFERENCE = 0.10; //Used for separating colors into ranges.
	private static final int TRANSPARENCY_THRESHOLD = 17;          //Pixels with lower alpha levels are considered transparent.
	private static final int OPACITY_THRESHOLD = 50;               //Pixels with higher alpha levels are considered opaque.
	private static final int SOLID_THRESHOLD = 120;			       //Pixels with higher alpha levels probably shouldn't have been removed...
	 
	public static Color getAverageColor(Color[][] image)
	{
		int r = 0, g = 0, b = 0;
		int pixelCount = 0;
		
		for (int x = 0; x < image.length; x++)
		{
			for (int y = 0; y < image[0].length; y++)
			{
				if (image[x][y].getAlpha() > OPACITY_THRESHOLD)
				{
					r += image[x][y].getRed();
					g += image[x][y].getGreen();
					b += image[x][y].getBlue();
					
					pixelCount++;
				}
			}
		}
		
		if (pixelCount == 0) return new Color(0, 0, 0, 0);
		
		r /= pixelCount;
		g /= pixelCount;
		b /= pixelCount;
		
		return new Color(r, g, b);
	}
	
	/**
	 * Replaces all pixels with the same color, (a)rgb.
	 */
	public static Color[][] fillColors(Color[][] image, Color rgb)
	{
		int w = image.length, h = image[0].length;
		
		for (int i = 0; i < w; i++)
		{
			for (int j = 0; j < h; j++)
			{
				image[i][j] = rgb;
			}
		}
		
		return image;
	}
	
	/**
	 * Only width passed to this function because some images may have multiple frames.
	 */
	public static BufferedImage scaleImage(BufferedImage image, int newW, int newH)
	{
		BufferedImage scaledImage = new BufferedImage(newW, newH, image.getType());
		
		Graphics2D graphics = scaledImage.createGraphics();
		graphics.drawImage(image, 0, 0, newW, newH, null);
		graphics.dispose();
		
		return scaledImage;
	}
	
	/**
	 * For each pixel in the image, finds which color is the most different from its counterpart.
	 */
	public static Color getMostUniqueColor(Color[][] background, Color[][] image)
	{
		double greatestDifference = 0.0;
		Color mostUniqueColor = null;

		for (int x = 0; x < image.length; x++)
		{
			for (int y = 0; y < image[0].length; y++)
			{
				double difference = getDifference(image[x][y], background[x][y]);
				
				if (difference > greatestDifference)
				{
					greatestDifference = difference;
					
					mostUniqueColor = image[x][y];
				}
			}
		}
		
		return mostUniqueColor;
	}
	
	/**
	 * Finds the greatest difference for any two pixels in an image. Differs from
	 * getMostUniqueColor() in that it is not based on the background. This should
	 * provide an estimation of how strong a threshold is needed to extract an image.
	 * 
	 * Edit: These numbers are actually the same as those from getMostUniqueColor().
	 * Move these to the main class and use both pieces of information from the same
	 * function?
	 * 
	 * Edit 2: In addition to producing the same information, this function is less
	 * efficient. Delete me.
	 */
	public static double getGreatestDifference(Color[][] image)
	{
		double greatestDifference = 0.0;
		
		for (int x = 0; x < image.length; x++)
		{
			for (int y = 0; y < image[0].length; y++)
			{
				for (int x2 = 0; x2 < image.length; x2++)
				{
					for (int y2 = 0; y2 < image[0].length; y2++)
					{
						double difference = getDifference(image[x][y], image[x2][y2]);
						
						if (difference > greatestDifference)
						{
							greatestDifference = difference;
						}
					}
				}
			}
		}
		
		System.out.println("Greatest difference from ore texture: " + greatestDifference);
		
		
		return greatestDifference;
	}
	
	/**
	 * Looks through an array of "ore" colors to help estimate which pixels are part of the
	 * actual ore and not the background.
	 */	
	public static Color guessOreColor(Color[][] background, Color[][] image)
	{
		List<Color> allMatches = new ArrayList<>();
		
		for (int x = 0; x < image.length; x++)
		{
			for (int y = 0; y < image[0].length; y++)
			{
				Color color = image[x][y];
				Color closestOreColor = getClosestOreColor(color);
				
				double differenceFromBackground = getDifference(image[x][y], background[x][y]);
				double differenceFromClosestOreColor = getDifference(image[x][y], closestOreColor);
				
				if (differenceFromClosestOreColor < differenceFromBackground)
				{
					//System.out.println("Current match: " + "Color(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ").");
					//System.out.println("Matches with: " + "Color(" + closestOreColor.getRed() + ", " + closestOreColor.getGreen() + ", " + closestOreColor.getBlue() + ").");
					
					if (closestOreColor.getRed() == 252 && closestOreColor.getGreen() == 227 && closestOreColor.getBlue() == 124) System.out.println("Current match: " + "Color(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ").");
					
					allMatches.add(closestOreColor);
				}
			}
		}
		
		if (allMatches.size() < 1) return null;
		
		return getMostCommonColor(allMatches.toArray(new Color[allMatches.size()]));
	}
	
	/**
	 * Gets the closest ore color from the list
	 * OR returns the default value  
	 */
	public static Color getClosestOreColor(Color color)
	{
		double minimumDifference = 1.0;
		Color closestColor = null;
		
		for (Color oreColor : equalizeColorSums(OreColors.getAllColors()))
		{
			double difference = getDifference(color, oreColor);

			if (difference < minimumDifference)
			{
				minimumDifference = difference;
				
				closestColor = oreColor;
			}
		}
		
		return closestColor;
	}
	
	public static Color getMostCommonColor(Color[] color)
	{
		int highestCount = 0;
		
		Color mostCommonColor = color[0];
		
		for (int i = 0; i < color.length; i++)
		{
			int colorCount = 0;
			
			for (int j = 0; j < color.length; j++)
			{
				if (color[i] == color[j]) colorCount++;
			}
			
			if (colorCount > highestCount)
			{
				highestCount = colorCount;
				
				mostCommonColor = color[i];
			}
		}
		
		System.out.println("Most common color (from array): " + "Color(" + mostCommonColor.getRed() + ", " + mostCommonColor.getGreen() + ", " + mostCommonColor.getBlue() + ").");
		
		return mostCommonColor;
	}
	
	public static double getAverageDifferenceFromColor(Color color, Color[][] image)
	{
		double averageDifference = 0.0;
		int pixelCount = 0;
		
		for (int x = 0; x < image.length; x++)
		{
			for (int y = 0; y < image[0].length; y++)
			{
				if (image[x][y].getAlpha() > TRANSPARENCY_THRESHOLD)
				{
					averageDifference += getDifference(color, image[x][y]);
					
					pixelCount++;
				}
				
				
			}
		}
		
		averageDifference /= pixelCount;
		
		return averageDifference;
	}
	
	/**
	 * For each pixel, if the foreground is closer to any color than the background, tally.
	 */
	public static double getPercentPixelsCloseToColor(Color[][] background, Color[][] image, Color... colors)
	{
		double percent = 0.0;
		
		for (int x = 0; x < image.length; x++)
		{
			for (int y = 0; y < image[0].length; y++)
			{
				for (Color color : colors)
				{
					double differenceFromBackground = getDifference(image[x][y], background[x][y]);
					double differenceFromColor = getDifference(image[x][y], color);
					
					if (differenceFromColor < differenceFromBackground)
					{
						percent++;
						
						break;
					}
				}
			}
		}
		
		percent /= (image.length * image[0].length);
		
		return (100 - (100 * percent));
	}
	
	/**
	 * Gets the difference, returns the foreground pixel if any value is outside of the accepted RGB range (0-255).
	 */  //algorithm2 is similar, but shorter--may remove.
	public static Color getOrePixel(Color foreground, Color background, double alphaPercent)
	{
		if (foreground.getRGB() == background.getRGB()) return new Color(0, 0, 0, 0);
		
		int r = (int) ((foreground.getRed() - background.getRed() * (1 - alphaPercent)) / alphaPercent);
		int g = (int) ((foreground.getGreen() - background.getGreen() * (1 - alphaPercent)) / alphaPercent);
		int b = (int) ((foreground.getBlue() - background.getBlue() * (1 - alphaPercent)) / alphaPercent);
		
		//if any is > 255 || < 0 -> keep foreground pixel.
		if (r > 255 || g > 255 || b > 255 || r < 0 || g < 0 || b < 0) return foreground;
		
		return new Color(0, 0, 0, 0); //else return nothing.
	}
	
	public static Color getOrePixel2(Color foreground, Color background, double threshold)
	{
		if (IMGTools.getDifference(foreground, background) > threshold) return foreground;
		
		return new Color(0, 0, 0, 0);
	}
	
	/** 
	 * Using the distance formula to determine the 0 - 1 "distance" between each color. 
	 * Imagine r, g, and b as x, y, and z on a coordinate plane; they are dimensions, not additive values.
	 * e.g. Color(255, 255, 0) and Color(255, 0, 255) are totally different colors, so getting the average doesn't work.
	 */
	public static double getDifference(Color foreground, Color background)
	{
		int r = foreground.getRed() - background.getRed();
		int g = foreground.getGreen() - background.getGreen();
		int b = foreground.getBlue() - background.getBlue();
		
		int r2 = (r * r), g2 = (g * g), b2 = (b * b);
		
		return Math.sqrt(r2 + g2 + b2) / 441.673; //Roughly the maximum distance.
	}
	
	/**
	 * Gets the average difference from all adjacent pixels.
	 */
	public static double getAverageDifference(Color[][] image)
	{
		double averageDifference = 0.0;
		int pixelCount = 0;
		
		for (int x = 0; x < image.length; x++)
		{
			for (int y = 0; y < image[0].length; y++)
			{				
				for (int x2 = 0; x2 < image.length; x2++)
				{
					for (int y2 = 0; y2 < image[0].length; y2++)
					{						
						if (arePixelsAdjacent(x, y, x2, y2))
						{
							averageDifference += getDifference(image[x][y], image[x2][y2]);
							
							pixelCount++;
						}
					}
				}
			}
		}
		
		averageDifference /= pixelCount;
		
		return averageDifference;
	}

	/**
	 * Basically just calculates the SD for r, g, and b of each color,
	 * then returns that as a color.
	 */
	public static Color getStandardDeviation(Color[][] image)
	{
		LimitlessColor average = LimitlessColor.fromColor(getAverageColor(image));
		
		List<LimitlessColor> devianceValues = new ArrayList<>();

		for (int x = 0; x < image.length; x++)
		{
			for (int y = 0; y < image[0].length; y++)
			{
				LimitlessColor noLimits = LimitlessColor.fromColor(image[x][y]);
				
				LimitlessColor deviance = LimitlessColor.subtractColors(noLimits, average);
				
				deviance.squareColor();
				
				devianceValues.add(deviance);
			}
		}
		
		LimitlessColor sum = devianceValues.get(0);
		
		for (int i = 1; i < devianceValues.size(); i++)
		{
			sum = LimitlessColor.addColors(sum, devianceValues.get(i));
		}
		
		sum.divideBy(devianceValues.size());
		
		sum.toSquareRoot();
		
		return sum.getColor();
	}
	
	public static boolean isPixelDarker(Color foreground, Color background)
	{
		int fgTotal = (Math.abs(foreground.getRed() + foreground.getGreen() + foreground.getBlue()));
		int bgTotal = (Math.abs(background.getRed() + background.getGreen() + background.getBlue()));
		
		return fgTotal < bgTotal;
	}
	
	/**
	 * In 8 directions, if > 5 are alpha = 0, remove pixel. Does not include border pixels.
	 * Can be repeated to remove textures that are unusually narrow. Probably not wise for <32x textures. 
	 */
	public static Color[][] removeLonePixels(Color[][] image)
	{
		for (int x = 1; x < image.length - 1; x++)
		{
			for (int y = 1; y < image[0].length - 1; y++)
			{
				int alphaCount = getSurroundingAlphaCount(image, x, y);
				
				if (alphaCount > 5) image[x][y] = new Color(0, 0, 0, 0);
			}
		}
		
		return image;
	}
	
	/**
	 * Better version of removeLonePixels(). It works by testing each pixel to see if it's a part
	 * of a larger mass. If the amount of pixels per cluster == (1/16 * image.length)^2, it is 
	 * considered part of the ore sprite. Its benefit is that it only filters lone pixels out
	 * if they are so distant from a cluster that the algorithm isn't able to find them. This
	 * should allow surrounding pixels to be filled in a way that more aptly restores the original
	 * shape of the ore sprites than was previously possible.
	 * 
	 * Note: this may need to be modified to verify that each pixel is of a similar shade to the
	 * average of the cluster.
	 * 
	 * Edit: This could be modified into an actual extraction algorithm, which would work instead
	 * by making sure that each pixel in the cluster is of the same color range, where it finally
	 * ignores any color range that is on average close to the background color. May be unnecessary
	 * and or ultimately no different from algorithm2(). Testing is needed.
	 * 
	 * Note 2: an unusually high number of clusters may indicate that the texture hasn't generated
	 * correctly. Not sure how to use that, though, especially considering that, if this is
	 * happening for one image, it's most likely also happening for a lot of others.
	 */
	public static List<Color[][]> getPixelClusters(Color[][] image)
	{
		boolean[][] isPixelUsed = new boolean[image.length][image[0].length];
		List<Color[][]> clusters = new ArrayList<>();
		
		//For each pixel...
		for (int x = 0; x < image.length; x++)
		for (int y = 0; y < image[0].length; y++)
		{			
			//If the pixel is visible and not already used, create a cluster.
			if (!isPixelUsed[x][y] && image[x][y].getAlpha() > TRANSPARENCY_THRESHOLD)
			{
				Color[][] currentCluster = createBlankImage(image.length, image[0].length);
				int clusterSize = 1;
				currentCluster[x][y] = image[x][y];
				isPixelUsed[x][y] = true;
				
				boolean clusterHasMorePixels = true;
				
				//While there may be more pixels to add, continue looking
				while (clusterHasMorePixels)
				{
					Color averageColor = getAverageColor(currentCluster);
					clusterHasMorePixels = false;

					//For each pixel in the current cluster
					for (int cX = 0; cX < currentCluster.length; cX++)
					for (int cY = 0; cY < currentCluster[0].length; cY++)
					{
						if (currentCluster[cX][cY].getAlpha() > TRANSPARENCY_THRESHOLD)
						{
							//Look through pixels in the original and see if one is adjacent.
							for (int x2 = 0; x2 < image.length; x2++)
							for (int y2 = 0; y2 < image[0].length; y2++)
							{
								double differenceFromAverage = getDifference(averageColor, image[x2][y2]);								
								
								if (arePixelsAdjacent(cX, cY, x2, y2) &&                    //Pixels are adjacent
								!isPixelUsed[x2][y2] &&                                     //Pixel is not already used       //Dupe?
								differenceFromAverage < COLOR_RANGE_MAX_DIFFERENCE &&       //Color is in the same range as the rest of the cluster
								image[x2][y2].getAlpha() > TRANSPARENCY_THRESHOLD &&        //Original pixel exists
								currentCluster[x2][y2].getAlpha() < TRANSPARENCY_THRESHOLD) //Pixel is not already in cluster //Dupe of ^?
								{
									currentCluster[x2][y2] = image[x2][y2];
									isPixelUsed[x2][y2] = true;
									clusterSize++;
									clusterHasMorePixels = true;
								}
							}
						}
					}
				}
				
				//It's a valid cluster if it's larger than the size of one single pixel in 16x. 
				//This is where the lone pixels get removed (but not in 16x).
				if (clusterSize >= Math.pow((image.length / 16), 2))
				{
					clusters.add(currentCluster);
					
					System.out.println("Cluster #" + clusters.indexOf(currentCluster) +". Size: " + clusterSize);
					
					Main.writeImageToFile(Main.createImageFromColors(currentCluster), System.getProperty("user.dir") + "/cluster_" + clusters.indexOf(currentCluster) + ".png");
				}
			}//end if (!isPixelUsed[x][y] && image[x][y].getAlpha() > TRANSPARENCY_THRESHOLD)
		}
		
		return clusters;
	}
	
	/**
	 * Helps to avoid NullPointerExceptions.
	 */
	public static Color[][] createBlankImage(int w, int h)
	{
		Color[][] image = new Color[w][h];
		
		for (int x = 0; x < w; x++)
		{
			for (int y = 0; y < h; y++)
			{
				image[x][y] = new Color(0, 0, 0, 0);
			}
		}
		
		return image;
	}
	
	/**
	 * The specific distance used is to include diagonal pixels. 
	 * That is intentional for >16x images.
	 */
	public static boolean arePixelsAdjacent(int x1, int y1, int x2, int y2)
	{
		if ((x1 == x2) && (y1 == y2)) return false;
		
		double distance = Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
		
		return distance < 1.5;
	}
	
	//private static Color[][] combineColorMatrices(Color[][]... images)
	{
		
	}
	
	/**
	 * Goes around the edge of the textures (on the borders between the pixels with full transparency and everything else)
	 * and removes them if they are too different from the average color. If the pixel is brown but most of the ore is blue, 
	 * it gets removed. If the background and the ore are pretty similar, nothing happens anyway. 
	 */
	public static Color[][] removeOffColorPixelsFromBorders(Color[][] image)
	{
		Color averageColor = getAverageColor(image);
		
		for (int x = 0; x < image.length; x++)
		{
			for (int y = 0; y < image[0].length; y++)
			{
				double difference = getDifference(image[x][y], averageColor);
				
				try
				{
					if ((getSurroundingAlphaCount(image, x, y) > 2) && difference > 0.25) //Make this threshold a variable? Use algorithm2?
					{
						image[x][y] = new Color(0, 0, 0, 0);
					}
				}
				
				//This means border pixels are removed.
				catch (IndexOutOfBoundsException ignored) {image[x][y] = new Color(0, 0, 0, 0);} 
			}
		}
		
		return image;
	}
	
	/**
	 * Using an image with clusters of non-alpha pixels, re-add the surrounding pixels at 50% opacity.
	 * Makes sure the entire ore textures are both included and blended to the background.
	 */
	public static Color[][] reAddSurroundingPixels(Color[][] image, Color[][] original)
	{
		for (int x = 1; x < image.length - 1; x++)
		{
			for (int y = 1; y < image[0].length - 1; y++)
			{
				if ((image[x][y].getAlpha() == 255) && (getSurroundingAlphaCount(image, x, y) < 4))
				{
					//Neighbor pixel's alpha < half ? original pixel w/ 2/3 opacity.
					image[x + 1][y] = testAlphaAndReplaceColor(image[x + 1][y], original[x + 1][y], 127, 200);
					image[x - 1][y] = testAlphaAndReplaceColor(image[x - 1][y], original[x - 1][y], 127, 200);
					image[x][y + 1] = testAlphaAndReplaceColor(image[x][y + 1], original[x][y + 1], 127, 200);
					image[x][y - 1] = testAlphaAndReplaceColor(image[x][y - 1], original[x][y - 1], 127, 200);
					
					//Diagonal pixel's alpha < half ? original pixel w/ quarter opacity.
					image[x + 1][y + 1] = testAlphaAndReplaceColor(image[x + 1][y + 1], original[x + 1][y + 1], 127, 63);
					image[x - 1][y - 1] = testAlphaAndReplaceColor(image[x - 1][y - 1], original[x - 1][y - 1], 127, 63);
					image[x + 1][y - 1] = testAlphaAndReplaceColor(image[x + 1][y - 1], original[x + 1][y - 1], 127, 63);
					image[x - 1][y + 1] = testAlphaAndReplaceColor(image[x - 1][y + 1], original[x - 1][y + 1], 127, 63);
				}
			}
		}
		
		return image;
	}
	
	public static int getSurroundingAlphaCount(Color[][] image, int x, int y)
	{
		int alphaCount = 0;
		
		if (image[x + 1][y].getAlpha() < TRANSPARENCY_THRESHOLD) alphaCount++;
		if (image[x - 1][y].getAlpha() < TRANSPARENCY_THRESHOLD) alphaCount++;
		if (image[x][y + 1].getAlpha() < TRANSPARENCY_THRESHOLD) alphaCount++;
		if (image[x][y - 1].getAlpha() < TRANSPARENCY_THRESHOLD) alphaCount++;
		if (image[x + 1][y + 1].getAlpha() < TRANSPARENCY_THRESHOLD) alphaCount++;
		if (image[x - 1][y - 1].getAlpha() < TRANSPARENCY_THRESHOLD) alphaCount++;
		if (image[x + 1][y - 1].getAlpha() < TRANSPARENCY_THRESHOLD) alphaCount++;
		if (image[x - 1][y + 1].getAlpha() < TRANSPARENCY_THRESHOLD) alphaCount++;
		
		return alphaCount;
	}
	
	/**
	 * Uses getDifference() to determine the alpha level for each pixel.
	 * Uses isPixelDarker() to determine whether each pixel should be black or white (push or pull)
	 */
	public static Color[][] convertToPushAndPull(Color[][] originalImage, Color[][] background)
	{
		Color[][] newImage = new Color[originalImage.length][originalImage[0].length];
		
		for (int x = 0; x < originalImage.length; x++)
		{
			for (int y = 0; y < originalImage[0].length; y++)
			{
				int alpha = (int) (255 * getDifference(originalImage[x][y], background[x][y]));
				
				alpha = (int) ((double) alpha);
				
				if (alpha > 200) alpha = 200;
				if (alpha < 0) alpha = 0;
				
				if (isPixelDarker(originalImage[x][y], background[x][y]))
				{					
					newImage[x][y] = new Color(0, 0, 0, alpha);
				}
				
				else newImage[x][y] = new Color(255, 255, 255, alpha);
			}
		}
		
		return newImage;
	}
	
	public static Color testAlphaAndReplaceColor(Color targetColor, Color originalColor, int targetAlpha, int newAlpha)
	{
		if (targetColor.getAlpha() < targetAlpha) return new Color(originalColor.getRed(), originalColor.getGreen(), originalColor.getBlue(), newAlpha);
		
		return targetColor;
	}
	
	/**
	 * Lowers the alpha value of all pixels based on those of their counterparts.
	 */
	public static Color[][] removePixelsUsingMask(Color[][] image, Color[][] mask)
	{
		int scaleW = 1;
		
		if (mask.length > image.length) scaleW = mask.length / image.length;
		if (image.length > mask.length) scaleW = image.length / mask.length;
		
		int scaleH = 1;
		
		if (mask[0].length > image[0].length) scaleH = mask[0].length / image[0].length;
		if (image[0].length > mask[0].length) scaleH = image[0].length / mask[0].length;
		
		for (int x = 0; x < image.length; x++)
		{
			for (int y = 0; y < image[0].length; y++)
			{
				int r = image[x][y].getRed();
				int g = image[x][y].getGreen();
				int b = image[x][y].getBlue();
				int a = (int) ((double) image[x][y].getAlpha() * (1.0 - ((double) mask[x * scaleW][y * scaleH].getAlpha() / 255)));

				if (a < 0) a = 0;
				if (a > 255) a = 255;
				
				image[x][y] = new Color(r, g, b, a);
			}
		}
		
		return image;
	}
	
	public static Color[][] overlayImage(Color[][] foreground, Color[][] background)
	{
		for (int x = 0; x < foreground.length; x++)
		{
			for (int y = 0; y < foreground[0].length; y++)
			{
				foreground[x][y] = blendPixels(foreground[x][y], background[x][y]);
			}
		}
		
		return foreground;
	}
	
	/**
	 * Basically just getting a weighted average of each color relative to the foreground's alpha level. 
	 * Foreground gets alpha level * its color, background gets the rest * its color. Final alpha = sum of both.
	 */
	public static Color blendPixels(Color foreground, Color background)
	{			
		int r = ((foreground.getRed() * foreground.getAlpha()) + (background.getRed() * (255 - foreground.getAlpha()))) / 255;
		int g = ((foreground.getGreen() * foreground.getAlpha()) + (background.getGreen() * (255 - foreground.getAlpha()))) / 255;
		int b = ((foreground.getBlue() * foreground.getAlpha()) + (background.getBlue() * (255 - foreground.getAlpha()))) / 255;
		
		if (foreground.getAlpha() > OPACITY_THRESHOLD) //I threw this in here to see if it would remove some of the harsh outlines. Wasn't originally here.
		{                                              //Basically, just keep the overlay pixel's colors if it's strong enough. That's why its existence doesn't make sense.
			r = foreground.getRed(); g = foreground.getGreen(); b = foreground.getBlue();
		}
		
		int a = foreground.getAlpha() + background.getAlpha();
		
		if (a < TRANSPARENCY_THRESHOLD && (r == 255 && g == 255 && b == 255)) return new Color(0, 0, 0, 0); //Don't keep white pixels that are hardly there.
		
		a = (int) ((double) a * TEXTURE_SHARPEN_RATIO); //Sharpen the ones that aren't.
		
		if (a > 255) a = 255;                           //Don't pass the limit.
		
		return new Color(r, g, b, a);
	}

	/**
	 * Just used for getting the average of multiple colors more neatly.
	 */
	public static Color[][] arrayToMatrix(Color... colors)
	{
		Color[][] newMatrix = new Color[1][colors.length];
		
		for (int i = 0; i < colors.length; i++)
		{
			newMatrix[0][i] = colors[i];
		}
		
		return newMatrix;
	}
	
	/**
	 * Used for equalizing color sums.
	 */
	public static Color[] matrixToArray(Color[][] colors)
	{
		Color[] newArray = new Color[colors.length * colors[0].length];
		
		for (int x = 0; x < colors.length; x++)
		{
			for (int y = 0; y < colors[0].length; y++)
			{
				newArray[x * colors.length + y] = colors[x][y];
			}
		}
		
		return newArray;
	}
	
	public static int getChannelAverage(Color color)
	{
		return (color.getRed() + color.getGreen() + color.getBlue()) / 3;
	}
	
	/**
	 * While this unfortunately does change the colors, it effectively increases their distance from one another.
	 * Key word being "effectively." When getDifference() is called for each color, the algorithm is more
	 * likely to find the correct "color" (in its mutated form) than it otherwise would be.
	 */
	public static Color[] equalizeColorSums(Color[] colors)
	{
		int averageSum = 0;
		
		for (Color color : colors)
		{
			averageSum += (color.getRed() + color.getGreen() + color.getBlue());
		}
		
		averageSum /= colors.length;

		Color[] newColors = new Color[colors.length];
		
		for (int i = 0; i < newColors.length; i++)
		{
			int r = colors[i].getRed();
			int g = colors[i].getGreen();
			int b = colors[i].getBlue();
			
			int deviance = (averageSum - (r + g + b));
			
			r += (deviance / 3);
			g += (deviance / 3);
			b += (deviance / 3);
			
			r = r > 250 ? 250 : r < 0 ? 0 : r;
			g = g > 250 ? 250 : g < 0 ? 0 : g;
			b = b > 250 ? 250 : b < 0 ? 0 : b;

			newColors[i] = new Color(r, g, b);
		}
		
		return newColors;
	}
	
	/**
	 * The average colors of these ores, excluding their backgrounds. 
	 * Some were deliberately left out (e.g. most white and grey ores);
	 * this is because we're trying to produce the farthest color from the background,
	 * which will work less often given the most common colors for background textures.
	 */
	private static enum OreColors
	{
		AMBER(223, 152, 39),
		AMETHYST(221, 116, 251),
		COPPER(139, 70, 0),
		DIAMOND(155, 229, 250),
		EMERALD(67, 203, 114),
		GOLD(252, 227, 124),
		LAPIS(28, 70, 165),
		LEAD(97, 116, 146),
		MALACHITE(81, 179, 157),
		MERCURY(110, 73, 79),
		REDSTONE(178, 2, 2),
		PERIDOT(151, 180, 92),
		RUBY(203, 78, 122),
		TOPAZ(204, 135, 84),
		SOME_OBSCURE_ORE(72, 112, 94);
		
		private int r, g, b;
		
		private OreColors(int r, int g, int b)
		{
			this.r = r;
			this.g = g;
			this.b = b;
			
//			double mean = ((r + g + b) / 3);
//			
//			double rDev = Math.pow((r - mean), 2);
//			double gDev = Math.pow((g - mean), 2);
//			double bDev = Math.pow((b - mean), 2);
//			
//			double SD = Math.sqrt(((rDev + gDev + bDev) / 2));
//			
//			System.out.println(toString() + ":");
//			System.out.println("SD: " + SD);
//			System.out.println("Sum: " + (r + g + b));
		}
		
		public Color getColor()
		{
			return new Color(r, g, b);
		}
		
		public static Color[] getAllColors()
		{
			Color[] colors = new Color[values().length];
			
			for (int i = 0; i < values().length; i++)
			{
				colors[i] = values()[i].getColor();
			}
			
			return colors;
		}
	}
	
	private static class LimitlessColor
	{
		private int r, g, b;
		
		LimitlessColor(int r, int g, int b)
		{
			this.r = r;
			this.g = g;
			this.b = b;
		}
		
		public static LimitlessColor fromColor(Color color)
		{
			return new LimitlessColor(color.getRed(), color.getGreen(), color.getBlue());
		}
		
		public static LimitlessColor addColors(LimitlessColor... colors)
		{
			int r = colors[0].r, g = colors[0].g, b = colors[0].b;
			
			for (int i = 1; i < colors.length; i++)
			{
				r += colors[i].getRed();
				g += colors[i].getGreen();
				b += colors[i].getBlue();
			}
			
			return new LimitlessColor(r, g, b);
		}
		
		public static LimitlessColor subtractColors(LimitlessColor... colors)
		{
			int r = colors[0].r, g = colors[0].g, b = colors[0].b;
			
			for (int i = 1; i < colors.length; i++)
			{
				r -= colors[i].getRed();
				g -= colors[i].getGreen();
				b -= colors[i].getBlue();
			}
			
			return new LimitlessColor(r, g, b);
		}
		
		public void divideBy(int number)
		{
			this.r /= number;
			this.g /= number;
			this.b /= number;
		}
		
		public void squareColor()
		{
			this.r = (int) Math.pow(r, 2);
			this.g = (int) Math.pow(g, 2);
			this.b = (int) Math.pow(b, 2);
		}
		
		public void toSquareRoot()
		{
			this.r = (int) Math.sqrt(r);
			this.g = (int) Math.sqrt(g);
			this.b = (int) Math.sqrt(b);
		}
		
		public int getRed() {return r;}
		
		public int getGreen() {return g;}
		
		public int getBlue() {return b;}
		
		public Color getColor()
		{
			if (r < 0 || g < 0 || b < 0 || r > 255 || g > 255 || b > 255)
			{
				System.err.println("Error: Your color is still limitless. Cannot convert from invalid color ranges.");
				
				System.err.println("r = " + r + ", g = " + g + ", b = " + b);
				
				return null;
			}
			
			return new Color(r, g, b);
		}
		
		public static LimitlessColor getLimitlessAverage(LimitlessColor... colors)
		{
			int r = 0, g = 0, b = 0;
			
			for (LimitlessColor color : colors)
			{
				r += color.getRed();
				g += color.getGreen();
				b += color.getBlue();
			}
			
			r /= colors.length;
			g /= colors.length;
			b /= colors.length;
			
			return new LimitlessColor(r, g, b);
		}
	}
}

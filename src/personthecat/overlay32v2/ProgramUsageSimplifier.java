package personthecat.overlay32v2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ProgramUsageSimplifier
{
	public static List<String> getOreTextureLocations(Scanner scanner)
	{
		List<String> overlays = new ArrayList<>();		
		
		System.out.println("Run in automatic mode (use all textures found in /ore_textures/? (Y/n):");
		
		if (scanner.nextLine().equals("n"))
		{
			System.out.println("\nLast ore texture: " + MakeLifeEasier.getLastOreTexture());
			System.out.print("Enter the original ore texture (leave blank to reuse last ore texture): ");
			
			String oreTextureLocation = scanner.nextLine().replaceAll("\"", "").replace("\\", "/");
			
			if (oreTextureLocation.isEmpty()) oreTextureLocation = MakeLifeEasier.getLastOreTexture();
			
			MakeLifeEasier.saveLastOreTexture(oreTextureLocation);
			
			overlays.add(oreTextureLocation);
			
			System.out.println();
		}
		
		else
		{
			new File(System.getProperty("user.dir") + "/output").mkdirs();
			
			for (File file : new File(System.getProperty("user.dir") + "/ore_textures").listFiles())
			{
				if (file.isFile() && file.getName().endsWith(".png")) overlays.add(file.toString());
			}
		}

		if (overlays.size() < 1) System.err.println("Unable to find any textures...");
		
		return overlays;
	}
	
	public static String getBackgroundLocation(Scanner scanner)
	{
		System.out.println("Last background: " + MakeLifeEasier.getLastBackground());
		System.out.print("Enter a suitable background texture (leave blank to reuse last background): ");
		
		String backgroundLocation = scanner.nextLine().replaceAll("\"", "").replace("\\", "/");
		
		if (backgroundLocation.isEmpty()) backgroundLocation = MakeLifeEasier.getLastBackground();
		
		MakeLifeEasier.saveLastBackground(backgroundLocation);

		return backgroundLocation;
	}
	
	public static boolean useFancyTextures(Scanner scanner)
	{
		System.out.print("\nUse blended overlays? (Y/n): ");

		if (scanner.nextLine().equals("n")) return false;
		
		else return true;
	}
}

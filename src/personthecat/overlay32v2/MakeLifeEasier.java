package personthecat.overlay32v2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class MakeLifeEasier 
{
	static File txt = new File(System.getProperty("user.dir") + "/recent_textures.txt");
	
	static String previousContents;
	static String background, oreTexture;
	
	static {reset();}
	
	private static void reset()
	{
		try
		{
			previousContents = new String(Files.readAllBytes(txt.toPath()), StandardCharsets.UTF_8);
		}
		
		catch (IOException e) {previousContents = "No background found,No overlay found";}
		
		background = previousContents.split(",")[0];
		oreTexture = previousContents.split(",")[1];
	}
	
	public static void saveLastBackground(String location)
	{
		writeToFile(txt, previousContents.replaceAll(background, location));
		
		reset();
	}
	
	public static String getLastBackground()
	{
		return background;
	}
	
	public static void saveLastOreTexture(String location)
	{
		writeToFile(txt, previousContents.replaceAll(oreTexture, location));
		
		reset();
	}
	
	public static String getLastOreTexture()
	{
		return oreTexture;
	}
	
	private static void writeToFile(File file, String line)
	{
		try
		{
			FileWriter writer = new FileWriter(file);
			
			writer.write(line);				
			
			writer.close();
		}
		
		catch (IOException e) {System.err.println("Could not write new file");}
	}
}

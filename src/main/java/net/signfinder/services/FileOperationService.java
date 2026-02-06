package net.signfinder.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.fabricmc.loader.api.FabricLoader;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.core.AutoSaveMode;
import net.signfinder.managers.AutoSaveManager.SavedSignData;

/**
 * Handles file I/O operations for auto-save functionality.
 * Responsible for reading/writing detection data to disk.
 */
public class FileOperationService implements DataPersistenceService
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(FileOperationService.class);
	
	private final Gson gson;
	
	public FileOperationService()
	{
		gson = new GsonBuilder().setPrettyPrinting().create();
	}
	
	/**
	 * Gets the auto-save directory for the current world save.
	 * Creates the directory if it doesn't exist.
	 *
	 * @return Path to saves/[world_name]/signfinder/autosave/
	 */
	private Path getAutoSaveDir()
	{
		Minecraft client = Minecraft.getInstance();
		Path gameDir = FabricLoader.getInstance().getGameDir();
		
		// Try to get current world name
		String worldName = client.name();
		
		// Use world-specific directory: saves/[world_name]/signfinder/autosave/
		Path autoSaveDir = gameDir.resolve("saves").resolve(worldName)
			.resolve("signfinder").resolve("autosave");
		
		try
		{
			Files.createDirectories(autoSaveDir);
		}catch(IOException e)
		{
			LOGGER.error("Failed to create autosave directory {}: {}",
				autoSaveDir, e.getMessage());
		}
		
		return autoSaveDir;
	}
	
	/**
	 * Sanitizes a string to be safe for use as a filename.
	 */
	private String sanitizeFileName(String name)
	{
		if(name == null)
		{
			return "unknown";
		}
		
		// Replace invalid characters with underscore
		return name.replaceAll("[<>:\"/\\\\|?*]", "_").replaceAll("\\s+", "_") // Replace
																				// spaces
																				// with
																				// underscore
			.toLowerCase();
	}
	
	@Override
	public boolean saveDetectionData(Map<String, List<SavedSignData>> data)
	{
		SignFinderConfig config = SignFinderMod.getInstance().getConfig();
		String filename = generateFilename(config.auto_save_mode);
		Path autoSaveDir = getAutoSaveDir();
		Path saveFile = autoSaveDir.resolve(filename);
		
		try
		{
			String json = gson.toJson(data);
			Files.writeString(saveFile, json, StandardCharsets.UTF_8);
			LOGGER.debug("Saved detection data to: {}", saveFile);
			return true;
		}catch(IOException e)
		{
			LOGGER.error("Failed to save auto-detection data to {}: {}",
				saveFile, e.getMessage());
			return false;
		}
	}
	
	@Override
	public Map<String, List<SavedSignData>> loadDetectionData()
	{
		SignFinderConfig config = SignFinderMod.getInstance().getConfig();
		String filename = generateFilename(config.auto_save_mode);
		Path autoSaveDir = getAutoSaveDir();
		Path saveFile = autoSaveDir.resolve(filename);
		
		if(!Files.exists(saveFile))
		{
			LOGGER.debug("No auto-save file found: {}", saveFile);
			return Map.of();
		}
		
		try
		{
			String json = Files.readString(saveFile, StandardCharsets.UTF_8);
			TypeToken<Map<String, List<SavedSignData>>> token =
				new TypeToken<>()
				{};
			Map<String, List<SavedSignData>> result =
				gson.fromJson(json, token.getType());
			LOGGER.debug("Loaded detection data from: {}", saveFile);
			return result != null ? result : Map.of();
		}catch(IOException e)
		{
			LOGGER.error("Failed to load auto-detection data from {}: {}",
				saveFile, e.getMessage());
			return Map.of();
		}
	}
	
	@Override
	public String getSaveDirectory()
	{
		return getAutoSaveDir().toString();
	}
	
	@Override
	public boolean hasSavedData()
	{
		SignFinderConfig config = SignFinderMod.getInstance().getConfig();
		String filename = generateFilename(config.auto_save_mode);
		Path autoSaveDir = getAutoSaveDir();
		Path saveFile = autoSaveDir.resolve(filename);
		return Files.exists(saveFile);
	}
	
	private String generateFilename(AutoSaveMode mode)
	{
		return switch(mode)
		{
			case AUTO_OVERWRITE -> "auto_detected_signs.json";
			case NEW_FILE -> "auto_detected_signs_" + System.currentTimeMillis()
				+ ".json";
			case DAILY_SPLIT -> "auto_detected_signs_" + LocalDate.now()
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".json";
		};
	}
}

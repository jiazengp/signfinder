package net.signfinder.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.models.SignSearchResult;
import net.signfinder.managers.AutoSaveManager.SavedSignData;
import net.signfinder.services.DataValidationService.ValidationResult;

/**
 * Manages local cached sign data in memory.
 * Handles adding, removing, and maintaining cached detection results.
 */
public class LocalDataCacheService
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(LocalDataCacheService.class);
	
	private final Map<String, Map<BlockPos, SignSearchResult>> detectedSigns =
		new ConcurrentHashMap<>();
	private final DataValidationService validationService;
	private final DataPersistenceService persistenceService;
	
	private boolean hasNewData = false;
	
	public LocalDataCacheService(DataValidationService validationService,
		DataPersistenceService persistenceService)
	{
		this.validationService = validationService;
		this.persistenceService = persistenceService;
	}
	
	/**
	 * Add a detected sign to the cache.
	 */
	public void addDetectedSign(SignSearchResult result)
	{
		String worldKey = getCurrentWorldKey();
		Map<BlockPos, SignSearchResult> worldData = detectedSigns
			.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>());
		
		// Check if we already have this sign with the same content to avoid
		// unnecessary saves
		SignSearchResult existing = worldData.get(result.getPos());
		if(existing != null
			&& existing.getMatchedText().equals(result.getMatchedText()))
		{
			return; // No changes, skip adding
		}
		
		worldData.put(result.getPos(), result);
		hasNewData = true;
		LOGGER.debug("Added detected sign at position: {}", result.getPos());
	}
	
	/**
	 * Remove a detected sign from the cache.
	 */
	public void removeDetectedSign(BlockPos pos)
	{
		String worldKey = getCurrentWorldKey();
		Map<BlockPos, SignSearchResult> worldData = detectedSigns.get(worldKey);
		if(worldData != null)
		{
			SignSearchResult removed = worldData.remove(pos);
			if(removed != null)
			{
				hasNewData = true;
				LOGGER.debug("Removed cached sign at position: {}", pos);
			}
		}
	}
	
	/**
	 * Check if auto-save is needed and perform save operation.
	 */
	public void checkAndSave()
	{
		MinecraftClient client = MinecraftClient.getInstance();
		if(client.world == null || client.player == null)
			return;
		
		SignFinderConfig config = SignFinderMod.getInstance().getConfig();
		if(!config.auto_save_detection_data)
			return;
		
		if(!hasNewData)
			return;
		
		String worldKey = getCurrentWorldKey();
		Map<BlockPos, SignSearchResult> worldData = detectedSigns.get(worldKey);
		if(worldData == null || worldData.isEmpty())
			return;
		
		// Convert memory data to save format
		List<SavedSignData> dataToSave = worldData.values().stream()
			.map(this::convertToSavedData).collect(Collectors.toList());
		
		// Combine with existing data from other worlds
		Map<String, List<SavedSignData>> allData =
			new ConcurrentHashMap<>(persistenceService.loadDetectionData());
		allData.put(worldKey, dataToSave);
		
		// Save to file
		if(persistenceService.saveDetectionData(allData))
		{
			hasNewData = false;
			LOGGER.debug("Auto-saved {} detected signs for world: {}",
				dataToSave.size(), worldKey);
		}else
		{
			LOGGER.warn("Failed to auto-save detected signs");
		}
	}
	
	/**
	 * Get all local data for the current world.
	 */
	public List<SignSearchResult> getLocalData()
	{
		Map<String, List<SavedSignData>> savedData =
			persistenceService.loadDetectionData();
		String currentWorldKey = getCurrentWorldKey();
		List<SavedSignData> worldData =
			savedData.getOrDefault(currentWorldKey, List.of());
		
		return worldData.stream().map(this::convertFromSavedData)
			.collect(Collectors.toList());
	}
	
	/**
	 * Clean up cached local data by validating against current world state.
	 */
	public void cleanupCachedLocalData()
	{
		MinecraftClient client = MinecraftClient.getInstance();
		if(client.world == null || client.player == null)
			return;
		
		SignFinderConfig config = SignFinderMod.getInstance().getConfig();
		if(!config.auto_save_detection_data)
			return;
		
		List<SignSearchResult> localData = getLocalData();
		if(localData.isEmpty())
			return;
		
		Map<String, List<SavedSignData>> updatedData;
		boolean hasChanges = false;
		
		String currentWorldKey = getCurrentWorldKey();
		List<SavedSignData> worldData = new ArrayList<>();
		
		LOGGER.debug("Cleaning up {} local data entries", localData.size());
		
		for(SignSearchResult localResult : localData)
		{
			ValidationResult validation =
				validationService.validateSignAtPosition(client.world,
					localResult.getPos(), localResult);
			
			LOGGER.debug("Position {} validation status: {}",
				localResult.getPos(), validation.status());
			
			switch(validation.status())
			{
				case VALID:
				worldData.add(convertToSavedData(localResult));
				break;
				case MODIFIED:
				if(validation.updatedResult() != null)
				{
					worldData
						.add(convertToSavedData(validation.updatedResult()));
					hasChanges = true;
					LOGGER.debug("Updated data for position {}",
						localResult.getPos());
				}
				break;
				case REMOVED:
				hasChanges = true;
				LOGGER.debug("Removed data for position {}",
					localResult.getPos());
				break;
			}
		}
		
		if(hasChanges)
		{
			updatedData = Map.of(currentWorldKey, worldData);
			persistenceService.saveDetectionData(updatedData);
			LOGGER.debug("Saved updated local data with {} entries",
				worldData.size());
		}else
		{
			LOGGER.debug("No changes detected in local data");
		}
	}
	
	/**
	 * Validate cached memory data against current world state.
	 */
	public void validateCachedMemoryData()
	{
		MinecraftClient client = MinecraftClient.getInstance();
		if(client.world == null || client.player == null)
			return;
		
		String worldKey = getCurrentWorldKey();
		Map<BlockPos, SignSearchResult> worldData = detectedSigns.get(worldKey);
		if(worldData == null || worldData.isEmpty())
			return;
		
		List<BlockPos> toRemove = new ArrayList<>();
		List<BlockPos> toUpdate = new ArrayList<>();
		
		for(Map.Entry<BlockPos, SignSearchResult> entry : worldData.entrySet())
		{
			BlockPos pos = entry.getKey();
			SignSearchResult cachedResult = entry.getValue();
			
			ValidationResult validation = validationService
				.validateSignAtPosition(client.world, pos, cachedResult);
			
			switch(validation.status())
			{
				case REMOVED:
				toRemove.add(pos);
				break;
				case MODIFIED:
				toUpdate.add(pos);
				break;
				case VALID:
				break;
			}
		}
		
		// Remove deleted signs
		for(BlockPos pos : toRemove)
		{
			worldData.remove(pos);
			hasNewData = true;
		}
		
		// Update modified signs
		for(BlockPos pos : toUpdate)
		{
			updateSignData(pos, client);
		}
		
		LOGGER.debug("Memory validation completed: {} removed, {} updated",
			toRemove.size(), toUpdate.size());
	}
	
	private void updateSignData(BlockPos pos, MinecraftClient client)
	{
		try
		{
			if(client.world == null)
				return;
			String[] currentText = net.signfinder.util.SignTextUtils
				.getSignText(client.world, pos);
			if(currentText == null)
				return;
			
			SignSearchResult updatedResult = new SignSearchResult(pos,
				client.player.getEntityPos(), currentText,
				String.join(" ", currentText),
				SignFinderMod.getInstance().getConfig().text_preview_length);
			
			String worldKey = getCurrentWorldKey();
			detectedSigns
				.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>())
				.put(pos, updatedResult);
			hasNewData = true;
		}catch(Exception e)
		{
			LOGGER.warn("Failed to update sign data at {}: {}", pos,
				e.getMessage());
		}
	}
	
	private SavedSignData convertToSavedData(SignSearchResult result)
	{
		SavedSignData data = new SavedSignData();
		data.x = result.getPos().getX();
		data.y = result.getPos().getY();
		data.z = result.getPos().getZ();
		data.signText = result.getSignText();
		data.matchedText = result.getMatchedText();
		data.updateTime = System.currentTimeMillis();
		return data;
	}
	
	private SignSearchResult convertFromSavedData(SavedSignData data)
	{
		BlockPos pos = new BlockPos(data.x, data.y, data.z);
		MinecraftClient client = MinecraftClient.getInstance();
		
		return new SignSearchResult(pos,
			client.player != null ? client.player.getEntityPos()
				: pos.toCenterPos(),
			data.signText, data.matchedText,
			SignFinderMod.getInstance().getConfig().text_preview_length);
	}
	
	/**
	 * Gets the current world dimension key for data grouping.
	 * Automatically handles any dimension including custom modded dimensions.
	 */
	private String getCurrentWorldKey()
	{
		MinecraftClient client = MinecraftClient.getInstance();
		if(client.world == null)
			return "unknown";
		
		RegistryKey<World> worldKey = client.world.getRegistryKey();
		return getDimensionKey(worldKey);
	}
	
	/**
	 * Converts a world registry key to a safe string identifier.
	 * Handles vanilla dimensions with friendly names and custom dimensions with
	 * full identifiers.
	 */
	private String getDimensionKey(RegistryKey<World> worldKey)
	{
		if(worldKey == null)
			return "unknown";
		
		// Handle common vanilla dimensions with friendly names
		if(worldKey == World.OVERWORLD)
			return "overworld";
		else if(worldKey == World.NETHER)
			return "nether";
		else if(worldKey == World.END)
			return "end";
		else
		{
			// For modded/custom dimensions, use the full identifier
			String dimensionId = worldKey.getValue().toString();
			
			// Sanitize the dimension ID for safe file system usage
			return sanitizeDimensionKey(dimensionId);
		}
	}
	
	/**
	 * Sanitizes dimension keys to be safe for use in file operations and data
	 * structures.
	 */
	private String sanitizeDimensionKey(String dimensionId)
	{
		if(dimensionId == null || dimensionId.isEmpty())
			return "unknown";
		
		// Replace problematic characters with safe alternatives
		return dimensionId.replaceAll(":", "_") // Replace namespace separator
			.replaceAll("[/\\\\]", "_") // Replace path separators
			.replaceAll("[<>:\"|?*]", "_") // Replace illegal filename
											// characters
			.replaceAll("\\s+", "_") // Replace whitespace with underscore
			.toLowerCase(); // Normalize to lowercase
	}
}

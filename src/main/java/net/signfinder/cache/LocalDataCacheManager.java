package net.signfinder.cache;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.signfinder.SignFinderConfig;
import net.signfinder.models.EntitySearchResult;
import net.signfinder.models.SignSearchResult;
import net.signfinder.managers.AutoSaveManager;
import net.signfinder.search.SearchQueryProcessor;
import net.signfinder.services.SearchQuery;
import net.signfinder.SignFinderMod;

/**
 * Manages local cached data separately from search operations.
 * Handles validation, merging, and cleanup of local detection cache.
 */
public class LocalDataCacheManager
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(LocalDataCacheManager.class);
	
	private final AutoSaveManager autoSaveManager;
	private final SearchQueryProcessor queryProcessor;
	
	public LocalDataCacheManager(AutoSaveManager autoSaveManager,
		SearchQueryProcessor queryProcessor)
	{
		this.autoSaveManager = autoSaveManager;
		this.queryProcessor = queryProcessor;
	}
	
	/**
	 * Gets local cached data that matches the search criteria and is within
	 * range.
	 * This method is called by search service to supplement live search
	 * results.
	 */
	public List<EntitySearchResult> getMatchingLocalData(SearchQuery query,
		SignFinderConfig config, Vec3 playerPos)
	{
		List<SignSearchResult> localData = autoSaveManager.getLocalData();
		
		return localData.stream()
			.filter(result -> isWithinSearchRadius(result.getPos(), playerPos,
				query.radius()))
			.filter(result -> matchesSearchQuery(result, query, config))
			.map(result -> convertToEntityResult(result, playerPos, config))
			.toList();
	}
	
	/**
	 * Adds a new detection result to local cache.
	 */
	public void addToLocalCache(SignSearchResult result)
	{
		try
		{
			autoSaveManager.addDetectedSign(result);
		}catch(Exception e)
		{
			LOGGER.warn("Failed to add to local cache", e);
		}
	}
	
	/**
	 * Removes a detection result from local cache.
	 */
	public void removeFromLocalCache(BlockPos pos)
	{
		try
		{
			autoSaveManager.removeDetectedSign(pos);
		}catch(Exception e)
		{
			LOGGER.warn("Failed to remove from local cache", e);
		}
	}
	
	/**
	 * Updates an existing entry in local cache with new data.
	 */
	public void updateLocalCache(BlockPos pos, SignSearchResult updatedResult)
	{
		try
		{
			// Remove old entry first
			autoSaveManager.removeDetectedSign(pos);
			// Add updated entry
			autoSaveManager.addDetectedSign(updatedResult);
			
			LOGGER.debug("Updated local cache for position: {}", pos);
		}catch(Exception e)
		{
			LOGGER.warn("Failed to update local cache for position {}: {}", pos,
				e.getMessage());
		}
	}
	
	/**
	 * Performs auto-save if needed.
	 */
	public void performAutoSave()
	{
		try
		{
			autoSaveManager.checkAndSave();
		}catch(Exception e)
		{
			LOGGER.warn("Failed to perform auto-save", e);
		}
	}
	
	/**
	 * Gets all local cached data for the current world.
	 */
	public List<SignSearchResult> getAllLocalData()
	{
		try
		{
			return autoSaveManager.getLocalData();
		}catch(Exception e)
		{
			LOGGER.warn("Failed to get local data", e);
			return List.of();
		}
	}
	
	/**
	 * Gets all local cached data within the specified range from player
	 * position.
	 * This is used for full range updates regardless of search criteria.
	 */
	public List<EntitySearchResult> getAllLocalDataInRange(Vec3 playerPos,
                                                           int radius)
	{
		try
		{
			List<SignSearchResult> allLocalData =
				autoSaveManager.getLocalData();
			
			return allLocalData.stream()
				.filter(result -> isWithinSearchRadius(result.getPos(),
					playerPos, radius))
				.map(result -> convertToEntityResult(result, playerPos,
					getDefaultConfig()))
				.toList();
		}catch(Exception e)
		{
			LOGGER.warn("Failed to get local data in range", e);
			return List.of();
		}
	}
	
	private boolean isWithinSearchRadius(BlockPos pos, Vec3 playerPos,
		int radius)
	{
		double distance = Math.sqrt(pos.distToCenterSqr(playerPos));
		return distance <= radius;
	}
	
	private boolean matchesSearchQuery(SignSearchResult result,
		SearchQuery query, SignFinderConfig config)
	{
		String combinedText = String.join(" ", result.getSignText());
		return queryProcessor.matches(combinedText, query, config);
	}
	
	private EntitySearchResult convertToEntityResult(SignSearchResult result,
		Vec3 playerPos, SignFinderConfig config)
	{
		return new EntitySearchResult(result.getPos(), playerPos,
			result.getSignText(), result.getMatchedText(),
			config.text_preview_length, result.getUpdateTime());
	}
	
	private SignFinderConfig getDefaultConfig()
	{
		try
		{
			return SignFinderMod.getInstance().getConfig();
		}catch(Exception e)
		{
			// Fallback - create a minimal config for text preview length
			SignFinderConfig config = new SignFinderConfig();
			return config;
		}
	}
}

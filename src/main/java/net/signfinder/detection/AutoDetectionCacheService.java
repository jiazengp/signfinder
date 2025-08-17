package net.signfinder.detection;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.signfinder.SignFinderConfig;
import net.signfinder.models.SignSearchResult;
import net.signfinder.cache.LocalDataCacheManager;

/**
 * Dedicated service for managing auto-detection cache operations.
 * Handles adding/removing detected signs and periodic maintenance.
 */
public class AutoDetectionCacheService
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(AutoDetectionCacheService.class);
	
	private final LocalDataCacheManager localDataManager;
	private final MinecraftClient mc;
	
	public AutoDetectionCacheService(LocalDataCacheManager localDataManager)
	{
		this.localDataManager = localDataManager;
		this.mc = MinecraftClient.getInstance();
	}
	
	/**
	 * Adds a newly detected sign to the auto-detection cache.
	 *
	 * @param result
	 *            The detected sign result
	 * @param config
	 *            Current configuration
	 */
	public void addDetectedSign(SignSearchResult result,
		SignFinderConfig config)
	{
		if(!config.auto_save_detection_data)
		{
			return;
		}
		
		try
		{
			localDataManager.addToLocalCache(result);
			LOGGER.debug("Added detected sign at {} to cache", result.getPos());
		}catch(Exception e)
		{
			LOGGER.warn("Failed to add detected sign to cache", e);
		}
	}
	
	/**
	 * Removes a sign from the auto-detection cache.
	 *
	 * @param pos
	 *            Position of the sign to remove
	 * @param config
	 *            Current configuration
	 */
	public void removeDetectedSign(BlockPos pos, SignFinderConfig config)
	{
		if(!config.auto_save_detection_data)
		{
			return;
		}
		
		try
		{
			localDataManager.removeFromLocalCache(pos);
			LOGGER.debug("Removed detected sign at {} from cache", pos);
		}catch(Exception e)
		{
			LOGGER.warn("Failed to remove detected sign from cache", e);
		}
	}
	
	/**
	 * Removes nearby detected signs based on player proximity.
	 *
	 * @param config
	 *            Current configuration including removal distance
	 */
	public void removeNearbyDetectedSigns(SignFinderConfig config)
	{
		if(!config.auto_save_detection_data || mc.player == null)
		{
			return;
		}
		
		Vec3d playerPos = mc.player.getPos();
		double removalDistanceSq =
			config.auto_removal_distance * config.auto_removal_distance;
		
		List<SignSearchResult> localData = localDataManager.getAllLocalData();
		
		for(SignSearchResult result : localData)
		{
			BlockPos pos = result.getPos();
			double distanceSq = pos.getSquaredDistance(playerPos);
			
			if(distanceSq <= removalDistanceSq)
			{
				removeDetectedSign(pos, config);
			}
		}
	}
	
	/**
	 * Performs periodic maintenance of the auto-detection cache.
	 *
	 * @param config
	 *            Current configuration
	 */
	public void performMaintenance(SignFinderConfig config)
	{
		if(!config.auto_save_detection_data)
		{
			return;
		}
		
		try
		{
			// Auto-save functionality disabled - no longer perform automatic
			// saves
			localDataManager.performAutoSave();
			LOGGER.debug("Auto-detection cache maintenance completed");
		}catch(Exception e)
		{
			LOGGER.warn("Failed to perform auto-detection cache maintenance",
				e);
		}
	}
	
	/**
	 * Gets all cached detection results for display/debugging.
	 *
	 * @return List of all cached detection results
	 */
	public List<SignSearchResult> getAllCachedDetections()
	{
		return localDataManager.getAllLocalData();
	}
	
	/**
	 * Clears all cached detection data.
	 */
	public void clearAllCachedDetections()
	{
		List<SignSearchResult> allData = localDataManager.getAllLocalData();
		
		for(SignSearchResult result : allData)
		{
			localDataManager.removeFromLocalCache(result.getPos());
		}
		
		LOGGER.info("Cleared all cached detection data");
	}
}

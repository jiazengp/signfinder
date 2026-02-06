package net.signfinder.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.signfinder.SignFinderConfig;
import net.signfinder.models.EntitySearchResult;
import net.signfinder.models.SignSearchResult;
import net.signfinder.cache.LocalDataCacheManager;
import net.signfinder.cache.SignDataCache;
import net.signfinder.cache.SignDataCache.SignData;
import net.signfinder.services.SearchQuery;
import net.signfinder.services.SearchService;
import net.signfinder.util.ChunkUtils;
import net.signfinder.util.ItemFrameUtils;
import java.util.Arrays;

/**
 * Main implementation of search service for finding entities.
 * Handles both signs and item frames with unified search logic.
 */
public class EntitySearchService implements SearchService
{
	private final Minecraft mc;
	private final SignDataCache signCache;
	private final SearchQueryProcessor queryProcessor;
	private final LocalDataCacheManager localDataManager;
	
	public EntitySearchService(SignDataCache signCache,
		SearchQueryProcessor queryProcessor,
		LocalDataCacheManager localDataManager)
	{
		this.mc = Minecraft.getInstance();
		this.signCache = signCache;
		this.queryProcessor = queryProcessor;
		this.localDataManager = localDataManager;
	}
	
	@Override
	public List<EntitySearchResult> searchEntities(SearchQuery query,
		SignFinderConfig config)
	{
		if(mc.player == null)
		{
			return List.of();
		}
		
		Vec3 playerPos = mc.player.position();
		Map<BlockPos, EntitySearchResult> liveResultMap = new HashMap<>();
		
		// Search loaded entities first
		searchLoadedEntities(query, config, playerPos, liveResultMap);
		
		// Perform full range update of local cache if auto-save is enabled
		if(config.auto_save_detection_data)
		{
			performFullRangeUpdate(query.radius(), playerPos, config,
				liveResultMap);
		}
		
		// Combine live results and local cached data with proper ordering
		
		return combineResultsWithLocalData(liveResultMap, query, config,
			playerPos);
	}
	
	@Override
	public List<SignSearchResult> findAllSigns(Vec3 playerPos, int radius)
	{
		List<SignSearchResult> results = new ArrayList<>();
		List<SignBlockEntity> signs = findSignsInRadius(playerPos, radius);
		
		for(SignBlockEntity sign : signs)
		{
			Optional<SignData> signData = getOrCacheSignData(sign);
			signData.ifPresent(
				data -> results.add(new SignSearchResult(sign.getBlockPos(),
					playerPos, data.lines(), data.combinedText(), 100)));
		}
		
		results.sort(Comparator.comparingDouble(SignSearchResult::getDistance));
		return results;
	}
	
	@Override
	public void clearCaches()
	{
		signCache.clear();
	}
	
	@Override
	public void performPeriodicCleanup()
	{
		signCache.cleanExpired();
	}
	
	private void searchLoadedEntities(SearchQuery query,
		SignFinderConfig config, Vec3 playerPos,
		Map<BlockPos, EntitySearchResult> resultMap)
	{
		// Search signs if enabled
		if(config.entity_search_range.includesSigns())
		{
			searchSigns(query, config, playerPos, resultMap);
		}
		
		// Search item frames if enabled
		if(config.entity_search_range.includesItemFrames())
		{
			searchItemFrames(query, config, playerPos, resultMap);
		}
	}
	
	private void searchSigns(SearchQuery query, SignFinderConfig config,
		Vec3 playerPos, Map<BlockPos, EntitySearchResult> resultMap)
	{
		List<SignBlockEntity> signs =
			findSignsInRadius(playerPos, query.radius());
		
		for(SignBlockEntity sign : signs)
		{
			Optional<SignData> signData = getOrCacheSignData(sign);
			if(signData.isEmpty())
				continue;
			
			SignData data = signData.get();
			if(queryProcessor.matches(data.combinedText(), query, config))
			{
				EntitySearchResult result =
					new EntitySearchResult(sign, playerPos, data.lines(),
						data.combinedText(), config.text_preview_length);
				resultMap.put(sign.getBlockPos(), result);
			}
		}
	}
	
	private void searchItemFrames(SearchQuery query, SignFinderConfig config,
		Vec3 playerPos, Map<BlockPos, EntitySearchResult> resultMap)
	{
		List<ItemFrame> itemFrames =
			findItemFramesInRadius(playerPos, query.radius());
		
		for(ItemFrame itemFrame : itemFrames)
		{
			if(!ItemFrameUtils.hasItem(itemFrame))
				continue;
			
			String itemName = ItemFrameUtils.getItemFrameItemName(itemFrame,
				query.caseSensitive());
			if(!itemName.isEmpty()
				&& queryProcessor.matches(itemName, query, config))
			{
				EntitySearchResult result = new EntitySearchResult(itemFrame,
					playerPos, itemName, itemName, config.text_preview_length);
				resultMap.put(itemFrame.getPos(), result);
			}
		}
	}
	
	private List<SignBlockEntity> findSignsInRadius(Vec3 center, int radius)
	{
		List<SignBlockEntity> signs = new ArrayList<>();
		double radiusSq = radius * radius;
		
		ChunkUtils.getLoadedBlockEntities().forEach(blockEntity -> {
			if(blockEntity instanceof SignBlockEntity signEntity)
			{
				Vec3 signPos = Vec3.atCenterOf(signEntity.getBlockPos());
				if(center.distanceToSqr(signPos) <= radiusSq)
				{
					signs.add(signEntity);
				}
			}
		});
		
		return signs;
	}
	
	private List<ItemFrame> findItemFramesInRadius(Vec3 center, int radius)
	{
		List<ItemFrame> itemFrames = new ArrayList<>();
		double radiusSq = radius * radius;
		
		ChunkUtils.getLoadedEntities().forEach(entity -> {
			if(entity instanceof ItemFrame itemFrame
				&& ItemFrameUtils.hasItem(itemFrame))
			{
				Vec3 framePos = Vec3.atLowerCornerOf(itemFrame.getPos());
				if(center.distanceToSqr(framePos) <= radiusSq)
				{
					itemFrames.add(itemFrame);
				}
			}
		});
		
		return itemFrames;
	}
	
	/**
	 * Performs full range update of local cache data regardless of search
	 * criteria.
	 * Updates all cached entities within the search range with current live
	 * data.
	 */
	private void performFullRangeUpdate(int radius, Vec3 playerPos,
		SignFinderConfig config,
		Map<BlockPos, EntitySearchResult> liveResultMap)
	{
		try
		{
			// Get all local data within range (regardless of search criteria)
			List<EntitySearchResult> localDataInRange =
				localDataManager.getAllLocalDataInRange(playerPos, radius);
			
			for(EntitySearchResult localResult : localDataInRange)
			{
				BlockPos pos = localResult.getPos();
				
				// Check if we have live data for this position
				EntitySearchResult liveResult = liveResultMap.get(pos);
				if(liveResult != null)
				{
					// We have live data, compare and update if needed
					if(shouldUpdateLocalData(liveResult, localResult))
					{
						updateLocalCacheWithLiveData(liveResult, config);
					}
				}else
				{
					// No live data found, check if the entity still exists at
					// this position
					if(shouldRemoveFromCache(pos, localResult, config))
					{
						localDataManager.removeFromLocalCache(pos);
						System.out
							.println("Removed obsolete cached data at " + pos);
					}
				}
			}
		}catch(Exception e)
		{
			// Log error but don't interrupt search
			System.err
				.println("Error during full range update: " + e.getMessage());
		}
	}
	
	/**
	 * Combines live search results with local cached data using proper
	 * prioritization.
	 * Live results are shown first (sorted by distance), followed by local
	 * cached data (also sorted by distance).
	 */
	private List<EntitySearchResult> combineResultsWithLocalData(
		Map<BlockPos, EntitySearchResult> liveResultMap, SearchQuery query,
		SignFinderConfig config, Vec3 playerPos)
	{
		
		// First, add all live results sorted by distance
		List<EntitySearchResult> liveResults =
			new ArrayList<>(liveResultMap.values());
		liveResults
			.sort(Comparator.comparingDouble(EntitySearchResult::getDistance));
		List<EntitySearchResult> finalResults = new ArrayList<>(liveResults);
		
		// Then add local cached data based on configuration
		if(config.auto_save_detection_data)
		{
			boolean shouldIncludeLocalData = config.always_include_local_data
				|| (liveResultMap.isEmpty() || query.query().trim().isEmpty());
			
			if(shouldIncludeLocalData)
			{
				List<EntitySearchResult> localResults = getFilteredLocalData(
					liveResultMap, query, config, playerPos);
				
				// Sort local results by distance
				localResults.sort(Comparator
					.comparingDouble(EntitySearchResult::getDistance));
				
				// Add local results after live results
				finalResults.addAll(localResults);
			}
		}
		
		return finalResults;
	}
	
	/**
	 * Gets local cached data that matches search criteria, excluding positions
	 * already found in live results.
	 */
	private List<EntitySearchResult> getFilteredLocalData(
		Map<BlockPos, EntitySearchResult> liveResultMap, SearchQuery query,
		SignFinderConfig config, Vec3 playerPos)
	{
		List<EntitySearchResult> localResults =
			localDataManager.getMatchingLocalData(query, config, playerPos);
		List<EntitySearchResult> filteredLocalResults = new ArrayList<>();
		
		// Filter out positions that already have live results
		for(EntitySearchResult localResult : localResults)
		{
			BlockPos pos = localResult.getPos();
			
			// Only include local data if we don't have live data for this
			// position
			if(!liveResultMap.containsKey(pos))
			{
				filteredLocalResults.add(localResult);
			}
		}
		
		return filteredLocalResults;
	}
	
	/**
	 * Checks if local cached data should be updated with live data.
	 */
	private boolean shouldUpdateLocalData(EntitySearchResult liveResult,
		EntitySearchResult localResult)
	{
		// Compare text content
		String[] liveText = liveResult.getDisplayText();
		String[] localText = localResult.getDisplayText();
		
		// Only update if text content differs
		return !Arrays.equals(liveText, localText);
	}
	
	/**
	 * Determines if an entity should be removed from cache when no live data is
	 * found.
	 * Only checks actual entity existence, not time-based factors.
	 */
	private boolean shouldRemoveFromCache(BlockPos pos,
		EntitySearchResult localResult, SignFinderConfig config)
	{
		// Only remove if we can confirm the entity is actually gone
		if(mc.player == null || mc.level == null)
		{
			return false;
		}
		
		try
		{
			// Check if chunk is loaded
			if(!mc.level.isLoaded(pos))
			{
				return false; // Chunk not loaded, don't remove (entity might
								// still exist)
			}
			
			if(localResult
				.getEntityType() == EntitySearchResult.EntityType.SIGN)
			{
				// For signs, check if there's still a sign block
				String blockName = mc.level.getBlockState(pos).getBlock()
					.toString().toLowerCase();
				boolean isSignBlock = blockName.contains("sign");
				
				if(!isSignBlock)
				{
					// Sign block is definitely gone
					return true;
				}
			}else
			{
				// For item frames, check if there's still an item frame entity
				// at this position
				List<ItemFrame> nearbyFrames =
					findItemFramesInRadius(Vec3.atCenterOf(pos), 1);
				boolean hasItemFrame = nearbyFrames.stream()
					.anyMatch(frame -> frame.getPos().equals(pos)
						&& ItemFrameUtils.hasItem(frame));
				
				if(!hasItemFrame)
				{
					// Item frame is definitely gone or empty
					return true;
				}
			}
		}catch(Exception e)
		{
			// If we get an error checking, be conservative and don't remove
			// Log at debug level since this is expected in some cases
		}
		
		return false;
	}
	
	/**
	 * Updates local cache with current live data.
	 */
	private void updateLocalCacheWithLiveData(EntitySearchResult liveResult,
		SignFinderConfig config)
	{
		try
		{
			// Create a new SignSearchResult from the live data
			SignSearchResult updatedResult = null;
			if(mc.player != null)
			{
				updatedResult = new SignSearchResult(liveResult.getPos(),
					mc.player.position(), liveResult.getDisplayText(),
					liveResult.getMatchedText(), config.text_preview_length);
			}
			
			// Update the local cache
			localDataManager.updateLocalCache(liveResult.getPos(),
				updatedResult);
			
		}catch(Exception e)
		{
			// Log error but don't throw - search should continue
			// Exception is expected in some cases during cache updates
		}
	}
	
	private Optional<SignData> getOrCacheSignData(SignBlockEntity sign)
	{
		BlockPos pos = sign.getBlockPos();
		
		// Try to get from cache first
		Optional<SignData> cached = signCache.get(pos);
		if(cached.isPresent())
		{
			return cached;
		}
		
		// Create new sign data and cache it
		SignData data = signCache.createSignData(sign);
		signCache.put(pos, data);
		
		return Optional.of(data);
	}
}

package net.signfinder.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.signfinder.models.EntitySearchResult;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.util.ChunkUtils;
import net.signfinder.util.ItemFrameUtils;
import net.signfinder.util.SignTextUtils;

public class SearchResultManager
{
	private static final MinecraftClient MC = MinecraftClient.getInstance();
	
	private final List<SignBlockEntity> searchResultSigns = new ArrayList<>();
	private final List<ItemFrameEntity> searchResultItemFrames =
		new ArrayList<>();
	private final EntityDetectionManager detectionManager;
	
	public SearchResultManager(EntityDetectionManager detectionManager)
	{
		this.detectionManager = detectionManager;
	}
	
	public void setSearchResults(List<EntitySearchResult> results)
	{
		searchResultSigns.clear();
		searchResultItemFrames.clear();
		if(MC.world == null)
			return;
		
		// Update ItemFrame position index to ensure search results can be found
		detectionManager.updateItemFrameIndex();
		Map<BlockPos, ItemFrameEntity> itemFrameIndex =
			detectionManager.getItemFramePositionIndex();
		
		for(EntitySearchResult result : results)
		{
			if(result.getEntityType() == EntitySearchResult.EntityType.SIGN)
			{
				processSignResult(result);
			}else if(result
				.getEntityType() == EntitySearchResult.EntityType.ITEM_FRAME)
			{
				processItemFrameResult(result, itemFrameIndex);
			}
		}
		
		SignFinderMod.LOGGER.info(
			"Set entity search results: {} signs, {} item frames",
			searchResultSigns.size(), searchResultItemFrames.size());
	}
	
	private void processSignResult(EntitySearchResult result)
	{
		if(MC.world.getBlockEntity(
			result.getPos()) instanceof SignBlockEntity signEntity)
		{
			if(isSignResultStillValid(signEntity, result))
			{
				searchResultSigns.add(signEntity);
			}else
			{
				SignFinderMod.LOGGER.debug(
					"Sign at {} has been modified, skipping highlight",
					result.getPos());
			}
		}else
		{
			SignFinderMod.LOGGER.debug(
				"Sign at {} no longer exists, skipping highlight",
				result.getPos());
		}
	}
	
	private void processItemFrameResult(EntitySearchResult result,
		Map<BlockPos, ItemFrameEntity> itemFrameIndex)
	{
		ItemFrameEntity itemFrame = itemFrameIndex.get(result.getPos());
		if(itemFrame != null && !itemFrame.isRemoved())
		{
			searchResultItemFrames.add(itemFrame);
		}else
		{
			SignFinderMod.LOGGER.debug(
				"Item frame at {} no longer exists or was removed, skipping highlight. Index size: {}",
				result.getPos(), itemFrameIndex.size());
			
			// Try direct lookup for fallback
			boolean foundBySearch = ChunkUtils.getLoadedEntities()
				.anyMatch(entity -> entity instanceof ItemFrameEntity frame
					&& frame.getBlockPos().equals(result.getPos())
					&& ItemFrameUtils.hasItem(frame));
			if(foundBySearch)
			{
				SignFinderMod.LOGGER.warn(
					"Item frame exists but not in index - possible timing issue");
			}
		}
	}
	
	private boolean isSignResultStillValid(SignBlockEntity signEntity,
		EntitySearchResult result)
	{
		try
		{
			SignFinderConfig config =
				SignFinderMod.getInstance().getConfigHolder().getConfig();
			String currentSignText =
				SignTextUtils.getSignText(signEntity, config.case_sensitive);
			
			String matchedText = result.getMatchedText();
			String searchText = config.case_sensitive ? currentSignText
				: currentSignText.toLowerCase();
			String queryText =
				config.case_sensitive ? matchedText : matchedText.toLowerCase();
			
			return searchText.contains(queryText);
		}catch(Exception e)
		{
			SignFinderMod.LOGGER.warn("Error validating sign result at {}: {}",
				result.getPos(), e.getMessage());
			return false;
		}
	}
	
	public void removeNearbyResults(SignFinderConfig config)
	{
		if(MC.player == null)
			return;
		
		if(searchResultSigns.isEmpty() && searchResultItemFrames.isEmpty())
			return;
		
		Vec3d playerPos = MC.player.getEntityPos();
		double removeDistanceSq =
			config.auto_removal_distance * config.auto_removal_distance;
		boolean shouldPlaySound;
		
		if(config.clear_all_highlights_on_approach)
		{
			shouldPlaySound =
				clearAllIfPlayerNearAny(playerPos, removeDistanceSq);
		}else
		{
			shouldPlaySound =
				removeIndividualNearbyResults(playerPos, removeDistanceSq);
		}
		
		if(shouldPlaySound && MC.player != null)
		{
			MC.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),
				0.5f, 2.0f);
		}
	}
	
	private boolean clearAllIfPlayerNearAny(Vec3d playerPos,
		double removeDistanceSq)
	{
		boolean playerNearAnyEntity =
			searchResultSigns.stream().anyMatch(sign -> {
				Vec3d signPos = Vec3d.ofCenter(sign.getPos());
				double distanceSq = playerPos.squaredDistanceTo(signPos);
				return distanceSq <= removeDistanceSq;
			}) || searchResultItemFrames.stream().anyMatch(itemFrame -> {
				Vec3d itemFramePos = itemFrame.getEntityPos();
				double distanceSq = playerPos.squaredDistanceTo(itemFramePos);
				return distanceSq <= removeDistanceSq;
			});
		
		if(playerNearAnyEntity && (!searchResultSigns.isEmpty()
			|| !searchResultItemFrames.isEmpty()))
		{
			searchResultSigns.clear();
			searchResultItemFrames.clear();
			return true;
		}
		return false;
	}
	
	private boolean removeIndividualNearbyResults(Vec3d playerPos,
		double removeDistanceSq)
	{
		int beforeSignSize = searchResultSigns.size();
		int beforeItemFrameSize = searchResultItemFrames.size();
		
		searchResultSigns.removeIf(sign -> {
			Vec3d signPos = Vec3d.ofCenter(sign.getPos());
			double distanceSq = playerPos.squaredDistanceTo(signPos);
			return distanceSq <= removeDistanceSq;
		});
		
		searchResultItemFrames.removeIf(itemFrame -> {
			Vec3d itemFramePos = itemFrame.getEntityPos();
			double distanceSq = playerPos.squaredDistanceTo(itemFramePos);
			return distanceSq <= removeDistanceSq;
		});
		
		int afterSignSize = searchResultSigns.size();
		int afterItemFrameSize = searchResultItemFrames.size();
		return (beforeSignSize > afterSignSize)
			|| (beforeItemFrameSize > afterItemFrameSize);
	}
	
	public void clearResults()
	{
		searchResultSigns.clear();
		searchResultItemFrames.clear();
	}
	
	public boolean removeResultByPos(int x, int y, int z)
	{
		BlockPos targetPos = new BlockPos(x, y, z);
		boolean signRemoved =
			searchResultSigns.removeIf(sign -> sign.getPos().equals(targetPos));
		boolean itemFrameRemoved = searchResultItemFrames
			.removeIf(itemFrame -> itemFrame.getBlockPos().equals(targetPos));
		return signRemoved || itemFrameRemoved;
	}
	
	public boolean hasResultAtPos(BlockPos pos)
	{
		boolean hasSign = searchResultSigns.stream()
			.anyMatch(sign -> sign.getPos().equals(pos));
		boolean hasItemFrame = searchResultItemFrames.stream()
			.anyMatch(itemFrame -> itemFrame.getBlockPos().equals(pos));
		return hasSign || hasItemFrame;
	}
	
	// Getters
	public List<SignBlockEntity> getSearchResultSigns()
	{
		return searchResultSigns;
	}
	
	public List<ItemFrameEntity> getSearchResultItemFrames()
	{
		return searchResultItemFrames;
	}
}

package net.signfinder.services;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.signfinder.models.SignSearchResult;
import net.signfinder.util.ChunkUtils;
import net.signfinder.util.ItemFrameUtils;
import net.signfinder.services.DataValidationService.ValidationResult;
import net.signfinder.services.DataValidationService.ValidationStatus;

/**
 * Service for validating entities and maintaining item frame index.
 * Handles consistency checks and entity state validation.
 */
public class EntityValidationService
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(EntityValidationService.class);
	
	private final Map<BlockPos, ItemFrame> itemFramePositionIndex =
		new HashMap<>();
	private final DataValidationService validationService;
	
	public EntityValidationService()
	{
		this.validationService = new DataValidationService();
	}
	
	/**
	 * Update the item frame position index with currently loaded entities.
	 */
	public void updateItemFrameIndex()
	{
		itemFramePositionIndex.clear();
		ChunkUtils.getLoadedEntities().forEach(entity -> {
			if(entity instanceof ItemFrame itemFrame
				&& ItemFrameUtils.hasItem(itemFrame))
			{
				itemFramePositionIndex.put(itemFrame.getPos(), itemFrame);
			}
		});
		
		LOGGER.debug("Updated item frame index with {} entries",
			itemFramePositionIndex.size());
	}
	
	/**
	 * Get item frame at specific position from index.
	 */
	public ItemFrame getItemFrameAt(BlockPos pos)
	{
		return itemFramePositionIndex.get(pos);
	}
	
	/**
	 * Get all indexed item frames.
	 */
	public Map<BlockPos, ItemFrame> getAllItemFrames()
	{
		return Map.copyOf(itemFramePositionIndex);
	}
	
	/**
	 * Validate if a sign entity is still consistent with cached data.
	 *
	 * @param signEntity
	 *            The sign entity to validate
	 * @param cachedResult
	 *            Previously cached result for comparison
	 * @return Validation result indicating current status
	 */
	public ValidationResult validateSignEntity(ItemFrame signEntity,
		SignSearchResult cachedResult)
	{
		Minecraft client = Minecraft.getInstance();
		if(client.level == null)
		{
			return new ValidationResult(ValidationStatus.VALID, null);
		}
		
		return validationService.validateSignAtPosition(client.level,
			signEntity.getPos(), cachedResult);
	}
	
	/**
	 * Validate if an item frame entity is still valid and accessible.
	 *
	 * @param itemFrame
	 *            The item frame entity to validate
	 * @return true if item frame is still valid
	 */
	public boolean validateItemFrame(ItemFrame itemFrame)
	{
		try
		{
			// Check if item frame still exists and has an item
			return !itemFrame.isRemoved() && ItemFrameUtils.hasItem(itemFrame);
		}catch(Exception e)
		{
			LOGGER.debug("Item frame validation failed at {}: {}",
				itemFrame.getPos(), e.getMessage());
			return false;
		}
	}
	
	/**
	 * Validate all entities in memory cache against current world state.
	 * This removes entities that no longer exist or have changed significantly.
	 */
	public EntityValidationResult validateAllCachedEntities(
		Map<BlockPos, SignSearchResult> cachedSigns,
		Map<BlockPos, ItemFrame> cachedItemFrames)
	{
		Minecraft client = Minecraft.getInstance();
		if(client.level == null)
		{
			return new EntityValidationResult(Map.of(), Map.of());
		}
		
		Map<BlockPos, SignSearchResult> validSigns = new HashMap<>();
		Map<BlockPos, ItemFrame> validItemFrames = new HashMap<>();
		
		// Validate cached signs
		for(Map.Entry<BlockPos, SignSearchResult> entry : cachedSigns
			.entrySet())
		{
			BlockPos pos = entry.getKey();
			SignSearchResult cachedResult = entry.getValue();
			
			ValidationResult validation = validationService
				.validateSignAtPosition(client.level, pos, cachedResult);
			
			switch(validation.status())
			{
				case VALID:
				validSigns.put(pos, cachedResult);
				break;
				case MODIFIED:
				if(validation.updatedResult() != null)
				{
					validSigns.put(pos, validation.updatedResult());
				}
				break;
				case REMOVED:
				LOGGER.debug("Removed invalid sign at {}", pos);
				break;
			}
		}
		
		// Validate cached item frames
		for(Map.Entry<BlockPos, ItemFrame> entry : cachedItemFrames.entrySet())
		{
			BlockPos pos = entry.getKey();
			ItemFrame itemFrame = entry.getValue();
			
			if(validateItemFrame(itemFrame))
			{
				validItemFrames.put(pos, itemFrame);
			}else
			{
				LOGGER.debug("Removed invalid item frame at {}", pos);
			}
		}
		
		LOGGER.debug(
			"Validation completed: {}/{} signs valid, {}/{} item frames valid",
			validSigns.size(), cachedSigns.size(), validItemFrames.size(),
			cachedItemFrames.size());
		
		return new EntityValidationResult(validSigns, validItemFrames);
	}
	
	/**
	 * Result class for entity validation operations.
	 */
	public static class EntityValidationResult
	{
		public final Map<BlockPos, SignSearchResult> validSigns;
		public final Map<BlockPos, ItemFrame> validItemFrames;
		
		public EntityValidationResult(
			Map<BlockPos, SignSearchResult> validSigns,
			Map<BlockPos, ItemFrame> validItemFrames)
		{
			this.validSigns = validSigns;
			this.validItemFrames = validItemFrames;
		}
	}
}

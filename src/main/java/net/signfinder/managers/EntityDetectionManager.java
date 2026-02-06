package net.signfinder.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.signfinder.SignFinderConfig;
import net.signfinder.services.EntityDetectionService;
import net.signfinder.services.EntityValidationService;
import net.signfinder.models.SignSearchResult;
import net.signfinder.util.SignTextUtils;
import net.signfinder.util.ItemFrameUtils;

/**
 * Coordinated entity detection functionality using service-oriented
 * architecture.
 * Manages auto-detection of signs and item frames that match configured
 * criteria.
 */
public class EntityDetectionManager
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(EntityDetectionManager.class);
	
	private final List<SignBlockEntity> highlightedSigns = new ArrayList<>();
	private final List<ItemFrame> highlightedItemFrames =
		new ArrayList<>();
	
	private final EntityDetectionService detectionService;
	private final EntityValidationService validationService;
	
	public EntityDetectionManager()
	{
		detectionService = new EntityDetectionService();
		validationService = new EntityValidationService();
		
		LOGGER.info(
			"EntityDetectionManager initialized with service architecture");
	}
	
	/**
	 * Perform auto-detection based on configuration settings.
	 */
	public void performAutoDetection(SignFinderConfig config)
	{
		if(!config.enable_auto_detection)
		{
			clearHighlighted();
			return;
		}
		
		clearHighlighted();
		
		// Auto-detect signs if enabled
		if(config.entity_search_range.includesSigns())
		{
			detectSigns(config);
		}
		
		// Auto-detect item frames if enabled
		if(config.entity_search_range.includesItemFrames())
		{
			detectItemFrames(config);
		}
		
		LOGGER.debug(
			"Auto-detection completed: {} signs, {} item frames highlighted",
			highlightedSigns.size(), highlightedItemFrames.size());
	}
	
	/**
	 * Update the item frame index for current loaded entities.
	 */
	public void updateItemFrameIndex()
	{
		validationService.updateItemFrameIndex();
	}
	
	/**
	 * Get the item frame position index.
	 */
	public Map<BlockPos, ItemFrame> getItemFramePositionIndex()
	{
		return validationService.getAllItemFrames();
	}
	
	/**
	 * Clean up all detection data and highlighted entities.
	 */
	public void cleanup()
	{
		clearHighlighted();
		LOGGER.debug("EntityDetectionManager cleanup completed");
	}
	
	// Getters for highlighted entities
	public List<SignBlockEntity> getHighlightedSigns()
	{
		return List.copyOf(highlightedSigns);
	}
	
	public List<ItemFrame> getHighlightedItemFrames()
	{
		return List.copyOf(highlightedItemFrames);
	}
	
	// Getters for services (for testing and advanced usage)
	public EntityDetectionService getDetectionService()
	{
		return detectionService;
	}
	
	public EntityValidationService getValidationService()
	{
		return validationService;
	}
	
	private void detectSigns(SignFinderConfig config)
	{
		List<SignBlockEntity> detectedSigns =
			detectionService.detectMatchingSigns(config);
		
		if(config.enable_sign_highlighting && config.auto_highlight_detected)
		{
			highlightedSigns.addAll(detectedSigns);
		}
		
		// Auto-save detected signs if enabled
		if(config.auto_save_detection_data)
		{
			saveDetectedSigns(detectedSigns, config);
		}
	}
	
	private void detectItemFrames(SignFinderConfig config)
	{
		updateItemFrameIndex();
		List<ItemFrame> detectedFrames =
			detectionService.detectMatchingItemFrames(config);
		
		if(config.enable_sign_highlighting && config.auto_highlight_detected)
		{
			highlightedItemFrames.addAll(detectedFrames);
		}
		
		// Auto-save detected item frames if enabled
		if(config.auto_save_detection_data)
		{
			saveDetectedItemFrames(detectedFrames, config);
		}
	}
	
	private void saveDetectedSigns(List<SignBlockEntity> detectedSigns,
		SignFinderConfig config)
	{
		Minecraft client = Minecraft.getInstance();
		if(client.player == null)
			return;
		
		for(SignBlockEntity sign : detectedSigns)
		{
			try
			{
				String[] signText = SignTextUtils.getSignTextArray(sign);
				String matchedText = String.join(" ", signText);
				
				SignSearchResult result = new SignSearchResult(sign.getBlockPos(),
					client.player.position(), signText, matchedText,
					config.text_preview_length);
				
				AutoSaveManager.INSTANCE.addDetectedSign(result);
			}catch(Exception e)
			{
				LOGGER.warn("Failed to save detected sign at {}: {}",
					sign.getBlockPos(), e.getMessage());
			}
		}
	}
	
	private void saveDetectedItemFrames(List<ItemFrame> detectedFrames,
		SignFinderConfig config)
	{
		Minecraft client = Minecraft.getInstance();
		if(client.player == null)
			return;
		
		for(ItemFrame itemFrame : detectedFrames)
		{
			try
			{
				String itemName = ItemFrameUtils.getItemName(itemFrame);
				String[] itemNameArray = {itemName};
				
				SignSearchResult result = new SignSearchResult(
					itemFrame.getPos(), client.player.position(),
					itemNameArray, itemName, config.text_preview_length);
				
				AutoSaveManager.INSTANCE.addDetectedSign(result);
			}catch(Exception e)
			{
				LOGGER.warn("Failed to save detected item frame at {}: {}",
					itemFrame.getPos(), e.getMessage());
			}
		}
	}
	
	void clearHighlighted()
	{
		highlightedSigns.clear();
		highlightedItemFrames.clear();
	}
}

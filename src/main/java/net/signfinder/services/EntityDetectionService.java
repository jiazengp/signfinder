package net.signfinder.services;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.decoration.ItemFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.signfinder.SignFinderConfig;
import net.signfinder.util.ChunkUtils;
import net.signfinder.util.ItemFrameUtils;
import net.signfinder.util.SignTextUtils;

/**
 * Service for detecting signs and item frames that match configured criteria.
 * Handles the actual scanning and filtering logic.
 */
public class EntityDetectionService
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(EntityDetectionService.class);
	
	/**
	 * Detect signs that contain container references based on configuration.
	 *
	 * @param config
	 *            Configuration containing detection keywords and settings
	 * @return List of detected sign entities
	 */
	public List<SignBlockEntity> detectMatchingSigns(SignFinderConfig config)
	{
		List<SignBlockEntity> detectedSigns = new ArrayList<>();
		
		ChunkUtils.getLoadedBlockEntities().forEach(blockEntity -> {
			if(blockEntity instanceof SignBlockEntity signEntity)
			{
				if(containsContainerReference(signEntity, config)
					&& !containsIgnoreWords(signEntity, config))
				{
					detectedSigns.add(signEntity);
				}
			}
		});
		
		LOGGER.debug("Detected {} matching signs", detectedSigns.size());
		return detectedSigns;
	}
	
	/**
	 * Detect item frames that contain container references based on
	 * configuration.
	 *
	 * @param config
	 *            Configuration containing detection keywords and settings
	 * @return List of detected item frame entities
	 */
	public List<ItemFrame> detectMatchingItemFrames(SignFinderConfig config)
	{
		List<ItemFrame> detectedFrames = new ArrayList<>();
		
		ChunkUtils.getLoadedEntities().forEach(entity -> {
			if(entity instanceof ItemFrame itemFrame
				&& ItemFrameUtils.hasItem(itemFrame))
			{
				
				if(containsContainerReferenceItemFrame(itemFrame, config)
					&& !containsIgnoreWordsItemFrame(itemFrame, config))
				{
					detectedFrames.add(itemFrame);
				}
			}
		});
		
		LOGGER.debug("Detected {} matching item frames", detectedFrames.size());
		return detectedFrames;
	}
	
	private boolean containsContainerReference(SignBlockEntity sign,
		SignFinderConfig config)
	{
		if(config.container_keywords == null
			|| config.container_keywords.length == 0)
			return false;
		
		String[] signText = SignTextUtils.getSignTextArray(sign);
		if(signText == null)
			return false;
		
		String fullText = String.join(" ", signText);
		
		for(String keyword : config.container_keywords)
		{
			if(keyword.trim().isEmpty())
				continue;
			
			boolean matches = config.case_sensitive ? fullText.contains(keyword)
				: fullText.toLowerCase().contains(keyword.toLowerCase());
			
			if(matches)
			{
				LOGGER.debug("Sign at {} matches keyword '{}'",
					sign.getBlockPos(), keyword);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean containsIgnoreWords(SignBlockEntity sign,
		SignFinderConfig config)
	{
		if(config.ignore_words == null || config.ignore_words.length == 0)
			return false;
		
		String[] signText = SignTextUtils.getSignTextArray(sign);
		if(signText == null)
			return false;
		
		String fullText = String.join(" ", signText);
		
		for(String ignoreWord : config.ignore_words)
		{
			if(ignoreWord.trim().isEmpty())
				continue;
			
			boolean matches =
				config.case_sensitive ? fullText.contains(ignoreWord)
					: fullText.toLowerCase().contains(ignoreWord.toLowerCase());
			
			if(matches)
			{
				LOGGER.debug("Sign at {} ignored due to word '{}'",
					sign.getBlockPos(), ignoreWord);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean containsContainerReferenceItemFrame(ItemFrame itemFrame,
		SignFinderConfig config)
	{
		if(config.container_keywords == null
			|| config.container_keywords.length == 0)
			return false;
		
		String itemName = ItemFrameUtils.getItemName(itemFrame);
		if(itemName == null)
			return false;
		
		for(String keyword : config.container_keywords)
		{
			if(keyword.trim().isEmpty())
				continue;
			
			boolean matches = config.case_sensitive ? itemName.contains(keyword)
				: itemName.toLowerCase().contains(keyword.toLowerCase());
			
			if(matches)
			{
				LOGGER.debug("Item frame at {} matches keyword '{}'",
					itemFrame.getPos(), keyword);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean containsIgnoreWordsItemFrame(ItemFrame itemFrame,
		SignFinderConfig config)
	{
		if(config.ignore_words == null || config.ignore_words.length == 0)
			return false;
		
		String itemName = ItemFrameUtils.getItemName(itemFrame);
		if(itemName == null)
			return false;
		
		for(String ignoreWord : config.ignore_words)
		{
			if(ignoreWord.trim().isEmpty())
				continue;
			
			boolean matches =
				config.case_sensitive ? itemName.contains(ignoreWord)
					: itemName.toLowerCase().contains(ignoreWord.toLowerCase());
			
			if(matches)
			{
				LOGGER.debug("Item frame at {} ignored due to word '{}'",
					itemFrame.getPos(), ignoreWord);
				return true;
			}
		}
		
		return false;
	}
}

package net.signfinder.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.BlockPos;
import net.signfinder.SignFinderConfig;
import net.signfinder.util.ChunkUtils;
import net.signfinder.util.ItemFrameUtils;
import net.signfinder.util.SignTextUtils;

public class EntityDetectionManager
{
	
	private final List<SignBlockEntity> highlightedSigns = new ArrayList<>();
	private final List<ItemFrameEntity> highlightedItemFrames =
		new ArrayList<>();
	private final Map<BlockPos, ItemFrameEntity> itemFramePositionIndex =
		new HashMap<>();
	
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
	}
	
	private void detectSigns(SignFinderConfig config)
	{
		ChunkUtils.getLoadedBlockEntities().forEach(blockEntity -> {
			if(blockEntity instanceof SignBlockEntity signEntity)
			{
				if(containsContainerReference(signEntity, config)
					&& !containsIgnoreWords(signEntity, config))
				{
					if(config.enable_sign_highlighting)
					{
						highlightedSigns.add(signEntity);
					}
				}
			}
		});
	}
	
	private void detectItemFrames(SignFinderConfig config)
	{
		updateItemFrameIndex();
		itemFramePositionIndex.values().forEach(itemFrame -> {
			if(containsContainerReferenceItemFrame(itemFrame, config)
				&& !containsIgnoreWordsItemFrame(itemFrame, config))
			{
				if(config.enable_sign_highlighting)
				{
					highlightedItemFrames.add(itemFrame);
				}
			}
		});
	}
	
	public void updateItemFrameIndex()
	{
		itemFramePositionIndex.clear();
		ChunkUtils.getLoadedEntities().forEach(entity -> {
			if(entity instanceof ItemFrameEntity itemFrame
				&& ItemFrameUtils.hasItem(itemFrame))
			{
				itemFramePositionIndex.put(itemFrame.getBlockPos(), itemFrame);
			}
		});
	}
	
	private boolean containsContainerReference(SignBlockEntity sign,
		SignFinderConfig config)
	{
		String signText =
			SignTextUtils.getSignText(sign, config.case_sensitive_search);
		return containsKeywords(signText, config.container_keywords,
			config.case_sensitive_search);
	}
	
	private boolean containsIgnoreWords(SignBlockEntity sign,
		SignFinderConfig config)
	{
		String signText =
			SignTextUtils.getSignText(sign, config.case_sensitive_search);
		return containsKeywords(signText, config.ignore_words,
			config.case_sensitive_search);
	}
	
	private boolean containsContainerReferenceItemFrame(
		ItemFrameEntity itemFrame, SignFinderConfig config)
	{
		String itemName = ItemFrameUtils.getItemFrameItemName(itemFrame,
			config.case_sensitive_search);
		return containsKeywords(itemName, config.container_keywords,
			config.case_sensitive_search);
	}
	
	private boolean containsIgnoreWordsItemFrame(ItemFrameEntity itemFrame,
		SignFinderConfig config)
	{
		String itemName = ItemFrameUtils.getItemFrameItemName(itemFrame,
			config.case_sensitive_search);
		return containsKeywords(itemName, config.ignore_words,
			config.case_sensitive_search);
	}
	
	private boolean containsKeywords(String text, String[] keywords,
		boolean caseSensitive)
	{
		return SignTextUtils.containsAnyKeyword(text, keywords, caseSensitive);
	}
	
	public void clearHighlighted()
	{
		highlightedSigns.clear();
		highlightedItemFrames.clear();
	}
	
	public void cleanup()
	{
		clearHighlighted();
		itemFramePositionIndex.clear();
	}
	
	// Getters
	public List<SignBlockEntity> getHighlightedSigns()
	{
		return highlightedSigns;
	}
	
	public List<ItemFrameEntity> getHighlightedItemFrames()
	{
		return highlightedItemFrames;
	}
	
	public Map<BlockPos, ItemFrameEntity> getItemFramePositionIndex()
	{
		return itemFramePositionIndex;
	}
}

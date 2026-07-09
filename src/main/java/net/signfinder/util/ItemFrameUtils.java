package net.signfinder.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemFrameUtils
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(ItemFrameUtils.class);
	
	public static String getItemFrameItemName(ItemFrame itemFrame,
		boolean caseSensitive)
	{
		if(!hasItem(itemFrame))
			return "";
		
		try
		{
			Component itemName = itemFrame.getItem().getDisplayName();
			;
			String name = itemName.getString();
			LOGGER.debug("Item frame name: {}", name);
			return caseSensitive ? name : name.toLowerCase();
		}catch(Exception e)
		{
			return "";
		}
	}
	
	public static String getItemName(ItemFrame itemFrame)
	{
		return getItemFrameItemName(itemFrame, false);
	}
	
	public static String getItemDisplayName(ItemFrame itemFrame)
	{
		return getItemName(itemFrame);
	}
	
	public static boolean hasItem(ItemFrame itemFrame)
	{
		if(itemFrame == null || itemFrame.isRemoved())
			return false;
		
		try
		{
			ItemStack itemStack = itemFrame.getItem();
			return !itemStack.isEmpty();
		}catch(Exception e)
		{
			return false;
		}
	}
}

package net.signfinder.util;

import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class ItemFrameUtils
{
	public static String getItemFrameItemName(ItemFrameEntity itemFrame,
		boolean caseSensitive)
	{
		if(!hasItem(itemFrame))
			return "";
		
		try
		{
			Text itemName = itemFrame.getHeldItemStack().getName();
			String name = itemName != null ? itemName.getString() : "";
			return name != null ? (caseSensitive ? name : name.toLowerCase())
				: "";
		}catch(Exception e)
		{
			return "";
		}
	}
	
	public static boolean hasItem(ItemFrameEntity itemFrame)
	{
		if(itemFrame == null || itemFrame.isRemoved())
			return false;
		
		try
		{
			ItemStack itemStack = itemFrame.getHeldItemStack();
			return itemStack != null && !itemStack.isEmpty();
		}catch(Exception e)
		{
			return false;
		}
	}
}

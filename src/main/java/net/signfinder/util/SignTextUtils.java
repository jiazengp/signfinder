package net.signfinder.util;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public enum SignTextUtils
{
	;
	public static String getSignText(SignBlockEntity sign,
		boolean caseSensitive)
	{
		String[] lines = new String[4];
		for(int i = 0; i < 4; i++)
		{
			Text text = sign.getFrontText().getMessage(i, false);
			lines[i] = caseSensitive ? text.getString()
				: text.getString().toLowerCase();
		}
		return String.join(" ", lines);
	}
	
	public static String getSignText(SignBlockEntity sign)
	{
		return getSignText(sign, false);
	}
	
	public static String[] getSignText(World world, BlockPos pos)
	{
		if(world.getBlockEntity(pos) instanceof SignBlockEntity sign)
		{
			return getSignTextArray(sign);
		}
		return null;
	}
	
	public static String[] getSignTextArray(SignBlockEntity sign)
	{
		String[] lines = new String[4];
		for(int i = 0; i < 4; i++)
		{
			Text text = sign.getFrontText().getMessage(i, false);
			lines[i] = text.getString();
		}
		return lines;
	}
	
	/**
	 * Check if text contains any specified keywords.
	 */
	public static boolean containsAnyKeyword(String text, String[] keywords,
		boolean caseSensitive)
	{
		if(keywords == null)
			return false;
		
		for(String keyword : keywords)
		{
			if(keyword != null && !keyword.isEmpty())
			{
				String processedKeyword =
					caseSensitive ? keyword : keyword.toLowerCase();
				if(text.contains(processedKeyword))
					return true;
			}
		}
		return false;
	}
}

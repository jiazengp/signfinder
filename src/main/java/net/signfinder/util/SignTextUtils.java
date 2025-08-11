package net.signfinder.util;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.Text;

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
	
	/**
	 * 检查文本是否包含任何指定的关键词
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

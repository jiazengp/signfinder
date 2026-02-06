package net.signfinder.util;


import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public enum SignTextUtils
{
	;
	public static String getSignText(SignBlockEntity sign,
                                     boolean caseSensitive)
	{
		String[] lines = new String[4];
		for(int i = 0; i < 4; i++)
		{
            Component component = sign.getFrontText().getMessage(i, false);
            String line = component.getString();

            lines[i] = caseSensitive ? line : line.toLowerCase();

        }
		return String.join(" ", lines);
	}
	
	public static String getSignText(SignBlockEntity sign)
	{
		return getSignText(sign, false);
	}

    public static String[] getSignText(Level level, BlockPos pos)
    {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign)
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
            Component text = sign.getFrontText().getMessage(i, false);
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

package net.signfinder.managers;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.signfinder.SignFinderMod;

public class ColorManager
{
	private static final int[] COLOR_CYCLE = {0x00FF00, 0xFF0000, 0x0000FF,
		0xFFFF00, 0xFF00FF, 0x00FFFF, 0xFF8000, 0xFFFFFF};
	
	private static final String[] COLOR_NAMES =
		{"green", "red", "blue", "yellow", "purple", "cyan", "orange", "white"};
	
	private final Map<BlockPos, Integer> customColors = new HashMap<>();
	private final SearchResultManager searchResultManager;
	
	public ColorManager(SearchResultManager searchResultManager)
	{
		this.searchResultManager = searchResultManager;
	}
	
	public String cycleHighlightColor(int x, int y, int z)
	{
		BlockPos targetPos = new BlockPos(x, y, z);
		
		if(!searchResultManager.hasResultAtPos(targetPos))
			return null;
		
		int currentColor = customColors.getOrDefault(targetPos, COLOR_CYCLE[0]);
		int currentIndex = 0;
		for(int i = 0; i < COLOR_CYCLE.length; i++)
		{
			if(COLOR_CYCLE[i] == currentColor)
			{
				currentIndex = i;
				break;
			}
		}
		
		int nextIndex = (currentIndex + 1) % COLOR_CYCLE.length;
		customColors.put(targetPos, COLOR_CYCLE[nextIndex]);
		
		return COLOR_NAMES[nextIndex];
	}
	
	public int getHighlightColor(BlockPos pos)
	{
		return customColors.getOrDefault(pos, SignFinderMod.getInstance()
			.getConfigHolder().getConfig().sign_highlight_color);
	}
	
	public void removeCustomColor(BlockPos pos)
	{
		customColors.remove(pos);
	}
	
	public void clearCustomColors()
	{
		customColors.clear();
	}
}

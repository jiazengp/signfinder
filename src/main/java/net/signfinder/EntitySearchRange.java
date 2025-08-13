package net.signfinder;

public enum EntitySearchRange
{
	SIGNS_ONLY,
	ITEM_FRAMES_ONLY,
	BOTH;
	
	public boolean includesSigns()
	{
		return this == SIGNS_ONLY || this == BOTH;
	}
	
	public boolean includesItemFrames()
	{
		return this == ITEM_FRAMES_ONLY || this == BOTH;
	}
	
	@Override
	public String toString()
	{
		return "text.autoconfig.signfinder.option.entity_search_range."
			+ name().toLowerCase();
	}
}

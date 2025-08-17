package net.signfinder.core;

public enum AutoSaveMode
{
	AUTO_OVERWRITE,
	NEW_FILE,
	DAILY_SPLIT;
	
	@Override
	public String toString()
	{
		return "text.autoconfig.signfinder.option.auto_save_mode."
			+ name().toLowerCase();
	}
}

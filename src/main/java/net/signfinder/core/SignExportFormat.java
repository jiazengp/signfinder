package net.signfinder.core;

public enum SignExportFormat
{
	TEXT(true, false),
	JSON(false, true);
	
	private final boolean text;
	private final boolean json;
	
	SignExportFormat(boolean text, boolean json)
	{
		this.text = text;
		this.json = json;
	}
	
	public boolean isTextFormat()
	{
		return text;
	}
	
	public boolean isJsonFormat()
	{
		return json;
	}
	
	@Override
	public String toString()
	{
		return "text.autoconfig.signfinder.option.export_format."
			+ name().toLowerCase();
	}
}

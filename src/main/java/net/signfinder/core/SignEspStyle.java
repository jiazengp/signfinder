package net.signfinder.core;

public enum SignEspStyle
{
	BOXES(true, false),
	LINES(false, true),
	LINES_AND_BOXES(true, true);
	
	private final boolean boxes;
	private final boolean lines;
	
	private SignEspStyle(boolean boxes, boolean lines)
	{
		this.boxes = boxes;
		this.lines = lines;
	}
	
	public boolean hasBoxes()
	{
		return boxes;
	}
	
	public boolean hasLines()
	{
		return lines;
	}
	
	@Override
	public String toString()
	{
		return "text.autoconfig.signfinder.option.highlight_style."
			+ name().toLowerCase();
	}
}

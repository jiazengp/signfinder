package net.signfinder;

import java.util.HashMap;
import java.util.Map;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "signfinder")
public class SignFinderConfig implements ConfigData
{
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.BoundedDiscrete(min = 1, max = 1000)
	public int default_search_radius = 500;
	
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.BoundedDiscrete(min = 1, max = 10)
	public int max_results_per_page = 5;
	
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.BoundedDiscrete(min = 10, max = 30)
	public int text_preview_length = 20;
	
	@ConfigEntry.Gui.Tooltip
	public boolean enable_auto_detection = false;
	
	@ConfigEntry.Gui.Tooltip
	public boolean enable_sign_highlighting = true;
	
	@ConfigEntry.Gui.Tooltip
	public boolean auto_remove_nearby = true;
	
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.BoundedDiscrete(min = 1, max = 10)
	public double auto_remove_distance = 2.0;
	
	@ConfigEntry.Gui.Tooltip
	public boolean auto_clear_other_highlights = true;
	
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.ColorPicker
	public int sign_highlight_color = 0x00FF00;
	
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(
		option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public SignEspStyle highlight_style = SignEspStyle.BOXES;
	
	@ConfigEntry.Gui.Tooltip
	public boolean case_sensitive_search = false;
	
	@ConfigEntry.Gui.Tooltip
	public String[] ignore_words = {};
	
	@ConfigEntry.Gui.Tooltip
	public String[] container_keywords = {};
	
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(
		option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public SignExportFormat export_format = SignExportFormat.TEXT;
	
	@ConfigEntry.Gui.CollapsibleObject
	@ConfigEntry.Gui.Excluded
	public SearchPresets search_presets = new SearchPresets();
	
	public static class SearchPresets implements ConfigData
	{
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.Excluded
		public Map<String, String> text_presets = new HashMap<>();
		
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.Excluded
		public Map<String, String> regex_presets = new HashMap<>();
	}
}

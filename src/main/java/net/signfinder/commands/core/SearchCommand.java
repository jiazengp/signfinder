package net.signfinder.commands.core;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.signfinder.models.EntitySearchResult;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.services.SearchQuery;
import net.signfinder.services.SearchQuery.SearchType;
import net.signfinder.commands.specialized.ResultDisplayCommand;

import java.util.List;

public class SearchCommand extends BaseCommand
{
	public static int executeSearch(
		CommandContext<FabricClientCommandSource> ctx, Integer radius,
		String presetName, SearchType forceType)
	{
		if(!validatePlayerInWorld(ctx))
			return 0;
		
		SignFinderMod signFinder = getSignFinderInstance(ctx);
		if(signFinder == null)
			return 0;
		
		SignFinderConfig config = signFinder.getConfigHolder().getConfig();
		
		String queryString;
		SearchType searchType;
		
		try
		{
			if(forceType == SearchType.PRESET)
			{
				queryString = StringArgumentType.getString(ctx, "preset_name");
				searchType = SearchType.PRESET;
			}else
			{
				String argName = forceType == SearchType.REGEX ? "pattern"
					: (forceType == SearchType.ARRAY ? "keywords" : "query");
				
				queryString = StringArgumentType.getString(ctx, argName);
				
				searchType = forceType != null ? forceType : SearchType.TEXT;
			}
			
			// Empty query is valid - searches all entities in range
			if(queryString == null)
			{
				queryString = "";
			}
		}catch(Exception e)
		{
			// Send helpful error message about using quotes
			ctx.getSource()
				.sendFeedback(Component
					.translatable("signfinder.error.invalid_query_format")
					.withStyle(ChatFormatting.RED));
			ctx.getSource().sendFeedback(
				Component.translatable("signfinder.help.use_quotes")
					.withStyle(ChatFormatting.YELLOW));
			return 0;
		}
		
		int searchRadius =
			radius != null ? radius : config.default_search_radius;
		
		SearchQuery query = new SearchQuery(queryString, searchType,
			searchRadius, config.case_sensitive);
		
		// Always use unified entity search system
		List<EntitySearchResult> entityResults =
			signFinder.getSearchService().searchEntities(query, config);
		
		// 保存预设
		if(presetName != null && !presetName.isEmpty())
		{
			savePreset(presetName, queryString, searchType, config);
			ctx.getSource()
				.sendFeedback(Component
					.translatable("signfinder.message.preset_saved", presetName)
					.withStyle(ChatFormatting.GREEN));
		}
		
		// 缓存结果
		String cacheKey = getPlayerCacheKey();
		int currentPage = CommandUtils.getCurrentPage(cacheKey);
		int totalPages = CommandUtils.calculateTotalPages(entityResults.size(),
			config.max_results_per_page);
		currentPage = Math.max(1, Math.min(currentPage, totalPages));
		CommandUtils.cacheEntitySearchResults(cacheKey, entityResults,
			currentPage, searchRadius);
		CommandUtils.cacheSearchQuery(cacheKey, queryString);
		
		// 将搜索结果传递给SignFinderMod进行渲染
		signFinder.setEntitySearchResults(entityResults);
		
		ResultDisplayCommand.displayEntityResults(ctx.getSource(),
			entityResults, currentPage, config, searchRadius);
		
		return entityResults.size();
	}
	
	public static int executeSearchAll(
		CommandContext<FabricClientCommandSource> ctx)
	{
		if(!validatePlayerInWorld(ctx))
			return 0;
		
		SignFinderMod signFinder = getSignFinderInstance(ctx);
		if(signFinder == null)
			return 0;
		
		SignFinderConfig config = signFinder.getConfigHolder().getConfig();
		Minecraft mc = Minecraft.getInstance();
		
		if(mc.player == null)
			return 0;
		
		int searchRadius = config.default_search_radius;
		
		ctx.getSource().sendFeedback(Component
			.translatable("signfinder.search.all_signs", searchRadius));
		
		// Use empty query to match all entities (based on config)
		SearchQuery query =
			new SearchQuery("", SearchType.TEXT, searchRadius, false);
		List<EntitySearchResult> entityResults =
			signFinder.getSearchService().searchEntities(query, config);
		
		if(entityResults.isEmpty())
		{
			ctx.getSource().sendFeedback(Component.translatable(
				"signfinder.message.no_matching_signs", searchRadius));
			return 0;
		}
		
		String cacheKey = getPlayerCacheKey();
		String queryString = Component
			.translatable("signfinder.export.all_signs_title").getString();
		int currentPage = 1;
		CommandUtils.cacheEntitySearchResults(cacheKey, entityResults,
			currentPage, searchRadius);
		CommandUtils.cacheSearchQuery(cacheKey, queryString);
		
		signFinder.setEntitySearchResults(entityResults);
		
		ResultDisplayCommand.displayEntityResults(ctx.getSource(),
			entityResults, currentPage, config, searchRadius);
		
		return entityResults.size();
	}
	
	private static void savePreset(String presetName, String query,
		SearchType type, SignFinderConfig config)
	{
		if(type == SearchType.REGEX)
		{
			config.search_presets.regex_presets.put(presetName, query);
		}else
		{
			config.search_presets.text_presets.put(presetName, query);
		}
		SignFinderMod.getInstance().getConfigHolder().save();
	}
}

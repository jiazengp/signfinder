package net.signfinder.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.SignSearchEngine;
import net.signfinder.SignSearchEngine.SearchQuery;
import net.signfinder.SignSearchEngine.SearchType;
import net.signfinder.SignSearchResult;

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
		
		if(forceType == SearchType.PRESET)
		{
			queryString = StringArgumentType.getString(ctx, "preset_name");
			searchType = SearchType.PRESET;
		}else
		{
			queryString = StringArgumentType.getString(ctx,
				forceType == SearchType.REGEX ? "pattern"
					: forceType == SearchType.ARRAY ? "keywords" : "query");
			searchType = forceType != null ? forceType : SearchType.TEXT;
		}
		
		int searchRadius =
			radius != null ? radius : config.default_search_radius;
		
		SearchQuery query = new SearchQuery(queryString, searchType,
			searchRadius, config.case_sensitive_search);
		List<SignSearchResult> results =
			SignSearchEngine.searchSigns(query, config);
		
		// 保存预设
		if(presetName != null && !presetName.isEmpty())
		{
			savePreset(presetName, queryString, searchType, config);
			ctx.getSource()
				.sendFeedback(Text
					.translatable("signfinder.message.preset_saved", presetName)
					.formatted(Formatting.GREEN));
		}
		
		// 缓存结果
		String cacheKey = getPlayerCacheKey();
		// 保持在当前页面，如果没有缓存则从第1页开始
		int currentPage = CommandUtils.getCurrentPage(cacheKey);
		// 确保页面有效
		int totalPages = CommandUtils.calculateTotalPages(results.size(),
			config.max_results_per_page);
		currentPage = Math.max(1, Math.min(currentPage, totalPages));
		CommandUtils.cacheSearchResults(cacheKey, results, currentPage,
			searchRadius);
		CommandUtils.cacheSearchQuery(cacheKey, queryString);
		
		// 将搜索结果传递给SignFinderMod进行渲染
		signFinder.setSearchResults(results);
		
		ResultDisplayCommand.displayResults(ctx.getSource(), results,
			currentPage, config, searchRadius);
		
		return results.size();
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
		MinecraftClient mc = MinecraftClient.getInstance();
		
		if(mc.player == null)
			return 0;
		
		Vec3d playerPos = mc.player.getPos();
		int searchRadius = config.default_search_radius;
		
		ctx.getSource().sendFeedback(
			Text.translatable("signfinder.search.all_signs", searchRadius));
		
		List<SignSearchResult> results =
			SignSearchEngine.INSTANCE.findAllSigns(playerPos, searchRadius);
		
		if(results.isEmpty())
		{
			ctx.getSource().sendFeedback(Text.translatable(
				"signfinder.message.no_matching_signs", searchRadius));
			return 0;
		}
		
		String cacheKey = getPlayerCacheKey();
		String queryString =
			Text.translatable("signfinder.export.all_signs_title").getString();
		int currentPage = 1;
		CommandUtils.cacheSearchResults(cacheKey, results, currentPage,
			searchRadius);
		CommandUtils.cacheSearchQuery(cacheKey, queryString);
		
		signFinder.setSearchResults(results);
		
		ResultDisplayCommand.displayResults(ctx.getSource(), results,
			currentPage, config, searchRadius);
		
		return results.size();
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

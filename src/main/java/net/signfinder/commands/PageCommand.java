package net.signfinder.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.SignSearchResult;

import java.util.List;

public class PageCommand extends BaseCommand
{
	public static int executePage(CommandContext<FabricClientCommandSource> ctx)
	{
		if(!validatePlayerInWorld(ctx))
			return 0;
		
		String cacheKey = getPlayerCacheKey();
		List<SignSearchResult> results =
			CommandUtils.getCachedResults(cacheKey);
		
		if(results == null || results.isEmpty())
		{
			ctx.getSource().sendFeedback(
				Text.translatable("signfinder.error.no_search_results")
					.formatted(Formatting.RED));
			return 0;
		}
		
		int page = IntegerArgumentType.getInteger(ctx, "page_number");
		SignFinderConfig config =
			SignFinderMod.getInstance().getConfigHolder().getConfig();
		int searchRadius = CommandUtils.getSearchRadius(cacheKey,
			config.default_search_radius);
		
		CommandUtils.setCurrentPage(cacheKey, page);
		ResultDisplayCommand.displayResults(ctx.getSource(), results, page,
			config, searchRadius);
		
		return 1;
	}
	
	public static int showCurrentPage(
		CommandContext<FabricClientCommandSource> ctx)
	{
		if(!validatePlayerInWorld(ctx))
			return 0;
		
		String cacheKey = getPlayerCacheKey();
		List<SignSearchResult> results =
			CommandUtils.getCachedResults(cacheKey);
		
		if(results == null || results.isEmpty())
		{
			ctx.getSource().sendFeedback(
				Text.translatable("signfinder.error.no_search_results")
					.formatted(Formatting.RED));
			return 0;
		}
		
		int currentPage = CommandUtils.getCurrentPage(cacheKey);
		SignFinderConfig config =
			SignFinderMod.getInstance().getConfigHolder().getConfig();
		int searchRadius = CommandUtils.getSearchRadius(cacheKey,
			config.default_search_radius);
		
		ResultDisplayCommand.displayResults(ctx.getSource(), results,
			currentPage, config, searchRadius);
		
		return 1;
	}
}

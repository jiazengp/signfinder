package net.signfinder.commands;

import java.util.List;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.signfinder.EntitySearchResult;
import net.signfinder.SignExportFormat;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.SignSearchEngine;
import net.signfinder.util.ExportUtils;

public class ExportCommand extends BaseCommand
{
	public static int executeExport(
		CommandContext<FabricClientCommandSource> ctx,
		SignExportFormat exportFormat)
	{
		if(!validatePlayerInWorld(ctx))
			return 0;
		
		SignFinderMod signFinder = getSignFinderInstance(ctx);
		if(signFinder == null)
			return 0;
		
		SignExportFormat format = exportFormat == null
			? SignExportFormatArgument.getFormat(ctx, "format") : exportFormat;
		SignFinderConfig config = signFinder.getConfigHolder().getConfig();
		MinecraftClient mc = MinecraftClient.getInstance();
		
		String playerKey = getPlayerCacheKey();
		List<EntitySearchResult> currentEntityResults =
			CommandUtils.getCachedEntityResults(playerKey);
		String currentQuery = CommandUtils.getCachedQuery(playerKey);
		
		// Export current search results if available
		if(currentEntityResults != null && !currentEntityResults.isEmpty())
		{
			ctx.getSource().sendFeedback(
				Text.translatable("signfinder.export.exporting_results",
					currentQuery != null ? currentQuery
						: Text.translatable("signfinder.export.unknown_query")
							.getString()));
			boolean success = ExportUtils.INSTANCE.exportEntitySearchResult(
				currentEntityResults, currentQuery, format);
			return success ? 0 : 1;
		}
		
		// No search results exist, export according to config settings
		if(mc.player == null)
			return 0;
		Vec3d playerPos = mc.player.getPos();
		int defaultRadius = config.default_search_radius;
		
		// Generate dynamic message based on search range
		Text searchRangeText =
			Text.translatable(config.entity_search_range.toString());
		ctx.getSource()
			.sendFeedback(Text.translatable("signfinder.export.no_cache",
				searchRangeText.getString(), defaultRadius));
		
		// Search for all entities using empty query (matches everything)
		SignSearchEngine.SearchQuery searchQuery =
			new SignSearchEngine.SearchQuery("",
				SignSearchEngine.SearchType.TEXT, defaultRadius, false);
		List<EntitySearchResult> allEntities =
			SignSearchEngine.searchEntities(searchQuery, config);
		
		if(allEntities.isEmpty())
		{
			ctx.getSource().sendFeedback(
				Text.translatable("signfinder.export.no_signs_found",
					searchRangeText.getString(), defaultRadius));
			return 1;
		}
		
		boolean success = ExportUtils.INSTANCE.exportEntitySearchResult(
			allEntities,
			Text.translatable("signfinder.export.all_signs_title").getString(),
			format);
		return success ? 0 : 1;
	}
}

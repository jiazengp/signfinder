package net.signfinder.commands.specialized;

import java.util.List;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.signfinder.core.SignExportFormat;
import net.signfinder.models.EntitySearchResult;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.util.ExportUtils;
import net.signfinder.commands.core.BaseCommand;
import net.signfinder.commands.core.CommandUtils;

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
		Minecraft mc = Minecraft.getInstance();
		
		String playerKey = getPlayerCacheKey();
		List<EntitySearchResult> currentEntityResults =
			CommandUtils.getCachedEntityResults(playerKey);
		String currentQuery = CommandUtils.getCachedQuery(playerKey);
		
		// Export current search results if available
		if(currentEntityResults != null && !currentEntityResults.isEmpty())
		{
			ctx.getSource().sendFeedback(
				Component.translatable("signfinder.export.exporting_results",
					currentQuery != null ? currentQuery
						: Component.translatable("signfinder.export.unknown_query")
							.getString()));
			boolean success = ExportUtils.INSTANCE.exportEntitySearchResult(
				currentEntityResults, currentQuery, format);
			return success ? 0 : 1;
		}
		
		// No search results exist, export according to config settings
		if(mc.player == null)
			return 0;
		Vec3 playerPos = mc.player.position();
		int defaultRadius = config.default_search_radius;
		
		// Generate dynamic message based on search range
		Component searchRangeText =
                Component.translatable(config.entity_search_range.toString());
		ctx.getSource()
			.sendFeedback(Component.translatable("signfinder.export.no_cache",
				searchRangeText.getString(), defaultRadius));
		
		// Search for all entities using empty query (matches everything)
		// Create a modified config that excludes local data for export
		boolean originalAutoSave = config.auto_save_detection_data;
		config.auto_save_detection_data = false; // Temporarily disable local
													// data
		
		try
		{
			net.signfinder.services.SearchQuery searchQuery =
				new net.signfinder.services.SearchQuery("",
					net.signfinder.services.SearchQuery.SearchType.TEXT,
					defaultRadius, false);
			List<EntitySearchResult> allEntities = signFinder.getSearchService()
				.searchEntities(searchQuery, config);
			
			if(allEntities.isEmpty())
			{
				ctx.getSource().sendFeedback(
                        Component.translatable("signfinder.export.no_signs_found",
						searchRangeText.getString(), defaultRadius));
				return 1;
			}
			
			boolean success =
				ExportUtils.INSTANCE.exportEntitySearchResult(allEntities,
                        Component.translatable("signfinder.export.all_signs_title")
						.getString(),
					format);
			return success ? 0 : 1;
		}finally
		{
			config.auto_save_detection_data = originalAutoSave; // Restore
																// original
																// setting
		}
	}
}

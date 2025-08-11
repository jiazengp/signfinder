package net.signfinder.commands;

import java.util.List;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.signfinder.*;
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
		List<SignSearchResult> currentResults =
			CommandUtils.getCachedResults(playerKey);
		String currentQuery = CommandUtils.getCachedQuery(playerKey);
		
		// If no search results exist, export all signs within default range
		if(currentResults == null || currentResults.isEmpty())
		{
			if(mc.player == null)
				return 0;
			Vec3d playerPos = mc.player.getPos();
			int defaultRadius = config.default_search_radius;
			
			ctx.getSource().sendFeedback(
				Text.translatable("signfinder.export.no_cache", defaultRadius));
			
			List<SignSearchResult> allSigns = SignSearchEngine.INSTANCE
				.findAllSigns(playerPos, defaultRadius);
			
			if(allSigns.isEmpty())
			{
				ctx.getSource().sendFeedback(Text.translatable(
					"signfinder.export.no_signs_found", defaultRadius));
				return 1;
			}
			
			boolean success = ExportUtils.INSTANCE.exportSignSearchResult(
				allSigns, Text.translatable("signfinder.export.all_signs_title")
					.getString(),
				format);
			return success ? 0 : 1;
		}
		
		// Export current search results
		ctx.getSource().sendFeedback(
			Text.translatable("signfinder.export.exporting_results",
				currentQuery != null ? currentQuery
					: Text.translatable("signfinder.export.unknown_query")
						.getString()));
		boolean success = ExportUtils.INSTANCE
			.exportSignSearchResult(currentResults, currentQuery, format);
		return success ? 0 : 1;
	}
}

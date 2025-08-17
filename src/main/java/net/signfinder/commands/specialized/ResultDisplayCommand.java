package net.signfinder.commands.specialized;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.signfinder.core.SignExportFormat;
import net.signfinder.models.EntitySearchResult;
import net.signfinder.SignFinderConfig;
import net.signfinder.commands.core.CommandUtils;

import java.util.List;

public class ResultDisplayCommand
{
	public static void displayEntityResults(FabricClientCommandSource source,
		List<EntitySearchResult> results, int page, SignFinderConfig config,
		int searchRadius)
	{
		displayGenericResults(source, results, page, config, searchRadius,
			CommandUtils::createEntityResultText);
	}
	
	private static <T> void displayGenericResults(
		FabricClientCommandSource source, List<T> results, int page,
		SignFinderConfig config, int searchRadius,
		ResultTextCreator<T> textCreator)
	{
		if(results.isEmpty())
		{
			displayEmptyResults(source, searchRadius);
			return;
		}
		
		int totalPages = CommandUtils.calculateTotalPages(results.size(),
			config.max_results_per_page);
		page = Math.max(1, Math.min(page, totalPages));
		
		int[] indices = CommandUtils.getPageIndices(page, results.size(),
			config.max_results_per_page);
		int startIndex = indices[0];
		int endIndex = indices[1];
		
		source.sendFeedback(
			Text.translatable("signfinder.message.search_results_title",
				results.size(), searchRadius).formatted(Formatting.YELLOW));
		source.sendFeedback(
			Text.translatable("signfinder.message.page_info", page, totalPages)
				.formatted(Formatting.GRAY));
		
		// 显示结果
		for(int i = startIndex; i < endIndex; i++)
		{
			MutableText text = textCreator.createText(results.get(i), i + 1);
			source.sendFeedback(text);
		}
		
		if(totalPages > 1)
		{
			MutableText pageControl =
				CommandUtils.createPaginationControls(page, totalPages);
			source.sendFeedback(pageControl);
		}
		
		if(totalPages > 3)
		{
			MutableText exportReminder = Text
				.translatable("signfinder.message.export_reminder")
				.formatted(Formatting.GOLD).append(" ")
				.append(CommandUtils.createExportButton(SignExportFormat.JSON))
				.append(" ")
				.append(CommandUtils.createExportButton(SignExportFormat.TEXT));
			source.sendFeedback(exportReminder);
		}
	}
	
	@FunctionalInterface
	private interface ResultTextCreator<T>
	{
		MutableText createText(T result, int index);
	}
	
	private static void displayEmptyResults(FabricClientCommandSource source,
		int searchRadius)
	{
		source.sendFeedback(Text
			.translatable("signfinder.message.no_matching_signs", searchRadius)
			.formatted(Formatting.YELLOW));
	}
}

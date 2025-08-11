package net.signfinder.commands;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignSearchResult;

import java.util.List;

public class ResultDisplayCommand
{
	public static void displayResults(FabricClientCommandSource source,
		List<SignSearchResult> results, int page, SignFinderConfig config,
		int searchRadius)
	{
		if(results.isEmpty())
		{
			source.sendFeedback(
				Text.translatable("signfinder.message.no_matching_signs",
					searchRadius).formatted(Formatting.YELLOW));
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
			MutableText text =
				CommandUtils.createResultText(results.get(i), i + 1);
			source.sendFeedback(text);
		}
		
		// 分页控制
		if(totalPages > 1)
		{
			MutableText pageControl =
				CommandUtils.createPaginationControls(page, totalPages);
			source.sendFeedback(pageControl);
		}
		
		// Excel导出提醒 - 超过3页时显示
		if(totalPages > 3)
		{
			MutableText exportReminder =
				Text.translatable("signfinder.message.export_reminder")
					.formatted(Formatting.GOLD).append(" ")
					.append(CommandUtils.createExportButton());
			source.sendFeedback(exportReminder);
		}
	}
}

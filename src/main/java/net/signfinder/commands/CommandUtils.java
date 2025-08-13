package net.signfinder.commands;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.signfinder.EntitySearchResult;
import net.signfinder.SignExportFormat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;

public class CommandUtils
{
	private static final Map<String, List<EntitySearchResult>> entitySearchResultCache =
		new ConcurrentHashMap<>();
	private static final Map<String, Integer> currentPageCache =
		new ConcurrentHashMap<>();
	private static final Map<String, Integer> searchRadiusCache =
		new ConcurrentHashMap<>();
	private static final Map<String, String> searchQueryCache =
		new ConcurrentHashMap<>();
	private static final Map<String, Long> cacheTimestamps =
		new ConcurrentHashMap<>();
	
	private static final int MAX_CACHE_SIZE = 50;
	private static final int MAX_CACHE_SIZE_HARD_LIMIT = 100; // 硬限制，达到后强制清理
	private static final long CACHE_EXPIRY_TIME = 300000; // 5分钟
	private static final long CACHE_CHECK_INTERVAL = 60000; // 1分钟检查一次
	private static long lastCacheCheck = 0;
	
	public static void cacheEntitySearchResults(String playerKey,
		List<EntitySearchResult> results, int currentPage, int searchRadius)
	{
		performCacheMaintenanceIfNeeded();
		
		if(entitySearchResultCache.size() >= MAX_CACHE_SIZE_HARD_LIMIT)
		{
			cleanOldestCacheEntries();
		}
		
		entitySearchResultCache.put(playerKey, results);
		currentPageCache.put(playerKey, currentPage);
		searchRadiusCache.put(playerKey, searchRadius);
		cacheTimestamps.put(playerKey, System.currentTimeMillis());
	}
	
	public static void cacheSearchQuery(String playerKey, String query)
	{
		searchQueryCache.put(playerKey, query);
	}
	
	public static String getCachedQuery(String playerKey)
	{
		return searchQueryCache.get(playerKey);
	}
	
	public static List<EntitySearchResult> getCachedEntityResults(
		String playerKey)
	{
		return entitySearchResultCache.get(playerKey);
	}
	
	public static int getCurrentPage(String playerKey)
	{
		return currentPageCache.getOrDefault(playerKey, 1);
	}
	
	public static int getSearchRadius(String playerKey, int defaultRadius)
	{
		return searchRadiusCache.getOrDefault(playerKey, defaultRadius);
	}
	
	public static void clearCache(String playerKey)
	{
		entitySearchResultCache.remove(playerKey);
		currentPageCache.remove(playerKey);
		searchRadiusCache.remove(playerKey);
		searchQueryCache.remove(playerKey);
		cacheTimestamps.remove(playerKey);
	}
	
	// 清理所有缓存
	public static void clearAllCaches()
	{
		entitySearchResultCache.clear();
		currentPageCache.clear();
		searchRadiusCache.clear();
		searchQueryCache.clear();
		cacheTimestamps.clear();
	}
	
	private static void cleanExpiredCache()
	{
		long currentTime = System.currentTimeMillis();
		Iterator<Map.Entry<String, Long>> iterator =
			cacheTimestamps.entrySet().iterator();
		
		while(iterator.hasNext())
		{
			Map.Entry<String, Long> entry = iterator.next();
			String playerKey = entry.getKey();
			long timestamp = entry.getValue();
			
			if(currentTime - timestamp > CACHE_EXPIRY_TIME)
			{
				// 清理该玩家的所有缓存
				clearCache(playerKey);
				iterator.remove();
			}
		}
	}
	
	public static void performPeriodicCleanup()
	{
		cleanExpiredCache();
	}
	
	private static void performCacheMaintenanceIfNeeded()
	{
		long currentTime = System.currentTimeMillis();
		if(currentTime - lastCacheCheck > CACHE_CHECK_INTERVAL)
		{
			lastCacheCheck = currentTime;
			if(entitySearchResultCache.size() > MAX_CACHE_SIZE)
			{
				cleanExpiredCache();
			}
		}
	}
	
	private static void cleanOldestCacheEntries()
	{
		int targetSize = MAX_CACHE_SIZE / 2; // 清理到一半大小
		cacheTimestamps.entrySet().stream().sorted(Map.Entry.comparingByValue())
			.limit(entitySearchResultCache.size() - targetSize)
			.map(Map.Entry::getKey).forEach(CommandUtils::clearCache);
	}
	
	// 获取缓存统计信息
	public static String getCacheStats()
	{
		return String.format("Cache: %d/%d entries, %d queries",
			entitySearchResultCache.size(), MAX_CACHE_SIZE_HARD_LIMIT,
			searchQueryCache.size());
	}
	
	public static void setCurrentPage(String playerKey, int page)
	{
		currentPageCache.put(playerKey, page);
	}
	
	public static MutableText createEntityResultText(EntitySearchResult result,
		int index)
	{
		String fullText =
			result.getEntityType() == EntitySearchResult.EntityType.SIGN
				? String.join("\n", result.getSignText())
				: result.getItemName();
		
		return createGenericResultText(result.getFormattedResult(index),
			result.getPos(), fullText);
	}
	
	private static MutableText createGenericResultText(Text formattedResult,
		BlockPos pos, String fullText)
	{
		MutableText text = (MutableText)formattedResult;
		
		text.styled(style -> style
			.withHoverEvent(new HoverEvent.ShowText(Text
				.translatable("signfinder.tooltip.target_coords", pos.getX(),
					pos.getY(), pos.getZ())
				.append("\n")
				.append(Text.translatable("signfinder.tooltip.full_text"))
				.append("\n" + fullText).append("\n")
				.append(Text.translatable("signfinder.tooltip.click_to_copy"))))
			.withClickEvent(new ClickEvent.CopyToClipboard(
				pos.getX() + "," + pos.getY() + "," + pos.getZ())));
		
		text.append(" ");
		
		text.append(Text.translatable("signfinder.button.remove_highlight")
			.styled(style -> style.withColor(Formatting.RED)
				.withHoverEvent(new HoverEvent.ShowText(
					Text.translatable("signfinder.tooltip.remove_highlight")))
				.withClickEvent(
					new ClickEvent.RunCommand(CommandConstants.COMMAND_PREFIX
						+ " " + CommandConstants.SUBCOMMAND_REMOVE + " "
						+ pos.getX() + " " + pos.getY() + " " + pos.getZ()))));
		
		text.append(" ");
		
		text.append(Text.translatable("signfinder.button.mark")
			.styled(style -> style.withColor(Formatting.YELLOW)
				.withHoverEvent(new HoverEvent.ShowText(
					Text.translatable("signfinder.tooltip.change_color")))
				.withClickEvent(
					new ClickEvent.RunCommand(CommandConstants.COMMAND_PREFIX
						+ " " + CommandConstants.SUBCOMMAND_COLOR + " "
						+ pos.getX() + " " + pos.getY() + " " + pos.getZ()))));
		
		return text;
	}
	
	public static MutableText createPaginationControls(int currentPage,
		int totalPages)
	{
		MutableText pageControl = Text.literal("");
		
		if(currentPage > 1)
		{
			pageControl
				.append(Text.translatable("signfinder.button.previous_page")
					.styled(style -> style.withColor(Formatting.GREEN)
						.withClickEvent(new ClickEvent.RunCommand(
							CommandConstants.COMMAND_PREFIX + " "
								+ CommandConstants.SUBCOMMAND_PAGE + " "
								+ (currentPage - 1)))));
		}
		
		if(currentPage < totalPages)
		{
			pageControl.append(Text.translatable("signfinder.button.next_page")
				.styled(style -> style.withColor(Formatting.GREEN)
					.withClickEvent(new ClickEvent.RunCommand(
						CommandConstants.COMMAND_PREFIX + " "
							+ CommandConstants.SUBCOMMAND_PAGE + " "
							+ (currentPage + 1)))));
		}
		
		// Add clickable page numbers
		if(totalPages > 1)
		{
			pageControl
				.append(createPageNumberControls(currentPage, totalPages));
		}
		
		return pageControl;
	}
	
	private static MutableText createPageNumberControls(int currentPage,
		int totalPages)
	{
		MutableText pageNumbers = Text.literal(" (");
		
		// Calculate page range to display
		int[] pageRange = calculatePageRange(currentPage, totalPages);
		int startPage = pageRange[0];
		int endPage = pageRange[1];
		
		// Add ellipsis at the beginning if we're not showing from page 1
		if(startPage > 1)
		{
			pageNumbers.append(createClickablePageNumber(1, currentPage));
			if(startPage > 2)
			{
				pageNumbers.append(Text.literal(", ..."));
			}
			pageNumbers.append(Text.literal(", "));
		}
		
		// Add page numbers in range
		for(int page = startPage; page <= endPage; page++)
		{
			if(page > startPage)
			{
				pageNumbers.append(Text.literal(", "));
			}
			
			if(page == currentPage)
			{
				// Current page - highlighted, no click
				pageNumbers.append(Text.literal(String.valueOf(page))
					.styled(style -> style.withColor(Formatting.DARK_AQUA)));
			}else
			{
				pageNumbers
					.append(createClickablePageNumber(page, currentPage));
			}
		}
		
		// Add ellipsis at the end if we're not showing to the last page
		if(endPage < totalPages)
		{
			if(endPage < totalPages - 1)
			{
				pageNumbers.append(Text.literal(", ..."));
			}
			pageNumbers.append(Text.literal(", "));
			pageNumbers
				.append(createClickablePageNumber(totalPages, currentPage));
		}
		
		pageNumbers.append(Text.literal(")"));
		return pageNumbers;
	}
	
	private static MutableText createClickablePageNumber(int page,
		int currentPage)
	{
		return Text.literal(String.valueOf(page)).styled(style -> style
			.withColor(Formatting.AQUA)
			.withHoverEvent(new HoverEvent.ShowText(
				Text.translatable("signfinder.tooltip.goto_page", page)))
			.withClickEvent(
				new ClickEvent.RunCommand(CommandConstants.COMMAND_PREFIX + " "
					+ CommandConstants.SUBCOMMAND_PAGE + " " + page)));
	}
	
	private static int[] calculatePageRange(int currentPage, int totalPages)
	{
		if(totalPages <= 6)
		{
			// Show all pages if 6 or fewer
			return new int[]{1, totalPages};
		}
		
		// Show current page ± 3 pages
		int radius = 3;
		int startPage = Math.max(1, currentPage - radius);
		int endPage = Math.min(totalPages, currentPage + radius);
		
		// Adjust range to always show 7 pages when possible
		if(endPage - startPage + 1 < 7)
		{
			if(startPage == 1)
			{
				endPage = Math.min(totalPages, startPage + 6);
			}else if(endPage == totalPages)
			{
				startPage = Math.max(1, endPage - 6);
			}
		}
		
		return new int[]{startPage, endPage};
	}
	
	public static int calculateTotalPages(int resultCount,
		int maxResultsPerPage)
	{
		return (int)Math.ceil((double)resultCount / maxResultsPerPage);
	}
	
	public static int[] getPageIndices(int page, int totalResults,
		int maxResultsPerPage)
	{
		int startIndex = (page - 1) * maxResultsPerPage;
		int endIndex = Math.min(startIndex + maxResultsPerPage, totalResults);
		return new int[]{startIndex, endIndex};
	}
	
	public static MutableText createExportButton(
		SignExportFormat signExportFormat)
	{
		return Text
			.translatable("signfinder.button.export",
				Text.translatable(signExportFormat.toString()))
			.styled(style -> style.withColor(Formatting.BLUE)
				.withHoverEvent(new HoverEvent.ShowText(
					Text.translatable("signfinder.tooltip.export")))
				.withClickEvent(
					new ClickEvent.RunCommand(CommandConstants.COMMAND_PREFIX
						+ " " + CommandConstants.SUBCOMMAND_EXPORT + " "
						+ signExportFormat.name())));
	}
}

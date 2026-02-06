package net.signfinder.commands.core;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.signfinder.core.SignExportFormat;
import net.signfinder.models.EntitySearchResult;

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
	
	public static MutableComponent createEntityResultText(EntitySearchResult result,
                                                   int index)
	{
		String fullText =
			result.getEntityType() == EntitySearchResult.EntityType.SIGN
				? String.join("\n", result.getSignText())
				: result.getItemName();
		
		return createGenericResultText(result.getFormattedResult(index),
			result.getPos(), fullText, result.isLocalData());
	}

    private static MutableComponent createGenericResultText(
                Component formattedResult,
                BlockPos pos,
                String fullText,
                boolean isLocalData
        ) {
            MutableComponent text = formattedResult.copy();

            text.withStyle(style -> style
                    .withHoverEvent(new HoverEvent.ShowText(
                            Component.translatable(
                                            "signfinder.tooltip.target_coords",
                                            pos.getX(), pos.getY(), pos.getZ()
                                    )
                                    .append("\n")
                                    .append(Component.translatable("signfinder.tooltip.full_text"))
                                    .append("\n")
                                    .append(Component.literal(fullText))
                                    .append("\n")
                                    .append(Component.translatable("signfinder.tooltip.click_to_copy"))
                    ))
                    .withClickEvent(new ClickEvent.CopyToClipboard(
                            pos.getX() + "," + pos.getY() + "," + pos.getZ()
                    ))
            );

            if (!isLocalData) {
                text.append(" ");

                text.append(
                        Component.translatable("signfinder.button.remove_highlight")
                                .copy()
                                .withStyle(style -> style
                                        .withColor(ChatFormatting.RED)
                                        .withHoverEvent(new HoverEvent.ShowText(
                                                Component.translatable(
                                                        "signfinder.tooltip.remove_highlight"
                                                )
                                        ))
                                        .withClickEvent(new ClickEvent.RunCommand(
                                                CommandConstants.COMMAND_PREFIX + " "
                                                        + CommandConstants.SUBCOMMAND_REMOVE + " "
                                                        + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                                        ))
                                )
                );

                text.append(" ");

                text.append(
                        Component.translatable("signfinder.button.mark")
                                .copy()
                                .withStyle(style -> style
                                        .withColor(ChatFormatting.GREEN)
                                        .withHoverEvent(new HoverEvent.ShowText(
                                                Component.translatable(
                                                        "signfinder.tooltip.change_color"
                                                )
                                        ))
                                        .withClickEvent(new ClickEvent.RunCommand(
                                                CommandConstants.COMMAND_PREFIX + " "
                                                        + CommandConstants.SUBCOMMAND_COLOR + " "
                                                        + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                                        ))
                                )
                );
            }

            return text;
        }

    public static MutableComponent createPaginationControls(int currentPage, int totalPages)
    {
        MutableComponent pageControl = Component.empty();

        if (currentPage > 1)
        {
            pageControl.append(
                    Component.translatable("signfinder.button.previous_page")
                            .copy()
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.GREEN)
                                    .withClickEvent(new ClickEvent.RunCommand(
                                            CommandConstants.COMMAND_PREFIX + " "
                                                    + CommandConstants.SUBCOMMAND_PAGE + " "
                                                    + (currentPage - 1)
                                    ))
                            )
            );
        }

        if (currentPage < totalPages)
        {
            pageControl.append(
                    Component.translatable("signfinder.button.next_page")
                            .copy()
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.GREEN)
                                    .withClickEvent(new ClickEvent.RunCommand(
                                            CommandConstants.COMMAND_PREFIX + " "
                                                    + CommandConstants.SUBCOMMAND_PAGE + " "
                                                    + (currentPage + 1)
                                    ))
                            )
            );
        }

        if (totalPages > 1)
        {
            pageControl.append(
                    createPageNumberControls(currentPage, totalPages)
            );
        }

        return pageControl;
    }


    private static Component createPageNumberControls(
            int currentPage,
            int totalPages
    ) {
        MutableComponent pageNumbers = Component.literal(" (");

        // Calculate page range to display
        int[] pageRange = calculatePageRange(currentPage, totalPages);
        int startPage = pageRange[0];
        int endPage = pageRange[1];

        // Ellipsis at beginning
        if (startPage > 1) {
            pageNumbers.append(createClickablePageNumber(1, currentPage));
            if (startPage > 2) {
                pageNumbers.append(Component.literal(", ..."));
            }
            pageNumbers.append(Component.literal(", "));
        }

        // Page numbers in range
        for (int page = startPage; page <= endPage; page++) {
            if (page > startPage) {
                pageNumbers.append(Component.literal(", "));
            }

            if (page == currentPage) {
                // current page (highlighted, not clickable)
                pageNumbers.append(
                        Component.literal(String.valueOf(page))
                                .copy()
                                .withStyle(style ->
                                        style.withColor(ChatFormatting.DARK_AQUA)
                                )
                );
            } else {
                pageNumbers.append(
                        createClickablePageNumber(page, currentPage)
                );
            }
        }

        // Ellipsis at end
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                pageNumbers.append(Component.literal(", ..."));
            }
            pageNumbers.append(Component.literal(", "));
            pageNumbers.append(
                    createClickablePageNumber(totalPages, currentPage)
            );
        }

        pageNumbers.append(Component.literal(")"));
        return pageNumbers;
    }

    private static Component createClickablePageNumber(int page, int currentPage)
    {
        return Component.literal(String.valueOf(page))
                .copy()
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable(
                                        "signfinder.tooltip.goto_page",
                                        page
                                )
                        ))
                        .withClickEvent(new ClickEvent.RunCommand(
                                CommandConstants.COMMAND_PREFIX + " "
                                        + CommandConstants.SUBCOMMAND_PAGE + " " + page
                        ))
                );
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
				endPage = startPage + 6;
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

    public static Component createExportButton(SignExportFormat signExportFormat)
    {
        return Component.translatable(
                        "signfinder.button.export",
                        Component.translatable(signExportFormat.toString())
                )
                .withStyle(style -> style
                        .withColor(ChatFormatting.BLUE)
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable("signfinder.tooltip.export")
                        ))
                        .withClickEvent(new ClickEvent.RunCommand(
                                CommandConstants.COMMAND_PREFIX + " "
                                        + CommandConstants.SUBCOMMAND_EXPORT + " "
                                        + signExportFormat.name()
                        ))
                );
    }

}

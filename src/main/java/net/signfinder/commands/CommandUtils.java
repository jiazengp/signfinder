package net.signfinder.commands;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.signfinder.SignSearchResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;

public class CommandUtils
{
	private static final Map<String, List<SignSearchResult>> searchResultCache =
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
	private static final long CACHE_EXPIRY_TIME = 300000; // 5分钟
	
	public static void cacheSearchResults(String playerKey,
		List<SignSearchResult> results, int currentPage, int searchRadius)
	{
		if(searchResultCache.size() > MAX_CACHE_SIZE)
		{
			cleanExpiredCache();
		}
		
		searchResultCache.put(playerKey, results);
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
	
	public static List<SignSearchResult> getCachedResults(String playerKey)
	{
		return searchResultCache.get(playerKey);
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
		searchResultCache.remove(playerKey);
		currentPageCache.remove(playerKey);
		searchRadiusCache.remove(playerKey);
		searchQueryCache.remove(playerKey);
		cacheTimestamps.remove(playerKey);
	}
	
	// 清理所有缓存
	public static void clearAllCaches()
	{
		searchResultCache.clear();
		currentPageCache.clear();
		searchRadiusCache.clear();
		searchQueryCache.clear();
		cacheTimestamps.clear();
	}
	
	// 清理过期缓存
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
	
	// 定期清理过期缓存的公共方法
	public static void performPeriodicCleanup()
	{
		cleanExpiredCache();
	}
	
	public static void setCurrentPage(String playerKey, int page)
	{
		currentPageCache.put(playerKey, page);
	}
	
	public static MutableText createResultText(SignSearchResult result,
		int index)
	{
		MutableText text = (MutableText)result.getFormattedResult(index);
		
		text.styled(style -> style
			.withHoverEvent(new HoverEvent.ShowText(Text
				.translatable("signfinder.tooltip.target_coords",
					result.getPos().getX(), result.getPos().getY(),
					result.getPos().getZ())
				.append("\n")
				.append(Text.translatable("signfinder.tooltip.full_text"))
				.append("\n" + String.join("\n", result.getSignText()))
				.append("\n")
				.append(Text.translatable("signfinder.tooltip.click_to_copy"))))
			.withClickEvent(
				new ClickEvent.CopyToClipboard(result.getPos().getX() + ","
					+ result.getPos().getY() + "," + result.getPos().getZ())));
		
		text.append(" ");
		
		text.append(Text.translatable("signfinder.button.remove_highlight")
			.styled(style -> style.withColor(Formatting.RED)
				.withHoverEvent(new HoverEvent.ShowText(
					Text.translatable("signfinder.tooltip.remove_highlight")))
				.withClickEvent(
					new ClickEvent.RunCommand(CommandConstants.COMMAND_PREFIX
						+ " " + CommandConstants.SUBCOMMAND_REMOVE + " "
						+ result.getPos().getX() + " " + result.getPos().getY()
						+ " " + result.getPos().getZ()))));
		
		text.append(" ");
		
		text.append(Text.translatable("signfinder.button.mark")
			.styled(style -> style.withColor(Formatting.YELLOW)
				.withHoverEvent(new HoverEvent.ShowText(
					Text.translatable("signfinder.tooltip.change_color")))
				.withClickEvent(
					new ClickEvent.RunCommand(CommandConstants.COMMAND_PREFIX
						+ " " + CommandConstants.SUBCOMMAND_COLOR + " "
						+ result.getPos().getX() + " " + result.getPos().getY()
						+ " " + result.getPos().getZ()))));
		
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
		
		return pageControl;
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
	
	public static MutableText createExportButton()
	{
		return Text.translatable("signfinder.button.export_excel")
			.styled(style -> style.withColor(Formatting.BLUE)
				.withHoverEvent(new HoverEvent.ShowText(
					Text.translatable("signfinder.tooltip.export_excel")))
				.withClickEvent(
					new ClickEvent.RunCommand(CommandConstants.COMMAND_PREFIX
						+ " " + CommandConstants.SUBCOMMAND_EXPORT)));
	}
}

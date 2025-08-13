package net.signfinder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.lang.ref.WeakReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.signfinder.util.ChunkUtils;
import net.signfinder.util.ItemFrameUtils;

public class SignSearchEngine
{
	public static final SignSearchEngine INSTANCE = new SignSearchEngine();
	private static final MinecraftClient MC = MinecraftClient.getInstance();
	private static final Logger LOGGER =
		LoggerFactory.getLogger("SignSearchEngine");
	
	// 缓存系统 - 使用WeakReference避免内存泄漏
	private static final Map<BlockPos, WeakReference<CachedSignData>> signCache =
		new ConcurrentHashMap<>();
	private static final Map<String, Pattern> patternCache =
		new LinkedHashMap<>(100, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(
				Map.Entry<String, Pattern> eldest)
			{
				return size() > MAX_PATTERN_CACHE_SIZE;
			}
		};
	private static final int MAX_PATTERN_CACHE_SIZE = 100;
	private static final int MAX_SIGN_CACHE_SIZE = 1000;
	private static final int MAX_RECURSION_DEPTH = 5;
	
	// 缓存的告示牌数据
	private static class CachedSignData
	{
		final String[] lines;
		final String combinedText;
		final long cacheTime;
		
		CachedSignData(String[] lines, String combinedText)
		{
			this.lines = lines.clone();
			this.combinedText = combinedText;
			this.cacheTime = System.currentTimeMillis();
		}
		
		boolean isValid()
		{
			// 缓存1秒后过期，避免告示牌内容变化时显示错误
			return System.currentTimeMillis() - cacheTime < 1000;
		}
		
		boolean isExpired()
		{
			// 5秒后视为过期，用于清理缓存
			return System.currentTimeMillis() - cacheTime > 5000;
		}
	}
	
	public enum SearchType
	{
		TEXT,
		REGEX,
		ARRAY,
		PRESET
	}
	
	public record SearchQuery(String query, SearchType type, int radius,
		boolean caseSensitive)
	{}
	
	public List<SignSearchResult> findAllSigns(Vec3d playerPos, int radius)
	{
		List<SignSearchResult> results = new ArrayList<>();
		List<SignBlockEntity> signs =
			findSignsInRadiusOptimized(playerPos, radius);
		
		for(SignBlockEntity sign : signs)
		{
			CachedSignData signData = getCachedSignData(sign);
			
			results.add(new SignSearchResult(sign.getPos(), playerPos,
				signData.lines, signData.combinedText, 100));
		}
		
		results.sort(Comparator.comparingDouble(SignSearchResult::getDistance));
		return results;
	}
	
	public static List<SignSearchResult> searchSigns(SearchQuery searchQuery,
		SignFinderConfig config)
	{
		List<SignSearchResult> results = new ArrayList<>();
		Vec3d playerPos = Objects.requireNonNull(MC.player).getPos();
		
		List<SignBlockEntity> signs =
			findSignsInRadiusOptimized(playerPos, searchQuery.radius);
		
		for(SignBlockEntity sign : signs)
		{
			CachedSignData signData = getCachedSignData(sign);
			
			if(matchesQuery(signData.combinedText, searchQuery, config, 0))
			{
				results.add(new SignSearchResult(sign.getPos(), playerPos,
					signData.lines, signData.combinedText,
					config.text_preview_length));
			}
		}
		
		results.sort(Comparator.comparingDouble(SignSearchResult::getDistance));
		
		return results;
	}
	
	public static List<EntitySearchResult> searchEntities(
		SearchQuery searchQuery, SignFinderConfig config)
	{
		List<EntitySearchResult> results = new ArrayList<>();
		Vec3d playerPos = Objects.requireNonNull(MC.player).getPos();
		
		EntitySearchRange searchRange = config.entity_search_range;
		
		if(searchRange.includesSigns())
		{
			List<SignBlockEntity> signs =
				findSignsInRadiusOptimized(playerPos, searchQuery.radius);
			
			for(SignBlockEntity sign : signs)
			{
				CachedSignData signData = getCachedSignData(sign);
				
				if(signData == null)
					continue;
				
				if(matchesQuery(signData.combinedText, searchQuery, config, 0))
				{
					results.add(
						new EntitySearchResult(sign, playerPos, signData.lines,
							signData.combinedText, config.text_preview_length));
				}
			}
		}
		
		if(searchRange.includesItemFrames())
		{
			List<ItemFrameEntity> itemFrames =
				findItemFramesInRadius(playerPos, searchQuery.radius);
			
			for(ItemFrameEntity itemFrame : itemFrames)
			{
				if(!isItemFrameStillValid(itemFrame))
					continue;
				
				String itemName = ItemFrameUtils.getItemFrameItemName(itemFrame,
					searchQuery.caseSensitive);
				
				if(!itemName.isEmpty()
					&& matchesQuery(itemName, searchQuery, config, 0))
				{
					results.add(new EntitySearchResult(itemFrame, playerPos,
						itemName, itemName, config.text_preview_length));
				}
			}
		}
		
		results
			.sort(Comparator.comparingDouble(EntitySearchResult::getDistance));
		
		return results;
	}
	
	private static List<SignBlockEntity> findSignsInRadiusOptimized(
		Vec3d center, int radius)
	{
		List<SignBlockEntity> signs = new ArrayList<>();
		double radiusSq = radius * radius;
		
		ChunkUtils.getLoadedBlockEntities().forEach(blockEntity -> {
			if(blockEntity instanceof SignBlockEntity signEntity)
			{
				Vec3d signPos = Vec3d.ofCenter(signEntity.getPos());
				if(center.squaredDistanceTo(signPos) <= radiusSq)
				{
					signs.add(signEntity);
				}
			}
		});
		
		return signs;
	}
	
	private static List<ItemFrameEntity> findItemFramesInRadius(Vec3d center,
		int radius)
	{
		List<ItemFrameEntity> itemFrames = new ArrayList<>();
		double radiusSq = radius * radius;
		
		ChunkUtils.getLoadedEntities().forEach(entity -> {
			if(entity instanceof ItemFrameEntity itemFrame
				&& ItemFrameUtils.hasItem(itemFrame))
			{
				Vec3d itemFramePos = itemFrame.getPos();
				if(center.squaredDistanceTo(itemFramePos) <= radiusSq)
				{
					itemFrames.add(itemFrame);
				}
			}
		});
		
		return itemFrames;
	}
	
	private static CachedSignData getCachedSignData(SignBlockEntity sign)
	{
		BlockPos pos = sign.getPos();
		WeakReference<CachedSignData> weakRef = signCache.get(pos);
		CachedSignData cached = weakRef != null ? weakRef.get() : null;
		
		// 检查缓存是否有效且告示牌仍然存在
		if(cached != null && cached.isValid() && isSignStillValid(sign))
		{
			return cached;
		}
		
		// 如果告示牌不再有效，清理缓存
		if(!isSignStillValid(sign))
		{
			signCache.remove(pos);
			return null;
		}
		
		String[] lines = extractSignText(sign);
		String combinedText = String.join(" ", lines);
		CachedSignData newData = new CachedSignData(lines, combinedText);
		
		signCache.put(pos, new WeakReference<>(newData));
		
		// 定期清理过期缓存
		if(signCache.size() > MAX_SIGN_CACHE_SIZE)
		{
			cleanExpiredCache();
		}
		
		return newData;
	}
	
	// 检查告示牌是否仍然有效
	private static boolean isSignStillValid(SignBlockEntity sign)
	{
		if(MC.world == null || sign == null)
			return false;
		
		// 检查世界中该位置是否仍有相同的告示牌实体
		return MC.world.getBlockEntity(sign.getPos()) == sign;
	}
	
	// 检查物品展示框是否仍然有效
	private static boolean isItemFrameStillValid(ItemFrameEntity itemFrame)
	{
		if(MC.world == null || itemFrame == null)
			return false;
		
		// 检查实体是否仍然存活且在正确位置
		return !itemFrame.isRemoved() && ItemFrameUtils.hasItem(itemFrame);
	}
	
	private static String[] extractSignText(SignBlockEntity sign)
	{
		String[] lines = new String[4];
		for(int i = 0; i < 4; i++)
		{
			Text text = sign.getFrontText().getMessage(i, false);
			lines[i] = text.getString();
		}
		return lines;
	}
	
	private static void cleanExpiredCache()
	{
		signCache.entrySet().removeIf(entry -> {
			CachedSignData data = entry.getValue().get();
			return data == null || data.isExpired();
		});
	}
	
	private static boolean matchesQuery(String text, SearchQuery query,
		SignFinderConfig config, int recursionDepth)
	{
		if(recursionDepth > MAX_RECURSION_DEPTH)
		{
			return false; // 防止递归过深
		}
		
		String searchText = query.caseSensitive ? text : text.toLowerCase();
		String queryText =
			query.caseSensitive ? query.query : query.query.toLowerCase();
		
		switch(query.type)
		{
			case TEXT:
			return searchText.contains(queryText);
			
			case REGEX:
			try
			{
				Pattern pattern =
					getCachedPattern(queryText, query.caseSensitive);
				return pattern.matcher(searchText).find();
			}catch(PatternSyntaxException e)
			{
				// 记录正则表达式错误并回退到文本搜索
				LOGGER.warn(
					"Invalid regex pattern '{}': {}. Falling back to text search.",
					queryText, e.getMessage());
				return searchText.contains(queryText);
			}catch(Exception e)
			{
				// 记录其他异常
				LOGGER.error(
					"Unexpected error during regex matching for pattern '{}': {}",
					queryText, e.getMessage());
				return false;
			}
			
			case ARRAY:
			String[] keywords = queryText.split("[,，]");
			return Arrays.stream(keywords).map(String::trim)
				.anyMatch(keyword -> searchText.contains(
					query.caseSensitive ? keyword : keyword.toLowerCase()));
			
			case PRESET:
			// 从预设中获取实际查询文本
			String presetQuery = getPresetQuery(queryText, config);
			if(presetQuery == null)
				return false;
			
			// 检查预设是否为正则表达式
			if(config.search_presets.regex_presets.containsKey(queryText))
			{
				SearchQuery regexQuery = new SearchQuery(presetQuery,
					SearchType.REGEX, query.radius, query.caseSensitive);
				return matchesQuery(text, regexQuery, config,
					recursionDepth + 1);
			}else
			{
				SearchQuery arrayQuery = new SearchQuery(presetQuery,
					SearchType.ARRAY, query.radius, query.caseSensitive);
				return matchesQuery(text, arrayQuery, config,
					recursionDepth + 1);
			}
			
			default:
			return false;
		}
	}
	
	// 获取缓存的正则表达式模式
	private static Pattern getCachedPattern(String regex, boolean caseSensitive)
	{
		String cacheKey = (caseSensitive ? "cs:" : "ci:") + regex;
		
		synchronized(patternCache)
		{
			Pattern pattern = patternCache.get(cacheKey);
			
			if(pattern == null)
			{
				pattern = caseSensitive ? Pattern.compile(regex)
					: Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
				
				// LinkedHashMap会自动管理LRU，不需要手动清理
				patternCache.put(cacheKey, pattern);
			}
			
			return pattern;
		}
	}
	
	// 清理缓存的公共方法
	public static void clearCaches()
	{
		signCache.clear();
		synchronized(patternCache)
		{
			patternCache.clear();
		}
	}
	
	// 定期清理过期缓存的方法
	public static void performPeriodicCleanup()
	{
		// 清理过期的告示牌缓存
		cleanExpiredCache();
	}
	
	private static String getPresetQuery(String presetName,
		SignFinderConfig config)
	{
		if(config.search_presets.text_presets.containsKey(presetName))
			return config.search_presets.text_presets.get(presetName);
		
		if(config.search_presets.regex_presets.containsKey(presetName))
			return config.search_presets.regex_presets.get(presetName);
		
		return null;
	}
	
}

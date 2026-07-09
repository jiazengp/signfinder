package net.signfinder.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.signfinder.services.CacheService;
import net.signfinder.util.SignTextUtils;

/**
 * Thread-safe cache for sign data with automatic expiration and validation.
 * Uses time-based expiry to prevent memory leaks.
 */
public class SignDataCache
	implements CacheService<BlockPos, SignDataCache.SignData>
{
	private static final int MAX_CACHE_SIZE = 1000;
	private static final long CACHE_VALIDITY_MS = 5000;
	private static final long CACHE_EXPIRY_MS = 10000;
	
	private final Map<BlockPos, SignData> cache = new ConcurrentHashMap<>();
	
	@Override
	public Optional<SignDataCache.SignData> get(BlockPos pos)
	{
		SignData data = cache.get(pos);
		if(data == null)
		{
			return Optional.empty();
		}
		
		if(!data.isValid())
		{
			cache.remove(pos);
			return Optional.empty();
		}
		
		return Optional.of(data);
	}
	
	@Override
	public void put(BlockPos pos, SignDataCache.SignData data)
	{
		cache.put(pos, data);
		
		// Cleanup if cache gets too large
		if(cache.size() > MAX_CACHE_SIZE)
		{
			cleanExpired();
		}
	}
	
	@Override
	public void remove(BlockPos pos)
	{
		cache.remove(pos);
	}
	
	@Override
	public void clear()
	{
		cache.clear();
	}
	
	@Override
	public int cleanExpired()
	{
		int removed = 0;
		var iterator = cache.entrySet().iterator();
		
		while(iterator.hasNext())
		{
			var entry = iterator.next();
			SignData data = entry.getValue();
			
			if(data == null || data.isExpired())
			{
				iterator.remove();
				removed++;
			}
		}
		
		return removed;
	}
	
	@Override
	public int size()
	{
		return cache.size();
	}
	
	/**
	 * Creates SignData from a sign block entity.
	 */
	public SignData createSignData(SignBlockEntity sign)
	{
		String[] lines = SignTextUtils.getSignTextArray(sign);
		
		String combinedText = String.join(" ", lines);
		return new SignData(lines, combinedText, System.currentTimeMillis());
	}
	
	/**
	 * Immutable cached sign data with expiration.
	 */
	public record SignData(String[] lines, String combinedText, long cacheTime)
	{
		public SignData(String[] lines, String combinedText, long cacheTime)
		{
			this.lines = lines.clone();
			this.combinedText = combinedText;
			this.cacheTime = cacheTime;
		}
		
		@Override
		public String[] lines()
		{
			return lines.clone();
		}
		
		public boolean isValid()
		{
			return System.currentTimeMillis() - cacheTime < CACHE_VALIDITY_MS;
		}
		
		public boolean isExpired()
		{
			return System.currentTimeMillis() - cacheTime > CACHE_EXPIRY_MS;
		}
	}
}

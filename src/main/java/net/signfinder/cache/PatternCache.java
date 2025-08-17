package net.signfinder.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.signfinder.services.CacheService;

/**
 * Thread-safe LRU cache for compiled regex patterns.
 * Prevents memory leaks from excessive pattern compilation.
 */
public class PatternCache implements CacheService<String, Pattern>
{
	private static final int MAX_CACHE_SIZE = 100;
	
	private final Map<String, Pattern> cache =
		new LinkedHashMap<>(MAX_CACHE_SIZE + 1, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(
				Map.Entry<String, Pattern> eldest)
			{
				return size() > MAX_CACHE_SIZE;
			}
		};
	
	@Override
	public synchronized Optional<Pattern> get(String key)
	{
		Pattern pattern = cache.get(key);
		return Optional.ofNullable(pattern);
	}
	
	@Override
	public synchronized void put(String key, Pattern pattern)
	{
		cache.put(key, pattern);
	}
	
	@Override
	public synchronized void remove(String key)
	{
		cache.remove(key);
	}
	
	@Override
	public synchronized void clear()
	{
		cache.clear();
	}
	
	@Override
	public synchronized int cleanExpired()
	{
		// Patterns don't expire, only LRU eviction
		return 0;
	}
	
	@Override
	public synchronized int size()
	{
		return cache.size();
	}
	
	/**
	 * Gets or compiles a regex pattern with specified case sensitivity.
	 *
	 * @param regex
	 *            Regular expression string
	 * @param caseSensitive
	 *            Whether to compile with case sensitivity
	 * @return Optional containing compiled pattern, empty if invalid regex
	 */
	public Optional<Pattern> getOrCompile(String regex, boolean caseSensitive)
	{
		String cacheKey = (caseSensitive ? "cs:" : "ci:") + regex;
		
		Optional<Pattern> cached = get(cacheKey);
		if(cached.isPresent())
		{
			return cached;
		}
		
		try
		{
			Pattern pattern = caseSensitive ? Pattern.compile(regex)
				: Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			
			put(cacheKey, pattern);
			return Optional.of(pattern);
		}catch(PatternSyntaxException e)
		{
			// Invalid regex, don't cache
			return Optional.empty();
		}
	}
}

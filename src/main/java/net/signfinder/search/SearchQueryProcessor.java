package net.signfinder.search;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.signfinder.SignFinderConfig;
import net.signfinder.cache.PatternCache;
import net.signfinder.services.SearchQuery;
import net.signfinder.services.SearchQuery.SearchType;

/**
 * Processes search queries and matches text content.
 * Handles different search types with proper fallback logic.
 */
public class SearchQueryProcessor
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(SearchQueryProcessor.class);
	private static final int MAX_RECURSION_DEPTH = 5;
	
	private final PatternCache patternCache;
	
	public SearchQueryProcessor(PatternCache patternCache)
	{
		this.patternCache = patternCache;
	}
	
	/**
	 * Checks if text matches the search query.
	 *
	 * @param text
	 *            Text to check
	 * @param query
	 *            Search query
	 * @param config
	 *            Configuration for presets
	 * @return true if text matches query
	 */
	public boolean matches(String text, SearchQuery query,
		SignFinderConfig config)
	{
		return matches(text, query, config, 0);
	}
	
	private boolean matches(String text, SearchQuery query,
		SignFinderConfig config, int depth)
	{
		if(depth > MAX_RECURSION_DEPTH)
		{
			LOGGER.warn("Maximum recursion depth exceeded for query: {}",
				query.query());
			return false;
		}
		
		String searchText = query.caseSensitive() ? text : text.toLowerCase();
		String queryText =
			query.caseSensitive() ? query.query() : query.query().toLowerCase();
		
		return switch(query.type())
		{
			case TEXT -> searchText.contains(queryText);
			case REGEX -> matchesRegex(searchText, queryText,
				query.caseSensitive());
			case ARRAY -> matchesArray(searchText, queryText);
			case PRESET -> matchesPreset(text, query, config, depth);
		};
	}
	
	private boolean matchesRegex(String text, String pattern,
		boolean caseSensitive)
	{
		try
		{
			Optional<Pattern> compiledPattern =
				patternCache.getOrCompile(pattern, caseSensitive);
			
			if(compiledPattern.isPresent())
			{
				return compiledPattern.get().matcher(text).find();
			}else
			{
				LOGGER.warn(
					"Invalid regex pattern '{}', falling back to text search",
					pattern);
				return text.contains(pattern);
			}
		}catch(Exception e)
		{
			LOGGER.error(
				"Unexpected error during regex matching for pattern '{}': {}",
				pattern, e.getMessage());
			return false;
		}
	}
	
	private boolean matchesArray(String text, String keywords)
	{
		String[] keywordArray = keywords.split("[,，]");
		
		return Arrays.stream(keywordArray).map(String::trim)
			.filter(keyword -> !keyword.isEmpty()).anyMatch(text::contains);
	}
	
	private boolean matchesPreset(String text, SearchQuery query,
		SignFinderConfig config, int depth)
	{
		String presetQuery = getPresetQuery(query.query(), config);
		if(presetQuery == null)
		{
			LOGGER.warn("Preset '{}' not found", query.query());
			return false;
		}
		
		SearchType presetType = determinePresetType(query.query(), config);
		SearchQuery expandedQuery = new SearchQuery(presetQuery, presetType,
			query.radius(), query.caseSensitive());
		
		return matches(text, expandedQuery, config, depth + 1);
	}
	
	private String getPresetQuery(String presetName, SignFinderConfig config)
	{
		if(config.search_presets.text_presets.containsKey(presetName))
		{
			return config.search_presets.text_presets.get(presetName);
		}
		
		if(config.search_presets.regex_presets.containsKey(presetName))
		{
			return config.search_presets.regex_presets.get(presetName);
		}
		
		return null;
	}
	
	private SearchType determinePresetType(String presetName,
		SignFinderConfig config)
	{
		if(config.search_presets.regex_presets.containsKey(presetName))
		{
			return SearchType.REGEX;
		}
		
		// Default to ARRAY for text presets (allows comma-separated values)
		return SearchType.ARRAY;
	}
}

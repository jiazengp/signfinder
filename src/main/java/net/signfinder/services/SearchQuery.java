package net.signfinder.services;

/**
 * Immutable search query record containing all search parameters.
 * Provides type-safe encapsulation of search criteria.
 */
public record SearchQuery(String query, SearchType type, int radius,
	boolean caseSensitive)
{
	public enum SearchType
	{
		TEXT,
		REGEX,
		ARRAY,
		PRESET
	}
	
	/**
	 * Creates a basic text search query.
	 */
	public static SearchQuery text(String query, int radius,
		boolean caseSensitive)
	{
		return new SearchQuery(query, SearchType.TEXT, radius, caseSensitive);
	}
	
	/**
	 * Creates a regex search query.
	 */
	public static SearchQuery regex(String pattern, int radius,
		boolean caseSensitive)
	{
		return new SearchQuery(pattern, SearchType.REGEX, radius,
			caseSensitive);
	}
	
	/**
	 * Creates an array (comma-separated) search query.
	 */
	public static SearchQuery array(String keywords, int radius,
		boolean caseSensitive)
	{
		return new SearchQuery(keywords, SearchType.ARRAY, radius,
			caseSensitive);
	}
	
	/**
	 * Creates a preset search query.
	 */
	public static SearchQuery preset(String presetName, int radius,
		boolean caseSensitive)
	{
		return new SearchQuery(presetName, SearchType.PRESET, radius,
			caseSensitive);
	}
}

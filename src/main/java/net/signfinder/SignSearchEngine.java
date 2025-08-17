package net.signfinder;

import java.util.List;

import net.minecraft.util.math.Vec3d;
import net.signfinder.cache.PatternCache;
import net.signfinder.cache.SignDataCache;
import net.signfinder.cache.LocalDataCacheManager;
import net.signfinder.managers.AutoSaveManager;
import net.signfinder.models.SignSearchResult;
import net.signfinder.search.EntitySearchService;
import net.signfinder.search.SearchQueryProcessor;
import net.signfinder.services.SearchService;

/**
 * Refactored SignSearchEngine using clean architecture.
 * Delegates to specialized services for better maintainability.
 */
public class SignSearchEngine
{
	public static final SignSearchEngine INSTANCE = new SignSearchEngine();
	
	private final SearchService searchService;
	
	private SignSearchEngine()
	{
		// Initialize service dependencies
		SignDataCache signCache = new SignDataCache();
		PatternCache patternCache = new PatternCache();
		SearchQueryProcessor queryProcessor =
			new SearchQueryProcessor(patternCache);
		LocalDataCacheManager localDataManager =
			new LocalDataCacheManager(AutoSaveManager.INSTANCE, queryProcessor);
		
		searchService = new EntitySearchService(signCache, queryProcessor,
			localDataManager);
	}
	
	public List<SignSearchResult> findAllSigns(Vec3d playerPos, int radius)
	{
		return searchService.findAllSigns(playerPos, radius);
	}
	
	public void clearCaches()
	{
		searchService.clearCaches();
	}
	
	public void performPeriodicCleanup()
	{
		searchService.performPeriodicCleanup();
	}
	
}

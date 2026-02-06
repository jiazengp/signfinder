package net.signfinder;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import net.signfinder.commands.core.CommandUtils;
import net.signfinder.managers.AutoSaveManager;
import net.signfinder.managers.ColorManager;
import net.signfinder.managers.EntityDetectionManager;
import net.signfinder.managers.HighlightRenderManager;
import net.signfinder.managers.KeyMappingHandler;
import net.signfinder.managers.SearchResultManager;
import net.signfinder.models.EntitySearchResult;
import net.signfinder.services.SearchService;
import net.signfinder.services.ServiceRegistry;
import net.signfinder.cache.LocalDataCacheManager;
import net.signfinder.cache.SignDataCache;
import net.signfinder.cache.PatternCache;
import net.signfinder.detection.AutoDetectionCacheService;
import net.signfinder.search.EntitySearchService;
import net.signfinder.search.SearchQueryProcessor;

public final class SignFinderMod
{
	private static final Minecraft MC = Minecraft.getInstance();
	public static final String MOD_ID = "signfinder";
	public static final Logger LOGGER =
		LoggerFactory.getLogger(MOD_ID.toUpperCase());
	
	public final ConfigHolder<SignFinderConfig> configHolder;
	private final boolean enabled;
	
	private final EntityDetectionManager detectionManager;
	private final SearchResultManager searchResultManager;
	private final ColorManager colorManager;
	private final HighlightRenderManager renderManager;
	private SearchService searchService;
	private LocalDataCacheManager localDataManager;
	private AutoDetectionCacheService autoDetectionCache;
	
	private int cacheCleanupCounter = 0;
	private static final int CACHE_CLEANUP_INTERVAL = 6000;
	private int autoSaveCounter = 0;
	
	public SignFinderMod()
	{
		LOGGER.info("Starting SignFinder...");
		
		configHolder = AutoConfig.register(SignFinderConfig.class,
			GsonConfigSerializer::new);
		enabled = true;
		
		// Initialize services and register them
		initializeServices();
		
		// Initialize managers using dependency injection
		detectionManager = new EntityDetectionManager();
		searchResultManager = new SearchResultManager(detectionManager);
		colorManager = new ColorManager(searchResultManager);
		renderManager = new HighlightRenderManager(colorManager);
		
		// Initialize other components
		new KeyMappingHandler(configHolder, detectionManager);
		
		ClientCommandRegistrationCallback.EVENT.register((dispatcher,
			registryAccess) -> SignSearchCommand.register(dispatcher));
		
		LOGGER.info("SignFinder initialized with {} services",
			ServiceRegistry.getServiceCount());
	}
	
	private void initializeServices()
	{
		// Initialize caches
		SignDataCache signCache = new SignDataCache();
		PatternCache patternCache = new PatternCache();
		SearchQueryProcessor queryProcessor =
			new SearchQueryProcessor(patternCache);
		
		// Register core services
		ServiceRegistry.registerService(SignDataCache.class, signCache);
		ServiceRegistry.registerService(PatternCache.class, patternCache);
		ServiceRegistry.registerService(SearchQueryProcessor.class,
			queryProcessor);
		
		// Initialize and register data services
		localDataManager =
			new LocalDataCacheManager(AutoSaveManager.INSTANCE, queryProcessor);
		autoDetectionCache = new AutoDetectionCacheService(localDataManager);
		searchService = new EntitySearchService(signCache, queryProcessor,
			localDataManager);
		
		ServiceRegistry.registerService(LocalDataCacheManager.class,
			localDataManager);
		ServiceRegistry.registerService(AutoDetectionCacheService.class,
			autoDetectionCache);
		ServiceRegistry.registerService(SearchService.class, searchService);
	}
	
	public void onUpdate()
	{
		if(!isEnabled() || MC.player == null || MC.level == null)
			return;
		
		SignFinderConfig config = configHolder.getConfig();
		
		performPeriodicCacheCleanup();
		performPeriodicAutoSave(config);
		
		if(config.auto_remove_on_approach)
		{
			searchResultManager.removeNearbyResults(config);
		}
		
		detectionManager.performAutoDetection(config);
		
		// Handle auto-detection cache maintenance separately
		autoDetectionCache.performMaintenance(config);
		
		// Periodic cleanup of search caches
		searchService.performPeriodicCleanup();
	}
	
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(!isEnabled())
			return;
		
		SignFinderConfig config = configHolder.getConfig();
		
		renderManager.renderHighlights(matrixStack, partialTicks, config,
			searchResultManager.getSearchResultSigns(),
			searchResultManager.getSearchResultItemFrames(),
			detectionManager.getHighlightedSigns(),
			detectionManager.getHighlightedItemFrames());
	}
	
	public boolean shouldCancelViewBobbing()
	{
		return isEnabled()
			&& configHolder.getConfig().highlight_style.hasLines();
	}
	
	public void setEntitySearchResults(List<EntitySearchResult> results)
	{
		searchResultManager.setSearchResults(results);
	}
	
	public void clearSearchResults()
	{
		searchResultManager.clearResults();
		colorManager.clearCustomColors();
	}
	
	public boolean removeSearchResultByPos(int x, int y, int z)
	{
		BlockPos targetPos = new BlockPos(x, y, z);
		boolean removed = searchResultManager.removeResultByPos(x, y, z);
		if(removed)
		{
			colorManager.removeCustomColor(targetPos);
		}
		return removed;
	}
	
	public String cycleHighlightColor(int x, int y, int z)
	{
		return colorManager.cycleHighlightColor(x, y, z);
	}
	
	public int getSignHighlightColor(BlockPos pos)
	{
		return colorManager.getHighlightColor(pos);
	}
	
	public void cleanup()
	{
		detectionManager.cleanup();
		searchResultManager.clearResults();
		colorManager.clearCustomColors();
		
		searchService.clearCaches();
		CommandUtils.clearAllCaches();
		
		LOGGER.info("SignFinder cleanup completed");
	}
	
	private void performPeriodicCacheCleanup()
	{
		cacheCleanupCounter++;
		if(cacheCleanupCounter >= CACHE_CLEANUP_INTERVAL)
		{
			cacheCleanupCounter = 0;
			performCacheCleanup();
		}
	}
	
	private void performPeriodicAutoSave(SignFinderConfig config)
	{
		if(!config.auto_save_detection_data)
			return;
		
		autoSaveCounter++;
		int autoSaveInterval = config.auto_save_interval_seconds * 20; // Convert
																		// seconds
																		// to
																		// ticks
																		// (20
																		// ticks
																		// per
																		// second)
		
		if(autoSaveCounter >= autoSaveInterval)
		{
			autoSaveCounter = 0;
			try
			{
				AutoSaveManager.INSTANCE.checkAndSave();
				LOGGER.debug("Periodic auto-save completed");
			}catch(Exception e)
			{
				LOGGER.warn("Error during auto-save", e);
			}
		}
	}
	
	private void performCacheCleanup()
	{
		try
		{
			searchService.performPeriodicCleanup();
			CommandUtils.performPeriodicCleanup();
			LOGGER.debug("Periodic cache cleanup completed");
		}catch(Exception e)
		{
			LOGGER.warn("Error during cache cleanup", e);
		}
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public ConfigHolder<SignFinderConfig> getConfigHolder()
	{
		return configHolder;
	}
	
	public SignFinderConfig getConfig()
	{
		return configHolder.getConfig();
	}
	
	public static SignFinderMod getInstance()
	{
		return SignFinderModInitializer.getInstance();
	}
	
	// Test access methods for validation
	public SearchResultManager getSearchResultManager()
	{
		return searchResultManager;
	}
	
	public EntityDetectionManager getDetectionManager()
	{
		return detectionManager;
	}
	
	public SearchService getSearchService()
	{
		return searchService;
	}
	
	public LocalDataCacheManager getLocalDataManager()
	{
		return localDataManager;
	}
	
	public AutoDetectionCacheService getAutoDetectionCache()
	{
		return autoDetectionCache;
	}
}

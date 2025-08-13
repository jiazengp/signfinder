package net.signfinder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.signfinder.commands.CommandUtils;
import net.signfinder.managers.ColorManager;
import net.signfinder.managers.EntityDetectionManager;
import net.signfinder.managers.HighlightRenderManager;
import net.signfinder.managers.KeyBindingHandler;
import net.signfinder.managers.SearchResultManager;

public final class SignFinderMod
{
	private static final MinecraftClient MC = MinecraftClient.getInstance();
	public static final String MOD_ID = "signfinder";
	public static final Logger LOGGER =
		LoggerFactory.getLogger(MOD_ID.toUpperCase());
	
	public final ConfigHolder<SignFinderConfig> configHolder;
	private final boolean enabled;
	
	private final EntityDetectionManager detectionManager;
	private final SearchResultManager searchResultManager;
	private final ColorManager colorManager;
	private final HighlightRenderManager renderManager;
	
	private int cacheCleanupCounter = 0;
	private static final int CACHE_CLEANUP_INTERVAL = 6000;
	
	public SignFinderMod()
	{
		LOGGER.info("Starting SignFinder...");
		
		configHolder = AutoConfig.register(SignFinderConfig.class,
			GsonConfigSerializer::new);
		enabled = true;
		
		detectionManager = new EntityDetectionManager();
		searchResultManager = new SearchResultManager(detectionManager);
		colorManager = new ColorManager(searchResultManager);
		renderManager = new HighlightRenderManager(colorManager);
		
		new KeyBindingHandler(configHolder, detectionManager);
		
		ClientCommandRegistrationCallback.EVENT.register((dispatcher,
			registryAccess) -> SignSearchCommand.register(dispatcher));
	}
	
	public void onUpdate()
	{
		if(!isEnabled() || MC.player == null || MC.world == null)
			return;
		
		SignFinderConfig config = configHolder.getConfig();
		
		performPeriodicCacheCleanup();
		
		if(config.auto_remove_nearby)
		{
			searchResultManager.removeNearbyResults(config);
		}
		
		detectionManager.performAutoDetection(config);
	}
	
	public void onRender(MatrixStack matrixStack, float partialTicks)
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
		
		// 清理全局缓存
		SignSearchEngine.clearCaches();
		CommandUtils.clearAllCaches();
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
	
	private void performCacheCleanup()
	{
		try
		{
			SignSearchEngine.performPeriodicCleanup();
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
}

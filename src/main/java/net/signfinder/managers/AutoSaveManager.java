package net.signfinder.managers;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.signfinder.models.SignSearchResult;
import net.signfinder.services.DataPersistenceService;
import net.signfinder.services.DataValidationService;
import net.signfinder.services.FileOperationService;
import net.signfinder.services.LocalDataCacheService;

import static net.signfinder.SignFinderMod.LOGGER;

/**
 * Coordinated auto-save functionality using service-oriented architecture.
 * This class acts as a facade for the underlying data persistence services.
 */
public enum AutoSaveManager
{
	INSTANCE;
	
	private final DataPersistenceService persistenceService;
	private final DataValidationService validationService;
	private final LocalDataCacheService cacheService;
	
	AutoSaveManager()
	{
		// Initialize services
		persistenceService = new FileOperationService();
		validationService = new DataValidationService();
		cacheService =
			new LocalDataCacheService(validationService, persistenceService);
		
		LOGGER.info(
			"AutoSaveManager initialized with service-oriented architecture");
	}
	
	public void addDetectedSign(SignSearchResult result)
	{
		cacheService.addDetectedSign(result);
	}
	
	public void removeDetectedSign(BlockPos pos)
	{
		cacheService.removeDetectedSign(pos);
	}
	
	public void checkAndSave()
	{
		cacheService.checkAndSave();
	}
	
	public List<SignSearchResult> getLocalData()
	{
		return cacheService.getLocalData();
	}
	
	public void validateCachedMemoryData()
	{
		cacheService.validateCachedMemoryData();
	}
	
	public void cleanupSavedLocalData()
	{
		cacheService.cleanupCachedLocalData();
	}
	
	// Getters for services (for testing and advanced usage)
	public DataPersistenceService getPersistenceService()
	{
		return persistenceService;
	}
	
	public DataValidationService getValidationService()
	{
		return validationService;
	}
	
	public LocalDataCacheService getCacheService()
	{
		return cacheService;
	}
	
	/**
	 * Data class for saved sign information.
	 * Moved here to maintain API compatibility.
	 */
	public static class SavedSignData
	{
		public int x, y, z;
		public String[] signText;
		public String matchedText;
		public long updateTime;
	}
}

package net.signfinder.services;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import net.signfinder.SignFinderConfig;
import net.signfinder.models.EntitySearchResult;
import net.signfinder.models.SignSearchResult;

/**
 * Core search service interface for finding signs and item frames.
 * Provides unified search capabilities with configurable parameters.
 */
public interface SearchService
{
	/**
	 * Search for entities (signs/item frames) based on query and configuration.
	 *
	 * @param query
	 *            Search query containing type, text, radius and case
	 *            sensitivity
	 * @param config
	 *            Application configuration
	 * @return List of matching entities sorted by distance
	 */
	List<EntitySearchResult> searchEntities(SearchQuery query,
		SignFinderConfig config);
	
	/**
	 * Find all signs within specified radius without filtering.
	 *
	 * @param playerPos
	 *            Player position for distance calculation
	 * @param radius
	 *            Search radius in blocks
	 * @return List of all signs found within radius
	 */
	List<SignSearchResult> findAllSigns(Vec3 playerPos, int radius);
	
	/**
	 * Clear all cached data to free memory.
	 */
	void clearCaches();
	
	/**
	 * Perform periodic cleanup of expired cache entries.
	 */
	void performPeriodicCleanup();
}

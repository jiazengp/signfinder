package net.signfinder.services;

import java.util.Optional;

/**
 * Generic cache service interface for managing cached data.
 * Provides common cache operations with type safety.
 */
public interface CacheService<K, V>
{
	/**
	 * Retrieves cached value for the given key.
	 *
	 * @param key
	 *            Cache key
	 * @return Optional containing value if present and valid
	 */
	Optional<V> get(K key);
	
	/**
	 * Stores value in cache with the given key.
	 *
	 * @param key
	 *            Cache key
	 * @param value
	 *            Value to cache
	 */
	void put(K key, V value);
	
	/**
	 * Removes entry from cache.
	 *
	 * @param key
	 *            Cache key to remove
	 */
	void remove(K key);
	
	/**
	 * Clears all cache entries.
	 */
	void clear();
	
	/**
	 * Removes expired entries from cache.
	 *
	 * @return Number of entries removed
	 */
	int cleanExpired();
	
	/**
	 * Gets current cache size.
	 *
	 * @return Number of entries in cache
	 */
	int size();
}

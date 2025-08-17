package net.signfinder.services;

import java.util.List;
import java.util.Map;

import net.signfinder.managers.AutoSaveManager.SavedSignData;

/**
 * Interface for data persistence operations.
 * Handles saving and loading of detection data to/from files.
 */
public interface DataPersistenceService
{
	/**
	 * Save detection data to file system.
	 *
	 * @param data
	 *            The data to save, organized by world key
	 * @return true if save was successful
	 */
	boolean saveDetectionData(Map<String, List<SavedSignData>> data);
	
	/**
	 * Load detection data from file system.
	 *
	 * @return The loaded data, empty map if no data exists
	 */
	Map<String, List<SavedSignData>> loadDetectionData();
	
	/**
	 * Get the save directory path.
	 *
	 * @return Path to the auto-save directory
	 */
	String getSaveDirectory();
	
	/**
	 * Check if auto-save data exists.
	 *
	 * @return true if save files exist
	 */
	boolean hasSavedData();
}

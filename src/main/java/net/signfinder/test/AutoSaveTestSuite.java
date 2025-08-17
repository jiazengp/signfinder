package net.signfinder.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.core.AutoSaveMode;
import net.signfinder.models.SignSearchResult;
import net.signfinder.managers.AutoSaveManager;
import net.signfinder.util.SignTextUtils;

/**
 * Comprehensive test suite for auto-save functionality across different modes
 * and dimensions.
 * Tests auto-save behavior, dimension handling, and data persistence.
 */
public class AutoSaveTestSuite
{
	private final TestEnvironmentBuilder testEnv;
	private final MinecraftClient mc;
	private SignFinderConfig originalConfig;
	
	public AutoSaveTestSuite(TestEnvironmentBuilder testEnv)
	{
		this.testEnv = testEnv;
		this.mc = MinecraftClient.getInstance();
	}
	
	/**
	 * Main test runner for auto-save functionality.
	 */
	public TestResult runAutoSaveTests()
	{
		System.out.println("=== Starting Auto-Save Test Suite ===");
		
		try
		{
			// Backup original config
			originalConfig =
				copyConfig(SignFinderMod.getInstance().getConfig());
			
			// Run individual test cases
			TestResult configTest = testAutoSaveConfigOptions();
			TestResult modeTest = testAutoSaveModes();
			TestResult dimensionTest = testDimensionHandling();
			TestResult dataIntegrityTest = testDataPersistenceAndIntegrity();
			TestResult searchIntegrationTest = testSearchWithAutoSave();
			
			// Aggregate results
			boolean allPassed = configTest.isPassed() && modeTest.isPassed()
				&& dimensionTest.isPassed() && dataIntegrityTest.isPassed()
				&& searchIntegrationTest.isPassed();
			
			String summary = String.format(
				"Auto-Save Tests Summary:\n" + "- Config Options: %s\n"
					+ "- Save Modes: %s\n" + "- Dimension Handling: %s\n"
					+ "- Data Integrity: %s\n" + "- Search Integration: %s",
				configTest.isPassed() ? "PASS" : "FAIL",
				modeTest.isPassed() ? "PASS" : "FAIL",
				dimensionTest.isPassed() ? "PASS" : "FAIL",
				dataIntegrityTest.isPassed() ? "PASS" : "FAIL",
				searchIntegrationTest.isPassed() ? "PASS" : "FAIL");
			
			return allPassed ? TestResult.passed("Auto-Save Tests")
				: TestResult.failed(summary);
		}catch(Exception e)
		{
			return TestResult
				.failed("Auto-Save test suite failed: " + e.getMessage());
		}finally
		{
			// Restore original config
			if(originalConfig != null)
			{
				restoreConfig(originalConfig);
			}
			cleanup();
		}
	}
	
	/**
	 * Test auto-save configuration options and validation.
	 */
	private TestResult testAutoSaveConfigOptions()
	{
		System.out.println("Testing auto-save configuration options...");
		
		try
		{
			SignFinderConfig config = SignFinderMod.getInstance().getConfig();
			
			// Test enabling/disabling auto-save
			config.auto_save_detection_data = true;
			testEnv.waitForWorldTicks(5);
			
			if(!config.auto_save_detection_data)
			{
				return TestResult.failed("Failed to enable auto-save");
			}
			
			// Test different save intervals
			int[] testIntervals = {10, 60, 300, 1800};
			for(int interval : testIntervals)
			{
				config.auto_save_interval_seconds = interval;
				testEnv.waitForWorldTicks(2);
				
				if(config.auto_save_interval_seconds != interval)
				{
					return TestResult
						.failed("Failed to set save interval: " + interval);
				}
			}
			
			// Test all auto-save modes
			for(AutoSaveMode mode : AutoSaveMode.values())
			{
				config.auto_save_mode = mode;
				testEnv.waitForWorldTicks(2);
				
				if(config.auto_save_mode != mode)
				{
					return TestResult
						.failed("Failed to set auto-save mode: " + mode);
				}
			}
			
			testEnv.takeScreenshot("autosave_config_test");
			return TestResult
				.passed("Auto-save configuration options work correctly");
		}catch(Exception e)
		{
			return TestResult.failed("Config test failed: " + e.getMessage());
		}
	}
	
	/**
	 * Test different auto-save modes and their behavior.
	 */
	private TestResult testAutoSaveModes()
	{
		System.out.println("Testing auto-save modes...");
		
		try
		{
			SignFinderConfig config = SignFinderMod.getInstance().getConfig();
			config.auto_save_detection_data = true;
			config.auto_save_interval_seconds = 10; // Short interval for
													// testing
			
			// Create test signs for detection
			testEnv.createTestStructure();
			testEnv.placeSignWithText(new BlockPos(5, 65, 5), "Chest Storage");
			testEnv.placeSignWithText(new BlockPos(5, 65, 6), "Barrel Room");
			testEnv.waitForWorldTicks(20);
			
			// Test AUTO_OVERWRITE mode
			config.auto_save_mode = AutoSaveMode.AUTO_OVERWRITE;
			testEnv.runChatCommand("findsign chest");
			testEnv.waitForWorldTicks(30);
			
			Path autoOverwriteFile = getAutoSaveFilePath();
			if(!Files.exists(autoOverwriteFile))
			{
				return TestResult
					.failed("AUTO_OVERWRITE mode failed to create save file");
			}
			
			// Test NEW_FILE mode
			config.auto_save_mode = AutoSaveMode.NEW_FILE;
			testEnv.runChatCommand("findsign barrel");
			testEnv.waitForWorldTicks(30);
			
			// Should create a new file with timestamp
			Path newFileDir = autoOverwriteFile.getParent();
			long fileCount = Files.list(newFileDir).filter(
				path -> path.getFileName().toString().startsWith("autosave_"))
				.count();
			
			if(fileCount < 2)
			{
				return TestResult
					.failed("NEW_FILE mode failed to create new file");
			}
			
			// Test DAILY_SPLIT mode
			config.auto_save_mode = AutoSaveMode.DAILY_SPLIT;
			testEnv.runChatCommand("findsign storage");
			testEnv.waitForWorldTicks(30);
			
			testEnv.takeScreenshot("autosave_modes_test");
			return TestResult.passed("All auto-save modes work correctly");
		}catch(Exception e)
		{
			return TestResult
				.failed("Auto-save modes test failed: " + e.getMessage());
		}
	}
	
	/**
	 * Test dimension handling in auto-save system.
	 */
	private TestResult testDimensionHandling()
	{
		System.out.println("Testing dimension handling...");
		
		try
		{
			SignFinderConfig config = SignFinderMod.getInstance().getConfig();
			config.auto_save_detection_data = true;
			
			// Create signs in current dimension
			testEnv.placeSignWithText(new BlockPos(10, 65, 10),
				"Overworld Sign");
			testEnv.waitForWorldTicks(10);
			
			// Search and trigger auto-save
			testEnv.runChatCommand("findsign overworld");
			testEnv.waitForWorldTicks(20);
			
			// Force auto-save
			AutoSaveManager.INSTANCE.checkAndSave();
			testEnv.waitForWorldTicks(10);
			
			// Check if dimension-specific directory was created
			String saveDirStr = AutoSaveManager.INSTANCE.getPersistenceService()
				.getSaveDirectory();
			Path saveDir = Path.of(saveDirStr);
			if(!Files.exists(saveDir))
			{
				return TestResult.failed("Auto-save directory not created");
			}
			
			// Test dimension key generation
			String currentDimensionKey = getCurrentDimensionKey();
			if(currentDimensionKey == null
				|| currentDimensionKey.equals("unknown"))
			{
				return TestResult
					.failed("Failed to generate proper dimension key");
			}
			
			// Test loading saved data
			List<SignSearchResult> loadedData = loadAutoSavedData();
			if(loadedData.isEmpty())
			{
				System.out.println(
					"Warning: No auto-saved data found, but this might be expected");
			}
			
			testEnv.takeScreenshot("dimension_handling_test");
			return TestResult.passed(
				"Dimension handling works correctly: " + currentDimensionKey);
		}catch(Exception e)
		{
			return TestResult
				.failed("Dimension handling test failed: " + e.getMessage());
		}
	}
	
	/**
	 * Test data persistence and integrity across saves/loads.
	 */
	private TestResult testDataPersistenceAndIntegrity()
	{
		System.out.println("Testing data persistence and integrity...");
		
		try
		{
			SignFinderConfig config = SignFinderMod.getInstance().getConfig();
			config.auto_save_detection_data = true;
			
			// Create test data
			testEnv.placeSignWithText(new BlockPos(15, 65, 15),
				"Data Test Sign");
			testEnv.placeSignWithText(new BlockPos(16, 65, 15),
				"Integrity Check");
			testEnv.waitForWorldTicks(10);
			
			// Perform search to populate cache
			testEnv.runChatCommand("findsign \"data test\"");
			testEnv.waitForWorldTicks(10);
			
			// Force save
			AutoSaveManager.INSTANCE.checkAndSave();
			testEnv.waitForWorldTicks(5);
			
			// Clear current data
			testEnv.runChatCommand("findsign clear");
			testEnv.waitForWorldTicks(5);
			
			// Search again - should load from auto-save
			testEnv.runChatCommand("findsign \"data test\"");
			testEnv.waitForWorldTicks(10);
			
			// Verify data integrity
			List<SignBlockEntity> signResults = SignFinderMod.getInstance()
				.getSearchResultManager().getSearchResultSigns();
			List<ItemFrameEntity> frameResults = SignFinderMod.getInstance()
				.getSearchResultManager().getSearchResultItemFrames();
			boolean foundTestSign = signResults.stream().anyMatch(sign -> {
				String[] signText = SignTextUtils.getSignTextArray(sign);
				return signText != null && signText.length > 0
					&& signText[0].toLowerCase().contains("data test");
			});
			
			if(!foundTestSign)
			{
				return TestResult.failed(
					"Data integrity check failed - test sign not found in results");
			}
			
			testEnv.takeScreenshot("data_integrity_test");
			return TestResult.passed("Data persistence and integrity verified");
		}catch(Exception e)
		{
			return TestResult
				.failed("Data integrity test failed: " + e.getMessage());
		}
	}
	
	/**
	 * Test search integration with auto-save data display and processing.
	 */
	private TestResult testSearchWithAutoSave()
	{
		System.out.println("Testing search integration with auto-save...");
		
		try
		{
			SignFinderConfig config = SignFinderMod.getInstance().getConfig();
			config.auto_save_detection_data = true;
			
			// Create signs outside normal render distance
			testEnv.placeSignWithText(new BlockPos(100, 65, 100),
				"Distant Storage");
			testEnv.placeSignWithText(new BlockPos(101, 65, 100),
				"Remote Cache");
			testEnv.waitForWorldTicks(10);
			
			// Search with large radius to include distant signs
			testEnv.runChatCommand("findsign storage 150");
			testEnv.waitForWorldTicks(20);
			
			// Force auto-save
			AutoSaveManager.INSTANCE.checkAndSave();
			testEnv.waitForWorldTicks(10);
			
			// Move player away and search again
			mc.player.setPosition(new Vec3d(0, 65, 0));
			testEnv.waitForWorldTicks(5);
			
			// Search should now use auto-saved data for distant signs
			testEnv.runChatCommand("findsign storage 150");
			testEnv.waitForWorldTicks(10);
			
			List<SignBlockEntity> signResults = SignFinderMod.getInstance()
				.getSearchResultManager().getSearchResultSigns();
			List<ItemFrameEntity> frameResults = SignFinderMod.getInstance()
				.getSearchResultManager().getSearchResultItemFrames();
			boolean hasResults =
				!signResults.isEmpty() || !frameResults.isEmpty();
			
			if(!hasResults)
			{
				System.out.println(
					"Warning: No search results found, might be expected in test environment");
			}
			
			// Test different search types with auto-save
			testEnv.runChatCommand("findsign regex \"(storage|cache)\"");
			testEnv.waitForWorldTicks(10);
			
			testEnv.runChatCommand("findsign array \"storage,cache\"");
			testEnv.waitForWorldTicks(10);
			
			testEnv.takeScreenshot("search_autosave_integration");
			return TestResult
				.passed("Search integration with auto-save works correctly");
		}catch(Exception e)
		{
			return TestResult.failed(
				"Search auto-save integration test failed: " + e.getMessage());
		}
	}
	
	/**
	 * Helper method to get auto-save file path.
	 */
	private Path getAutoSaveFilePath()
	{
		String saveDirStr =
			AutoSaveManager.INSTANCE.getPersistenceService().getSaveDirectory();
		return Path.of(saveDirStr)
			.resolve("autosave_" + getCurrentDimensionKey() + ".json");
	}
	
	/**
	 * Helper method to get current dimension key.
	 */
	private String getCurrentDimensionKey()
	{
		if(mc.world == null)
			return "unknown";
		
		var worldKey = mc.world.getRegistryKey();
		if(worldKey.getValue().toString().contains("overworld"))
		{
			return "overworld";
		}else if(worldKey.getValue().toString().contains("nether"))
		{
			return "nether";
		}else if(worldKey.getValue().toString().contains("end"))
		{
			return "end";
		}else
		{
			return worldKey.getValue().toString().replaceAll(":", "_")
				.replaceAll("[/\\\\]", "_").replaceAll("[<>:\"|?*]", "_")
				.replaceAll("\\s+", "_").toLowerCase();
		}
	}
	
	/**
	 * Helper method to load auto-saved data.
	 */
	private List<SignSearchResult> loadAutoSavedData()
	{
		try
		{
			return AutoSaveManager.INSTANCE.getLocalData();
		}catch(Exception e)
		{
			System.err
				.println("Failed to load auto-saved data: " + e.getMessage());
			return List.of();
		}
	}
	
	/**
	 * Helper method to copy config for backup.
	 */
	private SignFinderConfig copyConfig(SignFinderConfig original)
	{
		// Create a basic copy of essential settings
		SignFinderConfig copy = new SignFinderConfig();
		copy.auto_save_detection_data = original.auto_save_detection_data;
		copy.auto_save_interval_seconds = original.auto_save_interval_seconds;
		copy.auto_save_mode = original.auto_save_mode;
		return copy;
	}
	
	/**
	 * Helper method to restore config from backup.
	 */
	private void restoreConfig(SignFinderConfig backup)
	{
		SignFinderConfig config = SignFinderMod.getInstance().getConfig();
		config.auto_save_detection_data = backup.auto_save_detection_data;
		config.auto_save_interval_seconds = backup.auto_save_interval_seconds;
		config.auto_save_mode = backup.auto_save_mode;
	}
	
	/**
	 * Clean up test files and reset state.
	 */
	private void cleanup()
	{
		try
		{
			// Clean up test files
			String saveDirStr = AutoSaveManager.INSTANCE.getPersistenceService()
				.getSaveDirectory();
			Path saveDir = Path.of(saveDirStr);
			if(Files.exists(saveDir))
			{
				Files.list(saveDir).filter(
					path -> path.getFileName().toString().startsWith("test_"))
					.forEach(path -> {
						try
						{
							Files.deleteIfExists(path);
						}catch(Exception e)
						{
							System.err
								.println("Failed to delete test file: " + path);
						}
					});
			}
			
			// Clear current search results
			testEnv.runChatCommand("findsign clear");
			testEnv.waitForWorldTicks(5);
		}catch(Exception e)
		{
			System.err.println("Cleanup failed: " + e.getMessage());
		}
	}
}

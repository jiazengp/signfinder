package net.signfinder.test;

import static net.signfinder.test.WiModsTestHelper.*;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

/**
 * Main test client for SignFinder end-to-end testing.
 * Coordinates test execution and provides comprehensive mod testing.
 */
public final class SignFinderTestClient implements ModInitializer
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger("SignFinderTest");
	
	// Test tracking
	private TestResult overallResult = TestResult.empty();
	private long testStartTime = 0;
	
	@Override
	public void onInitialize()
	{
		if(System.getProperty("signfinder.e2eTest") == null)
			return;
		
		Thread.ofVirtual().name("SignFinder End-to-End Test")
			.uncaughtExceptionHandler((t, e) -> {
				LOGGER.error("Uncaught exception in test thread", e);
				System.exit(1);
			}).start(this::runTest);
	}
	
	private void runTest()
	{
		testStartTime = System.currentTimeMillis();
		LOGGER.info("Starting SignFinder Comprehensive End-to-End Test");
		
		try
		{
			// Initialize test environment
			initializeTestEnvironment();
			
			// Run comprehensive test suites
			runAllTestSuites();
			
			// Report final results
			reportFinalResults();
		}catch(Exception e)
		{
			LOGGER.error("Test execution failed", e);
			System.exit(1);
		}
	}
	
	private void initializeTestEnvironment()
	{
		LOGGER.info("Initializing test environment");
		
		waitForResourceLoading();
		
		if(submitAndGet(mc -> mc.options.onboardAccessibility))
		{
			LOGGER.info("Handling onboarding screen");
			waitForScreen(AccessibilityOnboardingScreen.class);
			clickButton("gui.continue");
		}
		
		// Navigate to game world
		waitForScreen(TitleScreen.class);
		waitForTitleScreenFade();
		takeScreenshot("title_screen", Duration.ZERO);
		
		clickButton("menu.singleplayer");
		
		if(submitAndGet(mc -> !mc.getLevelStorage().getLevelList().isEmpty()))
		{
			waitForScreen(SelectWorldScreen.class);
			takeScreenshot("select_world_screen");
			clickButton("selectWorld.create");
		}
		
		// Create test world with optimized settings
		waitForScreen(CreateWorldScreen.class);
		setTextFieldText(0,
			"SignFinder Test " + SharedConstants.getGameVersion().name());
		
		// Set to Creative mode for testing
		clickButton("selectWorld.gameMode");
		clickButton("selectWorld.gameMode");
		
		// Configure world type to Superflat for better testing environment
		try
		{
			// Try to access More tab for world generation settings
			clickButton("createWorld.tab.more");
			waitFor(Duration.ofMillis(500));
			
			// Set world type to Superflat
			clickButton("selectWorld.mapType");
			waitFor(Duration.ofMillis(300));
			clickButton("generator.flat");
			waitFor(Duration.ofMillis(300));
			
			LOGGER.info("Successfully configured Superflat world type");
		}catch(Exception e)
		{
			LOGGER.warn(
				"Could not configure Superflat world type, using default: {}",
				e.getMessage());
		}
		
		takeScreenshot("create_world_screen");
		clickButton("selectWorld.create");
		
		// Wait for world to load
		waitForWorldLoad();
		dismissTutorialToasts();
		waitForWorldTicks(200);
		
		// Optimize game settings for testing
		optimizeGameSettingsForTesting();
		
		runChatCommand("seed");
		takeScreenshot("in_game", Duration.ZERO);
		
		// Build test environment
		TestEnvironmentBuilder.buildComprehensiveTestRig();
		TestEnvironmentBuilder.buildItemFrameTestEnvironment();
		
		LOGGER.info("Test environment initialization completed");
	}
	
	private void runAllTestSuites()
	{
		LOGGER.info("Running comprehensive test suites");
		
		// Run search functionality tests
		TestResult searchResults = SearchTestSuite.runAllSearchTests();
		overallResult = overallResult.add(searchResults);
		LOGGER.info("Search tests completed: {}", searchResults);
		
		// Run highlighting functionality tests
		TestResult highlightResults = HighlightTestSuite.runAllHighlightTests();
		overallResult = overallResult.add(highlightResults);
		LOGGER.info("Highlight tests completed: {}", highlightResults);
		
		// Run auto-detection tests
		TestResult detectionResults = runAutoDetectionTests();
		overallResult = overallResult.add(detectionResults);
		LOGGER.info("Detection tests completed: {}", detectionResults);
		
		// Run auto-save tests
		TestResult autoSaveResults = runAutoSaveTests();
		overallResult = overallResult.add(autoSaveResults);
		LOGGER.info("Auto-save tests completed: {}", autoSaveResults);
		
		// Run dimension tests
		TestResult dimensionResults = runDimensionTests();
		overallResult = overallResult.add(dimensionResults);
		LOGGER.info("Dimension tests completed: {}", dimensionResults);
		
		// Run manager validation tests
		TestResult managerResults = runManagerTests();
		overallResult = overallResult.add(managerResults);
		LOGGER.info("Manager tests completed: {}", managerResults);
		
		// Run data validation tests
		TestResult dataValidationResults = runDataValidationTests();
		overallResult = overallResult.add(dataValidationResults);
		LOGGER.info("Data validation tests completed: {}",
			dataValidationResults);
		
		// Final cleanup and validation
		runCleanupTests();
	}
	
	private TestResult runAutoDetectionTests()
	{
		LOGGER.info("Running auto-detection tests");
		
		try
		{
			// Enable auto-detection
			var config = net.signfinder.SignFinderMod.getInstance().getConfig();
			config.enable_auto_detection = true;
			config.auto_highlight_detected = true;
			
			waitForWorldTicks(50);
			takeScreenshot("auto_detection_enabled");
			
			// Test ignore words
			config.ignore_words = new String[]{"ignore"};
			waitForWorldTicks(30);
			takeScreenshot("auto_detection_with_ignore_words");
			
			// Disable auto-detection
			config.enable_auto_detection = false;
			waitForWorldTicks(30);
			takeScreenshot("auto_detection_disabled");
			
			return TestResult.passed("Auto-detection tests");
		}catch(Exception e)
		{
			LOGGER.error("Auto-detection tests failed", e);
			return TestResult
				.failed("Auto-detection exception: " + e.getMessage());
		}
	}
	
	private TestResult runAutoSaveTests()
	{
		LOGGER.info("Running comprehensive auto-save tests");
		
		try
		{
			TestEnvironmentBuilder testEnv = new TestEnvironmentBuilder();
			AutoSaveTestSuite autoSaveTestSuite =
				new AutoSaveTestSuite(testEnv);
			
			TestResult autoSaveResult = autoSaveTestSuite.runAutoSaveTests();
			
			if(autoSaveResult.isPassed())
			{
				LOGGER.info("Auto-save tests passed: {}",
					autoSaveResult.getMessage());
			}else
			{
				LOGGER.error("Auto-save tests failed: {}",
					autoSaveResult.getMessage());
			}
			
			return autoSaveResult;
		}catch(Exception e)
		{
			LOGGER.error("Auto-save test suite failed", e);
			return TestResult
				.failed("Auto-save test suite exception: " + e.getMessage());
		}
	}
	
	private TestResult runDimensionTests()
	{
		LOGGER.info("Running comprehensive dimension tests");
		
		try
		{
			TestResult dimensionResult =
				DimensionTestSuite.runAllDimensionTests();
			
			if(dimensionResult.isPassed())
			{
				LOGGER.info("Dimension tests passed: {}",
					dimensionResult.getMessage());
			}else
			{
				LOGGER.error("Dimension tests failed: {}",
					dimensionResult.getMessage());
			}
			
			return dimensionResult;
		}catch(Exception e)
		{
			LOGGER.error("Dimension test suite failed", e);
			return TestResult
				.failed("Dimension test suite exception: " + e.getMessage());
		}
	}
	
	private TestResult runManagerTests()
	{
		LOGGER.info("Running manager validation tests");
		
		try
		{
			var mod = net.signfinder.SignFinderMod.getInstance();
			
			// Test manager functionality
			takeScreenshot("manager_auto_detection");
			
			// Clear all results
			runChatCommand("findsign clear");
			waitForWorldTicks(20);
			takeScreenshot("search_cleared");
			
			// Test search results manager
			runChatCommand("findsign chest");
			waitForWorldTicks(20);
			takeScreenshot("manager_search_results");
			
			// Test cleanup
			mod.cleanup();
			waitForWorldTicks(20);
			takeScreenshot("manager_cleanup");
			
			return TestResult.passed("Manager tests");
		}catch(Exception e)
		{
			LOGGER.error("Manager tests failed", e);
			return TestResult.failed("Manager exception: " + e.getMessage());
		}
	}
	
	private TestResult runDataValidationTests()
	{
		LOGGER.info("Running comprehensive data validation tests");
		
		try
		{
			TestEnvironmentBuilder testEnv = new TestEnvironmentBuilder();
			DataValidationTestSuite dataValidationSuite =
				new DataValidationTestSuite(testEnv);
			
			TestResult validationResult =
				dataValidationSuite.runDataValidationTests();
			
			if(validationResult.isPassed())
			{
				LOGGER.info("Data validation tests passed: {}",
					validationResult.getMessage());
			}else
			{
				LOGGER.error("Data validation tests failed: {}",
					validationResult.getMessage());
			}
			
			takeScreenshot("data_validation_complete");
			return validationResult;
		}catch(Exception e)
		{
			LOGGER.error("Data validation tests failed with exception", e);
			return TestResult
				.failed("Data validation exception: " + e.getMessage());
		}
	}
	
	private void optimizeGameSettingsForTesting()
	{
		LOGGER.info("Optimizing game settings for testing environment");
		
		try
		{
			// Set time to day and disable weather
			runChatCommand("time set day");
			runChatCommand("weather clear");
			runChatCommand("gamerule doDaylightCycle false");
			runChatCommand("gamerule doWeatherCycle false");
			runChatCommand("gamerule doMobSpawning false");
			runChatCommand("gamerule doFireTick false");
			runChatCommand("gamerule randomTickSpeed 0");
			
			// Position player at a good testing location
			runChatCommand("tp 0 70 0");
			
			waitForWorldTicks(20);
			LOGGER.info("Game settings optimized for testing");
		}catch(Exception e)
		{
			LOGGER.warn("Failed to optimize some game settings: {}",
				e.getMessage());
		}
	}
	
	private void runCleanupTests()
	{
		LOGGER.info("Running final cleanup and validation");
		
		try
		{
			// Test export functionality
			runChatCommand("findsign chest");
			waitForWorldTicks(20);
			runChatCommand("findsign export json");
			waitForWorldTicks(30);
			
			// Final validation screenshot
			takeScreenshot("final_validation");
			
			// Enable SignFinder for final screenshots
			var config = net.signfinder.SignFinderMod.getInstance().getConfig();
			config.enable_sign_highlighting = true;
			config.highlight_style = net.signfinder.core.SignEspStyle.BOXES;
			
			runChatCommand("findsign chest");
			waitForWorldTicks(20);
			takeScreenshot("SignFinder_enabled");
			
			config.highlight_style = net.signfinder.core.SignEspStyle.LINES;
			waitForWorldTicks(20);
			takeScreenshot("SignFinder_lines");
			
			config.highlight_style =
				net.signfinder.core.SignEspStyle.LINES_AND_BOXES;
			waitForWorldTicks(20);
			takeScreenshot("SignFinder_lines_and_boxes");
			
			// Open game menu for final screenshot
			openGameMenu();
			waitForWorldTicks(10);
			takeScreenshot("game_menu");
			closeScreen();
			
		}catch(Exception e)
		{
			LOGGER.warn("Cleanup tests encountered issues", e);
		}
	}
	
	private void reportFinalResults()
	{
		long testDuration = System.currentTimeMillis() - testStartTime;
		
		LOGGER.info("=".repeat(60));
		LOGGER.info("SIGNFINDER END-TO-END TEST RESULTS");
		LOGGER.info("=".repeat(60));
		LOGGER.info("Total Duration: {}ms ({:.1f} seconds)", testDuration,
			testDuration / 1000.0);
		LOGGER.info("Overall Results: {}", overallResult);
		LOGGER.info("Success Rate: {:.1f}%", overallResult.getSuccessRate());
		
		if(overallResult.getTestsFailed() > 0)
		{
			LOGGER.error("Tests failed! {} out of {} tests failed",
				overallResult.getTestsFailed(), overallResult.getTestsRun());
			System.exit(1);
		}else
		{
			LOGGER.info("All tests passed! SignFinder is working correctly.");
			System.exit(0);
		}
	}
}

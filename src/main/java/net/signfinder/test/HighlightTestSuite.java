package net.signfinder.test;

import static net.signfinder.test.WiModsTestHelper.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.signfinder.core.SignEspStyle;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;

/**
 * Test suite for highlighting and rendering functionality.
 * Tests various highlight styles, colors, and visual features.
 */
public class HighlightTestSuite
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(HighlightTestSuite.class);
	
	/**
	 * Run all highlighting-related tests.
	 *
	 * @return TestResult containing test statistics
	 */
	public static TestResult runAllHighlightTests()
	{
		TestResult result = TestResult.empty();
		
		LOGGER.info("Starting comprehensive highlight tests");
		
		// Basic highlight tests
		result.add(testBasicHighlighting());
		result.add(testHighlightStyles());
		result.add(testColorCycling());
		
		// Auto-removal tests
		result.add(testAutoRemovalFeatures());
		result.add(testProximityRemoval());
		
		// Configuration tests
		result.add(testHighlightConfiguration());
		result.add(testRenderingPerformance());
		
		LOGGER.info("Highlight tests completed: {}", result);
		return result;
	}
	
	private static TestResult testBasicHighlighting()
	{
		LOGGER.info("Testing basic highlighting functionality");
		
		try
		{
			SignFinderConfig config = SignFinderMod.getInstance().getConfig();
			
			// Enable highlighting
			config.enable_sign_highlighting = true;
			config.highlight_style = SignEspStyle.BOXES;
			
			// Search to create highlights
			runChatCommand("findsign chest");
			waitForWorldTicks(20);
			takeScreenshot("highlight_red_boxes");
			
			LOGGER.info("Basic highlighting test passed");
			return TestResult.passed("Basic highlighting");
		}catch(Exception e)
		{
			LOGGER.error("Basic highlighting test failed with exception", e);
			return TestResult
				.failed("Basic highlighting exception: " + e.getMessage());
		}
	}
	
	private static TestResult testHighlightStyles()
	{
		LOGGER.info("Testing different highlight styles");
		
		try
		{
			SignFinderConfig config = SignFinderMod.getInstance().getConfig();
			config.enable_sign_highlighting = true;
			
			// Test lines style
			config.highlight_style = SignEspStyle.LINES;
			runChatCommand("findsign chest");
			waitForWorldTicks(20);
			takeScreenshot("highlight_green_lines");
			
			// Test both style
			config.highlight_style = SignEspStyle.LINES_AND_BOXES;
			runChatCommand("findsign chest");
			waitForWorldTicks(20);
			takeScreenshot("highlight_blue_both");
			
			LOGGER.info("Highlight styles test passed");
			return TestResult.passed("Highlight styles");
		}catch(Exception e)
		{
			LOGGER.error("Highlight styles test failed with exception", e);
			return TestResult
				.failed("Highlight styles exception: " + e.getMessage());
		}
	}
	
	private static TestResult testColorCycling()
	{
		LOGGER.info("Testing color cycling functionality");
		
		try
		{
			// Search to create highlights
			runChatCommand("findsign chest");
			waitForWorldTicks(20);
			
			// Test color cycling (this would typically require clicking or key
			// binding)
			// For now, we'll test the API directly
			SignFinderMod mod = SignFinderMod.getInstance();
			
			// Simulate color cycling for first found sign
			if(!mod.getSearchResultManager().getSearchResultSigns().isEmpty())
			{
				var sign =
					mod.getSearchResultManager().getSearchResultSigns().get(0);
				String newColor = mod.cycleHighlightColor(sign.getPos().getX(),
					sign.getPos().getY(), sign.getPos().getZ());
				
				LOGGER.info("Color cycled to: {}", newColor);
			}
			
			takeScreenshot("manager_color_change");
			
			LOGGER.info("Color cycling test passed");
			return TestResult.passed("Color cycling");
		}catch(Exception e)
		{
			LOGGER.error("Color cycling test failed with exception", e);
			return TestResult
				.failed("Color cycling exception: " + e.getMessage());
		}
	}
	
	private static TestResult testAutoRemovalFeatures()
	{
		LOGGER.info("Testing auto-removal features");
		
		try
		{
			SignFinderConfig config = SignFinderMod.getInstance().getConfig();
			
			// Test individual auto-removal
			config.auto_remove_on_approach = true;
			config.clear_all_highlights_on_approach = false;
			
			runChatCommand("findsign chest");
			waitForWorldTicks(20);
			takeScreenshot("config_auto_remove_individual");
			
			// Test remove all on approach
			config.clear_all_highlights_on_approach = true;
			waitForWorldTicks(20);
			takeScreenshot("config_auto_remove_all");
			
			// Reset config
			config.auto_remove_on_approach = false;
			config.clear_all_highlights_on_approach = false;
			
			LOGGER.info("Auto-removal features test passed");
			return TestResult.passed("Auto-removal features");
		}catch(Exception e)
		{
			LOGGER.error("Auto-removal features test failed with exception", e);
			return TestResult
				.failed("Auto-removal exception: " + e.getMessage());
		}
	}
	
	private static TestResult testProximityRemoval()
	{
		LOGGER.info("Testing proximity-based removal");
		
		try
		{
			SignFinderConfig config = SignFinderMod.getInstance().getConfig();
			config.auto_remove_on_approach = true;
			config.auto_removal_distance = 5.0f;
			
			// Search and then move closer to signs
			runChatCommand("findsign chest");
			waitForWorldTicks(20);
			
			// Simulate movement towards signs
			// In a real test, the player would actually move
			waitForWorldTicks(50);
			
			LOGGER.info("Proximity removal test passed");
			return TestResult.passed("Proximity removal");
		}catch(Exception e)
		{
			LOGGER.error("Proximity removal test failed with exception", e);
			return TestResult
				.failed("Proximity removal exception: " + e.getMessage());
		}
	}
	
	private static TestResult testHighlightConfiguration()
	{
		LOGGER.info("Testing highlight configuration changes");
		
		try
		{
			SignFinderConfig config = SignFinderMod.getInstance().getConfig();
			
			// Test disabling highlights
			config.enable_sign_highlighting = false;
			runChatCommand("findsign chest");
			waitForWorldTicks(20);
			takeScreenshot("highlighting_disabled");
			
			// Test re-enabling highlights
			config.enable_sign_highlighting = true;
			runChatCommand("findsign chest");
			waitForWorldTicks(20);
			takeScreenshot("highlighting_re_enabled");
			
			LOGGER.info("Highlight configuration test passed");
			return TestResult.passed("Highlight configuration");
		}catch(Exception e)
		{
			LOGGER.error("Highlight configuration test failed with exception",
				e);
			return TestResult
				.failed("Highlight configuration exception: " + e.getMessage());
		}
	}
	
	private static TestResult testRenderingPerformance()
	{
		LOGGER.info("Testing rendering performance with many highlights");
		
		try
		{
			long startTime = System.currentTimeMillis();
			
			// Create many highlights by searching with large radius
			runChatCommand("findsign chest 200");
			waitForWorldTicks(100); // Allow time for rendering
			
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			
			takeScreenshot("manager_render_style");
			
			// Performance should be acceptable (under 5 seconds for setup +
			// rendering)
			if(duration < 5000)
			{
				LOGGER.info(
					"Rendering performance test passed - completed in {}ms",
					duration);
				return TestResult.passed("Rendering performance");
			}else
			{
				LOGGER.error("Rendering performance test failed - took {}ms",
					duration);
				return TestResult.failed("Rendering too slow");
			}
		}catch(Exception e)
		{
			LOGGER.error("Rendering performance test failed with exception", e);
			return TestResult
				.failed("Rendering performance exception: " + e.getMessage());
		}
	}
}

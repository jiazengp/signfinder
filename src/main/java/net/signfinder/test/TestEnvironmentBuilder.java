package net.signfinder.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.util.math.BlockPos;

/**
 * Responsible for building and setting up test environments.
 * Handles world creation, test structure building, and environment setup.
 */
public class TestEnvironmentBuilder
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(TestEnvironmentBuilder.class);
	
	/**
	 * Build a comprehensive test environment with various sign types and
	 * configurations.
	 * Optimized for superflat worlds.
	 */
	public static void buildComprehensiveTestRig()
	{
		LOGGER.info(
			"Building comprehensive test environment for superflat world");
		
		// Create a stable platform for testing
		buildTestPlatform();
		
		// Basic sign setup
		buildBasicSignSetup();
		
		// Complex container setup
		buildComplexContainerSetup();
		
		// Edge case testing setup
		buildEdgeCaseSetup();
		
		// Performance testing setup (reduced for superflat)
		buildOptimizedPerformanceTestSetup();
		
		LOGGER.info("Test environment construction completed");
		takeScreenshot("test_environment_built");
	}
	
	/**
	 * Build a stable platform for testing in superflat world.
	 */
	private static void buildTestPlatform()
	{
		LOGGER.debug("Building test platform for superflat world");
		
		// Create a large platform at Y=70 for consistent testing
		runChatCommand("fill -20 69 -20 20 69 20 stone");
		runChatCommand("fill -15 70 -15 15 70 15 air"); // Clear space above
		
		waitForWorldTicks(10);
		LOGGER.debug("Test platform built successfully");
	}
	
	private static void buildBasicSignSetup()
	{
		LOGGER.debug("Building basic sign setup");
		
		// Create basic signs for testing at fixed Y=70 level
		runChatCommand(
			"setblock 1 70 1 oak_sign{front_text:{messages:['[\"\",{\"text\":\"chest\"}]','[\"\"]','[\"\"]','[\"\"]']}}");
		runChatCommand(
			"setblock 2 70 1 oak_sign{front_text:{messages:['[\"\",{\"text\":\"barrel\"}]','[\"\"]','[\"\"]','[\"\"]']}}");
		runChatCommand(
			"setblock 3 70 1 oak_sign{front_text:{messages:['[\"\",{\"text\":\"shulker\"}]','[\"\"]','[\"\"]','[\"\"]']}}");
		
		// Wait for blocks to be placed
		waitForWorldTicks(20);
	}
	
	private static void buildComplexContainerSetup()
	{
		LOGGER.debug("Building complex container setup");
		
		// Multi-line signs at fixed coordinates
		runChatCommand(
			"setblock 4 70 1 oak_sign{front_text:{messages:['[\"\",{\"text\":\"Storage\"}]','[\"\",{\"text\":\"chest here\"}]','[\"\"]','[\"\"]']}}");
		runChatCommand(
			"setblock 5 70 1 oak_sign{front_text:{messages:['[\"\",{\"text\":\"Mixed\"}]','[\"\",{\"text\":\"barrel below\"}]','[\"\"]','[\"\"]']}}");
		
		// Case sensitivity testing
		runChatCommand(
			"setblock 6 70 1 oak_sign{front_text:{messages:['[\"\",{\"text\":\"CHEST\"}]','[\"\"]','[\"\"]','[\"\"]']}}");
		runChatCommand(
			"setblock 7 70 1 oak_sign{front_text:{messages:['[\"\",{\"text\":\"ChEsT\"}]','[\"\"]','[\"\"]','[\"\"]']}}");
		
		waitForWorldTicks(20);
	}
	
	private static void buildEdgeCaseSetup()
	{
		LOGGER.debug("Building edge case testing setup");
		
		// Empty and special character signs
		runChatCommand(
			"setblock 8 70 1 oak_sign{front_text:{messages:['[\"\"]','[\"\"]','[\"\"]','[\"\"]']}}");
		runChatCommand(
			"setblock 9 70 1 oak_sign{front_text:{messages:['[\"\",{\"text\":\"!@#$%\"}]','[\"\"]','[\"\"]','[\"\"]']}}");
		
		// Unicode and special characters
		runChatCommand(
			"setblock 10 70 1 oak_sign{front_text:{messages:['[\"\",{\"text\":\"箱子\"}]','[\"\"]','[\"\"]','[\"\"]']}}");
		
		waitForWorldTicks(20);
	}
	
	private static void buildPerformanceTestSetup()
	{
		LOGGER.debug("Building performance test setup");
		
		// Create many signs for performance testing
		for(int i = 0; i < 10; i++)
		{
			for(int j = 0; j < 10; j++)
			{
				int x = i - 50;
				int z = j - 50;
				runChatCommand(String.format(
					"setblock ~%d ~1 ~%d oak_sign{front_text:{messages:['[\"\",{\"text\":\"chest%d\"}]','[\"\"]','[\"\"]','[\"\"]']}}",
					x, z, i * 10 + j));
			}
		}
		
		waitForWorldTicks(50);
	}
	
	/**
	 * Optimized performance test setup for superflat world.
	 * Uses fewer signs and better positioning.
	 */
	private static void buildOptimizedPerformanceTestSetup()
	{
		LOGGER.debug("Building optimized performance test setup");
		
		// Create a smaller grid of signs for performance testing in superflat
		for(int i = 0; i < 5; i++)
		{
			for(int j = 0; j < 5; j++)
			{
				int x = (i - 2) * 3; // 3 block spacing
				int z = (j - 2) * 3 + 30; // Offset to avoid other signs
				runChatCommand(String.format(
					"setblock %d 70 %d oak_sign{front_text:{messages:['[\"\",{\"text\":\"perf%d\"}]','[\"\"]','[\"\"]','[\"\"]']}}",
					x, z, i * 5 + j));
			}
		}
		
		waitForWorldTicks(30);
		LOGGER.debug("Optimized performance test setup completed");
	}
	
	/**
	 * Build item frame test environment.
	 */
	public static void buildItemFrameTestEnvironment()
	{
		LOGGER.info("Building item frame test environment");
		
		// Create item frames with various items at fixed coordinates
		runChatCommand("setblock 11 70 1 oak_planks");
		runChatCommand(
			"summon item_frame 11 70 1 {Facing:4b,Item:{id:\"minecraft:chest\",count:1}}");
		
		runChatCommand("setblock 12 70 1 oak_planks");
		runChatCommand(
			"summon item_frame 12 70 1 {Facing:4b,Item:{id:\"minecraft:barrel\",count:1}}");
		
		runChatCommand("setblock 13 70 1 oak_planks");
		runChatCommand(
			"summon item_frame 13 70 1 {Facing:4b,Item:{id:\"minecraft:shulker_box\",count:1}}");
		
		waitForWorldTicks(30);
		LOGGER.info("Item frame test environment completed");
	}
	
	/**
	 * Create a test structure for auto-save testing.
	 * Optimized for superflat world at Y=70.
	 */
	public void createTestStructure()
	{
		LOGGER.debug("Creating test structure for auto-save testing");
		
		// Build a small platform for testing at fixed coordinates
		runChatCommand("fill -5 69 -5 5 69 5 stone");
		runChatCommand("fill -3 70 -3 3 70 3 air"); // Clear space above
		waitForWorldTicks(10);
	}
	
	/**
	 * Place a sign with specific text at the given position.
	 */
	public void placeSignWithText(BlockPos pos, String text)
	{
		LOGGER.debug("Placing sign at {} with text: {}", pos, text);
		
		// Escape quotes in the text for command
		String escapedText = text.replace("\"", "\\\"");
		
		// Place the sign with the specified text
		String command = String.format(
			"setblock %d %d %d oak_sign{front_text:{messages:['[\"\",{\"text\":\"%s\"}]','[\"\"]','[\"\"]','[\"\"]']}}",
			pos.getX(), pos.getY(), pos.getZ(), escapedText);
		
		runChatCommand(command);
		waitForWorldTicks(5);
	}
	
	/**
	 * Wait for the specified number of world ticks.
	 */
	public static void waitForWorldTicks(int ticks)
	{
		WiModsTestHelper.waitForWorldTicks(ticks);
	}
	
	/**
	 * Run a chat command.
	 */
	public static void runChatCommand(String command)
	{
		WiModsTestHelper.runChatCommand(command);
	}
	
	/**
	 * Take a screenshot with the given name.
	 */
	public static void takeScreenshot(String name)
	{
		WiModsTestHelper.takeScreenshot(name);
	}
}

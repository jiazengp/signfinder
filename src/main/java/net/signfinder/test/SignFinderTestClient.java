/*
 * Copyright (c) 2023-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.signfinder.test;

import static net.signfinder.test.WiModsTestHelper.*;

import java.time.Duration;
import java.util.function.Consumer;

import net.fabricmc.api.ModInitializer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.SignEspStyle;

public final class SignFinderTestClient implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		if(System.getProperty("signfinder.e2eTest") == null)
			return;
		
		Thread.ofVirtual().name("SignFinder End-to-End Test")
			.uncaughtExceptionHandler((t, e) -> {
				e.printStackTrace();
				System.exit(1);
			}).start(this::runTest);
	}
	
	private void runTest()
	{
		System.out.println("Starting SignFinder End-to-End Test");
		waitForResourceLoading();
		
		if(submitAndGet(mc -> mc.options.onboardAccessibility))
		{
			System.out.println("Onboarding is enabled. Waiting for it");
			waitForScreen(AccessibilityOnboardingScreen.class);
			System.out.println("Reached onboarding screen");
			clickButton("gui.continue");
		}
		
		waitForScreen(TitleScreen.class);
		waitForTitleScreenFade();
		System.out.println("Reached title screen");
		takeScreenshot("title_screen", Duration.ZERO);
		
		System.out.println("Clicking singleplayer button");
		clickButton("menu.singleplayer");
		
		if(submitAndGet(mc -> !mc.getLevelStorage().getLevelList().isEmpty()))
		{
			System.out.println("World list is not empty. Waiting for it");
			waitForScreen(SelectWorldScreen.class);
			System.out.println("Reached select world screen");
			takeScreenshot("select_world_screen");
			clickButton("selectWorld.create");
		}
		
		waitForScreen(CreateWorldScreen.class);
		System.out.println("Reached create world screen");
		
		setTextFieldText(0,
			"SignFinder Test " + SharedConstants.getGameVersion().name());
		clickButton("selectWorld.gameMode");
		clickButton("selectWorld.gameMode");
		takeScreenshot("create_world_screen");
		
		System.out.println("Creating test world");
		clickButton("selectWorld.create");
		
		waitForWorldLoad();
		dismissTutorialToasts();
		waitForWorldTicks(200);
		runChatCommand("seed");
		System.out.println("Reached singleplayer world");
		takeScreenshot("in_game", Duration.ZERO);
		
		System.out.println("Building test signs and containers");
		buildTestRig();
		waitForWorldTicks(20);
		clearChat();
		takeScreenshot("SignFinder_default");
		
		System.out.println("Testing search commands");
		testSearchCommands();
		
		System.out.println("Enabling SignFinder highlighting");
		updateConfig(config -> {
			config.enable_sign_highlighting = true;
		});
		takeScreenshot("SignFinder_enabled");
		
		System.out.println("Testing different highlight styles");
		updateConfig(config -> {
			config.highlight_style = SignEspStyle.LINES;
		});
		takeScreenshot("SignFinder_lines");
		
		updateConfig(config -> {
			config.highlight_style = SignEspStyle.LINES_AND_BOXES;
		});
		takeScreenshot("SignFinder_lines_and_boxes");
		
		System.out.println("Opening game menu");
		openGameMenu();
		takeScreenshot("game_menu");
		
		System.out.println("Returning to title screen");
		clickButton("menu.returnToMenu");
		waitForScreen(TitleScreen.class);
		
		System.out.println("Stopping the game");
		clickButton("menu.quit");
	}
	
	private void buildTestRig()
	{
		runChatCommand("fill ^-8 ^ ^ ^8 ^10 ^10 air");
		runChatCommand("fill ^-8 ^-1 ^ ^8 ^-1 ^10 stone");
		runChatCommand("fill ^-8 ^ ^10 ^8 ^10 ^10 stone");
		
		// 创建告示牌包含容器相关内容
		runChatCommand(
			"setblock ^-5 ^ ^5 oak_sign[rotation=8]{front_text:{messages:['[\"\"]','[\"Chest\"]','[\"Storage\"]','[\"\"]']}}");
		runChatCommand(
			"setblock ^-3 ^ ^5 oak_sign[rotation=8]{front_text:{messages:['[\"\"]','[\"Shop\"]','[\"Buy/Sell\"]','[\"\"]']}}");
		runChatCommand(
			"setblock ^-1 ^ ^5 oak_sign[rotation=8]{front_text:{messages:['[\"\"]','[\"Barrel\"]','[\"Items\"]','[\"\"]']}}");
		runChatCommand(
			"setblock ^1 ^ ^5 oak_sign[rotation=8]{front_text:{messages:['[\"\"]','[\"Hopper\"]','[\"System\"]','[\"\"]']}}");
		runChatCommand(
			"setblock ^3 ^ ^5 oak_sign[rotation=8]{front_text:{messages:['[\"\"]','[\"Furnace\"]','[\"Smelting\"]','[\"\"]']}}");
		
		// 创建对应的容器
		runChatCommand("setblock ^-5 ^2 ^5 chest");
		runChatCommand("setblock ^-3 ^2 ^5 barrel");
		runChatCommand("setblock ^-1 ^2 ^5 hopper");
		runChatCommand("setblock ^1 ^2 ^5 furnace");
		runChatCommand("setblock ^3 ^2 ^5 dispenser");
		
		// 创建一些普通告示牌（不应被高亮）
		runChatCommand(
			"setblock ^5 ^ ^5 oak_sign[rotation=8]{front_text:{messages:['[\"\"]','[\"Welcome\"]','[\"to Server\"]','[\"\"]']}}");
		runChatCommand(
			"setblock ^7 ^ ^5 oak_sign[rotation=8]{front_text:{messages:['[\"\"]','[\"Rules:\"]','[\"Be Nice\"]','[\"\"]']}}");
	}
	
	private void testSearchCommands()
	{
		System.out.println("Testing text search");
		runChatCommand("findsign chest");
		waitForWorldTicks(5);
		
		System.out.println("Testing regex search");
		runChatCommand("findsign regex [Cc]hest|[Bb]arrel");
		waitForWorldTicks(5);
		
		System.out.println("Testing array search");
		runChatCommand("findsign array shop,chest,barrel");
		waitForWorldTicks(5);
		
		System.out.println("Testing preset search");
		runChatCommand("findsign preset shop");
		waitForWorldTicks(5);
	}
	
	public static void updateConfig(Consumer<SignFinderConfig> configUpdater)
	{
		submitAndWait(mc -> {
			configUpdater.accept(
				SignFinderMod.getInstance().getConfigHolder().getConfig());
		});
	}
}

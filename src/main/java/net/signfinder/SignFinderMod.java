package net.signfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.signfinder.util.ChunkUtils;
import net.signfinder.util.RenderUtils;
import net.signfinder.util.SignTextUtils;
import net.signfinder.commands.CommandUtils;

public final class SignFinderMod
{
	private static final MinecraftClient MC = MinecraftClient.getInstance();
	public static final String MOD_ID = "signfinder";
	public static final Logger LOGGER =
		LoggerFactory.getLogger(MOD_ID.toUpperCase());
	public final ConfigHolder<SignFinderConfig> configHolder;
	
	private final KeyBinding toggleAutoDetectionKey;
	private final KeyBinding toggleHighlightingKey;
	
	private final boolean enabled;
	private final List<SignBlockEntity> highlightedSigns = new ArrayList<>();
	private final List<SignBlockEntity> searchResultSigns = new ArrayList<>();
	
	private int cacheCleanupCounter = 0;
	private static final int CACHE_CLEANUP_INTERVAL = 6000;
	
	private final Map<BlockPos, Integer> customSignColors = new HashMap<>();
	
	private static final int[] COLOR_CYCLE = {0x00FF00, 0xFF0000, 0x0000FF,
		0xFFFF00, 0xFF00FF, 0x00FFFF, 0xFF8000, 0xFFFFFF};
	
	private static final String[] COLOR_NAMES =
		{"green", "red", "blue", "yellow", "purple", "cyan", "orange", "white"};
	
	public SignFinderMod()
	{
		LOGGER.info("Starting SignFinder...");
		
		configHolder = AutoConfig.register(SignFinderConfig.class,
			GsonConfigSerializer::new);
		
		enabled = true;
		
		toggleAutoDetectionKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.signfinder.toggle_auto_detection",
				InputUtil.UNKNOWN_KEY.getCode(), "key.category.signfinder"));
		
		toggleHighlightingKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.signfinder.toggle_highlighting",
				InputUtil.UNKNOWN_KEY.getCode(), "key.category.signfinder"));
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while(toggleAutoDetectionKey.wasPressed())
				toggleAutoDetection();
			
			while(toggleHighlightingKey.wasPressed())
				toggleHighlighting();
		});
		
		ClientCommandRegistrationCallback.EVENT.register((dispatcher,
			registryAccess) -> SignSearchCommand.register(dispatcher));
	}
	
	private void toggleAutoDetection()
	{
		if(MC.player == null)
			return;
		
		SignFinderConfig config = configHolder.getConfig();
		config.enable_auto_detection = !config.enable_auto_detection;
		configHolder.save();
		
		if(!config.enable_auto_detection)
		{
			highlightedSigns.clear();
		}
		
		String messageKey = config.enable_auto_detection
			? "signfinder.message.auto_detection_enabled"
			: "signfinder.message.auto_detection_disabled";
		MC.player.sendMessage(Text.translatable(messageKey), true);
	}
	
	private void toggleHighlighting()
	{
		if(MC.player == null)
			return;
		
		SignFinderConfig config = configHolder.getConfig();
		config.enable_sign_highlighting = !config.enable_sign_highlighting;
		configHolder.save();
		
		String messageKey = config.enable_sign_highlighting
			? "signfinder.message.highlighting_enabled"
			: "signfinder.message.highlighting_disabled";
		MC.player.sendMessage(Text.translatable(messageKey), true);
	}
	
	public void onUpdate()
	{
		if(!isEnabled() || MC.player == null || MC.world == null)
			return;
		
		SignFinderConfig config = configHolder.getConfig();
		
		cacheCleanupCounter++;
		if(cacheCleanupCounter >= CACHE_CLEANUP_INTERVAL)
		{
			cacheCleanupCounter = 0;
			performCacheCleanup();
		}
		if(config.auto_remove_nearby && !searchResultSigns.isEmpty())
		{
			removeNearbySearchResults(config);
		}
		
		if(!config.enable_auto_detection)
		{
			highlightedSigns.clear();
			return;
		}
		
		highlightedSigns.clear();
		
		ChunkUtils.getLoadedBlockEntities().forEach(blockEntity -> {
			if(blockEntity instanceof SignBlockEntity signEntity)
			{
				if(containsContainerReference(signEntity)
					&& !containsIgnoreWords(signEntity, config))
				{
					if(config.enable_sign_highlighting)
					{
						highlightedSigns.add(signEntity);
					}
				}
			}
		});
	}
	
	private boolean containsContainerReference(SignBlockEntity sign)
	{
		SignFinderConfig config = configHolder.getConfig();
		String signText =
			SignTextUtils.getSignText(sign, config.case_sensitive_search);
		return SignTextUtils.containsAnyKeyword(signText,
			config.container_keywords, config.case_sensitive_search);
	}
	
	private boolean containsIgnoreWords(SignBlockEntity sign,
		SignFinderConfig config)
	{
		String signText =
			SignTextUtils.getSignText(sign, config.case_sensitive_search);
		return SignTextUtils.containsAnyKeyword(signText, config.ignore_words,
			config.case_sensitive_search);
	}
	
	private void removeNearbySearchResults(SignFinderConfig config)
	{
		if(MC.player == null)
			return;
		
		Vec3d playerPos = MC.player.getPos();
		double removeDistanceSq =
			config.auto_remove_distance * config.auto_remove_distance;
		boolean shouldPlaySound = false;
		
		if(config.auto_clear_other_highlights)
		{
			boolean playerNearAnySign =
				searchResultSigns.stream().anyMatch(sign -> {
					Vec3d signPos = Vec3d.ofCenter(sign.getPos());
					double distanceSq = playerPos.squaredDistanceTo(signPos);
					return distanceSq <= removeDistanceSq;
				});
			
			if(playerNearAnySign && !searchResultSigns.isEmpty())
			{
				searchResultSigns.clear();
				shouldPlaySound = true;
			}
		}else
		{
			int beforeSize = searchResultSigns.size();
			searchResultSigns.removeIf(sign -> {
				Vec3d signPos = Vec3d.ofCenter(sign.getPos());
				double distanceSq = playerPos.squaredDistanceTo(signPos);
				return distanceSq <= removeDistanceSq;
			});
			int afterSize = searchResultSigns.size();
			shouldPlaySound = beforeSize > afterSize;
		}
		
		if(shouldPlaySound && MC.player != null)
		{
			MC.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),
				0.5f, 2.0f);
		}
	}
	
	public boolean shouldCancelViewBobbing()
	{
		return isEnabled()
			&& configHolder.getConfig().highlight_style.hasLines();
	}
	
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(!isEnabled())
			return;
		
		SignFinderConfig config = configHolder.getConfig();
		
		if(config.enable_sign_highlighting
			&& (!searchResultSigns.isEmpty() || !highlightedSigns.isEmpty()))
		{
			LOGGER.debug(
				"Rendering {} search result signs and {} auto-detected signs",
				searchResultSigns.size(), highlightedSigns.size());
			renderSignHighlights(matrixStack, partialTicks, config);
		}
	}
	
	private void renderSignHighlights(MatrixStack matrixStack,
		float partialTicks, SignFinderConfig config)
	{
		SignEspStyle style = config.highlight_style;
		
		// 分别渲染每个搜索结果告示牌，支持自定义颜色
		for(SignBlockEntity sign : searchResultSigns)
		{
			Box signBox = new Box(sign.getPos());
			int color = getSignHighlightColor(sign.getPos());
			
			renderSingleSign(matrixStack, partialTicks, signBox, color, style);
		}
		
		// 渲染自动检测的告示牌（使用默认颜色）
		if(!highlightedSigns.isEmpty())
		{
			List<Box> autoDetectedBoxes = new ArrayList<>();
			for(SignBlockEntity sign : highlightedSigns)
			{
				autoDetectedBoxes.add(new Box(sign.getPos()));
			}
			
			int defaultColor = config.sign_highlight_color;
			renderSignGroup(matrixStack, partialTicks, autoDetectedBoxes,
				defaultColor, style);
		}
	}
	
	private void renderSingleSign(MatrixStack matrixStack, float partialTicks,
		Box signBox, int color, SignEspStyle style)
	{
		List<Box> singleBoxList = List.of(signBox);
		renderSignGroup(matrixStack, partialTicks, singleBoxList, color, style);
	}
	
	private void renderSignGroup(MatrixStack matrixStack, float partialTicks,
		List<Box> signBoxes, int color, SignEspStyle style)
	{
		if(signBoxes.isEmpty())
			return;
		
		if(style.hasBoxes())
		{
			int quadsColor = (color & 0xFFFFFF) | (0x60 << 24);
			int linesColor = (color & 0xFFFFFF) | (0xFF << 24);
			RenderUtils.drawSolidBoxes(matrixStack, signBoxes, quadsColor,
				false);
			RenderUtils.drawOutlinedBoxes(matrixStack, signBoxes, linesColor,
				false);
		}
		
		if(style.hasLines())
		{
			List<Vec3d> ends = signBoxes.stream().map(Box::getCenter).toList();
			int tracerColor = (color & 0xFFFFFF) | (0x80 << 24);
			
			RenderUtils.drawTracers(matrixStack, partialTicks, ends,
				tracerColor, false);
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
	
	public void setSearchResults(List<SignSearchResult> results)
	{
		searchResultSigns.clear();
		if(MC.world == null)
			return;
		
		for(SignSearchResult result : results)
		{
			// 通过BlockPos获取SignBlockEntity
			if(MC.world.getBlockEntity(
				result.getPos()) instanceof SignBlockEntity signEntity)
			{
				searchResultSigns.add(signEntity);
			}
		}
		
		LOGGER.info("Set search results: {} signs", searchResultSigns.size());
	}
	
	public void clearSearchResults()
	{
		searchResultSigns.clear();
		customSignColors.clear();
	}
	
	public boolean removeSearchResultByPos(int x, int y, int z)
	{
		BlockPos targetPos = new BlockPos(x, y, z);
		boolean removed =
			searchResultSigns.removeIf(sign -> sign.getPos().equals(targetPos));
		if(removed)
		{
			customSignColors.remove(targetPos);
		}
		return removed;
	}
	
	public String cycleHighlightColor(int x, int y, int z)
	{
		BlockPos targetPos = new BlockPos(x, y, z);
		
		boolean hasSign = searchResultSigns.stream()
			.anyMatch(sign -> sign.getPos().equals(targetPos));
		if(!hasSign)
			return null;
		
		int currentColor =
			customSignColors.getOrDefault(targetPos, COLOR_CYCLE[0]);
		int currentIndex = 0;
		for(int i = 0; i < COLOR_CYCLE.length; i++)
		{
			if(COLOR_CYCLE[i] == currentColor)
			{
				currentIndex = i;
				break;
			}
		}
		
		int nextIndex = (currentIndex + 1) % COLOR_CYCLE.length;
		customSignColors.put(targetPos, COLOR_CYCLE[nextIndex]);
		
		return COLOR_NAMES[nextIndex];
	}
	
	public int getSignHighlightColor(BlockPos pos)
	{
		return customSignColors.getOrDefault(pos,
			configHolder.getConfig().sign_highlight_color);
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
	
	public void cleanup()
	{
		highlightedSigns.clear();
		searchResultSigns.clear();
		customSignColors.clear();
		
		SignSearchEngine.clearCaches();
		CommandUtils.clearAllCaches();
	}
	
	public static SignFinderMod getInstance()
	{
		return SignFinderModInitializer.getInstance();
	}
}

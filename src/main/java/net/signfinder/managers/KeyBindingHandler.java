package net.signfinder.managers;

import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;

public class KeyBindingHandler
{
	private static final MinecraftClient MC = MinecraftClient.getInstance();
	
	private final KeyBinding toggleAutoDetectionKey;
	private final KeyBinding toggleHighlightingKey;
	private final ConfigHolder<SignFinderConfig> configHolder;
	private final EntityDetectionManager detectionManager;
	private static final KeyBinding.Category CATEGORY =
		KeyBinding.Category.create(Identifier.of(SignFinderMod.MOD_ID, "main"));
	
	public KeyBindingHandler(ConfigHolder<SignFinderConfig> configHolder,
		EntityDetectionManager detectionManager)
	{
		this.configHolder = configHolder;
		this.detectionManager = detectionManager;
		
		toggleAutoDetectionKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.signfinder.toggle_auto_detection",
				InputUtil.UNKNOWN_KEY.getCode(), CATEGORY));
		
		toggleHighlightingKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.signfinder.toggle_highlighting",
				InputUtil.UNKNOWN_KEY.getCode(), CATEGORY));
		
		registerEventHandlers();
	}
	
	private void registerEventHandlers()
	{
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while(toggleAutoDetectionKey.wasPressed())
				toggleAutoDetection();
			
			while(toggleHighlightingKey.wasPressed())
				toggleHighlighting();
		});
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
			detectionManager.clearHighlighted();
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
}

package net.signfinder.managers;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.signfinder.SignFinderConfig;

public class KeyMappingHandler
{
    private static final Minecraft MC = Minecraft.getInstance();

    private final KeyMapping toggleAutoDetectionKey;
    private final KeyMapping toggleHighlightingKey;
    private final ConfigHolder<SignFinderConfig> configHolder;
    private final EntityDetectionManager detectionManager;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath("signfinder", "signfinder"));

    public KeyMappingHandler(ConfigHolder<SignFinderConfig> configHolder,
                             EntityDetectionManager detectionManager)
    {
        this.configHolder = configHolder;
        this.detectionManager = detectionManager;

        toggleAutoDetectionKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.signfinder.toggle_auto_detection",
                        InputConstants.UNKNOWN.getValue(),
                        CATEGORY));

        toggleHighlightingKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.signfinder.toggle_highlighting",
                        InputConstants.UNKNOWN.getValue(),
                        CATEGORY));

        registerEventHandlers();
    }

    private void registerEventHandlers()
    {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(toggleAutoDetectionKey.consumeClick())
                toggleAutoDetection();

            while(toggleHighlightingKey.consumeClick())
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
            detectionManager.clearHighlighted();

        MC.player.displayClientMessage(Component.translatable(
                config.enable_auto_detection
                        ? "signfinder.message.auto_detection_enabled"
                        : "signfinder.message.auto_detection_disabled"
        ), false);
    }

    private void toggleHighlighting()
    {
        if(MC.player == null)
            return;

        SignFinderConfig config = configHolder.getConfig();
        config.enable_sign_highlighting =
                !config.enable_sign_highlighting;
        configHolder.save();

        MC.player.displayClientMessage(Component.translatable(
                config.enable_sign_highlighting
                        ? "signfinder.message.highlighting_enabled"
                        : "signfinder.message.highlighting_disabled"
        ), false);
    }
}

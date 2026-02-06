package net.signfinder.commands.specialized;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.commands.core.BaseCommand;
import net.signfinder.commands.core.CommandConstants;

public class PresetCommand extends BaseCommand
{
	public static int listPresets(CommandContext<FabricClientCommandSource> ctx)
	{
		SignFinderConfig config =
			SignFinderMod.getInstance().getConfigHolder().getConfig();
		
		ctx.getSource()
			.sendFeedback(Component
				.translatable("signfinder.message.search_presets_title")
				.withStyle(ChatFormatting.YELLOW));
		ctx.getSource().sendFeedback(Component.literal(""));
		
		displayTextPresets(ctx, config);
		displayRegexPresets(ctx, config);
		displayUsageInstructions(ctx, config);
		
		return 1;
	}
	
	private static void displayTextPresets(
		CommandContext<FabricClientCommandSource> ctx, SignFinderConfig config)
	{
		if(!config.search_presets.text_presets.isEmpty())
		{
			ctx.getSource()
				.sendFeedback(Component
					.translatable("signfinder.message.text_presets_header",
						config.search_presets.text_presets.size())
					.withStyle(ChatFormatting.GREEN));
			config.search_presets.text_presets.forEach((name, value) -> {
				Component presetText =
					createPresetText(name, value, ChatFormatting.AQUA);
				ctx.getSource().sendFeedback(presetText);
			});
			ctx.getSource().sendFeedback(Component.literal(""));
		}else
		{
			ctx.getSource().sendFeedback(
				Component.translatable("signfinder.message.no_text_presets")
					.withStyle(ChatFormatting.GRAY));
		}
	}
	
	private static void displayRegexPresets(
		CommandContext<FabricClientCommandSource> ctx, SignFinderConfig config)
	{
		if(!config.search_presets.regex_presets.isEmpty())
		{
			ctx.getSource()
				.sendFeedback(Component
					.translatable("signfinder.message.regex_presets_header",
						config.search_presets.regex_presets.size())
					.withStyle(ChatFormatting.GREEN));
			config.search_presets.regex_presets.forEach((name, value) -> {
				Component presetText =
					createPresetText(name, value, ChatFormatting.LIGHT_PURPLE);
				ctx.getSource().sendFeedback(presetText);
			});
			ctx.getSource().sendFeedback(Component.literal(""));
		}else
		{
			ctx.getSource()
				.sendFeedback(Component
					.translatable("signfinder.message.no_regex_presets")
					.withStyle(ChatFormatting.GRAY));
		}
	}
	
	private static Component createPresetText(String name, String value,
		ChatFormatting nameColor)
	{
		return Component.literal("• " + name).withStyle(style -> style
			.withColor(nameColor)
			.withClickEvent(
				new ClickEvent.SuggestCommand(CommandConstants.COMMAND_PREFIX
					+ " " + CommandConstants.SUBCOMMAND_PRESET + " " + name))
			.withHoverEvent(new HoverEvent.ShowText(Component
				.translatable("signfinder.message.preset_click_hint", name))))
			.append(Component.literal(" → ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(value).withStyle(ChatFormatting.WHITE));
	}
	
	private static void displayUsageInstructions(
		CommandContext<FabricClientCommandSource> ctx, SignFinderConfig config)
	{
		if(config.search_presets.text_presets.isEmpty()
			&& config.search_presets.regex_presets.isEmpty())
		{
			ctx.getSource()
				.sendFeedback(Component
					.translatable("signfinder.message.preset_usage_help")
					.withStyle(ChatFormatting.GRAY));
		}else
		{
			ctx.getSource()
				.sendFeedback(Component
					.translatable("signfinder.message.preset_usage_tip")
					.withStyle(ChatFormatting.GRAY));
		}
	}
}

package net.signfinder.commands.specialized;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
		
		ctx.getSource().sendFeedback(
			Text.translatable("signfinder.message.search_presets_title")
				.formatted(Formatting.YELLOW));
		ctx.getSource().sendFeedback(Text.literal(""));
		
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
				.sendFeedback(Text
					.translatable("signfinder.message.text_presets_header",
						config.search_presets.text_presets.size())
					.formatted(Formatting.GREEN));
			config.search_presets.text_presets.forEach((name, value) -> {
				MutableText presetText =
					createPresetText(name, value, Formatting.AQUA);
				ctx.getSource().sendFeedback(presetText);
			});
			ctx.getSource().sendFeedback(Text.literal(""));
		}else
		{
			ctx.getSource().sendFeedback(
				Text.translatable("signfinder.message.no_text_presets")
					.formatted(Formatting.GRAY));
		}
	}
	
	private static void displayRegexPresets(
		CommandContext<FabricClientCommandSource> ctx, SignFinderConfig config)
	{
		if(!config.search_presets.regex_presets.isEmpty())
		{
			ctx.getSource()
				.sendFeedback(Text
					.translatable("signfinder.message.regex_presets_header",
						config.search_presets.regex_presets.size())
					.formatted(Formatting.GREEN));
			config.search_presets.regex_presets.forEach((name, value) -> {
				MutableText presetText =
					createPresetText(name, value, Formatting.LIGHT_PURPLE);
				ctx.getSource().sendFeedback(presetText);
			});
			ctx.getSource().sendFeedback(Text.literal(""));
		}else
		{
			ctx.getSource().sendFeedback(
				Text.translatable("signfinder.message.no_regex_presets")
					.formatted(Formatting.GRAY));
		}
	}
	
	private static MutableText createPresetText(String name, String value,
		Formatting nameColor)
	{
		MutableText presetText = Text.literal("• " + name).formatted(nameColor)
			.append(Text.literal(" → ").formatted(Formatting.GRAY))
			.append(Text.literal(value).formatted(Formatting.WHITE));
		
		// 添加点击复制功能和悬停提示
		presetText.styled(style -> style
			.withClickEvent(
				new ClickEvent.SuggestCommand(CommandConstants.COMMAND_PREFIX
					+ " " + CommandConstants.SUBCOMMAND_PRESET + " " + name))
			.withHoverEvent(new HoverEvent.ShowText(Text
				.translatable("signfinder.message.preset_click_hint", name))));
		
		return presetText;
	}
	
	private static void displayUsageInstructions(
		CommandContext<FabricClientCommandSource> ctx, SignFinderConfig config)
	{
		if(config.search_presets.text_presets.isEmpty()
			&& config.search_presets.regex_presets.isEmpty())
		{
			ctx.getSource().sendFeedback(
				Text.translatable("signfinder.message.preset_usage_help")
					.formatted(Formatting.GRAY));
		}else
		{
			ctx.getSource().sendFeedback(
				Text.translatable("signfinder.message.preset_usage_tip")
					.formatted(Formatting.GRAY));
		}
	}
}

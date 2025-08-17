package net.signfinder.commands.specialized;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.signfinder.SignFinderMod;
import net.signfinder.commands.core.BaseCommand;
import net.signfinder.commands.core.CommandUtils;

public class HighlightCommand extends BaseCommand
{
	public static int removeHighlight(
		CommandContext<FabricClientCommandSource> ctx)
	{
		if(!validatePlayerInWorld(ctx))
			return 0;
		
		int x = IntegerArgumentType.getInteger(ctx, "x");
		int y = IntegerArgumentType.getInteger(ctx, "y");
		int z = IntegerArgumentType.getInteger(ctx, "z");
		
		SignFinderMod signFinder = getSignFinderInstance(ctx);
		if(signFinder != null)
		{
			boolean removed = signFinder.removeSearchResultByPos(x, y, z);
			if(removed)
			{
				ctx.getSource().sendFeedback(
					Text.translatable("signfinder.message.highlight_removed", x,
						y, z).formatted(Formatting.GREEN));
			}else
			{
				ctx.getSource().sendFeedback(
					Text.translatable("signfinder.message.highlight_not_found",
						x, y, z).formatted(Formatting.YELLOW));
			}
		}
		
		return 1;
	}
	
	public static int changeHighlightColor(
		CommandContext<FabricClientCommandSource> ctx)
	{
		if(!validatePlayerInWorld(ctx))
			return 0;
		
		int x = IntegerArgumentType.getInteger(ctx, "x");
		int y = IntegerArgumentType.getInteger(ctx, "y");
		int z = IntegerArgumentType.getInteger(ctx, "z");
		
		SignFinderMod signFinder = getSignFinderInstance(ctx);
		if(signFinder != null)
		{
			String colorName = signFinder.cycleHighlightColor(x, y, z);
			if(colorName != null)
			{
				// 获取颜色对应的 Minecraft 格式化颜色
				int color =
					signFinder.getSignHighlightColor(new BlockPos(x, y, z));
				Formatting mcColor = getMinecraftColor(color);
				
				MutableText colorText =
					Text.translatable("signfinder.color." + colorName);
				if(mcColor != null)
				{
					colorText = colorText.formatted(mcColor);
				}
				
				MutableText message =
					Text.translatable("signfinder.message.color_changed_to", x,
						y, z).append(" ").append(colorText);
				ctx.getSource()
					.sendFeedback(message.formatted(Formatting.GREEN));
			}else
			{
				ctx.getSource().sendFeedback(
					Text.translatable("signfinder.message.highlight_not_found",
						x, y, z).formatted(Formatting.YELLOW));
			}
		}
		
		return 1;
	}
	
	public static int clearResults(
		CommandContext<FabricClientCommandSource> ctx)
	{
		if(!validatePlayerInWorld(ctx))
			return 0;
		
		String cacheKey = getPlayerCacheKey();
		CommandUtils.clearCache(cacheKey);
		
		// 清除SignFinderMod中的渲染结果
		SignFinderMod signFinder = getSignFinderInstance(ctx);
		if(signFinder != null)
		{
			signFinder.clearSearchResults();
		}
		
		ctx.getSource().sendFeedback(
			Text.translatable("signfinder.message.results_cleared")
				.formatted(Formatting.GREEN));
		return 1;
	}
	
	private static Formatting getMinecraftColor(int color)
	{
		return switch(color)
		{
			case 0x00FF00 -> Formatting.GREEN; // 绿色
			case 0xFF0000 -> Formatting.RED; // 红色
			case 0x0000FF -> Formatting.BLUE; // 蓝色
			case 0xFFFF00 -> Formatting.YELLOW; // 黄色
			case 0xFF00FF -> Formatting.LIGHT_PURPLE; // 紫色
			case 0x00FFFF -> Formatting.AQUA; // 青色
			case 0xFF8000 -> Formatting.GOLD; // 橙色
			case 0xFFFFFF -> Formatting.WHITE; // 白色
			default -> Formatting.GREEN;
		};
	}
}

package net.signfinder.commands.specialized;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
				ctx.getSource()
					.sendFeedback(
						Component
							.translatable(
								"signfinder.message.highlight_removed", x, y, z)
							.withStyle(ChatFormatting.GREEN));
			}else
			{
				ctx.getSource()
					.sendFeedback(Component
						.translatable("signfinder.message.highlight_not_found",
							x, y, z)
						.withStyle(ChatFormatting.YELLOW));
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
				ChatFormatting mcColor = getMinecraftColor(color);
				
				Component colorComponent =
					Component.translatable("signfinder.color." + colorName)
						.withStyle(mcColor);
				
				Component message = Component
					.translatable("signfinder.message.color_changed_to", x, y,
						z)
					.append(" ").append(colorComponent);
				ctx.getSource().sendFeedback(
					message.copy().withStyle(ChatFormatting.GREEN));
			}else
			{
				ctx.getSource()
					.sendFeedback(Component
						.translatable("signfinder.message.highlight_not_found",
							x, y, z)
						.withStyle(ChatFormatting.YELLOW));
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
			Component.translatable("signfinder.message.results_cleared")
				.withStyle(ChatFormatting.GREEN));
		return 1;
	}
	
	private static ChatFormatting getMinecraftColor(int color)
	{
		return switch(color)
		{
			case 0x00FF00 -> ChatFormatting.GREEN; // 绿色
			case 0xFF0000 -> ChatFormatting.RED; // 红色
			case 0x0000FF -> ChatFormatting.BLUE; // 蓝色
			case 0xFFFF00 -> ChatFormatting.YELLOW; // 黄色
			case 0xFF00FF -> ChatFormatting.LIGHT_PURPLE; // 紫色
			case 0x00FFFF -> ChatFormatting.AQUA; // 青色
			case 0xFF8000 -> ChatFormatting.GOLD; // 橙色
			case 0xFFFFFF -> ChatFormatting.WHITE; // 白色
			default -> ChatFormatting.GREEN;
		};
	}
}

package net.signfinder.commands.core;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.signfinder.SignFinderMod;

public abstract class BaseCommand
{
	protected static final Minecraft MC = Minecraft.getInstance();
	
	protected static boolean validatePlayerInWorld(
		CommandContext<FabricClientCommandSource> ctx)
	{
		if(MC.player == null || MC.level == null)
		{
			ctx.getSource()
				.sendFeedback(Component.translatable("signfinder.error.not_in_world").withStyle(ChatFormatting.RED));
			return false;
		}
		return true;
	}
	
	protected static SignFinderMod getSignFinderInstance(
		CommandContext<FabricClientCommandSource> ctx)
	{
		SignFinderMod signFinder = SignFinderMod.getInstance();
		if(signFinder == null)
		{
			ctx.getSource().sendFeedback(
				Component.translatable("signfinder.error.not_initialized")
					.withStyle(ChatFormatting.RED));
		}
		return signFinder;
	}
	
	protected static String getPlayerCacheKey()
	{
		return MC.player != null ? MC.player.getStringUUID() : null;
	}
}

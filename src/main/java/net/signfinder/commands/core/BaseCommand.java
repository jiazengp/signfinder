package net.signfinder.commands.core;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.signfinder.SignFinderMod;

public abstract class BaseCommand
{
	protected static final MinecraftClient MC = MinecraftClient.getInstance();
	
	protected static boolean validatePlayerInWorld(
		CommandContext<FabricClientCommandSource> ctx)
	{
		if(MC.player == null || MC.world == null)
		{
			ctx.getSource()
				.sendFeedback(Text.translatable("signfinder.error.not_in_world")
					.formatted(Formatting.RED));
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
				Text.translatable("signfinder.error.not_initialized")
					.formatted(Formatting.RED));
		}
		return signFinder;
	}
	
	protected static String getPlayerCacheKey()
	{
		return MC.player != null ? MC.player.getUuidAsString() : null;
	}
}

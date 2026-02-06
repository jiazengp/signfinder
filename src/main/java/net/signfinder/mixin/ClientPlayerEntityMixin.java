package net.signfinder.mixin;

import net.signfinder.SignFinderMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayer
{
	public ClientPlayerEntityMixin(SignFinderMod chestEspMod, ClientLevel world,
		GameProfile profile)
	{
		super(world, profile);
	}
	
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
		ordinal = 0), method = "tick()V")
	private void onTick(CallbackInfo ci)
	{
		SignFinderMod signFinderMod = SignFinderMod.getInstance();
		if(signFinderMod != null)
			signFinderMod.onUpdate();
	}
}

package net.signfinder.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.signfinder.SignFinderMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements AutoCloseable
{
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
		ordinal = 0),
		method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V")
	private void onBobView(GameRenderer instance, PoseStack matrices,
		float tickDelta, Operation<Void> original)
	{
		SignFinderMod signFinder = SignFinderMod.getInstance();
		
		if(signFinder == null || !signFinder.shouldCancelViewBobbing())
			original.call(instance, matrices, tickDelta);
	}
}

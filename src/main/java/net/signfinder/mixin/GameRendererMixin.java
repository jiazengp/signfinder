package net.signfinder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.signfinder.SignFinderMod;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements AutoCloseable
{
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/GameRenderer;bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		ordinal = 0),
		method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V")
	private void onBobView(GameRenderer instance, MatrixStack matrices,
		float tickDelta, Operation<Void> original)
	{
		SignFinderMod signFinder = SignFinderMod.getInstance();
		
		if(signFinder == null || !signFinder.shouldCancelViewBobbing())
			original.call(instance, matrices, tickDelta);
	}
}

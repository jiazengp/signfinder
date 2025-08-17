package net.signfinder.rendering;

import net.minecraft.client.render.RenderLayer;

public final class SignFinderRenderLayers
{
	/**
	 * Returns either normal lines or ESP lines depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderLayer getLines()
	{
		return RenderLayer.getLines();
	}
	
	/**
	 * Returns either normal quads or ESP quads depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderLayer getQuads()
	{
		return RenderLayer.getDebugQuads();
	}
}

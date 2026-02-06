package net.signfinder.rendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.resources.Identifier;

public enum SignFinderPipelines
{
	;
	
	/**
	 * Similar to the RENDERTYPE_LINES Snippet, but without fog.
	 */
	public static final Snippet FOGLESS_LINES_SNIPPET = RenderPipeline
		.builder(RenderPipelines.MATRICES_FOG_SNIPPET,
			RenderPipelines.GLOBALS_SNIPPET)
		.withVertexShader(Identifier.parse("signfinder:core/fogless_lines"))
		.withFragmentShader(Identifier.parse("signfinder:core/fogless_lines"))
		.withBlend(BlendFunction.TRANSLUCENT).withCull(false)
		.withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
			Mode.LINES)
		.buildSnippet();
	
	/**
	 * Similar to the LINES ShaderPipeline, but with no fog.
	 */
	public static final RenderPipeline DEPTH_TEST_LINES =
		RenderPipelines.register(RenderPipeline.builder(FOGLESS_LINES_SNIPPET)
			.withLocation(
				Identifier.parse("signfinder:pipeline/depth_test_lines"))
			.build());
	
	/**
	 * Similar to the LINES ShaderPipeline, but with no depth test or fog.
	 */
	public static final RenderPipeline ESP_LINES =
		RenderPipelines.register(RenderPipeline.builder(FOGLESS_LINES_SNIPPET)
			.withLocation(Identifier.parse("signfinder:pipeline/esp_lines"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
	
	/**
	 * Similar to the DEBUG_QUADS ShaderPipeline, but with culling enabled.
	 */
	public static final RenderPipeline QUADS = RenderPipelines
		.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Identifier.parse("signfinder:pipeline/quads"))
			.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
			.build());
	
	/**
	 * Similar to the DEBUG_QUADS ShaderPipeline, but with culling enabled
	 * and no depth test.
	 */
	public static final RenderPipeline ESP_QUADS = RenderPipelines
		.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Identifier.parse("signfinder:pipeline/esp_quads"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
}

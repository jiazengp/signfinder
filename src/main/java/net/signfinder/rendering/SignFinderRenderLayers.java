package net.signfinder.rendering;

import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class SignFinderRenderLayers
{
    /**
     * Similar to {@link RenderType#getLines()}, but with line width 2.
     */
    public static final RenderType LINES = RenderType.create("signfinder:lines",
            RenderSetup.builder(SignFinderPipelines.DEPTH_TEST_LINES)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup());

    /**
     * Similar to {@link RenderType#getLines()}, but with line width 2 and no
     * depth test.
     */
    public static final RenderType ESP_LINES =
            RenderType.create("signfinder:esp_lines",
                    RenderSetup.builder(SignFinderPipelines.ESP_LINES)
                            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                            .createRenderSetup());

    /**
     * Similar to {@link RenderType#getDebugQuads()}, but with culling enabled.
     */
    public static final RenderType QUADS = RenderType.create("signfinder:quads",
            RenderSetup.builder(SignFinderPipelines.QUADS).sortOnUpload()
                    .createRenderSetup());

    /**
     * Similar to {@link RenderType#getDebugQuads()}, but with culling enabled
     * and no depth test.
     */
    public static final RenderType ESP_QUADS = RenderType.create(
            "signfinder:esp_quads", RenderSetup.builder(SignFinderPipelines.ESP_QUADS)
                    .sortOnUpload().createRenderSetup());

    /**
     * Returns either {@link #QUADS} or {@link #ESP_QUADS} depending on the
     * value of {@code depthTest}.
     */
    public static RenderType getQuads(boolean depthTest)
    {
        return depthTest ? QUADS : ESP_QUADS;
    }

    /**
     * Returns either {@link #LINES} or {@link #ESP_LINES} depending on the
     * value of {@code depthTest}.
     */
    public static RenderType getLines(boolean depthTest)
    {
        return depthTest ? LINES : ESP_LINES;
    }
}

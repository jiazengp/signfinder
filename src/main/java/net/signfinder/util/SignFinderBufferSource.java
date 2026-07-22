package net.signfinder.util;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * 26.2 replacement for the removed {@code MultiBufferSource.BufferSource},
 * using {@link StagedVertexBuffer} (based on Wurst7's WurstBufferSource).
 */
public final class SignFinderBufferSource
{
	private final StagedVertexBuffer stagedBuffer =
		new StagedVertexBuffer(() -> "SignFinder", RenderType.BIG_BUFFER_SIZE);
	private final List<StagedVertexBuffer.Draw> draws = new ArrayList<>();
	private final List<RenderType> drawTypes = new ArrayList<>();
	
	public VertexConsumer getBuffer(RenderType renderType)
	{
		if(!drawTypes.isEmpty() && drawTypes.getLast() == renderType
			&& renderType.canConsolidateConsecutiveGeometry())
			return stagedBuffer.getVertexBuilder(draws.getLast());
		
		StagedVertexBuffer.Draw draw =
			stagedBuffer.appendDraw(renderType.format(),
				renderType.primitiveTopology(), renderType.sortOnUpload()
					? RenderSystem.getProjectionType().vertexSorting() : null);
		
		draws.add(draw);
		drawTypes.add(renderType);
		return stagedBuffer.getVertexBuilder(draw);
	}
	
	public void uploadAndDraw()
	{
		try
		{
			if(draws.isEmpty())
				return;
			
			stagedBuffer.upload();
			
			for(int i = 0; i < draws.size(); i++)
			{
				StagedVertexBuffer.ExecuteInfo info =
					stagedBuffer.getExecuteInfo(draws.get(i));
				if(info != null)
					drawTypes.get(i).prepare().drawFromBuffer(info);
			}
			
			stagedBuffer.endDraw();
			
		}finally
		{
			draws.clear();
			drawTypes.clear();
			stagedBuffer.close();
		}
	}
}

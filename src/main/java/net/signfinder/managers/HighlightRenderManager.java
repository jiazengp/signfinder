package net.signfinder.managers;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.signfinder.core.SignEspStyle;
import net.signfinder.SignFinderConfig;
import net.signfinder.SignFinderMod;
import net.signfinder.util.RenderUtils;
import net.signfinder.util.ColorUtils;

public class HighlightRenderManager
{
	
	private final ColorManager colorManager;
	
	public HighlightRenderManager(ColorManager colorManager)
	{
		this.colorManager = colorManager;
	}
	
	public void renderHighlights(PoseStack PoseStack, float partialTicks,
		SignFinderConfig config, List<SignBlockEntity> searchResultSigns,
		List<ItemFrame> searchResultItemFrames,
		List<SignBlockEntity> highlightedSigns,
		List<ItemFrame> highlightedItemFrames)
	{
		if(!config.enable_sign_highlighting)
			return;
		
		if(hasAnyHighlights(searchResultSigns, searchResultItemFrames,
			highlightedSigns, highlightedItemFrames))
		{
			SignFinderMod.LOGGER.debug(
				"Rendering {} search result signs, {} auto-detected signs, {} search result item frames, {} auto-detected item frames",
				searchResultSigns.size(), highlightedSigns.size(),
				searchResultItemFrames.size(), highlightedItemFrames.size());
			
			renderEntityHighlights(PoseStack, partialTicks, config,
				searchResultSigns, searchResultItemFrames, highlightedSigns,
				highlightedItemFrames);
		}
	}
	
	private boolean hasAnyHighlights(List<SignBlockEntity> searchResultSigns,
		List<ItemFrame> searchResultItemFrames,
		List<SignBlockEntity> highlightedSigns,
		List<ItemFrame> highlightedItemFrames)
	{
		return !searchResultSigns.isEmpty() || !highlightedSigns.isEmpty()
			|| !searchResultItemFrames.isEmpty()
			|| !highlightedItemFrames.isEmpty();
	}
	
	private void renderEntityHighlights(PoseStack PoseStack, float partialTicks,
		SignFinderConfig config, List<SignBlockEntity> searchResultSigns,
		List<ItemFrame> searchResultItemFrames,
		List<SignBlockEntity> highlightedSigns,
		List<ItemFrame> highlightedItemFrames)
	{
		SignEspStyle style = config.highlight_style;
		
		// Render search results (supports custom colors)
		renderSearchResults(PoseStack, partialTicks, style, searchResultSigns,
			searchResultItemFrames);
		
		// Render auto-detected results (uses default colors)
		renderAutoDetectedEntities(PoseStack, partialTicks, config, style,
			highlightedSigns, highlightedItemFrames);
	}
	
	private void renderSearchResults(PoseStack PoseStack, float partialTicks,
		SignEspStyle style, List<SignBlockEntity> searchResultSigns,
		List<ItemFrame> searchResultItemFrames)
	{
		// 分别渲染每个搜索结果告示牌，支持自定义颜色
		for(SignBlockEntity sign : searchResultSigns)
		{
			AABB signAABB = new AABB(sign.getBlockPos());
			int color = colorManager.getHighlightColor(sign.getBlockPos());
			renderSingleEntity(PoseStack, partialTicks, signAABB, color, style);
		}
		
		// 分别渲染每个搜索结果物品展示框，支持自定义颜色
		for(ItemFrame itemFrame : searchResultItemFrames)
		{
			AABB itemFrameAABB = new AABB(itemFrame.getPos());
			int color = colorManager.getHighlightColor(itemFrame.getPos());
			renderSingleEntity(PoseStack, partialTicks, itemFrameAABB, color,
				style);
		}
	}
	
	private void renderAutoDetectedEntities(PoseStack PoseStack,
		float partialTicks, SignFinderConfig config, SignEspStyle style,
		List<SignBlockEntity> highlightedSigns,
		List<ItemFrame> highlightedItemFrames)
	{
		int defaultColor = config.sign_highlight_color;
		
		// 渲染自动检测的告示牌（使用默认颜色）
		if(!highlightedSigns.isEmpty())
		{
			List<AABB> autoDetectedAABBes = new ArrayList<>();
			for(SignBlockEntity sign : highlightedSigns)
			{
				autoDetectedAABBes.add(new AABB(sign.getBlockPos()));
			}
			renderEntityGroup(PoseStack, partialTicks, autoDetectedAABBes,
				defaultColor, style);
		}
		
		// 渲染自动检测的物品展示框（使用默认颜色）
		if(!highlightedItemFrames.isEmpty())
		{
			List<AABB> autoDetectedItemFrameAABBes = new ArrayList<>();
			for(ItemFrame itemFrame : highlightedItemFrames)
			{
				autoDetectedItemFrameAABBes.add(new AABB(itemFrame.getPos()));
			}
			renderEntityGroup(PoseStack, partialTicks,
				autoDetectedItemFrameAABBes, defaultColor, style);
		}
	}
	
	private void renderSingleEntity(PoseStack PoseStack, float partialTicks,
		AABB entityAABB, int color, SignEspStyle style)
	{
		List<AABB> singleAABBList = List.of(entityAABB);
		renderEntityGroup(PoseStack, partialTicks, singleAABBList, color,
			style);
	}
	
	private void renderEntityGroup(PoseStack PoseStack, float partialTicks,
		List<AABB> entityAABBes, int color, SignEspStyle style)
	{
		if(entityAABBes.isEmpty())
			return;
		
		// 获取配置的透明度设置
		SignFinderConfig config = SignFinderMod.getInstance().getConfig();
		int configuredAlpha = config.highlight_transparency;
		
		if(style.hasBoxes())
		{
			// 使用配置的透明度为填充颜色，但稍微降低一些以避免过于突出
			int fillAlpha = Math.max(10, configuredAlpha * 60 / 255); // 保持相对透明
			int quadsColor = ColorUtils.combineRgbWithAlpha(color, fillAlpha);
			
			// 轮廓线使用更高的不透明度以确保可见性
			int outlineAlpha = Math.max(128, configuredAlpha); // 至少50%不透明度
			int linesColor =
				ColorUtils.combineRgbWithAlpha(color, outlineAlpha);
			
			RenderUtils.drawSolidBoxes(PoseStack, entityAABBes, quadsColor,
				false);
			RenderUtils.drawSolidBoxes(PoseStack, entityAABBes, linesColor,
				false);
		}
		
		if(style.hasLines())
		{
			List<Vec3> ends =
				entityAABBes.stream().map(AABB::getCenter).toList();
			// 追踪线使用配置的透明度
			int tracerColor =
				ColorUtils.combineRgbWithAlpha(color, configuredAlpha);
			RenderUtils.drawTracers(PoseStack, partialTicks, ends, tracerColor,
				false);
		}
	}
}

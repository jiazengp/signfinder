package net.signfinder.managers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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
	
	public void renderHighlights(MatrixStack matrixStack, float partialTicks,
		SignFinderConfig config, List<SignBlockEntity> searchResultSigns,
		List<ItemFrameEntity> searchResultItemFrames,
		List<SignBlockEntity> highlightedSigns,
		List<ItemFrameEntity> highlightedItemFrames)
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
			
			renderEntityHighlights(matrixStack, partialTicks, config,
				searchResultSigns, searchResultItemFrames, highlightedSigns,
				highlightedItemFrames);
		}
	}
	
	private boolean hasAnyHighlights(List<SignBlockEntity> searchResultSigns,
		List<ItemFrameEntity> searchResultItemFrames,
		List<SignBlockEntity> highlightedSigns,
		List<ItemFrameEntity> highlightedItemFrames)
	{
		return !searchResultSigns.isEmpty() || !highlightedSigns.isEmpty()
			|| !searchResultItemFrames.isEmpty()
			|| !highlightedItemFrames.isEmpty();
	}
	
	private void renderEntityHighlights(MatrixStack matrixStack,
		float partialTicks, SignFinderConfig config,
		List<SignBlockEntity> searchResultSigns,
		List<ItemFrameEntity> searchResultItemFrames,
		List<SignBlockEntity> highlightedSigns,
		List<ItemFrameEntity> highlightedItemFrames)
	{
		SignEspStyle style = config.highlight_style;
		
		// Render search results (supports custom colors)
		renderSearchResults(matrixStack, partialTicks, style, searchResultSigns,
			searchResultItemFrames);
		
		// Render auto-detected results (uses default colors)
		renderAutoDetectedEntities(matrixStack, partialTicks, config, style,
			highlightedSigns, highlightedItemFrames);
	}
	
	private void renderSearchResults(MatrixStack matrixStack,
		float partialTicks, SignEspStyle style,
		List<SignBlockEntity> searchResultSigns,
		List<ItemFrameEntity> searchResultItemFrames)
	{
		// 分别渲染每个搜索结果告示牌，支持自定义颜色
		for(SignBlockEntity sign : searchResultSigns)
		{
			Box signBox = new Box(sign.getPos());
			int color = colorManager.getHighlightColor(sign.getPos());
			renderSingleEntity(matrixStack, partialTicks, signBox, color,
				style);
		}
		
		// 分别渲染每个搜索结果物品展示框，支持自定义颜色
		for(ItemFrameEntity itemFrame : searchResultItemFrames)
		{
			Box itemFrameBox = new Box(itemFrame.getBlockPos());
			int color = colorManager.getHighlightColor(itemFrame.getBlockPos());
			renderSingleEntity(matrixStack, partialTicks, itemFrameBox, color,
				style);
		}
	}
	
	private void renderAutoDetectedEntities(MatrixStack matrixStack,
		float partialTicks, SignFinderConfig config, SignEspStyle style,
		List<SignBlockEntity> highlightedSigns,
		List<ItemFrameEntity> highlightedItemFrames)
	{
		int defaultColor = config.sign_highlight_color;
		
		// 渲染自动检测的告示牌（使用默认颜色）
		if(!highlightedSigns.isEmpty())
		{
			List<Box> autoDetectedBoxes = new ArrayList<>();
			for(SignBlockEntity sign : highlightedSigns)
			{
				autoDetectedBoxes.add(new Box(sign.getPos()));
			}
			renderEntityGroup(matrixStack, partialTicks, autoDetectedBoxes,
				defaultColor, style);
		}
		
		// 渲染自动检测的物品展示框（使用默认颜色）
		if(!highlightedItemFrames.isEmpty())
		{
			List<Box> autoDetectedItemFrameBoxes = new ArrayList<>();
			for(ItemFrameEntity itemFrame : highlightedItemFrames)
			{
				autoDetectedItemFrameBoxes
					.add(new Box(itemFrame.getBlockPos()));
			}
			renderEntityGroup(matrixStack, partialTicks,
				autoDetectedItemFrameBoxes, defaultColor, style);
		}
	}
	
	private void renderSingleEntity(MatrixStack matrixStack, float partialTicks,
		Box entityBox, int color, SignEspStyle style)
	{
		List<Box> singleBoxList = List.of(entityBox);
		renderEntityGroup(matrixStack, partialTicks, singleBoxList, color,
			style);
	}
	
	private void renderEntityGroup(MatrixStack matrixStack, float partialTicks,
		List<Box> entityBoxes, int color, SignEspStyle style)
	{
		if(entityBoxes.isEmpty())
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
			
			RenderUtils.drawSolidBoxes(matrixStack, entityBoxes, quadsColor,
				false);
			RenderUtils.drawOutlinedBoxes(matrixStack, entityBoxes, linesColor,
				false);
		}
		
		if(style.hasLines())
		{
			List<Vec3d> ends =
				entityBoxes.stream().map(Box::getCenter).toList();
			// 追踪线使用配置的透明度
			int tracerColor =
				ColorUtils.combineRgbWithAlpha(color, configuredAlpha);
			RenderUtils.drawTracers(matrixStack, partialTicks, ends,
				tracerColor, false);
		}
	}
}

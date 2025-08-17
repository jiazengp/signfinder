package net.signfinder.models;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class EntitySearchResult
{
	public enum EntityType
	{
		SIGN,
		ITEM_FRAME
	}
	
	private final EntityType entityType;
	private final BlockPos pos;
	private final double distance;
	private final String[] signText; // Only for signs
	private final String itemName; // Only for item frames
	private final String combinedText;
	private final String combinedTextLower;
	private final String matchedText;
	private final String preview;
	private final boolean isLocalData;
	private final long updateTime;
	
	// Private constructor for Builder pattern
	private EntitySearchResult(Builder builder)
	{
		this.entityType = builder.entityType;
		this.pos = builder.pos;
		this.distance = Math.sqrt(pos.getSquaredDistance(builder.playerPos));
		this.signText = builder.signText;
		this.itemName = builder.itemName;
		this.combinedText = builder.combinedText;
		this.combinedTextLower = combinedText.toLowerCase();
		this.matchedText = builder.matchedText;
		this.preview =
			generatePreview(builder.matchedText, builder.previewLength);
		this.isLocalData = builder.isLocalData;
		this.updateTime = builder.updateTime;
	}
	
	// Constructor for signs
	public EntitySearchResult(SignBlockEntity sign, Vec3d playerPos,
		String[] signText, String matchedText, int previewLength)
	{
		this(new Builder().entityType(EntityType.SIGN).pos(sign.getPos())
			.playerPos(playerPos).signText(signText)
			.combinedText(String.join(" ", signText)).matchedText(matchedText)
			.previewLength(previewLength)
			.updateTime(System.currentTimeMillis()));
	}
	
	// Constructor for item frames
	public EntitySearchResult(ItemFrameEntity itemFrame, Vec3d playerPos,
		String itemName, String matchedText, int previewLength)
	{
		this(new Builder().entityType(EntityType.ITEM_FRAME)
			.pos(itemFrame.getBlockPos()).playerPos(playerPos)
			.itemName(itemName).combinedText(itemName).matchedText(matchedText)
			.previewLength(previewLength)
			.updateTime(System.currentTimeMillis()));
	}
	
	// Constructor for local data (assumes sign type)
	public EntitySearchResult(BlockPos pos, Vec3d playerPos, String[] signText,
		String matchedText, int previewLength, long updateTime)
	{
		this(new Builder().entityType(EntityType.SIGN).pos(pos)
			.playerPos(playerPos).signText(signText)
			.combinedText(String.join(" ", signText)).matchedText(matchedText)
			.previewLength(previewLength).isLocalData(true)
			.updateTime(updateTime));
	}
	
	// Builder class for reducing constructor complexity
	private static class Builder
	{
		private EntityType entityType;
		private BlockPos pos;
		private Vec3d playerPos;
		private String[] signText;
		private String itemName;
		private String combinedText;
		private String matchedText;
		private int previewLength;
		private boolean isLocalData = false;
		private long updateTime = System.currentTimeMillis();
		
		public Builder entityType(EntityType entityType)
		{
			this.entityType = entityType;
			return this;
		}
		
		public Builder pos(BlockPos pos)
		{
			this.pos = pos;
			return this;
		}
		
		public Builder playerPos(Vec3d playerPos)
		{
			this.playerPos = playerPos;
			return this;
		}
		
		public Builder signText(String[] signText)
		{
			this.signText = signText;
			return this;
		}
		
		public Builder itemName(String itemName)
		{
			this.itemName = itemName;
			return this;
		}
		
		public Builder combinedText(String combinedText)
		{
			this.combinedText = combinedText;
			return this;
		}
		
		public Builder matchedText(String matchedText)
		{
			this.matchedText = matchedText;
			return this;
		}
		
		public Builder previewLength(int previewLength)
		{
			this.previewLength = previewLength;
			return this;
		}
		
		public Builder isLocalData(boolean isLocalData)
		{
			this.isLocalData = isLocalData;
			return this;
		}
		
		public Builder updateTime(long updateTime)
		{
			this.updateTime = updateTime;
			return this;
		}
	}
	
	private String generatePreview(String matchedText, int previewLength)
	{
		if(combinedText.length() <= previewLength)
			return combinedText;
		
		String lowerMatchedText = matchedText.toLowerCase();
		int matchIndex = combinedTextLower.indexOf(lowerMatchedText);
		
		if(matchIndex == -1)
		{
			return combinedText.substring(0, previewLength) + "...";
		}
		
		StringBuilder preview =
			getPreviewStringBuilder(matchedText, previewLength, matchIndex);
		
		return preview.toString();
	}
	
	private @NotNull StringBuilder getPreviewStringBuilder(String matchedText,
		int previewLength, int matchIndex)
	{
		int matchLength = matchedText.length();
		int contextLength = Math.max(0, (previewLength - matchLength) / 2);
		
		int start = Math.max(0, matchIndex - contextLength);
		int end = Math.min(combinedText.length(), start + previewLength);
		
		if(end - start < previewLength)
		{
			start = Math.max(0, end - previewLength);
		}
		
		StringBuilder preview = new StringBuilder(previewLength + 6);
		
		if(start > 0)
			preview.append("...");
		
		preview.append(combinedText, start, end);
		
		if(end < combinedText.length())
			preview.append("...");
		return preview;
	}
	
	public EntityType getEntityType()
	{
		return entityType;
	}
	
	public BlockPos getPos()
	{
		return pos;
	}
	
	public double getDistance()
	{
		return distance;
	}
	
	public String[] getSignText()
	{
		return signText;
	}
	
	public String getItemName()
	{
		return itemName;
	}
	
	public String getMatchedText()
	{
		return matchedText;
	}
	
	public boolean isLocalData()
	{
		return isLocalData;
	}
	
	public long getUpdateTime()
	{
		return updateTime;
	}
	
	/**
	 * Gets display text - returns sign text for signs, item name as array for
	 * item frames.
	 */
	public String[] getDisplayText()
	{
		if(entityType == EntityType.SIGN)
		{
			return signText;
		}else
		{
			return new String[]{itemName};
		}
	}
	
	public Text getFormattedResult(int index)
	{
		String typePrefix =
			entityType == EntityType.SIGN ? "Sign" : "Item Frame";
		
		if(isLocalData)
		{
			MutableText result = Text.literal(String.format("%d. ", index));
			
			result.append(Text.literal(typePrefix));
			
			result.append(Text.literal(" "));
			
			MutableText localTag = Text.translatable("signfinder.label.local")
				.styled(style -> style
					.withColor(net.minecraft.util.Formatting.DARK_GRAY)
					.withItalic(true)
					.withHoverEvent(new net.minecraft.text.HoverEvent.ShowText(
						createLocalDataTooltip())));
			
			result.append(localTag);
			
			result.append(Text.literal(String.format(" [%.1fm] ", distance)));
			result.append(Text.literal(formatPosition()));
			result.append(Text.literal(" - "));
			result.append(Text.literal(preview));
			
			return result;
		}else
		{
			return Text.literal(String.format("%d. %s [%.1fm] %s - %s", index,
				typePrefix, distance, formatPosition(), preview));
		}
	}
	
	private MutableText createLocalDataTooltip()
	{
		MutableText tooltip = Text.literal("");
		
		tooltip.append(Text.translatable("signfinder.tooltip.local_data_title")
			.styled(style -> style.withColor(net.minecraft.util.Formatting.GOLD)
				.withBold(true)));
		
		tooltip.append(Text.literal("\n"));
		
		tooltip.append(
			Text.translatable("signfinder.tooltip.local_data_desc").styled(
				style -> style.withColor(net.minecraft.util.Formatting.GRAY)));
		return tooltip;
	}
	
	private String formatPosition()
	{
		return String.format("(%d, %d, %d)", pos.getX(), pos.getY(),
			pos.getZ());
	}
}

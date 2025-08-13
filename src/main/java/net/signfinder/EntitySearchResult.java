package net.signfinder;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
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
	}
	
	// Constructor for signs
	public EntitySearchResult(SignBlockEntity sign, Vec3d playerPos,
		String[] signText, String matchedText, int previewLength)
	{
		this(new Builder().entityType(EntityType.SIGN).pos(sign.getPos())
			.playerPos(playerPos).signText(signText)
			.combinedText(String.join(" ", signText)).matchedText(matchedText)
			.previewLength(previewLength));
	}
	
	// Constructor for item frames
	public EntitySearchResult(ItemFrameEntity itemFrame, Vec3d playerPos,
		String itemName, String matchedText, int previewLength)
	{
		this(new Builder().entityType(EntityType.ITEM_FRAME)
			.pos(itemFrame.getBlockPos()).playerPos(playerPos)
			.itemName(itemName).combinedText(itemName).matchedText(matchedText)
			.previewLength(previewLength));
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
	
	public Text getFormattedResult(int index)
	{
		String typePrefix =
			entityType == EntityType.SIGN ? "Sign" : "Item Frame";
		return Text.literal(String.format("%d. %s [%.1fm] %s - %s", index,
			typePrefix, distance, formatPosition(), preview));
	}
	
	private String formatPosition()
	{
		return String.format("(%d, %d, %d)", pos.getX(), pos.getY(),
			pos.getZ());
	}
}

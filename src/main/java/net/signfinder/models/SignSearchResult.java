package net.signfinder.models;


import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SignSearchResult
{
	private final BlockPos pos;
	private final double distance;
	private final String[] signText;
	private final String combinedText;
	private final String combinedTextLower;
	private final String matchedText;
	private final String preview;
	private final long updateTime;
	
	public SignSearchResult(BlockPos pos, Vec3 playerPos, String[] signText,
                            String matchedText, int previewLength)
	{
		this.pos = pos;
		this.distance = Math.sqrt(pos.distToCenterSqr(playerPos));
		this.signText = signText;
		this.combinedText = String.join(" ", signText); // 缓存组合文本
		this.combinedTextLower = combinedText.toLowerCase(); // 缓存小写版本
		this.matchedText = matchedText;
		this.preview = generatePreview(matchedText, previewLength);
		this.updateTime = System.currentTimeMillis();
	}
	
	public SignSearchResult(BlockPos pos, Vec3 playerPos, String[] signText,
                            String matchedText, int previewLength, long updateTime)
	{
		this.pos = pos;
		this.distance = Math.sqrt(pos.distToCenterSqr(playerPos));
		this.signText = signText;
		this.combinedText = String.join(" ", signText); // 缓存组合文本
		this.combinedTextLower = combinedText.toLowerCase(); // 缓存小写版本
		this.matchedText = matchedText;
		this.preview = generatePreview(matchedText, previewLength);
		this.updateTime = updateTime;
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
		
		StringBuilder preview = new StringBuilder(previewLength + 6); // 预留"..."空间
		
		if(start > 0)
			preview.append("...");
		
		preview.append(combinedText, start, end);
		
		if(end < combinedText.length())
			preview.append("...");
		return preview;
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
	
	public String getMatchedText()
	{
		return matchedText;
	}
	
	public long getUpdateTime()
	{
		return updateTime;
	}

	private String formatPosition()
	{
		return String.format("(%d, %d, %d)", pos.getX(), pos.getY(),
			pos.getZ());
	}
}

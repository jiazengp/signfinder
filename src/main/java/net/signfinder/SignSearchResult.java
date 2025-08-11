package net.signfinder;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class SignSearchResult
{
	private final BlockPos pos;
	private final double distance;
	private final String[] signText;
	private final String combinedText; // 缓存组合后的文本
	private final String combinedTextLower; // 缓存小写版本
	private final String matchedText;
	private final String preview;
	
	public SignSearchResult(BlockPos pos, Vec3d playerPos, String[] signText,
		String matchedText, int previewLength)
	{
		this.pos = pos;
		this.distance = Math.sqrt(pos.getSquaredDistance(playerPos));
		this.signText = signText;
		this.combinedText = String.join(" ", signText); // 缓存组合文本
		this.combinedTextLower = combinedText.toLowerCase(); // 缓存小写版本
		this.matchedText = matchedText;
		this.preview = generatePreview(matchedText, previewLength);
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
	
	public Text getFormattedResult(int index)
	{
		return Text.literal(String.format("%d. [%.1fm] %s - %s", index,
			distance, formatPosition(), preview));
	}
	
	private String formatPosition()
	{
		return String.format("(%d, %d, %d)", pos.getX(), pos.getY(),
			pos.getZ());
	}
}

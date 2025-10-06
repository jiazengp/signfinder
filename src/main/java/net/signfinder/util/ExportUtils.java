package net.signfinder.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.signfinder.core.SignExportFormat;
import net.signfinder.models.EntitySearchResult;
import net.signfinder.models.SignSearchResult;

public enum ExportUtils
{
	INSTANCE;
	
	public static final String DOWNLOADS_FOLDER_NAME = "downloads";
	public static final String EXPORT_FILE_PREFIX = "signfinder_export_";
	
	private static final DateTimeFormatter TIMESTAMP_FORMAT =
		DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
	private static final int DEFAULT_PREVIEW_LENGTH = 20;
	private static final double DISTANCE_PRECISION_MULTIPLIER = 10.0;
	private static final int EXPORT_COLUMN_COUNT = 11;
	private static final int MATCHED_TEXT_COLUMN_INDEX = 10;
	
	private static Path getDownloadsPath()
	{
		return FabricLoader.getInstance().getGameDir()
			.resolve(DOWNLOADS_FOLDER_NAME);
	}
	
	// Export method for EntitySearchResult - converts to SignSearchResult
	// format
	public boolean exportEntitySearchResult(List<EntitySearchResult> results,
		String searchQuery, SignExportFormat exportFormat)
	{
		List<SignSearchResult> convertedResults = new ArrayList<>();
		for(EntitySearchResult entityResult : results)
		{
			SignSearchResult converted =
				convertToSignSearchResult(entityResult);
			convertedResults.add(converted);
		}
		return exportSignSearchResult(convertedResults, searchQuery,
			exportFormat);
	}
	
	// Convert EntitySearchResult to SignSearchResult for export compatibility
	private SignSearchResult convertToSignSearchResult(
		EntitySearchResult entityResult)
	{
		if(entityResult.getEntityType() == EntitySearchResult.EntityType.SIGN)
		{
			// For signs, use the original sign text
			if(MinecraftClient.getInstance().player != null)
			{
				return new SignSearchResult(entityResult.getPos(),
					MinecraftClient.getInstance().player.getEntityPos(),
					entityResult.getSignText(), entityResult.getMatchedText(),
					DEFAULT_PREVIEW_LENGTH);
			}
		}else
		{
			// For item frames, create minimal text array with item name only
			String[] itemFrameText = {entityResult.getItemName(), "", "", ""};
			
			if(MinecraftClient.getInstance().player != null)
			{
				return new SignSearchResult(entityResult.getPos(),
					MinecraftClient.getInstance().player.getEntityPos(),
					itemFrameText, entityResult.getMatchedText(),
					DEFAULT_PREVIEW_LENGTH);
			}
		}
		return null;
	}
	
	public boolean exportSignSearchResult(List<SignSearchResult> results,
		String searchQuery, SignExportFormat exportFormat)
	{
		MinecraftClient mc = MinecraftClient.getInstance();
		if(mc.player == null)
			return false;
		
		if(results.isEmpty())
		{
			mc.player.sendMessage(
				Text.translatable("signfinder.export.no_results"), false);
			return false;
		}
		
		try
		{
			ExportContext context =
				createExportContext(searchQuery, exportFormat);
			Path filePath = createExportFile(context);
			
			if(exportFormat.isJsonFormat())
			{
				exportToJsonFile(results, context, filePath);
			}else
			{
				exportToTextFile(results, context, filePath);
			}
			
			sendExportSuccessMessages(mc, results.size(), context.fileName,
				filePath);
			return true;
		}catch(Exception e)
		{
			mc.player.sendMessage(
				Text.translatable("signfinder.export.error", e.getMessage()),
				false);
			return false;
		}
	}
	
	private void exportToTextFile(List<SignSearchResult> results,
		ExportContext context, Path filePath) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		
		// Add header information
		sb.append(
			String.format("Search Query: %s | World: %s | Export Time: %s\n\n",
				context.searchQuery, context.worldName,
				context.formattedExportTime));
		
		// Add table headers
		String[] headers = {"Index", "X", "Y", "Z", "Distance", "Line 1",
			"Line 2", "Line 3", "Line 4", "Full Text", "Matched Text"};
		sb.append(String.join("\t", headers)).append("\n");
		
		// Add data rows
		for(int i = 0; i < results.size(); i++)
		{
			SignSearchResult result = results.get(i);
			appendTextRow(sb, result, i + 1);
		}
		
		Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8);
	}
	
	private void exportToJsonFile(List<SignSearchResult> results,
		ExportContext context, Path filePath) throws IOException
	{
		Map<String, Object> exportData =
			Map.of("searchQuery", context.searchQuery, "world",
				context.worldName, "exportTime", context.formattedExportTime,
				"results", createJsonResultList(results));
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonOutput = gson.toJson(exportData);
		
		Files.writeString(filePath, jsonOutput, StandardCharsets.UTF_8);
	}
	
	private static double roundToOneDecimal(double value)
	{
		return Math.round(value * DISTANCE_PRECISION_MULTIPLIER)
			/ DISTANCE_PRECISION_MULTIPLIER;
	}
	
	// Helper classes and methods for optimized export structure
	private record ExportContext(String searchQuery, String worldName,
		String timestamp, String fileName, String formattedExportTime)
	{}
	
	private ExportContext createExportContext(String searchQuery,
		SignExportFormat format)
	{
		LocalDateTime exportTime = LocalDateTime.now();
		String timestamp = exportTime.format(TIMESTAMP_FORMAT);
		String formattedExportTime = exportTime
			.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		String fileName = EXPORT_FILE_PREFIX + timestamp
			+ (format.isJsonFormat() ? ".json" : ".txt");
		
		MinecraftClient mc = MinecraftClient.getInstance();
		String worldName = mc.world != null
			? mc.world.getRegistryKey().getValue().toString() : "Unknown";
		String query = searchQuery != null ? searchQuery : "All Signs";
		
		return new ExportContext(query, worldName, timestamp, fileName,
			formattedExportTime);
	}
	
	private Path createExportFile(ExportContext context) throws IOException
	{
		Path downloadsPath = getDownloadsPath();
		Files.createDirectories(downloadsPath);
		return downloadsPath.resolve(context.fileName);
	}
	
	private void appendTextRow(StringBuilder sb, SignSearchResult result,
		int index)
	{
		String[] signText =
			result.getSignText() != null ? result.getSignText() : new String[0];
		String fullText = String.join(" ", signText);
		String matchedText =
			result.getMatchedText() != null ? result.getMatchedText() : "";
		
		String[] columns = new String[EXPORT_COLUMN_COUNT];
		columns[0] = String.valueOf(index);
		columns[1] = String.valueOf(result.getPos().getX());
		columns[2] = String.valueOf(result.getPos().getY());
		columns[3] = String.valueOf(result.getPos().getZ());
		columns[4] =
			String.format("%.1f", roundToOneDecimal(result.getDistance()));
		
		// Fill sign text columns (5-8)
		for(int j = 0; j < 4; j++)
		{
			columns[5 + j] = j < signText.length ? signText[j] : "";
		}
		columns[9] = fullText;
		columns[MATCHED_TEXT_COLUMN_INDEX] = matchedText;
		
		sb.append(String.join("\t", columns)).append("\n");
	}
	
	private List<Map<String, Object>> createJsonResultList(
		List<SignSearchResult> results)
	{
		List<Map<String, Object>> resultList = new ArrayList<>();
		for(int i = 0; i < results.size(); i++)
		{
			resultList.add(createJsonResultItem(results.get(i), i + 1));
		}
		return resultList;
	}
	
	private Map<String, Object> createJsonResultItem(SignSearchResult result,
		int index)
	{
		String[] signText =
			result.getSignText() != null ? result.getSignText() : new String[0];
		
		return Map.of("index", index, "pos",
			Map.of("x", result.getPos().getX(), "y", result.getPos().getY(),
				"z", result.getPos().getZ()),
			"distance", roundToOneDecimal(result.getDistance()), "signText",
			signText, "fullText", String.join(" ", signText), "matchedText",
			result.getMatchedText() != null ? result.getMatchedText() : "");
	}
	
	private static void sendExportSuccessMessages(MinecraftClient mc,
		int resultCount, String fileName, Path filePath)
	{
		// Success message with count and filename
		Objects.requireNonNull(mc.player).sendMessage(Text.translatable(
			"signfinder.export.success", resultCount, fileName), false);
		
		// Clickable file path message
		MutableText fileLocationMessage =
			Text.translatable("signfinder.export.file_location_prefix")
				.append(createClickableFilePath(filePath));
		mc.player.sendMessage(fileLocationMessage, false);
	}
	
	private static MutableText createClickableFilePath(Path filePath)
	{
		String pathString = filePath.toString();
		
		return Text.literal(pathString)
			.styled(style -> style.withColor(Formatting.AQUA)
				.withUnderline(true)
				.withHoverEvent(new HoverEvent.ShowText(
					Text.translatable("signfinder.tooltip.click_to_open_file")))
				.withClickEvent(new ClickEvent.OpenFile(pathString)));
	}
}

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
import net.minecraft.text.Text;
import net.signfinder.SignExportFormat;
import net.signfinder.SignFinderConstants;
import net.signfinder.SignSearchResult;

public enum ExportUtils
{
	INSTANCE;
	
	private static final DateTimeFormatter TIMESTAMP_FORMAT =
		DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
	
	private static Path getDownloadsPath()
	{
		return FabricLoader.getInstance().getGameDir()
			.resolve(SignFinderConstants.DOWNLOADS_FOLDER_NAME);
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
			
			LocalDateTime exportTime = LocalDateTime.now();
			String timestamp = exportTime.format(TIMESTAMP_FORMAT);
			String formattedExportTime = exportTime
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			
			if(exportFormat.isJsonFormat())
			{
				return exportToJson(results, searchQuery, mc, timestamp,
					formattedExportTime);
			}else
			{
				return exportToText(results, searchQuery, mc, timestamp,
					formattedExportTime);
			}
		}catch(Exception e)
		{
			mc.player.sendMessage(
				Text.translatable("signfinder.export.error", e.getMessage()),
				false);
			return false;
		}
	}
	
	private boolean exportToText(List<SignSearchResult> results,
		String searchQuery, MinecraftClient mc, String timestamp,
		String formattedExportTime) throws IOException
	{
		Path downloadsPath = getDownloadsPath();
		Files.createDirectories(downloadsPath);
		
		String fileName =
			SignFinderConstants.EXPORT_FILE_PREFIX + timestamp + ".txt";
		Path filePath = downloadsPath.resolve(fileName);
		
		StringBuilder sb = new StringBuilder();
		
		String worldName = mc.world != null
			? mc.world.getRegistryKey().getValue().toString() : "Unknown";
		
		String infoText =
			String.format("Search Query: %s | World: %s | Export Time: %s",
				searchQuery != null ? searchQuery : "All Signs", worldName,
				formattedExportTime);
		sb.append(infoText).append("\n\n");
		
		String[] headers = {"Index", "X", "Y", "Z", "Distance", "Line 1",
			"Line 2", "Line 3", "Line 4", "Full Text", "Matched Text"};
		sb.append(String.join("\t", headers)).append("\n");
		
		for(int i = 0; i < results.size(); i++)
		{
			SignSearchResult result = results.get(i);
			String[] signText = result.getSignText() != null
				? result.getSignText() : new String[0];
			String fullText = String.join(" ", signText);
			String matchedText =
				result.getMatchedText() != null ? result.getMatchedText() : "";
			
			String[] columns = new String[11];
			columns[0] = String.valueOf(i + 1); // Index
			columns[1] = String.valueOf(result.getPos().getX());
			columns[2] = String.valueOf(result.getPos().getY());
			columns[3] = String.valueOf(result.getPos().getZ());
			columns[4] =
				String.format("%.1f", roundToOneDecimal(result.getDistance()));
			for(int j = 0; j < 4; j++)
			{
				columns[5 + j] = j < signText.length ? signText[j] : "";
			}
			columns[9] = fullText;
			columns[10] = matchedText;
			
			sb.append(String.join("\t", columns)).append("\n");
		}
		
		Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8);
		
		Objects.requireNonNull(mc.player).sendMessage(Text.translatable(
			"signfinder.export.success", results.size(), fileName), false);
		mc.player.sendMessage(Text.translatable(
			"signfinder.export.file_location", filePath.toString()), false);
		return true;
	}
	
	private boolean exportToJson(List<SignSearchResult> results,
		String searchQuery, MinecraftClient mc, String timestamp,
		String formattedExportTime) throws IOException
	{
		Path downloadsPath = getDownloadsPath();
		Files.createDirectories(downloadsPath);
		
		String fileName =
			SignFinderConstants.EXPORT_FILE_PREFIX + timestamp + ".json";
		Path filePath = downloadsPath.resolve(fileName);
		
		Map<String, Object> exportData = new HashMap<>();
		exportData.put("searchQuery",
			searchQuery != null ? searchQuery : "All Signs");
		exportData.put("world", mc.world != null
			? mc.world.getRegistryKey().getValue().toString() : "Unknown");
		exportData.put("exportTime", formattedExportTime);
		
		ArrayList<Map<String, Object>> resultList = new ArrayList<>();
		for(int i = 0; i < results.size(); i++)
		{
			SignSearchResult r = results.get(i);
			Map<String, Object> item = new HashMap<>();
			item.put("index", i + 1);
			item.put("pos", Map.of("x", r.getPos().getX(), "y",
				r.getPos().getY(), "z", r.getPos().getZ()));
			item.put("distance", roundToOneDecimal(r.getDistance()));
			item.put("signText",
				r.getSignText() != null ? r.getSignText() : new String[0]);
			item.put("fullText", String.join(" ",
				r.getSignText() != null ? r.getSignText() : new String[0]));
			item.put("matchedText",
				r.getMatchedText() != null ? r.getMatchedText() : "");
			resultList.add(item);
		}
		exportData.put("results", resultList);
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonOutput = gson.toJson(exportData);
		
		Files.writeString(filePath, jsonOutput, StandardCharsets.UTF_8);
		
		Objects.requireNonNull(mc.player).sendMessage(Text.translatable(
			"signfinder.export.success", results.size(), fileName), false);
		mc.player.sendMessage(Text.translatable(
			"signfinder.export.file_location", filePath.toString()), false);
		return true;
	}
	
	private static double roundToOneDecimal(double value)
	{
		return Math.round(value * 10.0) / 10.0;
	}
}

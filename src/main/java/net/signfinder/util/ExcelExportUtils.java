package net.signfinder.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.signfinder.SignFinderConstants;
import net.signfinder.SignSearchResult;

public enum ExcelExportUtils
{
	INSTANCE;
	
	private static final DateTimeFormatter TIMESTAMP_FORMAT =
		DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
	
	private static Path getDownloadsPath()
	{
		return FabricLoader.getInstance().getGameDir()
			.resolve(SignFinderConstants.DOWNLOADS_FOLDER_NAME);
	}
	
	public boolean exportToExcel(List<SignSearchResult> results,
		String searchQuery)
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
			// Ensure downloads directory exists
			Path downloadsPath = getDownloadsPath();
			Files.createDirectories(downloadsPath);
			
			String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
			String fileName = SignFinderConstants.EXPORT_FILE_PREFIX + timestamp
				+ SignFinderConstants.EXPORT_FILE_EXTENSION;
			Path filePath = downloadsPath.resolve(fileName);
			
			try(Workbook workbook = new XSSFWorkbook())
			{
				Sheet sheet =
					workbook.createSheet(SignFinderConstants.EXCEL_SHEET_NAME);
				
				createHeaderRow(workbook, sheet, searchQuery);
				populateDataRows(sheet, results);
				autoSizeColumns(sheet);
				
				try(FileOutputStream fileOut =
					new FileOutputStream(filePath.toFile()))
				{
					workbook.write(fileOut);
				}
			}
			
			mc.player.sendMessage(Text.translatable("signfinder.export.success",
				results.size(), fileName), false);
			mc.player.sendMessage(Text.translatable(
				"signfinder.export.file_location", filePath.toString()), false);
			return true;
		}catch(IOException e)
		{
			mc.player.sendMessage(
				Text.translatable("signfinder.export.error", e.getMessage()),
				false);
			return false;
		}
	}
	
	private void createHeaderRow(Workbook workbook, Sheet sheet,
		String searchQuery)
	{
		// Info row
		Row infoRow = sheet.createRow(0);
		Cell infoCell = infoRow.createCell(0);
		String worldName = MinecraftClient.getInstance().world != null
			? MinecraftClient.getInstance().world.getRegistryKey().getValue()
				.toString()
			: "Unknown";
		String infoText =
			String.format("Search Query: %s | World: %s | Export Time: %s",
				searchQuery != null ? searchQuery : "All Signs", worldName,
				LocalDateTime.now().format(
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		infoCell.setCellValue(infoText);
		
		// Header row
		Row headerRow =
			sheet.createRow(SignFinderConstants.EXCEL_HEADER_ROW_INDEX);
		String[] headers = {"Index", "X", "Y", "Z", "Distance", "Line 1",
			"Line 2", "Line 3", "Line 4", "Full Text", "Matched Text"};
		
		CellStyle headerStyle = createHeaderStyle(workbook);
		
		for(int i = 0; i < headers.length; i++)
		{
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}
	}
	
	private void populateDataRows(Sheet sheet, List<SignSearchResult> results)
	{
		CellStyle dataStyle = createDataStyle(sheet.getWorkbook());
		
		for(int i = 0; i < results.size(); i++)
		{
			SignSearchResult result = results.get(i);
			Row row =
				sheet.createRow(i + SignFinderConstants.EXCEL_DATA_START_ROW);
			
			// Index
			Cell indexCell = row.createCell(0);
			indexCell.setCellValue(i + 1);
			indexCell.setCellStyle(dataStyle);
			
			// Position
			Cell xCell = row.createCell(1);
			xCell.setCellValue(result.getPos().getX());
			xCell.setCellStyle(dataStyle);
			
			Cell yCell = row.createCell(2);
			yCell.setCellValue(result.getPos().getY());
			yCell.setCellStyle(dataStyle);
			
			Cell zCell = row.createCell(3);
			zCell.setCellValue(result.getPos().getZ());
			zCell.setCellStyle(dataStyle);
			
			// Distance
			Cell distanceCell = row.createCell(4);
			distanceCell
				.setCellValue(Math.round(result.getDistance() * 10.0) / 10.0);
			distanceCell.setCellStyle(dataStyle);
			
			// Sign text lines
			String[] signText = result.getSignText();
			for(int j = 0; j < 4; j++)
			{
				Cell lineCell = row.createCell(5 + j);
				lineCell.setCellValue(j < signText.length ? signText[j] : "");
				lineCell.setCellStyle(dataStyle);
			}
			
			// Full text
			Cell fullTextCell = row.createCell(9);
			fullTextCell.setCellValue(String.join(" ", signText));
			fullTextCell.setCellStyle(dataStyle);
			
			// Matched text
			Cell matchedTextCell = row.createCell(10);
			matchedTextCell.setCellValue(
				result.getMatchedText() != null ? result.getMatchedText() : "");
			matchedTextCell.setCellStyle(dataStyle);
		}
	}
	
	private CellStyle createHeaderStyle(Workbook workbook)
	{
		CellStyle style = workbook.createCellStyle();
		
		Font font = workbook.createFont();
		font.setBold(true);
		font.setColor(IndexedColors.WHITE.getIndex());
		style.setFont(font);
		
		style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		
		return style;
	}
	
	private CellStyle createDataStyle(Workbook workbook)
	{
		CellStyle style = workbook.createCellStyle();
		
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		
		style.setVerticalAlignment(VerticalAlignment.TOP);
		style.setWrapText(true);
		
		return style;
	}
	
	private void autoSizeColumns(Sheet sheet)
	{
		for(int i = 0; i < 11; i++)
		{
			sheet.autoSizeColumn(i);
			// Set minimum width for text columns
			if(i >= 5)
			{
				int currentWidth = sheet.getColumnWidth(i);
				sheet.setColumnWidth(i, Math.max(currentWidth,
					SignFinderConstants.EXCEL_TEXT_COLUMN_MIN_WIDTH));
			}
		}
	}
}

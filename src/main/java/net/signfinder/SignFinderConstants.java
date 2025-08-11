package net.signfinder;

public class SignFinderConstants
{
	// Export related constants
	public static final String DOWNLOADS_FOLDER_NAME = "downloads";
	public static final String EXPORT_FILE_PREFIX = "signfinder_export_";
	public static final String EXPORT_FILE_EXTENSION = ".xlsx";
	
	// Excel export constants
	public static final String EXCEL_SHEET_NAME = "Sign Search Results";
	public static final int EXCEL_TEXT_COLUMN_MIN_WIDTH = 3000;
	public static final int EXCEL_HEADER_ROW_INDEX = 2;
	public static final int EXCEL_DATA_START_ROW = 3;
	
	private SignFinderConstants()
	{
		// Utility class
	}
}

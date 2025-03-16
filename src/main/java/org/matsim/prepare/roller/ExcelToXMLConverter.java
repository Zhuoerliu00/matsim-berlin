package org.matsim.prepare.roller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class ExcelToXMLConverter {

	public static void main(String[] args) {
		String excelFilePath = "src/main/java/org/matsim/prepare/roller/vehicles_stations.xlsx"; // Path to the Excel file
		String xmlFilePath = "src/main/java/org/matsim/prepare/roller/output.xml"; // Output XML file path

		try {
			Map<String, List<Map<String, String>>> data = readExcelFile(excelFilePath);
			generateXML(data, xmlFilePath);
			System.out.println("XML file successfully generated: " + xmlFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the Excel file and extracts data from "stations" and "vehicles" sheets.
	 */
	private static Map<String, List<Map<String, String>>> readExcelFile(String filePath) throws IOException {
		Map<String, List<Map<String, String>>> data = new HashMap<>();
		try (FileInputStream file = new FileInputStream(new File(filePath));
			 Workbook workbook = new XSSFWorkbook(file)) {

			for (Sheet sheet : workbook) {
				String sheetName = sheet.getSheetName().toLowerCase();
				List<Map<String, String>> sheetData = new ArrayList<>();

				Row headerRow = sheet.getRow(0);
				if (headerRow == null) continue;

				List<String> headers = new ArrayList<>();
				for (Cell cell : headerRow) {
					headers.add(cell.getStringCellValue().trim());
				}

				for (int i = 1; i <= sheet.getLastRowNum(); i++) {
					Row row = sheet.getRow(i);
					if (row == null) continue;

					Map<String, String> rowData = new LinkedHashMap<>();
					for (int j = 0; j < headers.size(); j++) {
						Cell cell = row.getCell(j);
						if (cell != null) {
							rowData.put(headers.get(j), getCellValue(cell));
						}
					}
					sheetData.add(rowData);
				}

				data.put(sheetName, sheetData);
			}
		}
		return data;
	}

	/**
	 * Generates an XML file based on the extracted data with the correct attribute order.
	 */
	private static void generateXML(Map<String, List<Map<String, String>>> data, String outputPath) throws Exception {
		StringBuilder xmlBuilder = new StringBuilder();

		// Start XML
		xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xmlBuilder.append("<!DOCTYPE service SYSTEM \"../dtd/sharing_service_v1.dtd\">\n");
		xmlBuilder.append("<service>\n");

		// Add stations section
		xmlBuilder.append("   <stations>\n");
		if (data.containsKey("stations")) {
			for (Map<String, String> station : data.get("stations")) {
				String id = station.getOrDefault("station id", "");
				String link = station.getOrDefault("link", "");
				String capacity = station.getOrDefault("capacity", "");

				// Construct station element with the correct order
				xmlBuilder.append(String.format("       <station id=\"%s\" link=\"%s\" capacity=\"%s\"/>\n", id, link, capacity));
			}
		}
		xmlBuilder.append("   </stations>\n");

		// Add vehicles section
		xmlBuilder.append("   <vehicles>\n");
		if (data.containsKey("vehicles")) {
			for (Map<String, String> vehicle : data.get("vehicles")) {
				String id = vehicle.getOrDefault("vehicle id", "");
				String startLink = vehicle.getOrDefault("startLink", "");
				String startStation = vehicle.getOrDefault("startStation", "");

				// Construct vehicle element
				xmlBuilder.append(String.format("       <vehicle id=\"%s\" startLink=\"%s\" startStation=\"%s\"/>\n", id, startLink, startStation));
			}
		}
		xmlBuilder.append("   </vehicles>\n");

		// End XML
		xmlBuilder.append("</service>\n");

		// Write to file
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
			writer.write(xmlBuilder.toString());
		}
	}

	/**
	 * Retrieves the string value of an Excel cell.
	 */
	private static String getCellValue(Cell cell) {
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue().trim();
			case NUMERIC:
				return String.valueOf((int) cell.getNumericCellValue());
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			default:
				return "";
		}
	}
}

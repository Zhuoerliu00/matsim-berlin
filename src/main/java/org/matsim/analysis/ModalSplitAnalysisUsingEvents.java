package org.matsim.analysis;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModalSplitAnalysisUsingEvents {

	public static void main(String[] args) {
		String excelFilePath = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\LinksWithinPolygon_area.xlsx";
		String csvFilePath = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\berlin-v6.1-1pct\\berlin-v6.1.output_legs.csv";
		String outputFilePath = "C:\\Users\\cdk\\Desktop\\matsim HA1\\modal_split_results.xlsx";

		try {
			// Step 1: Read Excel file and extract link IDs
			Set<String> linkIds = readExcelFile(excelFilePath);

			// Step 2: Analyze modal split using CSV file
			Map<String, Integer> modalSplit = analyzeModalSplit(csvFilePath, linkIds);

			// Step 3: Calculate modal split percentages
			Map<String, Double> modalSplitPercentages = calculatePercentages(modalSplit);

			// Step 4: Write modal split results to Excel file
			writeResultsToExcel(modalSplitPercentages, outputFilePath);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Set<String> readExcelFile(String filePath) throws IOException {
		Set<String> linkIds = new HashSet<>();
		FileInputStream fis = new FileInputStream(filePath);
		Workbook workbook = new XSSFWorkbook(fis);
		Sheet sheet = workbook.getSheetAt(0);

		for (Row row : sheet) {
			Cell cell = row.getCell(0);
			if (cell != null && cell.getCellType() == CellType.STRING) {
				linkIds.add(cell.getStringCellValue());
			}
		}
		workbook.close();
		fis.close();
		return linkIds;
	}

	private static Map<String, Integer> analyzeModalSplit(String filePath, Set<String> linkIds) throws IOException {
		Map<String, Integer> modalSplit = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line;
		boolean header = true;

		while ((line = br.readLine()) != null) {
			if (header) {
				header = false;
				continue;
			}
			String[] fields = line.split(",");
			if (fields.length < 3) { // Ensure the line has at least 3 fields
				continue;
			}
			String linkId = fields[1]; // Assuming the link ID is in the second column
			String mode = fields[2]; // Assuming the mode is in the third column

			if (linkIds.contains(linkId)) {
				modalSplit.put(mode, modalSplit.getOrDefault(mode, 0) + 1);
			}
		}
		br.close();
		return modalSplit;
	}

	private static Map<String, Double> calculatePercentages(Map<String, Integer> modalSplit) {
		Map<String, Double> modalSplitPercentages = new HashMap<>();
		int total = modalSplit.values().stream().mapToInt(Integer::intValue).sum();

		for (Map.Entry<String, Integer> entry : modalSplit.entrySet()) {
			double percentage = (entry.getValue() * 100.0) / total;
			modalSplitPercentages.put(entry.getKey(), percentage);
		}
		return modalSplitPercentages;
	}

	private static void writeResultsToExcel(Map<String, Double> modalSplitPercentages, String filePath) throws IOException {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Modal Split Results");

		int rowNum = 0;
		Row headerRow = sheet.createRow(rowNum++);
		Cell headerCell1 = headerRow.createCell(0);
		headerCell1.setCellValue("Mode");
		Cell headerCell2 = headerRow.createCell(1);
		headerCell2.setCellValue("Percentage");

		for (Map.Entry<String, Double> entry : modalSplitPercentages.entrySet()) {
			Row row = sheet.createRow(rowNum++);
			Cell cell1 = row.createCell(0);
			cell1.setCellValue(entry.getKey());
			Cell cell2 = row.createCell(1);
			cell2.setCellValue(entry.getValue());
		}

		FileOutputStream fos = new FileOutputStream(filePath);
		workbook.write(fos);
		workbook.close();
		fos.close();
	}
}

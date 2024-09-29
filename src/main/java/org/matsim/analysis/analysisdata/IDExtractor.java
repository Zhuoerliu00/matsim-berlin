package org.matsim.analysis.analysisdata;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

public class IDExtractor {

	public static void main(String[] args) {
		String csvFilePath = "C:\\Users\\cdk\\Desktop\\matsim HA1\\Ergebnisse\\personIDs.csv";
		String excelFilePath = "C:\\Users\\cdk\\Desktop\\matsim HA1\\Ergebnisse\\uniqueIDs.xlsx";

		Set<String> firstColumnIds = new HashSet<>();
		Set<String> secondColumnIds = new HashSet<>();
		Set<String> uniqueIds = new HashSet<>();

		// Read the CSV file
		try (Reader reader = new FileReader(csvFilePath)) {
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(reader);

			for (CSVRecord record : records) {
				String firstColumnId = record.get(0);
				String secondColumnId = record.get(1);

				firstColumnIds.add(firstColumnId);
				secondColumnIds.add(secondColumnId);
			}

			// Find IDs present in the first column but not in the second column
			for (String id : secondColumnIds) {
				if (!firstColumnIds.contains(id)) {
					uniqueIds.add(id);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Write the unique IDs to a new Excel file
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Unique IDs");

			int rowNum = 0;
			for (String id : uniqueIds) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(id);
			}

			try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
				workbook.write(fileOut);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

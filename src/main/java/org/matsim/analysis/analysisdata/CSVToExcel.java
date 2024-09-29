package org.matsim.analysis.analysisdata;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CSVToExcel {
	public static void main(String[] args) {
		String csvFilePath = "C:\\Users\\cdk\\Desktop\\matsim HA1\\berlin-v6.1.output_persons.csv";
		String idFilePath = "C:\\Users\\cdk\\Desktop\\matsim HA1\\personIDs_5.csv";
		String excelFilePath = "C:\\Users\\cdk\\Desktop\\matsim HA1\\filtered_ids.xlsx";

		Set<String> targetIds = new HashSet<>();

		// 读取目标ID文件
		try (CSVReader idReader = new CSVReader(new FileReader(idFilePath))) {
			List<String[]> idData = idReader.readAll();
			for (String[] idRow : idData) {
				targetIds.add(idRow[0]); // 假设ID在第一列
			}
		} catch (IOException | CsvException e) {
			e.printStackTrace();
			return;
		}

		// 读取主CSV文件并筛选目标ID的数据
		try (
			CSVReader csvReader = new CSVReader(new FileReader(csvFilePath));
			Workbook workbook = new XSSFWorkbook();
		) {
			List<String[]> allData = csvReader.readAll();
			String[] header = allData.get(0);

			Sheet sheet = workbook.createSheet("Filtered Data");
			int rowNum = 0;

			// 写入表头
			Row headerRow = sheet.createRow(rowNum++);
			for (int i = 0; i < header.length; i++) {
				headerRow.createCell(i).setCellValue(header[i]);
			}

			// 写入匹配的行
			for (String[] rowData : allData) {
				if (targetIds.contains(rowData[0])) { // 假设ID在第一列
					Row row = sheet.createRow(rowNum++);
					for (int i = 0; i < rowData.length; i++) {
						row.createCell(i).setCellValue(rowData[i]);
					}
				}
			}

			// 写入Excel文件
			try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
				workbook.write(fileOut);
			}

			System.out.println("Data successfully written to Excel file.");

		} catch (IOException | CsvException e) {
			e.printStackTrace();
		}
	}
}

package org.matsim.analysis;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.core.utils.io.IOUtils;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class LegAnalyzer {

	public static void main(String[] args) {
		String inputFile = "F:\\Matsim\\output\\gartenfeld-v6.4.full-roller-10pct-100-v2\\gartenfeld-v6.4.full-roller-10pct-v2.output_legs.csv.gz";
		String outputExcel = "D:\\2024SS\\Masterarbeit\\leg_analysis_result.xlsx";

		Map<String, Set<String>> personModes = new HashMap<>();

		try (BufferedReader reader = IOUtils.getBufferedReader(inputFile)) {
			String header = reader.readLine(); // 跳过表头
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) continue;
				String[] tokens = line.split(";");
				if (tokens.length < 7) {
					System.err.println("跳过无效行: " + line);
					continue;
				}

				String personId = tokens[0].replaceAll("\"", "").trim();
				String mode = tokens[6].replaceAll("\"", "").trim(); // mode 在第7列（index 6）
				String station = tokens[11].replaceAll("\"", "").trim();

				personModes.computeIfAbsent(personId, k -> new HashSet<>()).add(mode);
				personModes.computeIfAbsent(personId, k -> new HashSet<>()).add(station);
			}

			// 过滤出同时使用 "pt" 和 "sharing_roller" 的 agent
			List<String> matchingAgents = personModes.entrySet().stream()
				.filter(e -> e.getValue().contains("pt") && e.getValue().contains("sharing_roller") && e.getValue().contains("338527879#0"))
				.map(Map.Entry::getKey)
				.sorted()
				.toList();

			// 写入 Excel 文件
			writeToExcel(matchingAgents, outputExcel);
			System.out.println("分析完成，共找到 " + matchingAgents.size() + " 个匹配的 agent，结果已写入: " + outputExcel);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeToExcel(List<String> agentIds, String filePath) {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Shared PT & Roller");

			// 表头
			Row headerRow = sheet.createRow(0);
			Cell headerCell = headerRow.createCell(0);
			headerCell.setCellValue("Agent ID");

			// 写入 agent ID
			for (int i = 0; i < agentIds.size(); i++) {
				Row row = sheet.createRow(i + 1);
				Cell cell = row.createCell(0);
				cell.setCellValue(agentIds.get(i));
			}

			// 自动列宽
			sheet.autoSizeColumn(0);

			// 写入文件
			try (FileOutputStream fos = new FileOutputStream(filePath)) {
				workbook.write(fos);
			}
		} catch (IOException e) {
			System.err.println("写入 Excel 文件失败: " + filePath);
			e.printStackTrace();
		}
	}
}

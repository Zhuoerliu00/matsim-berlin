package org.matsim.analysis;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.core.utils.io.IOUtils;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * This class analyzes a MATSim leg-level CSV output file to identify agents (persons)
 * who have used a combination of:
 * - public transport ("pt")
 * - shared micromobility mode ("sharing_roller")
 * - and passed through a specific network link (e.g., "338527879#0").
 *
 * Specifically, it:
 * 1. Parses a compressed .csv.gz leg file exported from a MATSim simulation.
 * 2. Tracks which modes and link IDs each agent has used.
 * 3. Filters for agents whose trips involve all three: pt, sharing_roller, and the target link.
 * 4. Writes the matching agent IDs to an Excel file.
 *
 */

public class LegAnalyzer {

	public static void main(String[] args) {
		String inputFile = "F:\\Matsim\\output\\gartenfeld-v6.4.full-roller-10pct-100-v2\\gartenfeld-v6.4.full-roller-10pct-v2.output_legs.csv.gz";
		String outputExcel = "D:\\2024SS\\Masterarbeit\\leg_analysis_result.xlsx";

		Map<String, Set<String>> personModes = new HashMap<>();

		try (BufferedReader reader = IOUtils.getBufferedReader(inputFile)) {
			String header = reader.readLine();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) continue;
				String[] tokens = line.split(";");
				if (tokens.length < 7) {
					System.err.println("跳过无效行: " + line);
					continue;
				}

				String personId = tokens[0].replaceAll("\"", "").trim();
				String mode = tokens[6].replaceAll("\"", "").trim();
				String station = tokens[11].replaceAll("\"", "").trim();

				personModes.computeIfAbsent(personId, k -> new HashSet<>()).add(mode);
				personModes.computeIfAbsent(personId, k -> new HashSet<>()).add(station);
			}


			List<String> matchingAgents = personModes.entrySet().stream()
				.filter(e -> e.getValue().contains("pt") && e.getValue().contains("sharing_roller") && e.getValue().contains("338527879#0"))
				.map(Map.Entry::getKey)
				.sorted()
				.toList();


			writeToExcel(matchingAgents, outputExcel);
			System.out.println(matchingAgents.size() + outputExcel);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeToExcel(List<String> agentIds, String filePath) {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Shared PT & Roller");

			Row headerRow = sheet.createRow(0);
			Cell headerCell = headerRow.createCell(0);
			headerCell.setCellValue("Agent ID");

			for (int i = 0; i < agentIds.size(); i++) {
				Row row = sheet.createRow(i + 1);
				Cell cell = row.createCell(0);
				cell.setCellValue(agentIds.get(i));
			}

			sheet.autoSizeColumn(0);

			try (FileOutputStream fos = new FileOutputStream(filePath)) {
				workbook.write(fos);
			}
		} catch (IOException e) {
			System.err.println("failrf: " + filePath);
			e.printStackTrace();
		}
	}
}

package org.matsim.analysis;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.core.utils.io.IOUtils;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * This class processes a MATSim-generated output_legs CSV file to identify agents (persons)
 * whose IDs start with "dng" and determine how many of them used the "sharing_roller" mode
 * (e.g. a shared e-scooter service).
 *
 * The program:
 * 1. Parses the input compressed leg-level CSV file.
 * 2. Filters all agents whose ID starts with "dng".
 * 3. Checks which of these agents have used the "sharing_roller" mode.
 * 4. Exports the list of those agents to an Excel (.xlsx) file for further analysis.
 *
 * This is useful for evaluating adoption rates of shared micromobility services within a
 * synthetic agent subgroup in MATSim simulation results.
 */

public class Gartenfelduser {

	public static void main(String[] args) {
		String inputFile = "F:\\Matsim\\gartenfeld-v6.4.full-roller-10pct-v1-500.300.legs.csv.gz";
		String outputExcel = "D:\\2024SS\\Masterarbeit\\output_analysis\\Gartenfelduser_result_v1.xlsx";

		Map<String, Set<String>> personModes = new HashMap<>();

		try (BufferedReader reader = IOUtils.getBufferedReader(inputFile)) {
			String header = reader.readLine();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) continue;
				String[] tokens = line.split(";");
				if (tokens.length < 7) {
					System.err.println("Skipping invalid line: " + line);
					continue;
				}

				String personId = tokens[0].replaceAll("\"", "").trim();
				String mode = tokens[6].replaceAll("\"", "").trim();

				personModes.computeIfAbsent(personId, k -> new HashSet<>()).add(mode);
			}

			// 1. Filter all agents with ID starting with "dng"
			List<String> dngAgents = new ArrayList<>();
			List<String> dngUsedSharingRoller = new ArrayList<>();

			for (Map.Entry<String, Set<String>> entry : personModes.entrySet()) {
				String personId = entry.getKey();
				Set<String> modes = entry.getValue();

				if (personId.startsWith("dng")) {
					dngAgents.add(personId);
					if (modes.contains("sharing_roller")) {
						dngUsedSharingRoller.add(personId);
					}
				}
			}

			// Print summary
			System.out.println("Total number of agents with ID starting with 'dng': " + dngAgents.size());
			System.out.println("Number of 'dng' agents who used 'sharing_roller': " + dngUsedSharingRoller.size());

			// Export matching agents to Excel
			writeToExcel(dngUsedSharingRoller, outputExcel);
			System.out.println("Result exported to: " + outputExcel);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeToExcel(List<String> agentIds, String filePath) {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("DNG_SharingRoller");

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
			System.err.println("Failed to export to: " + filePath);
			e.printStackTrace();
		}
	}
}

package org.matsim.analysis;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * This program compares the main transport mode used for each trip by "dng" agents
 * between two MATSim simulation outputs (e.g., base vs. with shared e-scooters).
 *
 * It reads two `output_trips.csv.gz` files, extracts the main mode for each trip,
 * and matches trips by agent ID and trip ID.
 *
 * The result is exported to an Excel file showing, for each trip:
 * - Person ID
 * - Trip ID
 * - Mode in the base scenario
 * - Mode in the modified (e.g., shared mobility) scenario
 *
 * This allows for detailed comparison of mode shifts at the trip level.
 */

public class CompareTripModes {

	public static void main(String[] args) {
		String baseFile = "F:\\Matsim\\gartenfeld-v6.4.full-base-10pct.300.trips.csv.gz";
		String v1File = "F:\\Matsim\\gartenfeld-v6.4.full-roller-10pct-v2-500.300.trips.csv.gz";
		String outputExcel = "D:/2024SS/Masterarbeit/output_analysis/trip_mode_comparison_dng_v2.xlsx";

		Map<String, String> baseModes = readTripModes(baseFile);
		Map<String, String> v1Modes = readTripModes(v1File);

		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("TripModeComparison");

			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("Person ID");
			header.createCell(1).setCellValue("Trip ID");
			header.createCell(2).setCellValue("Base Mode");
			header.createCell(3).setCellValue("V1 Mode");

			int rowIdx = 1;
			Set<String> allKeys = new HashSet<>();
			allKeys.addAll(baseModes.keySet());
			allKeys.addAll(v1Modes.keySet());

			for (String key : allKeys) {
				String[] parts = key.split("_trip_");
				String personId = parts[0];
				String tripId = parts.length > 1 ? parts[1] : "";

				Row row = sheet.createRow(rowIdx++);
				row.createCell(0).setCellValue(personId);
				row.createCell(1).setCellValue(tripId);
				row.createCell(2).setCellValue(baseModes.getOrDefault(key, ""));
				row.createCell(3).setCellValue(v1Modes.getOrDefault(key, ""));
			}

			for (int i = 0; i < 4; i++) {
				sheet.autoSizeColumn(i);
			}

			try (FileOutputStream fos = new FileOutputStream(outputExcel)) {
				workbook.write(fos);
				System.out.println("Comparison exported to: " + outputExcel);
			}

		} catch (IOException e) {
			System.err.println("Excel export failed.");
			e.printStackTrace();
		}
	}

	/**
	 * Reads trip modes from a compressed CSV file and returns a map with key: "person_tripId" and value: mode.
	 */
	private static Map<String, String> readTripModes(String filePath) {
		Map<String, String> tripModes = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(new GZIPInputStream(new FileInputStream(filePath)), StandardCharsets.UTF_8))) {

			String header = reader.readLine();
			if (header == null) return tripModes;

			String[] columns = header.split(";");
			int personIdIdx = -1, tripIdIdx = -1, modeIdx = -1;

			for (int i = 0; i < columns.length; i++) {
				if (columns[i].equalsIgnoreCase("person")) personIdIdx = i;
				if (columns[i].equalsIgnoreCase("trip_id")) tripIdIdx = i;
				if (columns[i].equalsIgnoreCase("main_mode")) modeIdx = i;
			}

			if (personIdIdx == -1 || tripIdIdx == -1 || modeIdx == -1) {
				System.err.println("Required columns not found.");
				return tripModes;
			}

			String line;
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split(";");
				if (tokens.length <= Math.max(personIdIdx, Math.max(tripIdIdx, modeIdx))) continue;

				String personId = tokens[personIdIdx];
				if (!personId.startsWith("dng")) continue;

				String tripId = tokens[tripIdIdx];
				String mode = tokens[modeIdx];

				tripModes.put(personId + "_trip_" + tripId, mode);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return tripModes;
	}
}

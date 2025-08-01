package org.matsim.analysis;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 *
 * This analysis script reads a MATSim events file and extracts all person departure events
 * associated with agents whose IDs start with "dng". It counts the number of legs (departures)
 * per transport mode used by these agents and calculates the modal share.
 *
 * The results are printed to the console and exported to an Excel file.
 *
 * Purpose:
 * - Analyze mode usage (e.g., pt, car, sharing_roller) of a specific subset of agents (e.g., "dng" group)
 * - Useful for evaluating scenario outcomes and agent behavior in MATSim simulations
 *
 * Input:
 * - A gzipped MATSim events file (.xml.gz)
 *
 * Output:
 * - An Excel (.xlsx) file with modal counts and shares
 *
 */
public class LegAnalyzerFromEvents {

	public static void main(String[] args) {
		String eventsFile = "F:\\Matsim\\output\\gartenfeld-v6.4.full-roller-10pct-100-v1\\gartenfeld-v6.4.full-roller-10pct-v1.output_events.xml.gz";
		String outputExcel = "D:\\2024SS\\Masterarbeit\\output_analysis\\Gartenfeld_mode_share_dng.xlsx";

		// Map to count modes of all dng agents
		Map<String, Integer> modeCount = new HashMap<>();
		int totalLegs = 0;

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new PersonDepartureEventHandler() {
			@Override
			public void handleEvent(PersonDepartureEvent event) {
				String personId = event.getPersonId().toString();
				if (personId.startsWith("dng")) {
					String mode = event.getLegMode();
					modeCount.put(mode, modeCount.getOrDefault(mode, 0) + 1);
				}
			}

			@Override
			public void reset(int iteration) {
				modeCount.clear();
			}
		});

		new MatsimEventsReader(events).readFile(eventsFile);

		for (int count : modeCount.values()) {
			totalLegs += count;
		}

		System.out.println("Total legs from agents starting with 'dng': " + totalLegs);
		for (Map.Entry<String, Integer> entry : modeCount.entrySet()) {
			double share = (double) entry.getValue() / totalLegs * 100;
			System.out.printf("Mode: %s, Count: %d, Share: %.2f%%%n", entry.getKey(), entry.getValue(), share);
		}

		writeModeShareToExcel(modeCount, totalLegs, outputExcel);
		System.out.println("Mode share exported to: " + outputExcel);
	}

	private static void writeModeShareToExcel(Map<String, Integer> modeCount, int total, String filePath) {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("DNG_Mode_Share");

			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("Mode");
			header.createCell(1).setCellValue("Count");
			header.createCell(2).setCellValue("Percentage");

			int rowIdx = 1;
			for (Map.Entry<String, Integer> entry : modeCount.entrySet()) {
				Row row = sheet.createRow(rowIdx++);
				row.createCell(0).setCellValue(entry.getKey());
				row.createCell(1).setCellValue(entry.getValue());
				row.createCell(2).setCellValue((double) entry.getValue() / total);
			}

			for (int i = 0; i < 3; i++) {
				sheet.autoSizeColumn(i);
			}

			try (FileOutputStream fos = new FileOutputStream(filePath)) {
				workbook.write(fos);
			}
		} catch (IOException e) {
			System.err.println("Failed to write Excel file.");
			e.printStackTrace();
		}
	}
}

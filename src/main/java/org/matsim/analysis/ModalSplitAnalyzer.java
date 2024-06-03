package org.matsim.analysis;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ModalSplitAnalyzer {
	public static void main(String[] args) throws IOException {
		// 读取Excel文件中的道路ID
		Set<String> regionLinkIds = new HashSet<>();
		String excelFilePath = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\LinksWithinPolygon_area.xlsx";
		FileInputStream file = new FileInputStream(excelFilePath);
		Workbook workbook = new XSSFWorkbook(file);
		Sheet sheet = workbook.getSheetAt(0);
		Iterator<Row> rowIterator = sheet.iterator();

		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			Cell cell = row.getCell(0);
			if (cell != null) {
				regionLinkIds.add(cell.getStringCellValue());
			}
		}
		workbook.close();
		file.close();

		// 加载网络文件
		String networkFile = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\berlin-v6.1-1pct\\berlin-v6.1.output_network.xml.gz";
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		// 创建事件管理器
		EventsManager eventsManager = new EventsManagerImpl();

		// 创建并注册事件处理器
		ModalSplitEventHandler modalSplitEventHandler = new ModalSplitEventHandler(regionLinkIds);
		eventsManager.addHandler(modalSplitEventHandler);

		// 读取事件文件
		String eventsFile = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\berlin-v6.1-1pct\\berlin-v6.1.output_events.xml.gz";
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
		reader.readFile(eventsFile);

		// 输出结果到Excel文件
		modalSplitEventHandler.writeModalSplitToExcel("C:\\Users\\cdk\\Desktop\\matsim HA1\\modal_split_output.xlsx");
	}

	static class ModalSplitEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {
		private final Set<String> regionLinkIds;
		private final Map<String, Integer> modeCounts = new HashMap<>();

		public ModalSplitEventHandler(Set<String> regionLinkIds) {
			this.regionLinkIds = regionLinkIds;
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (regionLinkIds.contains(event.getLinkId().toString())) {
				modeCounts.merge(event.getLegMode(), 1, Integer::sum);
			}
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			// 可选择在到达事件中也进行处理
		}

		@Override
		public void reset(int iteration) {
			modeCounts.clear();
		}

		public void writeModalSplitToExcel(String outputPath) throws IOException {
			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("Modal Split");

			int rowNum = 0;
			Row headerRow = sheet.createRow(rowNum++);
			headerRow.createCell(0).setCellValue("Mode");
			headerRow.createCell(1).setCellValue("Split (%)");

			int totalTrips = modeCounts.values().stream().mapToInt(Integer::intValue).sum();
			for (Map.Entry<String, Integer> entry : modeCounts.entrySet()) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(entry.getKey());
				double split = (double) entry.getValue() / totalTrips * 100;
				row.createCell(1).setCellValue(split);
			}

			FileOutputStream fileOut = new FileOutputStream(outputPath);
			workbook.write(fileOut);
			fileOut.close();
			workbook.close();
		}
	}
}

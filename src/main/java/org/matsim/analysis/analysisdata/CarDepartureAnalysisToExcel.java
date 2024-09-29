package org.matsim.analysis.analysisdata;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CarDepartureAnalysisToExcel {
	// 存储每个半小时段的机动车出行者数量
	private static Map<Integer, Integer> timeSlotCarCounts = new HashMap<>();

	// 处理机动车出发事件
	static class CarDepartureEventHandler implements PersonDepartureEventHandler {

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			// 只统计机动车出行（"car"模式）
			if (event.getLegMode().equals("car")) {
				double departureTime = event.getTime();
				// 将时间转换为半小时段，例如 0-1800秒为第一个半小时段
				int timeSlot = (int) (departureTime / 1800); // 每 1800 秒 = 30 分钟
				// 增加该时间段的机动车出行者数量
				timeSlotCarCounts.put(timeSlot, timeSlotCarCounts.getOrDefault(timeSlot, 0) + 1);
			}
		}

		@Override
		public void reset(int iteration) {
			timeSlotCarCounts.clear();
		}
	}

	public static void main(String[] args) {
		// 文件路径
		String eventsFilePath = "D:\\2024SS\\Matsim\\HA2\\berlin-v6.1-RoadPricing10pct-20eur-100\\berlin-v6.1.output_events.xml.gz";

		// 创建事件管理器
		EventsManager eventsManager = EventsUtils.createEventsManager();

		// 创建事件处理器
		CarDepartureEventHandler carHandler = new CarDepartureEventHandler();

		// 将事件处理器添加到事件管理器中
		eventsManager.addHandler(carHandler);

		// 使用 MatsimEventsReader 读取事件文件
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFilePath);

		// 输出到Excel
		String excelFilePath = "D:\\2024SS\\Matsim\\HA2\\berlin-v6.1-RoadPricing10pct-20eur-100\\car_departure_counts.xlsx";
		saveCarDepartureCountsToExcel(excelFilePath);
	}

	// 保存机动车出行统计到Excel文件
	private static void saveCarDepartureCountsToExcel(String filePath) {
		Workbook workbook = new XSSFWorkbook(); // 创建Excel工作簿
		Sheet sheet = workbook.createSheet("Car Departures");

		// 创建标题行
		Row headerRow = sheet.createRow(0);
		headerRow.createCell(0).setCellValue("Time Slot");
		headerRow.createCell(1).setCellValue("Car Departures");

		// 填充数据行
		int rowNum = 1;
		for (Map.Entry<Integer, Integer> entry : timeSlotCarCounts.entrySet()) {
			int timeSlot = entry.getKey();
			int hour = timeSlot / 2; // 计算小时
			int minutes = (timeSlot % 2) * 30; // 计算分钟（每 30 分钟一个时间段）

			Row row = sheet.createRow(rowNum++);
			// 生成时间段描述
			String timeSlotLabel = String.format("%02d:%02d - %02d:%02d", hour, minutes, hour, minutes + 30);
			row.createCell(0).setCellValue(timeSlotLabel); // 时间段
			row.createCell(1).setCellValue(entry.getValue()); // 出行者数量
		}

		// 保存文件
		try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
			workbook.write(fileOut);
			workbook.close();
			System.out.println("Car departure counts saved to Excel file: " + filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

package org.matsim.analysis;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.util.HashMap;
import java.util.Map;

public class RoadPricingAnalysisMATSim {
	// 存储线路ID和该线路的客运量
	private static Map<String, Integer> linePassengerCounts = new HashMap<>();
	// 存储车辆ID与线路ID的映射关系
	private static Map<String, String> vehicleToLineMap = new HashMap<>();

	// 处理乘客进入车辆的事件
	static class TransitPassengerEventHandler implements PersonEntersVehicleEventHandler {
		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			String vehicleId = event.getVehicleId().toString();
			// 查找该车辆所属的线路
			String lineId = vehicleToLineMap.get(vehicleId);
			if (lineId != null) {
				// 增加该线路的客运量
				linePassengerCounts.put(lineId, linePassengerCounts.getOrDefault(lineId, 0) + 1);
			}
		}

		@Override
		public void reset(int iteration) {
			linePassengerCounts.clear();
		}
	}

	// 处理公交线路的驾驶员启动事件，以获取车辆与线路的映射
	static class TransitDriverEventHandler implements TransitDriverStartsEventHandler {
		@Override
		public void handleEvent(TransitDriverStartsEvent event) {
			String vehicleId = event.getVehicleId().toString();
			String lineId = event.getTransitLineId().toString();
			// 将车辆ID与线路ID进行关联
			vehicleToLineMap.put(vehicleId, lineId);
		}

		@Override
		public void reset(int iteration) {
			vehicleToLineMap.clear();
		}
	}

	public static void main(String[] args) {
		// 文件路径
		String eventsFilePath = "D:\\2024SS\\Matsim\\HA2\\berlin-v6.1-RoadPricing10pct-0eur-100\\berlin-v6.1.output_events.xml.gz";

		// 创建事件管理器
		EventsManager eventsManager = EventsUtils.createEventsManager();

		// 创建事件处理器
		TransitPassengerEventHandler passengerHandler = new TransitPassengerEventHandler();
		TransitDriverEventHandler driverHandler = new TransitDriverEventHandler();

		// 将事件处理器添加到事件管理器中
		eventsManager.addHandler(passengerHandler);
		eventsManager.addHandler(driverHandler);

		// 使用 MatsimEventsReader 读取事件文件
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFilePath);

		// 输出每条线路的客运量
		printPassengerCounts();
	}

	// 输出各线路的客运量统计
	private static void printPassengerCounts() {
		System.out.println("Passenger counts per transit line:");
		for (Map.Entry<String, Integer> entry : linePassengerCounts.entrySet()) {
			System.out.println("Line " + entry.getKey() + ": " + entry.getValue() + " passengers");
		}
	}
}

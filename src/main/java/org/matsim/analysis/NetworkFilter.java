package org.matsim.analysis;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NetworkFilter {

	public static void main(String[] args) {
		// 定义文件路径
		String networkFilePath = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\berlin-v6.1-1pct\\berlin-v6.1.output_network.xml.gz";
		String newNetworkFilePath = "C:\\Users\\cdk\\Desktop\\matsim HA1\\berlin-v6.1.output_networknew.xml.gz";
		String excelFilePath = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\LinksWithinPolygon_area.xlsx";

		try {
			// 读取需要保留的道路ID
			Set<String> linkIdsToRetain = readLinkIdsFromExcel(excelFilePath);

			// 创建MATSim配置和场景
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);

			// 读取网络文件
			new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilePath);
			Network network = scenario.getNetwork();

			// 创建一个新的空网络
			Network newNetwork = NetworkUtils.createNetwork();

			// 复制所有节点到新网络
			for (Node node : network.getNodes().values()) {
				NetworkUtils.createAndAddNode(newNetwork, node.getId(), node.getCoord());
			}

			// 复制所需的链接到新网络
			for (String linkId : linkIdsToRetain) {
				Link link = network.getLinks().get(Id.createLinkId(linkId));
				if (link != null) {
					newNetwork.addLink(link);
				}
			}

			// 写入新的网络文件
			new NetworkWriter(newNetwork).write(newNetworkFilePath);

			System.out.println("Filtered network saved to " + newNetworkFilePath);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Set<String> readLinkIdsFromExcel(String excelFilePath) throws IOException {
		Set<String> linkIds = new HashSet<>();
		FileInputStream file = new FileInputStream(excelFilePath);
		Workbook workbook = new XSSFWorkbook(file);
		Sheet sheet = workbook.getSheetAt(0);

		Iterator<Row> rowIterator = sheet.iterator();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			Cell cell = row.getCell(0);
			if (cell != null) {
				linkIds.add(cell.getStringCellValue());
			}
		}
		workbook.close();
		file.close();
		return linkIds;
	}
}

package org.matsim.analysis;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
public class NodeTarifFinder {
	public static void main(String[] args) {
		// Load the network file
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("D:\\2024SS\\Matsim\\matsim-berlin\\output\\berlin-v6.1-1pct\\berlin-v6.1.output_network.xml");

		// Load the link IDs from the txt file
		Set<String> linkIds = new HashSet<>();
		try (BufferedReader br = new BufferedReader(new FileReader("D:\\2024SS\\Matsim\\matsim-berlin\\output\\S41_station.txt"))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("refId")) {
					String linkId = line.substring(line.indexOf("refId") + 5).replaceAll("[^a-zA-Z0-9_]", "");
					System.out.println(linkId);
					linkIds.add(linkId);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Create a new workbook and sheet for the results
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Link Nodes");

		// To keep track of processed nodes
		Set<Id<Node>> processedNodes = new HashSet<>();
		int rowNum = 0;

		// Iterate over the link IDs
		for (String linkId : linkIds) {
			Link link = network.getLinks().get(Id.createLinkId(linkId));
			if (link != null) {
				// Get the fromNode and toNode of the link
				Node fromNode = link.getFromNode();
				Node toNode = link.getToNode();

				// Check and store fromNode information if not already processed
				if (!processedNodes.contains(fromNode.getId())) {
					processedNodes.add(fromNode.getId());
					Row row = sheet.createRow(rowNum++);
					Cell idCell = row.createCell(0);
					idCell.setCellValue(fromNode.getId().toString());
					Cell xCell = row.createCell(1);
					xCell.setCellValue(fromNode.getCoord().getX());
					Cell yCell = row.createCell(2);
					yCell.setCellValue(fromNode.getCoord().getY());
				}

				// Check and store toNode information if not already processed
				if (!processedNodes.contains(toNode.getId())) {
					processedNodes.add(toNode.getId());
					Row row = sheet.createRow(rowNum++);
					Cell idCell = row.createCell(0);
					idCell.setCellValue(toNode.getId().toString());
					Cell xCell = row.createCell(1);
					xCell.setCellValue(toNode.getCoord().getX());
					Cell yCell = row.createCell(2);
					yCell.setCellValue(toNode.getCoord().getY());
				}
			}
		}

		// Write the workbook to a file
		try (FileOutputStream fileOut = new FileOutputStream("D:\\2024SS\\Matsim\\matsim-berlin\\output\\LinkNodes.xlsx")) {
			workbook.write(fileOut);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Close the workbook
		try {
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

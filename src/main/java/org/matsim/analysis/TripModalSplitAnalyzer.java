package org.matsim.analysis;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TripModalSplitAnalyzer {
	public static void main(String[] args) throws IOException {
		// 从Excel文件中读取多边形区域的坐标
		String polygonFilePath = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\S41_stationloc.xlsx";
		FileInputStream polygonFile = new FileInputStream(polygonFilePath);
		Workbook polygonWorkbook = new XSSFWorkbook(polygonFile);
		Sheet polygonSheet = polygonWorkbook.getSheetAt(0);

		Coordinate[] coordinates = new Coordinate[polygonSheet.getPhysicalNumberOfRows()];
		Iterator<Row> polygonRowIterator = polygonSheet.iterator();
		int index = 0;
		while (polygonRowIterator.hasNext()) {
			Row row = polygonRowIterator.next();
			double x = row.getCell(0).getNumericCellValue();
			double y = row.getCell(1).getNumericCellValue();
			coordinates[index++] = new Coordinate(x, y);
		}
		polygonWorkbook.close();
		polygonFile.close();

		// 确保多边形闭合
		if (!coordinates[0].equals(coordinates[coordinates.length - 1])) {
			Coordinate[] closedCoordinates = new Coordinate[coordinates.length + 1];
			System.arraycopy(coordinates, 0, closedCoordinates, 0, coordinates.length);
			closedCoordinates[coordinates.length] = coordinates[0];
			coordinates = closedCoordinates;
		}

		// 定义多边形区域
		GeometryFactory geometryFactory = new GeometryFactory();
		LinearRing linearRing = geometryFactory.createLinearRing(coordinates);
		Polygon polygon = geometryFactory.createPolygon(linearRing, null);

		// 读取CSV文件中的trip数据
		String tripFilePath = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\berlin-v6.1-1pct\\berlin-v6.1.output_trips.csv";
		BufferedReader tripReader = new BufferedReader(new FileReader(tripFilePath));

		// 统计各个模式的出行次数
		Map<String, Integer> modeCounts = new HashMap<>();
		String line;
		tripReader.readLine(); // 跳过表头

		while ((line = tripReader.readLine()) != null) {
			String[] parts = line.split(";");
			double startX = Double.parseDouble(parts[15]);
			double startY = Double.parseDouble(parts[16]);
			double endX = Double.parseDouble(parts[19]);
			double endY = Double.parseDouble(parts[20]);
			String mode = parts[9];

			Point startPoint = geometryFactory.createPoint(new Coordinate(startX, startY));
			Point endPoint = geometryFactory.createPoint(new Coordinate(endX, endY));

			if (polygon.contains(startPoint) || polygon.contains(endPoint)) {
				modeCounts.merge(mode, 1, Integer::sum);
			}
		}
		tripReader.close();

		// 输出结果到CSV文件
		writeModalSplitToCsv("C:\\Users\\cdk\\Desktop\\matsim HA1\\modal_split_output.csv", modeCounts);
	}

	private static void writeModalSplitToCsv(String outputPath, Map<String, Integer> modeCounts) throws IOException {
		FileWriter csvWriter = new FileWriter(outputPath);

		csvWriter.append("Mode,Split (%)\n");

		int totalTrips = modeCounts.values().stream().mapToInt(Integer::intValue).sum();
		for (Map.Entry<String, Integer> entry : modeCounts.entrySet()) {
			double split = (double) entry.getValue() / totalTrips * 100;
			csvWriter.append(entry.getKey()).append(",").append(String.format("%.2f", split)).append("\n");
		}

		csvWriter.flush();
		csvWriter.close();
	}
}

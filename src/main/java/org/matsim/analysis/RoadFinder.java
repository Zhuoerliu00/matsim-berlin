package org.matsim.analysis;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoadFinder {
	public static void main(String[] args) {
		// Load the network file
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("D:\\2024SS\\Matsim\\matsim-berlin\\output\\berlin-v6.1-1pct\\berlin-v6.1.output_network.xml");

		// Define the points of the polygon (you need to fill in the coordinates)
		List<Coord> polygonCoords = new ArrayList<>();
		//polygonCoords.add(CoordUtils.createCoord(844145.86, 5832047.61));
		//polygonCoords.add(CoordUtils.createCoord(850499.58, 5833719.79));
		//polygonCoords.add(CoordUtils.createCoord(851508.93, 5829723.68));
		//polygonCoords.add(CoordUtils.createCoord(850062.64, 5828313.83));
		//polygonCoords.add(CoordUtils.createCoord(844881.06, 5827384.78));
		//polygonCoords.add(CoordUtils.createCoord(844145.86, 5832047.61));
		try (FileInputStream fis = new FileInputStream("D:\\2024SS\\Matsim\\matsim-berlin\\output\\S41_stationloc.xlsx");
			 Workbook workbook = new XSSFWorkbook(fis)) {
			Sheet sheet = workbook.getSheetAt(0);
			for (Row row : sheet) {
				Cell latCell = row.getCell(0);
				Cell lonCell = row.getCell(1);
				if (latCell != null && lonCell != null) {
					double lat = latCell.getNumericCellValue();
					double lon = lonCell.getNumericCellValue();
					System.out.println(lat);
					polygonCoords.add(CoordUtils.createCoord(lat, lon));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Add more points as necessary

		// Create the polygon from the coordinates
		GeometryFactory geometryFactory = new GeometryFactory();
		Coordinate[] coordinates = new Coordinate[polygonCoords.size()];
		for (int i = 0; i < polygonCoords.size(); i++) {
			coordinates[i] = new Coordinate(polygonCoords.get(i).getX(), polygonCoords.get(i).getY());
		}
		Polygon polygon = geometryFactory.createPolygon(coordinates);

		// Find all links whose both ends are within the polygon
		Set<Id<Link>> linkIdsWithinPolygon = new HashSet<>();
		for (Link link : network.getLinks().values()) {
			//if (!link.getAllowedModes().contains("car")) {
			//	continue;
			//}
			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();
			if (polygon.contains(geometryFactory.createPoint(new Coordinate(fromNode.getCoord().getX(), fromNode.getCoord().getY())))
				&& polygon.contains(geometryFactory.createPoint(new Coordinate(toNode.getCoord().getX(), toNode.getCoord().getY())))) {
				linkIdsWithinPolygon.add(link.getId());
			}
		}

		// Print the IDs of the links within the polygon
		System.out.println("Links within the polygon:");
		for (Id<Link> linkId : linkIdsWithinPolygon) {
			System.out.println(linkId.toString());
		}

		// Save the results to an Excel file
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Links Within Polygon");

		int rowNum = 0;
		for (Id<Link> linkId : linkIdsWithinPolygon) {
			Row row = sheet.createRow(rowNum++);
			Cell cell = row.createCell(0);
			cell.setCellValue(linkId.toString());
		}

		try (FileOutputStream fileOut = new FileOutputStream("D:\\2024SS\\Matsim\\matsim-berlin\\output\\LinksWithinPolygon_area.xlsx")) {
			workbook.write(fileOut);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Closing the workbook
		try {
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

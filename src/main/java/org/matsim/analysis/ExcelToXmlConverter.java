package org.matsim.analysis;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ExcelToXmlConverter {

	public static void main(String[] args) {
		String excelFilePath = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\LinksWithinPolygon.xlsx";
		String xmlFilePath = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\output.xml";

		try (FileInputStream fis = new FileInputStream(excelFilePath)) {
			Workbook workbook = WorkbookFactory.create(fis);
			Sheet sheet = workbook.getSheetAt(0);

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();

			// 创建XML结构
			Element rootElement = doc.createElement("roadpricing");
			rootElement.setAttribute("type", "area");
			rootElement.setAttribute("name", "equil-net cordon-toll");
			doc.appendChild(rootElement);

			Element descriptionElement = doc.createElement("description");
			descriptionElement.appendChild(doc.createTextNode("cordon toll"));
			rootElement.appendChild(descriptionElement);

			Element linksElement = doc.createElement("links");
			rootElement.appendChild(linksElement);

			for (Row row : sheet) {
				if (row.getCell(0) != null) {
					String linkId = row.getCell(0).toString();
					Element linkElement = doc.createElement("link");
					linkElement.setAttribute("id", linkId);
					linksElement.appendChild(linkElement);
				}
			}

			Element costElement = doc.createElement("cost");
			costElement.setAttribute("start_time", "07:30");
			costElement.setAttribute("end_time", "17:30");
			costElement.setAttribute("amount", "5.00");
			rootElement.appendChild(costElement);

			// 将XML文件写入磁盘
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.matsim.org/files/dtd/roadpricing_v1.dtd");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(xmlFilePath));

			transformer.transform(source, result);

			System.out.println("File saved to " + xmlFilePath);

		} catch (IOException | ParserConfigurationException | TransformerException e) {
			e.printStackTrace();
		}
	}
}

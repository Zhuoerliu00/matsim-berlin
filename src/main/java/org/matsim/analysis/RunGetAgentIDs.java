package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.EventsUtils;
import org.matsim.vehicles.Vehicle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RunGetAgentIDs {
	public static void main(String[] args) {
		Set<String> monitoredLinkIds = readLinkIdsFromXML("D:\\2024SS\\Matsim\\matsim-berlin\\input\\v6.1\\roadpricing-berlin.xml");
		Set<Id<Person>> personIDs = new HashSet<>();
		var handler = new AreaPersonTrackerEventHandler(monitoredLinkIds);

		var manager = EventsUtils.createEventsManager();
		manager.addHandler(handler);
		EventsUtils.readEvents(manager, "C:\\Users\\cdk\\Desktop\\matsim HA1\\berlin-v6.1.output_events.xml.gz" );

		int count = 0;
		for(Id<Vehicle> vehicleId : handler.getVehicleIds()) {
			Id<Person> personId = handler.getDriverAgents().get(vehicleId);
			personIDs.add(personId);
			count++;
		}
		System.out.println(count);
		System.out.println(personIDs.size());


		try {
			savePersonIdsAsCsv(personIDs, "C:\\Users\\cdk\\Desktop\\matsim HA1\\personIDs.csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void savePersonIdsAsCsv(Set<Id<Person>> personIds, String filename) throws IOException {
		try (FileWriter writer = new FileWriter(filename)) {
			for (Id<Person> id : personIds) {
				if(id!= null) {
					writer.append(id.toString());
					writer.append('\n');
				}
			}
		}
	}



	public static Set<String> readLinkIdsFromXML(String filePath) {
		Set<String> linkIds = new HashSet<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(filePath);
			document.getDocumentElement().normalize();

			NodeList linkList = document.getElementsByTagName("link");
			for (int i = 0; i < linkList.getLength(); i++) {
				Element linkElement = (Element) linkList.item(i);
				String linkId = linkElement.getAttribute("id");
				linkIds.add(linkId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return linkIds;
	}
}

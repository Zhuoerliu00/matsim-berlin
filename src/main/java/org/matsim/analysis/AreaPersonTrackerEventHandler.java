package org.matsim.analysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class AreaPersonTrackerEventHandler implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler {
	private final Set<String> monitoredLinkIds;
	private final Set<Id<Vehicle>> vehicleIds = new HashSet<>();
	private final Map<Id<Vehicle>, Id<Person>> driverAgents = new ConcurrentHashMap<>();


	public AreaPersonTrackerEventHandler(Set<String> monitoredLinkIds) {
		this.monitoredLinkIds = monitoredLinkIds;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		double time = event.getTime();
		double timeInHr = time / 3600;

		if ((7.5 <= timeInHr && timeInHr <= 17.5)
			&& (event.getVehicleId() != null)
//		&& (event.getVehicleId().toString().contains("car"))
			&& (monitoredLinkIds.contains(event.getLinkId().toString()))
		) {
			vehicleIds.add(event.getVehicleId());
		}
	}

	public Set<Id<Vehicle>> getVehicleIds() {
		return vehicleIds;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if(event.getNetworkMode().toString().equals("car")) {
			driverAgents.put(event.getVehicleId(), event.getPersonId());
		}
	}

	public Map<Id<Vehicle>, Id<Person>> getDriverAgents() {
		return driverAgents;
	}
}


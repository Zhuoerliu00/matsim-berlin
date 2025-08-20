package org.matsim.run.gartenfeld;

import com.google.inject.name.Names;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModule;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.*;

import java.util.*;

/**
 * Station-based sharing for roller:
 * - Service mode: sharing_roller (used for mode choice/statistics)
 * - Underlying ride: roller (network routing)
 * - Sharing logic is provided by SharingModule (injects booking/pickup/dropoff + stock/capacity constraints)
 */
public class GartenfeldSharingRollerScenario extends GartenfeldScenario {

	private static final String BASE_MODE = "roller";           // Underlying network mode (like "car" in the example)
	private static final String SERVICE_ID = "roller";          // Service id (arbitrary, but recommended to match the base mode)
	private static final String SERVICE_MODE = "sharing_roller"; // Used only for choice/statistics (derived by SharingUtils)

	public static void main(String[] args) {
		MATSimApplication.run(GartenfeldSharingRollerScenario.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		config = super.prepareConfig(config);

		// 1) Define the sharing service: StationBased + input file + baseMode=roller
		SharingConfigGroup sharingCfg = ConfigUtils.addOrGetModule(config, SharingConfigGroup.class);

		SharingServiceConfigGroup service = new SharingServiceConfigGroup();
		service.setId(SERVICE_ID);
		service.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.StationBased);
		service.setServiceInputFile("/net/ils/zliu/input/gartenfeld/shared_roller_vehicles_stations.xml");
		service.setMaximumAccessEgressDistance(2000);
		service.setMode(BASE_MODE); // Key: use "roller" as the base mode (analogous to "car" in the example)
		service.setBaseFare(0.75);
		service.setTimeFare(0.24);
		service.setDistanceFare(0.0);
		sharingCfg.addService(service);

		final String serviceMode = SharingUtils.getServiceMode(service); // Usually "sharing_roller"

		// 2) ROUTING: add the base mode "roller" to networkModes; do NOT add "sharing_roller"

		RoutingConfigGroup routing = config.routing();
		Set<String> netModes = new HashSet<>(routing.getNetworkModes());
		netModes.add(BASE_MODE);                 // Let the riding legs use the network (roller)
		routing.setNetworkModes(netModes);


		// Optional: if you keep roller out of QSim (route-only, no execution), you may control leg time via teleported speed.
		// (I.e., network is used at routing, but execution remains teleported. To move into QSim, see a separate switch.)
		/*
		 RoutingConfigGroup.TeleportedModeParams tp = new RoutingConfigGroup.TeleportedModeParams(SERVICE_MODE);
		 tp.setTeleportedModeSpeed(5.56);
		 tp.setBeelineDistanceFactor(1.4353);
		 config.routing().addTeleportedModeParams(tp);
		 */

		// 3) Mode choice: include the service mode (as in the official example)
		List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
		if (!modes.contains(serviceMode)) {
			modes.add(serviceMode);
			config.subtourModeChoice().setModes(modes.toArray(new String[0]));
		}

		// 4) Sharing interaction activities: do not score them
		for (String act : List.of(SharingUtils.PICKUP_ACTIVITY, SharingUtils.DROPOFF_ACTIVITY, SharingUtils.BOOKING_ACTIVITY)) {
			ScoringConfigGroup.ActivityParams ap = new ScoringConfigGroup.ActivityParams(act);
			ap.setScoringThisActivityAtAll(false);
			config.scoring().addActivityParams(ap);
		}

		// 5) Add scoring params for the base mode "roller" (analogous to adding params for "car" in the example)
		if (config.scoring().getModes().get(BASE_MODE) == null) {
			ScoringConfigGroup.ModeParams mp = new ScoringConfigGroup.ModeParams(BASE_MODE);
			config.scoring().addModeParams(mp);
		}

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);

		// Sharing module: inserts booking/pickup/dropoff and enforces stock/capacity constraints
		controler.addOverridingModule(new SharingModule());
		/*controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(RoutingModule.class)
					.annotatedWith(Names.named("roller"))
					.toProvider(() -> new TeleportationRoutingModule(
						"sharing_roller",
						controler.getScenario(),
						5.0, // speed
						1.3,  // beeline distance factor
						null
					));
			}
		});*/

		// Analysis main mode identification: prefer sharing_roller (for stats)
		controler.addOverridingModule(new AbstractModule() {
			@Override public void install() {
				bind(MainModeIdentifier.class).to(CustomMMI.class);
				bind(AnalysisMainModeIdentifier.class).to(CustomMMI.class);
			}
		});

		// QSim components for sharing: keep them enabled
		SharingConfigGroup sharingCfg = ConfigUtils.addOrGetModule(controler.getConfig(), SharingConfigGroup.class);
		controler.configureQSimComponents(SharingUtils.configureQSim(sharingCfg));
	}

	/** Identify sharing_roller as the main mode (for stats); fall back to roller; otherwise delegate. */
	public static class CustomMMI implements AnalysisMainModeIdentifier {
		private final DefaultAnalysisMainModeIdentifier delegate = new DefaultAnalysisMainModeIdentifier();
		@Override public String identifyMainMode(List<? extends PlanElement> trip) {
			for (PlanElement pe : trip) if (pe instanceof Leg leg && "pt".equals(leg.getMode())) return "pt";
			for (PlanElement pe : trip) if (pe instanceof Leg leg && SERVICE_MODE.equals(leg.getMode())) return SERVICE_MODE;
			for (PlanElement pe : trip) if (pe instanceof Leg leg && BASE_MODE.equals(leg.getMode())) return BASE_MODE;
			return delegate.identifyMainMode(trip);
		}
	}
}

package org.matsim.policies.gartenfeld;

import com.google.inject.name.Names;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModule;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.*;
import org.matsim.core.controler.AbstractModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class defines and runs a MATSim scenario for the Gartenfeld area with integrated station-based
 * shared e-scooter ("sharing_roller") services. It extends the base `GartenfeldScenario` and enhances it
 * by configuring a multimodal simulation that includes public transport, car, walking, biking, and
 * shared micromobility.

 * Key functionalities:
 * - Configures a station-based scooter-sharing service using `SharingServiceConfigGroup`.
 * - Registers the `sharing_roller` mode as a teleportation-based routing mode with specified speed and distance factor.
 * - Adds relevant mode scoring and non-scoring activity types for booking/pick-up/drop-off phases.
 * - Integrates a custom `MainModeIdentifier` prioritizing `pt` and `sharing_roller` for intermodal trip detection.
 * - Installs the `SharingModule` for enabling simulation components and logic for shared mobility.
 * - Supports evaluation of how the presence of shared e-scooters influences mode choices in the urban network.

 * This setup is intended for use in research and planning applications analyzing intermodal behavior,
 * especially regarding first-/last-mile connections with shared micromobility in dense urban contexts.
 */

public class GartenfeldRollerScenario extends GartenfeldScenario {
    public static void main(String[] args) {

        MATSimApplication.runWithDefaults(GartenfeldRollerScenario.class, args,
			"--parking-garages", "NO_GARAGE",
			"--config:network.inputChangeEventsFile", "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/gartenfeld/input/gartenfeld-v6.4.network-change-events.xml.gz",
			"--config:facilities.inputFacilitiesFile", "./berlin-v6.4-facilities.xml.gz",
			"--config:controller.runId", "gartenfeld-v6.4.full-roller-1pct",
			"--config:network.inputNetworkFile", "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/gartenfeld/input/gartenfeld-v6.4.network.xml.gz",
			"--config:plans.inputPlansFile", "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/gartenfeld/input/gartenfeld-v6.4.population-full-1pct.xml.gz",
			"--config:controller.lastIteration", "5",
			"--config:controller.outputDirectory", "output/gartenfeld-v6.4.full-roller-1pct-test/"
        );
    }

    @Override
    protected void prepareScenario(Scenario scenario) {
        super.prepareScenario(scenario);
    }

    @Override
    protected Config prepareConfig(Config config) {
		config = super.prepareConfig(config);

		// Add sharing config module
		SharingConfigGroup sharingConfig = new SharingConfigGroup();
		config.addModule(sharingConfig);

		// Define the sharing service
		SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
		serviceConfig.setId("roller");
		serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.StationBased);
		serviceConfig.setMaximumAccessEgressDistance(2000);
		serviceConfig.setServiceInputFile("shared_roller_vehicles_stations.xml");
		serviceConfig.setMode("roller");
		serviceConfig.setBaseFare(0.75);
		serviceConfig.setTimeFare(0.24);
		serviceConfig.setDistanceFare(0.0);

		sharingConfig.addService(serviceConfig);

		// Register the shared mode as a teleportation mode
		String sharedMode = SharingUtils.getServiceMode(serviceConfig);
		RoutingConfigGroup.TeleportedModeParams sharedRoutingParams = new RoutingConfigGroup.TeleportedModeParams(sharedMode);
		sharedRoutingParams.setTeleportedModeSpeed(5.0);
		sharedRoutingParams.setBeelineDistanceFactor(1.3);
		config.routing().addTeleportedModeParams(sharedRoutingParams);

		// Add the shared mode to mode choice
		List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
		modes.add(sharedMode);
		config.subtourModeChoice().setModes(modes.toArray(new String[0]));


		// Add activity types used in shared mobility
		for (String act : List.of(SharingUtils.PICKUP_ACTIVITY, SharingUtils.DROPOFF_ACTIVITY, SharingUtils.BOOKING_ACTIVITY)) {
			ScoringConfigGroup.ActivityParams params = new ScoringConfigGroup.ActivityParams(act);
			params.setScoringThisActivityAtAll(false);
			config.scoring().addActivityParams(params);
		}

		// Score base mode (roller)
		ScoringConfigGroup.ModeParams rollerParams = new ScoringConfigGroup.ModeParams("sharing_roller");
		config.scoring().addModeParams(rollerParams);

		return config;
    }

    @Override
    protected void prepareControler(Controler controler) {
        super.prepareControler(controler);
		// register SharingModule which sets up routing, scoring, QSim components, etc.
		controler.addOverridingModule(new SharingModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(RoutingModule.class)
					.annotatedWith(Names.named("roller"))
					.toProvider(() -> new TeleportationRoutingModule(
						"sharing_roller",
						controler.getScenario(),
						5.0, // speed
						1.3  // beeline distance factor
					));
			}
		});

		// MainModeIdentifier with fallback and delegation
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(MainModeIdentifier.class).to(CustomAnalysisMainModeIdentifier.class);
				bind(AnalysisMainModeIdentifier.class).to(CustomAnalysisMainModeIdentifier.class);
			}
		});

		controler.addControlerListener(new SilentModeChoiceCoverageListener(
			new CustomAnalysisMainModeIdentifier(),
			controler.getScenario().getPopulation()
		));

		// Add shared mobility QSim components
		SharingConfigGroup sharingConfig = ConfigUtils.addOrGetModule(controler.getConfig(), SharingConfigGroup.class);
		controler.configureQSimComponents(SharingUtils.configureQSim(sharingConfig));

    }

	// Custom class for identifying sharing_roller
	public static class CustomAnalysisMainModeIdentifier implements AnalysisMainModeIdentifier {
		private final DefaultAnalysisMainModeIdentifier delegate = new DefaultAnalysisMainModeIdentifier();

		@Override
		public String identifyMainMode(List<? extends PlanElement> tripElements) {
			// Priority 1: If the trip includes any leg using pt, return "pt"
			for (PlanElement pe : tripElements) {
				if (pe instanceof Leg leg && "pt".equals(leg.getMode())) {
					return "pt";
				}
			}
			// Priority 2: If the trip includes any leg using sharing_roller, return "sharing_roller"
			for (PlanElement pe : tripElements) {
				if (pe instanceof Leg leg && "sharing_roller".equals(leg.getMode())) {
					return "sharing_roller";
				}
			}
			// Fallback: Use default delegate
			return delegate.identifyMainMode(tripElements);
		}
	}


}

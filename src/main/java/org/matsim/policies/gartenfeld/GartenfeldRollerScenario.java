package org.matsim.policies.gartenfeld;

//import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.core.controler.AbstractModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
			"--config:controller.outputDirectory", "output/gartenfeld-v6.4.full-roller-1pct/"
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
		serviceConfig.setBaseFare(0);
		serviceConfig.setTimeFare(0.1 / 60);

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
		// Important: register SharingModule which sets up routing, scoring, QSim components, etc.
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

		// Add shared mobility QSim components
		SharingConfigGroup sharingConfig = ConfigUtils.addOrGetModule(controler.getConfig(), SharingConfigGroup.class);
		controler.configureQSimComponents(SharingUtils.configureQSim(sharingConfig));

    }


}

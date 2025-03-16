package org.matsim.policies.gartenfeld;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;

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
			"--config:controller.lastIteration", "3",
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

		// We need to add the sharing config group
		SharingConfigGroup sharingConfig = new SharingConfigGroup();
		config.addModule(sharingConfig);

		// Define a service ...
		SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
		sharingConfig.addService(serviceConfig);

		// ... with a service id.
		serviceConfig.setId("roller");

		// ... with StationBased characteristics
		serviceConfig.setMaximumAccessEgressDistance(100000);
		serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.StationBased);
		serviceConfig.setServiceAreaShapeFile(null);

		// ... with a number of available vehicles and their initial locations
		serviceConfig.setServiceInputFile("shared_roller_vehicles_stations.xml");

		// ... and, we need to define the underlying mode, here "roller".
		serviceConfig.setMode("roller");
		serviceConfig.setBaseFare(0.75);    //basis 0.75€
		serviceConfig.setTimeFare(0.24/60);  //0.24 €/min

		// Finally, we need to make sure that the service mode is considered in mode choice.
		List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
		//modes.add(SharingUtils.getServiceMode(serviceConfig));
		modes.add(removeSharingPrefix(SharingUtils.getServiceMode(serviceConfig)));
		config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

		// We need to add interaction activity types to scoring
		ScoringConfigGroup.ActivityParams pickupParams = new ScoringConfigGroup.ActivityParams(SharingUtils.PICKUP_ACTIVITY);
		pickupParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(pickupParams);

		ScoringConfigGroup.ActivityParams dropoffParams = new ScoringConfigGroup.ActivityParams(SharingUtils.DROPOFF_ACTIVITY);
		dropoffParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(dropoffParams);

		ScoringConfigGroup.ActivityParams bookingParams = new ScoringConfigGroup.ActivityParams(SharingUtils.BOOKING_ACTIVITY);
		bookingParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(bookingParams);

		// We need to score roller
		ScoringConfigGroup.ModeParams rollerScoringParams = new ScoringConfigGroup.ModeParams("roller");
		config.scoring().addModeParams(rollerScoringParams);

        return config;
    }

    @Override
    protected void prepareControler(Controler controler) {
        super.prepareControler(controler);

    }

	public static String removeSharingPrefix(String mode) {
		return mode.replace("sharing_", "");
	}

}

package org.matsim.policies.gartenfeld;

import org.matsim.application.MATSimApplication;
import org.matsim.run.OpenBerlinScenario;

/**
 * Run class for the Gartenfeld scenario.
 */
public final class RunGartenfeldScenario {

	private RunGartenfeldScenario() {
	}

	public static void main(String[] args) {
		MATSimApplication.runWithDefaults(GartenfeldScenario.class, args,
			"--parking-garages", "NO_GARAGE",
			"--config:network.inputChangeEventsFile", "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/gartenfeld/input/gartenfeld-v6.4.network-change-events.xml.gz",
			"--config:facilities.inputFacilitiesFile", "D:\\2024SS\\Masterarbeit\\matsim-berlin\\input\\gartenfeld\\berlin-v6.4-facilities.xml.gz",
			"--config:controller.runId", "gartenfeld-v6.4.full-base-1pct",
			"--config:network.inputNetworkFile", "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/gartenfeld/input/gartenfeld-v6.4.network.xml.gz",
			"--config:plans.inputPlansFile", "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/gartenfeld/input/gartenfeld-v6.4.population-full-1pct.xml.gz",
			"--config:controller.lastIteration", "3",
			"--config:controller.outputDirectory", "output/gartenfeld-v6.4.full-base-1pct/"
			);
	}

}

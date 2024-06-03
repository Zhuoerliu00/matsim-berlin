package org.matsim.run;


import org.matsim.application.MATSimApplication;
/**
 * Run the {@link OpenBerlinScenario} with default configuration.
 */
public final class RunOpenBerlinScenario {

	private RunOpenBerlinScenario() {
	}

	public static void main(String[] args) {
		System.setProperty("java.opts", "-Xms4g -Xmx8g");
		MATSimApplication.runWithDefaults(OpenBerlinScenario.class, args);
	}

}

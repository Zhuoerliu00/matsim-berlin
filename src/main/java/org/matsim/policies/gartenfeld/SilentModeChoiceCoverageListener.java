package org.matsim.policies.gartenfeld;

import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.MainModeIdentifier;

import java.util.List;

/**
 * Silent alternative to ModeChoiceCoverageControlerListener.
 * Prevents "unknown mode" crash by using custom MainModeIdentifier,
 * but does not write any output.
 */
public class SilentModeChoiceCoverageListener implements IterationEndsListener {

	private final MainModeIdentifier modeIdentifier;
	private final Population population;

	public SilentModeChoiceCoverageListener(MainModeIdentifier modeIdentifier,
											Population population
											) {
		this.modeIdentifier = modeIdentifier;
		this.population = population;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

	}
}

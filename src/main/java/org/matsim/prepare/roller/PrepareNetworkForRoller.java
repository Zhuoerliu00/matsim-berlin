package org.matsim.prepare.roller;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.network.io.NetworkWriter;
import picocli.CommandLine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@CommandLine.Command(
	name = "prepare-network-roller",
	description = "Annotate network links to allow 'roller' where 'bike' is allowed; " +
		"optionally restrict by a service-area shapefile; then keep only the largest connected component for 'roller' to ensure routability."
)
public class PrepareNetworkForRoller implements MATSimAppCommand {

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions(); // used when --restrict-to-shp

	@CommandLine.Option(names = "--input-network", required = true, description = "Path to input network (.xml or .xml.gz)")
	private String inputNetwork;

	@CommandLine.Option(names = "--output-network", required = true, description = "Path to output network (.xml or .xml.gz)")
	private String outputNetwork;

	@CommandLine.Option(names = "--mode-name", defaultValue = "sharing_roller", description = "Mode name to add to allowed link modes (default: roller)")
	private String rollerMode;

	@CommandLine.Option(names = "--crs", defaultValue = "EPSG:25832", description = "CRS of the network and shapefile (default: EPSG:25832)")
	private String crs;

	@CommandLine.Option(names = "--restrict-to-shp", defaultValue = "false",
		description = "If true, only annotate links within the provided shapefile geometry (see --shape).")
	private boolean restrictToShp;

	@CommandLine.Option(names = "--endpoints-inside", defaultValue = "true",
		description = "When restricting by shapefile, require BOTH link endpoints to lie within the area (safer than midpoint). Default: true.")
	private boolean endpointsMustBeInside;

	@CommandLine.Option(names = "--buffer-meters", defaultValue = "0.0",
		description = "Optional buffer (meters) applied to the shapefile geometry before testing containment. Default: 0.0")
	private double bufferMeters;

	@CommandLine.Option(names = "--ensure-connected", defaultValue = "true",
		description = "Ensure connectivity by keeping only the largest connected component for the 'roller' subnet, removing 'roller' from other links. Default: true.")
	private boolean ensureConnected;

	public static void main(String[] args) {
		new PrepareNetworkForRoller().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		// 1) Load network
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(crs);
		config.network().setInputFile(inputNetwork);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		// 2) Prepare service area geometry (optional)
		Geometry area = null;
		if (restrictToShp) {
			area = shp.getGeometry();
			if (area == null) {
				throw new IllegalArgumentException("Shapefile geometry is null but --restrict-to-shp was set. Provide --shape and correct CRS.");
			}
			if (bufferMeters != 0.0) {
				area = area.buffer(bufferMeters);
			}
		}

		// 3) Annotate links: add 'roller' where 'bike' is allowed (+ optional area filter)
		int added = annotateNetwork(network, rollerMode, area, endpointsMustBeInside);
		System.out.printf("prepare-network-roller: added '%s' to %d link(s)%n", rollerMode, added);

		// 4) Ensure connectivity: keep only largest connected component for roller
		if (ensureConnected) {
			int removed = ensureConnectivityByLargestComponent(network, rollerMode);
			System.out.printf("prepare-network-roller: removed '%s' from %d link(s) not in largest connected component%n", rollerMode, removed);
		} else {
			System.out.println("prepare-network-roller: --ensure-connected=false (skipped connectivity cleanup).");
		}

		// 5) Write output (with CRS attribute)
		ProjectionUtils.putCRS(network, crs);
		new NetworkWriter(network).write(outputNetwork);
		System.out.printf("prepare-network-roller: wrote network to %s%n", outputNetwork);
		return 0;
	}

	/**
	 * Annotate links so that 'rollerMode' is included in allowed modes
	 * wherever 'bike' is already allowed, optionally restricted by a service-area polygon.
	 * If area != null and endpointsMustBeInside=true, both endpoints must lie within area.
	 * If area != null and endpointsMustBeInside=false, the link midpoint must be within area.
	 */
	public static int annotateNetwork(Network network, String rollerMode, Geometry area, boolean endpointsMustBeInside) {
		final String BIKE = "bike";
		int changed = 0, considered = 0;

		for (Link link : network.getLinks().values()) {
			Set<String> modes = link.getAllowedModes();
			if (modes == null || !modes.contains(BIKE)) continue; // only tag links that allow bike
			considered++;

			if (area != null) {
				boolean inside;
				if (endpointsMustBeInside) {
					inside = pointWithin(area, link.getFromNode()) && pointWithin(area, link.getToNode());
				} else {
					Coord mid = midpoint(link.getFromNode().getCoord(), link.getToNode().getCoord());
					inside = MGC.coord2Point(mid).within(area);
				}
				if (!inside) continue;
			}

			if (!modes.contains(rollerMode)) {
				// copy to avoid UnsupportedOperationException on unmodifiable sets
				Set<String> newModes = new HashSet<>(modes);
				newModes.add(rollerMode);
				// keep existing modes; only add roller
				link.setAllowedModes(newModes);
				changed++;
			}
		}

		System.out.printf("prepare-network-roller: considered %d bike links, added '%s' to %d links%n",
			considered, rollerMode, changed);
		return changed;
	}

	/**
	 * Keep only the largest connected component for the 'roller' subnet.
	 * Implementation:
	 *  1) Build a subnet filtered by 'roller'
	 *  2) Clean it with NetworkCleaner (keeps largest CC)
	 *  3) Remove 'roller' from links not present in the cleaned subnet
	 */
	public static int ensureConnectivityByLargestComponent(Network network, String rollerMode) {
		// 1) Filter roller subnet
		Network rollerNet = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(rollerNet, Collections.singleton(rollerMode));

		// 2) Clean (keeps largest connected component)
		new NetworkCleaner().run(rollerNet);

		// 3) Link IDs that remain connected
		Set<Id<Link>> connected = rollerNet.getLinks().keySet();

		// 4) Remove 'roller' from links not in the largest CC
		int removed = 0;
		for (Link l : network.getLinks().values()) {
			Set<String> modes = l.getAllowedModes();
			if (modes != null && modes.contains(rollerMode) && !connected.contains(l.getId())) {
				Set<String> nm = new HashSet<>(modes);
				nm.remove(rollerMode);
				l.setAllowedModes(nm);
				removed++;
			}
		}
		return removed;
	}

	private static boolean pointWithin(Geometry g, Node n) {
		Point p = MGC.coord2Point(n.getCoord());
		return p.within(g);
	}

	private static Coord midpoint(Coord a, Coord b) {
		return new Coord(0.5 * (a.getX() + b.getX()), 0.5 * (a.getY() + b.getY()));
	}
}

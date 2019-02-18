package RunAbfall;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.EngineInformation.FuelType;

public class Run_Abfall {

	static int stunden = 3600;
	static int minuten = 60;

	private static final Logger log = Logger.getLogger(Run_Abfall.class);

	private static final String original_Chessboard = "scenarios/networks/originalChessboard9x9.xml";
	private static final String modified_Chessboard = "scenarios/networks/modifiedChessboard9x9.xml";

	private enum scenarioAuswahl {
		originalChessboard, modifiedChessboard
	};

	public static void main(String[] args) {
		log.setLevel(Level.INFO);

		scenarioAuswahl scenarioWahl = scenarioAuswahl.modifiedChessboard;

		// MATSim config
		Config config = ConfigUtils.createConfig();

		switch (scenarioWahl) {
		case originalChessboard:
			config.controler().setOutputDirectory("output/original_Chessboard/05_FiniteSize");
			config.network().setInputFile(original_Chessboard);
			break;
		case modifiedChessboard:
			config.controler().setOutputDirectory("output/modified_Chessboard/01_InfiniteSize");
			config.network().setInputFile(modified_Chessboard);
			break;

		default:
			new RuntimeException("no scenario selected.");
		}
		int lastIteration = 0;
		config = Run_AbfallUtils.prepareConfig(config, lastIteration);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();
		Carrier myCarrier = CarrierImpl.newInstance(Id.create("BSR", Carrier.class));

		// create shipmets from every link to the garbage dump
		Id<Link> garbageDumpId = Id.createLinkId("j(0,9)R");
		Run_AbfallUtils.createShipmentsForCarrier(scenario, myCarrier, garbageDumpId, carriers);

		// create a garbage truck type
		String vehicleTypeId = "TruckType1";
		int capacity = 300;
		double maxVelocity = 50 / 3.6;
		double costPerDistanceUnit = 1;
		double costPerTimeUnit = 0.01;
		double fixCosts = 200;
		FuelType engineInformation = FuelType.diesel;
		double literPerMeter = 0.01;
		CarrierVehicleType carrierVehType = Run_AbfallUtils.createGarbageTruckType(vehicleTypeId, capacity, maxVelocity,
				costPerDistanceUnit, costPerTimeUnit, fixCosts, engineInformation, literPerMeter);
		CarrierVehicleTypes vehicleTypes = Run_AbfallUtils.adVehicleType(carrierVehType);

		// create vehicle at depot
		String vehicleID = "GargabeTruck";
		String linkDepot = "j(9,9)";
		double earliestStartingTime = 6 * stunden;
		double latestFinishingTime = 15 * stunden;

		CarrierVehicle garbageTruck1 = Run_AbfallUtils.createGarbageTruck(vehicleID, linkDepot, earliestStartingTime,
				latestFinishingTime, carrierVehType);

		// define Carriers
		FleetSize fleetSize = FleetSize.INFINITE;
		Run_AbfallUtils.defineCarriers(carriers, myCarrier, carrierVehType, vehicleTypes, garbageTruck1, fleetSize);

		// jsprit
		Run_AbfallUtils.solveWithJsprit(scenario, carriers, myCarrier, vehicleTypes);

		final Controler controler = new Controler(scenario);

		Run_AbfallUtils.platzhalter(scenario, carriers, controler);

		controler.run();

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans_Test01.xml");

	}

}

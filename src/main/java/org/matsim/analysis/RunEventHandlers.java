package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.core.events.EventsUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

public class RunEventHandlers {

	private static final String eventsFile = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\berlin-v6.1-1pct\\berlin-v6.1.output_events.xml.gz";
	private static final String outFile = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\berlin-v6.1-1pct\\outputFileForHandlers.csv";

	private static final String chartOutFile = "D:\\2024SS\\Matsim\\matsim-berlin\\output\\berlin-v6.1-1pct\\chartOutFileForHandlers.png";
	public static void main(String[] args) {
		var manager = EventsUtils.createEventsManager();
		var linkHandler = new LinkEventHandler();
		manager.addHandler(linkHandler);

		EventsUtils.readEvents(manager, eventsFile);

		Map<Integer, Integer> volumes = new TreeMap<>();
		for (var entry : linkHandler.getVolumes().entrySet()) {
			int normalizedHour = Integer.parseInt(entry.getKey()) % 24;
			volumes.merge(normalizedHour, entry.getValue(), Integer::sum);
		}

		//var volumes = linkHandler.getVolumes();

		try (var writer = Files.newBufferedWriter(Paths.get(outFile));
			 var printer = CSVFormat.DEFAULT.withDelimiter(';').withHeader("Hour", "Volume").print(writer)) {

			for (var volume : volumes.entrySet()) {
				printer.printRecord(volume.getKey(), volume.getValue());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}


		// Create a dataset for the chart
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (var entry : volumes.entrySet()) {
			dataset.addValue(entry.getValue(), "Volume", entry.getKey());
		}

		// Create the chart
		JFreeChart barChart = ChartFactory.createBarChart(
			"Hourly Link Volumes",
			"Hour",
			"Volume",
			dataset,
			PlotOrientation.VERTICAL,
			true, true, false);

		// Save the chart as a PNG file
		int width = 800;
		int height = 600;
		File barChartFile = new File(chartOutFile);
		try {
			ChartUtils.saveChartAsPNG(barChartFile, barChart, width, height);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

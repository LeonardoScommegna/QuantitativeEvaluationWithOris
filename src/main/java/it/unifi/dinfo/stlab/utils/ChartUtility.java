package it.unifi.dinfo.stlab.utils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.util.Map;

public class ChartUtility {

    /**
     * Creates and displays a line chart for an arbitrary number of time series.
     *
     * @param dataSeries A map where the key is the series name and the value is the data array (Y-axis)
     * @param startTime  The start time (e.g., 0.0)
     * @param timeStep   The time increment between values (e.g., 0.1)
     * @param chartTitle The title for the window and the chart
     */
    public static void plotData(Map<String, double[]> dataSeries, double startTime, double timeStep, String chartTitle) {

        XYSeriesCollection dataset = new XYSeriesCollection();

        // Iterate over all arrays passed in the map
        for (Map.Entry<String, double[]> entry : dataSeries.entrySet()) {
            String seriesName = entry.getKey();
            double[] values = entry.getValue();

            // Create a new series for JFreeChart
            XYSeries series = new XYSeries(seriesName);

            // Calculate the current time starting from the initial time
            double currentTime = startTime;

            // Populate the series: each value in the array corresponds to a time step
            for (int i = 0; i < values.length; i++) {
                series.add(currentTime, values[i]);
                currentTime += timeStep; // Increment the time for the next iteration
            }

            dataset.addSeries(series);
        }

        // Chart creation
        JFreeChart chart = ChartFactory.createXYLineChart(
                chartTitle,                // Chart title
                "Time",                    // X-axis label
                "Value",                   // Y-axis label
                dataset,                   // Dataset containing all series
                PlotOrientation.VERTICAL,  // Orientation
                true,                      // Show legend (essential with multiple series)
                true,                      // Use tooltips
                false                      // URLs (not needed)
        );

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(chartTitle);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
            frame.setContentPane(chartPanel);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
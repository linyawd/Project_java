package com.webtools;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BandwidthMonitor {
    private static final BandwidthMonitor INSTANCE = new BandwidthMonitor();

    private final TimeSeries downloadSeries = new TimeSeries("Завантажено");
    private final TimeSeriesCollection dataset = new TimeSeriesCollection(downloadSeries);

    private long totalDownloaded = 0;
    private final ConcurrentHashMap<String, Long> hourlyStats = new ConcurrentHashMap<>();

    private BandwidthMonitor() {
        
        new Timer(2000, e -> SwingUtilities.invokeLater(this::updateCurrentTime)).start();
    }

    public static BandwidthMonitor getInstance() {
        return INSTANCE;
    }

   
    public synchronized void addDownloaded(long bytes) {
        if (bytes <= 0) return;

        totalDownloaded += bytes;

        String hourKey = LocalDateTime.now().getHour() + ":00";
        hourlyStats.merge(hourKey, bytes, Long::sum);

       
        updateCurrentTime();
    }

    private void updateCurrentTime() {
        double valueInMB = totalDownloaded / (1024.0 * 1024.0);
        if (valueInMB < 0.01) valueInMB = 0.01; 

        Minute now = new Minute(java.util.Date.from(
                LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));

        downloadSeries.addOrUpdate(now, valueInMB);
    }

    public void showReport() {
        System.out.println("\n=== Звіт по трафіку ===");
        System.out.println("Всього завантажено: " + formatBytes(totalDownloaded));

        if (!hourlyStats.isEmpty()) {
            System.out.println("\nПо годинах:");
            hourlyStats.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> System.out.println(e.getKey() + " — " + formatBytes(e.getValue())));
        }

        
        SwingUtilities.invokeLater(this::showChart);
    }

    private void showChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Використання трафіку",
                "Час",
                "Завантажено (МБ)",
                dataset,
                true,
                true,
                false
        );

        NumberAxis rangeAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setAutoRangeMinimumSize(0.1);
        rangeAxis.setLowerMargin(0.15);
        rangeAxis.setUpperMargin(0.25);

        chart.getXYPlot().setBackgroundPaint(new Color(250, 250, 250));
        chart.getXYPlot().setDomainGridlinePaint(Color.LIGHT_GRAY);
        chart.getXYPlot().setRangeGridlinePaint(Color.LIGHT_GRAY);

        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(960, 620));
        panel.setMouseWheelEnabled(true);

        JFrame frame = new JFrame("Монітор трафіку");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " Б";
        if (bytes < 1024 * 1024) return String.format("%.1f КБ", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f МБ", bytes / (1024.0 * 1024.0));
        return String.format("%.2f ГБ", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
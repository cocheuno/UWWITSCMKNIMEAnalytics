package org.uwwitscm.analytics.kmeans;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.knime.core.node.NodeView;

/**
 * Node view using JFreeChart to render WCSS (left axis) and Average Silhouette (right axis).
 * Prepares datasets off the EDT and sets them into the chart on the EDT.
 *
 * Requirements:
 * - Place jfreechart-1.5.3.jar and jcommon-1.0.24.jar into plugin/lib/
 * - Ensure MANIFEST.MF Bundle-ClassPath includes those jars (see provided MANIFEST.MF).
 */
public class UWWKmeansNodeView extends NodeView<UWWKmeansNodeModel> {

    private final JPanel m_mainPanel;
    private final org.jfree.chart.ChartPanel m_chartPanel;
    private final JLabel m_status;

    private volatile SwingWorker<PreparedDatasets, Void> m_worker;

    // maximum number of points to render per series before downsampling in the background
    private static final int MAX_POINTS = 600;

    protected UWWKmeansNodeView(final UWWKmeansNodeModel nodeModel) {
        super(nodeModel);

        // create an empty JFreeChart with two empty datasets
        XYSeriesCollection emptyWcss = new XYSeriesCollection();
        XYSeriesCollection emptySil = new XYSeriesCollection();

        JFreeChart chart = ChartFactory.createXYLineChart(
            "KMeans diagnostics",
            "k",
            "WCSS",
            emptyWcss,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        chart.setPadding(new RectangleInsets(6, 6, 6, 6));
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(230, 230, 230));

        // primary renderer for WCSS (dataset 0)
        XYLineAndShapeRenderer renderer0 = new XYLineAndShapeRenderer(true, true);
        renderer0.setSeriesPaint(0, new Color(0, 102, 204));
        renderer0.setSeriesShapesVisible(0, true);
        plot.setRenderer(0, renderer0);
        plot.setDataset(0, emptyWcss);

        // secondary axis and dataset for silhouette (dataset 1)
        NumberAxis secondaryAxis = new NumberAxis("Avg Silhouette");
        secondaryAxis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(1, secondaryAxis);
        plot.setDataset(1, emptySil);
        plot.mapDatasetToRangeAxis(1, 1);

        XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer(true, true);
        renderer1.setSeriesPaint(0, new Color(255, 140, 0));
        renderer1.setSeriesShapesVisible(0, true);
        plot.setRenderer(1, renderer1);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        m_chartPanel = new org.jfree.chart.ChartPanel(chart);
        m_chartPanel.setMouseWheelEnabled(true);
        m_status = new JLabel("No data");
        m_mainPanel = new JPanel(new BorderLayout());
        m_mainPanel.add(m_chartPanel, BorderLayout.CENTER);
        m_mainPanel.add(m_status, BorderLayout.SOUTH);

        // register the panel with NodeView; do not override getComponent()
        setComponent(m_mainPanel);
    }

    @Override
    protected void modelChanged() {
        final UWWKmeansNodeModel model = getNodeModel();
        if (model == null) {
            cancelWorker();
            SwingUtilities.invokeLater(() -> {
                m_status.setText("No model");
                clearDatasets();
            });
            return;
        }

        // cancel any currently running worker
        cancelWorker();

        m_status.setText("Preparing chart...");
        m_worker = new SwingWorker<PreparedDatasets, Void>() {
            @Override
            protected PreparedDatasets doInBackground() throws Exception {
                // defensive copy of model maps (may be modified concurrently)
                Map<Integer, Double> wcssSrc = model.getWCSSData();
                Map<Integer, Double> silSrc = model.getSilhouetteData();

                Map<Integer, Double> wcssCopy = (wcssSrc != null) ? new LinkedHashMap<>(wcssSrc) : new LinkedHashMap<>();
                Map<Integer, Double> silCopy = (silSrc != null) ? new LinkedHashMap<>(silSrc) : new LinkedHashMap<>();

                // downsample if necessary
                wcssCopy = downsampleIfNeeded(wcssCopy, MAX_POINTS);
                silCopy = downsampleIfNeeded(silCopy, MAX_POINTS);

                // build JFreeChart datasets (off EDT)
                XYSeries wcssSeries = new XYSeries("WCSS");
                for (Map.Entry<Integer, Double> e : wcssCopy.entrySet()) {
                    // skip null values defensively
                    if (e.getValue() != null) {
                        wcssSeries.add(e.getKey(), e.getValue());
                    }
                }
                XYSeriesCollection wcssCollection = new XYSeriesCollection();
                wcssCollection.addSeries(wcssSeries);

                XYSeries silSeries = new XYSeries("Avg Silhouette");
                for (Map.Entry<Integer, Double> e : silCopy.entrySet()) {
                    if (e.getValue() != null) {
                        silSeries.add(e.getKey(), e.getValue());
                    }
                }
                XYSeriesCollection silCollection = new XYSeriesCollection();
                silCollection.addSeries(silSeries);

                return new PreparedDatasets(wcssCollection, silCollection);
            }

            @Override
            protected void done() {
                try {
                    PreparedDatasets ds = get(); // may throw CancellationException/ExecutionException
                    // update chart on EDT
                    SwingUtilities.invokeLater(() -> {
                        XYPlot plot = m_chartPanel.getChart().getXYPlot();
                        plot.setDataset(0, ds.wcss);
                        plot.setDataset(1, ds.silhouette);
                        // adjust axes auto ranges
                        plot.getRangeAxis(0).setAutoRange(true);
                        plot.getRangeAxis(1).setAutoRange(true);
                        m_status.setText("WCSS points: " + getSeriesItemCount(ds.wcss) + " | Silhouette points: " + getSeriesItemCount(ds.silhouette));
                    });
                } catch (InterruptedException | CancellationException e) {
                    // cancelled - no update
                    m_status.setText("Update cancelled");
                } catch (ExecutionException e) {
                    m_status.setText("Error preparing chart: " + e.getCause());
                }
            }
        };

        m_worker.execute();
    }

    @Override
    protected void onClose() {
        cancelWorker();
    }

    @Override
    protected void onOpen() {
        // Nothing special required; modelChanged will update as needed
    }

    private void cancelWorker() {
        SwingWorker<PreparedDatasets, Void> w = m_worker;
        if (w != null && !w.isDone()) {
            w.cancel(true);
        }
    }

    private void clearDatasets() {
        XYPlot plot = m_chartPanel.getChart().getXYPlot();
        plot.setDataset(0, new XYSeriesCollection());
        plot.setDataset(1, new XYSeriesCollection());
    }

    private static Map<Integer, Double> downsampleIfNeeded(Map<Integer, Double> src, int maxPoints) {
        if (src == null || src.size() <= maxPoints) return src;
        List<Integer> keys = new ArrayList<>(src.keySet());
        Collections.sort(keys);
        int total = keys.size();
        int bucketSize = (int) Math.ceil((double) total / maxPoints);
        LinkedHashMap<Integer, Double> out = new LinkedHashMap<>(maxPoints + 10);
        for (int i = 0; i < total; i += bucketSize) {
            int end = Math.min(i + bucketSize, total);
            double sum = 0;
            int count = 0;
            for (int j = i; j < end; j++) {
                Double v = src.get(keys.get(j));
                if (v != null) {
                    sum += v;
                    count++;
                }
            }
            double avg = (count > 0) ? (sum / count) : 0.0;
            out.put(keys.get(i), avg);
        }
        return out;
    }

    private static int getSeriesItemCount(XYSeriesCollection coll) {
        if (coll == null || coll.getSeriesCount() == 0) return 0;
        return coll.getSeries(0).getItemCount();
    }

    private static final class PreparedDatasets {
        final XYSeriesCollection wcss;
        final XYSeriesCollection silhouette;
        PreparedDatasets(XYSeriesCollection wcss, XYSeriesCollection silhouette) {
            this.wcss = wcss;
            this.silhouette = silhouette;
        }
    }
}
package org.uwwitscm.analytics.kmeans;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.knime.core.node.NodeView;

/**
 * Node view displaying WCSS (Elbow) and Silhouette plots using JFreeChart.
 */
public class UWWKmeansNodeView extends NodeView<UWWKmeansNodeModel> {

    private final JTabbedPane m_tabbedPane;
    private ChartPanel m_wcssChartPanel;
    private ChartPanel m_silhouetteChartPanel;

    protected UWWKmeansNodeView(final UWWKmeansNodeModel nodeModel) {
        super(nodeModel);

        m_tabbedPane = new JTabbedPane();

        // Create initial empty charts
        m_wcssChartPanel = new ChartPanel(createWCSSChart(null));
        m_silhouetteChartPanel = new ChartPanel(createSilhouetteChart(null));

        m_tabbedPane.addTab("Elbow (WCSS)", m_wcssChartPanel);
        m_tabbedPane.addTab("Silhouette Score", m_silhouetteChartPanel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(m_tabbedPane, BorderLayout.CENTER);

        setComponent(mainPanel);
    }

    private JFreeChart createWCSSChart(final Map<Integer, Double> wcssData) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        if (wcssData != null && !wcssData.isEmpty()) {
            XYSeries series = new XYSeries("WCSS");
            for (Map.Entry<Integer, Double> entry : wcssData.entrySet()) {
                series.add(entry.getKey().doubleValue(), entry.getValue());
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Elbow Method - WCSS",
                "Number of Clusters (k)",
                "Within-Cluster Sum of Squares",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        customizeChart(chart, new Color(0, 102, 204));
        return chart;
    }

    private JFreeChart createSilhouetteChart(final Map<Integer, Double> silhouetteData) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        if (silhouetteData != null && !silhouetteData.isEmpty()) {
            XYSeries series = new XYSeries("Silhouette");
            for (Map.Entry<Integer, Double> entry : silhouetteData.entrySet()) {
                series.add(entry.getKey().doubleValue(), entry.getValue());
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Silhouette Score",
                "Number of Clusters (k)",
                "Average Silhouette Coefficient",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        customizeChart(chart, new Color(0, 153, 51));
        return chart;
    }

    private void customizeChart(final JFreeChart chart, final Color lineColor) {
        chart.setBackgroundPaint(Color.WHITE);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // Reuse existing renderer if it's the right type, otherwise create new one
        XYLineAndShapeRenderer renderer;
        if (plot.getRenderer() instanceof XYLineAndShapeRenderer) {
            renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        } else {
            renderer = new XYLineAndShapeRenderer();
            plot.setRenderer(renderer);
        }
        renderer.setSeriesPaint(0, lineColor);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, true);
    }

    @Override
    protected void modelChanged() {
        final UWWKmeansNodeModel model = getNodeModel();
        if (model == null) {
            m_wcssChartPanel.setChart(createWCSSChart(null));
            m_silhouetteChartPanel.setChart(createSilhouetteChart(null));
            return;
        }

        Map<Integer, Double> wcssData = model.getWCSSData();
        m_wcssChartPanel.setChart(createWCSSChart(wcssData));

        Map<Integer, Double> silData = model.getSilhouetteData();
        m_silhouetteChartPanel.setChart(createSilhouetteChart(silData));
    }

    @Override
    protected void onClose() {
        // Dispose chart resources to prevent memory leaks
        if (m_wcssChartPanel != null) {
            JFreeChart chart = m_wcssChartPanel.getChart();
            if (chart != null) {
                chart.getPlot().dispose();
            }
        }
        if (m_silhouetteChartPanel != null) {
            JFreeChart chart = m_silhouetteChartPanel.getChart();
            if (chart != null) {
                chart.getPlot().dispose();
            }
        }
    }

    @Override
    protected void onOpen() {
        // Nothing special required
    }
}
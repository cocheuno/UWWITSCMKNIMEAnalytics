package org.uwwitscm.analytics.kmeans;

import java.awt.BorderLayout;
import java.awt.Font;
import java. util.Map;

import javax.swing. JPanel;
import javax. swing.JScrollPane;
import javax.swing.JTextArea;

import org.knime.core.node.NodeView;

/**
 * Simple text-based node view displaying WCSS and Silhouette data. 
 * JFreeChart version can be added later once dependencies are resolved.
 */
public class UWWKmeansNodeView extends NodeView<UWWKmeansNodeModel> {

    private final JPanel m_mainPanel;
    private final JTextArea m_textArea;

    protected UWWKmeansNodeView(final UWWKmeansNodeModel nodeModel) {
        super(nodeModel);

        m_textArea = new JTextArea();
        m_textArea.setEditable(false);
        m_textArea.setFont(new Font("Monospaced", Font. PLAIN, 12));

        m_mainPanel = new JPanel(new BorderLayout());
        m_mainPanel.add(new JScrollPane(m_textArea), BorderLayout.CENTER);

        setComponent(m_mainPanel);
    }

    @Override
    protected void modelChanged() {
        final UWWKmeansNodeModel model = getNodeModel();
        if (model == null) {
            m_textArea.setText("No model available.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== KMeans Clustering Results ===\n\n");

        // Display WCSS data
        Map<Integer, Double> wcssData = model.getWCSSData();
        if (wcssData != null && ! wcssData.isEmpty()) {
            sb.append("WCSS (Within-Cluster Sum of Squares):\n");
            sb.append(String.format("%-10s %s\n", "k", "WCSS"));
            sb.append("----------------------------\n");
            for (Map.Entry<Integer, Double> entry : wcssData.entrySet()) {
                sb.append(String.format("%-10d %. 4f\n", entry.getKey(), entry.getValue()));
            }
        } else {
            sb.append("No WCSS data available.\n");
        }

        sb.append("\n");

        // Display Silhouette data
        Map<Integer, Double> silData = model. getSilhouetteData();
        if (silData != null && !silData. isEmpty()) {
            sb.append("Average Silhouette Score:\n");
            sb.append(String.format("%-10s %s\n", "k", "Silhouette"));
            sb.append("----------------------------\n");
            for (Map.Entry<Integer, Double> entry : silData.entrySet()) {
                sb.append(String.format("%-10d %.4f\n", entry.getKey(), entry.getValue()));
            }
        } else {
            sb.append("No Silhouette data available.\n");
        }

        m_textArea. setText(sb.toString());
        m_textArea. setCaretPosition(0);
    }

    @Override
    protected void onClose() {
        // Nothing to clean up
    }

    @Override
    protected void onOpen() {
        // Nothing special required
    }
}
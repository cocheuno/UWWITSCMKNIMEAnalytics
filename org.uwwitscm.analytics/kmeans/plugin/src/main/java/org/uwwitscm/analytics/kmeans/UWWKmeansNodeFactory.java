package org.uwwitscm.analytics.kmeans;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class UWWKmeansNodeFactory extends NodeFactory<UWWKmeansNodeModel> {
    @Override public UWWKmeansNodeModel createNodeModel() { return new UWWKmeansNodeModel(); }
    @Override public int getNrNodeViews() { return 1; }
    @Override public NodeView<UWWKmeansNodeModel> createNodeView(final int viewIndex, final UWWKmeansNodeModel nodeModel) { return new UWWKmeansNodeView(nodeModel); }
    @Override public boolean hasDialog() { return true; }
    @Override public NodeDialogPane createNodeDialogPane() { return new UWWKmeansNodeDialog(); }
}
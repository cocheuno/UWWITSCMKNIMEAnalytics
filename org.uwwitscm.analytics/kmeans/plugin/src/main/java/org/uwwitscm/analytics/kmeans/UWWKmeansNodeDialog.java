package org.uwwitscm.analytics.kmeans;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

public class UWWKmeansNodeDialog extends DefaultNodeSettingsPane {
    protected UWWKmeansNodeDialog() {
        super();
        createNewGroup("Analysis Range");
        addDialogComponent(new DialogComponentNumber(new SettingsModelInteger(UWWKmeansNodeModel.CFGKEY_MIN_K, 2), "Min k:", 1));
        addDialogComponent(new DialogComponentNumber(new SettingsModelInteger(UWWKmeansNodeModel.CFGKEY_MAX_K, 10), "Max k:", 1));
        closeCurrentGroup();
        createNewGroup("Output Selection");
        addDialogComponent(new DialogComponentNumber(new SettingsModelInteger(UWWKmeansNodeModel.CFGKEY_SOLUTION_K, 3), "Chosen k:", 1));
        closeCurrentGroup();
    }
}
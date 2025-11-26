package org.uwwitscm.analytics.kmeans;

import org.knime.core.node.defaultnodesettings.*;

public class UWWKmeansNodeDialog extends DefaultNodeSettingsPane {
    protected UWWKmeansNodeDialog() {
        // Add your real settings here later
        createNewGroup("UWW KMeans Configuration");
        addDialogComponent(new DialogComponentStringSelection(
            new SettingsModelString("placeholder", "Ready"), "Status:", "Ready"));
    }
}
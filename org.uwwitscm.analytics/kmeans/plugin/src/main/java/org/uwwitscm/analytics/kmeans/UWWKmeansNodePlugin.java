package org.uwwitscm.analytics.kmeans;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class UWWKmeansNodePlugin extends Plugin {
    private static UWWKmeansNodePlugin plugin;

    public UWWKmeansNodePlugin() { plugin = this; }

    @Override public void start(BundleContext context) throws Exception { super.start(context); }
    @Override public void stop(BundleContext context) throws Exception { super.stop(context); plugin = null; }

    public static UWWKmeansNodePlugin getDefault() { return plugin; }
}
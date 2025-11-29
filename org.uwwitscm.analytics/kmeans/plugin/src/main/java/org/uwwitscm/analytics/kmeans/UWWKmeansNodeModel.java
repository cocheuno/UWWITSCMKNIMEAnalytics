package org.uwwitscm.analytics.kmeans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

public class UWWKmeansNodeModel extends NodeModel {

    static final String CFGKEY_MIN_K = "min_k";
    static final String CFGKEY_MAX_K = "max_k";
    static final String CFGKEY_SOLUTION_K = "solution_k";

    private final SettingsModelInteger m_min_k = new SettingsModelInteger(CFGKEY_MIN_K, 2);
    private final SettingsModelInteger m_max_k = new SettingsModelInteger(CFGKEY_MAX_K, 10);
    private final SettingsModelInteger m_solution_k = new SettingsModelInteger(CFGKEY_SOLUTION_K, 3);

    private Map<Integer, Double> m_wcssMap = new LinkedHashMap<>();
    private Map<Integer, Double> m_silMap = new LinkedHashMap<>();

    private static final NodeLogger logger = NodeLogger.getLogger(UWWKmeansNodeModel.class);

    protected UWWKmeansNodeModel() {
        super(1, 4);
    }

    public Map<Integer, Double> getWCSSData() { return m_wcssMap; }
    public Map<Integer, Double> getSilhouetteData() { return m_silMap; }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[0] == null) throw new InvalidSettingsException("Input table spec is missing.");

        if (m_min_k.getIntValue() < 2) throw new InvalidSettingsException("Min k must be at least 2.");
        if (m_max_k.getIntValue() <= m_min_k.getIntValue()) throw new InvalidSettingsException("Max k must be greater than Min k.");

        DataColumnSpec[] oldCols = new DataColumnSpec[inSpecs[0].getNumColumns()];
        for (int i = 0; i < inSpecs[0].getNumColumns(); i++) oldCols[i] = inSpecs[0].getColumnSpec(i);
        DataColumnSpec[] port0Cols = Arrays.copyOf(oldCols, oldCols.length + 1);
        port0Cols[oldCols.length] = new DataColumnSpecCreator("Cluster", StringCell.TYPE).createSpec();
        DataTableSpec spec0 = new DataTableSpec(port0Cols);

        DataTableSpec spec1 = new DataTableSpec(new DataColumnSpecCreator("k", IntCell.TYPE).createSpec(), new DataColumnSpecCreator("WCSS", DoubleCell.TYPE).createSpec());
        DataTableSpec spec2 = new DataTableSpec(new DataColumnSpecCreator("k", IntCell.TYPE).createSpec(), new DataColumnSpecCreator("Avg_Silhouette", DoubleCell.TYPE).createSpec());

        List<DataColumnSpec> centerCols = new ArrayList<>();
        centerCols.add(new DataColumnSpecCreator("k", IntCell.TYPE).createSpec());
        centerCols.add(new DataColumnSpecCreator("Cluster", StringCell.TYPE).createSpec());
        for (int i=0; i<inSpecs[0].getNumColumns(); i++) {
            if (inSpecs[0].getColumnSpec(i).getType().isCompatible(DoubleValue.class)) {
                centerCols.add(new DataColumnSpecCreator("Center_" + inSpecs[0].getColumnSpec(i).getName(), DoubleCell.TYPE).createSpec());
            }
        }
        DataTableSpec spec3 = new DataTableSpec(centerCols.toArray(new DataColumnSpec[0]));

        return new DataTableSpec[]{spec0, spec1, spec2, spec3};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        m_wcssMap.clear();
        m_silMap.clear();
        
        BufferedDataTable inputTable = inData[0];
        DataTableSpec inSpec = inputTable.getDataTableSpec(); 
        
        List<double[]> dataPoints = new ArrayList<>();
        List<RowKey> validKeys = new ArrayList<>(); 
        List<Integer> numericIndices = new ArrayList<>();
        
        for(int i=0; i<inSpec.getNumColumns(); i++) {
            if (inSpec.getColumnSpec(i).getType().isCompatible(DoubleValue.class)) numericIndices.add(i);
        }
        if (numericIndices.isEmpty()) throw new InvalidSettingsException("No numeric columns found in input!");
        int dims = numericIndices.size();
        
        for (DataRow row : inputTable) {
            double[] vec = new double[dims];
            boolean hasMissing = false;
            for (int i = 0; i < dims; i++) {
                DataCell cell = row.getCell(numericIndices.get(i));
                if (cell.isMissing()) { hasMissing = true; break; }
                vec[i] = ((DoubleValue)cell).getDoubleValue();
            }
            if (!hasMissing) { dataPoints.add(vec); validKeys.add(row.getKey()); }
        }
        
        if (dataPoints.isEmpty()) throw new InvalidSettingsException("Input table is empty or all rows contained missing values!");

        DataTableSpec[] outSpecs = configure(new DataTableSpec[]{inSpec});
        BufferedDataContainer container0 = exec.createDataContainer(outSpecs[0]); 
        BufferedDataContainer container1 = exec.createDataContainer(outSpecs[1]); 
        BufferedDataContainer container2 = exec.createDataContainer(outSpecs[2]); 
        BufferedDataContainer container3 = exec.createDataContainer(outSpecs[3]); 

        int minK = m_min_k.getIntValue();
        int maxK = m_max_k.getIntValue();
        int solutionK = m_solution_k.getIntValue();
        Map<RowKey, Integer> solutionAssignments = new HashMap<>();

        int startLoop = Math.min(minK, solutionK);
        int endLoop = Math.max(maxK, solutionK);

        for (int kLoop = startLoop; kLoop <= endLoop; kLoop++) {
            boolean isInRange = (kLoop >= minK && kLoop <= maxK);
            boolean isSolution = (kLoop == solutionK);
            if (!isInRange && !isSolution) continue;

            exec.checkCanceled();
            exec.setProgress((double)(kLoop - startLoop) / (endLoop - startLoop), "Clustering k=" + kLoop);

            KMeansResult r = runKMeansPlusPlus(dataPoints, kLoop, 100);

            if (isInRange) {
                double wcss = 0;
                for(int i=0; i<dataPoints.size(); i++) wcss += Math.pow(euclideanDistance(dataPoints.get(i), r.centroids.get(r.assignments[i])), 2);
                m_wcssMap.put(kLoop, wcss);
                container1.addRowToTable(new DefaultRow("k_"+kLoop, new IntCell(kLoop), new DoubleCell(wcss)));

                double avgSil = calculateAverageSilhouette(dataPoints, r.assignments, r.centroids);
                m_silMap.put(kLoop, avgSil);
                container2.addRowToTable(new DefaultRow("k_"+kLoop, new IntCell(kLoop), new DoubleCell(avgSil)));

                for (int c=0; c<kLoop; c++) {
                    DataCell[] cells = new DataCell[2 + dims];
                    cells[0] = new IntCell(kLoop);
                    cells[1] = new StringCell("Cluster_" + c);
                    for(int d=0; d<dims; d++) cells[2+d] = new DoubleCell(r.centroids.get(c)[d]);
                    container3.addRowToTable(new DefaultRow("k"+kLoop+"_c"+c, cells));
                }
            }
            if (isSolution) {
                for(int i=0; i<validKeys.size(); i++) solutionAssignments.put(validKeys.get(i), r.assignments[i]);
            }
        }

        container1.close(); container2.close(); container3.close();

        for (DataRow row : inputTable) {
            DataCell[] cells0 = new DataCell[row.getNumCells() + 1];
            for (int i=0; i<row.getNumCells(); i++) cells0[i] = row.getCell(i);
            if (solutionAssignments.containsKey(row.getKey())) {
                cells0[row.getNumCells()] = new StringCell("Cluster_" + solutionAssignments.get(row.getKey()));
            } else {
                cells0[row.getNumCells()] = new StringCell("Skipped");
            }
            container0.addRowToTable(new DefaultRow(row.getKey(), cells0));
        }
        container0.close();

        return new BufferedDataTable[]{container0.getTable(), container1.getTable(), container2.getTable(), container3.getTable()};
    }

    private static class KMeansResult {
        List<double[]> centroids;
        int[] assignments;
        public KMeansResult(List<double[]> c, int[] a) { centroids=c; assignments=a; }
    }

    private KMeansResult runKMeansPlusPlus(List<double[]> data, int k, int maxIter) {
        Random rand = new Random(12345); 
        List<double[]> centroids = new ArrayList<>();
        if (!data.isEmpty()) centroids.add(data.get(rand.nextInt(data.size())));

        for (int c = 1; c < k; c++) {
            double[] distSq = new double[data.size()];
            double sumDistSq = 0;
            for (int i = 0; i < data.size(); i++) {
                double minD = Double.MAX_VALUE;
                for (double[] center : centroids) {
                    double d = euclideanDistance(data.get(i), center);
                    if (d < minD) minD = d;
                }
                distSq[i] = minD * minD;
                sumDistSq += distSq[i];
            }
            double r = rand.nextDouble() * sumDistSq;
            double curSum = 0;
            for (int i = 0; i < data.size(); i++) {
                curSum += distSq[i];
                if (curSum >= r) { centroids.add(data.get(i)); break; }
            }
            if (centroids.size() < c + 1 && !data.isEmpty()) centroids.add(data.get(data.size()-1));
        }

        int[] assignments = new int[data.size()];
        boolean changed = true;
        int iter = 0;
        while(changed && iter < maxIter) {
            changed = false;
            for(int i=0; i<data.size(); i++) {
                int best = getNearest(data.get(i), centroids);
                if(assignments[i] != best) { assignments[i] = best; changed = true; }
            }
            if(changed) {
                double[][] sums = new double[k][data.get(0).length];
                int[] counts = new int[k];
                for(int i=0; i<data.size(); i++) {
                    int c = assignments[i];
                    counts[c]++;
                    for(int d=0; d<data.get(0).length; d++) sums[c][d] += data.get(i)[d];
                }
                List<double[]> nextCentroids = new ArrayList<>();
                for(int c=0; c<k; c++) {
                    double[] center = new double[data.get(0).length];
                    if(counts[c] > 0) {
                        for(int d=0; d<center.length; d++) center[d] = sums[c][d] / counts[c];
                    } else { center = centroids.get(c); }
                    nextCentroids.add(center);
                }
                centroids = nextCentroids;
            }
            iter++;
        }
        return new KMeansResult(centroids, assignments);
    }

    private int getNearest(double[] p, List<double[]> centroids) {
        int best = 0; double min = Double.MAX_VALUE;
        for(int i=0; i<centroids.size(); i++) {
            double d = euclideanDistance(p, centroids.get(i));
            if(d < min) { min = d; best = i; }
        }
        return best;
    }

    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0;
        for(int i=0; i<a.length; i++) sum += Math.pow(a[i]-b[i], 2);
        return Math.sqrt(sum);
    }

    private double calculateAverageSilhouette(List<double[]> data, int[] assignments, List<double[]> centroids) {
        int k = centroids.size();
        if (k < 2) return 0.0;
        double sumSil = 0; int count = 0;
        for (int i = 0; i < data.size(); i++) {
            int myCluster = assignments[i];
            double[] clusterDists = new double[k];
            int[] clusterCounts = new int[k];
            for (int j = 0; j < data.size(); j++) {
                if (i == j) continue;
                double dist = euclideanDistance(data.get(i), data.get(j));
                clusterDists[assignments[j]] += dist;
                clusterCounts[assignments[j]]++;
            }
            if (clusterCounts[myCluster] == 0) continue; 
            double a = clusterDists[myCluster] / clusterCounts[myCluster];
            double minB = Double.MAX_VALUE;
            for (int c = 0; c < k; c++) {
                if (c == myCluster) continue;
                if (clusterCounts[c] > 0) {
                    double b = clusterDists[c] / clusterCounts[c];
                    if (b < minB) minB = b;
                }
            }
            if (minB == Double.MAX_VALUE) minB = 0;
            double maxVal = Math.max(a, minB);
            double sil = (maxVal > 0) ? (minB - a) / maxVal : 0;
            sumSil += sil;
            count++;
        }
        return (count > 0) ? sumSil / count : 0.0;
    }

    @Override protected void saveSettingsTo(final NodeSettingsWO settings) { m_min_k.saveSettingsTo(settings); m_max_k.saveSettingsTo(settings); m_solution_k.saveSettingsTo(settings); }
    @Override protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException { m_min_k.loadSettingsFrom(settings); m_max_k.loadSettingsFrom(settings); m_solution_k.loadSettingsFrom(settings); }
    @Override protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException { m_min_k.validateSettings(settings); m_max_k.validateSettings(settings); m_solution_k.validateSettings(settings); }
    @Override protected void loadInternals(java.io.File nodeInternDir, org.knime.core.node.ExecutionMonitor exec) {}
    @Override protected void saveInternals(java.io.File nodeInternDir, org.knime.core.node.ExecutionMonitor exec) {}
    @Override protected void reset() { m_wcssMap.clear(); m_silMap.clear(); }
}
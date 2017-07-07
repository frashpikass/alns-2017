/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Random;
import solverModel.Cluster;

/**
 * This class holds a list of clusters and associates a probability value to
 * each one of them.
 * <br>When queried, this class picks clusters, one by one, and does a
 * probability test: if a cluster passes its own probability test, it's put in
 * the return list.
 * <br>The initial probability value of each cluster is 1.0.
 * <br>Methods in this class allow for a selective rescaling of the probability
 * of each cluster.
 * <br>In particular, one could increase (upscale) the probability by a gamma
 * factor, included between [0,1], in this fashion:
 * <br><i>newProbability</i> =
 * <i>gamma</i>*<i>oldProbability</i>+(1-<i>gamma</i>)
 * <br>or one could decrease (downscale) the probability by the same gamma
 * factor:
 * <br><i>newProbability</i> = <i>gamma</i>*<i>oldProbability</i>
 * <br>This class is used to implement some sort of taboo mechanism into ALNS,
 * to avoid going back to infeasible solutions.
 *
 * @author Frash
 */
public class ClusterRoulette {

    /**
     * List of clusters available to pick
     */
    private List<Cluster> clusters;

    /**
     * List of probabilities (one for each cluster)
     */
    private List<Double> probabilities;

    /**
     * List of frequencies of clusters being below average. Every time it's
     * updated, if a cluster has a probability below average, its entry is
     * increased by one.
     */
    private List<Double> nerfOccurrences;

    /**
     * Indicates how many times the nerf list was updated.
     */
    private double nerfNumberOfUpdates;

    /**
     * Constructor for class ClusterRoulette.
     * <br>Probabilities of extraction are initialized to 1.0.
     *
     * @param clusters list of clusters to start from
     */
    public ClusterRoulette(List<Cluster> clusters) {
        if (clusters != null && !clusters.isEmpty()) {
            this.clusters = clusters;
        } else {
            this.clusters = new ArrayList<>();
        }
        this.probabilities = new ArrayList<>();

        // Init probabilities to 1.0
        if (clusters != null && !clusters.isEmpty()) {
            clusters.forEach((_item) -> {
                probabilities.add(1.0);
            });
        }

        // Init nerf mechanism
        this.nerfOccurrences = new ArrayList<>();
        this.nerfNumberOfUpdates = 0;
        clusters.forEach((_item) -> {
            nerfOccurrences.add(0.0);
        });
    }

    /**
     * Returns a list of clusters, selected according to their chance of being
     * selected.
     *
     * @return the filtered list of clusters
     */
    public List<Cluster> query() {
        List<Cluster> ret = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i < clusters.size(); i++) {
            if (r.nextDouble() <= probabilities.get(i)) {
                ret.add(clusters.get(i));
            }
        }
        return ret;
    }

    /**
     * Returns a list of those clusters whose chance of being selected is more
     * than or equal the barrier value (high pass filter).
     *
     * @param barrier a value in range [0,1] to filter low cluster probabilities
     * @return the filtered list of clusters
     */
    public List<Cluster> queryHighPass(double barrier) {
        List<Cluster> ret = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            if (probabilities.get(i) >= barrier) {
                ret.add(clusters.get(i));
            }
        }
        return ret;
    }

    /**
     * Upscales the selected list of clusters by a factor gamma according to
     * <br><i>newProbability</i> =
     * <i>gamma</i>*<i>oldProbability</i>+(1-<i>gamma</i>) .
     * <br>If the clusters in the list are not in this structure, they are
     * ignored.
     *
     * @param gamma the scaling factor (double in range [0,1])
     * @param toUpdate the list of clusters to update
     */
    public void upscale(double gamma, List<Cluster> toUpdate) {
        gamma = gammaFilter(gamma);
        if (toUpdate != null && !toUpdate.isEmpty()) {
            for (Cluster t : toUpdate) {
                if (clusters.contains(t)) {
                    int index = this.clusters.indexOf(t);
                    probabilities.set(index, probabilities.get(index) * gamma + (1.0 - gamma));
                }
            }
        }
    }

    /**
     * Downscales the selected list of clusters by a factor gamma according to
     * <br><i>newProbability</i> = <i>gamma</i>*<i>oldProbability</i>.
     * <br>If the clusters in the list are not in this structure, they are
     * ignored.
     *
     * @param gamma the scaling factor (double in range [0,1])
     * @param toUpdate the list of clusters to update
     */
    public void downscale(double gamma, List<Cluster> toUpdate) {
        gamma = gammaFilter(gamma);
        if (toUpdate != null && !toUpdate.isEmpty()) {
            for (Cluster t : toUpdate) {
                if (clusters.contains(t)) {
                    int index = clusters.indexOf(t);
                    probabilities.set(index, probabilities.get(index) * gamma);
                }
            }
        }
    }

    /**
     * Filters the gamma parameter to make sure it's a double in range [0,1].
     *
     * @param gamma the scaling factor to bound (double in range [0,1])
     * @return the clipped scaling factor
     */
    private double gammaFilter(double gamma) {
        double ret = gamma;
        if (gamma <= 0.0) {
            ret = 0.0;
        } else if (gamma >= 1.0) {
            ret = 1.0;
        }
        return ret;
    }

    /**
     * A hot cluster is a cluster which has been just selected. The cooldown
     * process makes it a little less likely to select a hot cluster again.
     *
     * <br>The <i>cooldownGamma</i> factor is applied in a downscaling fashion
     * to hot clusters.
     *
     * @param cooldownGamma the cooldown factor. Should be a small double in
     * range [0,1]. Represents the decrease in probability,
     * <br>e.g.: <i>cooldownGamma</i> = 0.1
     * <br> =&gt; the new probability for hot clusters will be 10% less than the
     * old one
     * @param hotClusters clusters that have just been chosen
     */
    public void cooldown(double cooldownGamma, List<Cluster> hotClusters) {
        // Make sure the cooldown gamma is in range [0,1]
        cooldownGamma = gammaFilter(cooldownGamma);

        // Cooldown hot clusters
        this.downscale((1 - cooldownGamma), hotClusters);
    }

    /**
     * A hot cluster is a cluster which has been just selected. Other
     * non-selected clusters are called cold clusters.
     *
     * <br>The warmup process makes it a little more likely to chose a cold
     * cluster in the next extraction.
     *
     * <br>The <i>warmupGamma</i> factor is applied in an upscaling fashion to
     * cold clusters.
     *
     * @param warmupGamma the warmup factor. Should be a small double in range
     * [0,1]. Represents the increase in probability,
     * <br>e.g.: <i>warmupGamma</i> = 0.1
     * <br> =&gt; the new probability for cold clusters will be 10% more than
     * the old one
     * @param hotClusters clusters that have just been chosen
     */
    public void warmup(double warmupGamma, List<Cluster> hotClusters) {
        // Make sure the cooldown gamma is in range [0,1]
        warmupGamma = gammaFilter(warmupGamma);

        // Make a list of cold clusters
        List<Cluster> coldClusters = new ArrayList<>(clusters);
        if (hotClusters != null && !hotClusters.isEmpty()) {
            coldClusters.removeAll(hotClusters);
        }

        // Warm up cold clusters
        this.upscale((1 - warmupGamma), coldClusters);
    }

    /**
     * Computes the average probability for a cluster to be chosen.
     *
     * @return the average probability for a cluster to be chosen if the list of
     * clusters has some elements; 0.5 otherwise.
     */
    public double getAverageProbability() {
        OptionalDouble ret = probabilities.stream().mapToDouble(a -> a).average();
        if (ret.isPresent()) {
            return ret.getAsDouble();
        } else {
            return 0.5;
        }
    }

    /**
     * Returns a string representation of clusters in this structure with their
     * respective probability.
     *
     * @return a string representation of this ClusterRoulette.
     */
    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer();
        double avgProbability = this.getAverageProbability();

        for (int i = 0; i < clusters.size(); i++) {
            ret.append(" " + clusters.get(i) + ":" + probabilities.get(i));
            if (probabilities.get(i) >= avgProbability) {
                ret.append("!");
            }
        }

        return ret.toString();
    }

    /**
     * Return a string representation of the nerf occurrences list
     *
     * @return a string representation of the nerf occurrences list
     */
    public String nerfOccurrencesString() {
        StringBuffer ret = new StringBuffer();

        for (int i = 0; i < clusters.size(); i++) {
            ret.append(nerfOccurrences.get(i) + " ");
        }

        return ret.toString();
    }

    /**
     * Resets every nerf candidate occurrence counter to 0. Also resets to 0 the
     * counter for the number of updates.
     */
    public void resetNerfOccurrences() {
        for (int i = 0; i < clusters.size(); i++) {
            nerfOccurrences.set(i, 0.0);
        }
        nerfNumberOfUpdates = 0.0;
    }
    
    /**
     * Set the probability of being chosen to 0 for nerf candidates,
     * to 1 to other clusters.
     * 
     * @param nerfBarrier double in range [0,1]
     */
    public void punishNerfCandidatesAndResetOthers(double nerfBarrier){
        nerfBarrier = gammaFilter(nerfBarrier);
        
        List<Cluster> nerfCandidates = this.queryNerfCandidates(nerfBarrier);
        
        for(int i = 0; i < clusters.size(); i++){
            if(nerfCandidates.contains(clusters.get(i))){
                probabilities.set(i, 0.0);
            }
            else probabilities.set(i, 1.0);
        }
        
    }

    /**
     * Updates the list of nerf candidates by counting whether in this update
     * the candidate's probability was below average. Also updates the counter
     * for the number of updates.
     */
    public void updateNerfOccurrences() {
        double avgProbability = this.getAverageProbability();
        this.nerfNumberOfUpdates += 1.0;

        for (int i = 0; i < clusters.size(); i++) {
            if (probabilities.get(i) < avgProbability) {
                nerfOccurrences.set(i, nerfOccurrences.get(i) + 1.0);
            }
        }
    }

    /**
     * Queries the nerf candidate list and returns the clusters which have been
     * less probable than the average for more than nerfBarrier% of the times
     * recorded.
     *
     * @param nerfBarrier double in range [0,1]
     * @return a list of improbable clusters
     */
    public List<Cluster> queryNerfCandidates(double nerfBarrier) {
        nerfBarrier = gammaFilter(nerfBarrier);
        List<Cluster> ret = new ArrayList<>();

        for (int i = 0; i < clusters.size(); i++) {
            if (nerfOccurrences.get(i) / nerfNumberOfUpdates >= nerfBarrier) {
                ret.add(clusters.get(i));
            }
        }

        return ret;
    }

    /**
     * Queries the nerf candidate list and returns the clusters which have been
     * more probable than the average for more than nerfBarrier% of the times
     * recorded.
     *
     * @param nerfBarrier double in range [0,1]
     * @return a list of improbable clusters
     */
    public List<Cluster> queryNotNerfCandidates(double nerfBarrier) {
        nerfBarrier = gammaFilter(nerfBarrier);
        List<Cluster> ret = new ArrayList<>();

        for (int i = 0; i < clusters.size(); i++) {
            if (nerfOccurrences.get(i) / nerfNumberOfUpdates < nerfBarrier) {
                ret.add(clusters.get(i));
            }
        }

        return ret;
    }

}

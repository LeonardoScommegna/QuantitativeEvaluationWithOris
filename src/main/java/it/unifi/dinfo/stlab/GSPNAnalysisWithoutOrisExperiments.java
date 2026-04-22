package it.unifi.dinfo.stlab;

import it.unifi.dinfo.stlab.engineeredModules.SimpleGSPN;
import it.unifi.dinfo.stlab.utils.ChartUtility;

public class GSPNAnalysisWithoutOrisExperiments {
    public static void main(String[] args) {


        int queueSize = 16;
        int poolSize = 2;
        SimpleGSPN model = new SimpleGSPN.Builder(40./100., 25./100.)
                .setQueueSize(queueSize)
                .setPoolSize(poolSize)
                .build();



        // Steady State Analysis
        model.steadyStateAnalysis();
        System.out.println("Steady State Queued Request:  " + model.getSteadyStateQueue() );
        System.out.println("Steady State Rejection Rate:  " + model.getSteadyStateRejectionRate());
        System.out.println("Steady State Pool Unusage:    " + model.getSteadyStatePoolUnusage());

        // Transient Analysis
        double timestep = 0.1;
        model.transientAnalysis(350, timestep);

        java.util.Map<String, double[]> seriesMap = new java.util.LinkedHashMap<>();
        seriesMap.put("Queue Reward", model.getTransientQueue(timestep));
        seriesMap.put("Rejection Rate", model.getTransientRejectionRate(timestep));
        seriesMap.put("Unusage Reward", model.getTransientPoolUnusage(timestep));

        // PLOT
        ChartUtility.plotData(seriesMap, 0.0, timestep, "GSPN Transient Analysis");

        String  reward1 = "If(Queue> " + poolSize+ ",Queue-" + poolSize+ ",0)";
        String  reward2 = "If(Queue=="+ queueSize+",1,0)";
        String  reward3 = "If(Queue<"+ poolSize +", "+poolSize+"-Queue,0)";

        model.simulate(300, 0.1, 1, reward1, reward2, reward3 );
    }
}

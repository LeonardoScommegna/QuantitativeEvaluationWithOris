package it.unifi.dinfo.stlab;

import it.unifi.dinfo.stlab.engineeredModules.NonMarkovianModel;
import it.unifi.dinfo.stlab.utils.ChartUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RejuvenationTimeOptimization {
    public static void main(String[] args) {

        analyzeTimeRejuvenation(10, 500, 20);

    }

    public static void analyzeTimeRejuvenation(double minTimer, double maxTimer, double step){

        List<Double> unreliabilityPoints = new ArrayList<>();
        List<Double> unavailabilityPoints = new ArrayList<>();
        List<Double> unusefulUnavailabilityPoints = new ArrayList<>();
        for(double timer = minTimer; timer < maxTimer; timer+=step){
            NonMarkovianModel model = new NonMarkovianModel.Builder(timer).build();
            Map<String, Double> steadyStateMap = model.regenerativeSteadyState(model.getUnusefulUnavailabilityRewardString(), model.getUnavailabilityRewardString(), model.getUnreliabilityRewardString());
            unreliabilityPoints.add(steadyStateMap.get(model.getUnreliabilityRewardString()));
            unavailabilityPoints.add(steadyStateMap.get(model.getUnavailabilityRewardString()));
            unusefulUnavailabilityPoints.add(steadyStateMap.get(model.getUnusefulUnavailabilityRewardString()));
        }

        Map<String, double[]> dataSeries = new HashMap<>();
        dataSeries.put("Unreliability", unreliabilityPoints.stream().mapToDouble(Double::doubleValue).toArray());
        dataSeries.put("Unavailability", unavailabilityPoints.stream().mapToDouble(Double::doubleValue).toArray());
        dataSeries.put("Unuseful Unavailability", unusefulUnavailabilityPoints.stream().mapToDouble(Double::doubleValue).toArray());

        ChartUtility.plotData(dataSeries, minTimer, step, "Steady State Reward with waitClock Variations");
    }
}

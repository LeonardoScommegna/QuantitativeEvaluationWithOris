package it.unifi.dinfo.stlab;

import it.unifi.dinfo.stlab.engineeredModules.SimpleGSPN;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GSPNParameterOptimizationExperiment {

    public static void main(String[] args) {

        PoolDimensioningForMinimalRejectionRate(40, 10, 9);
        PoolDimensioningCombined(40, 10, 0.6,0.4, 9);



    }

    public static int PoolDimensioningCombined(double arrivalRate, double serviceRate, double weightRejection, double weightInefficiency, int maxPoolSize) {
        Map<Integer,Double> combinations = new HashMap<>();
        Map<Integer,Double> rejectionRatesMap = new HashMap<>();
        Map<Integer,Double> unusedResMap = new HashMap<>();

        for(int poolsize = 1; poolsize <= maxPoolSize; poolsize++) {
            SimpleGSPN model = new SimpleGSPN.Builder(arrivalRate, serviceRate)
                    .setPoolSize(poolsize)
                    .build();
            model.steadyStateAnalysis();
            double rejRate = model.getSteadyStateRejectionRate().doubleValue();
            double unusedRes = model.getSteadyStatePoolUnusage().doubleValue();
            combinations.put(poolsize, rejRate*weightRejection + unusedRes*weightInefficiency);
            rejectionRatesMap.put(poolsize, rejRate);
            unusedResMap.put(poolsize, unusedRes);
        }


        Integer best = combinations.entrySet()
                .stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        System.out.println("Best PoolSize for combined rewards: " + best);
        System.out.println("Avg unused res: " + unusedResMap.get(best) );
        System.out.println("Avg rejection rate: " + rejectionRatesMap.get(best) );


        return best;
    }

    public static int PoolDimensioningForMinimalRejectionRate(double arrivalRate, double serviceRate, int maxPoolSize) {
        Map<Integer,BigDecimal> rejectionRates = new HashMap<>();
        Map<Integer,Double> unusedResMap = new HashMap<>();
        for(int poolsize = 1; poolsize <= maxPoolSize; poolsize++) {
            SimpleGSPN model = new SimpleGSPN.Builder(arrivalRate, serviceRate)
                    .setPoolSize(poolsize)
                    .build();
            model.steadyStateAnalysis();
            rejectionRates.put(poolsize, model.getSteadyStateRejectionRate());
            unusedResMap.put(poolsize, model.getSteadyStatePoolUnusage().doubleValue());
        }

        Integer best = rejectionRates.entrySet()
                .stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        System.out.println("Best PoolSize: " + best);
        System.out.println("Avg unused res: " + unusedResMap.get(best) );
        System.out.println("Avg rejection rate: " + rejectionRates.get(best) );
        return best;
    }

    public static void simpleRateAnalysis(){
        List<Double> rates = List.of(10., 20., 30., 40.);
        for (double arrivalRate : rates) {
            for (double serviceRate : rates) {
                SimpleGSPN model = new SimpleGSPN.Builder(arrivalRate, serviceRate).build();
                model.steadyStateAnalysis();
                System.out.println("Arrival Rate:  " + arrivalRate + " -- Service Rate: " + serviceRate );
                System.out.println("Steady State Queued Request:  " + model.getSteadyStateQueue() );
                System.out.println("Steady State Rejection Rate:  " + model.getSteadyStateRejectionRate());
                System.out.println("Steady State Pool Unusage:    " + model.getSteadyStatePoolUnusage());
            }
        }
    }
}

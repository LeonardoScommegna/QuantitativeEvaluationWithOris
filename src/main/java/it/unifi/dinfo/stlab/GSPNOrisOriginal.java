package it.unifi.dinfo.stlab;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import it.unifi.dinfo.stlab.utils.ChartUtility;
import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.SteadyStateSolution;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.EnablingFunction;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;

public class GSPNOrisOriginal {
    public static void build(PetriNet net, Marking marking) {

        //Generating Nodes
        Place ARate = net.addPlace("ARate");
        Place PoolSize = net.addPlace("PoolSize");
        Place Queue = net.addPlace("Queue");
        Place QueueSize = net.addPlace("QueueSize");
        Place SRate = net.addPlace("SRate");
        Transition arrival = net.addTransition("arrival");
        Transition reject = net.addTransition("reject");
        Transition service = net.addTransition("service");

        //Generating Connectors
        net.addPrecondition(Queue, reject);
        net.addPrecondition(Queue, service);
        net.addPostcondition(arrival, Queue);

        //Generating Properties
        marking.setTokens(ARate, 40);
        marking.setTokens(PoolSize, 2);
        marking.setTokens(Queue, 0);
        marking.setTokens(QueueSize, 16);
        marking.setTokens(SRate, 25);
        arrival.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("1.0*ARate/100", net)));
        reject.addFeature(new EnablingFunction("Queue>QueueSize"));
        reject.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        reject.addFeature(new Priority(0));
        service.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("1.0*SRate/100*min(PoolSize,Queue)", net)));

    }


    // ADDED
    public static void main(String[] args) {


        PetriNet net = new PetriNet();
        Marking marking = new Marking();
        build(net, marking);

        RewardRate queueReward = RewardRate.fromString("If(Queue>PoolSize,Queue-PoolSize,0)");
        RewardRate rejectionRateReward = RewardRate.fromString("If(Queue==QueueSize,1*ARate/(100),0)");
        RewardRate unusageReward = RewardRate.fromString("If(Queue<PoolSize, PoolSize-Queue,0)");

        double step = 0.1; // the discretization step
        double limit = 350.0; // the time limit
        Pair<Map<Marking, Integer>, double[][]> result = GSPNTransient.builder()
//                .error() // the truncation error for fox and glynn algorithm
                .timePoints(0.0, limit , step)
                .build()
                .compute(net, marking);

        Map<Marking, Integer> statePos = result.first();   // marking -> column index
        double[][] probs = result.second();                // probs[t][stateIndex]

        // 2. Reward Evaluation for each timestep
        int numTimePoints = probs.length;

        double[] queueValues = new double[numTimePoints];
        double[] rejectionValues = new double[numTimePoints];
        double[] unusageValues = new double[numTimePoints];

        // 3. Expected value of the rewards in each timestep
        double currentTime = 0.0;

        for (int t = 0; t < numTimePoints; t++) {

            // Cycle on all the markings
            for (Map.Entry<Marking, Integer> entry : statePos.entrySet()) {
                Marking m = entry.getKey();
                int stateIndex = entry.getValue();

                // Prob of being at marking m at time t
                double prob = probs[t][stateIndex];

                // evaluate the reward  with the time and the marking
                double qVal = queueReward.evaluate(currentTime, m);
                double rVal = rejectionRateReward.evaluate(currentTime, m);
                double uVal = unusageReward.evaluate(currentTime, m);

                // Cumulate (Prob * Reward value)
                queueValues[t] += prob * qVal;
                rejectionValues[t] += prob * rVal;
                unusageValues[t] += prob * uVal;
            }
            // time update
            currentTime += step;
        }


        java.util.Map<String, double[]> seriesMap = new java.util.LinkedHashMap<>();

        seriesMap.put("Queue Reward", queueValues);
        seriesMap.put("Rejection Rate", rejectionValues);
        seriesMap.put("Unusage Reward", unusageValues);

        // PLOT
        ChartUtility.plotData(seriesMap, 0.0, step, "GSPN Transient Analysis");

        // Steady state solution
        Map<Marking, Double> steadyStateMap = GSPNSteadyState.builder().build().compute(net, marking);

        Map<Marking, BigDecimal> convertedSteadyStateMap = new HashMap<>();
        // Just a conversion from double do BigDecimal
        for (Map.Entry<Marking, Double> entry : steadyStateMap.entrySet()) {
            convertedSteadyStateMap.put(entry.getKey(), BigDecimal.valueOf(entry.getValue()));
        }
        SteadyStateSolution<Marking> solution = new SteadyStateSolution<>(convertedSteadyStateMap);

        // Computes rewards for a given solution
        SteadyStateSolution<RewardRate> rewards = SteadyStateSolution.computeRewards(solution, queueReward, rejectionRateReward, unusageReward);

        BigDecimal avgQueue = rewards.getSteadyState().get(queueReward);
        BigDecimal avgRejection = rewards.getSteadyState().get(rejectionRateReward);
        BigDecimal avgUnusage = rewards.getSteadyState().get(unusageReward);

        System.out.println("Steady State Queued Request:  " + avgQueue);
        System.out.println("Steady State Rejection Rate:  " + avgRejection);
        System.out.println("Steady State Pool Unusage:    " + avgUnusage);

    }
}

package it.unifi.dinfo.stlab.engineeredModules;

import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.*;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.*;
import org.oristool.simulator.Sequencer;
import org.oristool.simulator.TimeSeriesRewardResult;
import org.oristool.simulator.rewards.ContinuousRewardTime;
import org.oristool.simulator.rewards.RewardEvaluator;
import org.oristool.simulator.stpn.STPNSimulatorComponentsFactory;
import org.oristool.simulator.stpn.TransientMarkingConditionProbability;
import org.oristool.util.Pair;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleGSPN {

    private PetriNet net;
    private Marking marking;

    private double arrivalRate;
    private double serviceRate;

    private int queueSize;
    private int poolsize;


    private SteadyStateSolution<RewardRate> rewardSolution;
    private double[][] transientProbs;
    private Map<Marking, Integer> transientMarkingMapping;

    // telescopic builder by J.Bloch
    public static class Builder {
        private double arrivalRate;
        private double serviceRate;

        private int queueSize;
        private int poolsize;

        public Builder(double arrivalRate, double serviceRate){
            this.arrivalRate = arrivalRate;
            this.serviceRate = serviceRate;
            this.queueSize = 16;
            this.poolsize = 2;
        }

        public Builder setQueueSize(int queueSize){ this.queueSize = queueSize; return this; }
        public Builder setPoolSize(int poolSize){ this.poolsize = poolSize; return this; }

        public SimpleGSPN build(){
            return new SimpleGSPN(this);
        }
    }


    private SimpleGSPN(Builder builder){
        this.net = new PetriNet();
        this.marking = new Marking();

        // not so useful
        this.arrivalRate = builder.arrivalRate;
        this.serviceRate = builder.serviceRate;
        this.queueSize = builder.queueSize;
        this.poolsize = builder.poolsize;

        //Generating Nodes
        Place Queue = net.addPlace("Queue");
        Transition arrival = net.addTransition("arrival");
        Transition reject = net.addTransition("reject");
        Transition service = net.addTransition("service");

        //Generating Connectors
        net.addPrecondition(Queue, reject);
        net.addPrecondition(Queue, service);
        net.addPostcondition(arrival, Queue);

        // TODO Note that I have deleted these node and places
//        Place ARate = net.addPlace("ARate");
//        Place PoolSize = net.addPlace("PoolSize");
//        Place QueueSize = net.addPlace("QueueSize");
//        Place SRate = net.addPlace("SRate");
//        marking.setTokens(ARate, 40);
//        marking.setTokens(PoolSize, 2);
//        marking.setTokens(QueueSize, 16);
//        marking.setTokens(SRate, 25);



        //Generating Properties
        marking.setTokens(Queue, 0);
//        arrival.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("1.0*ARate/100", net)));
        arrival.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from(String.valueOf(this.arrivalRate), net)));
        reject.addFeature(new EnablingFunction("Queue>" + String.valueOf(this.queueSize)));
        reject.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        reject.addFeature(new Priority(0));
//        service.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("1.0*SRate/100*min(PoolSize,Queue)", net)));
        service.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal(String.valueOf(builder.serviceRate)), MarkingExpr.from("min("+ String.valueOf(this.poolsize) +",Queue)", net)));
        // the following should be equivalent
//        service.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from(String.valueOf(this.serviceRate) + "*min("+ String.valueOf(this.poolsize) +",Queue)", net)));
    }


    public String getQueueRewardString(){
        return "If(Queue>"+ this.poolsize + ",Queue- "+ this.poolsize + ",0)";
    }

    public String getRejectionRateRewardString(){
        return "If(Queue==" + this.queueSize + ", " +this.arrivalRate + ",0)";
    }

    public String getUnusageResourcesRewardString(){
        return "If(Queue< "+this.poolsize+", "+this.poolsize+"-Queue,0)";
    }

    public String getRewardOfInterestString(){
        return getQueueRewardString() + ";" + getRejectionRateRewardString() + ";" + getUnusageResourcesRewardString();
    }

    public void transientAnalysis(double limit, double step){
        Pair<Map<Marking, Integer>, double[][]> result = GSPNTransient.builder()
                .timePoints(0.0, limit , step)
                .build()
                .compute(net, marking);

        this.transientMarkingMapping = result.first();          // marking -> column index
        this.transientProbs = result.second();                // probs[t][stateIndex]
    }




    // TODO could be optimized to manage multiple rewards at once
    // TODO step could be a property of the model
    public double[] getTransientFromReward(String rewardAsString, double timestep){
        int numTimePoints = this.transientProbs.length;
        double[] values = new double[numTimePoints];

        double currentTime = 0.0;

        for (int t = 0; t < numTimePoints; t++) {

            // Cycle on all the markings
            for (Map.Entry<Marking, Integer> entry : this.transientMarkingMapping.entrySet()) {
                Marking m = entry.getKey();
                int stateIndex = entry.getValue();

                // Prob of being at marking m at time t
                double prob = this.transientProbs[t][stateIndex];

                // evaluate the reward  with the time and the marking
                RewardRate reward = RewardRate.fromString(rewardAsString);
                double valT = reward.evaluate(currentTime, m);

                // Cumulate (Prob * Reward value)
                values[t] += prob * valT;
            }
            // time update
            currentTime += timestep;
        }
        return values;
    }

    public double[] getTransientQueue(double timestep){
        return this.getTransientFromReward(getQueueRewardString(), timestep);
    }

    public double[] getTransientRejectionRate(double timestep){
        return this.getTransientFromReward(getRejectionRateRewardString(), timestep);
    }

    public double[] getTransientPoolUnusage(double timestep){
        return this.getTransientFromReward(getUnusageResourcesRewardString(), timestep);
    }


//    public void steadyStateAnalysis(){
//        Map<Marking, Double> steadyStateMap = GSPNSteadyState.builder().build().compute(net, marking);
//        Map<Marking, BigDecimal> convertedSteadyStateMap = new HashMap<>();
//        // Just a conversion from double do BigDecimal
//        for (Map.Entry<Marking, Double> entry : steadyStateMap.entrySet()) {
//            convertedSteadyStateMap.put(entry.getKey(), BigDecimal.valueOf(entry.getValue()));
//        }
//        SteadyStateSolution<Marking> solution = new SteadyStateSolution<>(convertedSteadyStateMap);
//        // Computes rewards for a given solution
//        this.rewardSolution = SteadyStateSolution.computeRewards(solution, getRewardOfInterestString() );
//    }


    public void steadyStateAnalysis(){
       steadyStateAnalysis(getRewardOfInterestString());
    }

    public void steadyStateAnalysis(String rewardsAsString){
        Map<Marking, Double> steadyStateMap = GSPNSteadyState.builder().build().compute(net, marking);
        Map<Marking, BigDecimal> convertedSteadyStateMap = new HashMap<>();
        // Just a conversion from double do BigDecimal
        for (Map.Entry<Marking, Double> entry : steadyStateMap.entrySet()) {
            convertedSteadyStateMap.put(entry.getKey(), BigDecimal.valueOf(entry.getValue()));
        }
        SteadyStateSolution<Marking> solution = new SteadyStateSolution<>(convertedSteadyStateMap);
        // Computes rewards for a given solution
        this.rewardSolution = SteadyStateSolution.computeRewards(solution, rewardsAsString );
    }

    public BigDecimal getSteadyStateReward(String rewardAsString){
        Map<RewardRate, BigDecimal> steadyState = this.rewardSolution.getSteadyState();
        BigDecimal reward = steadyState.entrySet().stream()
                .filter(t -> t.getKey().toString().equals(rewardAsString)).findFirst().get().getValue();
        return reward;
    }

    public BigDecimal getSteadyStateQueue(){
        Map<RewardRate, BigDecimal> steadyState = this.rewardSolution.getSteadyState();
        BigDecimal reward = steadyState.entrySet().stream()
                .filter(t -> t.getKey().toString().equals(getQueueRewardString())).findFirst().get().getValue();
        return reward;
    }

    public BigDecimal getSteadyStateRejectionRate(){
        Map<RewardRate, BigDecimal> steadyState = this.rewardSolution.getSteadyState();
        BigDecimal reward = steadyState.entrySet().stream()
                .filter(t -> t.getKey().toString().equals(getRejectionRateRewardString())).findFirst().get().getValue();
        return reward;
    }

    public BigDecimal getSteadyStatePoolUnusage(){
        Map<RewardRate, BigDecimal> steadyState = this.rewardSolution.getSteadyState();
        BigDecimal reward = steadyState.entrySet().stream()
                .filter(t -> t.getKey().toString().equals(getUnusageResourcesRewardString())).findFirst().get().getValue();
        return reward;
    }

//    public double[] simulate(double limit, double step, String rewardString, int runsNumber) {
    public void simulate(double limit, double step, int runsNumber, String... rewardStrings) {

        BigDecimal timeLimit = BigDecimal.valueOf(limit);
        BigDecimal timeStep = BigDecimal.valueOf(step);


        Sequencer s = new Sequencer(this.net, this.marking , new STPNSimulatorComponentsFactory(), new AnalysisLogger() {
            @Override
            public void log(String message) { }
            @Override
            public void debug(String string) { }
        });

        // Derive the number of time points
        int samplesNumber = (timeLimit.divide(timeStep)).intValue() + 1;

        // Create a reward (which is a sequencer observer)
        // One observer for each reward (all observing the same sequencer)
        Map<String, TransientMarkingConditionProbability> rewardObservers = new LinkedHashMap<>();
        for (String rewardString : rewardStrings) {
            TransientMarkingConditionProbability obs = new TransientMarkingConditionProbability(
                    s,
                    new ContinuousRewardTime(timeStep),
                    samplesNumber,
                    MarkingCondition.fromString(rewardString)
            );
            new RewardEvaluator(obs, runsNumber);
            rewardObservers.put(rewardString, obs);
        }

//        TransientMarkingConditionProbability reward = new TransientMarkingConditionProbability(
//                s, new ContinuousRewardTime(timeStep), samplesNumber, MarkingCondition.fromString(rewardString));

        // Create a reward evaluator (which is a reward observer)
//        RewardEvaluator rewardEvaluator = new RewardEvaluator(reward, runsNumber);



        // Run simulation
        s.simulate();

//        TimeSeriesRewardResult result = (TimeSeriesRewardResult)reward.evaluate();

        // Plot results
//        new TransientSolutionViewer(getTransientSolutionFromSimulatorResult(result, rewardString, this.marking, timeLimit, timeStep));



        // TransientSolution with N cols (one per each reward)
        List<Marking> regenerations = new ArrayList<>(Arrays.asList(this.marking));
        List<RewardRate> columnStates = Arrays.stream(rewardStrings)
                .map(RewardRate::fromString)
                .collect(Collectors.toList());

        TransientSolution<Marking, RewardRate> combinedSolution =
                new TransientSolution<>(timeLimit, timeStep, regenerations, columnStates, this.marking);

        int col = 0;
        for (Map.Entry<String, TransientMarkingConditionProbability> entry : rewardObservers.entrySet()) {
            TimeSeriesRewardResult simResult = (TimeSeriesRewardResult) entry.getValue().evaluate();

            TransientSolution<Marking, RewardRate> singleRewardSolution =
                    getTransientSolutionFromSimulatorResult(
                            simResult, entry.getKey(), this.marking, timeLimit, timeStep);

            // Populate the col of the combined solution
            for (int t = 0; t < samplesNumber; t++) {
                combinedSolution.getSolution()[t][0][col] =
                        singleRewardSolution.getSolution()[t][0][0];
            }
            col++;
        }

        new TransientSolutionViewer(combinedSolution);
    }

    public static TransientSolution<Marking, RewardRate> getTransientSolutionFromSimulatorResult(
            TimeSeriesRewardResult result, String rewardString, Marking initialMarking, BigDecimal timeLimit, BigDecimal timeStep) {

        RewardRate rewardRate = RewardRate.fromString(rewardString);
        List<Marking> regenerations = new ArrayList<>(Arrays.asList(initialMarking));
        List<RewardRate> columnStates = new ArrayList<>();
        columnStates.add(rewardRate);

        TransientSolution<Marking, RewardRate> solution = new TransientSolution<>(timeLimit, timeStep, regenerations, columnStates, initialMarking);

        List<Marking> mrkTmp = new ArrayList<>(result.getMarkings());
        TransientSolution<Marking, Marking> tmpSolution = new TransientSolution<>(timeLimit, timeStep, regenerations, mrkTmp, initialMarking);

        for (int t = 0; t < tmpSolution.getSolution().length; t++) {
            for (int i = 0; i < mrkTmp.size(); i++) {
                tmpSolution.getSolution()[t][0][i] = result.getTimeSeries(mrkTmp.get(i))[t].doubleValue();
            }
        }

        // Evaluate the reward
        TransientSolution<Marking, RewardRate> rewardTmpResult = TransientSolution.computeRewards(false, tmpSolution, rewardRate);
        for (int t = 0; t < solution.getSolution().length; t++) {
            solution.getSolution()[t][0][columnStates.indexOf(rewardRate)]
                    = rewardTmpResult.getSolution()[t][0][0];
        }

        return solution;
    }


}

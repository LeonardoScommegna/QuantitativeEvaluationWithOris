package it.unifi.dinfo.stlab.engineeredModules;

import org.oristool.models.pn.PostUpdater;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.*;
import org.oristool.models.stpn.steady.RegSteadyState;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticStateFeature;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class NonMarkovianModel {

    private PetriNet net;
    private Marking marking;

    private StochasticTransitionFeature repairFeature;
    private StochasticTransitionFeature detectFeature;
    private StochasticTransitionFeature failFeature;
    private StochasticTransitionFeature errFeature;
    private double waitClockTime;
    private StochasticTransitionFeature rejFormErrFeature;
    private StochasticTransitionFeature rejFormOkFeature;


    // telescopic builder by J.Bloch
    public static class Builder {

        private double waitClockTime;
        private StochasticTransitionFeature repairFeature;
        private StochasticTransitionFeature detectFeature;
        private StochasticTransitionFeature failFeature;
        private StochasticTransitionFeature errFeature;
        private StochasticTransitionFeature rejFromErrFeature;
        private StochasticTransitionFeature rejFromOkFeature;

        public Builder(double waitClockTime) {
            this.waitClockTime = waitClockTime;
            this.repairFeature = StochasticTransitionFeature.newUniformInstance(BigDecimal.valueOf(0), BigDecimal.valueOf(25));
            this.detectFeature = StochasticTransitionFeature.newUniformInstance(BigDecimal.valueOf(0), BigDecimal.valueOf(5));
            this.failFeature = StochasticTransitionFeature.newErlangInstance(2, BigDecimal.valueOf(0.01));
            this.errFeature = StochasticTransitionFeature.newErlangInstance(2, BigDecimal.valueOf(0.02));
            this.rejFromErrFeature = StochasticTransitionFeature.newUniformInstance(BigDecimal.valueOf(0), BigDecimal.valueOf(5));
            this.rejFromOkFeature = StochasticTransitionFeature.newUniformInstance(BigDecimal.valueOf(0), BigDecimal.valueOf(5));
        }

        public Builder repair(double eft, double lft){
            this.repairFeature = StochasticTransitionFeature.newUniformInstance(BigDecimal.valueOf(eft), BigDecimal.valueOf(lft));
            return this;
        }

        public Builder detect(double eft, double lft){
            this.detectFeature = StochasticTransitionFeature.newUniformInstance(BigDecimal.valueOf(eft), BigDecimal.valueOf(lft));
            return this;
        }

        public Builder fail(int k, double rate){
            this.failFeature = StochasticTransitionFeature.newErlangInstance(k, BigDecimal.valueOf(rate));
            return this;
        }

        public Builder err(int k, double rate){
            this.errFeature = StochasticTransitionFeature.newErlangInstance(k, BigDecimal.valueOf(rate));
            return this;
        }

        public Builder rejFromErr(double eft, double lft){
            this.rejFromErrFeature = StochasticTransitionFeature.newUniformInstance(BigDecimal.valueOf(eft), BigDecimal.valueOf(lft));
            return this;
        }

        public Builder rejFromOk(double eft, double lft){
            this.rejFromOkFeature = StochasticTransitionFeature.newUniformInstance(BigDecimal.valueOf(eft), BigDecimal.valueOf(lft));
            return this;
        }

        public NonMarkovianModel build(){return new NonMarkovianModel(this);}
    }

    private NonMarkovianModel(Builder builder){
        this.net = new PetriNet();
        this.marking = new Marking();

        this.repairFeature = builder.repairFeature;
        this.detectFeature = builder.detectFeature;
        this.failFeature =  builder.failFeature;
        this.errFeature =   builder.errFeature;
        this.rejFormErrFeature = builder.rejFromErrFeature;
        this.rejFormOkFeature =  builder.rejFromOkFeature;
        this.waitClockTime =  builder.waitClockTime;


        //Generating Nodes
        Place Clock = net.addPlace("Clock");
        Place Detected = net.addPlace("Detected");
        Place Err = net.addPlace("Err");
        Place Ko = net.addPlace("Ko");
        Place Ok = net.addPlace("Ok");
        Place Rej = net.addPlace("Rej");
        Transition detect = net.addTransition("detect");
        Transition error = net.addTransition("error");
        Transition fail = net.addTransition("fail");
        Transition rejFromErr = net.addTransition("rejFromErr");
        Transition rejFromOk = net.addTransition("rejFromOk");
        Transition repair = net.addTransition("repair");
        Transition waitClock = net.addTransition("waitClock");

        //Generating Connectors
        net.addInhibitorArc(Rej, error);
        net.addInhibitorArc(Detected, waitClock);
        net.addInhibitorArc(Rej, fail);
        net.addPostcondition(error, Err);
        net.addPrecondition(Rej, rejFromOk);
        net.addPrecondition(Ok, rejFromOk);
        net.addPostcondition(rejFromOk, Clock);
        net.addPrecondition(Err, rejFromErr);
        net.addPrecondition(Detected, repair);
        net.addPrecondition(Ko, detect);
        net.addPostcondition(rejFromErr, Clock);
        net.addPrecondition(Clock, waitClock);
        net.addPrecondition(Rej, rejFromErr);
        net.addPostcondition(waitClock, Rej);
        net.addPostcondition(rejFromOk, Ok);
        net.addPostcondition(fail, Ko);
        net.addPostcondition(rejFromErr, Ok);
        net.addPostcondition(repair, Ok);
        net.addPrecondition(Ok, error);
        net.addPrecondition(Err, fail);
        net.addPostcondition(detect, Detected);

        //Generating Properties
        marking.setTokens(Clock, 1);
        marking.setTokens(Detected, 0);
        marking.setTokens(Err, 0);
        marking.setTokens(Ko, 0);
        marking.setTokens(Ok, 1);
        marking.setTokens(Rej, 0);
        detect.addFeature(this.detectFeature);
        error.addFeature(this.errFeature);
        fail.addFeature(this.failFeature);
        rejFromErr.addFeature(this.rejFormErrFeature);
        rejFromOk.addFeature(this.rejFormOkFeature);
        repair.addFeature(new PostUpdater("Clock=1, Rej=0", net));
        repair.addFeature(this.repairFeature);
        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(this.waitClockTime), MarkingExpr.from("1", net)));
        waitClock.addFeature(new Priority(0));
    }



    public String getUnreliabilityRewardString(){
        return "Ko";
    }

    public String getUnavailabilityRewardString(){
        return "If(Ko+Detected>0 || Rej>0,1,0)";
    }

    public String getUnusefulUnavailabilityRewardString(){
        return "If(Ok>0 && Rej>0,1,0)";
    }

    public void regenerativeTransient(double limit, double step, String... rewardStrings) {

        BigDecimal timeLimit = BigDecimal.valueOf(limit);
        BigDecimal timeStep  = BigDecimal.valueOf(step);

        RegTransient analysis = RegTransient.builder()
                .timeBound(timeLimit)
                .timeStep(timeStep)
                .build();

        TransientSolution<DeterministicEnablingState, Marking> result =
                analysis.compute(this.net, this.marking);

        if (rewardStrings.length == 0) {
            // Marking Distribution
            new TransientSolutionViewer(result);
            return;
        }

        // just converting the reward strings in a RewardRate List
        List<RewardRate> rewardRates = Arrays.stream(rewardStrings)
                .map(RewardRate::fromString)
                .collect(Collectors.toList());

        TransientSolution<DeterministicEnablingState, RewardRate> rewardSolution =
                TransientSolution.computeRewards(false, result, rewardRates.toArray(new RewardRate[0]));

        new TransientSolutionViewer(rewardSolution);
    }

    public Map<String, Double > regenerativeSteadyState(String... rewardStrings) {

        RegSteadyState analysis = RegSteadyState.builder().build();

        SteadyStateSolution<Marking> result = analysis.compute(this.net, this.marking);
        Map<Marking, BigDecimal> probs = result.getSteadyState();


        System.out.println("Steady-state probabilities:");
        for (Marking m : probs.keySet()) {
            System.out.printf("%1.6f -- %s%n", probs.get(m), m);
        }

        if (rewardStrings.length == 0) return new HashMap<>();

        // just converting the reward strings in a RewardRate List
        List<RewardRate> rewardRates = Arrays.stream(rewardStrings)
                .map(RewardRate::fromString)
                .collect(Collectors.toList());

        // Extracting the Rewards
        SteadyStateSolution<RewardRate> rewardResult =
                SteadyStateSolution.computeRewards(result, rewardRates.toArray(new RewardRate[0]));

        System.out.println("\nRewards Steady State Analysis");

        Map<String, Double> resultMap = new HashMap<>();

        rewardRates.forEach(rr ->{
                    resultMap.put(rr.toString(), rewardResult.getSteadyState().get(rr).doubleValue());
                    System.out.printf("  %s  →  %.6f%n",
                            rr, rewardResult.getSteadyState().get(rr).doubleValue());
                });


        return resultMap;

    }


}

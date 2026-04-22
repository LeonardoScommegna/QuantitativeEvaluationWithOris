package it.unifi.dinfo.stlab;

import it.unifi.dinfo.stlab.engineeredModules.NonMarkovianModel;

import java.util.Map;

public class nonMarkovianWithoutOrisExperiment {
    public static void main(String[] args) {

        NonMarkovianModel model = new NonMarkovianModel.Builder(150).build();

//        model.regenerativeTransient(10000, 2.5, model.getUnusefulUnavailabilityRewardString(), model.getUnavailabilityRewardString(), model.getUnreliabilityRewardString());
        Map<String, Double> steadyStateMap = model.regenerativeSteadyState(model.getUnusefulUnavailabilityRewardString(), model.getUnavailabilityRewardString(), model.getUnreliabilityRewardString());

    }
}

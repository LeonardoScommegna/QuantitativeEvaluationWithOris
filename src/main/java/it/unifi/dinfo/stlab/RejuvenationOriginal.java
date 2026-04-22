package it.unifi.dinfo.stlab;

import org.oristool.models.pn.PostUpdater;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

import java.math.BigDecimal;

public class RejuvenationOriginal {
    public static void build(PetriNet net, Marking marking) {

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
        detect.addFeature(StochasticTransitionFeature.newUniformInstance(new BigDecimal("0"), new BigDecimal("5")));
        error.addFeature(StochasticTransitionFeature.newErlangInstance(new Integer("2"), new BigDecimal("0.02")));
        fail.addFeature(StochasticTransitionFeature.newErlangInstance(new Integer("2"), new BigDecimal("0.01")));
        rejFromErr.addFeature(StochasticTransitionFeature.newUniformInstance(new BigDecimal("0"), new BigDecimal("5")));
        rejFromOk.addFeature(StochasticTransitionFeature.newUniformInstance(new BigDecimal("0"), new BigDecimal("5")));
        repair.addFeature(new PostUpdater("Clock=1, Rej=0", net));
        repair.addFeature(StochasticTransitionFeature.newUniformInstance(new BigDecimal("0"), new BigDecimal("25")));
        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("150"), MarkingExpr.from("1", net)));
        waitClock.addFeature(new Priority(0));
    }
}

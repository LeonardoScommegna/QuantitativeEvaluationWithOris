package it.unifi.dinfo.stlab;

import elasticity.accountant.DutchAccountant;
import elasticity.accountant.RomanAccountant;
import elasticity.controllers.RospoController;
import elasticity.scaler.LinearScaler;
import it.unifi.dinfo.stlab.utils.ChartUtility;
import modeling.ActivityWithResources;
import modeling.RospoHyperTree;
import org.oristool.eulero.evaluation.approximator.DoubleTruncatedEXPApproximation;
import org.oristool.eulero.evaluation.heuristics.AnalysisHeuristicsVisitor;
import org.oristool.eulero.evaluation.heuristics.SDFHeuristicsVisitor;
import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Simple;
import org.oristool.eulero.modeling.stochastictime.ExponentialTime;
import utils.RospoModelFactory;
import utils.distributions.VariableExponentialTime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class ToadExperiment {

    public static void main(String[] args) {

        double timelimit = 8.;
        double timestep = 0.1;

        ActivityWithResources lowInertia = ActivityWithResources.of(new Simple("lowInertia", new VariableExponentialTime(BigDecimal.valueOf(2))), 1.1);
        ActivityWithResources highInertia = ActivityWithResources.of(new Simple("highInertia", new VariableExponentialTime(BigDecimal.valueOf(0.9))), 0.9);

        ActivityWithResources workflow = RospoModelFactory.forkJoin(lowInertia, highInertia);

        AnalysisHeuristicsVisitor myAnalyzer = new SDFHeuristicsVisitor(BigInteger.valueOf(3), BigInteger.valueOf(3), new DoubleTruncatedEXPApproximation());
        double[] res = workflow.analyze(BigDecimal.valueOf(timelimit), BigDecimal.valueOf(timestep), myAnalyzer);

        Map<String, double[]> dataSeries = new HashMap<>();
        dataSeries.put("CDF", res);


        Activity balancedWorkflow = balance(workflow);
        double[] balancedRes = balancedWorkflow.analyze(BigDecimal.valueOf(timelimit), BigDecimal.valueOf(timestep), myAnalyzer);

        dataSeries.put("CDFBalanced", balancedRes);
        ChartUtility.plotData(dataSeries, 0, timestep, "Eulero Analysis");

    }

    public static Activity balance(ActivityWithResources activity){
        LinearScaler timeScaler = new LinearScaler();
        Activity model = activity.clone(); // defensive copy
        RospoHyperTree hyperTree = RospoController.reduce((ActivityWithResources) model);
        RospoHyperTree balancedTree = RospoController.balance(hyperTree, timeScaler, new DutchAccountant());
        balancedTree = RospoController.scale(balancedTree, ((ActivityWithResources)activity).getInitialResources(), timeScaler );
        return model;
    }
}

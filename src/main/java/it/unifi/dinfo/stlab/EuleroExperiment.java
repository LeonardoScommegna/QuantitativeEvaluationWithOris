package it.unifi.dinfo.stlab;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.unifi.dinfo.stlab.utils.ChartUtility;
import org.oristool.eulero.evaluation.approximator.DoubleTruncatedEXPApproximation;
import org.oristool.eulero.evaluation.heuristics.AnalysisHeuristicsVisitor;
import org.oristool.eulero.evaluation.heuristics.SDFHeuristicsVisitor;
import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Composite;
import org.oristool.eulero.modeling.ModelFactory;
import org.oristool.eulero.modeling.Simple;
import org.oristool.eulero.modeling.stochastictime.ExponentialTime;
import org.oristool.eulero.modeling.stochastictime.UniformTime;
import utils.RospoModelFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EuleroExperiment {
    public static void main(String[] args) {

        double higherBranchRate = 1.;
        double lowerBranchRate = 2.;

        Composite workflow = generateWorkflow(higherBranchRate, lowerBranchRate);


        // analysis
        double timelimit = 9.;
        double timestep = 0.1;
        AnalysisHeuristicsVisitor myAnalyzer = new SDFHeuristicsVisitor(BigInteger.valueOf(3), BigInteger.valueOf(3), new DoubleTruncatedEXPApproximation());
        double[] res = workflow.analyze(BigDecimal.valueOf(timelimit), BigDecimal.valueOf(timestep), myAnalyzer);


        Map<String, double[]> dataSeries = new HashMap<>();
        dataSeries.put("CDF", res);

//        Composite workflowBalanced = generateWorkflow(1.5, 1.5);
//        double[] res2 = workflowBalanced.analyze(BigDecimal.valueOf(timelimit), BigDecimal.valueOf(timestep), myAnalyzer);
//        dataSeries.put("CDFBalanced", res2);

        ChartUtility.plotData(dataSeries, 0, timestep, "Eulero Analysis");
    }

    public static Composite generateWorkflow(double rate4, double rate5){
        Simple task1 = new Simple("task1", new ExponentialTime(BigDecimal.valueOf(1)));
        Simple task2 = new Simple("task2", new ExponentialTime(BigDecimal.valueOf(1)));

        Simple task3 = new Simple("task3", new ExponentialTime(BigDecimal.valueOf(rate4)));
        Simple task4 = new Simple("task4", new ExponentialTime(BigDecimal.valueOf(rate5)));

        Composite higherBranch = ModelFactory.sequence(task1, task3);
        Composite lowerBranch = ModelFactory.sequence(task2, task4);

        return ModelFactory.forkJoin(higherBranch, lowerBranch);
    }

}

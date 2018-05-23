/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.cim2015.examples;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.OptionParser;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 30/06/15.
 */
public final class ExperimentsParallelML {

    static int[] batchSizes = {100};
    /*Options for core comparisons*/
    static boolean coreComparison = false;
    static int batchSize = 1000;

    /*Options for batch size comparisons*/
    static boolean parallel = true;
    static boolean sampleData = true;
    static String pathToFile = "datasets/simulated/tmp.arff";
    static int sampleSize = 1000000;
    static int numDiscVars = 5;
    static int numGaussVars = 5;
    static int numStatesHiddenDiscVars = 10;
    static int numHiddenGaussVars = 2;
    static int numStates = 10;

    public static boolean isParallel() {
        return parallel;
    }

    public static void setParallel(boolean parallel) {
        ExperimentsParallelML.parallel = parallel;
    }

    public static int getSampleSize() {
        return sampleSize;
    }

    public static void setSampleSize(int sampleSize) {
        ExperimentsParallelML.sampleSize = sampleSize;
    }


    public static int getNumDiscVars() {
        return numDiscVars;
    }

    public static void setNumDiscVars(int numDiscVars) {
        ExperimentsParallelML.numDiscVars = numDiscVars;
    }

    public static int getNumGaussVars() {
        return numGaussVars;
    }

    public static void setNumGaussVars(int numGaussVars) {
        ExperimentsParallelML.numGaussVars = numGaussVars;
    }

    public static int getNumStatesHiddenDiscVars() {
        return numStatesHiddenDiscVars;
    }

    public static void setNumStatesHiddenDiscVars(int numStatesHiddenDiscVars) {
        ExperimentsParallelML.numStatesHiddenDiscVars = numStatesHiddenDiscVars;
    }

    public static int getNumHiddenGaussVars() {
        return numHiddenGaussVars;
    }

    public static void setNumHiddenGaussVars(int numHiddenGaussVars) {
        ExperimentsParallelML.numHiddenGaussVars = numHiddenGaussVars;
    }

    public static int getNumStates() {
        return numStates;
    }

    public static void setNumStates(int numStates) {
        ExperimentsParallelML.numStates = numStates;
    }

    public static boolean isSampleData() {
        return sampleData;
    }

    public static void setSampleData(boolean sampleData) {
        ExperimentsParallelML.sampleData = sampleData;
    }

    public static boolean isCoreComparison() {
        return coreComparison;
    }

    public static void setCoreComparison(boolean coreComparison) {
        ExperimentsParallelML.coreComparison = coreComparison;
    }

    public static int getBatchSize() {
        return batchSize;
    }

    public static void setBatchSize(int batchSize) {
        ExperimentsParallelML.batchSize = batchSize;
    }

    public static String getPathToFile() {
        return pathToFile;
    }

    public static void setPathToFile(String pathToFile) {
        ExperimentsParallelkMeans.pathToFile = pathToFile;
    }

    static DAG dag;

    public static void compareNumberOfCores() throws IOException {

        System.out.println("Comparison with different number of cores");
        System.out.println("-----------------------------------------");
        createBayesianNetwork();
        if(isSampleData())
            sampleBayesianNetwork();

        DataStream<DataInstance> data = DataStreamLoader.open(getPathToFile());
        ParallelMaximumLikelihood parameterLearningAlgorithm = new ParallelMaximumLikelihood();
        parameterLearningAlgorithm.setParallelMode(isParallel());
        parameterLearningAlgorithm.setDAG(dag);
        parameterLearningAlgorithm.setDataStream(data);
        parameterLearningAlgorithm.setWindowsSize(getBatchSize());


        System.out.println("Available number of processors: " + Runtime.getRuntime().availableProcessors());

        //We discard the first five experiments and then record the following 10 repetitions
        double average = 0.0;
        for (int j = 0; j <15; j++) {
            long start = System.nanoTime();
            parameterLearningAlgorithm.runLearning();
            long duration = (System.nanoTime() - start) / 1;
            double seconds = duration / 1000000000.0;
            System.out.println("Iteration ["+j+"] = "+seconds + " secs");
            if(j>4){
                average+=seconds;
            }
            data.restart();
        }
        System.out.println("Average = "+average/10.0 + " secs");
    }

    public static void compareBatchSizes() throws IOException {

        System.out.println("Batch size comparisons");
        System.out.println("----------------------");

        createBayesianNetwork();
        if(isSampleData())
            sampleBayesianNetwork();

        DataStream<DataInstance> data = DataStreamLoader.open(getPathToFile());


        ParallelMaximumLikelihood parameterLearningAlgorithm = new ParallelMaximumLikelihood();
        parameterLearningAlgorithm.setParallelMode(isParallel());
        parameterLearningAlgorithm.setDAG(dag);
        parameterLearningAlgorithm.setDataStream(data);


        //We discard the first five experiments and then record the following 10 repetitions
        for (int i = 0; i < batchSizes.length; i++) {
            long average = 0L;
            for (int j = 0; j < 5; j++) {
                parameterLearningAlgorithm.setWindowsSize(batchSizes[i]);
                long start = System.nanoTime();
                parameterLearningAlgorithm.runLearning();
                long duration = (System.nanoTime() - start) / 1;
                double seconds = duration / 1000000000.0;
                //System.out.println("Iteration ["+j+"] = "+seconds + " secs");
                if (j > 4) {
                    average += seconds;
                }
            }
            System.out.println(batchSizes[i]+"\t"+average/10.0 + " secs");
        }
    }

    private static void createBayesianNetwork(){
        /* ********** */
        /* Create DAG */
        /* Create all variables */
        Variables variables = new Variables();

        IntStream.range(0, getNumDiscVars() -1)
                .forEach(i -> variables.newMultinomialVariable("DiscreteVar" + i, getNumStates()));

        IntStream.range(0, getNumGaussVars())
                .forEach(i -> variables.newGaussianVariable("GaussianVar" + i));

        Variable classVar = variables.newMultinomialVariable("ClassVar", getNumStates());

        if(getNumHiddenGaussVars() > 0)
            IntStream.rangeClosed(0, getNumHiddenGaussVars() - 1).forEach(i -> variables.newGaussianVariable("GaussianSPVar_" + i));
        //if(numStatesHiddenDiscVars > 0)
        Variable discreteHiddenVar = variables.newMultinomialVariable("DiscreteSPVar", getNumStatesHiddenDiscVars());

        dag = new DAG(variables);

        /* Link variables */
        dag.getParentSets().stream()
                .filter(parentSet -> !parentSet.getMainVar().equals(classVar) && !parentSet.getMainVar().getName().startsWith("GaussianSPVar_")
                        && !parentSet.getMainVar().getName().startsWith("DiscreteSPVar"))
                .forEach(w -> {
                    w.addParent(classVar);
                    w.addParent(discreteHiddenVar);
                    if (getNumHiddenGaussVars() > 0 && w.getMainVar().isNormal())
                        IntStream.rangeClosed(0, getNumHiddenGaussVars() - 1).forEach(i -> w.addParent(variables.getVariableByName("GaussianSPVar_" + i)));
                });

        /*Add classVar as parent of all super-parent variables*/
        if(getNumHiddenGaussVars() > 0)
            IntStream.rangeClosed(0, getNumHiddenGaussVars()-1).parallel()
                    .forEach(hv -> dag.getParentSet(variables.getVariableByName("GaussianSPVar_" + hv)).addParent(classVar));
        dag.getParentSet(variables.getVariableByName("DiscreteSPVar")).addParent(classVar);


        System.out.println(dag.toString());
    }
    private static void sampleBayesianNetwork()  throws IOException {


        BayesianNetwork bn = new BayesianNetwork(dag);

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(0);

        //The method sampleToDataStream returns a DataStream with ten DataInstance objects.
        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(getSampleSize());

        //We finally save the sampled data set to an arff file.
        DataStreamWriter.writeDataToFile(dataStream, getPathToFile());
    }

    public static String classNameID(){
        return "eu.amidst.cim2015.examples.ExperimentsParallelML";
    }

    public static String getOption(String optionName) {
        return OptionParser.parse(classNameID(), listOptions(), optionName);
    }

    public static int getIntOption(String optionName){
        return Integer.parseInt(getOption(optionName));
    }

    public static boolean getBooleanOption(String optionName){
        return getOption(optionName).equalsIgnoreCase("true") || getOption(optionName).equalsIgnoreCase("T");
    }

    public static String listOptions(){

        return  classNameID() +",\\"+
                "-sampleSize, 1000000, Sample size of the dataset\\" +
                "-numStates, 10, Num states of all disc. variables (including the class)\\"+
                "-GV, 5, Num of gaussian variables\\"+
                "-DV, 5, Num of discrete variables\\"+
                "-SPGV, 2, Num of gaussian super-parent variables\\"+
                "-SPDV, 10, Num of states for super-parent discrete variable\\"+
                "-sampleData, true, Sample arff data\\"+
                "-parallelMode, true, Run in parallel\\"+
                "-coreComparison, true, Perform comparisons varying the number of cores\\"+
                "-windowsSize, 1000, Batch size for comparisons in the number of cores\\"+
                "-pathToFile, datasetsTests/tmp.arff,Path to sample file if sampleData is set to false\\";
    }

    public static void loadOptions(){
        setNumGaussVars(getIntOption("-GV"));
        setNumDiscVars(getIntOption("-DV"));
        setNumHiddenGaussVars(getIntOption("-SPGV"));
        setNumStatesHiddenDiscVars(getIntOption("-SPDV"));
        setNumStates(getIntOption("-numStates"));
        setSampleSize(getIntOption("-sampleSize"));
        setSampleData(getBooleanOption("-sampleData"));
        setParallel(getBooleanOption("-parallelMode"));
        setCoreComparison(getBooleanOption("-coreComparison"));
        setBatchSize(getIntOption("-windowsSize"));
        setPathToFile(getOption("-pathToFile"));
    }

    public static void main(String[] args) throws Exception {
        OptionParser.setArgsOptions(ExperimentsParallelML.class, args);
        ExperimentsParallelML.loadOptions();
        if(isCoreComparison())
            compareNumberOfCores();
        else
            compareBatchSizes();
    }

}

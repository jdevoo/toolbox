/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */
package eu.amidst.flinklink.core.learning.parametric;


import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.learning.parametric.bayesian.DataPosterior;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.utils.Serialization;
import org.apache.flink.api.common.aggregators.ConvergenceCriterion;
import org.apache.flink.api.common.aggregators.DoubleSumAggregator;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.functions.RichReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.IterativeDataSet;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.DoubleValue;
import org.apache.flink.util.Collector;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.List;

/**
 * This class implements the {@link ParameterLearningAlgorithm} interface, and defines the parallel Maximum Likelihood algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#pmlexample"> http://amidst.github.io/toolbox/CodeExamples.html#pmlexample </a>  </p>
 *
 */
public class ParallelVB implements ParameterLearningAlgorithm {

    public static String SVB="SVB";
    public static String LATENT_VARS="LATENT_VARS";

    /**
     * Represents the {@link DataFlink} used for learning the parameters.
     */
    protected DataFlink<DataInstance> dataFlink;

    /**
     * Represents the directed acyclic graph {@link DAG}.
     */
    protected DAG dag;

    protected SVB svb;

    protected int batchSize = 100;

    protected int maximumGlobalIterations = 10;

    protected double globalThreshold = 0.01;

    public ParallelVB(){
        this.svb = new SVB();
    }

    public void setGlobalThreshold(double globalThreshold) {
        this.globalThreshold = globalThreshold;
    }

    public void setMaximumGlobalIterations(int maximumGlobalIterations) {
        this.maximumGlobalIterations = maximumGlobalIterations;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public SVB getSVB() {
        return svb;
    }

    public void initLearning() {
        this.svb.setDAG(this.dag);
        this.svb.setWindowsSize(batchSize);
        this.svb.initLearning();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void setDataFlink(DataFlink<DataInstance> data) {
        this.dataFlink = data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogMarginalProbability() {
        throw new UnsupportedOperationException("Method not implemented yet");
    }


    public DataSet<DataPosterior> computePosteriorOverLatentVariables(List<Variable> latentVariables){

        Attribute seq_id = this.dataFlink.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        try{
            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.dag.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));
            config.setBytes(LATENT_VARS, Serialization.serializeObject(latentVariables));

            return this.dataFlink
                    .getBatchedDataSet(this.batchSize)
                    .flatMap(new ParallelVBMapInference())
                    .withParameters(config);

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runLearning() {

        try{

            this.initLearning();

            final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

            // get input data
            CompoundVector param = this.svb.getNaturalParameterPrior();
            DataSet<CompoundVector> paramSet = env.fromElements(param);

            // set number of bulk iterations for KMeans algorithm
            IterativeDataSet<CompoundVector> loop = paramSet.iterate(maximumGlobalIterations)
                                                    .registerAggregationConvergenceCriterion("ELBO_" + this.dag.getName(), new DoubleSumAggregator(), new ConvergenceELBO(this.globalThreshold));

            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.dag.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));

            DataSet<CompoundVector> newparamSet = this.dataFlink
                    .getBatchedDataSet(this.batchSize)
                    .map(new ParallelVBMap())
                    .withParameters(config)
                    .withBroadcastSet(loop, "VB_PARAMS_" + this.dag.getName())
                    .reduce(new ParallelVBReduce());

            // feed new centroids back into next iteration
            DataSet<CompoundVector> finlparamSet = loop.closeWith(newparamSet);

            param.sum(finlparamSet.collect().get(0));
            this.svb.updateNaturalParameterPrior(param);
        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDAG(DAG dag_) {
        this.dag = dag_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSeed(int seed) {
        this.svb.setSeed(seed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
        return this.svb.getLearntBayesianNetwork();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutput(boolean activateOutput) {
        this.svb.setOutput(activateOutput);
    }


    static class ParallelVBMap extends RichMapFunction<DataOnMemory<DataInstance>, CompoundVector> {

        DoubleSumAggregator elbo;

        SVB svb;

        @Override
        public CompoundVector map(DataOnMemory<DataInstance> dataBatch) throws Exception {
            SVB.BatchOutput out = svb.updateModelOnBatchParallel(dataBatch);
            elbo.aggregate(out.getElbo());
            return out.getVector();
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            String bnName = parameters.getString(BN_NAME, "");
            svb = Serialization.deserializeObject(parameters.getBytes(SVB, null));

            Collection<CompoundVector> collection = getRuntimeContext().getBroadcastVariable("VB_PARAMS_" + bnName);
            CompoundVector updatedPrior = collection.iterator().next();
            svb.updateNaturalParameterPrior(updatedPrior);

            elbo = getIterationRuntimeContext().getIterationAggregator("ELBO_"+bnName);
        }
    }


    static class ParallelVBMapInference extends RichFlatMapFunction<DataOnMemory<DataInstance>, DataPosterior> {

        List<Variable> latentVariables;
        SVB svb;

        @Override
        public void flatMap(DataOnMemory<DataInstance> dataBatch, Collector<DataPosterior> out) {
            svb.computePosteriorOverLatentVariables(dataBatch, latentVariables)
                    .stream()
                    .forEach(record -> out.collect(record));
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            svb = Serialization.deserializeObject(parameters.getBytes(SVB, null));
            latentVariables = Serialization.deserializeObject(parameters.getBytes(LATENT_VARS, null));
        }
    }

    static class ParallelVBReduce extends RichReduceFunction<CompoundVector> {
        @Override
        public CompoundVector reduce(CompoundVector value1, CompoundVector value2) throws Exception {
            value2.sum(value1);
            return value2;
        }
    }


    static class ConvergenceELBO implements ConvergenceCriterion<DoubleValue>{

        final double threshold;
        double previousELBO = Double.NaN;

        public ConvergenceELBO(double threshold){
            this.threshold=threshold;
        }
        @Override
        public boolean isConverged(int iteration, DoubleValue value) {
            if (iteration==1) {
                previousELBO=value.getValue();
                return false;
            }else if (Math.abs(value.getValue() - previousELBO)>threshold) {
                this.previousELBO=value.getValue();
                return false;
            }else {
                return true;
            }
        }
    }
}
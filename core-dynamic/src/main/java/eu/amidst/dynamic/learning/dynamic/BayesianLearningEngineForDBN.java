/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.learning.dynamic;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public final class BayesianLearningEngineForDBN {

    private static BayesianLearningAlgorithmForDBN bayesianLearningAlgorithmForDBN = new StreamingVariationalBayesVMPForDBN();

    public static void setBayesianLearningAlgorithmForDBN(BayesianLearningAlgorithmForDBN bayesianLearningAlgorithmForDBN) {
        BayesianLearningEngineForDBN.bayesianLearningAlgorithmForDBN = bayesianLearningAlgorithmForDBN;
    }

    public static double updateModel(DataOnMemory<DynamicDataInstance> batch){
        return bayesianLearningAlgorithmForDBN.updateModel(batch);
    }

    public static void runLearning() {
        bayesianLearningAlgorithmForDBN.runLearning();
    }

    public static double getLogMarginalProbability(){
        return bayesianLearningAlgorithmForDBN.getLogMarginalProbability();
    }

    public static void setDataStream(DataStream<DynamicDataInstance> data){
        bayesianLearningAlgorithmForDBN.setDataStream(data);
    }

    public void setParallelMode(boolean parallelMode) {
        bayesianLearningAlgorithmForDBN.setParallelMode(parallelMode);
    }

    public static void setDynamicDAG(DynamicDAG dag){
        bayesianLearningAlgorithmForDBN.setDynamicDAG(dag);
    }

    public static DynamicBayesianNetwork getLearntDBN(){
        return bayesianLearningAlgorithmForDBN.getLearntDBN();
    }

    public static void main(String[] args) throws Exception{

    }
}
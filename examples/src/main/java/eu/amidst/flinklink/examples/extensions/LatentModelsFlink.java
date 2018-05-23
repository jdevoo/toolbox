package eu.amidst.flinklink.examples.extensions;

import eu.amidst.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;
import eu.amidst.latentvariablemodels.staticmodels.Model;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.io.FileNotFoundException;

/**
 * Created by rcabanas on 14/06/16.
 */
public class LatentModelsFlink {
    public static void main(String[] args) throws FileNotFoundException {
        boolean hadoop_cluster = false;

        if (args.length>1){
            hadoop_cluster = Boolean.parseBoolean(args[0]);
        }

        final ExecutionEnvironment env;

        //Set-up Flink session.
        if(hadoop_cluster){
            env = ExecutionEnvironment.getExecutionEnvironment();
            env.getConfig().disableSysoutLogging();
        }else{
            Configuration conf = new Configuration();
            conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
            conf.setInteger("taskmanager.numberOfTaskSlots",Main.PARALLELISM);
            env = ExecutionEnvironment.createLocalEnvironment(conf);
            env.setParallelism(Main.PARALLELISM);
            env.getConfig().disableSysoutLogging();
        }
        //Load the datastream
        String filename = "datasets/simulated/exampleDS_d0_c5.arff";
        DataFlink<DataInstance> data = DataFlinkLoader.loadDataFromFile(env, filename, false);

        //Learn the model
        Model model = new FactorAnalysis(data.getAttributes());
        ((FactorAnalysis)model).setNumberOfLatentVariables(3);
        model.updateModel(data);
        BayesianNetwork bn = model.getModel();

        System.out.println(bn);

    }
}

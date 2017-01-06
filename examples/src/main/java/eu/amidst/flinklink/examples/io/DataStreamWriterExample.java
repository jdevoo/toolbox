package eu.amidst.flinklink.examples.io;

import eu.amidst.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.utils.DataSetGenerator;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

/**
 * Created by rcabanas on 09/06/16.
 */
public class DataStreamWriterExample {
    public static void main(String[] args) throws Exception {

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

        //generate a random dataset
        DataFlink<DataInstance> dataFlink = new DataSetGenerator().generate(env,1234,1000,2,3);

        //Saves it as a distributed arff file
        DataFlinkWriter.writeDataToARFFFolder(dataFlink, "datasets/simulated/distributed.arff");
    }
}


//TODO: Write to standard arff --> convert to datastream??
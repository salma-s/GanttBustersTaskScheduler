package app;

import algorithm.Algorithm;
import algorithm.AlgorithmBuilder;
import algorithm.common.utility.AlgorithmType;
import fileio.IO;
import graph.Graph;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        IO io = new IO("example.dot", "outputExample.dot");
        Graph graph = new Graph(io.getNodeMap(), io.getEdgeList());
        AlgorithmBuilder algorithmBuilder = new AlgorithmBuilder(AlgorithmType.SEQUENTIAL, graph, 1, 0);
        Algorithm algorithm = algorithmBuilder.getAlgorithm();
        io.write(algorithm.solve());

    }

// DON'T REMOVE THIS
//    public static void visualisationSample() {
//        //launch(args);
//
//        //#omp parallel for
//        for(int i = 0; i < 10; i++){
//            System.out.println("Hello world from parallel code, executed by " + Pyjama.omp_get_thread_num());
//        }
//
//        System.out.println("Finished Execution");
//    }
}

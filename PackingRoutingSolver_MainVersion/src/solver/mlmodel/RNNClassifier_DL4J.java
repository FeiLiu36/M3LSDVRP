/* *****************************************************************************
 *
 *
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package solver.mlmodel;

import java.io.File;
import java.util.List;

import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.NumberedFileInputSplit;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
//import org.deeplearning4j.examples.utils.DownloaderUtility;
//import org.deeplearning4j.examples.utils.PlotUtil;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.InvocationType;
import org.deeplearning4j.optimize.listeners.EvaluativeListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.learning.config.Nadam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "Linear" Data Classification Example
 * <p>
 * Based on the data from Jason Baldridge:
 * https://github.com/jasonbaldridge/try-tf/tree/master/simdata
 *
 * @author Josh Patterson
 * @author Alex Black (added plots)
 */
@SuppressWarnings("DuplicatedCode")
public class RNNClassifier_DL4J {
// 

    
    private static final Logger log = LoggerFactory.getLogger(RNNClassifier_DL4J.class);
    
    //'baseDir': Base directory for the data. Change this if you want to save the data somewhere else


    private static String featuresDirTrain = "../data/RNNtraindatafeature";
    private static String labelsDirTrain = "../data/RNNtraindatalabel";
    private static String featuresDirTest = "../data/RNNtestdatafeature";
    private static String labelsDirTest = "../data/RNNtestdatalabel";

    public static void training(String[] args) throws Exception {
        int seed = 123;
        double learningRate = 0.0001;

        int nEpochs = 1;
        int numInputs = 10;
        int numOutputs = 2;
        int numHiddenNodes = 128;
        int nlayers = 1;

        int miniBatchSize = 128;
        int numLabelClasses = 2;
        int ntraindata = 34998;
        int ntestdata = 14998;
        
        /***
         * read input args
         */
        
    	for (String arg: args) {
    		String[] arglist = arg.split(":");
    		if (arglist[0].equals("lr")) {
    			learningRate = Double.valueOf(arglist[1]) ;
    		}
    		else if (arglist[0].equals("nh")) {		
    			numHiddenNodes = Integer.valueOf(arglist[1]);
    		}
    		else if (arglist[0].equals("nepoch")) {		
    			nEpochs = Integer.valueOf(arglist[1]);
    		}
    		else {
    			System.out.println("Error, no such arg, the args must be");
    			System.out.println(" lr : learning rate");
    			System.out.println(" nh : number of hidden nodes");
    			System.out.println(" for example the input is (lr:0.001 nh:128)");
    		}
    	}

    	
   
        //dataLocalPath = DownloaderUtility.CLASSIFICATIONDATA.Download();
        //Load the training data:
        SequenceRecordReader trainFeatures = new CSVSequenceRecordReader(0,",");
        trainFeatures.initialize(new NumberedFileInputSplit(featuresDirTrain + "/%d.csv", 0, ntraindata));
        SequenceRecordReader trainLabels = new CSVSequenceRecordReader();
        trainLabels.initialize(new NumberedFileInputSplit(labelsDirTrain + "/%d.csv", 0, ntraindata));
        


//        DataSetIterator trainData = new SequenceRecordReaderDataSetIterator(trainFeatures, trainLabels, miniBatchSize, numLabelClasses,
//            false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);
      DataSetIterator trainData = new SequenceRecordReaderDataSetIterator(trainFeatures, trainLabels, miniBatchSize, numLabelClasses,
    		  false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);
        

        //Normalize the training data
//        DataNormalization normalizer = new NormalizerStandardize();
//        normalizer.fit(trainData);              //Collect training data statistics
//        trainData.reset();

        //Use previously collected statistics to normalize on-the-fly. Each DataSet returned by 'trainData' iterator will be normalized
//        trainData.setPreProcessor(normalizer);


        // ----- Load the test data -----
        //Same process as for the training data.
        SequenceRecordReader testFeatures = new CSVSequenceRecordReader(0,",");
        testFeatures.initialize(new NumberedFileInputSplit(featuresDirTest + "/%d.csv", 0, ntestdata));
        SequenceRecordReader testLabels = new CSVSequenceRecordReader();
        testLabels.initialize(new NumberedFileInputSplit(labelsDirTest + "/%d.csv", 0, ntestdata));
        
        

//        DataSetIterator testData = new SequenceRecordReaderDataSetIterator(testFeatures, testLabels, miniBatchSize, numLabelClasses,
//            false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);
        DataSetIterator testData = new SequenceRecordReaderDataSetIterator(testFeatures, testLabels, miniBatchSize, numLabelClasses,
    		  false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);

//        testData.setPreProcessor(normalizer);   //Note that we are using the exact same normalization process as the training data


        // ----- Configure the network -----
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)    //Random number generator seed for improved repeatability. Optional.
                .weightInit(WeightInit.XAVIER)
                .updater(new Nadam())
                //.gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)  //Not always required, but helps with this data set
                .gradientNormalizationThreshold(0.5)
                .list()
                .layer(new LSTM.Builder().activation(Activation.TANH).nIn(numInputs).nOut(numHiddenNodes).build())
                
//                .layer(new LSTM.Builder().activation(Activation.TANH).nIn(numHiddenNodes).nOut(numHiddenNodes).build())
//                .layer(new LSTM.Builder().activation(Activation.TANH).nIn(numHiddenNodes).nOut(numHiddenNodes).build())
//                .layer(new LSTM.Builder().activation(Activation.TANH).nIn(numHiddenNodes).nOut(numHiddenNodes).build())
//                .layer(new LSTM.Builder().activation(Activation.TANH).nIn(numHiddenNodes).nOut(numHiddenNodes).build())
                
                .layer(new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
                        .activation(Activation.RELU)
                        .build())
                
                
                .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX).nIn(numHiddenNodes).nOut(numLabelClasses).build())
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        log.info("Starting training...");
        double timestart = System.currentTimeMillis();
        net.setListeners(new ScoreIterationListener(100), new EvaluativeListener(testData, 10, InvocationType.EPOCH_END));   //Print the score (loss function value) every 20 iterations
        net.setLearningRate(learningRate);
        net.fit(trainData, nEpochs);
        System.out.println("time cost = "+ (System.currentTimeMillis()-timestart));
    
        log.info("Evaluating...");
        Evaluation eval = net.evaluate(testData);
        log.info(eval.stats());
        
        net.save(new File("RNNmodel"));

        log.info("----- Example Complete -----");
    }
    
    public static void testing(String[] args) throws Exception {
    	
        int ntestdata = 10000;
        int miniBatchSize = 128;
        //int[] batch_list = {1,8,16,32,64,128,256,512,1024,2048,10000};
        int[] batch_list = {1024,2048};
        int numLabelClasses = 2;
        int nBatch = (ntestdata/miniBatchSize);
        

        // ----- Load the test data -----
        //Same process as for the training data.
        SequenceRecordReader testFeatures = new CSVSequenceRecordReader(0,",");
        testFeatures.initialize(new NumberedFileInputSplit(featuresDirTest + "/%d.csv", 0, ntestdata));
        SequenceRecordReader testLabels = new CSVSequenceRecordReader();
        testLabels.initialize(new NumberedFileInputSplit(labelsDirTest + "/%d.csv", 0, ntestdata));
    	
        DataSetIterator testData = new SequenceRecordReaderDataSetIterator(testFeatures, testLabels, miniBatchSize, numLabelClasses,
    		  false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);
//        
//        DataSet sample = testData.next();
//
        DataSet next = testData.next();
        INDArray features = next.getFeatures().dup();
        INDArray labels = next.getLabels().dup();
        
        int nload = 0;
        while (testData.hasNext()) {
        	
        	//System.out.println(nload);
        	next = testData.next();
        	INDArray feature0 = next.getFeatures();
        	INDArray label0 = next.getLabels();
//        	System.out.println(feature0);
//        	System.out.println(label0);
            features = Nd4j.concat(0, features, feature0.dup());
            labels = Nd4j.concat(0, labels, label0.dup());
            nload ++;
        }

    	
    	MultiLayerNetwork net = MultiLayerNetwork.load(new File("RNNmodel"), false);
    	
    	
        log.info("Evaluating...");
        Evaluation eval = net.evaluate(testData);
        log.info(eval.stats());

        for (int n =0;n<(batch_list.length);n++) {
        	
        	nBatch = ntestdata/batch_list[n];
        	miniBatchSize = batch_list[n];
        	
            double timestart = System.currentTimeMillis();

        	for (int batch=0;batch<nBatch;batch++) {
            	INDArrayIndex index = NDArrayIndex.interval(batch*miniBatchSize,(batch+1)*miniBatchSize);
            	net.output(features.get(index)).argMax(1).getDouble(-1);   ;        		
        	}
        	
    		double timeend = System.currentTimeMillis();
    		System.out.print("batch size = "+batch_list[n]);
    		System.out.println(" time cost = "+ (timeend-timestart));
        }
        


        
        //NDArrayIndex id = new NDArrayIndex(0);
//        
        //new List() samplelist = [0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5];

    	
//
//        
//        for (int i=0;i<10000;i++) {
//        	net.output(testsample1).argMax(1).getDouble(-1);        	
//        	//System.out.print(net.output(testsample1).argMax(1).getDouble(-1));
//        }
        

    	
    	//System.out.print(net.evaluate(testData).getPredictions(miniBatchSize, numLabelClasses).toString());
    	//System.out.print(net.output(testsample1));
    	

    	//System.out.print(testlabel);
    }
    
    
    public static void main(String[] args) throws Exception {
    	
    	//training(args);
    	testing(args);
    	
    }
    
    
    
}


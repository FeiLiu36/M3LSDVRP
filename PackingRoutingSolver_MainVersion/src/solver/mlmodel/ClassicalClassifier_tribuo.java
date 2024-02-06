

package solver.mlmodel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.tribuo.*;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.dtree.CARTClassificationTrainer;

import org.tribuo.classification.libsvm.*;
import org.tribuo.classification.libsvm.SVMClassificationType.SVMMode;
import org.tribuo.common.libsvm.*;
import org.tribuo.common.tree.RandomForestTrainer;

import static org.tribuo.common.tree.AbstractCARTTrainer.MIN_EXAMPLES;

import org.tribuo.classification.xgboost.XGBoostClassificationTrainer;
import org.tribuo.classification.dtree.impurity.*;
import org.tribuo.classification.ensemble.VotingCombiner;
import org.tribuo.classification.evaluation.LabelEvaluation;
import org.tribuo.classification.evaluation.LabelEvaluator;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.evaluation.TrainTestSplitter;
import org.tribuo.classification.liblinear.LibLinearClassificationTrainer;
import org.tribuo.classification.mnb.MultinomialNaiveBayesTrainer;
import org.tribuo.classification.sgd.fm.FMClassificationTrainer;
import org.tribuo.classification.sgd.kernel.KernelSVMTrainer;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;


import solver.routing.jmetal.util.JMException;



public class ClassicalClassifier_tribuo {
	

	public void runAll(FileOutputStream acc_results, FileOutputStream recall_results, FileOutputStream prec_results,
			MutableDataset<Label> testData, Model<Label> model ,String modelName, boolean volumeConstraint, String data_type) throws IOException {
		
		if (!volumeConstraint) {
			
			acc_results.write(modelName.getBytes());
			recall_results.write(modelName.getBytes());
			prec_results.write(modelName.getBytes());
			

			
			System.out.println("*** size of testData: "+testData.size());
			
			LabelEvaluation evaluation = new LabelEvaluator().evaluate(model,testData);
			
			System.out.println(evaluation.toString());
			
			acc_results.write((String.valueOf(evaluation.accuracy())+"  ").getBytes());

			
			acc_results.write("\n".getBytes());
			recall_results.write("\n".getBytes());
			prec_results.write("\n".getBytes());	
		}
		else {
			
			acc_results.write(modelName.getBytes());
			recall_results.write(modelName.getBytes());
			prec_results.write(modelName.getBytes());

			for (double v_percent=0.3; v_percent<1.0;v_percent+=0.1) {
				MutableDataset<Label> testDataV = new MutableDataset<>(testData.getProvenance(),testData.getOutputFactory());
				//testDataV.clear();
				
				if (data_type == "sequential") {
					for (int i=0; i<testData.size();i++) {
						//if (testData.getExample(i).get)
						Example midExample = testData.getExample(i);
						double v_midExample = 0;
						for (int j=0; j<midExample.size();j++) {
							if (j%10==0) {
								v_midExample += midExample.lookup(String.valueOf(j)).getValue();
								
							}
							
						}
						if (v_midExample < v_percent) {
							testDataV.add(midExample);
						}
					}
				}
				else if (data_type=="global") {
					for (int i=0; i<testData.size();i++) {
						//if (testData.getExample(i).get)
						Example midExample = testData.getExample(i);
						double v_midExample = midExample.lookup("P_v").getValue();
						if (v_midExample < v_percent) {
							testDataV.add(midExample);
						}
					}					
				}

				
				System.out.println("*** size of testData: "+testDataV.size());
			
				LabelEvaluation evaluation = new LabelEvaluator().evaluate(model,testDataV);
				
				System.out.println(evaluation.toString());
				
				acc_results.write((String.valueOf(evaluation.accuracy())+"  ").getBytes());
				recall_results.write((String.valueOf(evaluation.recall(new Label("1")))+"  ").getBytes());
				prec_results.write((String.valueOf(evaluation.precision(new Label("1")))+"  ").getBytes());


			}
			acc_results.write("\n".getBytes());
			recall_results.write("\n".getBytes());
			prec_results.write("\n".getBytes());

			
		}
	

	}
	
	public void runOne(FileOutputStream acc_results, FileOutputStream recall_results, FileOutputStream prec_results,
			FileOutputStream acc_results_pw, FileOutputStream recall_results_pw, FileOutputStream prec_results_pw,
			MutableDataset<Label> testData, Model<Label> model ,String modelName) throws IOException {
		
	}
	
	public void runTraining( String data_type, String input_dir, String output_dir, boolean isVolumeConstraint) throws IOException{
		
		
		DataSource<Label> loadData = null;

		if (data_type == "global") {
			// Load labelled iris data
			String[] dataHeaders = new String[]{"P_v", "P_w", "N_b", "N_tb", "N_c", "A_v", "V_v", "A_h", "V_h", "A_l", "V_l", "A_w"
					, "V_w","label"};
			loadData =
			        new CSVLoader<>(new LabelFactory()).loadDataSource(Paths.get(input_dir+"data_global13_50000.dat"),
			                                     /* Output column   */ dataHeaders[13],
			                                     /* Column headers  */ dataHeaders);

		}
		
		else if (data_type == "sequential") {
			
			String[] dataHeaders = new String[] {};
			ArrayList<String> list = new ArrayList<>(Arrays.asList(dataHeaders));
			
			int nfeatures = 11;
			int nwarehouses = 15;
					
			for (int nf = 0 ; nf < nfeatures*nwarehouses; nf++) {
				list.add(String.valueOf(nf));
			}
			list.add("label");
			dataHeaders = list.toArray(new String[list.size()]);
	
			loadData = new CSVLoader<>(new LabelFactory()).loadDataSource(Paths.get(input_dir+"data_sequantial165_50000.csv"),
				                                     /* Output column   */ dataHeaders[nfeatures*nwarehouses],
				                                     /* Column headers  */ dataHeaders);

		}
		
		else {
			System.out.println("wrong data type !");
			System.exit(-1);
		}
		
		TrainTestSplitter<Label> splitData = new TrainTestSplitter<>(loadData,
                /* Train fraction */ 0.7,
                      /* RNG seed */ 1);
		
		MutableDataset<Label> trainData = new MutableDataset<>(splitData.getTrain());
		MutableDataset<Label> testData = new MutableDataset<>(splitData.getTest());
		
	
		int number_classifiers = 7;
		
		FileOutputStream acc_results   = new FileOutputStream(output_dir+"results_acc_"+data_type+".dat");
		FileOutputStream recall_results   = new FileOutputStream(output_dir+"results_recall_"+data_type+".dat");
		FileOutputStream prec_results   = new FileOutputStream(output_dir+"results_prec_"+data_type+".dat");

				
			
		for (int classifier_type = 0; classifier_type<number_classifiers; classifier_type++){
			
			// linear classifier
			if (classifier_type==0&& data_type == "global") {
				double timeStart = System.currentTimeMillis();
				
				String modelName = "linear  ";
				
				System.out.println("### linear classifier ###");
				
				LibLinearClassificationTrainer LibLinTrainer = new LibLinearClassificationTrainer();
				Model<Label> model = LibLinTrainer.train(trainData);
				
				File tmpFile = new File(output_dir+"model_Linear_"+data_type+".ser");
				try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmpFile))) {
				    oos.writeObject(model);
				}
				
				runAll( acc_results,  recall_results, prec_results,
						testData,  model ,modelName,isVolumeConstraint,data_type);
				
				double timeRun = (System.currentTimeMillis()-timeStart)/1000;
				System.out.print(modelName+" running time = "+timeRun);
			}
			
//			// multinomialNaiveBayes classifier
//			if (classifier_type==1) {
//				double timeStart = System.currentTimeMillis();
//				
//				String modelName = "MNB  ";
//
//				
//				System.out.println("### multinomialNaiveBayes ###");
//				
//				MultinomialNaiveBayesTrainer MBayesTrainer = new MultinomialNaiveBayesTrainer();
//				Model<Label> model = MBayesTrainer.train(trainData);
//
//				
//				File tmpFile = new File("MultiNB_model.ser");
//				try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmpFile))) {
//				    oos.writeObject(model);
//				}	
//				
//				runAll( acc_results,  recall_results, prec_results,
//						 acc_results_pw, recall_results_pw, prec_results_pw,
//						testData,  model ,modelName,isVolumeConstraint);	
//				
//				double timeRun = (System.currentTimeMillis()-timeStart)/1000;
//				System.out.print(modelName+" running time = "+timeRun);
//			}
			
			// logistic regression classifier
			if (classifier_type==2&& data_type == "global") {
				
				double timeStart = System.currentTimeMillis();
				
				String modelName = "LR  ";
				
				System.out.println("### logistic regression ###");
				
				LogisticRegressionTrainer LRTrainer = new LogisticRegressionTrainer();
				Model<Label> model = LRTrainer.train(trainData);
		        		
				
				File tmpFile = new File(output_dir+"model_LR_"+data_type+".ser");
				try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmpFile))) {
				    oos.writeObject(model);
				}		
				
				runAll( acc_results,  recall_results, prec_results,
						testData,  model ,modelName,isVolumeConstraint,data_type);	
				
				double timeRun = (System.currentTimeMillis()-timeStart)/1000;
				System.out.print(modelName+" running time = "+timeRun);
				
			}
			
			// svm with rbf classifier 
			// skip svm when using sequential data, because it is too time-consuming
			if (classifier_type==3 && data_type == "global") {
				
				double timeStart = System.currentTimeMillis();
				
				String modelName = "SVM  ";
				
				System.out.println("### SVM ###");
				
				LibSVMClassificationTrainer SVMTrainer = new LibSVMClassificationTrainer(new SVMParameters<>(new SVMClassificationType(SVMMode.C_SVC), KernelType.RBF));
				Model<Label> model = SVMTrainer.train(trainData);

				
				File tmpFile = new File(output_dir+"model_SVM_RBF_"+data_type+".ser");
				try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmpFile))) {
				    oos.writeObject(model);
				}	
				
				runAll( acc_results,  recall_results, prec_results,
						testData,  model ,modelName,isVolumeConstraint,data_type);	
				
				double timeRun = (System.currentTimeMillis()-timeStart)/1000;
				System.out.print(modelName+" running time = "+timeRun);
				
			}

			
			// decision tree
			
			if (classifier_type==4) {
				
				double timeStart = System.currentTimeMillis();
				
				String modelName = "CART  ";
				
				System.out.println("### CART ###");
				
				CARTClassificationTrainer cartTrainer = new CARTClassificationTrainer();
				Model<Label> model = cartTrainer.train(trainData);
				
				File tmpFile = new File(output_dir+"model_CART_"+data_type+".ser");
				try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmpFile))) {
				    oos.writeObject(model);
				}
				
				runAll( acc_results,  recall_results, prec_results,
						testData,  model ,modelName,isVolumeConstraint,data_type);	
				
				double timeRun = (System.currentTimeMillis()-timeStart)/1000;
				System.out.print(modelName+" running time = "+timeRun);				
			}


			// random forest with 10 trees
			
			if (classifier_type==5) {
				
				double timeStart = System.currentTimeMillis();		
				
				String modelName = "RF  ";
				
				System.out.println("### random forest with 50 trees  ###");
				
			    final CARTClassificationTrainer subsamplingTree = new CARTClassificationTrainer(Integer.MAX_VALUE,
			            MIN_EXAMPLES,0.0f,  0.5f, false, new GiniIndex(), Trainer.DEFAULT_SEED);
				RandomForestTrainer<Label> rfT = new RandomForestTrainer<>(subsamplingTree,new VotingCombiner(),50);
				Model<Label> model = rfT.train(trainData);
				
				File tmpFile = new File(output_dir+"model_RF50_"+data_type+".ser");
				try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmpFile))) {
				    oos.writeObject(model);
				}	
				
				runAll( acc_results,  recall_results, prec_results,
						testData,  model ,modelName,isVolumeConstraint,data_type);	
				
				double timeRun = (System.currentTimeMillis()-timeStart)/1000;
				System.out.print(modelName+" running time = "+timeRun);				
			}	
			

		}
		


	}
	
	public void runTesting(int classifier_type) throws IOException, ClassNotFoundException{
		
		DataSource<Label> irisData;
		
		if (classifier_type >= 10) {
			
			// Load labelled iris data
			String[] dataHeaders = new String[] {};
			ArrayList<String> list = new ArrayList<>(Arrays.asList(dataHeaders));
			
			for (int nfeatures = 0 ; nfeatures < 11*15; nfeatures++) {
				list.add(String.valueOf(nfeatures));
			}
			list.add("label");
			dataHeaders = list.toArray(new String[list.size()]);
			
			irisData =
			        new CSVLoader<>(new LabelFactory()).loadDataSource(Paths.get("data50000.csv"),
			                                     /* Output column   */ dataHeaders[11*15],
			                                     /* Column headers  */ dataHeaders);
			
		}
		else {
			
			
			// Load labelled iris data
				String[] irisHeaders = new String[]{"P_v", "P_w", "N_b", "N_tb", "N_c", "A_v", "V_v", "A_h", "V_h", "A_l", "V_l", "A_w"
						, "V_w","label"};
				irisData =
				        new CSVLoader<>(new LabelFactory()).loadDataSource(Paths.get("data.dat"),
				                                     /* Output column   */ irisHeaders[13],
				                                     /* Column headers  */ irisHeaders);

		}
		
		// Split iris data into training set (70%) and test set (30%)
		TrainTestSplitter<Label> splitIrisData = new TrainTestSplitter<>(irisData,
		                       /* Train fraction */ 0.7,
		                             /* RNG seed */ 1L);
		MutableDataset<Label> trainData = new MutableDataset<>(splitIrisData.getTrain());
		MutableDataset<Label> testData = new MutableDataset<>(splitIrisData.getTest());
		

	
		
		Model<Label> classifier = null;
		
		if (classifier_type == 0) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream("Linear_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 1) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream("MultiNB_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 2) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream("LR_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
	
		else if (classifier_type == 3) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream("SVM_RBF_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 4) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream("CART_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 5) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream("RF10_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 6) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream("RF20_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 7) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream("RF50_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 21) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream("CART_model_sequential.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 25) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream("RF50_model_sequential.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}

		
		System.out.println("start prediction");
		double timestart = System.currentTimeMillis();
		
		for (int repeat=0;repeat<5;repeat++) {
			for (int ncheck=0;ncheck<2000;ncheck++) {
				Prediction<Label> prediction = classifier.predict(testData.getExample(ncheck));
			}
			
		}

		
		
		double timeend = System.currentTimeMillis();
		System.out.println("time cost = "+ (timeend-timestart));
		System.out.println("end prediction");
		
	
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, JMException {
		

		String data_type = "sequential";
		int model_type = 4;
	    boolean isTraining = true;
	    boolean isVolumeConstraint = true;
	    
	    
	    String input_dir = "./data_ML/";
	    	
	    String output_dir = "./results_ML/";
		
		ClassicalClassifier_tribuo test = new ClassicalClassifier_tribuo();
		
		if (isTraining) {		
			test.runTraining(data_type,input_dir,output_dir,isVolumeConstraint);
		}
		else {
			test.runTesting(model_type);
		}
	}
}

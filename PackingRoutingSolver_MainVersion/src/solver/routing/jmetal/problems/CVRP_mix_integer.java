//  MOEAD_SDVRP 
//  For EMO2021 Huawei VRP competition
//
//  Author:         LIU Fei  
//  E-mail:         fliu36-c@my.cityu.edu.hk
//  Create Date:    2021.2.1
//  Last modified   Date: 2021.2.15
//

package solver.routing.jmetal.problems;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.tribuo.Example;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.impl.ArrayExample;

import solver.Route;
import solver.packing.Packing;
import solver.routing.jmetal.core.Problem;
import solver.routing.jmetal.core.Solution;
import solver.routing.jmetal.encodings.solutionType.PermutationBinarySolutionType;
import solver.routing.jmetal.encodings.variable.Permutation;
import solver.routing.jmetal.util.JMException;
import solver.util.Box;
import solver.util.Carriage;
import solver.util.Node;


//import solver.Track1;

/**
 * Class representing a multi-objective TSP (Traveling Salesman Problem) problem.
 * This class is tested with two objectives and the KROA150 and KROB150 
 * instances of TSPLIB
 */
public class CVRP_mix_integer extends Problem {

  public int         numberOfCities_ ; 
  public double [][] distanceMatrix_ ;
  public double [][] costMatrix_;
  public int [] new_node_no;
  public double [] new_node_v;
  
  /** The capacity that all vehicles in fruitybun-data.vrp have. */
  public static double[] VEHICLE_CAPACITY ;
  public static double[] VEHICLE_VOLUME ;
  public static double[][] nodes_v_w_0;
  public static HashMap<String, Double> Distances;
  //public static final int VEHICLE_CAPACITY2 = 100;

  /** The number of nodes in the fruitybun CVRP i.e. the depot and the customers */
  public static int NUM_trucks = 10;
  //public static final int Type_trucks = 3;
  public static int NUM_nodes;
  public static double Split_minv = 0;
  public static int If_check = 0;
  public static final double Split_precision = 6.0;
  public static double Relax_ratio = 0;
  public static double Relax_volume = 0;
  public static double[] truck_weight_ratio;
  ArrayList<Node> clients;
  ArrayList<Node> clients_trans;
  ArrayList<Node> clients_half_trans;
  public int[] if_large_box;
  public double[] if_hard_node;
  
  static ArrayList<Carriage> BASIC_TRUCKS ;
  static Node depot_start ;
  static Node depot_end ;	
  static HashMap distanceMap;
  static Model<Label> classifier;
  static MultiLayerNetwork RNNnet;
  static int classifier_type;
  static int packing_type;
  static String model_directory;
  
  public static double threshold;
  
  
	/** 
	* Creates a new VRP problem with expensive packing constraints
	* @throws ClassNotFoundException 
	*/
  public CVRP_mix_integer(String solutionType,
              double split_minv,
              double[] VEHICLE_CAPACITY0,
              double[] VEHICLE_VOLUME0,
              int NUM_nodes0,
              HashMap<String, Double> Distances0,
              double[][] nodes_v_w0,
              double ralax_ratio0,
//              double[] truck_weight_ratio0,
              ArrayList<Node> clients0,
              ArrayList<Node> clients_trans0,
              ArrayList<Node> clients_half_trans0,
              int[] if_large_box0,
              HashMap distanceMap0,
              ArrayList<Carriage> BASIC_TRUCKS0,
              Node depot_start0,
              Node depot_end0,
              double threshold0,
              int classifier_type0,
              int packing_type0,
              String model_directory0
              ) throws IOException, ClassNotFoundException {
    //numberOfVariables_  = 2;
	Split_minv = split_minv;
    numberOfObjectives_ = 2;
    numberOfConstraints_= 2;
    problemName_        = "CVRP_mix";
    VEHICLE_CAPACITY = VEHICLE_CAPACITY0;
    VEHICLE_VOLUME = VEHICLE_VOLUME0; 
    //nodes_v_w = nodes_v_w0;
    nodes_v_w_0  = nodes_v_w0;
    NUM_nodes = NUM_nodes0;
    Distances = Distances0;
    Relax_ratio = ralax_ratio0;
    Relax_volume = Relax_ratio*VEHICLE_VOLUME[0];
//    truck_weight_ratio = truck_weight_ratio0;
    //If_check = if_check;
    clients = clients0;
    clients_trans = clients_trans0;
    clients_half_trans = clients_half_trans0;
    if_large_box = if_large_box0;
    NUM_trucks = VEHICLE_CAPACITY.length;
    distanceMap = distanceMap0;
    
    BASIC_TRUCKS = BASIC_TRUCKS0;
    depot_start = depot_start0 ;
    depot_end = depot_end0 ;
    threshold = threshold0;
    classifier_type = classifier_type0;
    packing_type = packing_type0;
    model_directory = model_directory0;

	double[][] nodes_v_w = new double[NUM_nodes][2]; //client_volume_weight
    for(int i=0;i<NUM_nodes;i++) {
    	nodes_v_w[i][0] = nodes_v_w_0[i][0];
    	nodes_v_w[i][1] = nodes_v_w_0[i][1];
    }

    int new_num_nodes = 0;
             
//    length_       = new int[numberOfVariables_];
    
    for (int i = 0; i < NUM_nodes; i++) {
    	while (nodes_v_w[i][0]> 1.1*VEHICLE_VOLUME[VEHICLE_CAPACITY.length-1]) {
    		nodes_v_w[i][0] -= truck_weight_ratio[i]*VEHICLE_VOLUME[VEHICLE_CAPACITY.length-1];	
    	}
    	if  (nodes_v_w[i][0] > Split_minv) {//è¦�ä¸�è¦�åˆ†å‰²ã€‚
        	new_num_nodes += 2; 
    	}
    	else {
        	new_num_nodes += 1; 	
    	} 
    	//System.out.println(nodes_v_w[i][0]);
    }

     numberOfVariables_ = new_num_nodes - NUM_nodes + 2;//è‡³å°‘ä¸€å€‹ç¯€é»žã€‚
     length_       = new int[numberOfVariables_];
     length_      [0] = new_num_nodes;
     length_      [1] = new_num_nodes-1;
     for (int i = 2; i < numberOfVariables_; i++) {
    	 length_      [i] = 2; 
     }
     

     numberOfCities_ = new_num_nodes;
     
     if (solutionType.compareTo("PermutationBinary") == 0)
     	solutionType_ = new PermutationBinarySolutionType(this,1,numberOfVariables_-1) ;
     else {
     	System.out.println("Error: solution type " + solutionType + " invalid") ;
     	System.exit(-1) ;
     }
     


		if (classifier_type == 0) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"Linear_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 1) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"MultiNB_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 2) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"LR_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
	
		else if (classifier_type == 3) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"SVM_RBF_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 4) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"CART_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 5) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"RF10_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 6) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"RF20_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 7) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"RF50_model.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 10) {
				//try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"RNNmodel")))) {
				    //ois.setObjectInputFilter(filter);
				RNNnet = MultiLayerNetwork.load(new File(model_directory+"RNNmodel"), true);
				//}
		}
		else if (classifier_type == 20) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"CART_model_sequential.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 25) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"RF50_model_sequential.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		else if (classifier_type == 21) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model_directory+"RF10_model_sequential.ser")))) {
			    //ois.setObjectInputFilter(filter);
			classifier = (Model<Label>) ois.readObject();
			}
		}
		
		else if (classifier_type == 100) {
			// using expensive packing heuristics for each check
		}
		
     
     
  } // mTSP    
 /** 
  * Evaluates a solution 
  * @param solution The solution to evaluate
  */      
  public void evaluate(Solution solution) {
    double fitness1   ;
    double fitness2   ;
    double weights_precent ;
    int n_truck_used ;
//    double w_const ;
    int new_num_nodes = 0;
   
    
    fitness1   = 0.0 ;
    fitness2   = 0.0 ;
    weights_precent = 0.0;//ç•¶å‰�çš„è¼‰é‡�ã€‚
    n_truck_used = 0;
//    w_const = 0;
    int Type_trucks = VEHICLE_CAPACITY.length;
    
       
    //new_node_coords = new double[numberOfCities_][2];
    new_node_no = new int [numberOfCities_];//æ–°ç¯€é»žçš„ç·¨è™Ÿã€‚ã€‚
    new_node_v = new double[numberOfCities_];//æ–°ç¯€é»žçš„é«”ç©�ã€‚
    
	double[][] nodes_v_w = new double[NUM_nodes][2]; //client_volume_weight
    for(int i=0;i<NUM_nodes;i++) {
    	nodes_v_w[i][0] = nodes_v_w_0[i][0];
    	nodes_v_w[i][1] = nodes_v_w_0[i][1];
    }
    
    for (int i = 0; i < NUM_nodes; i++) {
    	while (nodes_v_w[i][0]> 1.1*VEHICLE_VOLUME[VEHICLE_CAPACITY.length-1]) {
    		nodes_v_w[i][0] -= truck_weight_ratio[i]*VEHICLE_VOLUME[VEHICLE_CAPACITY.length-1];	
    	}
//    	if  (nodes_v_w[i][0] > Split_minv) {//è¦�ä¸�è¦�åˆ†å‰²ã€‚
//        	new_num_nodes += 2; 
//    	}
//    	else {
//        	new_num_nodes += 1; 	
//    	} 
    	//System.out.println(nodes_v_w[i][0]);
    }
    

    int var = 2;
    for (int i = 0; i < NUM_nodes; i++) {
    	if  (nodes_v_w[i][0] > Split_minv) {//æœ‰æ–°å¢žçš„ç¯€é»ž
    		String s = solution.getDecisionVariables()[var].toString() ; 
    	    char [] s0 = s.toCharArray();
    	    double s00 = 0;
    	    for (int j=0;j<2;j++) {
    	    	s00 += (s0[j]- 48)*Math.pow(2, j);//å°‡äºŒé€²åˆ¶è½‰æ�›ç‚ºæ•´æ•¸ã€‚0,1,2,3
    	    	//System.out.println((s0[j]- 48)*2^j);
    	    }
    	    //new_node_coords[new_num_nodes] = node_coords[i];
    	    new_node_no[new_num_nodes] = i+1;//æ–°å¢žä¸€å€‹è§£é»žï¼Œ
        	new_node_v[new_num_nodes] = nodes_v_w[i][0]*(s00/Split_precision);//æ–°å¢žç¯€é»žå°�æ‡‰çš„é«”ç©�ã€‚
        	new_num_nodes += 1; 
    	    //new_node_coords[new_num_nodes] = node_coords[i];
        	new_node_no[new_num_nodes] = i+1;
        	new_node_v[new_num_nodes] = nodes_v_w[i][0]*(1.0-s00/Split_precision);//6.0
        	new_num_nodes += 1; 
    		
    		var += 1;
    	}
    	else {//å¦‚æžœé€™å€‹ç¯€é»žçš„é«”ç©�å¾ˆå°�ï¼Œä¸€å€‹è»Šå­�å�¯ä»¥è£�ä¸‹ã€‚æ²’æœ‰æ–°å¢žçš„ç¯€é»žã€‚
    		//new_node_coords[new_num_nodes] = node_coords[i];
    		new_node_no[new_num_nodes] = i+1;//é€™å€‹ç¯€é»žçš„ç·¨è™Ÿã€‚
    		new_node_v[new_num_nodes] = nodes_v_w[i][0];    //é€™å€‹ç¯€é»žçš„é«”ç©�ã€‚	
    		new_num_nodes += 1; //ä¸‹ä¸€å€‹ç¯€é»žã€‚	
    	}

    }    
     
    
	String if_start = solution.getDecisionVariables()[1].toString();
	char [] if_start0 = if_start.toCharArray();
	
	int[] route_use_large = new int[numberOfCities_];
	double[] route_hard = new double[numberOfCities_];
	int nroute = 0;
    for (int i = 0; i < (numberOfCities_ ); i++) {
    	route_hard[i] = 1.0;

        int x ; 
        x = ((Permutation)solution.getDecisionVariables()[0]).vector_[i] ;  
        if(i>0) {
            if (  (if_start0[i-1]- 48) == 1) {
            	nroute += 1;
            }
        }
        if ( if_large_box[new_node_no[x]-1]==1) {
        	route_use_large[nroute] = 1;
        }
//        if (if_hard_node[new_node_no[x]-1]<route_hard[nroute]) {
//        	route_hard[nroute] = if_hard_node[new_node_no[x]-1];
//        }
        //route_hard[nroute] =  route_hard[nroute]*if_hard_node[new_node_no[x]-1];
    }
	
	
    double VEHICLE_CAPACITY_precent;
    VEHICLE_CAPACITY_precent = 0;
    
    nroute = 0;
    for (int i = 0; i < (numberOfCities_ ); i++) {
      int x ; 
      int x_last;

      
      x = ((Permutation)solution.getDecisionVariables()[0]).vector_[i] ;   
      if (i==0) {
    	  fitness1 += distance2_start_end(0,x);//å¾žèµ·é»žåˆ°xé€™å€‹ç¯€é»žçš„è·�é›¢ã€‚
    	  weights_precent = new_node_v[x];//é€™å€‹ç¯€é»žçš„é«”ç©�ã€‚
      }
      else {
          x_last = ((Permutation)solution.getDecisionVariables()[0]).vector_[i-1] ; //ä¸Šä¸€å€‹ç¯€é»žã€‚
          //System.out.println(if_start0[i-1]);
          if ( (if_start0[i-1]- 48) == 1) {//æ˜¯å�¦é–‹å§‹ç¯€é»žã€‚
        	  fitness1 += distance2_start_end(NUM_nodes+1,x_last);
        	  fitness1 += distance2_start_end(0,x);
        	  if (route_use_large[nroute] == 1) {
        		  VEHICLE_CAPACITY_precent = (VEHICLE_VOLUME[Type_trucks-1]*Relax_ratio);
        	  }
        	  else {
            	  for (int j = 0; j < Type_trucks; j++) {
            		  if(route_hard[nroute]*(VEHICLE_VOLUME[j]*Relax_ratio)>weights_precent) {
            			  VEHICLE_CAPACITY_precent = route_hard[nroute]*(VEHICLE_VOLUME[j]*Relax_ratio);
            			  break;
            		  }
            		  VEHICLE_CAPACITY_precent = route_hard[nroute]*(VEHICLE_VOLUME[j]*Relax_ratio);
            	     // System.out.println(VEHICLE_CAPACITY_precent );
            	      //System.out.println(VEHICLE_CAPACITY[j]);
            	  }
        	  }

        	  fitness2 += weights_precent/VEHICLE_CAPACITY_precent;
        	  n_truck_used += 1;
//        	  if (weights_precent > VEHICLE_CAPACITY_precent) {
//        		  w_const += 1;
//        	  }
        	  weights_precent = new_node_v[x];
        	  nroute +=1;
          }
          else {
        	  fitness1 += distance2(x,x_last);
        	  weights_precent += new_node_v[x];
          }
      }
      //System.out.println(weights_precent);
      if (i==(numberOfCities_-1 )) {
    	  if (route_use_large[nroute] == 1) {
    		  VEHICLE_CAPACITY_precent = (VEHICLE_VOLUME[Type_trucks-1]*Relax_ratio);
    	  }
    	  else {
	    	  for (int j = 0; j < Type_trucks; j++) {
	    		  if(route_hard[nroute]*(VEHICLE_VOLUME[j]*Relax_ratio)>weights_precent) {
	    			  VEHICLE_CAPACITY_precent = route_hard[nroute]*(VEHICLE_VOLUME[j]*Relax_ratio);
	    			  break;
	    		  }
	    		  VEHICLE_CAPACITY_precent = route_hard[nroute]*(VEHICLE_VOLUME[j]*Relax_ratio);
	    	      //System.out.println(VEHICLE_CAPACITY_precent );
	    	     // System.out.println(VEHICLE_CAPACITY[j]);
	    	  }
    	  }
    	  fitness1 += distance2_start_end(NUM_nodes+1,x);
    	  fitness2 += weights_precent/VEHICLE_CAPACITY_precent;
    	  n_truck_used += 1;
//    	  if (weights_precent > VEHICLE_CAPACITY_precent) {
//    		  w_const += 1;
//    	  }
      }
      //System.out.println(VEHICLE_CAPACITY.toString());
//  cout << "I : " << i << ", x = " << x << ", y = " << y << endl ; 
    //System.out.println(weights_precent);
    } // for
    //fitness1 = fitness1;
    //fitness2 = 1-fitness2/n_truck_used;
    fitness1 = fitness1/10000;
    fitness2 = 1-fitness2/n_truck_used;

    /*
    if(fitness2 < 0) {
    	System.out.println(n_truck_used);
		for(int i=0;i<VEHICLE_VOLUME.length;i++) {
	    	System.out.println(VEHICLE_VOLUME[i]);
		  }
    	System.out.println(VEHICLE_CAPACITY_precent);
    	System.out.println(w_const);
    	System.out.println(Ralax_ratio);
    	System.out.println("check");
    }
    */
    //fitness2 = n_truck_used;
    
    solution.setObjective(0, fitness1);            
    solution.setObjective(1, fitness2);
  } // evaluate
  
  
  
  public void evaluateConstraints(Solution solution) throws JMException {

	    double weights_precent ;
//	    int n_truck_used ;
	    double w_const ;
	    double first_const ;
	    double w_all = 0.0;
//	    int new_num_nodes = 0;

	    weights_precent = 0.0;
//	    n_truck_used = 0;
	    w_const = 0;
	    first_const = 0;
	    int Type_trucks = VEHICLE_CAPACITY.length;
	    
		String if_start = solution.getDecisionVariables()[1].toString();
		char [] if_start0 = if_start.toCharArray();
		
	    
		int[] route_use_large = new int[numberOfCities_];
		double[] route_hard = new double[numberOfCities_];
		int nroute = 0;
	    for (int i = 0; i < (numberOfCities_ ); i++) {
	    	route_hard[i] = 1.0;

	        int x ; 
	        x = ((Permutation)solution.getDecisionVariables()[0]).vector_[i] ;  
	        if(i>0) {
	            if (  (if_start0[i-1]- 48) == 1) {
	            	nroute += 1;
	            }
	        }
	        if ( if_large_box[new_node_no[x]-1]==1) {
	        	route_use_large[nroute] = 1;
	        }
//	        if (if_hard_node[new_node_no[x]-1]<route_hard[nroute]) {
//	        	route_hard[nroute] = if_hard_node[new_node_no[x]-1];
//	        }
	        //route_hard[nroute] =  route_hard[nroute]*if_hard_node[new_node_no[x]-1];
	    }
		
	    nroute = 0;
	    
	    for (int i = 1; i < numberOfCities_; i++) {
	    	//System.out.println(clients.get(new_node_no[i]-1).isMustFirst());
	    	int x;
	    	int x_last;
	    	x = ((Permutation)solution.getDecisionVariables()[0]).vector_[i] ;
	    	x_last = ((Permutation)solution.getDecisionVariables()[0]).vector_[i-1] ; 
	    	if (clients.get(new_node_no[x]-1).isMustFirst()) {
	    		int check_last;
	    		if(i-2>=0) {
	    			check_last = (if_start0[i-2]- 48);
	    		}
	    		else {
	    			check_last = 1;
	    		}
	    		if((if_start0[i-1]- 48) == 1 || (check_last == 1 && new_node_no[x]==new_node_no[x_last])){
	    			first_const += 0;
	    		}
	    		else {
	    			first_const += 1E10;
	    		}
	    	}
	    }
	    
        double VEHICLE_CAPACITY_precent;
        VEHICLE_CAPACITY_precent = 0;
	    for (int i = 0; i < (numberOfCities_ ); i++) {
	        int x ; 
	        int y ;
	        int x_last;

	        x = ((Permutation)solution.getDecisionVariables()[0]).vector_[i] ;   
	        if (i==0) {

	      	  weights_precent = new_node_v[x];
	        }
	        else {
	            x_last = ((Permutation)solution.getDecisionVariables()[0]).vector_[i-1] ; 
	            //System.out.println(if_start0[i-1]);
	            if ( (if_start0[i-1]- 48) == 1) {

	          	  for (int j = 0; j < Type_trucks; j++) {
	          		  if(route_hard[nroute]*(VEHICLE_VOLUME[j]*Relax_ratio)>weights_precent) {
	          			  VEHICLE_CAPACITY_precent = route_hard[nroute]*(VEHICLE_VOLUME[j]*Relax_ratio);
	          			  break;
	          		  }
	          		  VEHICLE_CAPACITY_precent =route_hard[nroute]* (VEHICLE_VOLUME[j]*Relax_ratio);
	          	  }

			      //System.out.println(weights_precent/VEHICLE_CAPACITY_precent);
//	          	  n_truck_used += 1;
		      	  //System.out.println(VEHICLE_CAPACITY_precent);
	          	  if (weights_precent > VEHICLE_CAPACITY_precent) {
	          		  w_const += weights_precent - VEHICLE_CAPACITY_precent;
	          	  }
	          	  w_all += weights_precent;
	          	  weights_precent = new_node_v[x];
	          	  nroute +=1;
	            }
	            else {

	          	  weights_precent += new_node_v[x];
	            }
	        }
	        //System.out.println(weights_precent);
	        if (i==(numberOfCities_-1 )) {
	          w_all += weights_precent;
	      	  for (int j = 0; j < Type_trucks; j++) {
	      		  if(route_hard[nroute]*(VEHICLE_VOLUME[j]*Relax_ratio)>weights_precent) {
	      			  VEHICLE_CAPACITY_precent = route_hard[nroute]*(VEHICLE_VOLUME[j]*Relax_ratio);
	      			  break;
	      		  }
	      		  VEHICLE_CAPACITY_precent = route_hard[nroute]*(VEHICLE_VOLUME[j]*Relax_ratio);
	      	  }

	      	  
		      //System.out.println(weights_precent/VEHICLE_CAPACITY_precent);
//	      	  n_truck_used += 1;
	      	  //System.out.println(VEHICLE_CAPACITY_precent);
	      	  if (weights_precent > VEHICLE_CAPACITY_precent) {
	      		  w_const += weights_precent - VEHICLE_CAPACITY_precent;
	      	  }
	        }

	  //  cout << "I : " << i << ", x = " << x << ", y = " << y << endl ; 
	       // System.out.println(weights_precent);
	      }
	    
	    
	    //double packingPenalty = 0.0;
	    double packingPenalty = 0.0;
	    //double penalty = 0.0;
	    try {
	    	packingPenalty = check_Routes(Split_minv,solution,Relax_ratio,truck_weight_ratio,if_hard_node);
	    	//packingPenalty = check_Routes(Split_minv,solution,Relax_ratio,truck_weight_ratio,if_hard_node);
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	    
//	   System.out.println(packingPenalty);
//	   System.out.println("w_const"+w_const);
	    
	    int number = 0;
	    double total = 0.0;
	    
 	    if (first_const > 0) {
	    	number += 1;
	    	total -= first_const;
	    }

 	    if ( w_const > 0) {
	    	number += 1;
	    	total -= w_const;
	    }
	    
 	    if (threshold<1.0 && packingPenalty > 0) {
	    	number += 1;
	    	total -= packingPenalty;
		    //total -= penalty*1000000.0;
	    }
 	    
 	   // System.out.println("first_const "+first_const+" w_const/1E8 "+(w_const)+" packingPenalty "+packingPenalty);
	    


//	    if(packingPenalty > 0) {
//	    	number += 1;
//	    	total -= 1000000000;
//	    	
//	    }
	    //System.out.println("check packingPenalty: "+ packingPenalty);
	   

 	    
	    solution.setOverallConstraintViolation(total);    
	    solution.setNumberOfViolatedConstraint(number);	    
	    
	  } // evaluateConstraints
  
  

  private double distance2(int start_c,int end_c) {
		//double x1 = new_node_coords[start_c][0];
		//double y1 = new_node_coords[start_c][1];
		//double x2 = new_node_coords[end_c][0];
		//double y2 = new_node_coords[end_c][1];
		String twoPlatform;
		twoPlatform = String.valueOf(new_node_no[start_c])+'+'+String.valueOf(new_node_no[end_c]);
	    //return Math.sqrt(Math.pow((x1-x2),2) + Math.pow((y1-y2),2));
		//System.out.println(twoPlatform);
		//HashMap<String, Double> Distancescheck = Distances;
	    return Distances.get(twoPlatform);
	  }
  private double distance2_start_end(int start_end,int mid) {
		String twoPlatform;
		if (start_end==0) {
			twoPlatform = String.valueOf(start_end)+'+'+String.valueOf(new_node_no[mid]);
		}
		else {
			twoPlatform = String.valueOf(new_node_no[mid])+'+'+String.valueOf(start_end);
		}
		//System.out.println(twoPlatform);
		//HashMap<String, Double> Distancescheck = Distances;
	    //return Math.sqrt(Math.pow((x1-x2),2) + Math.pow((y1-y2),2));
	    return Distances.get(twoPlatform);
	  }
  
	private double check_Routes(double  split_minv, Solution aSolutionsList_, double relax_ratio,double[] truck_weight_ratio, double[]if_hard_node) throws IOException, ClassNotFoundException {
		
		double[][] client_v_w = new double[NUM_nodes][2]; //client_volume_weight
	    for(int i=0;i<NUM_nodes;i++) {
	    	client_v_w[i][0] = nodes_v_w_0[i][0];
	    	client_v_w[i][1] = nodes_v_w_0[i][1];
	    }
	    
	    
	    
	    double Split_precision = 6.0; // split precision, predefined parameter
	    
	    boolean isfeasible = true; // feasibility of packing, which is predicted by the classifiers
	    
		double predictScore = 1; // score of infeasible packing, a large scale indicates infeasible packing, vice versa
	    
		double penalty = 0; // penalty feedback
	    
	    int x = 0 ;




//		Solution_vrp solution = new Solution_vrp();
//		solution.distanceMap=distanceMap;
	      	
	    int n_loadboxes = 0;
		int new_num_nodes = 0;
		int numberOfCities_ = 0;
		    
		int[] nodes_record = new int[NUM_nodes];
		    
		    
//	    for (int i = 0; i < NUM_nodes; i++) {
//	    	while (client_v_w[i][0]>1.1*VEHICLE_VOLUME[NUM_trucks-1]) {
//	    		client_v_w[i][0] -= truck_weight_ratio[i]*VEHICLE_VOLUME[NUM_trucks-1];
//	    		
//	    	}
//	    	if  (client_v_w[i][0] >  split_minv) {
//	        	new_num_nodes += 2; 
//	    	}
//	    	else {
//	        	new_num_nodes += 1;
//	    	} 	
//	    }//for NUM_nodes
	    
	    for (int i = 0; i < NUM_nodes; i++) {
	    	int nk =0;
	    	while (client_v_w[i][0]>1.1*VEHICLE_VOLUME[NUM_trucks-1]) {
	    		
	    		client_v_w[i][0] -= truck_weight_ratio[i]*VEHICLE_VOLUME[NUM_trucks-1];
	    		
	    		Route route = new Route((int)(1000+Math.random()*8999));
	    		
	        	ArrayList<Box> unloadboxes = new ArrayList<Box>();
				ArrayList<Box> unloadboxes_trans = new ArrayList<Box>();
				ArrayList<Box> unloadboxes_half_trans = new ArrayList<Box>();
	        	
	        	double volume_precent = 0.0;
	        	int ngoods = clients.get(i).getGoodsNum();
	        	//ArrayList<Box> goods = new ArrayList<Box>();
	        	for (int k = nodes_record[i]; k < ngoods; k++) {
	        		volume_precent += clients.get(i).getGoods().get(k).getVolume();
	        		if( volume_precent >= truck_weight_ratio[i]*VEHICLE_VOLUME[NUM_trucks-1]) {
	        			nodes_record[i] = k;
			        	//nk = k;
	        			break;
	        		}
	        		//goods.add(clients.get(node_id).getGoods().get(k));
	        		unloadboxes.add(new Box(clients.get(i).getGoods().get(k)));
	        		unloadboxes_trans.add(new Box(clients_trans.get(i).getGoods().get(k)));
	        		unloadboxes_half_trans.add(new Box(clients_half_trans.get(i).getGoods().get(k)));
	        		//client_v_w[i][0] -= clients.get(i).getGoods().get(k).getVolume();
	        	}
	        	
    	
	        	LinkedList<Node> nodes = new LinkedList<Node>();
	        	
	        	nodes.add(new Node(depot_start));
	        	nodes.add(new Node(clients.get(i)));
	        	nodes.add(new Node(depot_end));
	        	
	        	route.setNodes(nodes);
	        	
	        	ArrayList<Integer> k = null;
	        	
//		        for (int j = 0; j < NUM_trucks; j++) {
//					Carriage vehicle1 = new Carriage(BASIC_TRUCKS.get(j));
//					route.setCarriage(vehicle1);	
//					boolean if_check = true;
//					double unload_v = 0.0;
//					double unload_w = 0.0;
//					for (int nbox = 0; nbox < unloadboxes.size(); nbox++) {
//						if(unloadboxes.get(nbox).getHeight()>vehicle1.getHeight()) {
//							if_check = false;break;
//						}
//						if(unloadboxes.get(nbox).getWidth() >vehicle1.getWidth()) {
//							if_check = false;break;
//						}
//						if(unloadboxes.get(nbox).getLength() >vehicle1.getLength()) {
//							if_check = false;break;
//						}
//						unload_v += unloadboxes.get(nbox).getVolume();
//						unload_w += unloadboxes.get(nbox).getWeight();
//					}
//					if (unload_v > vehicle1.getTruckVolume()||unload_w>vehicle1.getCapacity()) {
//						if_check = false;
//					}
//					if (if_check) {
//						k = pack.packingrun(unloadboxes,packing_type);
//						if (k.size() == unloadboxes.size()) {
//							break;
//						}
//						k = pack.packingrun(unloadboxes_trans,packing_type);
//						if (k.size() == unloadboxes.size()) {
//							break;
//						}
//						k = pack.packingrun(unloadboxes_half_trans,packing_type);
//						if (k.size() == unloadboxes.size()) {
//							break;
//						}
//					}
//		        }
		        if (k!=null) {
			        nk += k.size();
//			        solution.getRoutes().add(route);
//			        solution.evaluation();
		        }
	    	}
	    	n_loadboxes += nk;
	    	if  (client_v_w[i][0] >  split_minv) {
	        	new_num_nodes += 2; 
	    	}
	    	else {
	        	new_num_nodes += 1;
	    	} 	
	    }//for CLIENT_NUM
	    

		    
		numberOfCities_ = new_num_nodes;
		int[] client_city_map = new int[numberOfCities_];
		double[] new_node_v = new double[numberOfCities_];
			
			
		int var = 2;
		new_num_nodes = 0;
		for (int i = 0; i < NUM_nodes; i++) {
			if  (client_v_w[i][0] >   split_minv) {
				String s = aSolutionsList_.getDecisionVariables()[var].toString() ; 
			    char [] s0 = s.toCharArray();
			    double s00 = 0;
			    for (int j=0;j<2;j++) {
			    	s00 += (s0[j]- 48)*Math.pow(2, j);
			    }
			    client_city_map[new_num_nodes] = i;
		    	new_node_v[new_num_nodes] = client_v_w[i][0]*(s00/Split_precision);
		    	new_num_nodes += 1; 
		    	client_city_map[new_num_nodes] = i;
		    	new_node_v[new_num_nodes] = client_v_w[i][0]*(1.0-s00/Split_precision);
		    	new_num_nodes += 1; 
				
				var += 1;
			}
			else {
				client_city_map[new_num_nodes] = i;
				new_node_v[new_num_nodes] = client_v_w[i][0];    	
				new_num_nodes += 1; 	
			}
		}
		        
		    	    
		String if_start = aSolutionsList_.getDecisionVariables()[1].toString();
		char [] if_start0 = if_start.toCharArray();
		
		//int[] route_use_large = new int[numberOfCities_];
		double[] route_hard = new double[numberOfCities_];
		int nroute = 0;
	    for (int i = 0; i < (numberOfCities_ ); i++) {
	    	route_hard[i] = 1.0;

	        int x1 ; 
	        x1 = ((Permutation)aSolutionsList_.getDecisionVariables()[0]).vector_[i] ;  
	        if(i>0) {
	            if (  (if_start0[i-1]- 48) == 1) {
	            	nroute += 1;
	            }
	        }
//		        if ( if_large_box[new_node_no[x]-1]==1) {
//		        	route_use_large[nroute] = 1;
//		        }
//		        if (if_hard_node[client_city_map[x1]]<route_hard[nroute]) {
//		        	route_hard[nroute] = if_hard_node[client_city_map[x1]];
//		        }
	        //route_hard[nroute] =  route_hard[nroute]*if_hard_node[new_node_no[x]-1];
	    }
			

        int n_route = 0; // number of customers each route has
        double[] w_route = new double[500]; // total volume each route has
        int[][] route_node_map = new int[500][500];
        int[] n_node = new int[500];
        for (int i = 0; i < 500; i++) {
        	 n_node[i] = 0;
        	 w_route[i] = 0.0;
        }
        for (int j = 0; j < numberOfCities_; j++) {
      	  
	      	x = ((Permutation)aSolutionsList_.getDecisionVariables()[0]).vector_[j];
	      	if (j>0 && (if_start0[j-1]-48)==1) {
			          n_route += 1;
	      	}
	        w_route[n_route] += new_node_v[x];
	        route_node_map[n_route][n_node[n_route]] = x;
	        n_node[n_route] += 1;
        }
        
        int[] node_if_check = new int[NUM_nodes];
        for (int i = 0; i < NUM_nodes; i++) {
        	node_if_check[i] = 0;
        }
				       
        
	        
        for (int i = 0; i < n_route+1; i++) {
        	
	        ArrayList <Integer> sort_check = new ArrayList<Integer>();
	        int[][] sort_nodes0 = new int[n_node[i]][2];
	        for (int ss= 0;ss<n_node[i];ss++) {
	        	sort_nodes0[ss][0]=100;
	        	sort_nodes0[ss][1]=100;
	        }
	        //sort_nodes0 = route_node_map[i];
//		        int nsort = 0;
	        for (int j = 0; j < n_node[i]; j++) {
	        	
	        	int new_node_id = route_node_map[i][j];
	        	int node_id = client_city_map[new_node_id];
	        	
        		int id_add0 = 0;
        		int ifadd = 1;
        		if (sort_check != null) {
        			id_add0 = sort_check.size();
        			
        			for (int ss =0;ss<sort_check.size();ss++) {
        				if(sort_check.get(ss)==node_id) {
        					ifadd = 0;
        					id_add0 = ss;
        				}	
        			}
        		}
        		if(ifadd==1) {
        			sort_check.add(node_id);
        		}
	        	if(sort_nodes0[id_add0][0]==100) {
	        		sort_nodes0[id_add0][0] = new_node_id;
	        	}
	        	else {
	        		sort_nodes0[id_add0][1] = new_node_id;
	        	}
	        }
	        int nj = 0;
	        for (int ss = 0; ss < n_node[i]; ss++ ){
	        	if (sort_nodes0[ss][0]!=100) { 
	        		route_node_map[i][nj] = sort_nodes0[ss][0];
	        		nj+=1;
	        	}
	        	if (sort_nodes0[ss][1]!=100) { 
	        		route_node_map[i][nj] = sort_nodes0[ss][1];
	        		nj+=1;
	        	}
	        }

        	
	        //ArrayList<Node> node_mid =  new ArrayList<Node>();
        	int current_truck_id = 0;
	        Route route = new Route((int)(1000+Math.random()*8999));
	        for (int j = 0; j < NUM_trucks; j++) {
	        	if (w_route[i] < route_hard[i]*VEHICLE_VOLUME[j]*relax_ratio) {
	        		current_truck_id = j;
	        		break;
	        	}
	        	current_truck_id = j;
	        }
	        
	        
			Carriage vehicle = new Carriage(BASIC_TRUCKS.get(current_truck_id));
			route.setCarriage(vehicle);	
			LinkedList<Node> nodes = new LinkedList<Node>();
			ArrayList<Box> unloadboxes = new ArrayList<Box>();
			ArrayList<Box> unloadboxes_trans = new ArrayList<Box>();
			ArrayList<Box> unloadboxes_half_trans = new ArrayList<Box>();
			
			ArrayList<ArrayList<Box>> unloadboxes_each_route = new ArrayList<ArrayList<Box>>();
			
			nodes.add(new Node(depot_start));
			
			int[] node_id_last = new int[n_node[i]];
			for(int nn=0;nn<n_node[i];nn++) {
				node_id_last[nn] =100;
			}
			

				
		    for (int j = 0; j < n_node[i]; j++) {
		        	
	        	//nodeä¸Šé�¢æ‰€æœ‰çš„boxes
	        	ArrayList<Box> unloadboxes_node = new ArrayList<Box>();
	        	ArrayList<Box> unloadboxes_node_trans = new ArrayList<Box>();
	        	ArrayList<Box> unloadboxes_node_half_trans = new ArrayList<Box>();
	        	int new_node_id = route_node_map[i][j];
	        	int node_id = client_city_map[new_node_id];
	        	
	        	
	        	if(new_node_v[new_node_id]>0.1) {
	        	
		        	int ngoods = clients.get(node_id).getGoodsNum();
		        	//System.out.println(node_id);
		        	//node_mid.add(new Node(clients.get(node_id)));
		        	if (j>0) {
		        		int if_add = 1;
			        	for (int node_check = 0; node_check < j; node_check++) {
			        		if (node_id == node_id_last[node_check]) {
			        			if_add = 0;
//			        			if(node_check!=j-1) {
//			        				System.out.println("wrong");
//			        			}
			        		}
			        	}
		        		if(if_add == 1) {
			        		//if(new_node_v[new_node_id]>0.1) {
		        			nodes.add(new Node(clients.get(node_id)));  
			        			//node_id_last[j] = node_id;
			        		//}
		        		}
		        	}
		        	else {
		        		//if(new_node_v[new_node_id]>0.1) {
		        			nodes.add(new Node(clients.get(node_id))); 
		        			//node_id_last[j] = node_id;
		        		//}
		        		      		
		        	}

		        	node_id_last[j] = node_id;
		        	//int ngoods = clients.get(node_id).getGoodsNum();
		        	double volume_precent = 0.0;
		        	//ArrayList<Box> goods = new ArrayList<Box>();
		        	for (int k = node_if_check[node_id]+nodes_record[node_id]; k < ngoods; k++) {
		        		volume_precent += clients.get(node_id).getGoods().get(k).getVolume();
		        		if(k>node_if_check[node_id]+nodes_record[node_id]) {
			        		if(node_if_check[node_id] == 0 && volume_precent > new_node_v[new_node_id]) {
			        			node_if_check[node_id] = k-nodes_record[node_id];
	//		        			if (node_if_check[node_id]==0) {
	//		        				nodes.remove(new Node(clients.get(node_id)));
	//		        			}
			        			break;
			        		}
		        		}
		        		//goods.add(clients.get(node_id).getGoods().get(k));
		        		unloadboxes_node.add(new Box(clients.get(node_id).getGoods().get(k)));
//		        		Box a = clients.get(node_id).getGoods().get(k);
//		        		Box b = clients_trans.get(node_id).getGoods().get(k);
		        		unloadboxes_node_trans.add(new Box(clients_trans.get(node_id).getGoods().get(k)));
		        		unloadboxes_node_half_trans.add(new Box(clients_half_trans.get(node_id).getGoods().get(k)));
		        	}
		        	
		        	unloadboxes_each_route.add(unloadboxes_node);
		        	
		        	for (int n_unload=0;n_unload<unloadboxes_node.size();n_unload++) {
		        		unloadboxes.add(new Box(unloadboxes_node.get(n_unload)));
		        		unloadboxes_trans.add(new Box(unloadboxes_node_trans.get(n_unload)));
		        		unloadboxes_half_trans.add(new Box(unloadboxes_node_half_trans.get(n_unload)));
		        	}
	        	}
		    }

			nodes.add(new Node(depot_end));				
			route.setNodes(nodes);

			ArrayList<Integer> k = null;
			
			
		
			if (unloadboxes.size() >0) {
				isfeasible = false;

		        for (int j = 0; j < NUM_trucks; j++) {
					Carriage vehicle1 = new Carriage(BASIC_TRUCKS.get(j));
				
				//Carriage vehicle1 = new Carriage(BASIC_TRUCKS.get(current_truck_id));
					route.setCarriage(vehicle1);	
					boolean if_check = true;
					double unload_v = 0.0;
					double unload_w = 0.0;
					for (int nbox = 0; nbox < unloadboxes.size(); nbox++) {
						if(unloadboxes.get(nbox).getHeight()>vehicle1.getHeight()) {
							if_check = false;break;
						}
						if(unloadboxes.get(nbox).getWidth() >vehicle1.getWidth()) {
							if_check = false;break;
						}
						if(unloadboxes.get(nbox).getLength() >vehicle1.getLength()) {
							if_check = false;break;
						}
						unload_v += unloadboxes.get(nbox).getVolume();
						unload_w += unloadboxes.get(nbox).getWeight();
					}
					if (unload_v > vehicle1.getTruckVolume()||unload_w > vehicle1.getCapacity()) {
						if_check = false;
						//System.out.println("check false "+ j+"unload_v "+unload_v+"vehicle1.getTruckVolume() "+vehicle1.getTruckVolume());				
					}
					if (if_check) {
						
//						int max_k = 0;
//						k = pack.packingrun(unloadboxes,packing_type);
//						if (k.size() >max_k) {
//							max_k = k.size();
//						}
//						k = pack.packingrun(unloadboxes_trans,packing_type);
//						if (k.size() >max_k) {
//							max_k = k.size();
//						}
//						k = pack.packingrun(unloadboxes_half_trans,packing_type);
//						if (k.size() >max_k) {
//							max_k = k.size();
//						}
//						
//						Integer label;
//						
//						if (max_k == unloadboxes.size()) {
//							label = 1;
//						}
//						else {
//							label = 0;
//						}
						if (classifier_type==100) {
							
							Packing pack = new Packing();
							
							
							k = pack.packingrun(unloadboxes,vehicle1,packing_type);
							if (k.size() == unloadboxes.size()) {
								//System.out.println("feasible solution");
//								route.setBoxes(pack.get_Boxes());
//								route.setLoadWeight(pack.get_loadWeight());
//								route.setLoadVolumn(pack.get_loadVolumn());
								isfeasible = true;
								break;
							}
//							k = pack.packingrun(unloadboxes_trans,vehicle1,packing_type);
//							if (k.size() == unloadboxes.size()) {
//								//System.out.println("feasible solution");
////								route.setBoxes(pack.get_Boxes());
////								route.setLoadWeight(pack.get_loadWeight());
////								route.setLoadVolumn(pack.get_loadVolumn());
//								isfeasible = true;
//								break;
//							}
//							k = pack.packingrun(unloadboxes_half_trans,vehicle1,packing_type);
//							if (k.size() == unloadboxes.size()) {
//								//System.out.println("feasible solution");
////								route.setBoxes(pack.get_Boxes());
////								route.setLoadWeight(pack.get_loadWeight());
////								route.setLoadVolumn(pack.get_loadVolumn());
//								isfeasible = true;
//								break;
//							}
							//System.out.println("unfeasible solution");
							
						}
						else {
							
						
						if (classifier_type>=10) {
							int nfeatures = 11*15; // maximum 10 warehouses and each warehouse has 11 features
							
							double[] feature = new double[nfeatures];
							
							for (int nroute0=0; nroute0 < unloadboxes_each_route.size() ; nroute0++) {
								ArrayList<Box> unloadboxescurrent = unloadboxes_each_route.get(nroute0);
								
								double sumv=0;
								//double sumweight = 0;
								//double sumw=0;
								double meanv=0;
								double vv=0;
								double stdvv=0;
								double sumh=0;
								double meanh=0;
								double vh=0;
								double stdvh=0;
								double suml=0;
								double meanl=0;
								double vl=0;
								double stdvl=0;
								double sumw=0;
								double meanw=0;
								double vw=0;
								double stdvw=0;
								
								double VV = vehicle1.getTruckVolume();
								double VH = vehicle1.getHeight();
								double VL = vehicle1.getLength();
								double VW = vehicle1.getWidth();
								
								for(int b=0; b<unloadboxescurrent.size(); b++) {
									sumv += unloadboxescurrent.get(b).getVolume()/VV;
									sumh += unloadboxescurrent.get(b).getHeight()/VH;
									suml += unloadboxescurrent.get(b).getLength()/VL;
									sumw += unloadboxescurrent.get(b).getWidth()/VW;
								}
								
								meanv = sumv / unloadboxescurrent.size();
								meanh = sumh / unloadboxescurrent.size();
								meanl = suml / unloadboxescurrent.size();
								meanw = sumw / unloadboxescurrent.size();
								
								for(int b=0; b<unloadboxescurrent.size(); b++) {
									vv = vv + (Math.pow((unloadboxescurrent.get(b).getVolume()/VV-meanv), 2));
									vh = vh + (Math.pow((unloadboxescurrent.get(b).getHeight()/VH-meanh), 2));
									vl = vl + (Math.pow((unloadboxescurrent.get(b).getLength()/VL-meanl), 2));
									vw = vw + (Math.pow((unloadboxescurrent.get(b).getWidth()/VW-meanw), 2));
								}
								
								if (unloadboxescurrent.size()>1) {
									stdvv = Math.sqrt(vv / (unloadboxescurrent.size()-1));
									stdvh = Math.sqrt(vh / (unloadboxescurrent.size()-1));
									stdvl = Math.sqrt(vl / (unloadboxescurrent.size()-1));
									stdvw = Math.sqrt(vw / (unloadboxescurrent.size()-1));
								}
								else {
									stdvv = 0;
									stdvh = 0;
									stdvl = 0;
									stdvw = 0;
								}
								
				
								
								feature[nroute0*10+0] = sumv;
								//feature[nroute0*11+1] = sumw;
								feature[nroute0*10+1] = ((double) unloadboxescurrent.size())/1000.0;
								//feature[nroute0*12+3] = ((double) n_box_type)/100.0;
								//feature[nroute0*13+4] = ((double) n_plat)/10.0;
								feature[nroute0*10+2] = meanv;
								feature[nroute0*10+3] = stdvv;
								feature[nroute0*10+4] = meanh;
								feature[nroute0*10+5] = stdvh;
								feature[nroute0*10+6] = meanl;
								feature[nroute0*10+7] = stdvl;
								feature[nroute0*10+8] = meanw;
								feature[nroute0*10+9] = stdvw;								
								
							
							}
							
							if (classifier_type==10) {
						        double[][][] samplelist = new double[1][10][15];
						        for (int f = 0; f<10;f++) {
						        	for (int ff = 0; ff<15;ff++) {
						        		samplelist[0][f][ff] = feature[ff*10+f];
						        	}			        	
						        }

						        INDArray testsample1 = Nd4j.create(samplelist);	
						        INDArray predictioin = RNNnet.output(testsample1);
						        predictScore = predictioin.getDouble(0,0,14);
						        predictScore = 0.5;
								
							}
							else {
								
								String[] dataHeaders = new String[] {};
								ArrayList<String> list = new ArrayList<>(Arrays.asList(dataHeaders));
								
								int nfeatures0 = 11;
								int nwarehouses = 15;
										
								for (int nf = 0 ; nf < nfeatures0*nwarehouses; nf++) {
									list.add(String.valueOf(nf));
								}
								//list.add("label");
								dataHeaders = list.toArray(new String[list.size()]);
						        
								
								Label output = new Label("label");
								Example<Label> samp = new ArrayExample<>(output,dataHeaders,feature);
								
								//if (unloadboxes.size()>1) {
								Prediction<Label> prediction = classifier.predict(samp);
								//predictLabel = Integer.valueOf(prediction.getOutput().getLabel());
							
								predictScore = prediction.getOutputScores().get("0").getScore();
								
							}
							



							
						}
						else {
							int n_box_type = 0;
							double[] n_box_types = new double[unloadboxes.size()];
							
							for (int nbox = 0; nbox < unloadboxes.size(); nbox++) {

								if (n_box_type>0) {
									int if_add_box = 1;
									for (int ntype = 0; ntype<n_box_type; ntype++ ) {
										if (unloadboxes.get(nbox).getVolume()==n_box_types[ntype] ) {
											if_add_box = 0;
										}
									}
									if(if_add_box == 1) {
										n_box_types[ n_box_type] = unloadboxes.get(nbox).getVolume();
										n_box_type += 1;
									}
								}
								else {
									n_box_types[ n_box_type] = unloadboxes.get(nbox).getVolume();
									n_box_type = 1;
								}								
							}
							
							double sumv=0;
							double meanv=0;
							double vv=0;
							double stdvv=0;
							double sumh=0;
							double meanh=0;
							double vh=0;
							double stdvh=0;
							double suml=0;
							double meanl=0;
							double vl=0;
							double stdvl=0;
							double sumw=0;
							double meanw=0;
							double vw=0;
							double stdvw=0;
							
							double VV = vehicle1.getTruckVolume();
							double VH = vehicle1.getHeight();
							double VL = vehicle1.getLength();
							double VW = vehicle1.getWidth();
							
							for(int b=0; b<unloadboxes.size(); b++) {
								sumv += unloadboxes.get(b).getVolume()/VV;
								sumh += unloadboxes.get(b).getHeight()/VH;
								suml += unloadboxes.get(b).getLength()/VL;
								sumw += unloadboxes.get(b).getWidth()/VW;
							}
							
							meanv = sumv / unloadboxes.size();
							meanh = sumh / unloadboxes.size();
							meanl = suml / unloadboxes.size();
							meanw = sumw / unloadboxes.size();
							
							for(int b=0; b<unloadboxes.size(); b++) {
								vv = vv + (Math.pow((unloadboxes.get(b).getVolume()/VV-meanv), 2));
								vh = vh + (Math.pow((unloadboxes.get(b).getHeight()/VH-meanh), 2));
								vl = vl + (Math.pow((unloadboxes.get(b).getLength()/VL-meanl), 2));
								vw = vw + (Math.pow((unloadboxes.get(b).getWidth()/VW-meanw), 2));
							}
							
							if (unloadboxes.size()>1) {
								stdvv = Math.sqrt(vv / (unloadboxes.size()-1));
								stdvh = Math.sqrt(vh / (unloadboxes.size()-1));
								stdvl = Math.sqrt(vl / (unloadboxes.size()-1));
								stdvw = Math.sqrt(vw / (unloadboxes.size()-1));
							}
							else {
								stdvv = 0;
								stdvh = 0;
								stdvl = 0;
								stdvw = 0;
							}
							
							
							
							double[] feature = new double[13];
							feature[0] = unload_v/vehicle1.getTruckVolume();
							feature[1] = unload_w/vehicle1.getCapacity();
							feature[2] = ((double) unloadboxes.size())/1000.0;
							feature[3] = ((double) n_box_type)/100.0;
							feature[4] = ((double) n_node[i])/10.0;
							feature[5] = meanv;
							feature[6] = stdvv;
							feature[7] = meanh;
							feature[8] = stdvh;
							feature[9] = meanl;
							feature[10] = stdvl;
							feature[11] = meanw;
							feature[12] = stdvw;
							
							
//						    FileOutputStream ffff   = new FileOutputStream("data.dat",true)     ;
//							
//							for (int b=0;b<13;b++) {
//								ffff.write((String.valueOf(feature[b])+", ").getBytes());
//								}
//							ffff.write(label.toString().getBytes());
//							ffff.write("\n".getBytes());

						
							String[] Headers = new String[]{"P_v", "P_w", "N_b", "N_tb", "N_c", "A_v", "V_v", "A_h", "V_h", "A_l", "V_l", "A_w"
									, "V_w"};
							
							Label output = new Label("label");
							Example<Label> samp = new ArrayExample<>(output,Headers,feature);
							
							//if (unloadboxes.size()>1) {
							Prediction<Label> prediction = classifier.predict(samp);
							//predictLabel = Integer.valueOf(prediction.getOutput().getLabel());
						
							predictScore = prediction.getOutputScores().get("0").getScore();
							//System.out.println("predict label = "+prediction.getOutput().getLabel());
							//}
							
							
							
						}
						
						
						if (predictScore < threshold ) {
							isfeasible = true;
							break;
						}
//						else {
//							//System.out.println("feature: "+feature[0]);
//						}
				
					} // classifier ==100 else

		        }
		        }

			}
			if (isfeasible == false) {
				//isfeasiblefinal = false;

				penalty += 100000;

			}
        }//for n_route+1
        //System.out.println(penalty);    

//        if (penalty == 100000) {
//        	penalty = 0;
//        			}
//        if (penalty == 0)
//    		System.out.println("feasible solution found !");
	    return penalty;

	}

}





package solver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import solver.util.Box;
import solver.util.Carriage;
import solver.util.L;
import solver.util.Node;

import feasibilityCheck.Check;
import solver.inoutput.Input;
import solver.packing.Packing;
import solver.routing.jmetal.core.Algorithm;
import solver.routing.jmetal.core.Operator;
import solver.routing.jmetal.core.Problem;
import solver.routing.jmetal.core.Solution;
import solver.routing.jmetal.core.SolutionSet;
import solver.routing.jmetal.encodings.variable.Permutation;
import solver.routing.jmetal.metaheuristics.moead.cMOEAD;
import solver.routing.jmetal.operators.crossover.CrossoverFactory;
import solver.routing.jmetal.operators.mutation.MutationFactory;
import solver.routing.jmetal.problems.CVRP_mix_integer_new;
import solver.routing.jmetal.util.JMException;
import solver.routing.jmetal.util.fast_nondom;

import solver.inoutput.Output;



/**
 * 
 * Program start from here
 * @author Fei Liu, Qingling ZHU
 * @email  fliu36-c@my.cityu.edu.hk
 * 
 */

public class RPSolver {
	
	/**
	 * ==============================
	 * Parameter setting
	 * ==============================
	 */

	
	static final boolean debug = false; // Switch debug mode
	static final boolean isUpdateExtreme = false; // Switch if update extreme point, used for EMO2021 competition
	static final boolean is_calculate_hv = true; // Switch if calculate hypervolume (HV)
	

	static double TIME_LIMITS = 200; // Running time limit (s), default 60s
	static final int PACKING_LIMITS = 2000; // Maximum number of packing feasibility checking, default = 2000
	static final int POPSIZE = 50; // Size of population for MOEA/D, default = 50
	static final int T_ = 5; // Neighborhood size for MOEA/D, default = 5
	static final int maxE = 2000; // Maximum number of evaluations, two offsprings each crossover,the number of population = maxE/(POPSIZE*2), default = 2000
	static int n_repeat = 10000; // Number of repeat runs for each instance, default = 10000
	static int classifier_type = 4; // Type of classifier, default = 4 (CART)
	static int packing_type = 2; // Type of used packing heuristics, default = 2 (use all three heuristics)
	static int n_pop_restart = 5; // Algorithm will stop after n_pop_restart non-improved populations, default = 5
	static double timeRatio_reduce = 0.5; // Reduce ratio 
	static double theta = 0.5; // theta
	static int encoding_strategy = 1; // 1 = full encoding (s,x,p); 2 = encoding without split encoding (simply use half) (x,p); 
		// 3 = encoding with only maximum capacity vehicle (x,p); 4 = encoding without dividing encoding (x)
		
	

	static int n_3DPacking; // Number of calls of time-consuming 3D bin packing heuristics,
	static int TRUCKTYPE_NUM; // Number of vehicle types
	static int CLIENT_NUM; // Number of clients (customers)
	static int BOX_NUM; // Number of boxes (cargoes)
	static ArrayList<Carriage> BASIC_TRUCKS; // List of trucks (vehicles)
	static HashMap<String, Double> distanceMap; // Distance map
	static Node depot_start ; // Depot node for start
	static Node depot_end ; // Depot node for end
	static double[] VEHICLE_CAPACITY ; // Vehicle capacity list
	static double[] VEHICLE_VOLUME ; // Vehicle volume list
	static double[][] client_volume_weight; // Total volume and Total weight of the boxes (cargoes) in each client (customer)
	static ArrayList<Node> clients ; // List of clients
	static ArrayList<Node> clients_trans ; // List of transposed clients, the same clients but with transposed direction (90 degrees) along X-Y plane.
	static ArrayList<Node> clients_half_trans ; // List of half transposed clients, half transposed (if (length > 2318 || if_half%2==0) )
	static ArrayList<Node> clients_v ; //sort clients by volume
	static ArrayList<Node> clients_trans_v ; //sort clients by volume
	static ArrayList<Node> clients_half_trans_v; //sort clients by volume
	static HashMap<Integer, String> PlatformIDCodeMap; // platform Id (NO.) to code (Name) mapping, 
	static long begintime; // program begin time
	
	static String resultAll="result_all.dat"; // File output all results
	static String resultFinal="result_final.dat"; // File output final results
	
	static Packing pack = new Packing();
	
	static Output output = new Output();
	
	
	// ===================================================== main procedure
	@SuppressWarnings("unchecked")	
	public static void main(String[] args) throws IOException, ClassNotFoundException, JMException {
				
		/**
		 * =================================================
		 * 1) Read input args
		 * =================================================
		 */
			
		String input_directory = args[0]+"/data/inputs"; 
		String output_directory = args[0]+"/data/outputs";	
		String model_directory = args[0]+"/model/"; 
		classifier_type = Integer.valueOf(args[1]).intValue(); 	
		// type of classifier: 0=linear, 1=MNB, 2=logistic regression, 3=svm_rbf, 4=CART, 5=random forest 10,6=random forest 20,7=random forest 50
		packing_type = Integer.valueOf(args[2]).intValue();
		// type of packing: 0=simpleHeuristic, 1=simpleHeuristic+newHeuristic1, 2=simpleHeuristic+newHeuristic1+newHeuristic2		
		n_repeat = Integer.valueOf(args[3]).intValue();
		n_repeat = 10000;
		// number of repeats		
		TIME_LIMITS = Integer.valueOf(args[4]).intValue();
		// time limit
		theta = Double.valueOf(args[5]);
		// theta
		encoding_strategy = Integer.valueOf(args[6]).intValue();
		
		
		

		/**
		 * ==========================================================
		 * 2) Pre-processing
		 * ==========================================================
		 */		
		
	    FileOutputStream fos   = new FileOutputStream(resultAll)     ;
	    OutputStreamWriter osw = new OutputStreamWriter(fos)    ;
	    BufferedWriter bw      = new BufferedWriter(osw)        ;
	    FileOutputStream fosFinal   = new FileOutputStream(resultFinal)     ;
	    OutputStreamWriter oswFinal = new OutputStreamWriter(fosFinal)    ;
	    BufferedWriter bwFinal      = new BufferedWriter(oswFinal)        ;
	    
		bw.write("no.    file_name  number_cust  number_box  number_typet  hv  dis  loadr  aver_dis  "
				+ "aver_loadr  number_solutions  time_cost  number_packing  time_packing");
		bw.newLine();
	    
		double totalLengthAll=0;
		double loadingRatioAll=0;
		double totalLengthAllN=0;
		double loadingRatioAllN=0;
		int nSolutionAll=0;
		double packingusedTimeAll = 0;
		

		
		
		double max_single_time = 0.0;

	    
		// Set ideal and nadir points, read them from file extremes
		Map<String, Double[]> idealNadirMap = new HashMap<String, Double[]>();// problemName->ideal&nadir
		if (is_calculate_hv) {
			File f = new File("./data/extremes");
			if (f.exists()) {
				idealNadirMap = readExtreme();
			} else {
				// If file extremes not exist, set the ideal and nadir points to min and max malue, respectively
				f = new File(input_directory);
				String[] filenames = f.list();
				for (int fileidx = 0; fileidx < filenames.length; fileidx++) {
					Double[] initialPoints = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_VALUE };
					idealNadirMap.put(filenames[fileidx], initialPoints);
				}
			}
		}

		File f = new File(input_directory);
		String[] filenames = f.list();

		double total_hv = 0.0;
		double total_time = 0.0;
  
		SolutionSet_vrp final_ss = new SolutionSet_vrp();
		System.out.println(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
		
		
		
		/**
		 * ==========================================================
		 * 3) Start of main program for each instance
		 * 			3.1		Read data of instance
		 * 			3.2		Send vehicles for these client, which has too many cargoes, before start routing-packing procedure
		 * 			3.3		Routing-first-packing-second procedure
		 * ==========================================================
		 */
		
		
		for (int fileidx = 0; fileidx < filenames.length; fileidx++) {
			
			begintime = System.nanoTime();

			/**
			 * ==========================================================
			 * 3.1 read data of instance
			 * ==========================================================
			 */			

			Double[] idealNadir;
			String filename = filenames[fileidx];
					
			double packingusedTime = 0;
			
			if (is_calculate_hv)
				idealNadir = (Double[]) idealNadirMap.get(filename);
			
			if (idealNadir == null) {
				Double[] bound = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_VALUE };
				idealNadir = bound;
			}
			

			Input inputs = new Input(input_directory+"/"+filename);
			
			//inputs.ReadSDCSS();
			inputs.ReadJson();
			//inputs.ReadSH();
			
			CLIENT_NUM = inputs.get_CLIENT_NUM();//
			BOX_NUM = inputs.get_BOX_NUM();//
			TRUCKTYPE_NUM = inputs.get_RUCKTYPE_NUM(); 
			PlatformIDCodeMap = inputs.get_PlatformIDCodeMap();// platform ID map to code
//			HashMap<String, Integer> PlatformCodeIDMap = new HashMap<String, Integer>();// platform Code map to ID
//			PlatformCodeIDMap = inputs.get_PlatformCodeIDMap();
			
			BASIC_TRUCKS = inputs.get_BASIC_TRUCKS();
			distanceMap = inputs.get_distanceMap();
			
			

			
			clients = inputs.get_clients();
			clients_trans = inputs.get_clients_trans();
			clients_half_trans = inputs.get_clients_half_trans();
			clients_v = inputs.get_clients_v();
			clients_trans_v = inputs.get_clients_trans_v();
			clients_half_trans_v = inputs.get_clients_half_trans_v();
			
			client_volume_weight = inputs.get_client_volume_weight();
			
			depot_end = inputs.get_depot_end();
			depot_start = inputs.get_depot_start();
			

			
			/**
			 * ==============================
			 * 3.2 Send vehicles for these client, which has too many cargoes, before start routing 
			 * ==============================
			 */
			
			

			n_3DPacking = 0; // count number of 3D bin packings

			VEHICLE_CAPACITY = new double[TRUCKTYPE_NUM]; // store vehicle capacity
			VEHICLE_VOLUME = new double[TRUCKTYPE_NUM]; // store vehicle volume
			
		    for (int i=0; i<TRUCKTYPE_NUM; i++ ) {
		    	VEHICLE_CAPACITY[i] = (double) BASIC_TRUCKS.get(i).getCapacity();
		    	VEHICLE_VOLUME[i] = (double) BASIC_TRUCKS.get(i).getTruckVolume();
		    }

		    
		    // check whether the client contains boxes, whose height is larger than the height of (the smallest) vehicle  
		    // if homogeneous fleet is used, there is no need to check, otherwise the instance is infeasible
		    int[] if_large_box = new int[clients.size()];  
		    for (int i=0; i<clients.size(); i++) {
				for (int j=0; j<clients.get(i).getGoodsNum(); j++) {
					if (clients.get(i).getGoods().get(j).getHeight() > BASIC_TRUCKS.get(0).getHeight()){
						if_large_box[i] = 1;
						break;
					}
				}
			}
		    
		    
			/**
			 * 1) truck_weight_ratio stores the percentage of packing 
			 * for these clients whose total volume of boxes is larger than the capacity of vehicle, 
			 * the subset of boxes will be check with a decreasing percentage of ( double nr=1.0 ;nr>0.549; nr-=0.02 )
			 * once a feasible packing is generated, the feasible percentage nr will be stored in the percentage list (truck_weight_ratio)
			 * 
			 * 2) truck_weight_ratio will be used in the solution_vrp function to generate final solution
			 * 
			 * 3) this strategy is not strict and the boxes in the large client has not been excluded from it, it may result in the infeasibility in the routing
			 */
//		    double[] truck_weight_ratio = new double[CLIENT_NUM];
//		    for (int i = 0; i < CLIENT_NUM; i++) {
//		    	truck_weight_ratio[i] = 1.0;
//		    	if (client_v_w[i][0]>0.8*VEHICLE_VOLUME[TRUCKTYPE_NUM-1]) {//Ã©â‚¬â„¢Ã¥â‚¬â€¹Ã§Â¯â‚¬Ã©Â»Å¾Ã§Â®Â±Ã¥Â­ï¿½Ã§Å¡â€žÃ©Â«â€�Ã§Â©ï¿½Ã¦Â¯â€�Ã¨Â¼Æ’Ã¥Â¤Â§Ã£â‚¬â€šÃ¥Â¾Ë†Ã¥Â¤Â§Ã¦Â¦â€šÃ§Å½â€¡Ã¤Â¸â‚¬Ã¨Â¼â€ºÃ¨Â»Å Ã¨Â£ï¿½Ã¤Â¸ï¿½Ã¥Â®Å’Ã£â‚¬â€š
//		    		for (double nr=1.0 ;nr>0.549; nr-=0.02) {
//		    			truck_weight_ratio[i] = nr;
//		    			//Ã§â€šÂºnode i Ã¥Â»ÂºÃ§Â«â€¹Ã¤Â¸â‚¬Ã¦Â¢ï¿½Ã¨Â·Â¯Ã¥Â¾â€˜Ã£â‚¬â€š
//			    		Route route = new Route((int)(1000+Math.random()*8999));
//			    		
//			        	ArrayList<Box> unloadboxes = new ArrayList<Box>();
//						ArrayList<Box> unloadboxes_trans = new ArrayList<Box>();
//						ArrayList<Box> unloadboxes_half_trans = new ArrayList<Box>();
//			        	
//			        	double volume_precent = 0.0;
//			        	int ngoods = clients.get(i).getGoodsNum();
//			        	//ArrayList<Box> goods = new ArrayList<Box>();
//			        	for (int k = 0; k < ngoods; k++) {
//			        		volume_precent += clients.get(i).getGoods().get(k).getVolume();
//			        		if( volume_precent >= truck_weight_ratio[i]*VEHICLE_VOLUME[TRUCKTYPE_NUM-1]) {
//			        			break;
//			        		}
//			        		//goods.add(clients.get(node_id).getGoods().get(k));
//			        		unloadboxes.add(new Box(clients.get(i).getGoods().get(k)));
//			        		unloadboxes_trans.add(new Box(clients_trans.get(i).getGoods().get(k)));
//			        		unloadboxes_half_trans.add(new Box(clients_half_trans.get(i).getGoods().get(k)));
//			        		//client_v_w[i][0] -= clients.get(i).getGoods().get(k).getVolume();
//			        	}
//			        	
//			        	ArrayList<Integer> k = null;
//
//						Carriage vehicle1 = new Carriage(BASIC_TRUCKS.get(TRUCKTYPE_NUM-1));//Ã§â€�Â¨Ã¦Å“â‚¬Ã¥Â¾Å’Ã¤Â¸â‚¬Ã§Â¨Â®Ã¨Â»Å Ã¥Å¾â€¹Ã£â‚¬â€š
//						route.setCarriage(vehicle1);	
//	
////						boolean pack_checking = true;
////						double unload_v = 0.0;//Ã¦Å“â€°Ã¥Â¤Å¡Ã¥Â°â€˜Ã©Â«â€�Ã§Â©ï¿½Ã¦Â²â€™Ã¦Å“â€°Ã¨Â£ï¿½Ã¨Â¼â€°Ã§Å¡â€žÃ£â‚¬â€š
////						double unload_w = 0.0;
////						for (int nbox = 0; nbox < unloadboxes.size(); nbox++) {
////							if(unloadboxes.get(nbox).getHeight()>vehicle1.getHeight()) {//Ã©â‚¬â„¢Ã¥â‚¬â€¹Ã§Â®Â±Ã¥Â­ï¿½Ã¤Â¸ï¿½Ã¨Æ’Â½Ã§â€�Â¨Ã©â‚¬â„¢Ã¥â‚¬â€¹Ã¨Â»Å Ã¥Â­ï¿½Ã¨Â£ï¿½Ã£â‚¬â€š
////								pack_checking = false;break;
////							}
////							if(unloadboxes.get(nbox).getWidth() >vehicle1.getWidth()) {
////								pack_checking = false;break;
////							}
////							if(unloadboxes.get(nbox).getLength() >vehicle1.getLength()) {
////								pack_checking = false;break;
////							}
////							unload_v += unloadboxes.get(nbox).getVolume();
////							unload_w += unloadboxes.get(nbox).getWeight();
////						}
////						if (pack_checking) {
//							n_3DPacking += 1;
//							k = pack.packingrun(unloadboxes,vehicle1,packing_type);
//							if (k.size() == unloadboxes.size()) {
//								route.setBoxes(pack.get_Boxes());
//								route.setLoadWeight(pack.get_loadWeight());
//								route.setLoadVolumn(pack.get_loadVolumn());
//								break;
//							}
//							k = pack.packingrun(unloadboxes_trans,vehicle1,packing_type);
//							if (k.size() == unloadboxes.size()) {
//								route.setBoxes(pack.get_Boxes());
//								route.setLoadWeight(pack.get_loadWeight());
//								route.setLoadVolumn(pack.get_loadVolumn());
//								break;
//							}
//							k = pack.packingrun(unloadboxes_half_trans,vehicle1,packing_type);
//							if (k.size() == unloadboxes.size()) {
//								route.setBoxes(pack.get_Boxes());
//								route.setLoadWeight(pack.get_loadWeight());
//								route.setLoadVolumn(pack.get_loadVolumn());
//								break;
//							}
////						}
//		    		}
//		    	}//if 
//		    }//for each node
		    
			/**
			 * new strategy to send vehicles to these clients with large total volume 
			 */
		    
		    ArrayList<Route> pre_route_list = new ArrayList<Route>();
		    
		    double pre_route_dis = 0.0;

		    for (int i = 0; i < CLIENT_NUM; i++) {
		    	
		    	ArrayList<Box> unloadboxes = clients.get(i).getGoods();
		    	
		    	double unloadboxesvolume = clients.get(i).getGoodsVolumn();
		    	
		    	
		    	while (unloadboxesvolume>0.8*VEHICLE_VOLUME[0]) {


		    		int nloadboxes = 0;
		    		
		    		Route route = new Route((int)(1000+Math.random()*8999));
        	
		        	
		        	ArrayList<Integer> loadedboxesid = null;

					Carriage vehicle1 = new Carriage(BASIC_TRUCKS.get(TRUCKTYPE_NUM-1));//Ã§â€�Â¨Ã¦Å“â‚¬Ã¥Â¾Å’Ã¤Â¸â‚¬Ã§Â¨Â®Ã¨Â»Å Ã¥Å¾â€¹Ã£â‚¬â€š
					route.setCarriage(vehicle1);
					

					n_3DPacking += 1;
					loadedboxesid = pack.packingrun(unloadboxes,vehicle1,packing_type);
					if (loadedboxesid.size() > nloadboxes) {
						nloadboxes = loadedboxesid.size() ;
						route.setBoxes(pack.get_Boxes());
						route.setLoadWeight(pack.get_loadWeight());
						route.setLoadVolumn(pack.get_loadVolumn());
						
					}
					
					LinkedList<Node> nodes = new LinkedList<Node>();
					
					nodes.add(new Node(depot_start));
					Node node_copy = new Node(clients.get(i) ) ;
					node_copy.setGoods(pack.get_Boxes());
					nodes.add(  node_copy );					
					nodes.add(new Node(depot_end));				
					route.setNodes(nodes);
//					loadedboxesid = pack.packingrun(unloadboxes_trans,vehicle1,packing_type);
//					if (loadedboxesid.size() > nloadboxes) {
//						nloadboxes = loadedboxesid.size() ;
//						route.setLoadWeight(pack.get_loadWeight());
//						route.setLoadVolumn(pack.get_loadVolumn());
//						
//					}
//					loadedboxesid = pack.packingrun(unloadboxes_half_trans,vehicle1,packing_type);
//					if (loadedboxesid.size() > nloadboxes) {
//						nloadboxes = loadedboxesid.size() ;
//						route.setLoadWeight(pack.get_loadWeight());
//						route.setLoadVolumn(pack.get_loadVolumn());						
//					}
					pre_route_list.add(route);
					
					pre_route_dis += distanceMap.get("0+"+String.valueOf(i+1));
					pre_route_dis += distanceMap.get(String.valueOf(i+1)+"+"+String.valueOf(CLIENT_NUM+1));
					
					//System.out.println(unloadboxes.size());
					for (int nbox=0;nbox<nloadboxes;nbox++) {
						unloadboxesvolume -= unloadboxes.get(0).getVolume();
						unloadboxes.remove(0);	
						BOX_NUM -=1;
		    		}
					//System.out.println(unloadboxes.size());
					
		    	}
		    	
		    	double unloadboxes_volume = 0.0;
		    	double unloadboxes_weight = 0.0;
		    	for (int nbox=0;nbox<unloadboxes.size();nbox++) {	    		
		    		unloadboxes_volume += unloadboxes.get(nbox).getVolume();
		    		unloadboxes_weight += unloadboxes.get(nbox).getWeight();	
		    	}
		    	
		    	client_volume_weight[i][0] = unloadboxes_volume;
		    	client_volume_weight[i][1] = unloadboxes_weight;
		    	clients.get(i).setGoodsVolumn(unloadboxes_volume);
		    	clients.get(i).setGoodsWeight(unloadboxes_weight);
		    	    	
		    	clients.get(i).setGoods(unloadboxes);
		    	clients.get(i).setGoodsNum(unloadboxes.size());
		    }//for each node
		    
		    
		    double[][] client_v_w = new double[CLIENT_NUM][2]; //client_volume_weight;
		    for(int i=0;i<CLIENT_NUM;i++) {
		    	client_v_w[i][0] = client_volume_weight[i][0];
		    	client_v_w[i][1] = client_volume_weight[i][1];
		    }
		    		
		    HashMap PF_out = new HashMap();
	    	ArrayList<double[]> best_results = new ArrayList <double[]>();

	    	
			/**
			 * ==============================
			 * 3.3 Start of the routing first packing second procedure
			 * ==============================
			 */
	    	
			
			Problem problem;
			Algorithm algorithm;
			Operator crossover ;
			Operator mutation;
			Operator mutation_mod;

			System.out.print(""+fileidx+"\t"+filenames[fileidx] + "\t" + CLIENT_NUM + "\t" + BOX_NUM + "\t" + TRUCKTYPE_NUM + "\t");
			ArrayList<Node> curr_clients = new ArrayList<Node>();
			bw.write(""+fileidx+"\t"+filenames[fileidx] + "\t" + CLIENT_NUM + "\t" + BOX_NUM + "\t" + TRUCKTYPE_NUM + "\t");
			
			// main settings *************************************************
			boolean is_found_feasible = false;
		    int n_split = 1;
		    double[] relax_ratio_all = {1.0};
		    //double[] relax_ratio_all = {1.0,0.9,0.8,0.7,0.6,0.5,0.4};
		    //double[] relax_ratio_all = {0.95,0.925,0.9,0.875,0.85,0.825,0.8,0.775,0.75,0.725,0.7,0.675,0.65,0.625,0.6,0.575,0.55,0.525,0.5,0.475,0.45,0.425, 0.4, 0.375, 0.35};
		    double[] thres_list = {0.5};
		    double relax_ratio = 0.9;
		    timeRatio_reduce = 0.5;
		    //double threshold = 0.5;
		    // main settings finished *************************************************
			int record_nruns = 0;
			
			for(int runs = 0; runs < n_repeat; runs++) {							
				
				

		    	for (int thres_r = 0; thres_r < thres_list.length; thres_r++) {//
		    		    	
		    	double threshold = thres_list[thres_r];
		    	for (int relax_r = 0; relax_r < relax_ratio_all.length; relax_r++) {
		    	
		    		if(!is_found_feasible && (TIME_LIMITS-(System.nanoTime() - begintime) / (1e9))<timeRatio_reduce*TIME_LIMITS && relax_ratio>0.4) {
		    			relax_ratio = relax_ratio-0.1;
		    			timeRatio_reduce = timeRatio_reduce*0.5;
		    		}
	    
				    for (int split_w = 1; split_w < n_split+1; split_w++) {//1,2,3,4
				    	record_nruns += 1;
				    	if (debug) {
				    		
				    		System.out.println("No. run "+ record_nruns+ " with relax_ratio = "+relax_ratio);
				    	}
				    	
					    ArrayList<double[]> save_objs = new ArrayList<double[]>();
					    ArrayList<Double> save_consts = new ArrayList<Double>();
					    ArrayList<Solution> save_vars = new ArrayList<Solution>() ; 

					   // ArrayList<double[]> save_objs0 = new ArrayList<double[]>();
				    		
					    
					    int n_solution = 0;
					    //int n_all = 0;
					    
					    double split_minv = 0.0 ;//Ã¨Â¶â€¦Ã©ï¿½Å½Ã¥Â¤Å¡Ã¥Â°â€˜Ã¥Â°Â±Ã©â€“â€¹Ã¥Â§â€¹Ã¥Ë†â€ Ã¥â€°Â²Ã£â‚¬â€š
					    if (split_w==1) {
					    	split_minv = theta*VEHICLE_VOLUME[0];
					    }

				    	//double split_minv =   ((double) split_w/ (double) n_split)*(max_v-min_v)+min_v-100.0; 
					    
						/**
						 * ==============================
						 * Use the jmetal framework to perform 
						 * 		multi-objective evolutionary optimization on routing
						 * 
						 * CVRP_mix_integer is the problem model, 
						 * 		which returns the values of objectives and constraints
						 * 
						 * 3D bin packing is replace by classifiers inside the CVRP_mix_integer
						 * ==============================
						 */
					    
				    	
				    	problem = new CVRP_mix_integer_new(encoding_strategy,"PermutationBinary",split_minv,VEHICLE_CAPACITY,VEHICLE_VOLUME,CLIENT_NUM,distanceMap,
				    			client_v_w,relax_ratio,clients,clients_trans,clients_half_trans,if_large_box,
				    			distanceMap,BASIC_TRUCKS,depot_start,depot_end,threshold,classifier_type,packing_type,model_directory);
				
				    	algorithm = new cMOEAD(problem,n_pop_restart);
				
				        // Algorithm parameters
				        algorithm.setInputParameter("populationSize",POPSIZE);
				        algorithm.setInputParameter("maxEvaluations",maxE);
				        algorithm.setInputParameter("T", T_) ;
				        algorithm.setInputParameter("delta", 0.9) ;
				        algorithm.setInputParameter("nr", 2) ;
				
				        /* Crossver operator */
				        HashMap parameters = new HashMap() ;
				        parameters.put("PMXCrossoverProbability", 0.8) ;
				        parameters.put("binaryCrossoverProbability", 0.8) ;
				        //crossover = CrossoverFactory.getCrossoverOperator("TwoPointsCrossover", parameters);
				        crossover = CrossoverFactory.getCrossoverOperator("PMXsinglepointCrossover", parameters);                
				        
				        /* Mutation operator */
				        parameters = new HashMap() ;
				        parameters.put("SwapMutationProbability", 0.15) ;
				        parameters.put("binaryMutationProbability", 0.15) ;
				        mutation = MutationFactory.getMutationOperator("SwapBitFlipMutation", parameters);  
				        
			        
				        /* Mutation operator */
				        parameters = new HashMap() ;
				        parameters.put("SwapMutationProbability", 0.2) ;
				        //parameters.put("binaryMutationProbability", 0.12) ;
				        mutation_mod = MutationFactory.getMutationOperator("SwapMutation_mod", parameters);  
				        
				        
				        algorithm.addOperator("crossover",crossover);
				        algorithm.addOperator("mutation",mutation);
				        
//				        algorithm.addOperator("crossover_end",crossover_end);
//				        algorithm.addOperator("mutation_end",mutation_end);
				        algorithm.addOperator("mutation_mod",mutation_mod);
				        
				        // Execute the Algorithm
//				        long initTime = System.currentTimeMillis();
				        
						/**
						 * ==============================
						 * routing executed here
						 * 		the approximated non-dominated samples 
						 * ==============================
						 */
				        SolutionSet population = algorithm.execute();
//				        population_last = population;
//				        long estimatedTime = System.currentTimeMillis() - initTime;

				        HashMap results = population.getresults();
				        
				        final List<Solution> solutions;
				        solutions = (List<Solution>) results.get("result");
				        
				        for (Solution aSolutionsList_ : solutions) {
				        	save_objs.add(aSolutionsList_.objective_);
				        	save_vars.add(aSolutionsList_);
				        	save_consts.add(aSolutionsList_.overallConstraintViolation_);
				        	//System.out.print(aSolutionsList_.getDecisionVariables()[1]+" ");
				        	//System.out.print(aSolutionsList_.numberOfViolatedConstraints_);
				        	//System.out.println(aSolutionsList_.overallConstraintViolation_);
				            //n_solution += 1;
				        	//all_split_minv [n_all] = split_minv;
				            //n_all +=1;
				          }
				
				        ArrayList<double[]> save_objs0 = new ArrayList<double[]>();
					    ArrayList<Solution> save_vars0 = new ArrayList<Solution>() ;
					    
				        for (int n = 0; n<save_objs.size(); n++) {
				        	boolean if_add = true;
				        	if (save_consts.get(n) < 0) {//Ã¤Â¸ï¿½Ã¨Â¦ï¿½Ã¤Â¸ï¿½Ã¥ï¿½Â¯Ã¨Â¡Å’Ã¨Â§Â£
				        		if_add = false;continue;
				        	}		   
				        	
				        	for (int nn=0; nn<best_results.size(); nn++ ) {
				        		//Ã¥Â¦â€šÃ¦Å¾Å“Ã¨Â¢Â«Ã§â€¢Â¶Ã¥â€°ï¿½Ã¦Å“â‚¬Ã¥Â¥Â½Ã§Å¡â€žÃ¨Â§Â£Ã¦â€�Â¯Ã©â€¦ï¿½
				        		if (save_objs.get(n)[0]>best_results.get(nn)[0]&&save_objs.get(n)[1]*relax_ratio*0.98>best_results.get(nn)[1]) {
				        			if_add = false;break;
				        		}
				        	}
				        	if(if_add) {				        		
					        	save_objs0.add(save_objs.get(n));
					        	save_vars0.add(save_vars.get(n));
				        	}
				        }
				        
				        if (save_objs0.size()>0) {
				        	
				        	//System.out.println(">>>>>>> is feasible >>>>>>>>");
				        	//System.out.println(save_vars0);
				        	
					        ArrayList<double[]> save_objs1 = new ArrayList<double[]>();
						    ArrayList<Solution> save_vars1 = new ArrayList<Solution>() ;

				        				    
				        	fast_nondom FND = new fast_nondom();
				        	
					        PF_out = FND.fast_nondominated_sort(save_objs0);
					        n_solution = (int)PF_out.get("n");
//					        double[][] PF = (double[][]) PF_out.get("PF");
//					        int[] no_PF =(int[])PF_out.get("no_PF");

						    int[] save_feasible_no = new int[n_solution];
//						    System.out.println(""+runs+":"+n_solution);
						    for (int n = 0; n<n_solution; n++) {
						    		save_vars1.add(save_vars0.get(((int[])PF_out.get("no_PF"))[n]));
						    		save_objs1.add(save_objs0.get(((int[])PF_out.get("no_PF"))[n]));
						    }
						    
						    
							/**
							 * ==============================
							 * 3d bin packing executed here
							 * 		to generate loading solutions for the approximated non-dominated samples 
							 * ==============================
							 */
						    long packingtimestart = System.nanoTime();
						    SolutionSet_vrp solutionSet = get_Solutions(encoding_strategy,split_minv,save_vars1,relax_ratio,save_feasible_no);
							packingusedTime += (System.nanoTime() - packingtimestart) / (1e9);
						    
						    for (int n_solu=0;n_solu<solutionSet.size();n_solu++) {
//							    solutionSet_last.add(solutionSet.get(n_solu));
						    	allocateBoxes2Node(solutionSet.get(n_solu));
						    	final_ss.add(solutionSet.get(n_solu));
						    	is_found_feasible = true;
						    }
						    for (int n = 0; n<n_solution; n++) {
						    	//System.out.println("check feasibility :  "+"no. "+n+" "+ save_feasible_no[n] +"relax = "+relax_ratio);
						    	if(save_feasible_no[n]==1) {
						    		double[] save_mid = save_objs0.get(n);
						    		save_mid[1] = save_mid[1]*relax_ratio;
						    		best_results.add(save_mid);
//						    		best_results.add(save_objs0.get(n));				    		
						    	}
						    }
						    
				        }//if save_objs>0
				        if(n_3DPacking>=PACKING_LIMITS || (n_3DPacking >= PACKING_LIMITS/2 && !is_found_feasible)||(System.nanoTime() - begintime) / (1e9) > TIME_LIMITS) {
				        	break;
				        }
				    }//for split_w
			        if(n_3DPacking>=PACKING_LIMITS || (n_3DPacking >= PACKING_LIMITS/2 && !is_found_feasible)||(System.nanoTime() - begintime) / (1e9) > TIME_LIMITS) {
			        	break;
			        }
			    }// for (int split_w = 1; split_w < n_split+1; split_w++)
		    	}//for relax_r

		        if(n_3DPacking>=PACKING_LIMITS || (n_3DPacking >= PACKING_LIMITS/2 && !is_found_feasible)||(System.nanoTime() - begintime) / (1e9) > TIME_LIMITS) {
		        	break;
		        }

		    }
			System.out.printf("\t step 2 time:npack(RFPS):%.1f:%d",(System.nanoTime() - begintime) / (1e9),n_3DPacking);
			System.out.printf(" nruns="+record_nruns);
					
			final_ss.removeDomintes();
			
			
			/**
			 * ==============================
			 * add the pre send routes to the final solutions
			 * ==============================
			 */
			
		    for (int n_solu=0;n_solu<final_ss.size();n_solu++) {
		    	for (int n_route_pre=0;n_route_pre<pre_route_list.size();n_route_pre++) {
		    		final_ss.get(n_solu).getRoutes().add(pre_route_list.get(n_route_pre));
		    	}
		    }
					

			if(is_calculate_hv)
			System.out.printf("\t hv(PFRS+RFPS):%4.3f", final_ss.get2DHV(idealNadir));
			if (debug)
				output.outputJSON(final_ss, filenames[fileidx], PlatformIDCodeMap,
						output_directory + '/' + filenames[fileidx]);

			
			//if(test)
			double hv0 = final_ss.get2DHV(idealNadir);	
			final_ss.printObjectivesToFile("./results/"+filenames[fileidx] + "_OBJ","./results/"+filenames[fileidx] + "_OBJOriginal",idealNadir,CLIENT_NUM,BOX_NUM,hv0);
			output.outputJSON(final_ss, filenames[fileidx], PlatformIDCodeMap, output_directory + '/' + filenames[fileidx]);
			// /**************Ã§Â»Å¸Ã¨Â®Â¡Ã¥Â½â€œÃ¥â€°ï¿½Ã©â€”Â®Ã©Â¢ËœÃ§Å¡â€žÃ§Â»â€œÃ¦Å¾Å“**************/
			long endtime = System.nanoTime();
			double usedTime = (endtime - begintime) / (1e9);
			//// System.out.println();
			//// System.out.println("Program tookÃ¯Â¼Å¡"+usedTime+"s");

			if (is_calculate_hv) {
				double hv = final_ss.get2DHV(idealNadir);
				System.out.printf("\t finalhv:%4.3f", hv);
				bw.write("\t"+hv);
				total_hv = total_hv + hv;
			}
			System.out.print("\t solution num:" + final_ss.size());
			System.out.printf("\t time:%.1f s", usedTime);
			System.out.print("\t 3dbpp: " + n_3DPacking);
			if (final_ss.size()<1) {
				System.out.print("\t min actural dis: no solution" );
			}
			else {
				System.out.print("\t min actural dis: " + (final_ss.get(0).getF2()+pre_route_dis));
			}
			
			
			double totalLength=0;
			double loadingRatio=0;
			double totalLengthN=0;
			double loadingRatioN=0;
			double minF1 = idealNadir[0],minF2 = idealNadir[1],maxF1=idealNadir[2],maxF2=idealNadir[3];
			for (int i=0; i<final_ss.size();i++) {
				totalLength += final_ss.get(i).getF2();
				loadingRatio += final_ss.get(i).getF1();
				totalLengthN += ( final_ss.get(i).getF2()-minF2)/(maxF2-minF2);
				loadingRatioN += ( final_ss.get(i).getF1()-minF1)/(maxF1-minF1);
			}
			
			totalLengthAll += totalLength;
			loadingRatioAll += loadingRatio;
			totalLengthAllN += totalLengthN;
			loadingRatioAllN += loadingRatioN;
			nSolutionAll += final_ss.size();
			packingusedTimeAll += packingusedTime;
				 	
			
			bw.write("\t"+totalLength/final_ss.size());
			bw.write("\t"+loadingRatio/final_ss.size());
			bw.write("\t"+totalLengthN/final_ss.size());
			bw.write("\t"+loadingRatioN/final_ss.size());
			bw.write("\t"+final_ss.size());
			bw.write("\t"+usedTime);
			bw.write("\t"+n_3DPacking);
			bw.write("\t"+packingusedTime);
			bw.newLine();
			
			if (isUpdateExtreme) {
			
				double[] newIdealNadir = final_ss.getIdealNadir();
	
				Double[] mid = idealNadirMap.get(filenames[fileidx]);
				if (newIdealNadir[0]<mid[0] ){
					mid[0]  = newIdealNadir[0];
				}
				if (newIdealNadir[1]<mid[1]){
					mid[1] = newIdealNadir[1];
				}
				idealNadirMap.put(filenames[fileidx],mid);
				
					writeExtreme(idealNadirMap);
			}
			
			
			total_time = total_time + usedTime;
			if(usedTime>max_single_time)
				max_single_time = usedTime;
			if(Math.floorMod(fileidx+1, 50)==0)
				System.out.println(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
			System.out.println();

			final_ss.clear();
			
			/**
			 * end of one order.
			 */
		} // filename
		/**
		 * end of all orders
		 */
		
		System.out.println("Total HV:\t" + total_hv);
		System.out.println("Total Time:\t" + total_time);
		System.out.println("max single time:"+max_single_time);
		System.out.println(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
		// Ã¤Â¿ï¿½Ã¥Â­ËœÃ¦â€°â‚¬Ã¦Å“â€°idealNadirMap
		bwFinal.write("aver_dis   aver_loadr  normalized_aver_dis  normalized_aver_loadr  total_hv  total_time  pack_time");
		bwFinal.newLine();
		bwFinal.write("\t"+totalLengthAll/nSolutionAll);
		bwFinal.write("\t"+loadingRatioAll/nSolutionAll);
		bwFinal.write("\t"+totalLengthAllN/nSolutionAll);
		bwFinal.write("\t"+loadingRatioAllN/nSolutionAll);
		bwFinal.write("\t"+total_hv);
		bwFinal.write("\t"+total_time);
		bwFinal.write("\t"+packingusedTimeAll);
		
		bwFinal.close();
		bw.close();
		
		
		if (isUpdateExtreme)
			writeExtreme(idealNadirMap);
	}

	// =============================================================

	/**
	 * Ã¥Ë†Â¤Ã¦â€“Â­Ã§Â»â€œÃ¥ï¿½Ë†Ã¥ï¿½Å½Ã§Å¡â€žÃ¤Â¸Â¤Ã¦ï¿½Â¡Ã¨Â·Â¯Ã§ÂºÂ¿Ã¦ËœÂ¯Ã¥ï¿½Â¦Ã¦Å“â€°Ã©â€¡ï¿½Ã¥ï¿½Â Ã§Å¡â€žplatformÃ¯Â¼Å’
	 * 
	 * @param r1
	 *            Ã¨Â·Â¯Ã§ÂºÂ¿1
	 * @param r2
	 *            Ã¨Â·Â¯Ã§ÂºÂ¿2
	 * @return Ã¨Â¿â€�Ã¥â€ºÅ¾Ã©â€¡ï¿½Ã¥ï¿½Â Ã§Å¡â€žplatformÃ§Å¡â€žid
	 */
	public static ArrayList<Integer> isOverlap(Route r1, Route r2) {
		ArrayList<Integer> overlapIdx = new ArrayList<Integer>();
		for (int i = 1; i < r1.getNodes().size() - 1; i++) {
			for (int j = 1; j < r2.getNodes().size() - 1; j++) {
				if (r1.getNodes().get(i).getPlatformID() == r2.getNodes().get(j).getPlatformID()) {// Ã¥Å“Â¨r2Ã¤Â¸Â­Ã¦Å“â€°r1_iÃ¨Â¿â„¢Ã¤Â¸Âªnode
					overlapIdx.add(r1.getNodes().get(i).getPlatformID());
				}
			}
		}
		return overlapIdx;
	}

	/**
	 * 
	 * @param r1
	 * @param r2
	 * @return Ã¨Â¿â€�Ã¥â€ºÅ¾Ã§â€ºÂ¸Ã¥ï¿½Å’Ã¨Å â€šÃ§â€šÂ¹Ã§Å¡â€žÃ¤Â¸â€¹Ã¦Â â€¡Ã£â‚¬â€š
	 */
	public static ArrayList<L> RoutesOverlap(Route r1, Route r2) {
		ArrayList<L> overlapIdx = new ArrayList<L>();
		for (int i = 1; i < r1.getNodes().size() - 1; i++) {
			for (int j = 1; j < r2.getNodes().size() - 1; j++) {
				if (r1.getNodes().get(i).getPlatformID() == r2.getNodes().get(j).getPlatformID()
						&& !r1.getNodes().get(i).isMustFirst()) {// Ã¥Å“Â¨r2Ã¤Â¸Â­Ã¦Å“â€°r1_iÃ¨Â¿â„¢Ã¤Â¸Âªnode
					// Route r1 node i is same with Route r2 node j
					L l = new L();
					l.setI(i);
					l.setJ(j);
					overlapIdx.add(l);
				}
			}
		}
		return overlapIdx;
	}

	/**
	 * Ã¥Â°â€ Ã¦â€ºÂ´Ã¦â€“Â°Ã§Å¡â€židealÃ¥â€™Å’nadir pointsÃ¤Â¿ï¿½Ã¥Â­ËœÃ¥Ë†Â°.\\data\\extremes
	 * 
	 * @param ideaNadirMap
	 */
	@SuppressWarnings("unchecked")
	public static void writeExtreme(Map<String, Double[]> ideaNadirMap) {

		// Ã¥Ë†â€ºÃ¥Â»ÂºjsonÃ¥Â¯Â¹Ã¨Â±Â¡
		JSONObject extremesJSONObject = new JSONObject();
		JSONArray problemsArray = new JSONArray();
		File f = new File("./data/inputs");
		String[] filenames = f.list();
		for (int fileidx = 0; fileidx < filenames.length; fileidx++) {
			JSONObject problemObj = new JSONObject();
			problemObj.put("problemID", fileidx);
			problemObj.put("problemName", filenames[fileidx]);
			problemObj.put("outputFileForIdealF1", "./data/idealF1/" + filenames[fileidx]);
			problemObj.put("outputFileForIdealF2", "./data/idealF2/" + filenames[fileidx]);
			problemObj.put("outputFileForNadirF1", "./data/nadirF1/" + filenames[fileidx]);
			problemObj.put("outputFileForNadirF2", "./data/nadirF2/" + filenames[fileidx]);
			problemObj.put("idealF1", ideaNadirMap.get(filenames[fileidx])[0]);
			problemObj.put("idealF2", ideaNadirMap.get(filenames[fileidx])[1]);
			problemObj.put("nadirF1", ideaNadirMap.get(filenames[fileidx])[2]);
			problemObj.put("nadirF2", ideaNadirMap.get(filenames[fileidx])[3]);
			problemsArray.add(problemObj);
		}
		extremesJSONObject.put("problems", problemsArray);
		// Ã¥Â°â€ jsonÃ¥Â¯Â¹Ã¨Â±Â¡Ã¥â€ â„¢Ã¥â€¦Â¥Ã¦â€“â€¡Ã¤Â»Â¶Ã£â‚¬â€š
		try (FileWriter file = new FileWriter("./data/extremes")) {
			file.write(extremesJSONObject.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Ã¤Â»Å½.\\data\\extremesÃ©â€¡Å’Ã©ï¿½Â¢Ã¨Â¯Â»Ã¥ï¿½â€“ideal and nadir points.
	 * 
	 * @return
	 */
	public static Map<String, Double[]> readExtreme() {
		Map<String, Double[]> idealNadirMap = new HashMap<String, Double[]>();// problemName->ideal&nadir
		// JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();
		try (FileReader reader = new FileReader("./data/extremes")) {
			JSONObject obj = (JSONObject) jsonParser.parse(reader);// Ã¦Å“â‚¬Ã©Â¡Â¶Ã¥Â±â€š

			JSONArray problemsArray = (JSONArray) obj.get("problems");
			Iterator<JSONObject> iterator = problemsArray.iterator();// Ã§â€�Â¨Ã¦ï¿½Â¥Ã©ï¿½ï¿½Ã¥Å½â€ JSONArrayÃ¤Â¸Â­Ã§Å¡â€žJSONObject
			while (iterator.hasNext()) {
				JSONObject curr_problem = iterator.next();
				Double[] idealNadirValues = { (Double) curr_problem.get("idealF1"),
						(Double) curr_problem.get("idealF2"), (Double) curr_problem.get("nadirF1"),
						(Double) curr_problem.get("nadirF2") };
				idealNadirMap.put((String) curr_problem.get("problemName"), idealNadirValues);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return idealNadirMap;
	}
	

	/**
	 * Ã¥Ë†Â¤Ã¦â€“Â­solutionÃ¦ËœÂ¯Ã¥ï¿½Â¦Ã¥ï¿½Â¯Ã¨Â¡Å’Ã£â‚¬â€š
	 * 
	 * @param solution
	 * @param problem
	 * @param PlatformIDCodeMap
	 * @return
	 * @throws IOException
	 */
	public static boolean check_feasible(Solution_vrp solution, String problem, Map<Integer, String> PlatformIDCodeMap)
			throws IOException {
		output.outputJSON(solution, problem, ".\\data\\temp\\" + problem,PlatformIDCodeMap);
		Check orderCheck = Check.getOrderCheck(".\\data\\inputs", ".\\data\\temp", problem);
		if (!orderCheck.check()) {
			ArrayList<String> errorMessages = orderCheck.getErrorMessages();
			if (!errorMessages.isEmpty()) {
				System.out.println("the solution saved in temp is not feasible:" + problem);
				for (String errorMessage : errorMessages) {
					System.out.println(errorMessage);
				}
				System.out.println("");
			}
			return false;
		}
		return true;
	}

	/**
	 * Ã¦â€ºÂ´Ã¦â€“Â°idealÃ¥â€™Å’nadir points
	 * 
	 * @param solution
	 * @param idealNadir
	 * @param filenames
	 * @param fileidx
	 * @param PlatformIDCodeMap
	 * @return
	 * @throws IOException
	 */
	// Ã¦Â³Â¨Ã¦â€žï¿½Ã¯Â¼Å¡Ã¤Â¼Â Ã¥â€¦Â¥Ã§Å¡â€žÃ¥Â¿â€¦Ã©Â¡Â»Ã¦ËœÂ¯Ã¥ï¿½Â¯Ã¨Â¡Å’Ã¨Â§Â£Ã£â‚¬â€š
	public static boolean update_idealNadir(Solution_vrp solution, Double[] idealNadir, String[] filenames, int fileidx,
			Map<Integer, String> PlatformIDCodeMap) throws IOException {
		output.outputJSON(solution, filenames[fileidx], ".\\data\\temp\\" + filenames[fileidx],PlatformIDCodeMap);
		Check orderCheck = Check.getOrderCheck(".\\data\\inputs", ".\\data\\temp", filenames[fileidx]);
		if (!orderCheck.check()) {
			ArrayList<String> errorMessages = orderCheck.getErrorMessages();
			if (!errorMessages.isEmpty()) {
				System.out.println("the solution saved in temp is not feasible:" + filenames[fileidx]);
				// for (String errorMessage: errorMessages) {
				// System.out.println(errorMessage);
				// }
				System.out.println("");
			}
			return false;
		}
		if (solution.getF1() < idealNadir[0]) {
			idealNadir[0] = solution.getF1();// minF1
			output.outputJSON(solution, filenames[fileidx], ".\\data\\idealF1\\" + filenames[fileidx],PlatformIDCodeMap);
			System.out.print("new idealF1 is found.");
		}
		if (solution.getF2() < idealNadir[1]) {
			idealNadir[1] = solution.getF2();// minF2
			output.outputJSON(solution, filenames[fileidx], ".\\data\\idealF2\\" + filenames[fileidx],PlatformIDCodeMap);
			System.out.print("new idealF2 is found.");
		}
		if (solution.getF1() > idealNadir[2]) {
			idealNadir[2] = solution.getF1();// maxF1
			output.outputJSON(solution, filenames[fileidx], ".\\data\\nadirF1\\" + filenames[fileidx],PlatformIDCodeMap);
			System.out.print("new nadirF1 is found.");
		}
		if (solution.getF2() > idealNadir[3]) {
			idealNadir[3] = solution.getF2();// maxF2
			output.outputJSON(solution, filenames[fileidx], ".\\data\\nadirF2\\" + filenames[fileidx],PlatformIDCodeMap);
			System.out.print("new nadirF2 is found.");
		}
		return true;
	}

	/**
	 * Ã¥Â°â€ route.boxesÃ¥Ë†â€ Ã©â€¦ï¿½Ã¥Ë†Â°Ã¦Â¯ï¿½Ã¤Â¸ÂªÃ¨Å â€šÃ§â€šÂ¹Ã¤Â¸Å Ã£â‚¬â€šÃ¤Â½Â¿Ã¥â€¦Â¶Ã¤Â¸â‚¬Ã¤Â¸â‚¬Ã¥Â¯Â¹Ã¥Âºâ€�Ã£â‚¬â€š
	 * 
	 * @param incumbentSolution
	 */
	public static void allocateBoxes2Node(Solution_vrp incumbentSolution) {
		for (int routei = 0; routei < incumbentSolution.getRoutes().size(); routei++) {// Ã¥Â¯Â¹Ã¦Â¯ï¿½Ã¦ï¿½Â¡Ã¨Â·Â¯Ã¥Â¾â€žÃ£â‚¬â€š
			Route currRoute = new Route(incumbentSolution.getRoutes().get(routei));
			Iterator<Box> allBoxesIterator = currRoute.getBoxes().iterator();// Ã©ï¿½ï¿½Ã¥Å½â€ Ã¨Â¿â„¢Ã¦ï¿½Â¡Ã¨Â·Â¯Ã¥Â¾â€žÃ¤Â¸Å Ã§Å¡â€žÃ¦â€°â‚¬Ã¦Å“â€°Ã¨Å â€šÃ§â€šÂ¹Ã£â‚¬â€š
			int nodei = 1;
			ArrayList<Box> goods = new ArrayList<Box>();
			while (allBoxesIterator.hasNext()) {
				Box currBox = allBoxesIterator.next();
				if (currBox.getPlatformid() == currRoute.getNodes().get(nodei).getPlatformID()) {
					goods.add(new Box(currBox));
				} else {
					incumbentSolution.getRoutes().get(routei).getNodes().get(nodei).setGoods(goods);// Ã¥Â½â€œÃ¥â€°ï¿½Ã¨Â§Â£Ã¤Â¸ï¿½Ã¦ËœÂ¯Ã¨Â¿â„¢Ã¤Â¸ÂªÃ¥Â¹Â³Ã¥ï¿½Â°Ã§Å¡â€žÃ¤Âºâ€ Ã£â‚¬â€š
					nodei = nodei + 1;// Ã¤Â¸â€¹Ã¤Â¸â‚¬Ã¤Â¸ÂªÃ¨Å â€šÃ§â€šÂ¹Ã¥Â¹Â³Ã¥ï¿½Â°Ã£â‚¬â€š
					goods = new ArrayList<Box>();// Ã¦Â¸â€¦Ã§Â©Âºgoods
					goods.add(new Box(currBox));
				}
			}
			incumbentSolution.getRoutes().get(routei).getNodes().get(nodei).setGoods(goods);// Ã¥Â½â€œÃ¥â€°ï¿½Ã¨Â§Â£Ã¤Â¸ï¿½Ã¦ËœÂ¯Ã¨Â¿â„¢Ã¤Â¸ÂªÃ¥Â¹Â³Ã¥ï¿½Â°Ã§Å¡â€žÃ¤Âºâ€ Ã£â‚¬â€š
		}
	}

	/**
	 * 
	 * @param currRoute
	 */
	public static void allocateBoxes2Node(Route currRoute) {
		Iterator<Box> allBoxesIterator = currRoute.getBoxes().iterator();// Ã©ï¿½ï¿½Ã¥Å½â€ Ã¨Â¿â„¢Ã¦ï¿½Â¡Ã¨Â·Â¯Ã¥Â¾â€žÃ¤Â¸Å Ã§Å¡â€žÃ¦â€°â‚¬Ã¦Å“â€°Ã¨Å â€šÃ§â€šÂ¹Ã£â‚¬â€š
		int nodei = 1;
		ArrayList<Box> goods = new ArrayList<Box>();
		int goodsNum = 0;
		double goodsVolumn = 0.0;
		double goodsWeight = 0.0;

		while (allBoxesIterator.hasNext()) {
			Box currBox = allBoxesIterator.next();
			if (currBox.getPlatformid() == currRoute.getNodes().get(nodei).getPlatformID()) {
				goods.add(new Box(currBox));
				goodsNum += 1;
				goodsVolumn += currBox.getVolume();
				goodsWeight += currBox.getWeight();
			} else {
				currRoute.getNodes().get(nodei).setGoodsNum(goodsNum);
				currRoute.getNodes().get(nodei).setGoodsVolumn(goodsVolumn);
				currRoute.getNodes().get(nodei).setGoodsWeight(goodsWeight);
				currRoute.getNodes().get(nodei).setGoods(goods);// Ã¥Â½â€œÃ¥â€°ï¿½Ã¨Â§Â£Ã¤Â¸ï¿½Ã¦ËœÂ¯Ã¨Â¿â„¢Ã¤Â¸ÂªÃ¥Â¹Â³Ã¥ï¿½Â°Ã§Å¡â€žÃ¤Âºâ€ Ã£â‚¬â€š
				nodei = nodei + 1;// Ã¤Â¸â€¹Ã¤Â¸â‚¬Ã¤Â¸ÂªÃ¨Å â€šÃ§â€šÂ¹Ã¥Â¹Â³Ã¥ï¿½Â°Ã£â‚¬â€š
				goods = new ArrayList<Box>();// Ã¦Â¸â€¦Ã§Â©Âºgoods
				goods.add(new Box(currBox));
				goodsNum = 1;
				goodsVolumn = currBox.getVolume();
				goodsWeight = currBox.getWeight();
			}
		}
		currRoute.getNodes().get(nodei).setGoodsNum(goodsNum);
		currRoute.getNodes().get(nodei).setGoodsVolumn(goodsVolumn);
		currRoute.getNodes().get(nodei).setGoodsWeight(goodsWeight);
		currRoute.getNodes().get(nodei).setGoods(goods);// Ã¥Â½â€œÃ¥â€°ï¿½Ã¨Â§Â£Ã¤Â¸ï¿½Ã¦ËœÂ¯Ã¨Â¿â„¢Ã¤Â¸ÂªÃ¥Â¹Â³Ã¥ï¿½Â°Ã§Å¡â€žÃ¤Âºâ€ Ã£â‚¬â€š
		if (nodei + 2 != currRoute.getNodes().size())
			System.out.println("error in allocateBox2Node!!!");
	}



	
	static SolutionSet_vrp get_Solutions(int encoding_strategy, double  split_minv, ArrayList<Solution> solutionsList_0, double relax_ratio, int[] save_feasible_no) throws IOException {
		double[][] client_v_w = new double[CLIENT_NUM][2];//client_volume_weight;
	    for(int i=0;i<CLIENT_NUM;i++) {
	    	client_v_w[i][0] = client_volume_weight[i][0];
	    	client_v_w[i][1] = client_volume_weight[i][1];
	    }
	    double Split_precision = 6.0;
	    int x = 0 ;
	    
   
	    SolutionSet_vrp solutionSet = new SolutionSet_vrp();

	  
	    for (int n_sol =0; n_sol <solutionsList_0.size(); n_sol++) {
	    	//check for the feasibility of this solution.
	        if(n_3DPacking>PACKING_LIMITS) {
	        	break;
	        }
	    	Solution aSolutionsList_ = solutionsList_0.get(n_sol);
	    	
		    Solution_vrp solution = new Solution_vrp();
		    solution.distanceMap=distanceMap;
	      	
	        int n_loadboxes = 0;
		    int new_num_nodes = 0;
		    int numberOfCities_ = 0;
		    
		    int[] nodes_record = new int[CLIENT_NUM];
		    
		    // 
		    
		    for (int i = 0; i < CLIENT_NUM; i++) {
		    	if  (client_v_w[i][0] >  split_minv) {
		        	new_num_nodes += 2; 
		    	}
		    	else {
		        	new_num_nodes += 1;
		    	} 	
		    }//for CLIENT_NUM
		    

		    
			numberOfCities_ = new_num_nodes;
			int[] client_city_map = new int[numberOfCities_];
			     
//			double[][] new_node_coords = new double[numberOfCities_][2];
			double[] new_node_v = new double[numberOfCities_];
			
			
			if (encoding_strategy == 1) {
				int var = 2;
				new_num_nodes = 0;
				for (int i = 0; i < CLIENT_NUM; i++) {
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
				
			}
			else {
				
				int var = 2;
				new_num_nodes = 0;
				for (int i = 0; i < CLIENT_NUM; i++) {
					if  (client_v_w[i][0] >   split_minv) {

					    client_city_map[new_num_nodes] = i;
				    	new_node_v[new_num_nodes] = client_v_w[i][0]*0.5;
				    	new_num_nodes += 1; 
				    	client_city_map[new_num_nodes] = i;
				    	new_node_v[new_num_nodes] = client_v_w[i][0]*0.5;
				    	new_num_nodes += 1; 
						
						var += 1;
					}
					else {
						client_city_map[new_num_nodes] = i;
						new_node_v[new_num_nodes] = client_v_w[i][0];    	
						new_num_nodes += 1; 	
					}
				}
				
			}
			

		        
//		    no_results += 1;
			

	        int n_route = 0;
	        double[] w_route = new double[100];
	        int[][] route_node_map = new int[100][100];
	        int[] n_node = new int[100];
			
			if (encoding_strategy == 1 || encoding_strategy == 2 || encoding_strategy == 3 ) {
				
				String if_start = aSolutionsList_.getDecisionVariables()[1].toString();
				char [] if_start0 = if_start.toCharArray();

		        for (int i = 0; i < 100; i++) {
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
			
			}
			
			else {

		        for (int i = 0; i < 100; i++) {
		        	 n_node[i] = 0;
		        	 w_route[i] = 0.0;
		        }
		        for (int j = 0; j < numberOfCities_; j++) {
		      	  
			      	x = ((Permutation)aSolutionsList_.getDecisionVariables()[0]).vector_[j];
			      	if (w_route[n_route]>= VEHICLE_VOLUME[VEHICLE_VOLUME.length-1]*relax_ratio) {
					          n_route += 1;
			      	}
			        w_route[n_route] += new_node_v[x];
			        route_node_map[n_route][n_node[n_route]] = x;
			        n_node[n_route] += 1;
		        }
				
				
			}
		    	    

	        
	        int[] node_if_check = new int[CLIENT_NUM];
	        for (int i = 0; i < CLIENT_NUM; i++) {
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
		        //System.out.println(n_node[i]);
		        //System.out.println(nj);
	        	
		        //ArrayList<Node> node_mid =  new ArrayList<Node>();
	        	int current_truck_id = 0;
		        Route route = new Route((int)(1000+Math.random()*8999));
		        for (int j = 0; j < TRUCKTYPE_NUM; j++) {
		        	if (w_route[i] < VEHICLE_VOLUME[j]*relax_ratio) {
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
				
				nodes.add(new Node(depot_start));
				
				int[] node_id_last = new int[n_node[i]];
				for(int nn=0;nn<n_node[i];nn++) {
					node_id_last[nn] =100;
				}
				
		        for (int j = 0; j < n_node[i]; j++) {
		        	
		        	//nodeÃ¤Â¸Å Ã©ï¿½Â¢Ã¦â€°â‚¬Ã¦Å“â€°Ã§Å¡â€žboxes
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
					int truck_start = 0;
					
					if (encoding_strategy == 3 || encoding_strategy == 4) {
						truck_start = TRUCKTYPE_NUM - 1;
					}
					
			        for (int j = truck_start; j < TRUCKTYPE_NUM; j++) {
						Carriage vehicle1 = new Carriage(BASIC_TRUCKS.get(j));
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
						}
						if (if_check) {
					    	n_3DPacking++;
							k = pack.packingrun(unloadboxes,vehicle1,packing_type);
							if (k.size() == unloadboxes.size()) {
								route.setBoxes(pack.get_Boxes());
								route.setLoadWeight(pack.get_loadWeight());
								route.setLoadVolumn(pack.get_loadVolumn());
								break;
							}
							k = pack.packingrun(unloadboxes_trans,vehicle1,packing_type);
							if (k.size() == unloadboxes.size()) {
								route.setBoxes(pack.get_Boxes());
								route.setLoadWeight(pack.get_loadWeight());
								route.setLoadVolumn(pack.get_loadVolumn());
								break;
							}
							k = pack.packingrun(unloadboxes_half_trans,vehicle1,packing_type);
							if (k.size() == unloadboxes.size()) {
								route.setBoxes(pack.get_Boxes());
								route.setLoadWeight(pack.get_loadWeight());
								route.setLoadVolumn(pack.get_loadVolumn());
								break;
							}
						}
			        }
			                

					if (k!=null) {
						n_loadboxes += k.size();
						solution.getRoutes().add(route);
						if (k.size() !=unloadboxes.size()) {
							break;
						}
					}
				}
	        }//for n_route+1
	        
	        if (n_loadboxes == BOX_NUM) {
//	        if (true) {
	        	solution.evaluation();
	        	solutionSet.add(new Solution_vrp(solution));
	        	save_feasible_no[n_sol] = 1;        	
	        }
	        else {
	        	save_feasible_no[n_sol] = 0;
	        }
	    }
	    return solutionSet;
	}

}// end class
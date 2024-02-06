package solver.packing;

import java.util.ArrayList;

import solver.util.Box;
import solver.util.Carriage;
import solver.packing.ConstructiveHeuristics;



public class Packing {
	
	private boolean DEBUG = false;
	
	private ArrayList<Box> Boxes = new ArrayList<Box>();

	private ArrayList<Double> horizontal_levels = new ArrayList<Double>();
	private double loadWeight;
	private double loadVolumn;

	public ArrayList<Integer> packingrun(ArrayList<Box> boxingSequence,Carriage carriage,int packing_type){
		
		
		int sz = boxingSequence.size();

		
		ConstructiveHeuristics ch = new ConstructiveHeuristics();
		
		
		
		ArrayList<Integer> loadIdx;
		
		
		
		
		loadIdx = ch.dblf(boxingSequence,carriage,0);
		if(loadIdx.size()==sz) {
			if(DEBUG)
			carriage.setTruckTypeCode(carriage.getTruckTypeCode()+"-i0");
//			System.out.println("succ by dblf-0");
			Boxes = ch.get_Boxes();
			loadWeight = ch.get_loadWeight();
			loadVolumn = ch.get_loadVolumn();
			return loadIdx;
		}
		
		loadIdx = ch.dblf(boxingSequence,carriage,1);
		if(loadIdx.size()==sz) {
			if(DEBUG)
			carriage.setTruckTypeCode(carriage.getTruckTypeCode()+"-i1");
//			System.out.println("succ by dblf-1");
			Boxes = ch.get_Boxes();
			loadWeight = ch.get_loadWeight();
			loadVolumn = ch.get_loadVolumn();
			return loadIdx;
		}
		
		loadIdx = ch.dblf_node(boxingSequence,carriage);
		if(loadIdx.size()==sz) {
			if(DEBUG)
			carriage.setTruckTypeCode(carriage.getTruckTypeCode()+"-i4");
//				System.out.println("succ by dblf_node");
			Boxes = ch.get_Boxes();
			loadWeight = ch.get_loadWeight();
			loadVolumn = ch.get_loadVolumn();
			return loadIdx;
		}

		
		if (packing_type > 0) {
			loadIdx = ch.zqlbpp(boxingSequence,carriage,2);
			if(loadIdx.size()==sz) {
				if(DEBUG)
				carriage.setTruckTypeCode(carriage.getTruckTypeCode()+"-i2");
//				System.out.println("succ by zqlbpp-2");
				Boxes = ch.get_Boxes();
				loadWeight = ch.get_loadWeight();
				loadVolumn = ch.get_loadVolumn();
				return loadIdx;
			}
			
		}
		
		if (packing_type > 1) {
			loadIdx = ch.zqlbpp(boxingSequence,carriage,3);
			if(loadIdx.size()==sz) {
				if(DEBUG)
				carriage.setTruckTypeCode(carriage.getTruckTypeCode()+"-i3");
//				System.out.println("succ by zqlbpp-3");
				Boxes = ch.get_Boxes();
				loadWeight = ch.get_loadWeight();
				loadVolumn = ch.get_loadVolumn();
				return loadIdx;
			}		
		}
		
		Boxes = ch.get_Boxes(); // for these not all loaded, we still store the loaded boxes for pre-packing procedure
		
		return loadIdx;
	}
	
	public  ArrayList<Box> get_Boxes (){
		return Boxes;
	}
	
	public double get_loadWeight() {
		return loadWeight;
	}
	
	public double get_loadVolumn() {
		return loadVolumn;
	}
}

package solver.inoutput;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import solver.Route;
import solver.SolutionSet_vrp;
import solver.Solution_vrp;
import solver.util.Box;
import solver.util.Node;

public class Output {

	/**
	 * output function
	 * 
	 * @param solutionSet
	 * @param filename
	 * @param PlatformIDCodeMap
	 */
	public static void output(SolutionSet_vrp solutionSet, String filename, Map<Integer, String> PlatformIDCodeMap) {
		for (Solution_vrp s : solutionSet.solutionList_) {
			for (Route r : s.getRoutes()) {
				double curr_z = 0.0;
				ArrayList<Box> boxes = new ArrayList<Box>();
				for (Node n : r.getNodes()) {
					/**
					 * å°†node nçš„boxesåŠ å…¥åˆ°route.boxesé‡Œé�¢ã€‚
					 * 
					 */
					for (Box b : n.getGoods()) {
						Box new_b = new Box(b);
						new_b.setZCoor(b.getZCoor() + curr_z);
						boxes.add(new_b);
					}
					curr_z = curr_z + n.getDemands();
				}
				r.setBoxes(boxes);
			}
		}
		outputJSON(solutionSet, filename, PlatformIDCodeMap, ".\\data\\outputs\\" + filename);
	}

	/**
	 * 
	 * @param solution
	 * @param instanceName
	 * @param PlatformIDCodeMap
	 * @param outputFile
	 */
	@SuppressWarnings("unchecked")
	public void outputJSON(Solution_vrp solution, String instanceName,String outputFile, Map<Integer, String> PlatformIDCodeMap) {
		// output to json file.
		JSONObject outputJSONObject = new JSONObject();
		// {
		// "estimateCode":"E1594518281316",
		outputJSONObject.put("estimateCode", instanceName);
		// ***************************************************å‡†å¤‡truckArray
		JSONArray truckArray = new JSONArray();

		// Iterator<Route> iteratorRoute = solution.getRoutes().iterator();
		// while(iteratorRoute.hasNext()) {
		for (int routei = 0; routei < solution.getRoutes().size(); routei++) {
			// Carriage currTruck = .getCarriage();
			Route route = solution.getRoutes().get(routei);
			// ä¸€è¾†è½¦
			JSONObject truckJSONObject = new JSONObject();
			// è¿™è¾†è½¦åŸºæœ¬ä¿¡æ�¯ 1. innerHeight
			truckJSONObject.put("innerHeight", route.getCarriage().getHeight());

			// è¿™è¾†è½¦ç»�è¿‡çš„è·¯å¾„ä¿¡æ�¯->2. platformArray
			ArrayList<String> platformArray = new ArrayList<String>();
			for (int nodei = 1; nodei < route.getNodes().size() - 1; nodei++) {
				platformArray.add(PlatformIDCodeMap.get(route.getNodes().get(nodei).getPlatformID()));
			}
			//
			// while(iterator.hasNext())
			// if(iterator.)
			//
			truckJSONObject.put("platformArray", platformArray);
			truckJSONObject.put("volume", route.getLoadVolume());
			truckJSONObject.put("innerWidth", route.getCarriage().getWidth());
			truckJSONObject.put("truckTypeCode", route.getCarriage().getTruckTypeCode());
			truckJSONObject.put("piece", route.getBoxes().size());// number of boxes
			JSONArray spuArray = new JSONArray();// the boxes array

			// Iterator<Box> iteratorBox = route.getCarriage().getBoxes().iterator();
			int order = 1;
			// while(iteratorBox.hasNext()) {
			for (int boxi = 0; boxi < route.getBoxes().size(); boxi++) {
				Box box = route.getBoxes().get(boxi);// iteratorBox.next();
				JSONObject currBox = new JSONObject();// the current box information
				currBox.put("spuId", box.getSpuBoxID());
				currBox.put("order", order);
				order = order + 1;
				currBox.put("direction", box.getDirection());// length parallel to the vehicle's length
				currBox.put("x", box.getXCoor() + box.getWidth() / 2.0 - route.getCarriage().getWidth() / 2.0);// -box.getWidth()
				currBox.put("y", box.getYCoor() + box.getHeight() / 2 - route.getCarriage().getHeight() / 2.0);//
				currBox.put("length", box.getLength());
				currBox.put("weight", box.getWeight());
				currBox.put("height", box.getHeight());
				currBox.put("width", box.getWidth());
				currBox.put("platformCode", PlatformIDCodeMap.get(box.getPlatformid()));
				currBox.put("z", box.getZCoor() + box.getLength() / 2 - route.getCarriage().getLength() / 2.0);// -box.getHeight()
				spuArray.add(currBox);
			}
			truckJSONObject.put("spuArray", spuArray);// the array of boxes
			truckJSONObject.put("truckTypeId", route.getCarriage().getTruckTypeId());
			truckJSONObject.put("innerLength", route.getCarriage().getLength());
			truckJSONObject.put("maxLoad", route.getCarriage().getCapacity());
			truckJSONObject.put("weight", route.getLoadWeight());
			truckArray.add(truckJSONObject);
		}
		// ***************************************************
		outputJSONObject.put("truckArray", truckArray);

		File f = new File(outputFile);
		if (!f.getParentFile().exists()) {
			f.getParentFile().mkdir();
		}
		// ".\\data\\outputs"+used_truck_id+"\\"+instanceName
		try (FileWriter file = new FileWriter(outputFile)) {
			file.write(outputJSONObject.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * output JSON format, for EMO2021 competition
	 * 
	 * @param solutionSet
	 * @param instanceName
	 * @param PlatformIDCodeMap
	 * @param outputFile
	 */
	@SuppressWarnings("unchecked")
	public static  void outputJSON(SolutionSet_vrp solutionSet, String instanceName, Map<Integer, String> PlatformIDCodeMap,
			String outputFile) {
		// output to json file.
		JSONObject outputJSONObject = new JSONObject();
		// {
		// "estimateCode":"E1594518281316",
		outputJSONObject.put("estimateCode", instanceName);

		ArrayList<JSONArray> solutionArray = new ArrayList<JSONArray>();
		for (int solutioni = 0; solutioni < solutionSet.size(); solutioni++) {
			Solution_vrp solution = solutionSet.get(solutioni);
			// Iterator<Route> iteratorRoute = solution.getRoutes().iterator();
			// while(iteratorRoute.hasNext()) {
			// ***************************************************å‡†å¤‡truckArray
			JSONArray truckArray = new JSONArray();
			for (int routei = 0; routei < solution.getRoutes().size(); routei++) {
				// Carriage currTruck = .getCarriage();
				Route route = solution.getRoutes().get(routei);
				// ä¸€è¾†è½¦
				JSONObject truckJSONObject = new JSONObject();
				// è¿™è¾†è½¦åŸºæœ¬ä¿¡æ�¯ 1. innerHeight
				truckJSONObject.put("innerHeight", route.getCarriage().getHeight());

				// è¿™è¾†è½¦ç»�è¿‡çš„è·¯å¾„ä¿¡æ�¯->2. platformArray
				ArrayList<String> platformArray = new ArrayList<String>();
				for (int nodei = 1; nodei < route.getNodes().size() - 1; nodei++) {
					platformArray.add(PlatformIDCodeMap.get(route.getNodes().get(nodei).getPlatformID()));
				}
				//
				// while(iterator.hasNext())
				// if(iterator.)
				//
				truckJSONObject.put("platformArray", platformArray);
				truckJSONObject.put("volume", route.getLoadVolume());
				truckJSONObject.put("innerWidth", route.getCarriage().getWidth());
				truckJSONObject.put("truckTypeCode", route.getCarriage().getTruckTypeCode());
				truckJSONObject.put("piece", route.getBoxes().size());// number of boxes
				JSONArray spuArray = new JSONArray();// the boxes array

				// Iterator<Box> iteratorBox = route.getCarriage().getBoxes().iterator();
				int order = 1;
				// while(iteratorBox.hasNext()) {
				for (int boxi = 0; boxi < route.getBoxes().size(); boxi++) {
					Box box = route.getBoxes().get(boxi);// iteratorBox.next();
					JSONObject currBox = new JSONObject();// the current box information
					currBox.put("spuId", box.getSpuBoxID());
					currBox.put("order", order);
					order = order + 1;
					currBox.put("direction", box.getDirection());// length parallel to the vehicle's length
					currBox.put("x", box.getXCoor() + box.getWidth() / 2.0 - route.getCarriage().getWidth() / 2.0);// -box.getWidth()
					currBox.put("y", box.getYCoor() + box.getHeight() / 2 - route.getCarriage().getHeight() / 2.0);//
					currBox.put("length", box.getLength());
					currBox.put("weight", box.getWeight());
					currBox.put("height", box.getHeight());
					currBox.put("width", box.getWidth());
					currBox.put("platformCode", PlatformIDCodeMap.get(box.getPlatformid()));
					currBox.put("z", box.getZCoor() + box.getLength() / 2 - route.getCarriage().getLength() / 2.0);// -box.getHeight()
					spuArray.add(currBox);
				}
				truckJSONObject.put("spuArray", spuArray);// the array of boxes
				truckJSONObject.put("truckTypeId", route.getCarriage().getTruckTypeId());
				truckJSONObject.put("innerLength", route.getCarriage().getLength());
				truckJSONObject.put("maxLoad", route.getCarriage().getCapacity());
				truckJSONObject.put("weight", route.getLoadWeight());
				truckArray.add(truckJSONObject);
			} // routei
			solutionArray.add(truckArray);
		} // solutioni
			// ***************************************************
		outputJSONObject.put("solutionArray", solutionArray);

		File f = new File(outputFile);
		if (!f.getParentFile().exists()) {
			f.getParentFile().mkdir();
		}
		// ".\\data\\outputs"+used_truck_id+"\\"+instanceName
		try (FileWriter file = new FileWriter(outputFile)) {
			file.write(outputJSONObject.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
}

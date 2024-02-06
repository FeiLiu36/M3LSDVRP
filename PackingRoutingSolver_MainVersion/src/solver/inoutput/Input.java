package solver.inoutput;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import solver.util.Box;
import solver.util.Carriage;
import solver.util.Node;

import java.io.File;
import java.util.Scanner;

public class Input {
	
	private int CLIENT_NUM;
	private int BOX_NUM;
	private int TRUCKTYPE_NUM;
	private int BOXTYPE_NUM;
	private HashMap<Integer, String> PlatformIDCodeMap = new HashMap<Integer, String>();;
	private HashMap<String,Integer> PlatformCodeIDMap = new HashMap<String, Integer>();;
	private ArrayList<Carriage> BASIC_TRUCKS = new ArrayList<Carriage>();
	private HashMap<String, Double> distanceMap = new HashMap<String, Double>();
	private ArrayList<Node> clients = new ArrayList<Node>();
	private ArrayList<Node> clients_trans = new ArrayList<Node>(); 
	private ArrayList<Node> clients_half_trans = new ArrayList<Node>();
	private ArrayList<Node> clients_v = new ArrayList<Node>();
	private ArrayList<Node> clients_trans_v = new ArrayList<Node>();
	private ArrayList<Node> clients_half_trans_v = new ArrayList<Node>();
	private double[][] client_volume_weight; 
	private Node depot_start = new Node(); 
	private Node depot_end = new Node();
	
	private String filename;
	
	
	// constructive function with filename as the input arg
	public Input(String filename) {
		this.filename = filename;
	}
	
	
	/**
	 * ==========================================================
	 * Read the data of JSON format, for EMO2021 competition
	 * ==========================================================
	 */		
	
	public void ReadJson() {	
		
		// JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();
		// String instanceName = "E1594518281316";
		try (FileReader reader = new FileReader(filename)) {
			JSONObject obj = (JSONObject) jsonParser.parse(reader);
			Iterator<JSONObject> iterator;
			JSONObject algorithmBaseParamDto = (JSONObject) obj.get("algorithmBaseParamDto");
			JSONArray platformDtoList = (JSONArray) algorithmBaseParamDto.get("platformDtoList");
			CLIENT_NUM = platformDtoList.size();
			boolean[] mustFirst = new boolean[CLIENT_NUM];
			client_volume_weight = new double[CLIENT_NUM][2];
			for(int i=0;i<CLIENT_NUM;i++) {
				client_volume_weight[i][0] = 0.0;
				client_volume_weight[i][1] = 0.0;
			}
			
			BOX_NUM=0;
			
			/**
			 * ==========================================================
			 * platformID to code info
			 * ==========================================================
			 */	
			
			iterator = platformDtoList.iterator();
			int platformID = 0;
			PlatformIDCodeMap.put(platformID, "start_point");
			PlatformCodeIDMap.put("start_point", platformID);
			platformID++;
			while (iterator.hasNext()) {
				JSONObject platform = iterator.next();
				String platformCode = (String) platform.get("platformCode");
				PlatformIDCodeMap.put(platformID, platformCode);
				PlatformCodeIDMap.put(platformCode, platformID);
				mustFirst[platformID - 1] = (boolean) platform.get("mustFirst");
				platformID++;
			}
			PlatformIDCodeMap.put(platformID, "end_point");
			PlatformCodeIDMap.put("end_point", platformID);
			
			/**
			 * ==========================================================
			 * Truck (vehicle) info
			 * ==========================================================
			 */	
			JSONArray truckTypeDtoList = (JSONArray) algorithmBaseParamDto.get("truckTypeDtoList");
			TRUCKTYPE_NUM = truckTypeDtoList.size();
			for (int basic_truct = 0; basic_truct < TRUCKTYPE_NUM; basic_truct++) {
				Carriage truck = new Carriage();
				JSONObject curr_truck = (JSONObject) truckTypeDtoList.get(basic_truct);
	
				truck.setCapacity((double) curr_truck.get("maxLoad"));
				truck.setHeight((double) curr_truck.get("height"));
				truck.setLength((double) curr_truck.get("length"));
				truck.setWidth((double) curr_truck.get("width"));
				truck.setTruckId(basic_truct);
				truck.setTruckTypeId((String) curr_truck.get("truckTypeId"));
				truck.setTruckTypeCode((String) curr_truck.get("truckTypeCode"));
				BASIC_TRUCKS.add(truck);
			}
			
			/**
			 * ==========================================================
			 *Distance map info
			 * ==========================================================
			 */	
	
			JSONObject distanceMapJSON = (JSONObject) algorithmBaseParamDto.get("distanceMap");
			for (int clienti = 1; clienti <= CLIENT_NUM; clienti++) {
				for (int clientj = 1; clientj <= CLIENT_NUM; clientj++) {
					if (clienti != clientj) {
						// 
						String twoplatforms = PlatformIDCodeMap.get(clienti) + '+' + PlatformIDCodeMap.get(clientj);
						// 
						distanceMap.put(String.valueOf(clienti) + '+' + String.valueOf(clientj),
								(Double) distanceMapJSON.get(twoplatforms));
					} else {
						//
						distanceMap.put(String.valueOf(clienti) + '+' + String.valueOf(clientj), 0.0);
					}
				}
			}
			// 
			for (int clienti = 1; clienti <= CLIENT_NUM; clienti++) {
				String twoplatforms = PlatformIDCodeMap.get(0) + '+' + PlatformIDCodeMap.get(clienti);
				distanceMap.put(String.valueOf(0) + '+' + String.valueOf(clienti),
						(Double) distanceMapJSON.get(twoplatforms));
			}

			// 
			for (int clienti = 1; clienti <= CLIENT_NUM; clienti++) {
				String twoplatforms = PlatformIDCodeMap.get(clienti) + '+' + PlatformIDCodeMap.get(CLIENT_NUM + 1);
				distanceMap.put(String.valueOf(clienti) + '+' + String.valueOf(CLIENT_NUM + 1),
						(Double) distanceMapJSON.get(twoplatforms));
			}
	
			/**
			 * ==========================================================
			 * Client (customer) info
			 * ==========================================================
			 */	
			
			depot_start.setPlatformID(0);// start no. is always 0
			depot_start.setDemands(0);// start demands is always 0
			depot_start.setGoodsNum(0);// start goodsNum is always 0
			depot_start.setGoods(new ArrayList<Box>());//
			depot_start.setMustFirst(false);
	
			depot_end.setPlatformID(CLIENT_NUM + 1);//
			depot_end.setDemands(0);
			depot_end.setGoodsNum(0);
			depot_end.setGoods(new ArrayList<Box>());
			depot_end.setMustFirst(false);
	
			// 
			for (int i = 1; i <= CLIENT_NUM; i++) {
				Node client = new Node();
				ArrayList<Box> boxes = new ArrayList<Box>();
				int platform = i;
				client.setPlatformID(platform);
				client.setDemands(0);// demands==0,the client's demands are boxes
				client.setGoods(boxes);
				client.setGoodsNum(0);// goods num
				client.setLoadgoodsNum(0);// ]
				clients.add(client);
				
				Node client_trans = new Node();
				ArrayList<Box> boxes_trans = new ArrayList<Box>();
				int platform_trans = i;
				client_trans.setPlatformID(platform_trans);
				client_trans.setDemands(0);//demands==0,the client's demands are boxes
				client_trans.setGoods(boxes_trans);
				client_trans.setGoodsNum(0);//goods num
				client_trans.setLoadgoodsNum(0);
				clients_trans.add(client_trans);
				
				Node client_half_trans = new Node();
				ArrayList<Box> boxes_half_trans = new ArrayList<Box>();
				int platform_half_trans = i;
				client_half_trans.setPlatformID(platform_half_trans);//
				client_half_trans.setDemands(0);//demands==0,the client's demands are boxes
				client_half_trans.setGoods(boxes_half_trans);
				client_half_trans.setGoodsNum(0);//goods num
				client_half_trans.setLoadgoodsNum(0);//
				clients_half_trans.add(client_half_trans);
			}
			//
			
			/**
			 * ==========================================================
			 * Boxes (cargoes) info
			 * ==========================================================
			 */	
			
			JSONArray boxesJSONArray = (JSONArray) obj.get("boxes");
			iterator = boxesJSONArray.iterator();
			int if_half = 0;
			while (iterator.hasNext()) {
				JSONObject currBoxJSON = iterator.next();
				String platformCode = (String) currBoxJSON.get("platformCode");
				//
				platformID = PlatformCodeIDMap.get(platformCode);
				Box box = new Box();
				box.setSpuBoxID((String) currBoxJSON.get("spuBoxId"));// spuBoxId,specific unique box id
				box.setPlatformid(platformID);
				box.setHeight((double) currBoxJSON.get("height"));// height
				double width = (double) currBoxJSON.get("width");
				double length = (double) currBoxJSON.get("length");
				box.setWidth(width);// width
				box.setLength(length);// length
				box.setWeight((double) currBoxJSON.get("weight"));// fragile or not
				box.setXCoor(0.0);
				box.setYCoor(0.0);
				box.setZCoor(0.0);
				
				Box box_trans = new Box();
				box_trans.setSpuBoxID((String)currBoxJSON.get("spuBoxId"));//spuBoxId,specific unique box id
				box_trans.setPlatformid(platformID);
				box_trans.setHeight((double)currBoxJSON.get("height"));//height
				double width_trans = (double)currBoxJSON.get("length");
				double length_trans = (double)currBoxJSON.get("width");
				if (length > 2318) {
					width_trans = (double)currBoxJSON.get("width");
					length_trans = (double)currBoxJSON.get("length");
				}
	
				box_trans.setWidth(width_trans);//width
				box_trans.setLength(length_trans);//length
				box_trans.setWeight((double)currBoxJSON.get("weight"));//fragile or not
				box_trans.setXCoor(0.0);
				box_trans.setYCoor(0.0);
				box_trans.setZCoor(0.0);
				
				Box box_half_trans = new Box();
				box_half_trans.setSpuBoxID((String)currBoxJSON.get("spuBoxId"));//spuBoxId,specific unique box id
				box_half_trans.setPlatformid(platformID);
				box_half_trans.setHeight((double)currBoxJSON.get("height"));//height
				double width_half_trans = (double)currBoxJSON.get("length");
				double length_half_trans = (double)currBoxJSON.get("width");
				if (length > 2318 || if_half%2==0) {
					width_half_trans = (double)currBoxJSON.get("width");
					length_half_trans = (double)currBoxJSON.get("length");
				}
				if_half += 1;
	
				box_half_trans.setWidth(width_half_trans);//width
				box_half_trans.setLength(length_half_trans);//length
				box_half_trans.setWeight((double)currBoxJSON.get("weight"));//fragile or not
				box_half_trans.setXCoor(0.0);
				box_half_trans.setYCoor(0.0);
				box_half_trans.setZCoor(0.0);
				// if(width>length) {
				// box.setDirection(200);
				//// System.out.println();
				//// System.out.println(filenames[fileidx]);
				// }else {
				box.setDirection(100);
				box_trans.setDirection(100);
				box_half_trans.setDirection(100);
				// }
				BOX_NUM += 1;
				// 
				clients.get(platformID - 1).getGoods().add(box);
				clients_trans.get(platformID-1).getGoods().add(box_trans);
				clients_half_trans.get(platformID-1).getGoods().add(box_half_trans);
				//
				client_volume_weight[platformID-1][0] = client_volume_weight[platformID-1][0] + box.getWidth()*box.getLength()*box.getHeight();
				client_volume_weight[platformID-1][1] = client_volume_weight[platformID-1][1] + box.getWeight();
			}
			//
			for (int i = 0; i < CLIENT_NUM; i++) {
				clients.get(i).setGoodsNum(clients.get(i).getGoods().size());// goods num
				clients.get(i).setMustFirst(mustFirst[i]);
				clients_trans.get(i).setGoodsNum(clients_trans.get(i).getGoods().size());//goods num
				clients_trans.get(i).setMustFirst(mustFirst[i]);
				clients_half_trans.get(i).setGoodsNum(clients_half_trans.get(i).getGoods().size());//goods num
				clients_half_trans.get(i).setMustFirst(mustFirst[i]);
			}
	
	
			for (int i = 0; i < CLIENT_NUM; i++) {
				double weight_sum = 0.0;
				double volumn_sum = 0.0;
				for (Box b : clients.get(i).getGoods()) {
					weight_sum = weight_sum + b.getWeight();
					volumn_sum = volumn_sum + b.getVolume();
				}
				clients.get(i).setGoodsWeight(weight_sum);
				clients.get(i).setGoodsVolumn(volumn_sum);
			}
	
			BASIC_TRUCKS.sort(new Comparator<Carriage>() {
				public int compare(Carriage c1, Carriage c2) {
					double d1 = c1.getTruckVolume();
					double d2 = c2.getTruckVolume();
					if (d1 < d2)
						return -1;
					else if (d1 > d2)
						return 1;
					else
						return 0;
				}
			});
			
			//to get clients_v, clients_trans_v, clients_half_trans_v
			for(int i=0;i<CLIENT_NUM;i++) {
				clients_v.add(new Node(clients.get(i)));
				clients_trans_v.add(new Node(clients_trans.get(i)));
				clients_half_trans_v.add(new Node(clients_half_trans.get(i)));
			}
			
			/**
			 * ==========================================================
			 * Sort client list
			 * ==========================================================
			 */	
			
			//begin sort boxes of all kinds of clients
			for(int i=0;i<CLIENT_NUM;i++) {
				clients.get(i).getGoods().sort(new Comparator<Box>() {
					public int compare(Box b1, Box b2) {
						double volume1=b1.getLength()*b1.getWidth();
						double volume2=b2.getLength()*b2.getWidth();
						if(volume1>volume2)
							return -1;
						else if (volume1<volume2)
							return 1;
						else
							if(b1.getHeight()>b2.getHeight())
								return -1;
							else if(b1.getHeight()<b2.getHeight())
								return 1;
							else
								return 0;
					}
					
		
				});
			}
			for(int i=0;i<CLIENT_NUM;i++) {
				clients_trans.get(i).getGoods().sort(new Comparator<Box>() {
					public int compare(Box b1, Box b2) {
						double volume1=b1.getLength()*b1.getWidth();
						double volume2=b2.getLength()*b2.getWidth();
						if(volume1>volume2)
							return -1;
						else if (volume1<volume2)
							return 1;
						else
							if(b1.getHeight()>b2.getHeight())
								return -1;
							else if(b1.getHeight()<b2.getHeight())
								return 1;
							else
								return 0;
					}
				});
			}
			for(int i=0;i<CLIENT_NUM;i++) {
				clients_half_trans.get(i).getGoods().sort(new Comparator<Box>() {
					public int compare(Box b1, Box b2) {
						double volume1=b1.getLength()*b1.getWidth();
						double volume2=b2.getLength()*b2.getWidth();
						if(volume1>volume2)
							return -1;
						else if (volume1<volume2)
							return 1;
						else
							if(b1.getHeight()>b2.getHeight())
								return -1;
							else if(b1.getHeight()<b2.getHeight())
								return 1;
							else
								return 0;
					}
	
					
				});
			}
			for(int i=0;i<CLIENT_NUM;i++) {
				clients_v.get(i).getGoods().sort(new Comparator<Box>() {
					public int compare(Box b1, Box b2) {
						double volume1=b1.getVolume();
						double volume2=b2.getVolume();
						if(volume1>volume2)
							return -1;
						else if (volume1<volume2)
							return 1;
						else
							if(b1.getHeight()>b2.getHeight())
								return -1;
							else if(b1.getHeight()<b2.getHeight())
								return 1;
							else
								return 0;
					}
				});
			}
			for(int i=0;i<CLIENT_NUM;i++) {
				clients_trans_v.get(i).getGoods().sort(new Comparator<Box>() {
					public int compare(Box b1, Box b2) {
						double volume1=b1.getVolume();
						double volume2=b2.getVolume();
						if(volume1>volume2)
							return -1;
						else if (volume1<volume2)
							return 1;
						else
							if(b1.getHeight()>b2.getHeight())
								return -1;
							else if(b1.getHeight()<b2.getHeight())
								return 1;
							else
								return 0;
					}
				});
			}
			for(int i=0;i<CLIENT_NUM;i++) {
				clients_half_trans_v.get(i).getGoods().sort(new Comparator<Box>() {
					public int compare(Box b1, Box b2) {
						double volume1=b1.getVolume();
						double volume2=b2.getVolume();
						if(volume1>volume2)
							return -1;
						else if (volume1<volume2)
							return 1;
						else
							if(b1.getHeight()>b2.getHeight())
								return -1;
							else if(b1.getHeight()<b2.getHeight())
								return 1;
							else
								return 0;
					}
				});
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		}


	/**
	 * ==========================================================
	 * Read the data of SDCSS format
	 * 
	 * The instances are from reference paper "Local search techniques for a routing-packing problem"
	 * 
	 * ==========================================================
	 * @throws FileNotFoundException 
	 */	
	
	public void ReadSDCSS() throws FileNotFoundException {
		
        File file = new File(filename);
        Scanner scanner = new Scanner(file);
        scanner.nextLine();
        CLIENT_NUM = scanner.nextInt();
        scanner.nextLine();
        TRUCKTYPE_NUM = scanner.nextInt();
        scanner.nextLine();
        BOXTYPE_NUM = scanner.nextInt();
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
        
        BOX_NUM=0;
        
        
		client_volume_weight = new double[CLIENT_NUM][2];
		for(int i=0;i<CLIENT_NUM;i++) {
			client_volume_weight[i][0] = 0.0;
			client_volume_weight[i][1] = 0.0;
		}
		
		/**
		 * ==========================================================
		 * platformID to code info
		 * ==========================================================
		 */	
		
		
		int platformID = 0;
		PlatformIDCodeMap.put(platformID, "start_point");
		PlatformCodeIDMap.put("start_point", platformID);
		platformID++;
		for (int i =1;i<=CLIENT_NUM ;i++) {
			String platformCode = String.valueOf(i);
			PlatformIDCodeMap.put(platformID, platformCode);
			PlatformCodeIDMap.put(platformCode, platformID);
			platformID++;
		}
		PlatformIDCodeMap.put(platformID, "end_point");
		PlatformCodeIDMap.put("end_point", platformID);
		
		/**
		 * ==========================================================
		 * Truck (vehicle) info
		 * ==========================================================
		 */	

		for (int basic_truct = 0; basic_truct < TRUCKTYPE_NUM; basic_truct++) {
			Carriage truck = new Carriage();
			truck.setTruckTypeId((String) scanner.next());
			truck.setQuantity((int) scanner.nextInt() );
			truck.setCapacity((double) scanner.nextInt() );
			truck.setHeight((double) scanner.nextInt()*10 );
			truck.setWidth((double) scanner.nextInt()*10 );
			truck.setLength((double) scanner.nextInt()*10 );
			
//			truck.setTruckId(basic_truct);
//			truck.setTruckTypeId((String) curr_truck.get("truckTypeId"));
//			truck.setTruckTypeCode((String) curr_truck.get("truckTypeCode"));
			BASIC_TRUCKS.add(truck);
		}
		
		/**
		 * ==========================================================
		 * Box type
		 * ==========================================================
		 */	
		
		scanner.nextLine() ;
		scanner.nextLine() ;
		scanner.nextLine() ;
		
		ArrayList<Box> boxList = new ArrayList<Box>();
		for (int box_type = 0; box_type < BOXTYPE_NUM; box_type++) {
			Box box = new Box();
			String test = scanner.next();
			box.setSpuBoxID((String) test);// spuBoxId,specific unique box id
			//box.setPlatformid(platformID);
			scanner.nextInt(); // quantity not used
			box.setLength((double) scanner.nextDouble()*10);
			box.setWidth((double) scanner.nextDouble()*10);
			box.setHeight((double) scanner.nextDouble()*10);
			box.setFragile((double) scanner.nextDouble());
			box.setWeight((double) scanner.nextDouble());// fragile or not
			box.setXCoor(0.0);
			box.setYCoor(0.0);
			box.setZCoor(0.0);
			box.setDirection(100);
			boxList.add(box);
		}
		
		
		
		/**
		 * ==========================================================
		 *Distance map info
		 * ==========================================================
		 */	
		
		scanner.nextLine() ;
		scanner.nextLine() ;
		scanner.nextLine() ;
		
		
		double[][] clientCoordinate = new double[CLIENT_NUM+1][2];
		double[] clientDemand = new double[CLIENT_NUM+1];
		for (int clienti = 0; clienti <= CLIENT_NUM; clienti++) { 
			scanner.nextInt() ; // read id not used
			clientCoordinate[clienti][0] = scanner.nextInt() ;
			clientCoordinate[clienti][1] = scanner.nextInt() ;
			clientDemand[clienti] = scanner.nextInt() ;
		}

		
		for (int clienti = 1; clienti <= CLIENT_NUM; clienti++) {
			for (int clientj = 1; clientj <= CLIENT_NUM; clientj++) {
				if (clienti != clientj) {
					double dis = Math.sqrt(Math.pow((clientCoordinate[clienti][0]-clientCoordinate[clientj][0]),2)
							+Math.pow((clientCoordinate[clienti][1]-clientCoordinate[clientj][1]),2));
					distanceMap.put(String.valueOf(clienti) + '+' + String.valueOf(clientj),
							(Double) dis);
				} else {
					//
					distanceMap.put(String.valueOf(clienti) + '+' + String.valueOf(clientj), 0.0);
				}
			}
		}
		// 
		for (int clienti = 1; clienti <= CLIENT_NUM; clienti++) {
			double dis = Math.sqrt(Math.pow((clientCoordinate[clienti][0]-clientCoordinate[0][0]),2)
					+Math.pow((clientCoordinate[clienti][1]-clientCoordinate[0][1]),2));
			distanceMap.put(String.valueOf(0) + '+' + String.valueOf(clienti),
					(Double) dis);
		}
		// 
		for (int clienti = 1; clienti <= CLIENT_NUM; clienti++) {
			double dis = Math.sqrt(Math.pow((clientCoordinate[clienti][0]-clientCoordinate[0][0]),2)
					+Math.pow((clientCoordinate[clienti][1]-clientCoordinate[0][1]),2));
			distanceMap.put(String.valueOf(clienti) + '+' + String.valueOf(CLIENT_NUM + 1),
					(Double) dis);
		}

		/**
		 * ==========================================================
		 * Client (customer) info
		 * ==========================================================
		 */	
		
		depot_start.setPlatformID(0);// start no. is always 0
		depot_start.setDemands(0);// start demands is always 0
		depot_start.setGoodsNum(0);// start goodsNum is always 0
		depot_start.setGoods(new ArrayList<Box>());//
		depot_start.setMustFirst(false);

		depot_end.setPlatformID(CLIENT_NUM + 1);//
		depot_end.setDemands(0);
		depot_end.setGoodsNum(0);
		depot_end.setGoods(new ArrayList<Box>());
		depot_end.setMustFirst(false);

		// 
		for (int i = 1; i <= CLIENT_NUM; i++) {
			Node client = new Node();
			ArrayList<Box> boxes = new ArrayList<Box>();
			int platform = i;
			client.setPlatformID(platform);
			client.setDemands(0);// demands==0,the client's demands are boxes
			client.setGoods(boxes);
			client.setGoodsNum(0);// goods num
			client.setLoadgoodsNum(0);// ]
			clients.add(client);
			
			Node client_trans = new Node();
			ArrayList<Box> boxes_trans = new ArrayList<Box>();
			int platform_trans = i;
			client_trans.setPlatformID(platform_trans);
			client_trans.setDemands(0);//demands==0,the client's demands are boxes
			client_trans.setGoods(boxes_trans);
			client_trans.setGoodsNum(0);//goods num
			client_trans.setLoadgoodsNum(0);
			clients_trans.add(client_trans);
			
			Node client_half_trans = new Node();
			ArrayList<Box> boxes_half_trans = new ArrayList<Box>();
			int platform_half_trans = i;
			client_half_trans.setPlatformID(platform_half_trans);//
			client_half_trans.setDemands(0);//demands==0,the client's demands are boxes
			client_half_trans.setGoods(boxes_half_trans);
			client_half_trans.setGoodsNum(0);//goods num
			client_half_trans.setLoadgoodsNum(0);//
			clients_half_trans.add(client_half_trans);
		}
		//
		
		/**
		 * ==========================================================
		 * Boxes (cargoes) info
		 * ==========================================================
		 */	
		scanner.nextLine() ;
		scanner.nextLine() ;
		scanner.nextLine() ;
		
		scanner.nextLine() ; // client 0 no box
		int if_half = 0;
		for (int i = 1; i <= CLIENT_NUM; i++) {
			
			scanner.nextInt() ;
			int nbox = scanner.nextInt() ;
			
			for(int nb = 1; nb<=nbox;nb++) {	
				String boxID = scanner.next();
				String[] idString = boxID.split("t");
				int id = Integer.parseInt(idString[1]);
				
				int n = scanner.nextInt();	
				
				for (int j=1;j<=n;j++) {
					Box boxcopy = boxList.get(id);
					
					Box box = new Box();
					box.setSpuBoxID((String) boxcopy.getSpuBoxID());// spuBoxId,specific unique box id
					//box.setPlatformid(platformID);
					//scanner.nextDouble(); // quantity not used
					box.setLength((double) boxcopy.getLength());
					box.setWidth((double) boxcopy.getWidth());
					box.setHeight((double) boxcopy.getHeight());
					box.setFragile((double) boxcopy.getFragile());
					box.setWeight((double) boxcopy.getWeight());// fragile or not
					box.setXCoor(0.0);
					box.setYCoor(0.0);
					box.setZCoor(0.0);
					box.setDirection(100);
					box.setPlatformid(i);
					
					double width_trans = (double)boxcopy.getLength();
					double length_trans = (double)boxcopy.getWidth();
					
					Box box_trans = new Box();
					box_trans.setSpuBoxID((String) boxcopy.getSpuBoxID());// spuBoxId,specific unique box id
					//box.setPlatformid(platformID);
					//scanner.nextDouble(); // quantity not used
					box_trans.setLength((double) length_trans);
					box_trans.setWidth((double) width_trans);
					box_trans.setHeight((double) boxcopy.getHeight());
					box_trans.setFragile((double) boxcopy.getFragile());
					box_trans.setWeight((double) boxcopy.getWeight());// fragile or not
					box_trans.setXCoor(0.0);
					box_trans.setYCoor(0.0);
					box_trans.setZCoor(0.0);
					box_trans.setDirection(100);
					box_trans.setPlatformid(i);
					
					Box box_half_trans = new Box();
					box_half_trans.setSpuBoxID((String) boxcopy.getSpuBoxID());// spuBoxId,specific unique box id
					//box.setPlatformid(platformID);
					//scanner.nextDouble(); // quantity not used
					if (if_half%2==0) {
						box_half_trans.setLength((double) length_trans);
						box_half_trans.setWidth((double) width_trans);
					}
					else {
						box_half_trans.setLength((double) boxcopy.getLength());
						box_half_trans.setWidth((double) boxcopy.getWidth());
					}

					box_half_trans.setHeight((double) boxcopy.getHeight());
					box_half_trans.setFragile((double) boxcopy.getFragile());
					box_half_trans.setWeight((double) boxcopy.getWeight());// fragile or not
					box_half_trans.setXCoor(0.0);
					box_half_trans.setYCoor(0.0);
					box_half_trans.setZCoor(0.0);
					box_half_trans.setDirection(100);
					box_half_trans.setPlatformid(i);
					
					if_half+=1;
					BOX_NUM +=1;
					
					
					clients.get(i-1).getGoods().add(box);
					clients_trans.get(i-1).getGoods().add(box_trans);
					clients_half_trans.get(i-1).getGoods().add(box_half_trans);
					
					client_volume_weight[i-1][0] = client_volume_weight[i-1][0] + box.getWidth()*box.getLength()*box.getHeight();
					client_volume_weight[i-1][1] = client_volume_weight[i-1][1] + box.getWeight();
				}
				
								
			}
			scanner.nextLine() ;
			
		
		}
		

		//
		for (int i = 0; i < CLIENT_NUM; i++) {
			clients.get(i).setGoodsNum(clients.get(i).getGoods().size());// goods num
			clients.get(i).setMustFirst(false);
			clients_trans.get(i).setGoodsNum(clients_trans.get(i).getGoods().size());//goods num
			clients_trans.get(i).setMustFirst(false);
			clients_half_trans.get(i).setGoodsNum(clients_half_trans.get(i).getGoods().size());//goods num
			clients_half_trans.get(i).setMustFirst(false);
		}


		for (int i = 0; i < CLIENT_NUM; i++) {
			double weight_sum = 0.0;
			double volumn_sum = 0.0;
			for (Box b : clients.get(i).getGoods()) {
				weight_sum = weight_sum + b.getWeight();
				volumn_sum = volumn_sum + b.getVolume();
			}
			clients.get(i).setGoodsWeight(weight_sum);
			clients.get(i).setGoodsVolumn(volumn_sum);
		}

		BASIC_TRUCKS.sort(new Comparator<Carriage>() {
			public int compare(Carriage c1, Carriage c2) {
				double d1 = c1.getTruckVolume();
				double d2 = c2.getTruckVolume();
				if (d1 < d2)
					return -1;
				else if (d1 > d2)
					return 1;
				else
					return 0;
			}
		});
		
		//to get clients_v, clients_trans_v, clients_half_trans_v
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_v.add(new Node(clients.get(i)));
			clients_trans_v.add(new Node(clients_trans.get(i)));
			clients_half_trans_v.add(new Node(clients_half_trans.get(i)));
		}
		
		/**
		 * ==========================================================
		 * Sort client list
		 * ==========================================================
		 */	
		
		//begin sort boxes of all kinds of clients
		for(int i=0;i<CLIENT_NUM;i++) {
			clients.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getLength()*b1.getWidth();
					double volume2=b2.getLength()*b2.getWidth();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}
				
	
			});
		}
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_trans.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getLength()*b1.getWidth();
					double volume2=b2.getLength()*b2.getWidth();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}
			});
		}
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_half_trans.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getLength()*b1.getWidth();
					double volume2=b2.getLength()*b2.getWidth();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}

				
			});
		}
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_v.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getVolume();
					double volume2=b2.getVolume();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}
			});
		}
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_trans_v.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getVolume();
					double volume2=b2.getVolume();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}
			});
		}
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_half_trans_v.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getVolume();
					double volume2=b2.getVolume();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}
			});
		}
		
	}
	
	/**
	 * ==========================================================
	 * Read the data of Shanghai instances format
	 * 
	 * The instances are from reference paper "The Split Delivery Vehicle Routing Problem with three-dimensional loading constraints"
	 * 
	 * ==========================================================
	 * @throws FileNotFoundException 
	 */	
	
	public void ReadSH() throws FileNotFoundException {
		
		
		
        File file = new File(filename);
        Scanner scanner = new Scanner(file);
        
        
        
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
        CLIENT_NUM = scanner.nextInt();
        int vehicle_number  = scanner.nextInt(); // not used
        BOXTYPE_NUM = scanner.nextInt();
        BOX_NUM  = scanner.nextInt();
        TRUCKTYPE_NUM = 1; // unlimited homogeneous fleet
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();

		client_volume_weight = new double[CLIENT_NUM][2];
		for(int i=0;i<CLIENT_NUM;i++) {
			client_volume_weight[i][0] = 0.0;
			client_volume_weight[i][1] = 0.0;
		}
		
		/**
		 * ==========================================================
		 * platformID to code info
		 * ==========================================================
		 */	
			
		int platformID = 0;
		PlatformIDCodeMap.put(platformID, "start_point");
		PlatformCodeIDMap.put("start_point", platformID);
		platformID++;
		for (int i =1;i<=CLIENT_NUM ;i++) {
			String platformCode = String.valueOf(i);
			PlatformIDCodeMap.put(platformID, platformCode);
			PlatformCodeIDMap.put(platformCode, platformID);
			platformID++;
		}
		PlatformIDCodeMap.put(platformID, "end_point");
		PlatformCodeIDMap.put("end_point", platformID);
		
		/**
		 * ==========================================================
		 * Truck (vehicle) info
		 * ==========================================================
		 */	

		for (int basic_truct = 0; basic_truct < TRUCKTYPE_NUM; basic_truct++) {
			Carriage truck = new Carriage();
//			truck.setTruckTypeId((String) scanner.next());
//			truck.setQuantity((int) scanner.nextInt() );
			truck.setCapacity((double) scanner.nextInt() );
			truck.setHeight((double) scanner.nextInt()*10 );
			truck.setWidth((double) scanner.nextInt()*10 );
			truck.setLength((double) scanner.nextInt()*10 );
			
//			truck.setTruckId(basic_truct);
//			truck.setTruckTypeId((String) curr_truck.get("truckTypeId"));
//			truck.setTruckTypeCode((String) curr_truck.get("truckTypeCode"));
			BASIC_TRUCKS.add(truck);
		}
		
		/**
		 * ==========================================================
		 * Box type
		 * ==========================================================
		 */	
		
//		scanner.nextLine() ;
//		scanner.nextLine() ;
//		scanner.nextLine() ;
//		
//		ArrayList<Box> boxList = new ArrayList<Box>();
//		for (int box_type = 0; box_type < BOXTYPE_NUM; box_type++) {
//			Box box = new Box();
//			String test = scanner.next();
//			box.setSpuBoxID((String) test);// spuBoxId,specific unique box id
//			//box.setPlatformid(platformID);
//			scanner.nextInt(); // quantity not used
//			box.setLength((double) scanner.nextDouble()*10);
//			box.setWidth((double) scanner.nextDouble()*10);
//			box.setHeight((double) scanner.nextDouble()*10);
//			box.setFragile((double) scanner.nextDouble());
//			box.setWeight((double) scanner.nextDouble());// fragile or not
//			box.setXCoor(0.0);
//			box.setYCoor(0.0);
//			box.setZCoor(0.0);
//			box.setDirection(100);
//			boxList.add(box);
//		}
		
		
		

		/**
		 * ==========================================================
		 * Client (customer) info
		 * ==========================================================
		 */	
		
		depot_start.setPlatformID(0);// start no. is always 0
		depot_start.setDemands(0);// start demands is always 0
		depot_start.setGoodsNum(0);// start goodsNum is always 0
		depot_start.setGoods(new ArrayList<Box>());//
		depot_start.setMustFirst(false);

		depot_end.setPlatformID(CLIENT_NUM + 1);//
		depot_end.setDemands(0);
		depot_end.setGoodsNum(0);
		depot_end.setGoods(new ArrayList<Box>());
		depot_end.setMustFirst(false);

		// 
		for (int i = 1; i <= CLIENT_NUM; i++) {
			Node client = new Node();
			ArrayList<Box> boxes = new ArrayList<Box>();
			int platform = i;
			client.setPlatformID(platform);
			client.setDemands(0);// demands==0,the client's demands are boxes
			client.setGoods(boxes);
			client.setGoodsNum(0);// goods num
			client.setLoadgoodsNum(0);// ]
			clients.add(client);
			
			Node client_trans = new Node();
			ArrayList<Box> boxes_trans = new ArrayList<Box>();
			int platform_trans = i;
			client_trans.setPlatformID(platform_trans);
			client_trans.setDemands(0);//demands==0,the client's demands are boxes
			client_trans.setGoods(boxes_trans);
			client_trans.setGoodsNum(0);//goods num
			client_trans.setLoadgoodsNum(0);
			clients_trans.add(client_trans);
			
			Node client_half_trans = new Node();
			ArrayList<Box> boxes_half_trans = new ArrayList<Box>();
			int platform_half_trans = i;
			client_half_trans.setPlatformID(platform_half_trans);//
			client_half_trans.setDemands(0);//demands==0,the client's demands are boxes
			client_half_trans.setGoods(boxes_half_trans);
			client_half_trans.setGoodsNum(0);//goods num
			client_half_trans.setLoadgoodsNum(0);//
			clients_half_trans.add(client_half_trans);
		}
		//
		
		/**
		 * ==========================================================
		 * Boxes (cargoes) info
		 * ==========================================================
		 */	
		scanner.nextLine() ;
		scanner.nextLine() ;
		scanner.nextLine() ;// client 0 no box
		
		int nitem = 0;
		
		int if_half = 0;
		for (int i = 1; i <= CLIENT_NUM; i++) {
			
			scanner.nextInt() ;
			int nboxt = scanner.nextInt() ;
			
			for(int nbt = 1; nbt<=nboxt;nbt++) {	
			
				double height = scanner.nextInt()*10;
				double width = scanner.nextInt()*10;
				double length = scanner.nextInt()*10;
				
				int nbox = scanner.nextInt();
				
				int fragile = scanner.nextInt();
				int weight = scanner.nextInt();
				
				for (int nb=1;nb<=nbox;nb++)
				{
					nitem+=1;
					Box box = new Box();
					//box.setSpuBoxID((String) boxcopy.getSpuBoxID());// spuBoxId,specific unique box id
					//box.setPlatformid(platformID);
					//scanner.nextDouble(); // quantity not used
					box.setLength((double) length);
					box.setWidth((double)  width);
					box.setHeight((double) height);
					box.setFragile((double) fragile);
					box.setWeight((double) weight);// fragile or not
					box.setXCoor(0.0);
					box.setYCoor(0.0);
					box.setZCoor(0.0);
					box.setDirection(100);
					box.setPlatformid(i);
					
					double width_trans = (double)length;
					double length_trans = (double)width;
					
					Box box_trans = new Box();
					//box_trans.setSpuBoxID((String) boxcopy.getSpuBoxID());// spuBoxId,specific unique box id
					//box.setPlatformid(platformID);
					//scanner.nextDouble(); // quantity not used
					box_trans.setLength((double) length_trans);
					box_trans.setWidth((double) width_trans);
					box_trans.setHeight((double) height);
					box_trans.setFragile((double) fragile);
					box_trans.setWeight((double) weight);// fragile or not
					box_trans.setXCoor(0.0);
					box_trans.setYCoor(0.0);
					box_trans.setZCoor(0.0);
					box_trans.setDirection(100);
					box_trans.setPlatformid(i);
					
					Box box_half_trans = new Box();
					//box_half_trans.setSpuBoxID((String) boxcopy.getSpuBoxID());// spuBoxId,specific unique box id
					//box.setPlatformid(platformID);
					//scanner.nextDouble(); // quantity not used
					if (if_half%2==0) {
						box_half_trans.setLength((double) length_trans);
						box_half_trans.setWidth((double) width_trans);
					}
					else {
						box_half_trans.setLength((double) length);
						box_half_trans.setWidth((double)width);
					}

					box_half_trans.setHeight((double) height);
					box_half_trans.setFragile((double) fragile);
					box_half_trans.setWeight((double)weight);// fragile or not
					box_half_trans.setXCoor(0.0);
					box_half_trans.setYCoor(0.0);
					box_half_trans.setZCoor(0.0);
					box_half_trans.setDirection(100);
					box_half_trans.setPlatformid(i);
					
					if_half+=1;
					
					
					clients.get(i-1).getGoods().add(box);
					clients_trans.get(i-1).getGoods().add(box_trans);
					clients_half_trans.get(i-1).getGoods().add(box_half_trans);
					
					client_volume_weight[i-1][0] = client_volume_weight[i-1][0] + box.getWidth()*box.getLength()*box.getHeight();
					client_volume_weight[i-1][1] = client_volume_weight[i-1][1] + box.getWeight();
				}
								
			}
			scanner.nextLine() ;
			
		
		}

		
		/**
		 * ==========================================================
		 *Distance map info
		 * ==========================================================
		 */	
		
		scanner.nextLine() ;


		
		for (int clienti = 0; clienti < CLIENT_NUM; clienti++) {
			for (int clientj = clienti+1; clientj <= CLIENT_NUM; clientj++) {
					scanner.nextInt();
					scanner.nextInt();
					double dis = scanner.nextDouble();
					distanceMap.put(String.valueOf(clienti) + '+' + String.valueOf(clientj),
							(Double) dis);
					if(scanner.hasNextLine()) 
						scanner.nextLine();
					
			}
		}
			
		for (int clientj = 0; clientj <= CLIENT_NUM; clientj++) {
			for (int clienti = clientj; clienti <= CLIENT_NUM; clienti++) {
				
			if (clienti==clientj) {
				distanceMap.put(String.valueOf(clienti) + '+' + String.valueOf(clientj),
						(Double) 0.0);
			}
			else {

				distanceMap.put(String.valueOf(clienti) + '+' + String.valueOf(clientj),
						(Double) distanceMap.get(String.valueOf(clientj) + '+' + String.valueOf(clienti)));			
			}
			}
		}
		
		for (int clienti = 0; clienti <= CLIENT_NUM+1; clienti++) {
			int  clientj = CLIENT_NUM+1;
			if (clienti==clientj) {
				distanceMap.put(String.valueOf(clienti) + '+' + String.valueOf(clientj),
						(Double) 0.0);
			}
			else {
				distanceMap.put(String.valueOf(clienti) + '+' + String.valueOf(clientj),
						(Double) distanceMap.get(String.valueOf(0) + '+' + String.valueOf(clienti)));	
				distanceMap.put(String.valueOf(clientj) + '+' + String.valueOf(clienti),
						(Double) distanceMap.get(String.valueOf(0) + '+' + String.valueOf(clienti)));	
			}
		}
			
		// 
		//
		for (int i = 0; i < CLIENT_NUM; i++) {
			clients.get(i).setGoodsNum(clients.get(i).getGoods().size());// goods num
			clients.get(i).setMustFirst(false);
			clients_trans.get(i).setGoodsNum(clients_trans.get(i).getGoods().size());//goods num
			clients_trans.get(i).setMustFirst(false);
			clients_half_trans.get(i).setGoodsNum(clients_half_trans.get(i).getGoods().size());//goods num
			clients_half_trans.get(i).setMustFirst(false);
		}


		for (int i = 0; i < CLIENT_NUM; i++) {
			double weight_sum = 0.0;
			double volumn_sum = 0.0;
			for (Box b : clients.get(i).getGoods()) {
				weight_sum = weight_sum + b.getWeight();
				volumn_sum = volumn_sum + b.getVolume();
			}
			clients.get(i).setGoodsWeight(weight_sum);
			clients.get(i).setGoodsVolumn(volumn_sum);
		}

		BASIC_TRUCKS.sort(new Comparator<Carriage>() {
			public int compare(Carriage c1, Carriage c2) {
				double d1 = c1.getTruckVolume();
				double d2 = c2.getTruckVolume();
				if (d1 < d2)
					return -1;
				else if (d1 > d2)
					return 1;
				else
					return 0;
			}
		});
		
		//to get clients_v, clients_trans_v, clients_half_trans_v
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_v.add(new Node(clients.get(i)));
			clients_trans_v.add(new Node(clients_trans.get(i)));
			clients_half_trans_v.add(new Node(clients_half_trans.get(i)));
		}
		
		/**
		 * ==========================================================
		 * Sort client list
		 * ==========================================================
		 */	
		
		//begin sort boxes of all kinds of clients
		for(int i=0;i<CLIENT_NUM;i++) {
			clients.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getLength()*b1.getWidth();
					double volume2=b2.getLength()*b2.getWidth();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}
				
	
			});
		}
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_trans.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getLength()*b1.getWidth();
					double volume2=b2.getLength()*b2.getWidth();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}
			});
		}
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_half_trans.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getLength()*b1.getWidth();
					double volume2=b2.getLength()*b2.getWidth();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}

				
			});
		}
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_v.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getVolume();
					double volume2=b2.getVolume();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}
			});
		}
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_trans_v.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getVolume();
					double volume2=b2.getVolume();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}
			});
		}
		for(int i=0;i<CLIENT_NUM;i++) {
			clients_half_trans_v.get(i).getGoods().sort(new Comparator<Box>() {
				public int compare(Box b1, Box b2) {
					double volume1=b1.getVolume();
					double volume2=b2.getVolume();
					if(volume1>volume2)
						return -1;
					else if (volume1<volume2)
						return 1;
					else
						if(b1.getHeight()>b2.getHeight())
							return -1;
						else if(b1.getHeight()<b2.getHeight())
							return 1;
						else
							return 0;
				}
			});
		}
		
	}
	
	public int get_CLIENT_NUM() {;
		return this.CLIENT_NUM;
	}
	
	public int get_BOX_NUM() {
		return this.BOX_NUM;
	}
	
	public int get_RUCKTYPE_NUM() {
		return this.TRUCKTYPE_NUM;
	}
	
	public HashMap<Integer, String> get_PlatformIDCodeMap(){
		return this.PlatformIDCodeMap;
	}
	
	public HashMap<String,Integer> get_PlatformCodeIDMap(){
		return this.PlatformCodeIDMap;
	}

	public ArrayList<Carriage> get_BASIC_TRUCKS(){
		return this.BASIC_TRUCKS;
	}
	
	public HashMap<String, Double> get_distanceMap(){
		return this.distanceMap;
	}
	
	public ArrayList<Node> get_clients(){
		return this.clients;
	}
	
	public ArrayList<Node> get_clients_trans(){
		return this.clients_trans;
	}
	
	public ArrayList<Node> get_clients_half_trans(){
		return this.clients_half_trans;
	}
	
	public ArrayList<Node> get_clients_v(){
		return this.clients_v;
	}
	
	public ArrayList<Node> get_clients_trans_v(){
		return this.clients_trans_v;
	}
	
	public ArrayList<Node> get_clients_half_trans_v(){
		return this.clients_half_trans_v;
	}
	
	public double[][] get_client_volume_weight(){
		return this.client_volume_weight;
	}
	
	public Node get_depot_start() {
		return this.depot_start;
	}
	
	public Node get_depot_end() {
		return this.depot_end;
	}

}

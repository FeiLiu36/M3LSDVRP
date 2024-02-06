package solver.packing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import solver.util.Box;
import solver.util.Box2D;
import solver.util.Carriage;
import solver.util.EP;
import solver.util.L;
import solver.utilPacking.BlockHeuristic;
import solver.utilPacking.SimpleBlock;
import solver.utilPacking.SimpleSpace;
import solver.utilPacking.Space;
import solver.utilPacking.Utils;

public class ConstructiveHeuristics {
	
	private boolean DEBUG = false;
	
	
	/************跟车箱有关的变量************/
	private ArrayList<Box> Boxes = new ArrayList<Box>();
	private ArrayList<Box> sortedBoxes = new ArrayList<Box>();//sort Boxes according to y and x
	/**
	 * 存放所有clients的box序列
	 */
//	private ArrayList<Box> allBoxes = new ArrayList<Box>();
	private ArrayList<Double> horizontal_levels = new ArrayList<Double>();
	private double loadWeight;
	private double loadVolumn;
//	private double excessWeight;
//	private double excessLength;//当前转载长度。
	
	
	/**
	 * dblf算法装箱,根据yx进行排序找3DCorners
	 * @param clients
	 * @return 装箱的box下标数组,装箱个数
	 */

	
		public ArrayList<Integer> dblf(final ArrayList<Box> boxingSequence, Carriage carriage, int method) {
			double width = carriage.getWidth();
			double length = carriage.getLength();
			double height = carriage.getHeight();
			double capacity = carriage.getCapacity();
			ArrayList<Box> thisBoxes = new ArrayList<Box>();//按顺序保存该箱子。
			ArrayList<Box> thissortedBox = new ArrayList<Box>();
			ArrayList<Double> horizontal_levels = new ArrayList<Double>();
			ArrayList<Integer> loadIdx=new ArrayList<Integer>();//保存装在这辆车里面的箱子集
			double thisloadWeight=0.0;//保存已经装的箱子的重量
			double thisloadVolumn=0.0;
			int iter=0;
			while(iter<1) {
			//boxingSequence是请求放在当前小车的箱子序列，每个平台的箱子从大到小排序。
			horizontal_levels = new ArrayList<Double>();
			horizontal_levels.add(0.0);
			thissortedBox = new ArrayList<Box>();//清空已经存放的boxes
			thisBoxes = new ArrayList<Box>();//按顺序保存该箱子。
			thisloadWeight=0.0;//保存已经装的箱子的重量
			thisloadVolumn=0.0;
			loadIdx=new ArrayList<Integer>();//保存装在这辆车里面的箱子集
			Iterator<Box> iteratorBox;
			boolean insertConfirm;//是否成功插入当前箱子。
			for(int boxi=0;boxi<boxingSequence.size();boxi++) {
				insertConfirm=false;
				Box curr_box = boxingSequence.get(boxi);
				if(thisloadWeight + curr_box.getWeight()>capacity) {
					break;//当前箱子不能再加入这辆车了，退出寻找最优位置，并且退出遍历箱子。
				}
				//第一步先求3DCorners=========================================================
				ArrayList<Box> Corners3D = new ArrayList<Box>();//如果已经存放的箱子是0，则原点。
				
//				if(sortedBox.size()<1) {
//					Corners3D.add(new Box());
//				} else {
//				int k=0;//遍历每个Z平面，和Z轴length垂直的平面。
				for(int k=0;k<horizontal_levels.size() && horizontal_levels.get(k)+curr_box.getLength()<=carriage.getLength();k++) {
					//得到在这个平面之上的已经存放的boxes,I_k
					ArrayList<Box> I_k = new ArrayList<Box>();
					iteratorBox = thissortedBox.iterator();
					while(iteratorBox.hasNext()) {
						Box currBox = iteratorBox.next();
						if(currBox.getZCoor()+currBox.getLength()>horizontal_levels.get(k)) {
							I_k.add(new Box(currBox));
						}
					}
					
					//求2DCorners==========================================================begin
					if(I_k.size()<1) {
						//如果这个平面之上没有box,添加原点。
						Box corner = new Box();
						corner.setXCoor(0.0);corner.setYCoor(0.0);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
						Corners3D.add(corner);
					}else{
						//Phase 1: identify the extreme items e_1,...,e_m
						ArrayList<Integer> e = new ArrayList<Integer>();
						double bar_x = 0.0;//注意I_k是根据y,x排序的。
						for(int i=0;i<I_k.size();i++) {
							if(I_k.get(i).getXCoor()+I_k.get(i).getWidth()>bar_x) {
								e.add(i);bar_x=I_k.get(i).getXCoor()+I_k.get(i).getWidth();//
							}
						}
						//Phase 2: determine the corner points
						double XCoor = 0.0;
						double YCoor = I_k.get(e.get(0)).getYCoor()+I_k.get(e.get(0)).getHeight();
						if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//							Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
							Box corner = new Box();
							corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
							Corners3D.add(corner);
						}
						/**
						 * 是否添加？
						 */
//						if(I_k.get(e.get(0)).getXCoor()>0.0) {
						XCoor = I_k.get(e.get(0)).getXCoor();
						YCoor = I_k.get(e.get(0)).getYCoor()+I_k.get(e.get(0)).getHeight();
						if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//							Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
							Box corner = new Box();
							corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
							Corners3D.add(corner);
						}
//						}
						for(int j=1;j<e.size();j++) {
							XCoor = I_k.get(e.get(j-1)).getXCoor()+I_k.get(e.get(j-1)).getWidth();
							YCoor = I_k.get(e.get(j)).getYCoor()+I_k.get(e.get(j)).getHeight();
							if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//								Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
								Box corner = new Box();
								corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
								Corners3D.add(corner);
							}
							if(I_k.get(e.get(j)).getXCoor()>XCoor) {
							XCoor = I_k.get(e.get(j)).getXCoor();
							YCoor = I_k.get(e.get(j)).getYCoor()+I_k.get(e.get(j)).getHeight();
							if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//								Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
								Box corner = new Box();
								corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
								Corners3D.add(corner);
							}
							}
						}
						XCoor = I_k.get(e.get(e.size()-1)).getXCoor()+I_k.get(e.get(e.size()-1)).getWidth();
						YCoor = 0.0;
						if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//							Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
							Box corner = new Box();
							corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
							Corners3D.add(corner);
						}
					}
				}//for k (each horizontal level)
//				if(Corners3D.size()<1) {
//				Box corner = new Box();
//				corner.setXCoor(0.0);corner.setYCoor(0.0);corner.setZCoor(horizontal_levels.get(horizontal_levels.size()-1));corner.setPlatformid(horizontal_levels.size()-1);//记录哪个level
//				Corners3D.add(corner);
//				}
//				}// if I
				//结束求解3DCorners=========================================================end
//				Corners3D.sort(new Comparator<Box>() {
//					//靠近原点的。
//					@Override
//					public int compare(Box o1, Box o2) {
//						double d1 = Math.pow(o1.getXCoor(), 2.0)+Math.pow(o1.getZCoor(), 2.0)+Math.pow(o1.getYCoor(), 2.0);
//						double d2 = Math.pow(o2.getXCoor(), 2.0)+Math.pow(o2.getZCoor(), 2.0)+Math.pow(o2.getYCoor(), 2.0);
//						if(d1<d2)
//							return -1;
//						else if(d1>d2)
//							return 1;
//						else
//							return 0;
//					}
//				});
				//找第一个能够存放当前箱子的位置存放当前箱子。
				
				iteratorBox = Corners3D.iterator();
				while(iteratorBox.hasNext()) {
					
					Box curr_position = iteratorBox.next();
					if(method==1&&curr_position.getXCoor()>curr_box.getWidth()&&(curr_position.getZCoor()+curr_box.getLength()>horizontal_levels.get(horizontal_levels.size()-1))) {
						continue;
					}
					if(curr_position.getXCoor()+curr_box.getWidth()<=width&&
							curr_position.getYCoor()+curr_box.getHeight()<=height&&
									curr_position.getZCoor()+curr_box.getLength()<=length) {
						
						//判断这个位置能不能站稳
						//当前箱子的坐标： boxingSequence.x,y,z
						//当前箱子的底部高度：boxingSequence.y，如果为0的话，就可以了
						//遍历所有的已经放了的箱子，看是否支撑现在的箱子。（暴力了点）
						boolean support = false;
						if(curr_position.getYCoor()==0) {
							support = true;
						}else{
							//计算该箱子的底部面积。
							double bottomArea = curr_box.getWidth()*curr_box.getLength();
							double curr_y = curr_position.getYCoor();//+boxingSequence.get(i).getHeight();
							double crossArea = 0;
							//计算所有已放箱子的顶部与该箱子的底部交叉面积
							for (int boxii=0;boxii<thissortedBox.size();boxii++) {
								//如果这个箱子的顶部与boxingSequence.get(i)的底部在同一水平上
								Box existBox = thissortedBox.get(boxii);
								
								if(Math.abs(existBox.getYCoor()+existBox.getHeight()-curr_y)<=1.5) {
									double xc=curr_position.getXCoor(),zc=curr_position.getZCoor(),xe=existBox.getXCoor(),ze=existBox.getZCoor();
									double wc=curr_box.getWidth(),lc=curr_box.getLength(),we=existBox.getWidth(),le=existBox.getLength();
									
									if(!((xc+wc<xe)||(xe+we<xc)||(zc+lc<ze)||(ze+le<zc))) {//如果有交叉，则计算交叉面积。
										double [] XCoor = {xc,xc+wc,xe,xe+we};
										double [] ZCoor = {zc,zc+lc,ze,ze+le};
										//sort xc,xc+wc,xe,xe+we
										 Arrays.sort(XCoor);
										 Arrays.sort(ZCoor);
										//sort zc,zc+lc,ze,ze+le
										 crossArea = crossArea + Math.abs(XCoor[2]-XCoor[1])*Math.abs(ZCoor[2]-ZCoor[1]);
										 if(crossArea>=0.8*bottomArea) {support=true;break;}//如果支撑面积大于80%，则不用继续判断了。
									}
								}
							}
							
						}
						//
						if(support) {//当前箱子可以加入到这辆车中。
							Box loadBox = new Box(curr_box);
							loadBox.setXCoor(curr_position.getXCoor());
							loadBox.setYCoor(curr_position.getYCoor());
							loadBox.setZCoor(curr_position.getZCoor());
							
							//将这个箱子插入到sortedBox里面，按Y-X从大到小进行排序。
							int idx=0;
							for(idx=0;idx<thissortedBox.size();idx++) {//按y,x,z来排序。
								Box thisbox = thissortedBox.get(idx);
								//如果在一个水平面上，则对比X
								if(Math.abs(thisbox.getYCoor()+thisbox.getHeight()-loadBox.getYCoor()-loadBox.getHeight())<1.5) {
									if(thisbox.getXCoor()+thisbox.getWidth()<loadBox.getXCoor()+loadBox.getWidth()) {break;}
								}else if(thisbox.getYCoor()+thisbox.getHeight()<loadBox.getYCoor()+loadBox.getHeight()){//如果不在一个水平面上，则对比Y
									break;
								}
							}
							thissortedBox.add(idx, new Box(loadBox));//sort sortedBox by y,x coordinate
							//新增水平面。
							double curr_level = loadBox.getZCoor()+loadBox.getLength();
							boolean addFlag=true;
							for(idx=curr_position.getPlatformid();idx<horizontal_levels.size();idx++) {
								if(Math.abs(horizontal_levels.get(idx)-curr_level)<=0.0) {//两个level相差多远就不加入来了。
									addFlag=false;break;
								}else if(horizontal_levels.get(idx)>curr_level) {
									break;
								}
							}
							if(addFlag) horizontal_levels.add(idx, curr_level);
//							if(!horizontal_levels.contains(curr_level)) {
//								
//								for(idx=curr_position.getPlatformid();idx<horizontal_levels.size();idx++) {
//									if(horizontal_levels.get(idx)>curr_level) {
//										break;
//									}
//								}
//								horizontal_levels.add(idx, curr_level);
//							}
							//if this horizontal level loadBox.y+loadBox.height not in horizontal_levels, then add it and sort horizontal levels by increasing values
							
							thisBoxes.add(loadBox);
//							System.out.println(boxi+" -th box is loaded ...");
							thisloadWeight = thisloadWeight + loadBox.getWeight();
							thisloadVolumn = thisloadVolumn + loadBox.getVolume();
							loadIdx.add(boxi);//第i-2+1个箱子被装载了。
							insertConfirm=true;
							break;
						}
					}
				}//while all positions
//				if(!insertConfirm) {
//					break;
//				}
			}
			if(loadIdx.size()==boxingSequence.size()) break;
			else {
				//调整序列的顺序，和方向。
				
			}
			iter++;
			}//while true
			this.Boxes = thisBoxes;
			this.sortedBoxes = thissortedBox;//用于Add
			this.horizontal_levels = horizontal_levels;
			this.loadWeight=thisloadWeight;
			this.loadVolumn=thisloadVolumn;
			//calculate excessWeight
//			if(this.loadWeight>capacity) {this.excessWeight=this.loadWeight-capacity;}else {this.excessWeight=0;}
			//calculate excessLength.
//			if(back.backSequence.get(back.backSequence.size()-1).getZCoor()
//					+back.backSequence.get(back.backSequence.size()-1).getLength()>this.length)
//				this.excessLength = back.backSequence.get(back.backSequence.size()-1).getZCoor()
//						+back.backSequence.get(back.backSequence.size()-1).getLength()-this.length;
//			else
//				this.excessLength = 0;
//			System.out.println("excessLength:"+this.excessLength+";excessWeight:"+this.excessWeight);
//			if(left.leftSequence.size()<boxingSequence1.size())
//			System.out.println("input box size:"+boxingSequence1.size()+"this vehicle size:"+this.Boxes.size());
			if(DEBUG)
			carriage.setTruckTypeCode("dblf"+method);
			return loadIdx;//left.leftSequence.size();
		}

		
		
		
		
		
		/**
		 * A general deepest bottom left fill method
		 */
		/**
		 * dblf算法装箱,根据yx进行排序找3DCorners
		 * @param clients
		 * @param Box2D, 保存的箱子及其坐标，bin的长宽高及其重量。
		 * 
		 * @return 装箱的box下标数组,装箱个数
		 * 
		 */
			public ArrayList<Integer> dblf_general(ArrayList<Box> boxingSequence, Carriage carriage, Box2D box2d, int method, double height, double capacity) {
//				double width = carriage.getWidth();
//				double length = carriage.getLength();
//				double height = carriage.getHeight();
//				double capacity = carriage.getCapacity();
//				Box2D box2d= new Box2D();//initialize a box2d
				
//				ArrayList<Box> thisBoxes = new ArrayList<Box>();//按顺序保存该箱子。
				ArrayList<Box> thissortedBox = new ArrayList<Box>();
				ArrayList<Double> horizontal_levels = new ArrayList<Double>();
				ArrayList<Integer> loadIdx=new ArrayList<Integer>();//保存装在这辆车里面的箱子集
				double thisloadWeight=0.0;//保存已经装的箱子的重量
				double max_height = 0.0;//
				double length = 0.0;
				double width = 0.0;
				int start_box=0;
				double begin_height = 0.0;
				
				if(box2d.getBoxes().size()>0) {
					Iterator<Box> iBox = box2d.getBoxes().iterator();
					while(iBox.hasNext()) {
						Box curr_box = new Box(iBox.next());
						thissortedBox.add(curr_box);
						//新增水平面。
						double curr_level = curr_box.getZCoor()+curr_box.getLength();
						boolean addFlag=true;
						int idx=-1;
						for(idx=0;idx<horizontal_levels.size();idx++) {
							if(Math.abs(horizontal_levels.get(idx)-curr_level)<=0.0) {//两个level相差多远就不加入来了。
								addFlag=false;break;
							}else if(horizontal_levels.get(idx)>curr_level) {
								break;
							}
						}
						if(addFlag) horizontal_levels.add(idx, curr_level);
					}
					//sort box2d by 
					//将这个箱子插入到sortedBox里面，按Y-X从大到小进行排序。
					thissortedBox.sort(new Comparator<Box>() {

						@Override
						public int compare(Box thisbox, Box loadBox) {
							// TODO Auto-generated method stub
							//如果在一个水平面上，则对比X
							if(Math.abs(thisbox.getYCoor()+thisbox.getHeight()-loadBox.getYCoor()-loadBox.getHeight())<1.5) {
								if(thisbox.getXCoor()+thisbox.getWidth()<loadBox.getXCoor()+loadBox.getWidth()) {
									return 1;
								}else if(thisbox.getXCoor()+thisbox.getWidth()>loadBox.getXCoor()+loadBox.getWidth()) {
									return -1;
								}else {
									return 0;
								}
							}else if(thisbox.getYCoor()+thisbox.getHeight()<loadBox.getYCoor()+loadBox.getHeight()){//如果不在一个水平面上，则对比Y
								return 1;
							}else {
								return -1;
							}
						}
						
					});
//					for(idx=0;idx<thissortedBox.size();idx++) {//按y,x,z来排序。
//						Box thisbox = thissortedBox.get(idx);
//						//如果在一个水平面上，则对比X
//						if(Math.abs(thisbox.getYCoor()+thisbox.getHeight()-loadBox.getYCoor()-loadBox.getHeight())<1.5) {
//							if(thisbox.getXCoor()+thisbox.getWidth()<loadBox.getXCoor()+loadBox.getWidth()) {break;}
//						}else if(thisbox.getYCoor()+thisbox.getHeight()<loadBox.getYCoor()+loadBox.getHeight()){//如果不在一个水平面上，则对比Y
//							break;
//						}
//					}
//					thissortedBox.add(idx, new Box(loadBox));//sort sortedBox by y,x coordinate
					thisloadWeight=box2d.getWeight();//保存已经装的箱子的重量
					max_height = box2d.getHeight();
					length = box2d.getLength();
					width = box2d.getWidth();
					start_box = 0;
					begin_height = max_height;
				}else {
				if(boxingSequence.get(0).getWeight()<capacity) {
				//添加第一个（必须确保boxingSequence是按底面积排序的）
				Box curr_box = new Box(boxingSequence.get(0));
				curr_box.setXCoor(0.0);
				curr_box.setYCoor(0.0);
				curr_box.setZCoor(0.0);
				box2d.getBoxes().add(curr_box);
				thissortedBox.add(new Box(boxingSequence.get(0)));
				width=curr_box.getWidth();
				length=curr_box.getLength();
				thisloadWeight=curr_box.getWeight();
				loadIdx.add(0);
//				int iter=0;
//				while(iter<1) {
				//boxingSequence是请求放在当前小车的箱子序列，每个平台的箱子从大到小排序。
//				horizontal_levels = new ArrayList<Double>();
				horizontal_levels.add(0.0);
				horizontal_levels.add(curr_box.getLength());
				start_box=1;
				begin_height=0.0;
				}
				}
//				thissortedBox = new ArrayList<Box>();//清空已经存放的boxes
//				thisBoxes = new ArrayList<Box>();//按顺序保存该箱子。
//				thisloadWeight=0.0;//保存已经装的箱子的重量
//				loadIdx=new ArrayList<Integer>();//保存装在这辆车里面的箱子集
				Iterator<Box> iteratorBox;
//				boolean insertConfirm;//是否成功插入当前箱子。
				for(int boxi=start_box;boxi<boxingSequence.size();boxi++) {
//					insertConfirm=false;
					Box curr_box = boxingSequence.get(boxi);
					if(thisloadWeight + curr_box.getWeight()>capacity) {
						continue;//当前箱子不能再加入这辆车了，退出寻找最优位置，并且“退出遍历箱子”，不要退出，看看别的箱子是否可以。
					}
//					if(max_height+curr_box.getHeight()>height) {
//						continue;//再看看别的箱子是否可以。
//					}
					//第一步先求3DCorners=========================================================
					ArrayList<Box> Corners3D = new ArrayList<Box>();//如果已经存放的箱子是0，则原点。
//					if(sortedBox.size()<1) {
//						Corners3D.add(new Box());
//					} else {
//					int k=0;//遍历每个Z平面，和Z轴length垂直的平面。
					for(int k=0;k<horizontal_levels.size() && horizontal_levels.get(k)+curr_box.getLength()<=carriage.getLength();k++) {
						//得到在这个平面之上的已经存放的boxes,I_k
						ArrayList<Box> I_k = new ArrayList<Box>();
						iteratorBox = thissortedBox.iterator();
						while(iteratorBox.hasNext()) {
							Box currBox = iteratorBox.next();
							if(currBox.getZCoor()+currBox.getLength()>horizontal_levels.get(k)) {
								I_k.add(new Box(currBox));
							}
						}
						
						//求2DCorners==========================================================begin
						if(I_k.size()<1) {
							//如果这个平面之上没有box,添加原点。
							Box corner = new Box();
							corner.setXCoor(0.0);corner.setYCoor(0.0);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
							Corners3D.add(corner);
						}else{
							//Phase 1: identify the extreme items e_1,...,e_m
							ArrayList<Integer> e = new ArrayList<Integer>();
							double bar_x = 0.0;//注意I_k是根据y,x排序的。
							for(int i=0;i<I_k.size();i++) {
								if(I_k.get(i).getXCoor()+I_k.get(i).getWidth()>bar_x) {
									e.add(i);bar_x=I_k.get(i).getXCoor()+I_k.get(i).getWidth();//
								}
							}
							//Phase 2: determine the corner points
							double XCoor = 0.0;
							double YCoor = I_k.get(e.get(0)).getYCoor()+I_k.get(e.get(0)).getHeight();
							if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//								Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
								Box corner = new Box();
								corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
								Corners3D.add(corner);
							}
							/**
							 * 是否添加？
							 */
							if(I_k.get(e.get(0)).getXCoor()>0.0) {
								XCoor = I_k.get(e.get(0)).getXCoor();
							YCoor = I_k.get(e.get(0)).getYCoor()+I_k.get(e.get(0)).getHeight();
							if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//								Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
								Box corner = new Box();
								corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
								Corners3D.add(corner);
							}
							}
							for(int j=1;j<e.size();j++) {
								XCoor = I_k.get(e.get(j-1)).getXCoor()+I_k.get(e.get(j-1)).getWidth();
								YCoor = I_k.get(e.get(j)).getYCoor()+I_k.get(e.get(j)).getHeight();
								if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//									Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
									Box corner = new Box();
									corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
									Corners3D.add(corner);
								}
								if(I_k.get(e.get(j)).getXCoor()>XCoor) {
								XCoor = I_k.get(e.get(j)).getXCoor();
								YCoor = I_k.get(e.get(j)).getYCoor()+I_k.get(e.get(j)).getHeight();
								if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//									Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
									Box corner = new Box();
									corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
									Corners3D.add(corner);
								}
								}
							}
							XCoor = I_k.get(e.get(e.size()-1)).getXCoor()+I_k.get(e.get(e.size()-1)).getWidth();
							YCoor = 0.0;
							if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//								Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
								Box corner = new Box();
								corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
								Corners3D.add(corner);
							}
						}
					}//for k (each horizontal level)
					
					
					
//					}// if I
					//结束求解3DCorners=========================================================end
					//找第一个能够存放当前箱子的位置存放当前箱子。
					iteratorBox = Corners3D.iterator();
					while(iteratorBox.hasNext()) {
						
						Box curr_position = iteratorBox.next();
						if(method==1&&curr_position.getXCoor()>curr_box.getWidth()&&(curr_position.getZCoor()+curr_box.getLength()>horizontal_levels.get(horizontal_levels.size()-1))) {
							continue;
						}
						if(curr_position.getYCoor()+curr_box.getHeight()<=height&&curr_position.getYCoor()>=begin_height) {//首先高度要先满足。
							if(curr_position.getXCoor()+curr_box.getWidth()>width||curr_position.getZCoor()+curr_box.getLength()>length) {
								//如果长和宽有不满足，并且放的坐标在原点，则扩展长和宽
								if(curr_position.getXCoor()==0&&curr_position.getZCoor()==0) {
									width = Math.max(width, curr_box.getWidth());
									length = Math.max(length, curr_box.getLength());
								}else {
									continue;
								}
							}
							//判断这个位置能不能站稳
							//当前箱子的坐标： boxingSequence.x,y,z
							//当前箱子的底部高度：boxingSequence.y，如果为0的话，就可以了
							//遍历所有的已经放了的箱子，看是否支撑现在的箱子。（暴力了点）
							boolean support = false;
							if(curr_position.getYCoor()==0) {
								support = true;
							}else{
								//计算该箱子的底部面积。
								double bottomArea = curr_box.getWidth()*curr_box.getLength();
								double curr_y = curr_position.getYCoor();//+boxingSequence.get(i).getHeight();
								double crossArea = 0;
								//计算所有已放箱子的顶部与该箱子的底部交叉面积
								for (int boxii=0;boxii<thissortedBox.size();boxii++) {
									//如果这个箱子的顶部与boxingSequence.get(i)的底部在同一水平上
									Box existBox = thissortedBox.get(boxii);
									
									if(Math.abs(existBox.getYCoor()+existBox.getHeight()-curr_y)<=1.5) {
										double xc=curr_position.getXCoor(),zc=curr_position.getZCoor(),xe=existBox.getXCoor(),ze=existBox.getZCoor();
										double wc=curr_box.getWidth(),lc=curr_box.getLength(),we=existBox.getWidth(),le=existBox.getLength();
										
										if(!((xc+wc<xe)||(xe+we<xc)||(zc+lc<ze)||(ze+le<zc))) {//如果有交叉，则计算交叉面积。
											double [] XCoor = {xc,xc+wc,xe,xe+we};
											double [] ZCoor = {zc,zc+lc,ze,ze+le};
											//sort xc,xc+wc,xe,xe+we
											 Arrays.sort(XCoor);
											 Arrays.sort(ZCoor);
											//sort zc,zc+lc,ze,ze+le
											 crossArea = crossArea + Math.abs(XCoor[2]-XCoor[1])*Math.abs(ZCoor[2]-ZCoor[1]);
											 if(crossArea>=0.8*bottomArea) {support=true;break;}//如果支撑面积大于80%，则不用继续判断了。
										}
									}
								}
								
							}
							//
							if(support) {//当前箱子可以加入到这辆车中。
								Box loadBox = new Box(curr_box);
								loadBox.setXCoor(curr_position.getXCoor());
								loadBox.setYCoor(curr_position.getYCoor());
								loadBox.setZCoor(curr_position.getZCoor());
								
								//将这个箱子插入到sortedBox里面，按Y-X从大到小进行排序。
								int idx=0;
								for(idx=0;idx<thissortedBox.size();idx++) {//按y,x,z来排序。
									Box thisbox = thissortedBox.get(idx);
									//如果在一个水平面上，则对比X
									if(Math.abs(thisbox.getYCoor()+thisbox.getHeight()-loadBox.getYCoor()-loadBox.getHeight())<1.5) {
										if(thisbox.getXCoor()+thisbox.getWidth()<loadBox.getXCoor()+loadBox.getWidth()) {break;}
									}else if(thisbox.getYCoor()+thisbox.getHeight()<loadBox.getYCoor()+loadBox.getHeight()){//如果不在一个水平面上，则对比Y
										break;
									}
								}
								thissortedBox.add(idx, new Box(loadBox));//sort sortedBox by y,x coordinate
								//新增水平面。
								double curr_level = loadBox.getZCoor()+loadBox.getLength();
								boolean addFlag=true;
								for(idx=curr_position.getPlatformid();idx<horizontal_levels.size();idx++) {
									if(Math.abs(horizontal_levels.get(idx)-curr_level)<=0.0) {//两个level相差多远就不加入来了。
										addFlag=false;break;
									}else if(horizontal_levels.get(idx)>curr_level) {
										break;
									}
								}
								if(addFlag) horizontal_levels.add(idx, curr_level);
//								if(!horizontal_levels.contains(curr_level)) {
//									
//									for(idx=curr_position.getPlatformid();idx<horizontal_levels.size();idx++) {
//										if(horizontal_levels.get(idx)>curr_level) {
//											break;
//										}
//									}
//									horizontal_levels.add(idx, curr_level);
//								}
								//if this horizontal level loadBox.y+loadBox.height not in horizontal_levels, then add it and sort horizontal levels by increasing values
								
								box2d.getBoxes().add(loadBox);
								thisloadWeight = thisloadWeight + loadBox.getWeight();
								if(max_height<loadBox.getYCoor()+loadBox.getHeight())
									max_height = loadBox.getYCoor() + loadBox.getHeight();
								loadIdx.add(boxi);//第i-2+1个箱子被装载了。
//								insertConfirm=true;
								break;
							}
						}
					}//while all positions
//					if(!insertConfirm) {
//						break;
//					}
				}
//				if(loadIdx.size()==boxingSequence.size()) break;
//				else {
//					//调整序列的顺序，和方向。
//					
//				}
//				iter++;
//				}//while true
//				box2d.setBoxes(thisBoxes);
				box2d.setWeight(thisloadWeight);
				box2d.setHeight(max_height);
				box2d.setWidth(width);
				box2d.setLength(length);
//				this.Boxes = thisBoxes;
//				this.sortedBoxes = thissortedBox;
//				this.horizontal_levels = horizontal_levels;
//				this.loadWeight=thisloadWeight;
				//calculate excessWeight
//				if(this.loadWeight>capacity) {this.excessWeight=this.loadWeight-capacity;}else {this.excessWeight=0;}
				//calculate excessLength.
//				if(back.backSequence.get(back.backSequence.size()-1).getZCoor()
//						+back.backSequence.get(back.backSequence.size()-1).getLength()>this.length)
//					this.excessLength = back.backSequence.get(back.backSequence.size()-1).getZCoor()
//							+back.backSequence.get(back.backSequence.size()-1).getLength()-this.length;
//				else
//					this.excessLength = 0;
//				System.out.println("excessLength:"+this.excessLength+";excessWeight:"+this.excessWeight);
//				if(left.leftSequence.size()<boxingSequence1.size())
//				System.out.println("input box size:"+boxingSequence1.size()+"this vehicle size:"+this.Boxes.size());
				return loadIdx;//left.leftSequence.size();
			}

		
		/**
		 * Our proposed method
		 * @param boxingSequence
		 * @return
		 */
		public ArrayList<Integer> zqlbpp(final ArrayList<Box> boxingSequence,Carriage carriage,int method) {
			double width = carriage.getWidth();
			double length = carriage.getLength();
			double height = carriage.getHeight();
			double capacity = carriage.getCapacity();
			ArrayList<Integer> loadIdx = new ArrayList<Integer>();
//			int n_load = 0;
			double total_weight = 0.0;
			double total_volumn = 0.0;
			
			double temp;
			//============================================================encode
			//将boxingSequence分组，每一组的高度不超过height
			//boxingSequence按照底面积从大到小进行排序，底面积一样，就按高度从高到低进行排序。
			ArrayList<Box2D> Box2Dlist = new ArrayList<Box2D>();
//			Iterator<Box> iteratorBox = boxingSequence.iterator();
//			int method = 2;
			if(method==20) {
			//=============================method 1
//			Box2D b2D = new Box2D();
			Box2D curr_boxlist= new Box2D();
//			curr_boxlist.setBoxes(new ArrayList<Box>());
			double curr_platform = boxingSequence.get(0).getPlatformid();
			double curr_weight=0.0;
			double curr_height = 0.0;
//			double bottom_area = 0.0;
			for(int boxid=0;boxid<boxingSequence.size();boxid++) {
				Box curr_box = new Box(boxingSequence.get(boxid));
				if(total_weight+curr_box.getWeight()>capacity) {
					break;//weight constraint.
				}
				curr_box.setId(boxid);
				double suport_area = curr_box.getWidth()*curr_box.getLength();
				if(boxid>0)
					suport_area = Math.min(curr_box.getLength(), boxingSequence.get(boxid-1).getLength())*Math.min(curr_box.getWidth(), boxingSequence.get(boxid-1).getWidth());
				if(curr_height+curr_box.getHeight()>height||curr_box.getPlatformid()!=curr_platform||suport_area<0.8*curr_box.getWidth()*curr_box.getLength()) {
					curr_boxlist.setWeight(curr_weight);
					curr_boxlist.setHeight(curr_height);
					Box2Dlist.add(curr_boxlist);//加进来。
					curr_boxlist = new Box2D();//重新初始化
//					curr_boxlist.setBoxes(new ArrayList<Box>());
//					curr_boxlist.getBoxes().add(new Box(curr_box));
					curr_platform = curr_box.getPlatformid();
					curr_weight = 0.0;
					curr_height = 0.0;
				}
				curr_boxlist.getBoxes().add(curr_box);
//				curr_boxlist.setHeight(curr_boxlist.getHeight()+curr_box.getHeight());
				if(curr_boxlist.getWidth()<curr_box.getWidth())
					curr_boxlist.setWidth(curr_box.getWidth());
				if(curr_boxlist.getLength()<curr_box.getLength())
					curr_boxlist.setLength(curr_box.getLength());
				curr_weight = curr_weight+curr_box.getWeight();
				curr_height = curr_height+curr_box.getHeight();
				total_weight = total_weight+curr_box.getWeight();
				total_volumn = total_volumn+curr_box.getVolume();
			}
			curr_boxlist.setWeight(curr_weight);
			curr_boxlist.setHeight(curr_height);
			Box2Dlist.add(curr_boxlist);//加进来。
			
			//=============================method 1
			//combine
			//高度没有满的box2d能不能进一步叠加呢？后面叠加到前面。
			//两两组合
			boolean change_flag = true;
			while(change_flag) {
				change_flag=false;
			for(int i=0;i<Box2Dlist.size();i++) {
				for(int j=i+1;j<Box2Dlist.size();j++) {
					if(Box2Dlist.get(i).getBoxes().get(0).getPlatformid()==Box2Dlist.get(j).getBoxes().get(0).getPlatformid()) {
					double total_height = Box2Dlist.get(i).getHeight()+Box2Dlist.get(j).getHeight();
					if(total_height<=height) {
					Box boxi = Box2Dlist.get(i).getBoxes().get(Box2Dlist.get(i).getBoxes().size()-1);//最上面的。
					Box boxj = Box2Dlist.get(j).getBoxes().get(0);//最底下的。
					//判断j是否可以盖在i上面。
					double suport_area = Math.min(boxi.getLength(), boxj.getLength())*Math.min(boxi.getWidth(), boxj.getWidth());
					if(suport_area>=0.8*boxj.getLength()*boxj.getWidth()) {
						//组合这两组boxes
						//把j加到i中
						Iterator<Box> iteratorbj = Box2Dlist.get(j).getBoxes().iterator();
						while(iteratorbj.hasNext()) {
							Box curr_box = new Box(iteratorbj.next());
							Box2Dlist.get(i).getBoxes().add(curr_box);//加入到i中。
							//改变i的width和length
							if(curr_box.getLength()>Box2Dlist.get(i).getLength())
								Box2Dlist.get(i).setLength(curr_box.getLength());
							if(curr_box.getWidth()>Box2Dlist.get(i).getWidth())
								Box2Dlist.get(i).setWidth(curr_box.getWidth());
						}
						//改变i的height
						Box2Dlist.get(i).setHeight(total_height);
						//把j删除。
						Box2Dlist.remove(j);
						change_flag=true;
						break;
					}
					}
					}
				}// for j
				if(change_flag) break;
			}//for i
			
			}//while change_flag
			}
			/**
			 * 同一个平台的boxes在同一个柱子上可以一直加。
			 */
			if(method==2) {
			//=============================method 2
//				Box2D b2D = new Box2D();
			Box2D curr_boxlist= new Box2D();
//				curr_boxlist.setBoxes(new ArrayList<Box>());
			double curr_platform = boxingSequence.get(0).getPlatformid();
			double curr_weight=0.0;
			total_weight = 0.0;
			double curr_height = 0.0;
//				double bottom_area = 0.0;
//			ArrayList<Integer> packedIdx = new ArrayList<Integer>();
			ArrayList<Integer> unpackedIdx = new ArrayList<Integer>();
			for(int boxid=0;boxid<boxingSequence.size();boxid++) {
				unpackedIdx.add(boxid);
				if(boxingSequence.get(boxid).getHeight()>carriage.getHeight()||
						boxingSequence.get(boxid).getWidth()>carriage.getWidth()||
						boxingSequence.get(boxid).getLength()>carriage.getLength())
					return loadIdx;
//					System.out.println("this box can not be loaded.");
			}
			boolean finished = false;
			ArrayList<Integer> unpackedIdx_next = new ArrayList<Integer>();
			while(unpackedIdx.size()>0&&!finished) {//还有没有pack的box，则继续添加。
//				Iterator<Integer> iteratorInt = unpackedIdx.iterator();
//			for(int boxid=0;boxid<boxingSequence.size();boxid++) {
				curr_boxlist = new Box2D();//重新初始化
				int curr_boxlist_size = 0;
				curr_platform = boxingSequence.get(unpackedIdx.get(0)).getPlatformid();
				curr_weight = 0.0;
				curr_height = 0.0;
//				ArrayList<Integer> packedIdx = new ArrayList<Integer>();
//				while(iteratorInt.hasNext()) {
//					int boxid = iteratorInt.next();
				unpackedIdx_next = new ArrayList<Integer>();
				for(int unpackidx=0;unpackidx<unpackedIdx.size();unpackidx++) {
					int boxid = unpackedIdx.get(unpackidx);
					Box curr_box = new Box(boxingSequence.get(boxid));
					if(total_weight+curr_box.getWeight()>capacity) {
						finished=true;break;//weight constraint.
					}
					curr_box.setId(boxid);
					double suport_area = curr_box.getWidth()*curr_box.getLength();
					if(curr_boxlist_size>0)
						suport_area = Math.min(curr_box.getLength(), curr_boxlist.getBoxes().get(curr_boxlist_size-1).getLength())*Math.min(curr_box.getWidth(), curr_boxlist.getBoxes().get(curr_boxlist_size-1).getWidth());
					if(curr_height+curr_box.getHeight()>height||curr_box.getPlatformid()!=curr_platform||suport_area<0.8*curr_box.getWidth()*curr_box.getLength()) {
						//不要因为加了下一个box超过了就不再增加了。需要继续遍历boxes
						//1.平台不一样了，需要换吗？需要仔细斟酌下。
						//2.就是下一个box的支撑面不足，就不增加了吗？这个也是不合理的。
						//需要对每一个boxes进行遍历而得到一个box2d
						//如果不满足条件，则continue就好了。
						//加入到unpackedIdx
						unpackedIdx_next.add(boxid);//这些箱子没有装载的。
						continue;
	//					curr_boxlist.setWeight(curr_weight);
	//					curr_boxlist.setHeight(curr_height);
	//					Box2Dlist.add(curr_boxlist);//加进来。
	//					curr_boxlist = new Box2D();//重新初始化
	////						curr_boxlist.setBoxes(new ArrayList<Box>());
	////						curr_boxlist.getBoxes().add(new Box(curr_box));
	//					curr_platform = curr_box.getPlatformid();
	//					curr_weight = 0.0;
	//					curr_height = 0.0;
					}else {
						curr_boxlist.getBoxes().add(curr_box);
		//					curr_boxlist.setHeight(curr_boxlist.getHeight()+curr_box.getHeight());
						if(curr_boxlist.getWidth()<curr_box.getWidth())
							curr_boxlist.setWidth(curr_box.getWidth());
						if(curr_boxlist.getLength()<curr_box.getLength())
							curr_boxlist.setLength(curr_box.getLength());
						curr_weight = curr_weight+curr_box.getWeight();
						curr_height = curr_height+curr_box.getHeight();
						total_weight = total_weight+curr_box.getWeight();
//						//这个boxid应该要从unpackedIdx里面删除。
//						packedIdx.add(unpackidx);
						curr_boxlist_size++;
					}
				}
				if(curr_boxlist.getBoxes().size()>0) {//Box2Dlist不能加一个空的进来。
				curr_boxlist.setWeight(curr_weight);
				curr_boxlist.setHeight(curr_height);
				Box2Dlist.add(curr_boxlist);//加进来。
				}
//				//从unpackIdx里面删除
				unpackedIdx=new ArrayList<Integer>();
				Iterator<Integer> iteratorInt = unpackedIdx_next.iterator();
				while(iteratorInt.hasNext()) unpackedIdx.add(iteratorInt.next());
				
				
//				if(finished) {break;}
			}

			}
			/**
			 * method3的难点在于不在原点的箱子的坐标需要进行转换。
			 * 
			 */
			if(method==3) {
			//=============================method 2
				total_weight = 0.0;
				/**
				 * 将所有的boxes按platform进行划分。到platform_boxes里面。
				 */
				//取出一个platform的boxes，或者将所有的boxes按platform进行划分。
				ArrayList<ArrayList<Box>> platform_boxes = new ArrayList<ArrayList<Box>>();
				ArrayList<Box> curr_platform_boxes = new ArrayList<Box>();
				int curr_platform = boxingSequence.get(0).getPlatformid();
				boolean debug=true;
				for(int boxid=0;boxid<boxingSequence.size();boxid++) {
					Box curr_box = new Box(boxingSequence.get(boxid));
					curr_box.setId(boxid);
//					if(curr_box.getPlatformid()==3&&debug&&boxingSequence.size()==6) {
//						System.out.print("wait");
//						debug=false;
//					}
					if(curr_box.getPlatformid()!=curr_platform) {
						platform_boxes.add(curr_platform_boxes);//这个平台的box都加过来。
						curr_platform_boxes = new ArrayList<Box>();
						curr_platform=curr_box.getPlatformid();
						
					}
					curr_platform_boxes.add(curr_box);
				}
				platform_boxes.add(curr_platform_boxes);//这个平台的box都加过来。
//				if(boxingSequence.size()==6&&platform_boxes.size()==2) {
//					System.out.print("wait");
//				}
				
				for(int platformidx=0;platformidx<platform_boxes.size();platformidx++) {//每个平台。
					
					/**
					 * 我们装的时候，首先看看现有的Box2Dlist里面可不可以装下。
					 * Box2Dlist按照高度从高到低进行排序。
					 * dblf_general_add需要检查所有已经装箱的boxes
					 */
					ArrayList<Integer> loadBoxIdx=new ArrayList<Integer>();//已经装载的boxes的下标。
					for(int box2d_idx=0;box2d_idx<Box2Dlist.size();box2d_idx++) {//对所有已经装载的box2d
						if(Box2Dlist.get(box2d_idx).getBoxes().get(0).getPlatformid()!=platform_boxes.get(platformidx).get(0).getPlatformid())
							continue;
						/**
						 * 选出所有未装载的boxes
						 */
		//				route.setPlatforms(platforms);
						ArrayList<Box> unloadboxes = new ArrayList<Box>();
						ArrayList<Integer> unloadboxesIdx = new ArrayList<Integer>();
						for(int boxi=0;boxi<platform_boxes.get(platformidx).size();boxi++) {
							if(!loadBoxIdx.contains(boxi)) {
								Box curr_box = new Box(platform_boxes.get(platformidx).get(boxi));
								unloadboxes.add(curr_box);
								unloadboxesIdx.add(boxi);
//								if(thiswidth<curr_box.getWidth())
//									thiswidth = curr_box.getWidth();
//								if(thislength<curr_box.getLength())
//									thislength = curr_box.getLength();
							}
						}
						
						//检查这个box2d能否装下箱子了。
						Box2D curr_box2d = Box2Dlist.get(box2d_idx);
						double before_weight = curr_box2d.getWeight();
						//对curr_box2d加入箱子。
						ArrayList<Integer> thisloadIdx = dblf_general(unloadboxes,carriage, curr_box2d, 0, height, capacity-total_weight);
						total_weight = total_weight - before_weight + curr_box2d.getWeight();
						//将已经装载的box下标加到loadBoxIdx里面。
						Iterator<Integer> iterator = thisloadIdx.iterator();
						while(iterator.hasNext()) {
							//将已经装载的box下标加到loadBoxIdx里面。
							int index = unloadboxesIdx.get(iterator.next());
							loadBoxIdx.add(index);
//							boxesInthisvehicle.add(new Box(clients.get(i).getGoods().get(index)));
						}//
					}
					
					
					/**
					 * 用deepest bottom left fill 方法对这些boxes进行装箱。
					 * dblf_general增加一个新的boxes。
					 * 
					 */
//				ArrayList<Integer> loadBoxIdx=new ArrayList<Integer>();//已经装载的boxes的下标。
				while(loadBoxIdx.size()<platform_boxes.get(platformidx).size()) {
//					double thislength=0.0, thiswidth=0.0;
					/**
					 * 选出所有未装载的boxes
					 */
	//				route.setPlatforms(platforms);
					ArrayList<Box> unloadboxes = new ArrayList<Box>();
					ArrayList<Integer> unloadboxesIdx = new ArrayList<Integer>();
					for(int boxi=0;boxi<platform_boxes.get(platformidx).size();boxi++) {
						if(!loadBoxIdx.contains(boxi)) {
							Box curr_box = new Box(platform_boxes.get(platformidx).get(boxi));
							unloadboxes.add(curr_box);
							unloadboxesIdx.add(boxi);
//							if(thiswidth<curr_box.getWidth())
//								thiswidth = curr_box.getWidth();
//							if(thislength<curr_box.getLength())
//								thislength = curr_box.getLength();
						}
					}
					
					//
					Box2D curr_box2d = new Box2D();
					ArrayList<Integer> thisloadIdx;
					if(unloadboxes.get(0).getWeight()<capacity-total_weight)//至少装一个？？
						thisloadIdx = dblf_general(unloadboxes,carriage, curr_box2d, 0, height, capacity-total_weight);
					else
						break;
					//箱子开始的长宽为底面积最大的那个箱子。
					total_weight = total_weight + curr_box2d.getWeight();
					Box2Dlist.add(curr_box2d);//最终加入到box2Dlist里面。
					//将已经装载的box下标加到loadBoxIdx里面。
					Iterator<Integer> iterator = thisloadIdx.iterator();
					while(iterator.hasNext()) {
						//将已经装载的box下标加到loadBoxIdx里面。
						int index = unloadboxesIdx.get(iterator.next());
						loadBoxIdx.add(index);
//						boxesInthisvehicle.add(new Box(clients.get(i).getGoods().get(index)));
					}//
				}//while curr_platform_boxes
				}//for platformidx
			//=============================method 3
			}
			//============================================================encode
			
			
			
			
			
			//2D BPP
			//把Box2Dlist里面所有Boxes进行2D BPP
			ArrayList<Box2D> load_box2d=new ArrayList<Box2D>();
			ArrayList<Box2D> load_box2d_sort = new ArrayList<Box2D>();
			ArrayList<EP> poslist = new ArrayList<EP>();//x,z,width,length,platformID可以存放的platform，0表示都可以存放。
			ArrayList<Double> zlevels = new ArrayList<Double>();
			//在原點的extreme point
			EP ep = new EP();
			ep.setXCoor(0.0);
			ep.setZCoor(0.0);
			ep.setLength(length);
			ep.setWidth(width);
			ep.setBottom_area(length*width);
			poslist.add(ep);
			
			//sort Box2dlist By BOTTOM AREA.???
			for(int box2di=0;box2di<Box2Dlist.size();box2di++) {
				Box2D curr_box2d = Box2Dlist.get(box2di);//當前需要裝載的rectangle
				//选择一个ep,添加curr_box2d
//				int best_posi=0;
//				int posi=-1;
//				double residual_area = Double.MAX_VALUE;//保存最小的剩余面积。
				ArrayList<L> feasible_pos = new ArrayList<L>();
				for(int posi=0;posi<poslist.size();posi++) {
					//首先判断底面积是否够。
					//100方向判断。
					if(poslist.get(posi).getBottom_area()>=curr_box2d.getWidth()*curr_box2d.getLength()) {
					//然后判断长宽是否合适。
					if(curr_box2d.getWidth()<=poslist.get(posi).getWidth()&&
							curr_box2d.getLength()<=poslist.get(posi).getLength()&&
							poslist.get(posi).getXCoor()+curr_box2d.getWidth()<=width&&
							poslist.get(posi).getZCoor()+curr_box2d.getLength()<=length) {//判断当前位置是否可以放下当前box2d
						if(poslist.get(posi).getPlatformID()==0||curr_box2d.getBoxes().get(0).getPlatformid()==poslist.get(posi).getPlatformID()) {//这个位置只能放这个平台的箱子。
							//找到了可以放的位置，则退出。
//							break;
							L l = new L();
							l.setI(posi);
							l.setJ(100);
							l.setSij(-Math.min(poslist.get(posi).getWidth()-curr_box2d.getWidth(), poslist.get(posi).getLength()-curr_box2d.getLength()));
							l.setSmn(poslist.get(posi).getBottom_area()-curr_box2d.getWidth()*curr_box2d.getLength());
							feasible_pos.add(l);
						}
					}
					//200方向判断。
					if(curr_box2d.getLength()<=poslist.get(posi).getWidth()&&
							curr_box2d.getWidth()<=poslist.get(posi).getLength()&&
							poslist.get(posi).getXCoor()+curr_box2d.getLength()<=width&&
							poslist.get(posi).getZCoor()+curr_box2d.getWidth()<=length) {//判断当前位置是否可以放下当前box2d
						if(poslist.get(posi).getPlatformID()==0||curr_box2d.getBoxes().get(0).getPlatformid()==poslist.get(posi).getPlatformID()) {//这个位置只能放这个平台的箱子。
							//找到了可以放的位置，则退出。
//							break;
							L l = new L();
							l.setI(posi);
							l.setJ(200);
							l.setSij(-Math.min(poslist.get(posi).getWidth()-curr_box2d.getLength(), poslist.get(posi).getLength()-curr_box2d.getWidth()));
							l.setSmn(poslist.get(posi).getBottom_area()-curr_box2d.getWidth()*curr_box2d.getLength());
							feasible_pos.add(l);
						}
					}
					}
				}
				if(feasible_pos.size()==0) {continue;}//没有位置可以存放这些箱子，则继续存放其他箱子。
				Collections.sort(feasible_pos);
				int posi=feasible_pos.get(0).getI();
				curr_box2d.setDirection(feasible_pos.get(0).getJ());
				if(feasible_pos.get(0).getJ()==200) {
					temp = curr_box2d.getWidth();//暂存下width
					curr_box2d.setWidth(curr_box2d.getLength());//长宽互调。
					curr_box2d.setLength(temp);//
				}
					
				//当前位置可行。
				curr_box2d.setXCoor(poslist.get(posi).getXCoor());
				curr_box2d.setZCoor(poslist.get(posi).getZCoor());
				
				
				//添加新的ep，從一個變成多個。
				ArrayList<EP> neweps = new ArrayList<EP>();//
				
				double [] minBound = {-1,-1};
				double [] maxBound = {width,length};//neweps.get(0).getWidth();
				ep = new EP(poslist.get(posi));
				ep.setXCoor(curr_box2d.getXCoor()+curr_box2d.getWidth());
				ep.setZCoor(0.0);
				ep.setWidth(poslist.get(posi).getWidth()-curr_box2d.getWidth());
//				neweps[0].setLength(length);
				neweps.add(ep);
				//添加新的ep
				ep=new EP(poslist.get(posi));
				ep.setXCoor(0.0);
				ep.setZCoor(curr_box2d.getZCoor()+curr_box2d.getLength());
//				neweps[1].setWidth(width);
				ep.setLength(poslist.get(posi).getLength()-curr_box2d.getLength());
				neweps.add(ep);
				
				//檢查有沒有存放的箱子在新增的兩個空間裡面。
				Iterator<Box2D> iteratorBox2D = load_box2d.iterator();//这里得包含一个0箱子。
				while(iteratorBox2D.hasNext()) {//所有已经存放的箱子。
					Box2D bi2d = iteratorBox2D.next();
					
//					double x_i=bi2d.getXCoor(),z_i=bi2d.getZCoor();
//					double w_i=bi2d.getWidth(),l_i=bi2d.getLength();
					//添加6个extreme point
					//(curr_position.getXCoor()+curr_box.getWidth(),curr_position.getYCoor(),curr_position.getZCoor())
					// (x_k,y_k+h_k,z_k) in the direction of the Y axes
					//CanTakeProjection就是这个Corner point会穿过这个box
//					if(!(y_k+h_k<y_i||y_k+h_k>y_i+h_i||z_k<z_i||z_k>z_i+l_i)&&x_i+w_i>maxBound[YX]&&x_k>0) {
					if(neweps.get(0).getXCoor()>=bi2d.getXCoor()&&
							neweps.get(0).getXCoor()<bi2d.getXCoor()+bi2d.getWidth()&&
							bi2d.getZCoor()+bi2d.getLength()>minBound[0]&&
							curr_box2d.getZCoor()>0) {//这个新增的存放空間被已经存放的箱子覆盖了length
						//往前|
						//原来的length-当前box的这边。
						neweps.get(0).setZCoor(bi2d.getZCoor()+bi2d.getLength());
						neweps.get(0).setLength(neweps.get(0).getZCoor()+neweps.get(0).getLength()-bi2d.getZCoor()-bi2d.getLength());
						
						minBound[0] = bi2d.getZCoor()+bi2d.getLength();//所有已經存放的箱子的最右邊的坐標(length最大的）。
					}
					
					if(neweps.get(1).getZCoor()>=bi2d.getZCoor()&&
							neweps.get(1).getZCoor()<bi2d.getZCoor()+bi2d.getLength()&&
							bi2d.getXCoor()+bi2d.getWidth()>minBound[1]&&//在左边。
							curr_box2d.getXCoor()>0) {//这个新增的空間被已经存放的箱子覆盖了width
						//往左
						neweps.get(1).setXCoor(bi2d.getXCoor()+bi2d.getWidth());
						neweps.get(1).setWidth(neweps.get(1).getXCoor()+neweps.get(1).getWidth()-bi2d.getXCoor()-bi2d.getWidth());
						
						minBound[1] = bi2d.getXCoor()+bi2d.getWidth();//所有已經存放的箱子的最上面的坐標（width最大的）。
					}
				}
				//再次确定neweps.get(0).width
				//以及neweps.get(1).length??不确定是否需要。
				iteratorBox2D = load_box2d.iterator();//这里得包含一个0箱子。
				
				while(iteratorBox2D.hasNext()) {
					Box2D bi2d = iteratorBox2D.next();
					
//					double x_i=bi2d.getXCoor(),z_i=bi2d.getZCoor();
//					double w_i=bi2d.getWidth(),l_i=bi2d.getLength();
					//添加6个extreme point
					//(curr_position.getXCoor()+curr_box.getWidth(),curr_position.getYCoor(),curr_position.getZCoor())
					// (x_k,y_k+h_k,z_k) in the direction of the Y axes
					//CanTakeProjection就是这个Corner point会穿过这个box
//					if(!(y_k+h_k<y_i||y_k+h_k>y_i+h_i||z_k<z_i||z_k>z_i+l_i)&&x_i+w_i>maxBound[YX]&&x_k>0) {
					if(neweps.get(0).getZCoor()>=bi2d.getZCoor()&&
							neweps.get(0).getZCoor()<bi2d.getZCoor()+bi2d.getLength()&&
							bi2d.getXCoor()<=maxBound[0]) {//在右边。
						neweps.get(0).setWidth(bi2d.getXCoor()-neweps.get(0).getXCoor());
						maxBound[0] = bi2d.getXCoor();
//						neweps.get(0).setZCoor(bi2d.getZCoor()+bi2d.getLength());
//						neweps.get(0).setLength(neweps.get(0).getLength()+neweps.get(0).getZCoor()-bi2d.getZCoor()-bi2d.getLength());
//						maxBound[0] = bi2d.getZCoor()+bi2d.getLength();
					}
					
					if(neweps.get(1).getXCoor()>=bi2d.getXCoor()&&
							neweps.get(1).getXCoor()<bi2d.getXCoor()+bi2d.getWidth()&&
							bi2d.getZCoor()<=maxBound[1]) {//被其他箱子覆盖了，需要考虑platform覆盖的约束。
						//这个位置只能存放这个平台的箱子。
						neweps.get(1).setPlatformID(bi2d.getBoxes().get(0).getPlatformid());
//						neweps.get(1).setXCoor(bi2d.getXCoor()+bi2d.getWidth());
						neweps.get(1).setLength(bi2d.getZCoor()-neweps.get(1).getZCoor());
						maxBound[1] = bi2d.getZCoor();
					}
				}
				
				//删除覆盖的ep
				poslist.remove(posi);
				
				//调整poslist的width和length
				//Update Residual Space.
				//放了箱子之后，其他EP是否空间变化了。
				for(int EPi=0;EPi<poslist.size();EPi++) {
					//插入的box擋住了當前空間的length。（僅僅）
					if(poslist.get(EPi).getXCoor()>=curr_box2d.getXCoor() &&
							poslist.get(EPi).getXCoor()<curr_box2d.getXCoor()+curr_box2d.getWidth()&&
							poslist.get(EPi).getZCoor()<=curr_box2d.getZCoor()&&
							curr_box2d.getZCoor()<poslist.get(EPi).getLength()+poslist.get(EPi).getZCoor()) {
						//这个位置被其他箱子挡住了。注意得将这个位置的箱子属性设置为挡住的箱子的属性（platform）

//						ep=new EP(poslist.get(EPi));
//						ep.setXCoor(curr_box2d.getXCoor()+curr_box2d.getWidth());
//						ep.setWidth(poslist.get(EPi).getXCoor()+poslist.get(EPi).getWidth()-curr_box2d.getXCoor()-curr_box2d.getWidth());
//						neweps.add(ep);	
						
						
						poslist.get(EPi).setLength(curr_box2d.getZCoor()-poslist.get(EPi).getZCoor());
						poslist.get(EPi).setPlatformID(curr_box2d.getBoxes().get(0).getPlatformid());//这个位置只能放这个平台的箱子。如果platformID=0,则表示可以放任何平台的箱子。
							
					}
					//插入的box擋住了當前空間的width（僅僅）
					if(poslist.get(EPi).getZCoor()>=curr_box2d.getZCoor() &&
							poslist.get(EPi).getZCoor()<curr_box2d.getZCoor()+curr_box2d.getLength()&&
							poslist.get(EPi).getXCoor()<=curr_box2d.getXCoor()&&
							curr_box2d.getXCoor()<poslist.get(EPi).getWidth()+poslist.get(EPi).getXCoor()) {
//							poslist.get(EPi).setWidth(Math.min(poslist.get(EPi).getWidth(),curr_box2d.getXCoor()-poslist.get(EPi).getXCoor()));
						//是否添加新的空間？？？這會增加複雜度。
//						ep=new EP(poslist.get(EPi));
//						ep.setLength(poslist.get(EPi).getZCoor()+poslist.get(EPi).getLength()-curr_box2d.getZCoor()-curr_box2d.getLength());
//						ep.setZCoor(curr_box2d.getZCoor()+curr_box2d.getLength());
//						neweps.add(ep);
						
						poslist.get(EPi).setWidth(curr_box2d.getXCoor()-poslist.get(EPi).getXCoor());
					}
					//2021-2-17檢查是否解決問題。（既擋住了width又擋住了length，處理方式：只保留length這個方向。（不用改變platform）
//					if(poslist.get(EPi).getZCoor()+poslist.get(EPi).getLength()>curr_box2d.getZCoor() &&
//							poslist.get(EPi).getZCoor()+poslist.get(EPi).getLength()<=curr_box2d.getZCoor()+curr_box2d.getLength()&&
//							poslist.get(EPi).getXCoor()+poslist.get(EPi).getWidth()>curr_box2d.getXCoor()&&
//							poslist.get(EPi).getXCoor()+poslist.get(EPi).getWidth()<=curr_box2d.getXCoor()+curr_box2d.getWidth()&&
//							curr_box2d.getXCoor()-poslist.get(EPi).getXCoor()<poslist.get(EPi).getWidth()) {
////							poslist.get(EPi).setWidth(Math.min(poslist.get(EPi).getWidth(),curr_box2d.getXCoor()-poslist.get(EPi).getXCoor()));
//						poslist.get(EPi).setWidth(curr_box2d.getXCoor()-poslist.get(EPi).getXCoor());
//					}
					//2021-2-19检查是否解决问题。(当前箱子把空间的节点覆盖了。），往length移動。
					if(poslist.get(EPi).getZCoor()>=curr_box2d.getZCoor() &&
							poslist.get(EPi).getZCoor()<curr_box2d.getZCoor()+curr_box2d.getLength()&&
							poslist.get(EPi).getXCoor()>=curr_box2d.getXCoor()&&
							poslist.get(EPi).getXCoor()<curr_box2d.getXCoor()+curr_box2d.getWidth()) {
//						ep=new EP(poslist.get(EPi));
//						ep.setWidth(poslist.get(EPi).getXCoor()+poslist.get(EPi).getWidth()-curr_box2d.getXCoor()-curr_box2d.getWidth());
//						ep.setXCoor(curr_box2d.getXCoor()+curr_box2d.getWidth());
//						neweps.add(ep);
//						if(curr_box2d.getXCoor()-poslist.get(EPi).getXCoor()<poslist.get(EPi).getWidth()) {//確保空間>0
						poslist.get(EPi).setLength(poslist.get(EPi).getZCoor()+poslist.get(EPi).getLength()-(curr_box2d.getZCoor()+curr_box2d.getLength()));
//						poslist.get(EPi).setWidth(curr_box2d.getXCoor()-poslist.get(EPi).getXCoor());//這裡會小於等於0，相當於要把這個節點刪除。
						poslist.get(EPi).setZCoor(curr_box2d.getZCoor()+curr_box2d.getLength());
//						}
					}
					//當前box的頂點在當前的空間裡面。
					if(poslist.get(EPi).getZCoor()<curr_box2d.getZCoor()&&
							curr_box2d.getZCoor()<poslist.get(EPi).getZCoor()+poslist.get(EPi).getLength()&&
							poslist.get(EPi).getXCoor()<curr_box2d.getXCoor()&&
							curr_box2d.getXCoor()<poslist.get(EPi).getXCoor()+poslist.get(EPi).getWidth()) {
						ep=new EP(poslist.get(EPi));
						ep.setLength(curr_box2d.getZCoor()-poslist.get(EPi).getZCoor());
						
						if(ep.getLength()*ep.getWidth()>0&&ep.getPlatformID()==0) {
							ep.setPlatformID(curr_box2d.getBoxes().get(0).getPlatformid());//如果原來已經有屬性了呢？?
							neweps.add(ep);
						}
						
						
						poslist.get(EPi).setWidth(curr_box2d.getXCoor()-poslist.get(EPi).getXCoor());
						
					}
					if(poslist.get(EPi).getWidth()*poslist.get(EPi).getLength()<=0) {
						poslist.remove(EPi);EPi--;
					}
				}
				
				
				load_box2d.add(curr_box2d);//添加
				int idx = 0;
				ArrayList<Integer> sameZLevel = new ArrayList<Integer>();
				double maxZLevel = curr_box2d.getZCoor()+curr_box2d.getLength();
				for(idx=0;idx<load_box2d_sort.size();idx++) {//按照Z从大到小排序，X从小到大排序。
					if(Math.abs(load_box2d_sort.get(idx).getZCoor()+load_box2d_sort.get(idx).getLength()-curr_box2d.getZCoor()-curr_box2d.getLength())<5.0) {
						//将小的一个box的ZCoor强制调整为最大的那个box
						//记录这个box idx
						sameZLevel.add(idx);
						if(load_box2d_sort.get(idx).getZCoor()+load_box2d_sort.get(idx).getLength()>maxZLevel) {
							maxZLevel = load_box2d_sort.get(idx).getZCoor()+load_box2d_sort.get(idx).getLength();
						}
						//在同一Z水平上,对比X，从大到小排序。
						if(load_box2d_sort.get(idx).getXCoor()+load_box2d_sort.get(idx).getWidth()<curr_box2d.getXCoor()+curr_box2d.getWidth()) break;
					}else if(load_box2d_sort.get(idx).getZCoor()+load_box2d_sort.get(idx).getLength()<curr_box2d.getZCoor()+curr_box2d.getLength()) {
						break;
					}
				}
				//将相同Z-Level的box的ZCoor强制设置为最大的那个（对齐）
				for(int boxi = 0;boxi<sameZLevel.size();boxi++) {
					//改变她的length和最大的一样长。
					load_box2d_sort.get(sameZLevel.get(boxi)).setLength(Math.ceil(maxZLevel-load_box2d_sort.get(sameZLevel.get(boxi)).getZCoor()));
				}
				curr_box2d.setLength(Math.ceil(maxZLevel-curr_box2d.getZCoor()));//是得所有的这个都是
				load_box2d_sort.add(idx, new Box2D(curr_box2d));
				if(!zlevels.contains(curr_box2d.getZCoor()+curr_box2d.getLength())) {
					zlevels.add(curr_box2d.getZCoor()+curr_box2d.getLength());
				}
				
				
				//第一步先求3DCorners=========================================================
//				ArrayList<Box> Corners3D = new ArrayList<Box>();//如果已经存放的箱子是0，则原点。
//				if(sortedBox.size()<1) {
//					Corners3D.add(new Box());
//				} else {
//				int k=0;//遍历每个Z平面，和Z轴length垂直的平面。
//				for(int k=0;k<horizontal_levels.size() && horizontal_levels.get(k)+curr_box.getLength()<=carriage.getLength();k++) {
//					//得到在这个平面之上的已经存放的boxes,I_k
//					ArrayList<Box> I_k = new ArrayList<Box>();
//					iteratorBox = thissortedBox.iterator();
//					while(iteratorBox.hasNext()) {
//						Box currBox = iteratorBox.next();
//						if(currBox.getZCoor()+currBox.getLength()>horizontal_levels.get(k)) {
//							I_k.add(new Box(currBox));
//						}
//					}
					
					//求2DCorners==========================================================begin
//					if(I_k.size()<1) {
//						//如果这个平面之上没有box,添加原点。
//						Box corner = new Box();
//						corner.setXCoor(0.0);corner.setYCoor(0.0);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
//						Corners3D.add(corner);
//					}else{
					ArrayList<Box2D> I_k = load_box2d_sort;
					if(load_box2d_sort.size()>0) {
						//Phase 1: identify the extreme items e_1,...,e_m
						ArrayList<Integer> e = new ArrayList<Integer>();
						double bar_x = 0.0;//注意I_k是根据z,x排序的。
						for(int i=0;i<I_k.size();i++) {
							if(I_k.get(i).getXCoor()+I_k.get(i).getWidth()>bar_x) {
								e.add(i);bar_x=I_k.get(i).getXCoor()+I_k.get(i).getWidth();//
							}
						}
						//Phase 2: determine the corner points
						double XCoor = 0.0;
						double ZCoor = I_k.get(e.get(0)).getZCoor()+I_k.get(e.get(0)).getLength();
//						if(XCoor+curr_box2d.getWidth()<=width&&ZCoor+curr_box2d.getLength()<=length) {
//							Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
							ep = new EP();
							ep.setXCoor(XCoor);ep.setZCoor(ZCoor);//ep.setPlatformid(k);//记录哪个level
							ep.setLength(length-ZCoor);
							ep.setWidth(width-XCoor);
							neweps.add(ep);
//						}
						/**
						 * 是否添加？
						 */
//						if(I_k.get(e.get(0)).getXCoor()>0.0) {
//							XCoor = I_k.get(e.get(0)).getXCoor();
//						YCoor = I_k.get(e.get(0)).getYCoor()+I_k.get(e.get(0)).getHeight();
//						if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
////							Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
//							Box corner = new Box();
//							corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
//							Corners3D.add(corner);
//						}
//						}
						for(int j=1;j<e.size();j++) {
							XCoor = I_k.get(e.get(j-1)).getXCoor()+I_k.get(e.get(j-1)).getWidth();
							ZCoor = I_k.get(e.get(j)).getZCoor()+I_k.get(e.get(j)).getLength();
//							if(XCoor+curr_box2d.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//								Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
								ep = new EP();
								ep.setXCoor(XCoor);ep.setZCoor(ZCoor);//ep.setPlatformid(k);//记录哪个level
								ep.setLength(length-ZCoor);
								ep.setWidth(width-XCoor);
								neweps.add(ep);
//							}
//							if(I_k.get(e.get(j)).getXCoor()>XCoor) {
//							XCoor = I_k.get(e.get(j)).getXCoor();
//							YCoor = I_k.get(e.get(j)).getYCoor()+I_k.get(e.get(j)).getHeight();
//							if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
////								Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
//								Box corner = new Box();
//								corner.setXCoor(XCoor);corner.setYCoor(YCoor);corner.setZCoor(horizontal_levels.get(k));corner.setPlatformid(k);//记录哪个level
//								Corners3D.add(corner);
//							}
//							}
						}
						XCoor = I_k.get(e.get(e.size()-1)).getXCoor()+I_k.get(e.get(e.size()-1)).getWidth();
						ZCoor = 0.0;
//						if(XCoor+curr_box.getWidth()<=width&&YCoor+curr_box.getHeight()<=height) {
//							Corners2Dx.add(XCoor);Corners2Dy.add(YCoor);
						ep = new EP();
						ep.setXCoor(XCoor);ep.setZCoor(ZCoor);//corner.setPlatformid(k);//记录哪个level
						ep.setLength(length-ZCoor);
						ep.setWidth(width-XCoor);
						neweps.add(ep);
//						}
					}
//				}//for k (each horizontal level)
				
				
				
//				}// if I
				//结束求解3DCorners=========================================================end
				
//				//bottom-left fill (W-axis)
//				for(int epi=0;epi<neweps.length;epi++) {
//					int idx = 0;
//					boolean delete_flag=false;
//					for(idx=0;idx<poslist.size();idx++) {
//						if(neweps[epi].getXCoor()<poslist.get(idx).getXCoor()) {
//							break;
//						}else if(neweps[epi].getXCoor()>poslist.get(idx).getXCoor()) {
//							continue;
//						}else {
//							if(neweps[epi].getZCoor()<poslist.get(idx).getZCoor()) {
//								break;
//							}else if(neweps[epi].getZCoor()>poslist.get(idx).getZCoor()){
//								continue;
//							}else {
//								//相同的点删除。
//								delete_flag=true;
//							}
//						}
//					}
//					if(!delete_flag) {
//						poslist.add(idx, neweps[epi]);
//					}
//				}
				
				//新增的節點增加到哪裡。
				//bottom-left fill (L-axis)
					double bottom_area;
					boolean delete_flag, replace_flag;
				for(int epi=0;epi<neweps.size();epi++) {
					 delete_flag=false;
					 replace_flag=false;
					if(neweps.get(epi).getWidth()*neweps.get(epi).getLength()<=0) {
						delete_flag = true;continue;
					}
					for(idx=0;idx<poslist.size();idx++) {
						if(neweps.get(epi).getZCoor()<poslist.get(idx).getZCoor()) {//按Z軸的從小到大排序。
							break;
						}else if(neweps.get(epi).getZCoor()>poslist.get(idx).getZCoor()) {
							continue;
						}else {
							if(neweps.get(epi).getXCoor()<poslist.get(idx).getXCoor()) {
								break;
							}else if(neweps.get(epi).getXCoor()>poslist.get(idx).getXCoor()){
								continue;
							}else {
								//相同的空間删除。
								if(neweps.get(epi).getWidth()<=poslist.get(idx).getWidth()&&neweps.get(epi).getLength()<=poslist.get(idx).getLength()) {
									delete_flag=true;break;
								}
								else if(neweps.get(epi).getWidth()>=poslist.get(idx).getWidth()&&neweps.get(epi).getLength()>=poslist.get(idx).getLength()){
									replace_flag=true;break;//replace the smaller space.
								}else {
									break;//add in this position. idx
								}
							}
						}
					}
					if(!delete_flag) {
						bottom_area = neweps.get(epi).getLength()*neweps.get(epi).getWidth();
						neweps.get(epi).setBottom_area(bottom_area);
						if(replace_flag) {
							poslist.set(idx, neweps.get(epi));
//							System.out.println("replaced.");
						}
						else {
							poslist.add(idx, neweps.get(epi));
//							System.out.println("replaced.");
						}
					}
				}
				
				//min area heuristic
//				for(int epi=0;epi<neweps.length;epi++) {
//					int idx = 0;
//					boolean delete_flag=false;
//					for(idx=0;idx<poslist.size();idx++) {
//						if(neweps[epi].getXCoor()==poslist.get(idx).getXCoor()&&neweps[epi].getZCoor()==poslist.get(idx).getZCoor()) {
//							//相同的点删除。
//							delete_flag=true;break;
//						}
//						if(neweps[epi].getBottomArea()<poslist.get(idx).getBottomArea()) {
//							break;
//						}else if(neweps[epi].getBottomArea()>poslist.get(idx).getBottomArea()) {
//							continue;
//						}else {
//							if(neweps[epi].getZCoor()<poslist.get(idx).getZCoor())
//								break;
//							else if(neweps[epi].getZCoor()>poslist.get(idx).getZCoor())
//								continue;
//							else
//								if(neweps[epi].getXCoor()<poslist.get(idx).getXCoor())
//									break;
//								else if(neweps[epi].getXCoor()>poslist.get(idx).getXCoor())
//									continue;
//								else {
//									//相同的点删除。
//									delete_flag=true;break;
//								}
//						}
//					}
//					if(!delete_flag) {
//						poslist.add(idx, neweps[epi]);
//					}
//				}
				
				
				
				
			}
			
			//============================================================decode
//			load_box2d.sort(new Comparator<Object>() {
//
//				@Override
//				public int compare(Object o1, Object o2) {
//					Box2D b1=(Box2D)o1,b2=(Box2D)o2;
//					if(b1.getZCoor()<b2.getZCoor()) {
//						return 1;
//					}else if(b1.getZCoor()>b2.getZCoor()) {
//						return -1;
//					}else {
//						if(b1.getXCoor()>b2.getXCoor()) {
//							return 1;
//						}else if(b1.getXCoor()<b2.getXCoor()) {
//							return -1;
//						}else {
//							return 0;
//						}
//					}
//				}
//				
//			});
			//一个Zlevel一个Zlevel来decode
//			Iterator<Double> iteratord = zlevels.iterator();
//			ArrayList<Integer> loadIdx2d = new ArrayList<Integer>();
			this.Boxes = new ArrayList<Box>();
			total_weight = 0.0;
			total_volumn = 0.0;
			if(method==2) {
//			while(iteratord.hasNext()) {
//				double zlevel = iteratord.next();
				for(int idx =0;idx<load_box2d.size();idx++) {
//					if(!loadIdx2d.contains(idx)&&load_box2d.get(idx).getZCoor()+load_box2d.get(idx).getLength()<=zlevel) {
//						loadIdx2d.add(idx);
						//decode
						ArrayList<Box> boxes = load_box2d.get(idx).getBoxes();
						double YCoor = 0.0;
						int direction = load_box2d.get(idx).getDirection();
						for(int boxi=0;boxi<boxes.size();boxi++) {
							Box curr_box = new Box(boxes.get(boxi));
							curr_box.setYCoor(YCoor);
							curr_box.setXCoor(load_box2d.get(idx).getXCoor());
							curr_box.setZCoor(load_box2d.get(idx).getZCoor());
							if(direction==200)
							{
								temp = curr_box.getWidth();
								curr_box.setWidth(curr_box.getLength());
								curr_box.setLength(temp);
							}
							YCoor = YCoor+curr_box.getHeight();
							this.Boxes.add(curr_box);
							total_weight = total_weight + curr_box.getWeight();
							total_volumn = total_volumn + curr_box.getVolume();
							loadIdx.add(curr_box.getId());
						}
//					}
				}
//			}
			}//method1
			if(method==22) {
				
				ArrayList<Integer> platformIDs = new ArrayList<Integer>();
				ArrayList<Integer> n_boxes = new ArrayList<Integer>();
//				ArrayList<Box> next_platform_boxes = new ArrayList<Box>();
				int curr_platform_idx = 0;//指示当前平台的在this.Boxes的添加位置
				int curr_idx = 0;//指示当前平台在n_boxes的位置。
				int curr_platform = 0;
				for(int idx =0;idx<load_box2d.size();idx++) {
					int new_curr_platform = load_box2d.get(idx).getBoxes().get(0).getPlatformid();//最底下那个一定是当前的platform
					if(!platformIDs.contains(new_curr_platform)) {
						curr_platform=new_curr_platform;
						platformIDs.add(curr_platform);
						n_boxes.add(0);
						curr_idx = n_boxes.size()-1;//当前平台在n_boxes和platformIDs中的位置。
						curr_platform_idx = this.Boxes.size();
					}else {
						//已经存在了，并且不会和前面一个平台一样的话，找到当前platform的curr_platform_idx
						if(new_curr_platform!=curr_platform) {
							curr_idx = platformIDs.indexOf(curr_platform);//当前平台在n_boxes和platformIDs中的位置。
							int n_beforeBox = n_boxes.get(curr_idx);//这个平台之前有多少箱子？包含这个平台本身。
							for(int k=0;k<curr_idx;k++) {
								n_beforeBox = n_beforeBox+n_boxes.get(k);
							}
							curr_platform_idx=n_beforeBox;
						}
						
					}
//					if(!loadIdx2d.contains(idx)&&load_box2d.get(idx).getZCoor()+load_box2d.get(idx).getLength()<=zlevel) {
//						loadIdx2d.add(idx);
						//decode
						ArrayList<Box> boxes = load_box2d.get(idx).getBoxes();
//						double YCoor = 0.0;
						for(int boxi=0;boxi<boxes.size();boxi++) {
							Box curr_box = new Box(boxes.get(boxi));
//							curr_box.setYCoor(YCoor);
							curr_box.setXCoor(load_box2d.get(idx).getXCoor()+curr_box.getXCoor());
							curr_box.setZCoor(load_box2d.get(idx).getZCoor()+curr_box.getZCoor());
//							YCoor = YCoor+curr_box.getHeight();
							if(curr_box.getPlatformid()==curr_platform) {
								this.Boxes.add(curr_platform_idx,curr_box);//在这个位置插入。
								curr_platform_idx++;
								n_boxes.set(curr_idx, n_boxes.get(curr_idx)+1);
							} else {
								//如果不是当前的platform，找到插入的位置，
								if(platformIDs.contains(curr_box.getPlatformid())) {//如果已经加入到platformIDs里面了
									int i = platformIDs.indexOf(curr_box.getPlatformid());
									int n_beforeBox = n_boxes.get(i);//这个平台之前有多少箱子？包含这个平台本身。
									for(int k=0;k<i;k++) {
										n_beforeBox = n_beforeBox+n_boxes.get(k);
									}
									this.Boxes.add(n_beforeBox,curr_box);
									n_boxes.set(i, n_boxes.get(i)+1);
								}else {
									this.Boxes.add(curr_box);
									//首次加入。
									platformIDs.add(curr_box.getPlatformid());
									n_boxes.add(1);
								}
							}
							total_weight = total_weight + curr_box.getWeight();
							total_volumn = total_volumn + curr_box.getVolume();
							loadIdx.add(curr_box.getId());
						}
//					}
				}
			}
				if(method==3) {
//					for(int idx=0;idx<boxSequenc)
//					ArrayList<Integer> platformIDs = new ArrayList<Integer>();
//					ArrayList<Integer> n_boxes = new ArrayList<Integer>();
//					ArrayList<Box> next_platform_boxes = new ArrayList<Box>();
//					int curr_platform_idx = 0;//指示当前平台的在this.Boxes的添加位置
//					int curr_idx = 0;//指示当前平台在n_boxes的位置。
//					int curr_platform = 0;
//					ArrayList<Box> thisBoxes = new ArrayList<Box>();//没有排序的集合
					for(int idx =0;idx<load_box2d.size();idx++) {
//						int new_curr_platform = load_box2d.get(idx).getBoxes().get(0).getPlatformid();//最底下那个一定是当前的platform
//						if(!platformIDs.contains(new_curr_platform)) {
//							curr_platform=new_curr_platform;
//							platformIDs.add(curr_platform);
//							n_boxes.add(0);
//							curr_idx = n_boxes.size()-1;//当前平台在n_boxes和platformIDs中的位置。
//							curr_platform_idx = this.Boxes.size();
//						}else {
//							//已经存在了，并且不会和前面一个平台一样的话，找到当前platform的curr_platform_idx
//							if(new_curr_platform!=curr_platform) {
//								curr_idx = platformIDs.indexOf(curr_platform);//当前平台在n_boxes和platformIDs中的位置。
//								int n_beforeBox = n_boxes.get(curr_idx);//这个平台之前有多少箱子？包含这个平台本身。
//								for(int k=0;k<curr_idx;k++) {
//									n_beforeBox = n_beforeBox+n_boxes.get(k);
//								}
//								curr_platform_idx=n_beforeBox;
//							}
//							
//						}
//						if(!loadIdx2d.contains(idx)&&load_box2d.get(idx).getZCoor()+load_box2d.get(idx).getLength()<=zlevel) {
//							loadIdx2d.add(idx);
							//decode
							ArrayList<Box> boxes = load_box2d.get(idx).getBoxes();
//							double YCoor = 0.0;
							int direction = load_box2d.get(idx).getDirection();
							for(int boxi=0;boxi<boxes.size();boxi++) {
								Box curr_box = new Box(boxes.get(boxi));
//								curr_box.setYCoor(YCoor);
								if(direction==200) {
									temp = curr_box.getWidth();
									curr_box.setWidth(curr_box.getLength());
									curr_box.setLength(temp);
									double originX = curr_box.getXCoor();
									double originZ = curr_box.getZCoor();
									curr_box.setXCoor(originZ);
									curr_box.setZCoor(load_box2d.get(idx).getLength()-originX-curr_box.getLength());
//									if(curr_box.getXCoor()==0&&curr_box.getZCoor()==0) {
//									curr_box.setXCoor(load_box2d.get(idx).getXCoor());
//									curr_box.setZCoor(load_box2d.get(idx).getZCoor()+load_box2d.get(idx).getLength()-curr_box.getLength());
//									}else {
										//the new origin point (load_box2d.get(idx).getXCoor(),load_box2d.get(idx).getZCoor()+load_box2d.get(idx).getLength())
//										curr_box.setXCoor(load_box2d.get(idx).getXCoor()+curr_box.getZCoor());
//										curr_box.setZCoor(load_box2d.get(idx).getZCoor()+load_box2d.get(idx).getLength()-curr_box.getXCoor()-curr_box.getLength());
//									}
								}
								curr_box.setXCoor(load_box2d.get(idx).getXCoor()+curr_box.getXCoor());
								curr_box.setZCoor(load_box2d.get(idx).getZCoor()+curr_box.getZCoor());
//								YCoor = YCoor+curr_box.getHeight();
//								if(curr_box.getPlatformid()==curr_platform) {
//									this.Boxes.add(curr_platform_idx,curr_box);//在这个位置插入。
//									curr_platform_idx++;
//									n_boxes.set(curr_idx, n_boxes.get(curr_idx)+1);
//								} else {
									//如果不是当前的platform，找到插入的位置，
//									if(platformIDs.contains(curr_box.getPlatformid())) {//如果已经加入到platformIDs里面了
//										int i = platformIDs.indexOf(curr_box.getPlatformid());
//										int n_beforeBox = n_boxes.get(i);//这个平台之前有多少箱子？包含这个平台本身。
//										for(int k=0;k<i;k++) {
//											n_beforeBox = n_beforeBox+n_boxes.get(k);
//										}
//										this.Boxes.add(n_beforeBox,curr_box);
//										n_boxes.set(i, n_boxes.get(i)+1);
//									}else {
//									this.Boxes.set(curr_box.getId(), curr_box);
								//从后往前找到相同平台的地方加入。
								
								this.Boxes.add(curr_box);
//										//首次加入。
//										platformIDs.add(curr_box.getPlatformid());
//										n_boxes.add(1);
//									}
//								}
								total_weight = total_weight + curr_box.getWeight();
								total_volumn = total_volumn + curr_box.getVolume();
								loadIdx.add(curr_box.getId());
							}
//						}
					}
					//根据boxid进行排序。進來的時候是什麼順序，出去的時候還是這個順序。
					this.Boxes.sort(new Comparator<Box>() {
						public int compare(Box b1, Box b2) {
							if(b1.getId()<b2.getId())
								return -1;
							else if(b1.getId()>b2.getId())
								return 1;
							else
								return 0;
						}
						
					});
				//check
				if(DEBUG) {
				ArrayList<Integer> platformIDs = new ArrayList<Integer>();
				int curr_platform=this.Boxes.get(0).getPlatformid();
				platformIDs.add(curr_platform);
				for(int boxi=0;boxi<this.Boxes.size();boxi++) {
					if(this.Boxes.get(boxi).getPlatformid()!=curr_platform) {
						int new_platform = this.Boxes.get(boxi).getPlatformid();
						if(platformIDs.contains(new_platform))
							System.out.println("There two same platforms in one route!!!!");
						platformIDs.add(new_platform);
						curr_platform=new_platform;
					}
				}
				}
			}
//			this.setLoadWeight(total_weight);
			this.loadWeight = total_weight;
			this.loadVolumn = total_volumn;
			if(DEBUG)
			carriage.setTruckTypeCode("zqlbpp"+method);
			return loadIdx;
		}

			
		
			
			/**
			 * 可以调换顺序的。可以用来排一个平台里面的箱子。
			 * @param boxingSequence1
			 * @return
			 */
			public ArrayList<Integer> dblf_node(final ArrayList<Box> boxingSequence,Carriage carriage) {
//				double width = 355*1;//carriage.getWidth();
//				double length = 385*2;//carriage.getLength();
//				double height = 100*4;//carriage.getHeight();
				double width = carriage.getWidth();
				double length = carriage.getLength();
				double height = carriage.getHeight();
				double capacity = carriage.getCapacity();
			ArrayList<Integer> loadIdx=new ArrayList<Integer>();
//			ArrayList<Box> loadBoxes = new ArrayList<Box>();
			this.loadWeight=0.0;
			this.loadVolumn=0.0;
			this.Boxes = new ArrayList<Box>();
			//********* insert two empty box to boxingSequence
//			ArrayList<Box> boxingSequence = new ArrayList<Box>();
			Box zeroBox = new Box();
//			boxTop.setNumber(0);
			zeroBox.setHeight(0.0);
			zeroBox.setLength(0.0);
			zeroBox.setWidth(0.0);
			zeroBox.setXCoor(0.0);
			zeroBox.setYCoor(0.0);
			zeroBox.setZCoor(0.0);
//			boxingSequence.add(boxBack);
//			boxingSequence.add(boxTop);
//			for (Box box : boxingSequence1) {
//				Box box1 = new Box(box);
//				box1.setHeight(box.getHeight());
//				box1.setLength(box.getLength());
////				box1.setNumber(box.getNumber());
//				box1.setWidth(box.getWidth());
//				box1.setXCoor(box.getXCoor());
//				box1.setYCoor(box.getYCoor());
//				box1.setZCoor(box.getZCoor());
//				boxingSequence.add(box1);
//			}
			//***********************插入两个0箱子
//			double loaded_weights = 0;
			ArrayList<Box> left = new ArrayList<Box>();//LeftSequence
			ArrayList<Box> back = new ArrayList<Box>();//BackSequence
			ArrayList<Box> top = new ArrayList<Box>();//TopSequence
			//在back和top各插入一个0箱子
			back.add(new Box(zeroBox));
			top.add(new Box(zeroBox));
			
			//在原点插入第一个箱子
			Box curr_box = new Box(boxingSequence.get(0));
			curr_box.setXCoor(0.0);
			curr_box.setYCoor(0.0);
			curr_box.setZCoor(0.0);
			
			back.add(new Box(curr_box));
			left.add(new Box(curr_box));
			this.Boxes.add(new Box(curr_box));
			this.loadWeight=this.loadWeight+boxingSequence.get(0).getWeight();
			this.loadVolumn=this.loadVolumn+boxingSequence.get(0).getVolume();
//			loadBoxes.add(new Box(boxingSequence.get(0)));
			top.add(new Box(curr_box));
			loadIdx.add(0);//第一个箱子被装载了。
			//计算其他箱子的坐标。
			for(int i=1;i<boxingSequence.size();i++) {
				curr_box = new Box(boxingSequence.get(i));
				boolean insertConfirm = false;//begin to insert i-th box, insertConfirm to indicate whether this insertion is successful.
				if(this.loadWeight+curr_box.getWeight()>capacity)
					break;
				for(int j=0;j<back.size();j++) {
					if(back.get(j).getZCoor()+back.get(j).getLength()+curr_box.getLength()<=length) {
						curr_box.setZCoor(back.get(j).getZCoor()+back.get(j).getLength());
						//表示在backBox[j].Z+backBox[j].length这个坐标有空间来存放当前box[i]
					}
					else continue;
					for(int k=0;k<top.size();k++) {
						if(top.get(k).getYCoor()+top.get(k).getHeight()+curr_box.getHeight()<=height) {
							curr_box.setYCoor(top.get(k).getYCoor()+top.get(k).getHeight());
							//表示在topBox[k].Y+topBox[k].height这个坐标平面有空间来存放当前box[i]
						}
						else continue;
						curr_box.setXCoor(0);
						for(int p = 0;p<=left.size();p++) {//left裡面表示已經存放的箱子。
							//因为XCoor是从小到大排列的，所以就停止就好了。
							//遍历所有已经存放的box
							boolean flag = false;//表示這個位置需不需要進行驗證。
							if(p==left.size()||curr_box.getXCoor()+curr_box.getWidth()>width)
								flag = true;//p==left.size已經移動到X方向的最右邊了。
							else if(curr_box.getXCoor()+curr_box.getWidth()<=left.get(p).getXCoor())
								flag = true;
							else if(curr_box.getXCoor()<left.get(p).getXCoor()+left.get(p).getWidth()
									&&curr_box.getYCoor()<left.get(p).getYCoor()+left.get(p).getHeight()
									&&curr_box.getZCoor()<left.get(p).getZCoor()+left.get(p).getLength()
									) {//被已經存放的(上一個）箱子覆蓋了，則不需要判斷。/*這裡left也不能全按從小到大的順序排，也要按platform來排序。*/
								curr_box.setXCoor(left.get(p).getXCoor()+left.get(p).getWidth());
							}
							if(flag) {
								//这里得开始判断是否 能夠支撐。
								if(curr_box.getXCoor()+curr_box.getWidth()<=width) {
									//判断这个位置能不能站稳
									//当前箱子的坐标： boxingSequence.x,y,z
									//当前箱子的底部高度：boxingSequence.y，如果为0的话，就可以了
									//遍历所有的已经放了的箱子，看是否支撑现在的箱子。（暴力了点）
									boolean support = false;
									if(curr_box.getYCoor()==0) {
										support = true;
									}else{
										//计算该箱子的底部面积。
//										Box currBox = curr_box;
										double bottomArea = curr_box.getWidth()*curr_box.getLength();
										double curr_y = curr_box.getYCoor();//+boxingSequence.get(i).getHeight();
										double crossArea = 0;
										//计算所有已放箱子的顶部与该箱子的底部交叉面积
										for (int boxi=0;boxi<left.size();boxi++) {
											//如果这个箱子的顶部与boxingSequence.get(i)的底部在同一水平上
											Box existBox = left.get(boxi);
											if(Math.abs(existBox.getYCoor()+existBox.getHeight()-curr_y)<=0) {
												double xc=curr_box.getXCoor(),zc=curr_box.getZCoor(),xe=existBox.getXCoor(),ze=existBox.getZCoor();
												double wc=curr_box.getWidth(),lc=curr_box.getLength(),we=existBox.getWidth(),le=existBox.getLength();
												
												if(!((xc+wc<xe)||(xe+we<xc)||(zc+lc<ze)||(ze+le<zc))) {
													double [] XCoor = {xc,xc+wc,xe,xe+we};
													double [] ZCoor = {zc,zc+lc,ze,ze+le};
													//sort xc,xc+wc,xe,xe+we
													 Arrays.sort(XCoor);
													 Arrays.sort(ZCoor);
													//sort zc,zc+lc,ze,ze+le
													 crossArea = crossArea + Math.abs(XCoor[2]-XCoor[1])*Math.abs(ZCoor[2]-ZCoor[1]);
													 if(crossArea>0.8*bottomArea) {support=true;break;}
												}
//												if((xc+wc>xe)&&(ze+le>zc)) {
//													crossArea = crossArea+Math.min(xc+wc-xe,wc)*Math.min(ze+le-zc,lc);
//												}
//												if((xe+we>xc)&&(zc+lc>ze)) {
//													crossArea = crossArea+Math.min(xe+we-xc,wc)*Math.min(zc+lc-ze,lc);
//												}
												//判断交叉面积。
												
											}
										}
										
									}
									//
									if(support) {
										this.Boxes.add(curr_box);
										
//										loadBoxes.add(new Box(boxingSequence.get(i)));
										this.loadWeight = this.loadWeight + curr_box.getWeight();
										this.loadVolumn=this.loadVolumn+curr_box.getVolume();
										
										//left按platform的先後，然後按箱子的X從小到大排
										//同一個值，則按先後順序。
										int idx = 0;
										for(idx=0;idx<left.size();idx++) {//left裡面是已經存的箱子。
											if(curr_box.getXCoor() < left.get(idx).getXCoor()){
												break;
											}
										}
										left.add(idx,new Box(curr_box));
										
										
										//back,top都是按Z,Y从小到大进行排序的。
										boolean add_flag = true;
//										for(idx=0;idx<back.size();idx++) {
//											if(Math.abs(curr_box.getZCoor()+curr_box.getLength() - back.get(idx).getZCoor()-back.get(idx).getLength())<=1) {
//												add_flag=false;break;
//											}else if(curr_box.getZCoor()+curr_box.getLength() < back.get(idx).getZCoor()+back.get(idx).getLength()) {
//												break;
//											}
//										}
										for(idx=0;idx<back.size();idx++) {
											if(Math.abs(curr_box.getZCoor()+curr_box.getLength() - back.get(idx).getZCoor()-back.get(idx).getLength())<=0) {
												add_flag=false;break;
											}else if(curr_box.getZCoor()+curr_box.getLength() < back.get(idx).getZCoor()+back.get(idx).getLength()) {
												break;
											}
										}
										if(add_flag)
										back.add(idx, new Box(curr_box));//x-y plane
										
										add_flag=true;
//										for(idx=0;idx<top.size();idx++) {
//											if(Math.abs(curr_box.getYCoor()+curr_box.getHeight() - top.get(idx).getYCoor()-top.get(idx).getHeight())<=0) {
//												add_flag=false;break;
//											}else if(curr_box.getYCoor()+curr_box.getHeight() < top.get(idx).getYCoor()+top.get(idx).getHeight()) {
//												break;
//											}
//										}
										for(idx=0;idx<top.size();idx++) {
											if(Math.abs(curr_box.getYCoor()+curr_box.getHeight() - top.get(idx).getYCoor()-top.get(idx).getHeight())<=0) {
												add_flag=false;break;
											}else if(curr_box.getYCoor()+curr_box.getHeight() < top.get(idx).getYCoor()+top.get(idx).getHeight()) {
												break;//在兩個相差比較大的時候，插入。如果相差不大，則按先後順序。
											}
										}
										if(add_flag)
										top.add(idx,new Box(curr_box));//x-z plane
										/**
										 * left用来确定XCoor
										 */
										//left.leftSort();
										
//										for(int bi=0;bi<left.size()-1;bi++) {
//											for(int bj=0;bj<left.size()-bi-1;bj++) {
//												if(left.get(bj).getXCoor()>left.get(bj+1).getXCoor()) {
////													if(left.get(bj).getPlatformid()==left.get(bj+1).getPlatformid())
//														Collections.swap(left, bj, bj+1);
//												}
//											}
//										}
										//将boxingSequence.get(i).getXCoor插入到left里面。
										/**
										 * back用来确定ZCoor
										 */
										//back.backSort();
//										for(int bi=0;bi<back.size()-1;bi++) {
//											for(int bj=0;bj<back.size()-bi-1;bj++) {
//												if(back.get(bj).getZCoor()+back.get(bj).getLength()>
//												back.get(bj+1).getZCoor()+back.get(bj+1).getLength()) {
////													if(back.get(bj).getPlatformid()==back.get(bj+1).getPlatformid())
//														Collections.swap(back, bj, bj+1);
//												}
//											}
//										}
										/**
										 * top用来确定YCoor
										 */
										//top.topSort();
//										for(int bi=0;bi<top.size()-1;bi++) {
//											for(int bj=0;bj<top.size()-bi-1;bj++) {
//												if(top.get(bj).getYCoor()+top.get(bj).getHeight()>
//												top.get(bj+1).getYCoor()+top.get(bj+1).getHeight()) {
////													if(top.get(bj).getPlatformid()==top.get(bj+1).getPlatformid())
//														Collections.swap(top, bj, bj+1);
//												}
//											}
//										}
										insertConfirm=true;
										loadIdx.add(i);//第i-2+1个箱子被装载了。
									}
								}//if < width
								break;
							}//flag=true
						}//for p
						if(insertConfirm)
							break;
					}
					if(insertConfirm)
						break;
				}
			}
//			this.Boxes = left;
			//calculate excessWeight
//			if(this.loadWeight>capacity) {this.excessWeight=this.loadWeight-carriage.getCapacity();}else {this.excessWeight=0;}
			//calculate excessLength.
//			if(back.get(back.size()-1).getZCoor()
//					+back.get(back.size()-1).getLength()>length)
//				this.excessLength = back.get(back.size()-1).getZCoor()
//						+back.get(back.size()-1).getLength()-length;
//			else
//				this.excessLength = 0;
//			System.out.println("excessLength:"+this.excessLength+";excessWeight:"+this.excessWeight);
//			if(left.leftSequence.size()<boxingSequence1.size())
//			System.out.println("input box size:"+boxingSequence1.size()+"this vehicle size:"+this.Boxes.size());
			if(DEBUG)
			carriage.setTruckTypeCode("dblf_node");
			return loadIdx;//left.leftSequence.size();
		}
			
			/************跟车箱有关的变量************/
		public  ArrayList<Box> get_Boxes (){
			return this.Boxes;
		}
		
		public double get_loadWeight() {
			return this.loadWeight;
		}
		
		public double get_loadVolumn() {
			return this.loadVolumn;
		}

//			private ArrayList<Box> allBoxes = new ArrayList<Box>();
//			private ArrayList<Double> horizontal_levels = new ArrayList<Double>();
//			private double loadWeight;
//			private double loadVolumn;
//			private double excessWeight;
//			private double excessLength;//当前转载长度。

			

}

package solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import solver.util.Box;
import solver.util.Carriage;
import solver.util.EP;
import solver.util.Node;

/**
 * 路线类
 * @author dell
 *
 */
public class Route implements Cloneable{
	private final static boolean DEBUG = false;
	private int id;//
	private double distance;//
	private LinkedList<Node> nodes;//
	
	private Carriage carriage;
	
	
	
	/************跟车箱有关的变量************/
	private ArrayList<Box> Boxes = new ArrayList<Box>();
	private ArrayList<Box> sortedBoxes = new ArrayList<Box>();//sort Boxes according to y and x
	/**
	 * 存放所有clients的box序列
	 */
	private ArrayList<Box> allBoxes = new ArrayList<Box>();
	private ArrayList<Double> horizontal_levels = new ArrayList<Double>();
	private double loadWeight;
	private double loadVolumn;
	private double excessWeight;
	private double excessLength;//当前转载长度。
	
	public Route() {
		this.horizontal_levels = new ArrayList<Double>();
		this.horizontal_levels.add(0.0);
		excessLength=0.0;//初始转载长度为0
	}
	
	public Route(int id) {
		this.id = id;
		this.horizontal_levels = new ArrayList<Double>();
		this.horizontal_levels.add(0.0);
		excessLength = 0.0;//初始转载长度为0
	}
	
	public Route(Route r) {
		this.id = r.id;
		this.distance=r.distance;
		this.nodes = new LinkedList<Node>();
		for(Node node:r.getNodes())
			this.nodes.add(new Node(node));
//		this.boxStartIdx = new ArrayList<Integer>();
//		this.boxNum = new ArrayList<Integer>();
//		for(int nodei=0;nodei<r.nodes.size();nodei++) {
//			this.nodes.add(new Node(r.nodes.get(nodei)));
////			this.boxStartIdx.add(r.boxStartIdx.get(nodei));
////			this.boxNum.add(r.boxNum.get(nodei));
//		}
		this.carriage = new Carriage(r.getCarriage());
		
//		Iterator<Box> iteratorBox = r.Boxes.iterator();
		for(Box b:r.getBoxes()) {
			this.Boxes.add(new Box(b));
		}
//		iteratorBox = r.sortedBoxes.iterator();
		for(Box b:r.sortedBoxes) {
			this.sortedBoxes.add(new Box(b));
		}
//		Iterator<Double> iteratorDouble = r.horizontal_levels.iterator();
		for(Double d:r.horizontal_levels) {
			this.horizontal_levels.add(d);
		}
		this.loadWeight=r.loadWeight;
		this.loadVolumn=r.loadVolumn;
		this.excessWeight=r.excessWeight;
		this.excessLength=r.excessLength;
		
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public double getDistance() {
		return distance;
	}


	public void setDistance(double distance) {
		this.distance = distance;
	}


	public LinkedList<Node> getNodes() {
		return nodes;
	}


	public void setNodes(LinkedList<Node> nodes) {
		this.nodes = nodes;
	}
	
	public Carriage getCarriage() {
		return carriage;
	}
	
	public void setCarriage(Carriage carriage) {
		this.carriage = carriage;
	}
	
	public ArrayList<Box> getBoxes() {
		return Boxes;
	}

	public void setBoxes(ArrayList<Box> boxes) {
		this.Boxes = boxes;
	}
	
	public ArrayList<Box> getSortedBoxes() {
		return sortedBoxes;
	}

	public void setSortedBoxes(ArrayList<Box> boxes) {
		this.sortedBoxes = boxes;
	}
	
//	public void 
	public double getLoadWeight() {
		return loadWeight;
	}

	public void setLoadWeight(double loadWeight) {
		this.loadWeight = loadWeight;
	}
	
	public double getExcessWeight() {
		return excessWeight;
	}

	public void setExcessWeight(double excessWeight) {
		this.excessWeight = excessWeight;
	}

	public double getExcessLength() {
		return excessLength;
	}

	public void setExcessLength(double excessLength) {
		this.excessLength = excessLength;
	}
	
	public double getWeightRatio() {
		return this.loadWeight/this.carriage.getCapacity();
	}
	
	public double getLoadVolume() {
		Iterator<Box> iterator = this.Boxes.iterator();
		double volume=0.0;
		while(iterator.hasNext()) {
			Box box = iterator.next();
			volume = volume + box.getLength()*box.getWidth()*box.getHeight();
		}
		return volume;
//		return this.height*this.width*this.length;
	}

	public double getLoadWeight1() {
		Iterator<Box> iterator = this.Boxes.iterator();
		double volume=0.0;
		while(iterator.hasNext()) {
			Box box = iterator.next();
			volume = volume + box.getWeight();
		}
		return volume;
//		return this.height*this.width*this.length;
	}
	
	public double getVolumeRatio() {
		return this.getLoadVolume()/this.carriage.getTruckVolume();
	}
	
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		
		Route route = null;
		try {
			route = (Route)super.clone();
		}catch(CloneNotSupportedException e) {
			e.printStackTrace();
		}
		LinkedList<Node> nodes1 = new LinkedList<Node>();
		Iterator<Node> iterator = nodes.iterator();
		while(iterator.hasNext()) {
			nodes1.add((Node) iterator.next().clone());
		}
		route.setNodes(nodes1);
		return route;
	}

	
	
	/**
	 * 
	 * @param bk - The current insert box
	 * @param bi - the box i
	 * @param ZYX - the direction.
	 */
	public boolean CanTakeProjection(Box bk,Box bi,int ZYX) {
		double x_k=bk.getXCoor(),y_k=bk.getYCoor(),z_k=bk.getZCoor();
		double w_k=bk.getWidth(),h_k=bk.getHeight(),l_k=bk.getLength();
		double x_i=bi.getXCoor(),y_i=bi.getYCoor(),z_i=bi.getZCoor();
		double w_i=bi.getWidth(),h_i=bi.getHeight(),l_i=bi.getLength();
		//添加6个extreme point
		//(curr_position.getXCoor()+curr_box.getWidth(),curr_position.getYCoor(),curr_position.getZCoor())
		// (x_k+w_k,y_k,z_k) in the direction of the Y axes
		//CanTakeProjection就是这个Corner point会穿过这个box
//		if(CanTakeProjection(loadBox,bi,YX)&&bi.getXCoor()+bi.getWidth()>maxBound[YX]) {
//			;
//		}
		// (x_k+w_k,y_k,z_k) in the direction of the Z axes
		
		//(curr_position.getXCoor(),curr_position.getYCoor()+curr_box.getHeight(),curr_position.getZCoor())
		// (x_k,y_k+d_k,z_k) in the direction of the X axes
		
		// (x_k,y_k+d_k,z_k) in the direction of theZ axes
		
		//(curr_position.getXCoor(),curr_position.getYCoor(),curr_position.getZCoor()+curr_box.getLength())
		// (x_k,y_k,z_k+h_k) in the direction of the X axes
		
		// (x_k,y_k,z_k+h_k) in the direction of the Y axes
		switch (ZYX) {
		//ep (x_k,y_k+h_k,z_k)->Y轴上的extreme point
		case 0://YX
			//(y_k+h_k,z_k)是否落在(y_i,z_i),(y_i+h_i,z_i),(y_i+h_i,z_i+l_i),(y_i,z_i+l_i)
			// (x_i+w_i,y_k+d_k,z_k) in the direction of the Y axes
			if(y_k+h_k>y_i&&y_k+h_k<y_i+h_i&&z_k>z_i&&z_k<z_i+l_i)
				return true;
		case 1://YZ
			if(x_k>x_i&&x_k<x_i+w_i&&y_k+h_k>y_i&&y_k+h_k<y_i+h_i)//(x_k,y_k+h_k)是否落在(x_i,y_i),(x_i+w_i,y_i+h_i)
				return true;
		case 2://XY->(x_k+w_k,y_k,z_k)映射Y方向。
			if(x_k+w_k>x_i&&x_k+w_k<x_i+w_i&&z_k>z_i&&z_k<z_i+l_i)
				return true;
		case 3: //XZ->(x_k+w_k,y_k,z_k)映射Z方向。
			if(x_k+w_k>x_i&&x_k+w_k<x_i+w_i&&y_k>y_i&&y_k<y_i+h_i)
				return true;
		case 4: //ZX->(x_k,y_k,z_k+l_k)映射X方向。
			if(y_k>y_i&&y_k<y_i+h_i&&z_k+l_k>z_i&&z_k+l_k<z_i+l_i)
				return true;
		case 5: //ZY->(x_k,y_k,z_k+l_k)映射Y方向。
			if(x_k>x_i&&x_k<x_i+w_i&&z_k+l_k>z_i&&z_k+l_k<z_i+l_i)
				return true;
		default:
			System.out.println("this is wrong.");
			return false;
		}
	}
	/**
	 * Extreme point insertion using first fit strategy.
	 * input: a set of boxes
	 */
	
	public ArrayList<Integer> ep_insertion(ArrayList<Box> boxingSequence) {
		double width = this.carriage.getWidth();
		double length = this.carriage.getLength();
		double height = this.carriage.getHeight();
		double capacity = this.carriage.getCapacity();
		ArrayList<Integer> loadIdx = new ArrayList<Integer>();
		ArrayList<Box> thisBoxes = new ArrayList<Box>();//按顺序保存该箱子。
		ArrayList<EP> extremePoints = new ArrayList<EP>();
		double thisloadWeight = 0.0;
		
		boolean insertConfirm;//是否成功插入当前箱子。

		for(int boxi=0;boxi<boxingSequence.size();boxi++) {
			Box curr_box = boxingSequence.get(boxi);
			if(thisloadWeight + curr_box.getWeight()>capacity) {
				break;//当前箱子不能再加入这辆车了，退出寻找最优位置，并且退出遍历箱子。
			}
			insertConfirm=false;
			if(extremePoints.size()<1) {
//			Corners3D.add(new Box());
				Box loadbox = new Box(curr_box);
				loadbox.setXCoor(0.0);
				loadbox.setYCoor(0.0);
				loadbox.setZCoor(0.0);
				thisBoxes.add(loadbox);
				loadIdx.add(boxi);
				thisloadWeight = thisloadWeight+loadbox.getWeight();
//				extremeBox.add(loadbox);//
				//添加extreme points
				
				//Y
				EP b = new EP();
				b.setXCoor(0.0);
				b.setYCoor(curr_box.getHeight());
				b.setZCoor(0.0);
				b.setHeight(height-curr_box.getHeight());
				b.setWidth(width);
				b.setLength(length);
				extremePoints.add(b);
				
				
				//X
				b = new EP();
				b.setXCoor(curr_box.getWidth());
				b.setYCoor(0.0);
				b.setZCoor(0.0);
				b.setHeight(height);
				b.setWidth(width-curr_box.getWidth());
				b.setLength(length);
				extremePoints.add(b);
				
				//Z
				b = new EP();
				b.setXCoor(0.0);
				b.setYCoor(0.0);
				b.setZCoor(curr_box.getLength());
				b.setHeight(height);
				b.setWidth(width);
				b.setLength(length-curr_box.getLength());
				extremePoints.add(b);
				
				
				continue;
			}
			
			//if volume of item i is less than the space remaining in b
			for(int positioni=0;positioni<extremePoints.size();positioni++) {//for each extreme points
				//if item i can be placed at point p
				//insertConfirm = true;
				//assign coor to this box
				EP curr_position = new EP(extremePoints.get(positioni));
//				if(method==1&&curr_position.getXCoor()>10&&(curr_position.getZCoor()+curr_box.getLength()>horizontal_levels.get(horizontal_levels.size()-1))) {
//					continue;
//				}
				if(curr_box.getWidth()<=curr_position.getWidth()&&
						curr_box.getHeight()<=curr_position.getHeight()&&
								curr_box.getLength()<=curr_position.getLength()&&
								curr_position.getXCoor()+curr_box.getWidth()<=width&&
								curr_position.getYCoor()+curr_box.getHeight()<=height&&
								curr_position.getZCoor()+curr_box.getLength()<=length) {
					//如果这个地方的空间足够。
					
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
						for (int boxii=0;boxii<thisBoxes.size();boxii++) {
							//如果这个箱子的顶部与boxingSequence.get(i)的底部在同一水平上
							Box existBox = thisBoxes.get(boxii);
							
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
						
						
						double x_k=loadBox.getXCoor(),y_k=loadBox.getYCoor(),z_k=loadBox.getZCoor();
						double w_k=loadBox.getWidth(),h_k=loadBox.getHeight(),l_k=loadBox.getLength();
						
						
						
						//覆盖curr_position
						extremePoints.remove(positioni);
						
						//Update Residual Space.
						//放了箱子之后，其他EP是否空间变化了。
						for(int EPi=0;EPi<extremePoints.size();EPi++) {
							//
							if(extremePoints.get(EPi).getYCoor()>=loadBox.getYCoor() &&
									extremePoints.get(EPi).getYCoor()<loadBox.getYCoor()+loadBox.getHeight()) {
							//当这个extremepoint的XZ平面在这个box上下平面之间时：
								//isOnSide(nItem,Z)
								if(extremePoints.get(EPi).getXCoor()<=loadBox.getXCoor()&&
										extremePoints.get(EPi).getZCoor()<loadBox.getZCoor()+loadBox.getLength()&&
								   extremePoints.get(EPi).getZCoor()+extremePoints.get(EPi).getLength()>loadBox.getZCoor())//isOnSide(nItem,Z)
									extremePoints.get(EPi).setWidth(Math.min(extremePoints.get(EPi).getWidth(), loadBox.getXCoor()-extremePoints.get(EPi).getXCoor()));
								
								if(extremePoints.get(EPi).getZCoor()<=loadBox.getZCoor()&&
										extremePoints.get(EPi).getXCoor()<loadBox.getXCoor()+loadBox.getWidth()&&
										extremePoints.get(EPi).getXCoor()+extremePoints.get(EPi).getWidth()>loadBox.getXCoor())//isOnSide(nItem,X)
									extremePoints.get(EPi).setLength(Math.min(extremePoints.get(EPi).getLength(),loadBox.getZCoor()-extremePoints.get(EPi).getZCoor()));
							}
							if(extremePoints.get(EPi).getYCoor()<=loadBox.getYCoor()&&
									extremePoints.get(EPi).getZCoor()<loadBox.getZCoor()+loadBox.getLength()&&
									extremePoints.get(EPi).getZCoor()+extremePoints.get(EPi).getLength()>loadBox.getZCoor()&&
									extremePoints.get(EPi).getXCoor()<loadBox.getXCoor()+loadBox.getWidth()&&
									extremePoints.get(EPi).getXCoor()+extremePoints.get(EPi).getWidth()>loadBox.getXCoor()) {
								//当这个extreme Point的XZ平面在这个box的下方时：改变height
								extremePoints.get(EPi).setHeight(Math.min(extremePoints.get(EPi).getHeight(), loadBox.getYCoor()-extremePoints.get(EPi).getYCoor()));
							}
						}
						
						
						//新增6个新的ep
						EP[] neweps = new EP[6];
						double [] maxBound = {-1,-1,-1,-1,-1,-1};
						int YX=0,YZ=1,XY=2,XZ=3,ZX=4,ZY=5;
						//YX
						EP newep = new EP(curr_position);
						newep.setXCoor(0.0);
						newep.setYCoor(curr_position.getYCoor()+curr_box.getHeight());
						newep.setZCoor(curr_position.getZCoor());
						newep.setHeight(curr_position.getHeight()-curr_box.getHeight());
						newep.setWidth(curr_position.getWidth());
						newep.setLength(curr_position.getLength());
						neweps[YX] = newep;
						maxBound[YX] = 0;
						//YZ
						newep = new EP(curr_position);
						newep.setXCoor(curr_position.getXCoor());
						newep.setYCoor(curr_position.getYCoor()+curr_box.getHeight());
						newep.setZCoor(0.0);
						newep.setHeight(curr_position.getHeight()-curr_box.getHeight());
						newep.setLength(curr_position.getLength());
						newep.setWidth(curr_position.getWidth());
						neweps[YZ] = newep;
						maxBound[YZ] = 0.0;
						//XY
						newep = new EP(curr_position);
						newep.setXCoor(curr_position.getXCoor()+curr_box.getWidth());
						newep.setYCoor(0.0);
						newep.setZCoor(curr_position.getZCoor());
						newep.setWidth(curr_position.getWidth()-curr_box.getWidth());
						newep.setHeight(curr_position.getHeight());
						neweps[XY] = newep;
						maxBound[XY] = 0.0;
						//XZ
						newep = new EP(curr_position);
						newep.setXCoor(curr_position.getXCoor()+curr_box.getWidth());
						newep.setYCoor(curr_position.getYCoor());
						newep.setZCoor(0.0);
						newep.setWidth(curr_position.getWidth()-curr_box.getWidth());
//						newep.setLength(length);
						neweps[XZ] = newep;
						maxBound[XZ] = 0.0;
						//ZX
						newep = new EP(curr_position);
						newep.setXCoor(0.0);
						newep.setYCoor(curr_position.getYCoor());
						newep.setZCoor(curr_position.getZCoor()+curr_box.getLength());
						newep.setLength(curr_position.getLength()-curr_box.getLength());
//						newep.setWidth(width);
						neweps[ZX] = newep;
						maxBound[ZX] = 0.0;
						//ZY
						newep = new EP(curr_position);
						newep.setXCoor(curr_position.getXCoor());
						newep.setYCoor(0.0);
						newep.setZCoor(curr_position.getZCoor()+curr_box.getLength());
						newep.setLength(curr_position.getLength()-curr_box.getLength());
//						newep.setHeight(height);
						neweps[ZY] = newep;
						maxBound[ZY] = 0.0;
						
						Iterator<Box> iteratorBox = thisBoxes.iterator();//这里得包含一个0箱子。
						while(iteratorBox.hasNext()) {
							Box bi = iteratorBox.next();
							
							double x_i=bi.getXCoor(),y_i=bi.getYCoor(),z_i=bi.getZCoor();
							double w_i=bi.getWidth(),h_i=bi.getHeight(),l_i=bi.getLength();
							//添加6个extreme point
							//(curr_position.getXCoor()+curr_box.getWidth(),curr_position.getYCoor(),curr_position.getZCoor())
							// (x_k,y_k+h_k,z_k) in the direction of the Y axes
							//CanTakeProjection就是这个Corner point会穿过这个box
							if(!(y_k+h_k<y_i||y_k+h_k>y_i+h_i||z_k<z_i||z_k>z_i+l_i)&&x_i+w_i>maxBound[YX]&&x_k>0) {
								newep = new EP(curr_position);
								newep.setXCoor(bi.getXCoor()+bi.getWidth());
								newep.setYCoor(curr_position.getYCoor()+curr_box.getHeight());
								newep.setZCoor(curr_position.getZCoor());
								newep.setHeight(curr_position.getHeight()-curr_box.getHeight());
								assert curr_position.getXCoor()>bi.getXCoor() : "YX";
								newep.setWidth(curr_position.getWidth()+curr_position.getXCoor()-bi.getXCoor()-bi.getWidth());
								neweps[YX] = newep;
								maxBound[YX] = bi.getXCoor()+bi.getWidth();
							}
							// (x_k,y_k+h_k,z_k) in the direction of the Z axes
							if(!(x_k<x_i||x_k>x_i+w_i||y_k+h_k<y_i||y_k+h_k>y_i+h_i)&&z_i+l_i>maxBound[YZ]&&z_k>0) {
								newep = new EP(curr_position);
								newep.setXCoor(curr_position.getXCoor());
								newep.setYCoor(curr_position.getYCoor()+curr_box.getHeight());
								newep.setZCoor(bi.getZCoor()+bi.getLength());
								assert z_i+l_i<z_k : "YZ";
								newep.setHeight(curr_position.getHeight()-curr_box.getHeight());
								newep.setLength(curr_position.getLength()+curr_position.getZCoor()-bi.getZCoor()-bi.getLength());
								neweps[YZ] = newep;
								maxBound[YZ] = bi.getZCoor()+bi.getLength();
							}
							//(curr_position.getXCoor(),curr_position.getYCoor()+curr_box.getHeight(),curr_position.getZCoor())
							// (x_k+w_k,y_k,z_k) in the direction of the X axes
							if(!(x_k+w_k<x_i||x_k+w_k>x_i+w_i||z_k<z_i||z_k>z_i+l_i)&&y_i+h_i>maxBound[XY]&&y_k>0) {
								newep = new EP(curr_position);
								newep.setXCoor(curr_position.getXCoor()+curr_box.getWidth());
								newep.setYCoor(bi.getYCoor()+bi.getHeight());
								newep.setZCoor(curr_position.getZCoor());
								newep.setWidth(curr_position.getWidth()-curr_box.getWidth());
								assert curr_position.getYCoor()>bi.getYCoor()+bi.getHeight() : "XY";
								newep.setHeight(curr_position.getHeight()+curr_position.getYCoor()-bi.getYCoor()-bi.getHeight());
								neweps[XY] = newep;
								maxBound[XY] = bi.getYCoor()+bi.getHeight();
							}
							// (x_k+w_k,y_k,z_k) in the direction of the Z axes
							if(!(x_k+w_k<x_i||x_k+w_k>x_i+w_i||y_k<y_i||y_k>y_i+h_i)&&z_i+l_i>maxBound[XZ]&&z_k>0) {
								newep = new EP(curr_position);
								newep.setXCoor(curr_position.getXCoor()+curr_box.getWidth());
								newep.setYCoor(curr_position.getYCoor());
								newep.setZCoor(bi.getZCoor()+bi.getLength());
								newep.setWidth(curr_position.getWidth()-curr_box.getWidth());
								assert curr_position.getZCoor()>bi.getZCoor()+bi.getHeight() : "XZ";
								newep.setLength(curr_position.getLength()+curr_position.getZCoor()-bi.getZCoor()-bi.getHeight());
								neweps[XZ] = newep;
								maxBound[XZ] = bi.getZCoor()+bi.getLength();
							}
							//(curr_position.getXCoor(),curr_position.getYCoor(),curr_position.getZCoor()+curr_box.getLength())
							// (x_k,y_k,z_k+l_k) in the direction of the X axes
							if(!(y_k<y_i||y_k>y_i+h_i||z_k+l_k<z_i||z_k+l_k>z_i+l_i)&&x_i+w_i>maxBound[ZX]&&x_k>0) {
								newep = new EP(curr_position);
								newep.setXCoor(bi.getXCoor()+bi.getWidth());
								newep.setYCoor(curr_position.getYCoor());
								newep.setZCoor(curr_position.getZCoor()+curr_box.getLength());
								newep.setLength(curr_position.getLength()-curr_box.getLength());
								assert curr_position.getXCoor()>bi.getXCoor()+bi.getWidth() : "ZX";
								newep.setWidth(curr_position.getWidth()+curr_position.getXCoor()-bi.getXCoor()-bi.getWidth());
								neweps[ZX] = newep;
								maxBound[ZX] = bi.getXCoor()+bi.getWidth();
							}
							// (x_k,y_k,z_k+h_k) in the direction of the Y axes
							if(!(x_k<x_i||x_k>x_i+w_i||z_k+l_k<z_i||z_k+l_k>z_i+l_i)&&y_i+h_i>maxBound[ZY]) {
								newep = new EP(curr_position);
								newep.setXCoor(curr_position.getXCoor());
								newep.setYCoor(bi.getYCoor()+bi.getHeight());
								newep.setZCoor(curr_position.getZCoor()+curr_box.getLength());
								newep.setLength(curr_position.getLength()-curr_box.getLength());
								assert curr_position.getYCoor()>bi.getYCoor()+bi.getHeight() : "ZY";
								newep.setHeight(curr_position.getHeight()+curr_position.getYCoor()-bi.getYCoor()-bi.getHeight());
								neweps[ZY] = newep;
								maxBound[ZY] = bi.getYCoor()+bi.getHeight();
							}
						}
						//add neweps TO 3DEPL （3D extreme Point list)
						for(int newepsi=0;newepsi<6;newepsi++) {
							//先得到插入的位置。
							int idx=0;
							boolean delete_flag=false;
							for(idx=0;idx<extremePoints.size();idx++) {//按y,z,x来排序。
//								EP thisEP = extremePoints.get(idx);
								//对比Y（下降）,Z（上升）,X（上升）
								if(neweps[newepsi].getYCoor()>extremePoints.get(idx).getYCoor()) {//降序排
									break;
								}else if (neweps[newepsi].getYCoor()<=extremePoints.get(idx).getYCoor()) {
									continue;
								}else {
									if(neweps[newepsi].getZCoor()<extremePoints.get(idx).getZCoor()) {
										break;
									}else if(neweps[newepsi].getZCoor()>=extremePoints.get(idx).getZCoor()) {
										continue;
									}else {
//										if(neweps[newepsi].getZCoor()<extremePoints.get(idx).getZCoor()) {
//											break;
//										}else if(neweps[newepsi].getZCoor()>extremePoints.get(idx).getZCoor()) {
//											continue;
//										}else
										if(neweps[newepsi].getXCoor()==extremePoints.get(idx).getXCoor()){
											//删除这个有相同EP的点。
											delete_flag = true;
											break;
										}
									}
								}
									
							}
							if(!delete_flag)
								extremePoints.add(idx, neweps[newepsi]);//sort sortedBox by y,x coordinate
							
						}
						//order the 3DEPL by nondecreasing order of z(y),y(z),x deleting the duplicated EPs.
						
						
						
						thisBoxes.add(loadBox);
						thisloadWeight = thisloadWeight + loadBox.getWeight();
						loadIdx.add(boxi);//第i-2+1个箱子被装载了。
						insertConfirm=true;
						break;
					}
				}
				//delete p from extreme point set
				//add new extreme points to the end of extreme point list.
			}
			
			if(!insertConfirm) {break;}
		}
		
		this.Boxes = thisBoxes;
//		this.sortedBoxes = thisBoxes;
//		this.horizontal_levels = horizontal_levels;
		this.loadWeight=thisloadWeight;
		//calculate excessWeight
		if(this.loadWeight>capacity) {this.excessWeight=this.loadWeight-capacity;}else {this.excessWeight=0;}
		//calculate excessLength.
//		if(back.backSequence.get(back.backSequence.size()-1).getZCoor()
//				+back.backSequence.get(back.backSequence.size()-1).getLength()>this.length)
//			this.excessLength = back.backSequence.get(back.backSequence.size()-1).getZCoor()
//					+back.backSequence.get(back.backSequence.size()-1).getLength()-this.length;
//		else
//			this.excessLength = 0;
//		System.out.println("excessLength:"+this.excessLength+";excessWeight:"+this.excessWeight);
//		if(left.leftSequence.size()<boxingSequence1.size())
//		System.out.println("input box size:"+boxingSequence1.size()+"this vehicle size:"+this.Boxes.size());
		return loadIdx;//left.leftSequence.size();
		
//		return loadIdx;
	}
	
	
//	public ArrayList<Integer> hybrid_bpp(ArrayList<Box> boxingSequence){
//		int sz = boxingSequence.size();
////		ArrayList<Integer> final_loadIdx=new ArrayList<Integer>();//保存装在这辆车里面的箱子集
//		int curr_max = 0;
//		int method=0;
//		ArrayList<Integer> loadIdx = dblf(boxingSequence,0);
//		if(loadIdx.size()==sz) {
//			return loadIdx;
//		}else if(loadIdx.size()>curr_max) {
//			curr_max=loadIdx.size();
//			method=1;
//		}
//		loadIdx = dblf(boxingSequence,1);
//		if(loadIdx.size()==sz) {
//			return loadIdx;
//		}else if(loadIdx.size()>curr_max) {
//			curr_max=loadIdx.size();
//			method=2;
//		}
//		loadIdx = zqlbpp(boxingSequence,2);
//		if(loadIdx.size()==sz) {
//			return loadIdx;
//		}else if(loadIdx.size()>curr_max) {
//			curr_max=loadIdx.size();
//			method=3;
//		}
////		loadIdx = zqlbpp(boxingSequence,3);
////		if(loadIdx.size()==sz) {
////			return loadIdx;
////		}else if(loadIdx.size()>curr_max) {
////			curr_max=loadIdx.size();
////			method=4;
////		}
//		switch(method) {
//		case 1:loadIdx = dblf(boxingSequence,0);break;
//		case 2:loadIdx = dblf(boxingSequence,1);break;
//		case 3:loadIdx = zqlbpp(boxingSequence,2);break;
////		case 4:loadIdx = zqlbpp(boxingSequence,3);break;
//		case 0: System.out.println("not possible!!!");
//		}
//		return loadIdx;
//	}
//	
//	public boolean is_feasible_route(){
//		ArrayList<Box> boxingSequence = new ArrayList<Box>();
//		//把所有节点的box都加起来。
//		for(Node n:this.nodes)
//			for(Box b:n.getGoods())
//				boxingSequence.add(b);
//		int sz = boxingSequence.size();
//		ArrayList<Integer> loadIdx = dblf(boxingSequence,0);
//		if(loadIdx.size()==sz) {
//			return true;
//		}
//		loadIdx = dblf(boxingSequence,1);
//		if(loadIdx.size()==sz) {
//			return true;
//		}
//		loadIdx = zqlbpp(boxingSequence,2);
//		if(loadIdx.size()==sz) {
//			return true;
//		}
//		loadIdx = zqlbpp(boxingSequence,3);
//		if(loadIdx.size()==sz) {
//			return true;
//		}
//		return false;
//	}
//	
//	public ArrayList<Integer> all_bpp(ArrayList<Box> boxingSequence){
//		int sz = boxingSequence.size();
//		double max_load_rate = Double.MIN_VALUE;
//		double curr_load_rate;
//		int method=-1;
//		ArrayList<Integer> loadIdx = dblf(boxingSequence,0);
//		if(loadIdx.size()==sz) {
//			if(DEBUG)
//			this.carriage.setTruckTypeCode(this.carriage.getTruckTypeCode()+"-a0");
//			return loadIdx;
//		}
//		else {
//			curr_load_rate = Math.max(this.loadVolumn/this.carriage.getTruckVolume(), this.loadWeight/this.carriage.getCapacity());
//			if(curr_load_rate>max_load_rate) {
//				max_load_rate=curr_load_rate;
//				method=0;
//			}
//		}
//
//		loadIdx = dblf(boxingSequence,1);
//		if(loadIdx.size()==sz) {
//			if(DEBUG)
//			this.carriage.setTruckTypeCode(this.carriage.getTruckTypeCode()+"-a1");
//			return loadIdx;
//		}
//		else {
//			curr_load_rate = Math.max(this.loadVolumn/this.carriage.getTruckVolume(), this.loadWeight/this.carriage.getCapacity());
//			if(curr_load_rate>max_load_rate) {
//				max_load_rate=curr_load_rate;
//				method=1;
//			}
//		}
//		
//		loadIdx = zqlbpp(boxingSequence,2);
//		if(loadIdx.size()==sz) {
//			if(DEBUG)
//			this.carriage.setTruckTypeCode(this.carriage.getTruckTypeCode()+"-a2");
//			return loadIdx;
//		}
//		else {
//			curr_load_rate = Math.max(this.loadVolumn/this.carriage.getTruckVolume(), this.loadWeight/this.carriage.getCapacity());
//			if(curr_load_rate>max_load_rate) {
//				max_load_rate=curr_load_rate;
//				method=2;
//			}
//		}
//		
//		
//		loadIdx = zqlbpp(boxingSequence,3);
//		if(loadIdx.size()==sz) {
//			if(DEBUG)
//			this.carriage.setTruckTypeCode(this.carriage.getTruckTypeCode()+"-a3");
//			return loadIdx;
//		}
//		else {
//			curr_load_rate = Math.max(this.loadVolumn/this.carriage.getTruckVolume(), this.loadWeight/this.carriage.getCapacity());
//			if(curr_load_rate>max_load_rate) {
//				max_load_rate=curr_load_rate;
//				method=3;
//			}
//		}
//		
//		loadIdx = dblf_node(boxingSequence);
//		if(loadIdx.size()==sz) {
//			if(DEBUG)
//			this.carriage.setTruckTypeCode(this.carriage.getTruckTypeCode()+"-a4");
//			return loadIdx;
//		}
//		else {
//			curr_load_rate = Math.max(this.loadVolumn/this.carriage.getTruckVolume(), this.loadWeight/this.carriage.getCapacity());
//			if(curr_load_rate>max_load_rate) {
//				max_load_rate=curr_load_rate;
//				method=4;
//			}
//		}
//
//		switch(method) {
//		case 0:loadIdx = dblf(boxingSequence,0);break;
//		case 1:loadIdx = dblf(boxingSequence,1);break;
//		case 2:loadIdx = zqlbpp(boxingSequence,2);break;
//		case 3:loadIdx = zqlbpp(boxingSequence,3);break;
//		case 4:loadIdx = dblf_node(boxingSequence);break;
//		case -1: System.out.println("not possible!!!");
//		}
//		return loadIdx;
//	}
	

	



			public double getLoadVolumn() {
				return loadVolumn;
			}

			public void setLoadVolumn(double loadVolumn) {
				this.loadVolumn = loadVolumn;
			}
}

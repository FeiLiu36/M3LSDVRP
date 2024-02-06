package solver.util;

//import static solver.param.Param.*;
/**
 * Carriage class 
 * @author dell
 *
 */
public class Carriage  implements Cloneable {


	private double length;
	private double width;
	private double height;
	private double capacity;
	private String truckTypeId;
	private int truckId;
	private String truckTypeCode;
	private int quantity;
//	private ArrayList<Node> NodeBoxes = new ArrayList<Node>();
	
	
	public Carriage() {
		
	}
	
	public Carriage(Carriage c) {
		this.length=c.length;
		this.width=c.width;
		this.height=c.height;
		this.capacity=c.capacity;
		this.truckTypeId=c.truckTypeId;
		this.truckId = c.truckId;
		this.truckTypeCode=c.truckTypeCode;
		this.quantity = c.quantity;

		
	}
	
	public double getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}	

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}
	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		Carriage carriage = null;
		try {
			carriage = (Carriage)super.clone();
		}catch(CloneNotSupportedException e) {
			e.printStackTrace();
		}
		//这里可以设置不需要的变量。。。
		
		//
		return carriage;
	}


	public double getTruckVolume() {
		return this.width*this.length*this.height;
	}
	public String getTruckTypeCode() {
		return truckTypeCode;
	}

	public void setTruckTypeCode(String truckTypeCode) {
		this.truckTypeCode = truckTypeCode;
	}

	public String getTruckTypeId() {
		return truckTypeId;
	}

	public void setTruckTypeId(String truckTypeId) {
		this.truckTypeId = truckTypeId;
	}

	public int getTruckId() {
		return truckId;
	}

	public void setTruckId(int truckId) {
		this.truckId = truckId;
	}
	
}

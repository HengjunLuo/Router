package router;

public class Node {
	String id;
	int cost;
	String address;
	
	public Node(String id, int cost, String address) {
		this.id = id;
		this.cost = cost;
		this.address = address;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	
	public String toString() {
		return "[" + this.id + ", " + this.cost + "]";
	}
}

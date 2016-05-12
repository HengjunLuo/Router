package router;

public class Node {
	String id;
	int cost;
	String address;
	Node through;
	
	public Node(String id, int cost, String address) {
		this.id = id;
		this.cost = cost;
		this.address = address;
		this.through = null;
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
	
	public Node getThrough() {
		return through;
	}

	public void setThrough(Node through) {
		this.through = through;
	}

	public String toString() {
		return "[" + this.id + ", " + this.cost + "]";
	}
}

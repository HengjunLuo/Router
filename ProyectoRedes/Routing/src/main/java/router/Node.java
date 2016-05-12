package router;

public class Node {
	private String id;
	private int cost;
	private String address;
	private Node reachedThrough;
	
	public Node(String id, int cost, String address) {
		this.id = id;
		this.cost = cost;
		this.address = address;
		this.reachedThrough = null;
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
	
	public Node getReachedThrough() {
		return reachedThrough;
	}

	public void setReachedThrough(Node reachedThrough) {
		this.reachedThrough = reachedThrough;
		this.cost = reachedThrough.cost;
	}

	public String toString() {
		return "[" + this.id + ", " + this.cost + "]";
	}
}

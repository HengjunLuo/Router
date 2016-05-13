package router;

public class Node {
	private String id;
	private int cost;
	private String address;
	private Node reachedThrough;
	private boolean itIsAdjacent;
	
	public Node(String id, int cost, String address, boolean itIsAdjacent) {
		this.id = id;
		this.cost = cost;
		this.address = address;
		this.itIsAdjacent = itIsAdjacent;
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

	public boolean isItIsAdjacent() {
		return itIsAdjacent;
	}

	public void setItIsAdjacent(boolean itIsAdjacent) {
		this.itIsAdjacent = itIsAdjacent;
	}

	public String toString() {
		return "[" + this.id + ", " + this.cost + "]";
	}
}

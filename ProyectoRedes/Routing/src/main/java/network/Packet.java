package network;

import java.util.Map;

import router.RouterController;

public class Packet {
	public String from;
	public String type;
	public int len;
	public Map<String, Integer> costs;
	
	public Packet(String from, String type) {
		this.from = from;
		this.type = type;
		this.len = -1;
		this.costs = null;
	}

	public Packet(String from, String type, int len, Map<String, Integer> costs) {
		this.from = from;
		this.type = type;
		this.len = len;
		this.costs = costs;
	}
	
	public String toString() {
		String output = "Packet Description:\n";
		output += "\tFrom: " + from + "\n";
		output += "\tType: " + type + "\n";
		
		if (type.equals(RouterController.DV)) {
			output += "\tLen: " + len + "\n";
			for (String node: costs.keySet()) {
				output += "\t" + node + ": " + costs.get(node) + "\n";
			}
		}
		
		return output;
	}
}

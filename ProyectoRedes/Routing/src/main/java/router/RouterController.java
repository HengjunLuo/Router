package router;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import main.Utils;
import network.ClientSocket;
import network.NetworkController;
import network.Packet;
import network.ConnectivityChecker;

public class RouterController {
	public static final int PORT = 9080;
	public static final int NUM_THREADS = 4;
	public static final int NUM_NODES = 4;
	public static final int INFINITY = 99;
	public static final String KEEP_ALIVE = "KeepAlive";
	public static final String DV = "DV";
	public static final int timeT = 30;
	public static final int timeU = 90;
	public static String hostname;
	static boolean costChange = false;
	static private String TAG = "ROUTER CONTROLLER";

	NetworkController server;
	Thread threadServer;
	String setupFileName = "config.txt";
	
	private static Queue<Packet> events;
	private Map<String, Map<String, Node>> dvtable;
	private Map<String, Node> nodes;

	
	public RouterController() {
		File myNameFile = new File("..\\Routing\\src\\main\\resources\\myname.txt");
		// MAC syntax for paths
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			myNameFile = new File("../Routing/src/main/resources/myname.txt");
		}
		if (!myNameFile.exists()) {
			Utils.printLog(1, "Text file with host name doesn't exist.", TAG);
			System.exit(0);
		}
		hostname = Utils.readFile(myNameFile).get(0).trim();
		
		server = new NetworkController(PORT, NUM_THREADS);
		threadServer = new Thread(server);
		threadServer.start();
		
		dvtable = new HashMap<String, Map<String, Node>>();
		nodes = new HashMap<String, Node>();
		
		setupInitialState();
		startRouter();
	}
	
	private void setupInitialState() {
		Utils.printLog(3, "Setting router initial state...", TAG);
		// LOAD CONFIGURATION FILE
		File configFile = new File("..\\Routing\\src\\main\\resources\\config.txt");
		// MAC syntax for paths
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			configFile = new File("../Routing/src/main/resources/config.txt");
		}
		if (!configFile.exists()) {
			System.err.println("Unable to load configuration file.");
			System.exit(0);			
		}
		ArrayList<String> content = Utils.readFile(configFile);
		
		// CREATE CONNECTION TO HOSTS
		String address, hostname, cost;
		String[] splitted;
		int aux = 0;
		ClientSocket clientSocket;
		
		for (String line: content) {
			// Validate syntax
			splitted = line.split(" ");
			if (splitted.length != 3) {
				Utils.printLog(2, "Syntax error in config file. Skipping definition at line " + aux, TAG);
				continue;
			}
			address = splitted[0];
			hostname = splitted[1];
			cost = splitted[2];
			if (!Utils.validate(address) || hostname.equals("") || !cost.matches("[0-9]+")) {
				Utils.printLog(2, "Syntax error in config file. Skipping definition at line " + aux, TAG);
				continue;
			}
			
			// Create connection if doesn't exist one yet
			if (!NetworkController.existOutputConnection(hostname)) {
				Utils.printLog(3, "RouterController: There is no an output connection to '" + hostname + "'. Trying to get one.", TAG);
				clientSocket = new ClientSocket(address, PORT, hostname);
				new Thread(clientSocket).start();
			} else {
				Utils.printLog(2, "Trying to duplicate output connection with '" + hostname + "'.", TAG);
			}
			
			// Add new node
			nodes.put(hostname, new Node(hostname, Integer.parseInt(cost), address));
			aux++;
		}
		
		// Build initial DV Table
		for (Node node1: nodes.values()) {
			dvtable.put(node1.getId(), new HashMap<String, Node>());
			for (Node node2: nodes.values()) {
				aux = (node1.getId().equals(node2.getId())) ? node2.getCost() : INFINITY;
				dvtable.get(node1.getId()).put(node2.getId(), new Node(node2.getId(), aux, node2.getAddress()));
			}
		}
		
		// Start thread for connectivity verification.
		Thread aliveThread = new Thread(new ConnectivityChecker());
		aliveThread.start();
		
		System.out.println("Router initial state successfully configured.");
		System.out.println("************** INITIAL DISTANCE TABLE **************");
		this.printDTable();
	}
	
	public void startRouter() {
		// Send DV to all neighbors
		String data = "From:" + hostname + "\nType:DV\nLen:" + nodes.size() + "\n";
		for (Node node: nodes.values()) {
			data += node.getId() + ":" + node.getCost() + "\n";
		}
		Utils.printLog(3, "Sending initial DV to all nodes.", TAG);
		for (Node node: nodes.values()) {
			NetworkController.sendData(node.getId(), data);
		}
		
		events = new LinkedList<Packet>();
		Packet packet;
		int currentCost, newCost; 
		
		Utils.printLog(3, "RouterController: Starting main router controller.", TAG);
		while (true) {
			if (events.isEmpty()) {
				continue;
			}
			
			packet = events.poll();
			
			// PACKETS FROM ME TO NEIGHBORS
			if (packet.from.equals(hostname)) {
				Utils.printLog(3, "Sending KEP_ALIVES´s...", TAG);
				// Send KEEP_ALIVE packet
				if (packet.type.equals(RouterController.KEEP_ALIVE)) {
					data = "From:" + hostname + "\nType:" + KEEP_ALIVE + "\n";
					for (Node node: this.nodes.values()) {
						NetworkController.sendData(node.getId(), data);
					}
				}
				// Send DV packet
				else {
					Utils.printLog(3, "Ignoring packet from me to others... Not implemented yet.", TAG);
//					data = "From:" + hostname + "\nType:DV\nLen:" + nodes.size() + "\n";
//					for (Node node: nodes.values()) {
//						data += node.getId() + ":" + node.getCost() + "\n";
//					}
//					for (Node node: nodes.values()) {
//						NetworkController.sendData(node.getId(), data);
//					}
				}
			}
			// PACKETS FROM NEIGHBORS TO ME
			else {
				if (packet.type.equals(RouterController.DV)) {
					Utils.printLog(3, "Interpretando paquete tipo DV...", TAG);
					for (String source: packet.costs.keySet()) {
						currentCost = dvtable.get(source).get(packet.from).getCost();
						newCost = dvtable.get(packet.from).get(packet.from).getCost() + packet.costs.get(source);
						if (newCost < currentCost) {
							Utils.printLog(3, "DVTable: Cambiando costo a " + source + ", de " + currentCost + " a " + newCost, TAG);
							dvtable.get(source).get(packet.from).setCost(newCost);
						}
					}
					// Update forwarding table after changes applied
					this.updateForwardingTable();
				} else {
					Utils.printLog(3, "Leyendo paquete tipo KEEP_ALIVE de " + packet.from + "...", TAG);
				}
			}
		}
	}
	
	public static synchronized void receiveData(Packet packet) {
		Utils.printLog(3, "Queuing packet from '" + packet.from + "'", TAG);
		events.add(packet);
	}

	private void printDTable() {
		System.out.println("-------- DISTANCE TABLE --------");
		for (Map<String, Node> cols: dvtable.values()) {
			for (Node node: cols.values()) {
				System.out.print(node.toString());
			}
			System.out.println();
		}
	}
	
	private void printForwardingTable() {
		for(Node node: this.nodes.values()) {
			System.out.print(
				"[to: " + node.getId() +
				", cost: " + node.getCost() +
				", through:" + node.getReachedThrough().getId() + "]"
			);
		}
	}
	
	public void updateForwardingTable() {
		Map<String, Node> cols;
		for(String fila: this.dvtable.keySet()) {
			cols = this.dvtable.get(fila);
			Node through = null;
			for (Node col: cols.values()) {
				if (through == null) {
					through = col;
					continue;
				}
				
				if (col.getCost() < through.getCost()) {
					through = col;
				}
			}
			
			this.nodes.get(fila).setReachedThrough(through);
		}
	}
	
	public Map<String, String> getForwardingTable() {
		Map<String, String> result = new HashMap<String, String>();
		for (Node node: this.nodes.values()) {
			result.put(node.getId(), node.getReachedThrough().getId());
		}
		
		return result;
	}
	
	public static void considerSendingPackets() {
		if (costChange) {
			// TODO: WTF
		}
		// SENDS KEEP ALIVES
		else {
			events.add(new Packet(hostname, KEEP_ALIVE));
		}
	}
	
	
}

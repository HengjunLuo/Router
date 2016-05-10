package router;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import main.Utils;
import network.ClientSocket;
import network.NetworkController;
import network.Packet;

public class RouterController {
	public static final int PORT = 9080;
	public static final int NUM_THREADS = 4;
	public static final int NUM_NODES = 4;
	public static final int INFINITY = 999;
	public static final String KEEP_ALIVE = "KeepAlive";
	public static final String DV = "DV";
	public static String hostname;
	private String TAG = "ROUTER CONTROLLER";

	NetworkController server;
	Thread threadServer;
	String setupFileName = "config.txt";
	
	private static Queue<Packet> events;
	private Map<String, Map<String, Node>> dvtable;
	private Map<String, Node> nodes;

	
	public RouterController() {
		File myNameFile = new File("..\\Routing\\src\\main\\resources\\myname.txt");
		if (!myNameFile.exists()) {
			Utils.printError(1, "Text file with host name doesn't exist.", TAG);
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
		// LOAD CONFIGURATION FILE
		File configFile = new File("..\\Routing\\src\\main\\resources\\config.txt");
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
		Node node;
		
		for (String line: content) {
			// Validate syntax
			splitted = line.split(" ");
			if (splitted.length != 3) {
				Utils.printError(2, "Syntax error in config file. Skipping definition at line " + aux, TAG);
				continue;
			}
			address = splitted[0];
			hostname = splitted[1];
			cost = splitted[2];
			if (!Utils.validate(address) || hostname.equals("") || !cost.matches("[0-9]+")) {
				Utils.printError(2, "Syntax error in config file. Skipping definition at line " + aux, TAG);
				continue;
			}
			
			// Create connection if doesn't exist one yet
			if (!NetworkController.existOutputConnection(hostname)) {
				clientSocket = new ClientSocket(address, PORT, hostname);
				new Thread(clientSocket).start();
			} else {
				Utils.printError(2, "Trying to duplicate connection. A connection with " +
						hostname + " already exist.", TAG);
			}
			
			// Add new node
			nodes.put(hostname, new Node(hostname, Integer.parseInt(cost), address));
			aux++;
		}
		
		// Build initial DV Table
		for (Node node1: nodes.values()) {
			dvtable.put(node1.getId(), new HashMap<String, Node>());
			for (Node node2: nodes.values()) {
				aux = (node2.getId().equals(node2.getId())) ? node2.getCost() : INFINITY;
				dvtable.get(node1.getId()).put(node2.getId(), new Node(node2.getId(), aux, node2.getAddress()));
			}
		}
		this.printDTable();
	}
	
	public void startRouter() {
		// Send DV to all neighbors
		String data = "From:" + hostname + "\nType:DV\nLen:" + nodes.size() + "\n";
		for (Node node: nodes.values()) {
			data += node.getId() + ":" + node.getCost() + "\n";
		}
		for (Node node: nodes.values()) {
			NetworkController.sendData(node.getId(), data);
		}
		
		Packet packet;
		int currentCost, newCost;
		Timer timer = new Timer(); 
		
		while (true) {
			if (events.isEmpty()) {
				continue;
			}
			
			packet = events.poll();
			if (packet.type.equals(RouterController.DV)) {
				for (String source: packet.costs.keySet()) {
					currentCost = dvtable.get(packet.from).get(source).cost;
					newCost = dvtable.get(packet.from).get(packet.from).cost + packet.costs.get(source);
					if (newCost < currentCost) {
						System.out.println("Cambiando costo a " + source + ", de " + currentCost + " a " + newCost);
						dvtable.get(packet.from).get(source).cost = newCost;
					}
				}
			}
			
			timer.scheduleAtFixedRate(timerTask, 0, 1000);
		}
	}
	
	public static synchronized void receiveData(Packet packet) {
		events.add(packet);
	}

	TimerTask timerTask = new TimerTask() {
        public void run() {
        	printDTable();
        } 
    }; 


	private void printDTable() {
		for (Map<String, Node> cols: dvtable.values()) {
			for (Node node: cols.values()) {
				System.out.print(node.toString());
			}
			System.out.println();
		}
	}
}

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
import network.SenderThread;

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
	private static Map<String, Map<String, Node>> dvtable;
	private static Map<String, Node> nodes;

	
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
			
			// Add new node as adjacent
			nodes.put(hostname, new Node(hostname, Integer.parseInt(cost), address, true));
			// Reached by itself since it's a neighbor
			nodes.get(hostname).setReachedThrough(nodes.get(hostname));
			aux++;
		}
		
		// Build initial DV Table: All initial nodes are adjacent.
		for (Node node1: nodes.values()) {
			dvtable.put(node1.getId(), new HashMap<String, Node>());
			for (Node node2: nodes.values()) {
				aux = (node1.getId().equals(node2.getId())) ? node2.getCost() : INFINITY;
				dvtable.get(node1.getId()).put(node2.getId(), new Node(node2.getId(), aux, node2.getAddress(), true));
			}
		}
		
		// Start thread for connectivity verification.
		Thread aliveThread = new Thread(new ConnectivityChecker());
		aliveThread.start();
		
		// Start thread for considering sending DV or KEEP_ALIVE packet
		Thread senderThread = new Thread(new SenderThread());
		senderThread.start();
		
		System.out.println("Router initial state successfully configured.");
		this.printDTable();
	}
	
	public void startRouter() {
		// Setup initial state
		setupInitialState();
		
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
			Utils.printLog(3, "Executing event of type " + packet.type + "...", TAG);
			Utils.printLog(3, packet.toString(), TAG);
			
			// PACKETS FROM ME TO NEIGHBORS
			if (packet.from.equals(hostname)) {
				// Send KEEP_ALIVE packet
				if (packet.type.equals(RouterController.KEEP_ALIVE)) {
					Utils.printLog(3, "Sending KEP_ALIVES´s...", TAG);
					data = "From:" + hostname + "\nType:" + KEEP_ALIVE + "\n";
					for (Node node: nodes.values()) {
						// Only send to adjacent nodes.
						if (node.isItIsAdjacent()) {
							NetworkController.sendData(node.getId(), data);							
						}
					}
				}
				// Send DV packet
				else {
					Utils.printLog(3, "Sending DV packets...", TAG);
					data = "From:" + hostname + "\nType:DV\nLen:" + nodes.size() + "\n";
					for (String destiny: packet.costs.keySet()) {
						data += destiny + ":" + packet.costs.get(destiny) + "\n";
					}
					for (Node node: nodes.values()) {
						// Only send to adjacent nodes.
						if (node.isItIsAdjacent()) {
							NetworkController.sendData(node.getId(), data);
						}
					}
				}
			}
			

			// PACKETS FROM NEIGHBORS TO ME
			else {
				if (packet.type.equals(RouterController.DV)) {
					Utils.printLog(3, "Interpretando paquete tipo DV...", TAG);
					for (String destiny: packet.costs.keySet()) {
						// Obviar actualizaciones de DV hacia mi.
						// TODO: Confirmar con Pablo Estrada
						if (destiny.equals(hostname)) {
							continue;
						}
						// DV update to a known node
						if (dvtable.containsKey(destiny)) {
							currentCost = dvtable.get(destiny).get(packet.from).getCost();
							newCost = dvtable.get(packet.from).get(packet.from).getCost() + packet.costs.get(destiny);
							if (newCost < currentCost) {
								Utils.printLog(3, "DVTable: Cambiando costo a " + destiny + ", de " + currentCost + " a " + newCost, TAG);
								dvtable.get(destiny).get(packet.from).setCost(newCost);
							}
						}
						// DV update to a unknown node
						else {
							// Add new node with INFINITY costs since itsn't a neighbor. Address isn't important.
							nodes.put(destiny, new Node(destiny, INFINITY, "", false));
							nodes.get(destiny).setReachedThrough(nodes.get(packet.from));
							// Add a new row in DV table for new node
							dvtable.put(destiny, new HashMap<String, Node>());
							for (Node node: nodes.values()) {
								if (!node.isItIsAdjacent()) {
									continue;
								}
								// TODO: Verificar con Pablo Estrada
								int aux = (node.getId().equals(packet.from)) ?
									dvtable.get(packet.from).get(packet.from).getCost() + packet.costs.get(destiny)
									: INFINITY;
								dvtable.get(destiny).put(node.getId(), new Node(node.getId(), aux, node.getAddress(), false));
							}
						}
					}
					// Update forwarding table after changes applied
					this.updateForwardingTable();
				} else {
					Utils.printLog(3, "Leyendo paquete tipo KEEP_ALIVE de " + packet.from + "... Not implemented yet.", TAG);
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
		for(Node node: nodes.values()) {
			System.out.print(
				"[to: " + node.getId() +
				", cost: " + node.getCost() +
				", through:" + node.getReachedThrough().getId() + "]"
			);
		}
	}
	
	public void updateForwardingTable() {
		Utils.printLog(3, "Updating distance table...", TAG);
		Map<String, Node> cols;
		for(String fila: dvtable.keySet()) {
			cols = dvtable.get(fila);
			Node through = null;
			for (Node col: cols.values()) {
				if (through == null) {
					through = col;
					continue;
				}
				
				if (col.getCost() < through.getCost()) {
					through = col;
					costChange = true;
					Utils.printLog(3, "Cost changed during DV update. Cost to '"
							+ fila + "' is now " + col.getCost() + "'", TAG);
				}
			}
			
			nodes.get(fila).setReachedThrough(nodes.get(through.getId()));
		}
	}
	
	public Map<String, Node> getForwardingTable() {
		return nodes;
	}
	
	public static void considerSendingPackets() {
		Utils.printLog(3, "Checking router status...", TAG);
		if (costChange) {
			Utils.printLog(3, "Cost change ocurred. Building DV packet to send...", TAG);
			Map<String, Integer> costs = new HashMap<String, Integer>();
			for (Node node: nodes.values()) {
				costs.put(node.getAddress(), node.getCost());
			}
			events.add(new Packet(hostname, DV, nodes.size(), costs));
			costChange = false;
		}
		// SENDS KEEP ALIVES
		else {
			Utils.printLog(3, "No cost change. Building KEEP_ALIVE packet to send...", TAG);
			events.add(new Packet(hostname, KEEP_ALIVE));
		}
	}
	
	
}

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

public class RouterController implements Runnable {
	public static final int PORT = 9080;
	public static final int NUM_THREADS = 4;
	public static final int NUM_NODES = 4;
	public static final int INFINITY = 99;
	public static final String KEEP_ALIVE = "KeepAlive";
	public static final String DV = "DV";
	public static final int TIME_T = 5;
	public static final int TIME_U = 15;
	public static final int DEFAULT_COST = 5;
	
	public static String hostname;
	private static boolean costChange = false;
	private static String TAG = "ROUTER CONTROLLER";

	private NetworkController server;
	private Thread threadServer;
	
	private static Queue<Packet> events;
	private static Map<String, Map<String, Integer>> dvtable;
	private static Map<String, Node> nodes;
	private boolean isStopped = false;

	
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
		if (hostname == null || hostname.equals("")) {
			Utils.printLog(1, "Invalid hostname.", TAG);
			System.exit(0);
		}
		
		dvtable = new HashMap<String, Map<String, Integer>>();
		nodes = new HashMap<String, Node>();
		
		server = new NetworkController(PORT, NUM_THREADS);
		threadServer = new Thread(server);
		threadServer.start();
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
				ClientSocket clientSocket = new ClientSocket(address, PORT, hostname);
				new Thread(clientSocket).start();
			} else {
				Utils.printLog(2, "Trying to duplicate output connection with '" + hostname + "'.", TAG);
			}
			
			// Add new node as adjacent
			nodes.put(hostname, new Node(hostname, Integer.parseInt(cost), address, true));
			// Reached by itself since it's a neighbor
			nodes.get(hostname).setReachedThrough(hostname);
			aux++;
		}
		
		// Build initial DV Table: All initial nodes are adjacent.
		for (Node node1: nodes.values()) {
			dvtable.put(node1.getId(), new HashMap<String, Integer>());
			for (Node node2: nodes.values()) {
				aux = (node1.getId().equals(node2.getId())) ? node2.getCost() : INFINITY;
				dvtable.get(node1.getId()).put(node2.getId(), aux);
			}
		}
		
		// Start thread for connectivity verification
		Thread aliveThread = new Thread(new ConnectivityChecker());
		aliveThread.start();
		
		// Start thread for considering sending DV or KEEP_ALIVE packet
		Thread senderThread = new Thread(new SenderThread());
		senderThread.start();
		
		System.out.println("Router initial state successfully configured.");
		this.printDTable();
	}
	
	/**
	 * Main function: Router controller.
	 */
	public void run() {
		// Setup initial state
		setupInitialState();
		
		// Send DV packets to all neighbors
		Utils.printLog(3, "Sending initial DV to all nodes.", TAG);
		Map<String, Integer> costs = new HashMap<String, Integer>();
		for (Node node: nodes.values()) {
			costs.put(node.getId(), node.getCost());
		}
		for (Node node: nodes.values()) {
			if (node.isItIsAdjacent())
				NetworkController.sendPacket(node.getId(), new Packet(hostname, DV, costs.size(), costs));
		}
		
		events = new LinkedList<Packet>();
		Packet packet;
		int currentCost, newCost; 
		
		Utils.printLog(3, "RouterController: Starting main router controller.", TAG);
		while (!isStopped) {
			if (events.isEmpty()) {
				Utils.printLog(3, "No events to execute. Sleeping for 5s...", TAG);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}

			packet = events.poll();
			
			// PACKETS FROM ME TO NEIGHBORS
			if (packet.from.equals(hostname)) {
				Utils.printLog(3, "Executing output event...", TAG);
				Utils.printLog(3, packet.toString(), TAG);
				for (Node node: nodes.values()) {
					// Only send to adjacent nodes.
					if (node.isItIsAdjacent()) {
						NetworkController.sendPacket(node.getId(), packet);							
					}
				}
			}
			
			// PACKETS FROM NEIGHBORS TO ME
			else {
				Utils.printLog(3, "Executing input event of type " + packet.type + "...", TAG);
				Utils.printLog(3, packet.toString(), TAG);
				if (packet.type.equals(RouterController.DV)) {
					for (String destiny: packet.costs.keySet()) {
						// Obviar actualizaciones de DV hacia mi.
						// WARNING: Quizás esta no sea la mejor idea pero no nos queremos complicar.
						if (destiny.equals(hostname)) {
							continue;
						}
						// DV update to a known node
						if (nodes.containsKey(destiny)) {
							Utils.printLog(3, "DV update for known node.", TAG);
							currentCost = dvtable.get(destiny).get(packet.from);
							newCost = dvtable.get(packet.from).get(packet.from) + packet.costs.get(destiny);
							if (newCost < currentCost) {
								Utils.printLog(3, "DVTable: Cambiando costo a " + destiny + ", de " + currentCost + " a " + newCost, TAG);
								dvtable.get(destiny).put(packet.from, newCost);
							}
						}
						// DV update to a unknown node
						else {
							Utils.printLog(3, "DV update for unknown node.", TAG);
							// Add new node with INFINITY costs since itsn't a neighbor. Address isn't important.
							nodes.put(destiny, new Node(destiny, INFINITY, "", false));
							nodes.get(destiny).setReachedThrough(packet.from);
							// Add a new row in DV table for new node
							dvtable.put(destiny, new HashMap<String, Integer>());
							for (Node node: nodes.values()) {
								if (!node.isItIsAdjacent()) {
									continue;
								}
								// TODO: Verificar con Pablo Estrada
								int aux = (node.getId().equals(packet.from)) ?
									dvtable.get(packet.from).get(packet.from) + packet.costs.get(destiny)
									: INFINITY;
								dvtable.get(destiny).put(node.getId(), aux);
							}
						}
					}
					// Update forwarding table after changes applied
					updateForwardingTable();
					printDTable();
					printForwardingTable();
				} else {
					Utils.printLog(3, "Reading KEEP_ALIVE packet from " + packet.from + "... Not implemented yet.", TAG);
				}
			}
		}
	}
		
	public static synchronized void receivePacket(Packet packet) {
		Utils.printLog(3, "Queuing event for received packet from '" + packet.from + "'", TAG);
		events.add(packet);
	}

	private void printDTable() {
		System.out.println("-------- DISTANCE TABLE --------");
		for (String row: dvtable.keySet()) {
			System.out.print(row + ": ");
			for (String column: dvtable.get(row).keySet()) {
				System.out.print("[" + column + ":" + dvtable.get(row).get(column) + "]");
			}
			System.out.println();
		}
	}
	
	private static void printForwardingTable() {
		System.out.println("-------- FORWARDING TABLE --------");
		for(Node node: nodes.values()) {
			System.out.println(
				"[to: " + node.getId() +
				", cost: " + node.getCost() +
				", through:" + node.getReachedThrough() + "]"
			);
		}
	}
	
	private static void updateForwardingTable() {
		Utils.printLog(3, "Updating forwarding table...", TAG);
		Map<String, Integer> cols;
		
		for(String fila: dvtable.keySet()) {
			cols = dvtable.get(fila);
			int min = INFINITY;
			String through = "";
			for (String col: cols.keySet()) {
				if (cols.get(col) < min) {
					min = cols.get(col);
					through = col;
				}
			}
			
			int minAux = nodes.get(fila).getCost();
			String throughAux = nodes.get(fila).getReachedThrough();
			
			// Check if path changes.
			if (min != minAux || !through.equals(throughAux)) {
				Utils.printLog(3, "Cost changed during DV update. Cost to '" + fila + "' is now " + min + "'", TAG);
				nodes.get(fila).setReachedThrough(through);
				nodes.get(fila).setCost(min);
				costChange = true;
			}
		}
	}
	
	public static synchronized Map<String, Node> getForwardingTable() {
		return nodes;
	}
	
	public static synchronized void considerSendingPackets() {
		Utils.printLog(3, "Checking router status...", TAG);
		if (costChange) {
			Map<String, Integer> costs = new HashMap<String, Integer>();
			for (Node node: nodes.values()) {
				costs.put(node.getId(), node.getCost());
			}
			events.add(new Packet(hostname, DV, nodes.size(), costs));
			Utils.printLog(3, "Cost change ocurred. Event 'Send DV packets' added.", TAG);
			costChange = false;
		}
		// SENDS KEEP ALIVES
		else {
			events.add(new Packet(hostname, KEEP_ALIVE));
			Utils.printLog(3, "No cost change. Event 'Send KEEP_ALIVE packets' added.", TAG);
		}
	}
	
	public static synchronized void disconectNode(String id) {
		Utils.printLog(3, "Disconecting node '" + id + "'...", TAG);
		
		if (nodes.containsKey(id)) {
			nodes.remove(id);
			Utils.printLog(3, "Node '" + id + "' removed from nodes.", TAG);
		} else {
			Utils.printLog(2, "Trying to remove a non existent node from nodes: " + id, TAG);
		}

		// Deleting Node's column in DistanceTable.
		for (Map<String, Integer> rows: dvtable.values()) {
			if (rows.containsKey(id)) {
				rows.remove(id);
				Utils.printLog(3, "Column for node '" + id + "' removed from dvtable.", TAG);
			} else {
				Utils.printLog(2, "Trying to remove a non existent column from dvtable: " + id, TAG);
			}
		}
		
		// Delete Node's row in DistanceTable
		if (dvtable.containsKey(id)) {
			dvtable.remove(id);
			Utils.printLog(3, "Row for node '" + id + "' removed from dvtable.", TAG);
		} else {
			Utils.printLog(2, "Trying to remove a non existent row from dvtable: " + id, TAG);
		}
		
		// Update forwarding table
		updateForwardingTable();
	}
	
	public static synchronized void addNeighborNode(String id, String address) {
		// Trying to add this node to nodes.
		if (!nodes.containsKey(id)) {
			nodes.put(id, new Node(id, DEFAULT_COST, address, true));
			nodes.get(id).setReachedThrough(id);	// Reached through itself
			
			// Add columns for each row in Distance Table 
			for(Map<String, Integer> fila: dvtable.values()) {
				fila.put(id, INFINITY);
			}
		} else {
			Utils.printLog(2, "Node '" + id + "' already exist in nodes.", TAG);
			return;
		}
		
		// Trying to add this node in distance table.
		if (!dvtable.containsKey(id)) {
			dvtable.put(id, new HashMap<String, Integer>());
			for (Node node: nodes.values()) {
				if (!node.isItIsAdjacent()) {
					continue;
				}
				int aux = (node.getId().equals(id)) ? DEFAULT_COST : INFINITY;
				dvtable.get(id).put(node.getId(), aux);
			}
		} else {
			Utils.printLog(2, "Node '" + id + "' already has a row in dvtable.", TAG);
		}
	}
}

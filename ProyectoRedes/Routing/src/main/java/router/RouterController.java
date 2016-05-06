package router;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Queue;

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
	
	public RouterController() {
		File myNameFile = new File("..\\Routing\\src\\main\\resources\\myname.txt");
		if (!myNameFile.exists()) {
			Utils.printError(1, "Text file with host name doesn't exist.", TAG);
			System.exit(0);
		}
		hostname = Utils.readFile(myNameFile).get(0).trim();
		
		setupInitialState();
		
		server = new NetworkController(PORT, NUM_THREADS);
		threadServer = new Thread(server);
		threadServer.start();
		
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
			
			// Create connection
			clientSocket = new ClientSocket(address, PORT, hostname);
			new Thread(clientSocket).start();

			aux++;
		}
	}
	
	public void startRouter() {
		// TODO: Implement router listener here
		while (true) {
//			if (events.isEmpty()) {
//				continue;
//			}
			
			
		}
	}
	
	public static synchronized void receiveData(Packet packet) {
//		events.add(packet);
	}
}

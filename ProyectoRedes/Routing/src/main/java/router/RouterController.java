package router;

import java.io.File;
import java.util.ArrayList;
import java.util.Queue;

import main.Utils;
import network.NetworkController;
import network.Packet;

public class RouterController {
	public static final int PORT = 9080;
	public static final int NUM_THREADS = 4;
	public static final int NUM_NODES = 4;
	public static final int INFINITY = 999;
	public static final String KEEP_ALIVE = "KeepAlive";
	public static final String DV = "DV";
	public static final String hostname = "Cesar";

	NetworkController server;
	Thread threadServer;
	String setupFileName = "config.txt";
	
	private static Queue<Packet> events;
	
	public RouterController() {
		setupInitialState();
		
		server = new NetworkController(PORT, NUM_THREADS);
		threadServer = new Thread(server);
		threadServer.start();
		
		startRouter();
	}
	
	private void setupInitialState() {
//		File file = new File("src/files/config.txt");
//		ArrayList<String> content = Utils.readFile(file);
//		System.out.println(content.toString());
		// TODO: Implement initial configurations
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

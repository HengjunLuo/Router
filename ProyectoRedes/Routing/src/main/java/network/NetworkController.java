package network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import router.RouterController;
import main.Utils;

public final class NetworkController implements Runnable{

	protected ExecutorService threadPool = null;
	protected int serverPort;
	protected ServerSocket serverSocket = null;
	protected boolean isStopped = false;
	protected Thread runningThread = null;
	
	static String TAG = "NETWORK CONTROLLER";
	static Map<String, ServerRunnable> inputConnections;
	static Map<String, ClientSocket> outputConnections;
	
	protected String interfaceName = "Intel(R) Dual Band Wireless-AC 3160";
	static String LocalIPAddress = null;
	
	public NetworkController(int port, int nThreads) {

		this.serverPort = port;
		this.threadPool = Executors.newFixedThreadPool(nThreads);
		
		inputConnections = new HashMap<String, ServerRunnable>();
		outputConnections = new HashMap<String, ClientSocket>();
	}
	
	public void run() {
		synchronized(this) {
			this.runningThread = Thread.currentThread();
		}
		openServerSocket();
		while(!isStopped()) {
			Socket clientSocket = null;
			try {
				clientSocket = this.serverSocket.accept();
			} catch (IOException e) {
				if (isStopped()) {
					System.out.println("Server Stopped.");
					break;
				}
				throw new RuntimeException("Error accepting client connection", e);
			}
			
			System.out.println("\nNew client connection arrived:");
			
			// Start server listener for new node
			this.threadPool.execute(new ServerRunnable(clientSocket));
			
		}
		this.threadPool.shutdown();
		System.out.println("Server Stopped.");
	}

	private synchronized boolean isStopped() {
		return this.isStopped;
	}
	
	public synchronized void stop() {
		this.isStopped = true;
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}
	
	private void openServerSocket() {
		try {
			this.serverSocket = new ServerSocket(this.serverPort);
		} catch (IOException e) {
			throw new RuntimeException("Cannot open port " + RouterController.PORT, e);
		}
		System.out.println("Server started. Listening...");
	}
	
	
	public static synchronized void removeServerConnection(String host) {
		if (inputConnections.containsKey(host)) {
			inputConnections.remove(host);
		}
	}
	
	public static synchronized void removeClientConnection(String host) {
		if (outputConnections.containsKey(host)) {
			outputConnections.remove(host);
		}
	}
	
	public static synchronized void addNodeConnection(ServerRunnable listener, ClientSocket sender) {
		inputConnections.put(listener.getHost(), listener);
		outputConnections.put(sender.getHost(), sender);
	}
	
	public static synchronized void sendData(String host, String data) {
		if (outputConnections.containsKey(host)) {
			outputConnections.get(host).addData(data);
		} else {
			Utils.printError(2, "Trying to send data to a NULL node.", TAG);
		}
	}
	
	/**
	 * Called for listener connections to pass data received from nodes.
	 * @param data
	 */
	public static synchronized void receivePacket(Packet packet) {
//		System.out.println(packet.toString());
		RouterController.receiveData(packet);
	}
	
	public static synchronized boolean existOutputConnection(String host) {
		return outputConnections.containsKey(host);
	}

	public static synchronized boolean existInputConnection(String host) {
		return inputConnections.containsKey(host);
	}
}

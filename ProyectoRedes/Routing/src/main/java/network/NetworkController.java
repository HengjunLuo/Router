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
					Utils.printLog(3, "Server Stopped.", TAG);
					break;
				}
				throw new RuntimeException("Error accepting client connection", e);
			}
			
			Utils.printLog(3, "\nNew client connection arrived:", TAG);
			
			// Start server listener for new node
			this.threadPool.execute(new ServerRunnable(clientSocket));
			
		}
		this.threadPool.shutdown();
		Utils.printLog(3, "Server Stopped.", TAG);
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
		Utils.printLog(3, "Server started. Listening...", TAG);
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
	
	public static synchronized void sendData(String host, String data) {
		if (outputConnections.containsKey(host)) {
			outputConnections.get(host).addData(data);
		} else {
			Utils.printLog(2, "Trying to send data to a disconnected node: '" + host + "'", TAG);
		}
	}
	
	/**
	 * Called for listener connections to pass data received from nodes.
	 * @param data
	 */
	public static synchronized void receivePacket(Packet packet) {
		Utils.printLog(3, packet.toString(), TAG);
		RouterController.receiveData(packet);
	}
	
	public static synchronized boolean existOutputConnection(String host) {
		return outputConnections.containsKey(host);
	}

	public static synchronized boolean existInputConnection(String host) {
		return inputConnections.containsKey(host);
	}
	
	public static void checkKeepAlive() {
		long currentTime;
		for (ServerRunnable listener: inputConnections.values()) {
			currentTime = new Date().getTime();
			if (currentTime - listener.getLastAlive() > RouterController.timeU) {
				Utils.printLog(2, "The '" + listener.getHost() + "' host has been dropped.", TAG);;
				// TODO: Remove this connection or set cost to INFINITY
			} else {
				Utils.printLog(3, "Host '" + listener.getHost() + "' keeps alive.", TAG);;
			}
		}
	}
}

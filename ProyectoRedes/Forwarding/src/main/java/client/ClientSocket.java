package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;


import main.Utils;

public class ClientSocket implements Runnable{
	
	private String hostname = null;
	private int port = 0;
	private static Queue<String> dataQueue;
	private Socket clientSocket = null;
	private String address = null;
    private DataOutputStream output = null;
    private DataInputStream input = null;
    private boolean isStopped = false;
    private boolean connected = false;
    private boolean logged = false;
    
    private static String TAG = "CLIENT FORWARDING SOCKET";
    
    
    public ClientSocket(String address, int port, String hostname) {
    	this.address = address;
    	this.hostname = hostname;
    	this.port = port;
    	
    	dataQueue = new LinkedList<String>();
    	
		// Registering client socket
		//ForwardingController.addOutputConnection(hostname, clientSocket);
    }
	
	private boolean requestForConnection() {
    	// Instantiate connection socket and output/input stream
        try {
        	clientSocket = new Socket(this.address, this.port);
            output = new DataOutputStream(clientSocket.getOutputStream());
            input = new DataInputStream(clientSocket.getInputStream());
        } catch (Exception e) {
        	Utils.printLog(1, "Attempt connection with '" + hostname + "' failed.", TAG);
        	
        	return false;
        }
        
        Utils.printLog(3, "Output socket openned for '" + this.hostname + "'", TAG);
        
        return true;
	}
	
	private boolean login() {
		Utils.printLog(3, "Client login proccess with '" + this.hostname + "'...", TAG);
		if (!connected) {
			Utils.printLog(1, "Login attempt failed beacause socket is not connected.", TAG);
			return false;
		}
    	return false;
    }

	public void run() {
		// Attempt to connect with host
		if (!(connected = requestForConnection())) {
			this.closeConnection();
		}
		
        // Login process: if FALSE, brook connection
		if (!(logged = login())) {
			this.closeConnection();
		}

        // If logged successfully, start to sending data
		while (!isStopped) {
			
			// Attempt to connect with host
			if (!connected) {
				connected = requestForConnection();
			}
			
	        // Login process: if FALSE, brook connection
			if (!logged) {
				if (!(logged = login())) {
					continue;
				}
			}
			
			// If the queue is empty, sleep for 5s and continue.
			if (dataQueue.isEmpty()) {
				Utils.printLog(3, this.hostname + ": Empty queue. Sleeping for 5s...", TAG);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}

			// If the queue is not empty, send the packet at the head of queue.
			try {
				String data = dataQueue.poll();
				output.writeBytes(data);
				Utils.printLog(3, this.hostname + ": Data sent successuflly:\n" + data, TAG);
			} catch (IOException e) {
				Utils.printLog(1, "Sending data to " + this.hostname + " failed. " + e.getMessage(), TAG);
				closeConnection();
			}
		}
	}
	
	public synchronized void addData(String data) {
		Utils.printLog(3, this.hostname + ": Queing data to send:\n" + data, TAG);
		dataQueue.add(data);
	}
	
	public void closeConnection() {
		try {
			// Clean up
			if (output != null)
				output.close();
			if (input != null)
				input.close();
			if (clientSocket != null)
				clientSocket.close();
			
		} catch (IOException e) {
			Utils.printLog(1, "Clossing connection with '" + this.hostname + "'.", TAG);
			e.printStackTrace();
		}
		
		// Remove from network controller
		//ForwardingController.removeOutputConnection(this.hostname);
		
		// Set flag to stopped
		isStopped = true;
	}
	
	public synchronized String getHost() {
		return hostname;
	}
	
	public synchronized String getAddress() {
		return address;
	}
}

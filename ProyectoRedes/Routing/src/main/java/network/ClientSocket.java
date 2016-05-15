package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import router.RouterController;
import main.Utils;

public class ClientSocket implements Runnable{
	
	private String hostname = null;
	private int port = 0;
	private static Queue<String> dataQueue;
	private Socket clientSocket = null;
	protected String address = null;
    private DataOutputStream output = null;
    private DataInputStream input = null;
    private boolean isStopped = false;
    private boolean connected = false;
    private boolean logged = false;
    
    private static String TAG = "CLIENT SOCKET";
    
    
    public ClientSocket(String address, int port, String hostname) {
    	this.address = address;
    	this.hostname = hostname;
    	this.port = port;
    	
    	dataQueue = new LinkedList<String>();
    	
		// Registering client socket
		NetworkController.outputConnections.put(this.hostname, this);    	
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
        
        Utils.printLog(3, "Client socket openned for '" + this.hostname + "'", TAG);
        
        return true;
	}
	
	private boolean login() {
		Utils.printLog(3, "Client login proccess with '" + this.hostname + "'...", TAG);
		if (!connected) {
			Utils.printLog(3, "Login attempt failed beacause socket is not connected.", TAG);
			return false;
		}
		
    	String request = "From:" + RouterController.hostname + "\n" + "Type:HELLO\n";
    	String response1="", response2="";
    	
    	// Sending request message
    	try {
    		Utils.printLog(3, "Sending HELLO to '" + this.hostname + "'...", TAG);
			output.writeBytes(request);
		} catch (IOException e) {
			Utils.printLog(1, "Trying to send HELLO request to '" + this.hostname + "'", TAG);
		}

    	// Reading WELCOME message
    	try {
    		Utils.printLog(3, "Trying to read WELCOME from '" + this.hostname + "'...", TAG);
			response1 = input.readLine();
			response2 = input.readLine();
		} catch (IOException e) {
			Utils.printLog(1, "Trying to read WELCOME request from '" + this.hostname + "'", TAG);
		}
    	
    	if (response1.trim().equals("From:" + this.hostname) && response2.trim().equals("Type:WELCOME")) {
    		Utils.printLog(3, "Output connection stablished with '" + this.hostname + "'.", TAG);
    		
    		return true;
    	}
    	
    	return false;
    }

	public void run() {
		// Attempt to connect with host
		connected = requestForConnection();
		
        // Login process: if FALSE, brook connection
		logged = login();

        // If logged successfully, start to sending data
		while (!isStopped) {
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
			
			// Attempt to connect with host
			if (!connected) {
				connected = requestForConnection();
			}
			
	        // Login process: if FALSE, brook connection
			if (!logged) {
				logged = login();
				if (!logged) {
					continue;
				}
			}

			// If the queue is not empty, send the packet at the head of queue.
			try {
				String data = dataQueue.poll();
				Utils.printLog(3, this.hostname + ": Proceding to send data:\n" + data, TAG);
				output.writeBytes(data);
				Utils.printLog(3, this.hostname + ": Data sent successuflly:\n" + data, TAG);
			} catch (IOException e) {
				connected = false;
				Utils.printLog(1, "Sending data to " + this.hostname + " failed. " + e.getMessage(), TAG);
			}
		}
	}
	
	public synchronized void addData(String data) {
		Utils.printLog(3, this.hostname +": Queing data to send:\n" + data, TAG);
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
			
			// Remove from network controller
			NetworkController.removeClientConnection(this.hostname);
			
		} catch (IOException e) {
			Utils.printLog(1, "Clossing connection with '" + this.hostname + "'.", TAG);
			e.printStackTrace();
		}
		
		// Set flag to stopped
		isStopped = true;
	}
	
	public synchronized String getHost() {
		return hostname;
	}
}

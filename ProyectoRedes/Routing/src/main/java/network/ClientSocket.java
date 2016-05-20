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
	
	protected String hostname = null;
	protected int port = 0;
	protected Queue<String> dataQueue;
	protected Socket clientSocket = null;
	protected String address = null;
    protected DataOutputStream output = null;
    protected DataInputStream input = null;
    protected boolean isStopped = false;
    protected boolean connected = false;
    protected boolean logged = false;
    
    protected String TAG = "CLIENT SOCKET";
    
    
    public ClientSocket(String address, int port, String hostname) {
    	this.address = address;
    	this.hostname = hostname;
    	this.port = port;
    	
    	dataQueue = new LinkedList<String>();
    	
		// Registering client socket
		NetworkController.addOutputConnection(this.hostname, this);
    }
	
	private boolean requestForConnection() {
    	// Instantiate connection socket and output/input stream. If it fails, try again until 3 attempts.
		int attempts  = 0;
		while (true) {
	        try {
	        	clientSocket = new Socket(this.address, this.port);
	            output = new DataOutputStream(clientSocket.getOutputStream());
	            input = new DataInputStream(clientSocket.getInputStream());
	            break;
	        } catch (Exception e) {
	        	Utils.printLog(1, "Attempt connection with '" + hostname + "' failed.", TAG);
	        	// If it fails, sleep thread for 2 seconds and try again. 
	        	try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
	        	attempts++;
	        	// 3 attempts to connect
	        	if (attempts == 3) {
	        		Utils.printLog(1, "Unable to connect to '" + this.hostname + "'. Tried for 3 times.", TAG);
	        		return false;
	        	}
	        }
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
		
    	String request = "From:" + RouterController.hostname + "\n" + "Type:HELLO\n";
    	String response1 = "", response2 = "";
    	
    	// Sending request message
    	try {
    		Utils.printLog(3, "Sending HELLO to '" + this.hostname + "'...", TAG);
			output.writeBytes(request);
		} catch (IOException e) {
			Utils.printLog(1, "Trying to send HELLO request to '" + this.hostname + "' failed.", TAG);
			
			return false;
		}

    	// Reading WELCOME message
    	try {
    		Utils.printLog(3, "Trying to read WELCOME from '" + this.hostname + "'...", TAG);
			response1 = input.readLine();
			response2 = input.readLine();
//			System.out.println("Respuesta1: " + response1);
//			System.out.println("Respuesta2: " + response2);
		} catch (IOException e) {
			Utils.printLog(1, "Trying to read WELCOME from '" + this.hostname + "' failed.", TAG);
			
			return false;
		}
    	
    	if (response1.trim().equals("From:" + this.hostname) && response2.trim().equals("Type:WELCOME")) {
    		Utils.printLog(3, "Output connection stablished with '" + this.hostname + "'.", TAG);
    		
    		return true;
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
		NetworkController.removeOutputConnection(this.hostname);
		
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

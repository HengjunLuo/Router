package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;

import router.RouterController;
import main.Utils;

public class ClientSocket implements Runnable{
	
	private String hostname = null;
	
	private Queue<String> dataQueue;
	private Socket clientSocket = null;
	protected String address = null;
    private DataOutputStream output = null;
    private DataInputStream input = null;
    private boolean isStopped = false;
    
    private static String TAG = "CLIENT SOCKET";
    
    
    public ClientSocket(String address, int port, String hostname) {
    	this.address = address;
    	this.hostname = hostname;
    	
    	dataQueue = new LinkedList<String>();

    	// Instantiate connection socket and output/input stream
        try {
        	clientSocket = new Socket(this.address, port);
        	while (clientSocket == null) {}
            output = new DataOutputStream(clientSocket.getOutputStream());
            input = new DataInputStream(clientSocket.getInputStream());
        } catch (UnknownHostException e) {
        	Utils.printLog(1, "Don't know about host " + hostname, TAG);
        } catch (IOException e) {
        	Utils.printLog(1, "Couldn't get I/O for the connection to " + hostname, TAG);
        }
        
        // Add new connection
        Utils.printLog(3, "Client socket openned for '" + this.hostname + "'", TAG);
        NetworkController.outputConnections.put(this.hostname, this);
    }

	public void run() {
        // Login process: if FALSE, brook connection
        if (!login()) {
        	Utils.printLog(3, "Output connection with '" + this.hostname + "' failed.", TAG);
        	NetworkController.outputConnections.remove(this.hostname);
        	this.closeConnection();
        }

        // If logged successfully, start to sending data
		while (!isStopped) {
			// If the queue is empty, continue.
			if (dataQueue.isEmpty()) {
				continue;
			}
			
			// If the queue is not empty, send the packet at the head of queue.
			try {
				String data = dataQueue.poll();
				Utils.printLog(3, "Sending to " + this.hostname + ": " + data , TAG);
				output.writeBytes(data);
			} catch (IOException e) {
				Utils.printLog(1, "Sending data to " + this.hostname + " failed. " + e.getMessage(), TAG);
			}
		}
	}
	
	private boolean login() {
		Utils.printLog(3, "Client login proccess with '" + this.hostname + "'...", TAG);
    	String request = "From:" + RouterController.hostname + "\n" + "Type:HELLO\n";
    	String response1="", response2="";
    	
    	// Sending request message
    	try {
    		Utils.printLog(3, "Sending HELLO to '" + this.hostname + "'...", TAG);
			output.writeBytes(request);
		} catch (IOException e) {
			Utils.printLog(1, "Trying to send HELLO request to '" + this.hostname + "'", TAG);
			closeConnection();
		}

    	// Reading WELCOME message
    	try {
    		Utils.printLog(3, "Trying to read WELCOME from '" + this.hostname + "'...", TAG);
			response1 = input.readLine();
			response2 = input.readLine();
		} catch (IOException e) {
			Utils.printLog(1, e.getMessage(), TAG);
		}
    	
    	if (response1.trim().equals("From:" + this.hostname) && response2.trim().equals("Type:HELLO")) {
    		Utils.printLog(3, "Output connection stablished with '" + this.hostname + "'.", TAG);
    		
    		return true;
    	}
    	
    	return false;
    }
	
	public void addData(String data) {
		dataQueue.add(data);
	}
	
	public void closeConnection() {
		try {
			// Clean up
			output.close();
			input.close();
			clientSocket.close();
			
			// Remove from network controller
			NetworkController.removeClientConnection(this.hostname);
			
			// Set flag to stopped
			isStopped = true;
		} catch (IOException e) {
			Utils.printLog(1, "Clossing connection with '" + this.hostname + "'.", TAG);
			e.printStackTrace();
		}
	}
	
	public String getHost() {
		return hostname;
	}
}

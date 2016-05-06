package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
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
    
    private String TAG = "CLIENT SOCKET";
    
    
    public ClientSocket(String address, int port, String hostname) {
    	this.address = address;
    	this.hostname = hostname;
    	
    	dataQueue = new LinkedList<String>();

    	// Instantiate connection socket and output/input stream
        try {
        	clientSocket = new Socket(this.address, port);
            output = new DataOutputStream(clientSocket.getOutputStream());
            input = new DataInputStream(clientSocket.getInputStream());
        } catch (UnknownHostException e) {
        	Utils.printError(1, "Don't know about host " + hostname, TAG);
        } catch (IOException e) {
        	Utils.printError(1, "Couldn't get I/O for the connection to " + hostname, TAG);
        }
        
        // Start connection
        requestConnection();
    }
    
    private boolean requestConnection() {
    	String request = "From:" + RouterController.hostname + "\n" + "Type:HELLO";
    	String response;
    	try {
			output.writeBytes(request);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	try {
			response = input.readLine();
			System.out.println(response);
			response = input.readLine();
			System.out.println(response);
			response = input.readLine();
			System.out.println(response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return true;
    }

	public void run() {
		System.out.println("Enter to run method.");
		while (!isStopped) {
			// If the queue is empty, continue.
			if (dataQueue.isEmpty()) {
				continue;
			}
			
			// If the queue is not empty, send the packet at the head of queue.
			try {
				String data = dataQueue.poll();
				System.out.println("Sending to " + this.hostname + ": " + data );
				output.writeBytes(data);
			} catch (IOException e) {
				Utils.printError(1, "Sending data in node " + this.hostname, TAG);
				e.printStackTrace();
			}
		}
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
			Utils.printError(1, "Clossing client connection in node " + this.hostname, TAG);
			e.printStackTrace();
		}
	}
	
	public String getHost() {
		return hostname;
	}
}

package network;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import router.RouterController;
import main.Utils;

public class ServerRunnable implements Runnable {
	
	protected Socket clientSocket = null;
	protected String address = null;
	protected String hostname;
	BufferedReader input = null;
	PrintWriter output = null;
	
	static String TAG = "LISTENER";
	protected boolean listening = true;
	
	
	public ServerRunnable(Socket clientSocket) {
		this.clientSocket = clientSocket;
		
		// Get remote IP
		try {
			this.address = clientSocket.getLocalAddress().getHostAddress();
		} catch (Exception e1) {
			Utils.printError(1, "Trying to get romote IP from " + hostname, TAG);
			e1.printStackTrace();
		}
		
		try {
			input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		String data, from, type;
		String[] splitted;
		Map<String, Integer> costs;
		int len, cost;
		boolean firstLineReaded = false;
		
		System.out.println("Attending user requests:");

		try {
			// Handshaking
			data = login();
			if (data == null) {
				output.println("Handshaking error! Connection clossed.");
				output.flush();
				closeConnection();
				
				return;
			}
			
			// Handshaking successful
			output.println(
				"From:" + RouterController.hostname + "\n" + 
				"Type:WELCOME"
			);
			output.flush();
			
			this.hostname = data;
			System.out.println("Connection with " + this.hostname + " stablished");
			
			// Create a new output connection for this user
			ClientSocket sender = new ClientSocket(this.address, RouterController.PORT, this.hostname);
			new Thread(sender).start();
			
			// Add listener and sender for this user to the controller class
			NetworkController.addNodeConnection(this, sender);
			
			// Start to listening
			while (true) {
				
				// First line
				if (!firstLineReaded) {
					data = input.readLine();
				}
				splitted = data.split(":");

				if (splitted.length != 2) {
					Utils.printError(2, "Syntax error at line 1 from " + hostname, TAG);
					continue;
				}

				if (!splitted[0].equals("From")) {
					Utils.printError(1, "Syntax error at line 1 from " + hostname, TAG);
					continue;
				}
				
				from = splitted[1].trim();
				
				// Second line
				data = input.readLine();
				if (!data.trim().equals("Type:KeepAlive") && !data.trim().equals("Type:DV")) {
					Utils.printError(1, "Unknow type in received data from " + hostname, TAG);
					continue;
				}
				
				type = data.split(":")[1].trim();
				
				// No error -> build and send packet to network controller
				
				// Matching a KeepAlive packet
				if (type.equals(RouterController.KEEP_ALIVE)) {
					NetworkController.receivePacket(
							new Packet(from, type)
					);
				}
				// Matching a DistanceVector packet
				else {
					data = input.readLine();
					splitted = data.split(":");
					
					if (splitted.length != 2) {
						Utils.printError(2, "Syntax error at line 3 from " + hostname, TAG);
						continue;
					}
					if (!splitted[0].trim().equals("Len")) {
						Utils.printError(2, "Syntax error at line 3 from " + hostname, TAG);
						continue;
					}
					try {
						len = Integer.parseInt(data.split(":")[1]);
					} catch (NumberFormatException e) {
						Utils.printError(2, "Source : " + hostname + e.getMessage(), TAG);
						continue;
					}

					// Parsing costs: <Destiny>:<cost>
					costs = new HashMap<String, Integer>();
					for (int i=0; i<len; i++) {
						data = input.readLine();
						splitted = data.split(":");

						if (splitted.length != 2) {
							Utils.printError(2, "Syntax error at line " + (4 + i)+  " from " + hostname, TAG);
							continue;
						}
						
						try {
							cost = Integer.parseInt(splitted[1].trim());
						} catch (NumberFormatException e) {
							Utils.printError(2, "Source : " + hostname + e.getMessage(), TAG);
							continue;
						}
						
						costs.put(splitted[0].trim(), cost);
					}
					
					NetworkController.receivePacket(
							new Packet(from, RouterController.DV, len, costs)
					);
				}
			}
		}
		catch (IOException error) {
			error.printStackTrace();
		} finally {
			try {
				closeConnection();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Implements login protocol.
	 * @return Host's IP logged when login is successful
	 * 			Null where login was wrong
	 * @throws IOException
	 */
	public String login() throws IOException {
		String request, hostname;
		String[] splitted;
		
		// First line
		request = input.readLine();
		System.out.println("\nNew entry: \n" + request);
		splitted = request.split(":");
		
		if (splitted.length != 2) {
			return null;
		}
		if (!splitted[0].trim().equals("From")) {
			return null;
		}
		hostname = splitted[1].trim();

		// Second line
		request = input.readLine();
		System.out.println("\nNew entry: \n" + request);
		
		splitted = request.split(":");
		
		if (splitted.length != 2) {
			return null;
		}
		if (!splitted[0].trim().equals("Type") || !splitted[1].trim().equals("HELLO")) {
			return null;
		}
		
		// Logged successfully -> return host received
		return hostname;
	}
	
	public void closeConnection() throws IOException {
		// Clean up
		input.close();
		output.close();
		this.clientSocket.close();
		
		// Remove from network controller
		NetworkController.removeServerConnection(this.hostname);
		
		System.out.println("Client thread stopped.");
	}
	
	public String getHost() {
		return this.hostname;
	}
}

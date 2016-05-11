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
		
		System.out.println("Attending user requests:");

		try {
			// ********* Handshaking *********
			this.hostname = login();
			if (this.hostname == null) {
				Utils.printError(2, "Handshaking error! Connection clossed.", TAG);
				closeConnection();
				
				return;
			}
			
			if (NetworkController.existInputConnection(this.hostname)) {
				Utils.printError(2, "Trying to duplicate connection. A listener for " +
						this.hostname + " already exist.", TAG);
				
				return;
			}
			
			// Handshaking successful
			System.out.println("Returning WELCOME message to '" + this.hostname + "'");
			output.println(
				"From:" + RouterController.hostname + "\n" + 
				"Type:WELCOME"
			);
			output.flush();
			
			NetworkController.inputConnections.put(this.hostname, this);
			System.out.println("Connection with '" + this.hostname + "' stablished.");
			
			// Create a new output connection for this user if doesn't exist
			if (!NetworkController.existOutputConnection(this.hostname)) {
				System.out.println("There is no an output connection to '" + this.hostname + "'. Proceeding to create one.");
				ClientSocket sender = new ClientSocket(this.address, RouterController.PORT, this.hostname);
				NetworkController.outputConnections.put(this.hostname, sender);
				new Thread(sender).start();
			}
			

			// ********* Listening *********
			System.out.println("Starting connection listener for '" + this.hostname + "'...");
			while (true) {
				data = input.readLine();
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
					System.out.println("New KEEP_ALIVE packet from '" + this.hostname + "'");
					NetworkController.receivePacket(
							new Packet(from, type)
					);
				}
				// Matching a DistanceVector packet
				else {
					data = input.readLine();
					splitted = data.split(":");
					
					if (splitted.length != 2) {
						Utils.printError(2, "Syntax error at line 3 from " + this.hostname, TAG);
						continue;
					}
					if (!splitted[0].trim().equals("Len")) {
						Utils.printError(2, "Syntax error at line 3 from " + this.hostname, TAG);
						continue;
					}
					try {
						len = Integer.parseInt(data.split(":")[1]);
					} catch (NumberFormatException e) {
						Utils.printError(2, "Number format exception. Data recevide from " + this.hostname, TAG);
						continue;
					}

					// Parsing costs: <Destiny>:<cost>
					costs = new HashMap<String, Integer>();
					for (int i=0; i<len; i++) {
						data = input.readLine();
						splitted = data.split(":");

						if (splitted.length != 2) {
							Utils.printError(2, "Syntax error at line " + (4 + i)+  " from " + this.hostname, TAG);
							continue;
						}
						
						try {
							cost = Integer.parseInt(splitted[1].trim());
						} catch (NumberFormatException e) {
							Utils.printError(2, "Number format exception. Data recevide from " + this.hostname, TAG);
							continue;
						}
						
						costs.put(splitted[0].trim(), cost);
					}
					
					System.out.println("New DV packet from '" + this.hostname + "'");
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
		System.out.println("Login process...");
		String request, hostname;
		String[] splitted;
		
		// First line
		request = input.readLine();
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
		splitted = request.split(":");
		
		if (splitted.length != 2) {
			return null;
		}
		if (!splitted[0].trim().equals("Type") || !splitted[1].trim().equals("HELLO")) {
			return null;
		}
		
		System.out.println("Login process with '" + hostname + "' successfully completed.");
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

package network;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import router.RouterController;
import main.Utils;

public class ServerRunnable implements Runnable {
	
	protected Socket clientSocket = null;
	protected String address = null;
	protected String hostname;
	protected BufferedReader input = null;
	protected PrintWriter output = null;
	
	protected String TAG = "LISTENER";
	protected long lastAlive = 0;
	protected boolean connected = false;
	
	
	public ServerRunnable(Socket clientSocket) {
		this.clientSocket = clientSocket;
		
		// Get remote IP
		try {
			this.address = clientSocket.getInetAddress().getHostAddress();
		} catch (Exception e1) {
			Utils.printLog(1, "Trying to get romote IP from " + hostname, TAG);
			Utils.printLog(1, e1.getMessage(), TAG);
			System.exit(0);
		}
		
		try {
			input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		connected = true;
	}
	
	public void run() {
		String data, from, type;
		String[] splitted;
		Map<String, Integer> costs;
		int len, cost;
		boolean error = false;
		
		Utils.printLog(3, "Attending user requests...", TAG);

		// --------------------- HANDSHAKING ---------------------
		this.hostname = login();
		if (this.hostname == null) {
			Utils.printLog(2, "Handshaking error! Connection clossed.", TAG);
			closeConnection();
			
			return;
		}
		
		if (NetworkController.existInputConnection(this.hostname)) {
			Utils.printLog(2, "Trying to duplicate connection. A listener for " + this.hostname + " already exist.", TAG);
			closeConnection();
			
			return;
		}
		
		// Registering listener socket
		NetworkController.addInputConnection(this.hostname, this);
		
		// Handshaking successful
		Utils.printLog(3, "Returning WELCOME message to '" + this.hostname + "'", TAG);
		output.println(
			"From:" + RouterController.hostname + "\n" + 
			"Type:WELCOME"
		);
		output.flush();
		lastAlive = new Date().getTime();
		
		Utils.printLog(3, "Input connection with '" + this.hostname + "' stablished.", TAG);
		
		// Create a new output connection for this user if doesn't exist.
		if (!NetworkController.existOutputConnection(this.hostname)) {
			Utils.printLog(3, "ServerRunnable: There is no an output connection to '" + this.hostname + "'. Proceeding to create one.", TAG);
			ClientSocket sender = new ClientSocket(this.address, RouterController.PORT, this.hostname);
			new Thread(sender).start();
		}

		// --------------------- LISTENING ---------------------
		Utils.printLog(3, "Starting connection listener for '" + this.hostname + "'...", TAG);
		from = data = "";
		while (connected) {
			// Read until match 'From:Node'
			while (true) {
				try {
					data = input.readLine();
				} catch (IOException e1) {
					Utils.printLog(1, e1.getMessage(), TAG);
					closeConnection();
					error = true;
					break;
				}
				splitted = data.split(":");
				if (splitted.length == 2) {
					if (splitted[0].equals("From")) {
						from = splitted[1].trim();
						break;
					}
				}
			}
			// If previous loop arise in an error, connection was closed.
			if (error) {
				continue;
			}
			
			// Second line
			try {
				data = input.readLine();
			} catch (IOException e1) {
				Utils.printLog(1, e1.getMessage(), TAG);
				closeConnection();
				continue;
			}
			if (!data.trim().equals("Type:KeepAlive") && !data.trim().equals("Type:DV")) {
				Utils.printLog(1, "Unknow type in received data from '" + hostname + "'.", TAG);
				continue;
			}
			
			type = data.split(":")[1].trim();
			
			// No error -> build and send packet to network controller
			
			// Matching a KeepAlive packet
			if (type.equals(RouterController.KEEP_ALIVE)) {
				Utils.printLog(3, "New KEEP_ALIVE packet from '" + this.hostname + "'.", TAG);
				NetworkController.receivePacket(new Packet(from, type));
			}
			// Matching a DistanceVector packet
			else {
				try {
					data = input.readLine();
				} catch (IOException e1) {
					Utils.printLog(1, e1.getMessage(), TAG);
					closeConnection();
					continue;
				}
				splitted = data.split(":");
				
				if (splitted.length != 2) {
					Utils.printLog(2, "Syntax error at line 3 in received data from '" + this.hostname + "'.", TAG);
					continue;
				}
				if (!splitted[0].trim().equals("Len")) {
					Utils.printLog(2, "Syntax error at line 3 in received data from '" + this.hostname + "'.", TAG);
					continue;
				}
				try {
					len = Integer.parseInt(data.split(":")[1]);
				} catch (NumberFormatException e) {
					Utils.printLog(2, "Number format exception. Data recevide from '" + this.hostname + "'.", TAG);
					continue;
				}

				// Parsing costs: <Destiny>:<cost>
				costs = new HashMap<String, Integer>();
				for (int i=0; i<len; i++) {
					error = false;
					try {
						data = input.readLine();
					} catch (IOException e1) {
						Utils.printLog(1, e1.getMessage(), TAG);
						closeConnection();
						error = true;
						break;
					}
					splitted = data.split(":");

					if (splitted.length != 2) {
						Utils.printLog(2, "Syntax error at line " + (4 + i)+  " in received data from '" + this.hostname + "'", TAG);
						continue;
					}
					
					try {
						cost = Integer.parseInt(splitted[1].trim());
					} catch (NumberFormatException e) {
						Utils.printLog(2, "Number format exception. Data recevide from '" + this.hostname + "'.", TAG);
						continue;
					}
					
					costs.put(splitted[0].trim(), cost);
				}
				
				// If previous loop arise in an error, connection was closed.
				if (error) {
					continue;
				}
				
				Utils.printLog(3, "New DV packet from '" + this.hostname + "'", TAG);
				NetworkController.receivePacket(new Packet(from, RouterController.DV, len, costs));
			}
			
			// Last time it was received a packet from this host.
			lastAlive = new Date().getTime();
		}
		Utils.printLog(1, "Closing client thread.", TAG);
	}
	
	/**
	 * Implements login protocol.
	 * @return Host's IP logged when login is successful
	 * 			Null where login was wrong
	 * @throws IOException
	 */
	private String login() {
		Utils.printLog(3, "Login process...", TAG);
		String request, hostname = null;
		String[] splitted;
		
		// No login for NULL connection.
		if (!connected) {
			Utils.printLog(1, "Login attempt failed beacause socket is not connected.", TAG);
			return null;
		}
		
		// First line
		try {
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
		} catch (IOException e) {
			Utils.printLog(1, "Login process failed. " + e.getMessage(), TAG);
			
			return null;
		}
		
		Utils.printLog(3, "Login process with '" + hostname + "' successfully completed.", TAG);

		// Logged successfully -> return host received
		return hostname;
	}
	
	public void closeConnection() {
		// Clean up
		try {
			input.close();
			output.close();
			clientSocket.close();
		} catch (IOException e) {
			Utils.printLog(1, "Clossing connection with '" + this.hostname + "' failed.", TAG);
			Utils.printLog(1, e.getMessage(), TAG);
		}
		
		// Remove from network controller
		NetworkController.removeInputConnection(this.hostname);
		Utils.printLog(1, "Server thread for '" + this.hostname +"' stopped.", TAG);
		connected = false;
	}
	
	public String getHost() {
		return this.hostname;
	}
	
	public long getLastAlive() {
		return this.lastAlive;
	}
	
	public String getAddress() {
		return this.address;
	}
}

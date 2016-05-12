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
	private BufferedReader input = null;
	private PrintWriter output = null;
	
	static String TAG = "LISTENER";
	protected boolean listening = true;
	protected long lastAlive = 0;
	
	
	public ServerRunnable(Socket clientSocket) {
		this.clientSocket = clientSocket;
		
		// Get remote IP
		try {
			this.address = clientSocket.getLocalAddress().getHostAddress();
		} catch (Exception e1) {
			Utils.printLog(1, "Trying to get romote IP from " + hostname, TAG);
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
		
		Utils.printLog(3, "Attending user requests:", TAG);

		try {
			// ********* Handshaking *********
			this.hostname = login();
			if (this.hostname == null) {
				Utils.printLog(2, "Handshaking error! Connection clossed.", TAG);
				closeConnection();
				
				return;
			}
			
			if (NetworkController.existInputConnection(this.hostname)) {
				Utils.printLog(2, "Trying to duplicate connection. A listener for " +
						this.hostname + " already exist.", TAG);
				
				return;
			}
			
			// Handshaking successful
			Utils.printLog(3, "Returning WELCOME message to '" + this.hostname + "'", TAG);
			output.println(
				"From:" + RouterController.hostname + "\n" + 
				"Type:WELCOME"
			);
			output.flush();
			lastAlive = new Date().getTime();
			
			Utils.printLog(3, "Input connection with '" + this.hostname + "' stablished.", TAG);
			
			// Create a new output connection for this user if doesn't exist
			if (!NetworkController.existOutputConnection(this.hostname)) {
				Utils.printLog(3, "ServerRunnable: There is no an output connection to '" + this.hostname + "'. Proceeding to create one.", TAG);
				ClientSocket sender = new ClientSocket(this.address, RouterController.PORT, this.hostname);
				new Thread(sender).start();
			}
			

			// ********* Listening *********
			Utils.printLog(3, "Starting connection listener for '" + this.hostname + "'...", TAG);
			while (true) {
				data = input.readLine();
				splitted = data.split(":");

				if (splitted.length != 2) {
					Utils.printLog(2, "Syntax error at line 1 from " + hostname, TAG);
					continue;
				}

				if (!splitted[0].equals("From")) {
					Utils.printLog(1, "Syntax error at line 1 from " + hostname, TAG);
					continue;
				}
				
				from = splitted[1].trim();
				
				// Second line
				data = input.readLine();
				if (!data.trim().equals("Type:KeepAlive") && !data.trim().equals("Type:DV")) {
					Utils.printLog(1, "Unknow type in received data from " + hostname, TAG);
					continue;
				}
				
				type = data.split(":")[1].trim();
				
				// No error -> build and send packet to network controller
				
				// Matching a KeepAlive packet
				if (type.equals(RouterController.KEEP_ALIVE)) {
					Utils.printLog(3, "New KEEP_ALIVE packet from '" + this.hostname + "'", TAG);
					NetworkController.receivePacket(
							new Packet(from, type)
					);
				}
				// Matching a DistanceVector packet
				else {
					data = input.readLine();
					splitted = data.split(":");
					
					if (splitted.length != 2) {
						Utils.printLog(2, "Syntax error at line 3 from " + this.hostname, TAG);
						continue;
					}
					if (!splitted[0].trim().equals("Len")) {
						Utils.printLog(2, "Syntax error at line 3 from " + this.hostname, TAG);
						continue;
					}
					try {
						len = Integer.parseInt(data.split(":")[1]);
					} catch (NumberFormatException e) {
						Utils.printLog(2, "Number format exception. Data recevide from " + this.hostname, TAG);
						continue;
					}

					// Parsing costs: <Destiny>:<cost>
					costs = new HashMap<String, Integer>();
					for (int i=0; i<len; i++) {
						data = input.readLine();
						splitted = data.split(":");

						if (splitted.length != 2) {
							Utils.printLog(2, "Syntax error at line " + (4 + i)+  " from " + this.hostname, TAG);
							continue;
						}
						
						try {
							cost = Integer.parseInt(splitted[1].trim());
						} catch (NumberFormatException e) {
							Utils.printLog(2, "Number format exception. Data recevide from " + this.hostname, TAG);
							continue;
						}
						
						costs.put(splitted[0].trim(), cost);
					}
					
					Utils.printLog(3, "New DV packet from '" + this.hostname + "'", TAG);
					NetworkController.receivePacket(
							new Packet(from, RouterController.DV, len, costs)
					);
				}
				
				// Last time it was received a packet from this host.
				lastAlive = new Date().getTime();
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
		Utils.printLog(3, "Login process...", TAG);
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
		
		Utils.printLog(3, "Login process with '" + hostname + "' successfully completed.", TAG);
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
		
		Utils.printLog(3, "Client thread stopped.", TAG);
	}
	
	public String getHost() {
		return this.hostname;
	}
	
	public long getLastAlive() {
		return this.lastAlive;
	}
}

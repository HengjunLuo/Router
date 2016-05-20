package forward;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.swing.text.html.HTMLDocument.Iterator;

import client.ClientSocket;
import network.NetworkController;
import network.Packet;
import router.Node;
import router.RouterController;
import main.Utils;

public class ForwardingServer implements Runnable {
	protected Socket clientSocket = null;
	protected String address = null;
	protected String hostname;
	protected String message;
	protected String destiny;
	protected Node adyacentNode;
	private BufferedReader input = null;
	private PrintWriter output = null;
	protected String enteringRequest;
	static String TAG = "LISTENER";
	protected long lastAlive = 0;
	protected boolean connected = false;
	public static final int PORT = 1981;
	private String routerName;
	private Queue queueMsg = new LinkedList();
	public Queue QueueEnteringM = new LinkedList();
	public static Map<String, Node> FinDestiny;
	
	
	public ForwardingServer(Socket clientSocket) {
		this.clientSocket = clientSocket;
		this.readAbsoluteHostname();
		
		
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
		
		connected = true;
	}
	
	
	
	public String getThrougNode(String toHostname){
		FinDestiny = RouterController.getForwardingTable();
		String throughAux = FinDestiny.get(toHostname).getReachedThrough();
		return throughAux;
	}
	
	
	
	
	public void run() {
	 
		Utils.printLog(3, "Attending user requests:", TAG);
		login();
		
		// Busamos en la tabla de checha y le mando el destiny
		// lo cual nos va devolver un hostname ( through ) 
		String finalDest = getThrougNode(destiny);
		
		// this.destiny = through que nos devuelve la tabla de checha
					
		
		
		if(finalDest.equals(this.routerName)){
			// Enviar a sofi
			sendQueue(enteringRequest);
						
		}
		else{
			// Create a new output connection for this user if doesn't exist.
			if (!NetworkController.existOutputConnection(finalDest)) {
				Utils.printLog(3, "ServerRunnable: There is no an output connection to '" + finalDest + "'. Proceeding to create one.", TAG);
				ClientSocket sender = new ClientSocket(this.address, this.PORT, finalDest);
				sender.addData(this.enteringRequest);
				new Thread(sender).start();
			}
		}
		
		
		if(!queueMsg.isEmpty()){
			
			String msg = (String) queueMsg.poll();
			parseRequest(msg);
			
			String des = getThrougNode(destiny );
	
			if (!NetworkController.existOutputConnection(this.hostname)) {
				Utils.printLog(3, "ServerRunnable: There is no an output connection to '" + this.hostname + "'. Proceeding to create one.", TAG);
				ClientSocket sender = new ClientSocket(this.address, this.PORT, des);
				sender.addData(this.enteringRequest);
				new Thread(sender).start();
			}
		}
		
	}
	
	public void sendQueue(String msm){
		QueueEnteringM.add(msm);
	}
	
	public void queueMessage(String msj){
		queueMsg.add(msj);
	}
	
    private void readAbsoluteHostname(){
		File myNameFile = new File("..\\Routing\\src\\main\\resources\\myname.txt");
		// MAC syntax for paths
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			myNameFile = new File("../Routing/src/main/resources/myname.txt");
		}
		if (!myNameFile.exists()) {
			Utils.printLog(1, "Text file with host name doesn't exist.", TAG);
			System.exit(0);
		}
		this.routerName = Utils.readFile(myNameFile).get(0).trim();
		if (this.routerName == null || this.routerName.equals("")) {
			Utils.printLog(1, "Invalid hostname.", TAG);
			System.exit(0);
		}

    }
	
	public void parseRequest(String request){
		String[] splitted;
		// Split del mensaje origen, destiny, Message
		try {
			
			//Leyendo primera línea
			request = input.readLine();
			splitted = request.split(":");
			
			if (splitted.length != 2) {
				System.out.println("La primera línea del formato del mensaje no esta correcto");
				closeConnection();
				
			}
			if (splitted[0].trim().equals("From")) {
				hostname = splitted[1].trim();
			}else{
				System.out.println("La palabra From no existe en el mensaje");
				closeConnection();
			}
			
			// Second line
			request = input.readLine();
			splitted = request.split(":");
			
			if (splitted.length != 2) {
				System.out.println("El formato de la segunda línea no es el correcto");
				closeConnection();
			}
			if (splitted[0].trim().equals("To")) {
				destiny = splitted[1].trim();
			}else{
				System.out.println("No existe To en la segunda línea");
				closeConnection();
			}
			
			// Third line
			request= input.readLine();
			splitted = request.split(":");
			
			if (splitted.length != 2) {
				System.out.println("El formato de la tercera línea no es el correcto");
			}
			if (splitted[0].trim().equals("Msg")) {
				message = splitted[1].trim();
			}else{
				System.out.println("El formate de Mensaje: Msg no es el adecuado");
			}
			
			// Fourth line
			request= input.readLine();
			splitted = request.split(":");
			if (!splitted[0].trim().equals("EOF")) {
				System.out.println("No existe EOF en el mensaje");
			}
			

		} catch (IOException e) {
			Utils.printLog(1, "Login process failed. " + e.getMessage(), TAG);
		}
		
		
	}
	
	public void login() {
		Utils.printLog(3, "Login process...", TAG);
		String request= null;
		String[] splitted;
		
		// No login for NULL connection.
		if (!connected) {
			Utils.printLog(1, "Login attempt failed beacause socket is not connected.", TAG);
		}
		
		// First line
		try {
			request = input.readLine();
			
			parseRequest(request);
			enteringRequest = request;
			
		} catch (IOException e) {
			Utils.printLog(1, "Login process failed. " + e.getMessage(), TAG);
			
		}
		
		Utils.printLog(3, "Login process with '" + hostname + "' successfully completed.", TAG);

		// Logged successfully -> return host received
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
	
	public String getAddress() {
		return this.address;
	}
}


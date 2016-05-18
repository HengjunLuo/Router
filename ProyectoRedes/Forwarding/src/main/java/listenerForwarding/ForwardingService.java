package listenerForwarding;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ForwardingService implements Runnable {
    protected int serverPort;
    protected ServerSocket serverSocket = null;
    protected boolean isStopped = false;
    protected Thread runningThread = null;
    protected ExecutorService threadPool = Executors.newFixedThreadPool(10);
    protected InetAddress address;

    //region Static methods

    static boolean SendMessage(ForwarderMessage message){
        if (message.to.equalsIgnoreCase(Setup.ROUTER_NAME)) {
            // we are the target
            Setup.println("<<Received Incoming Message to ME from " + message.from + ">>\n" + message.text + "\n");
            return true;
        }
        InetAddress addr = null;
        try {
            Setup.println("[ForwardingService.SendMessage] Creando socket a " + message.to);
            Socket socket = new Socket(addr, Setup.FORWARDING_PORT);
            Setup.println("[ForwardingService.SendMessage] Enviando mensaje a " + addr.getHostAddress());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes(message.toString());
            out.flush();
            socket.close();
        } catch (Exception e) {
            Setup.println("[ForwardingService.SendMessage] No es posible enviar mensaje al destino " + e.getMessage());
            return false;
        }
        //JOptionPane.showMessageDialog(null, "El mensaje ha sido enviado a " + message.to);
        return true;
    }

    static ForwardingService server = null;

    public static boolean isServerRunning(){
        return server != null && server.isRunning;
    }

    public static void stop() {
        server.stopServer();
        server = null;
    }

    public static void start(InetAddress address, int port) {
        Setup.println("Forwarder iniciado en " + address.getHostAddress() + ":" + port);
        server = new ForwardingService(address, port);
        new Thread(server).start();
    }

    //endregion // static methods

    //region instance methods

    public ForwardingService(InetAddress address, int port) {
        this.serverPort = port;
        this.address = address;
    }

    protected boolean isRunning = false;

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort, 0, address);
            isRunning = true;
        } catch (IOException e) {
            isRunning = false;
            this.serverSocket = null;
            isStopped = true;
            Setup.println("[ForwardingService.openServerSocket] Forwarder detenido.");
            throw new RuntimeException("No se puede abrir el puerto " + serverPort, e);
        }
    }

    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while (!isStopped()) {
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if (isStopped()) {
                    Setup.println("[ForwardingService.run] Forwarder detenido.");
                    return;
                }
                throw new RuntimeException(
                        "Error aceptando conexion del cliente", e);
            }
            this.threadPool.execute(
                    new ForwarderWorker(clientSocket,
                            "Thread Pooled Server"));
   
        }
        this.threadPool.shutdown();
        Setup.println("[ForwardingService.run] Forwarder detenido.");
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stopServer() {
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error deteniendo el servidor", e);
        }
    }
    
    public void getTable(){
    	
    	
    }

    //endregion // instance methods

    class ForwarderWorker implements Runnable {

        protected Socket clientSocket = null;
        protected String serverText = null;

        public ForwarderWorker(Socket clientSocket, String serverText) {
            this.clientSocket = clientSocket;
            this.serverText = serverText;
            Setup.println("[ForwardingService] Conexion abierta desde: " +
                    clientSocket.getRemoteSocketAddress());
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                //get From:<Name Router>
                String line = in.readLine();
                Setup.println("<<Received FROM client>>\n" + line + "\n");
                //tokenizer From
                StringTokenizer st = new StringTokenizer(line, ":");
                //ignore "From"
                st.nextToken();
                //get name of Router
                String fromId = st.nextToken();

                //get "To:<type>"
                line = in.readLine();
                Setup.println("<<Received TO client>>\n" + line + "\n");
                //tokenizer To
                st = new StringTokenizer(line, ":");
                //ignore "To"
                st.nextToken();
                //get target
                String toId = st.nextToken();
                //get "Msg:msg"
                line = in.readLine();
                Setup.println("<<Received MSG client>>\n" + line + "\n");
                //tokenizer Msg
                st = new StringTokenizer(line, ":");
                //ignore "To"
                st.nextToken();
                String msg = st.nextToken("\n");
                while((line = in.readLine()) != null) {
                    msg += line + "\n";
                }
                in.close();

                if (toId.equalsIgnoreCase(Setup.ROUTER_NAME)) {
                    // we are the target
                    Setup.println("<<Received Incoming Message to ME from " + fromId + ">>\n" + msg + "\n");
                } else {
                    // forward message
                    Setup.println("<<Forwarding Incoming Message to " + toId + " from " + fromId + ">>\n" + msg + "\n");
                    SendMessage(new ForwarderMessage(fromId, toId, msg));
                }

            } catch (IOException e) {
                //report exception somewhere.
                e.printStackTrace();
                Setup.println("[ForwardingService.run] Error: " + e.getMessage());
            }
        }
        
    }
}


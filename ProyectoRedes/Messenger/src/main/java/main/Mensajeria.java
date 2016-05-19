package main;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.channels.Pipe.SinkChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.awt.event.ActionEvent;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import router.RouterController;
import java.awt.Font;
public class Mensajeria {

	private JFrame frmProyectoRouter;
	private Map<String, String> map  = new HashMap<String, String>();
	private ArrayList<String> nodes = new ArrayList<String>();
	public static String hostname;
	public String messageSent;
	private String messageReceived;
	public Queue<String> messagesQueue  = new LinkedList<String>();
	
	
	public String getMessageSent() {
		return messageSent;
	}
	
	public void setMessageReceived(String mensajeRecibido) {
		this.messageReceived = mensajeRecibido;
	}
	
	public String[] parseMessage(String mssgReceived){
		String[] mensajeLineas = mssgReceived.split("\n");
		String[] resultadoMensaje = new String[2];
		String cuerpoMsg = "";
		for(String linea: mensajeLineas){
			if(linea.startsWith("From")){
				String[] token = linea.split(":");
				resultadoMensaje[0] = token[1];
			}
			if(linea.startsWith("Msg")){
				String[] tokenMsg = linea.split(":");
				cuerpoMsg += tokenMsg[1];
			}
			if(!linea.startsWith("From") && !linea.startsWith("To") && !linea.startsWith("Msg") && !linea.startsWith("EOF")){
				cuerpoMsg += " " + linea;
			}
			
		}
		resultadoMensaje[1] = cuerpoMsg;
		System.out.println("From->: " + resultadoMensaje[0] + "\nCuerpo del mensaje: " + resultadoMensaje[1]);
		return resultadoMensaje;
	}
	
	public synchronized void addMessageToQueue(String newMessage){
		messagesQueue.add(newMessage);
	}
	
	
	private String[] updateTable(String nuevosNodosTabla){
		map.remove(map.values());
		String[] nuevaTabla = nuevosNodosTabla.split("\n");
		for(String linea: nuevaTabla){
			String[] nodos = linea.split(",");
			map.put(nodos[0], nodos[1]);
		}
		
		for(String key: map.keySet()){
			nodes.add(key);
		}
		String[] nodosArray = new String[nodes.size()];
		return nodes.toArray(nodosArray);
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Mensajeria window = new Mensajeria();
					window.frmProyectoRouter.setVisible(true);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
//		Thread router = new Thread(new RouterController());
//		router.start();
	}

	/**
	 * Create the application.
	 */
	public Mensajeria() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmProyectoRouter = new JFrame();
		frmProyectoRouter.setFont(new Font("Trebuchet MS", Font.PLAIN, 11));
		frmProyectoRouter.setTitle("Proyecto Router");
		frmProyectoRouter.setBounds(100, 100, 570, 500);
		frmProyectoRouter.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		File myNameFile = new File("..\\Routing\\src\\main\\resources\\myname.txt");
		// MAC syntax for paths
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			myNameFile = new File("../Routing/src/main/resources/myname.txt");
		}
		if (!myNameFile.exists()) {
			Utils.printLog(1, "Text file with host name doesn't exist.", "ROUTER CONTROLLER");
			System.exit(0);
		}
		hostname = Utils.readFile(myNameFile).get(0).trim();
		
		
		
		/*Estos valores solo los puse de ejemplo en el hashmap de 
		 * forwarding table para probar que si muestra en el 
		 * JList los nodos existentes*/  
		map.put("A", "B");
		map.put("C", "D");
		map.put("E", "F");
		map.put("G", "H");
		map.put("I", "J");
		map.put("K", "L");
		map.put("M", "N");
		map.put("O", "P");
		map.put("Q", "R");
		map.put("S", "T");
		map.put("U", "V");
		map.put("W", "X");
		map.put("Y", "Z");
		
		messageReceived = "From:REMITENTE"
				+ "\nTo:YO"
				+ "\nMsg:hola"
				+ "\nque hace"
				+ "\nhaciendo pruebas"
				+ "\no que hace"
				+ "\n."
				+ "\nEOF";
		frmProyectoRouter.getContentPane().setLayout(new BorderLayout(0, 0));

		final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
		frmProyectoRouter.getContentPane().add(tabbedPane);
		
		JPanel recibidosPanel = new JPanel();
		tabbedPane.addTab("Recibidos", null, recibidosPanel, null);
		recibidosPanel.setLayout(null);
		
		final JPanel enviarPanel = new JPanel();
		tabbedPane.addTab("Enviar Nuevo", null, enviarPanel, null);
		enviarPanel.setLayout(null);
		
		JLabel lblCuerpoDelMensaje_1 = new JLabel("Mensaje:");
		lblCuerpoDelMensaje_1.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		lblCuerpoDelMensaje_1.setBounds(46, 341, 83, 14);
		enviarPanel.add(lblCuerpoDelMensaje_1);
		
		JScrollPane msjNvoScrollPane = new JScrollPane();
		msjNvoScrollPane.setBounds(144, 325, 395, 50);
		enviarPanel.add(msjNvoScrollPane);
		
		final JTextArea mensajeTextArea = new JTextArea();
		mensajeTextArea.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
		mensajeTextArea.setBounds(250, 41, 278, 321);
		msjNvoScrollPane.setViewportView(mensajeTextArea);
		//enviarPanel.add(mensajeTextArea);
		
		JLabel lblEnviarA = new JLabel("Enviar a nodo:");
		lblEnviarA.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		lblEnviarA.setBounds(46, 11, 137, 14);
		enviarPanel.add(lblEnviarA);
		
		
		
		JScrollPane listScroller = new JScrollPane();
		listScroller.setBounds(46, 36, 83, 278);
		enviarPanel.add(listScroller);
		
		
		
		for(String key: map.keySet()){
			nodes.add(key);
		}
		
		final String[] nodosArray = new String[nodes.size()];
//		nodos.toArray(nodosArray);
		final JList nodosList = new JList(nodes.toArray(nodosArray));
		nodosList.setFont(new Font("Trebuchet MS", Font.BOLD, 13));
		nodosList.setBounds(0, 0, 95, 95);
		nodosList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		nodosList.setLayoutOrientation(JList.VERTICAL);
		nodosList.setVisibleRowCount(-1);
		listScroller.setViewportView(nodosList);
		
		JScrollPane msjsEnviadosScrollPane = new JScrollPane();
		msjsEnviadosScrollPane.setBounds(144, 36, 395, 278);
		enviarPanel.add(msjsEnviadosScrollPane);
		
		final JTextArea msjsEnviadosTextArea = new JTextArea();
		msjsEnviadosTextArea.setEditable(false);
		msjsEnviadosTextArea.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
		msjsEnviadosScrollPane.setViewportView(msjsEnviadosTextArea);
		
		JButton btnEnviarMensaje = new JButton("Enviar Mensaje");
		btnEnviarMensaje.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		btnEnviarMensaje.setBounds(402, 386, 137, 23);
		btnEnviarMensaje.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cuerpoMsj = mensajeTextArea.getText();
				String para = nodosArray[nodosList.getSelectedIndex()];
				
				messageSent = 
						"From:" + hostname
						+ "\nTo:"+ para
						+ "\nMsg:"+ cuerpoMsj + 
						"\nEOF";
				System.out.println(messageSent);
				msjsEnviadosTextArea.setText( msjsEnviadosTextArea.getText() + "Para " + para + ": " + cuerpoMsj + "\n"+
				"-----------------------------------------------------------------------------\n");
				/*AQUI LE TENEMOS QUE PASAR mensajeEnviado 
				 * A LA CLASE DE FORWARDING PARA QUE ESTA 
				 * LO MANDE A QUIEN CORRESPONDA*/
			}
		});
		enviarPanel.add(btnEnviarMensaje);
	
		
		JLabel recibidosLabel = new JLabel("Mensajes Recibidos");
		recibidosLabel.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		recibidosLabel.setBounds(25, 30, 217, 14);
		recibidosPanel.add(recibidosLabel);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(25, 55, 514, 307);
		recibidosPanel.add(scrollPane);
	
		final JTextArea msgRcbTextArea = new JTextArea();
		msgRcbTextArea.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
		msgRcbTextArea.setEditable(false);
		msgRcbTextArea.setBounds(25, 55, 514, 307);
		scrollPane.setViewportView(msgRcbTextArea);
		
		JButton btnMostrarMsjPrueba = new JButton("Actualizar chat");
		btnMostrarMsjPrueba.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		btnMostrarMsjPrueba.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				String[] extract = parseMessage();
//				msgRcbTextArea.setText( msgRcbTextArea.getText() + "\n" +extract[0] + " dice: " + extract[1] +"\n");
			}
		});
		btnMostrarMsjPrueba.setBounds(393, 373, 146, 23);
		recibidosPanel.add(btnMostrarMsjPrueba);
		
		JPanel ingresoManualPanel = new JPanel();
		tabbedPane.addTab("Tabla de Forwarding", null, ingresoManualPanel, null);
		ingresoManualPanel.setLayout(null);
		
		JScrollPane forwardScrollPane = new JScrollPane();
		forwardScrollPane.setBounds(10, 11, 529, 342);
		ingresoManualPanel.add(forwardScrollPane);
		
		final JTextArea tablaForwardTextArea = new JTextArea();
		tablaForwardTextArea.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
		tablaForwardTextArea.setBounds(0, 0, 4, 22);
		forwardScrollPane.setViewportView(tablaForwardTextArea);
		
		JButton btnIngresarTabla = new JButton("Ingresar Tabla");
		btnIngresarTabla.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(tablaForwardTextArea.getText()!=null){
					map.remove(map.values());
					//System.out.println(map.size());
					String[] nuevaTabla = tablaForwardTextArea.getText().split("\n");
					for(String linea: nuevaTabla){
						String[] nodos = linea.split(",");
						map.put(nodos[0], nodos[1]);
					}
				}
				for(String key: map.keySet()){
					
					String value = map.get(key);
					System.out.println(key + ", " + value);
				}
			}
		});
		btnIngresarTabla.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
		btnIngresarTabla.setBounds(416, 377, 123, 23);
		ingresoManualPanel.add(btnIngresarTabla);
		
		messagesQueue.add("From:Sophia\nTo:A\nMsg:hola que hace\nEOF");
		while(!messagesQueue.isEmpty()){
			messageReceived = messagesQueue.poll();
			String[] extract = parseMessage(messageReceived);
			msgRcbTextArea.setText( msgRcbTextArea.getText() + "\n" +extract[0] + " dice: " + extract[1] +"\n");
			
		}
		
	}

	
}

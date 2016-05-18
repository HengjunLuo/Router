package main;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JLabel;
import java.awt.GridLayout;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.awt.event.ActionEvent;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import java.awt.FlowLayout;
import router.RouterController;
import java.awt.Font;
public class Mensajeria {

	private JFrame frmProyectoRoute;
	private JTable table;
	private Map<String, String> map  = new HashMap<String, String>();
	private ArrayList<String> nodos = new ArrayList<String>();
	private DefaultListModel<String> listModel = new DefaultListModel<String>();
	public static String hostname;
	public String mensajeEnviado;
	private String mensajeRecibido;
	public DefaultTableModel model;
	
	
	
	public String getMensajeEnviado() {
		return mensajeEnviado;
	}
	
	public void setMensajeRecibido(String mensajeRecibido) {
		this.mensajeRecibido = mensajeRecibido;
	}
	
	public String[] extraerMensaje(){
		String[] mensajeLineas = mensajeRecibido.split("\n");
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

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Mensajeria window = new Mensajeria();
					window.frmProyectoRoute.setVisible(true);
					
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
		frmProyectoRoute = new JFrame();
		frmProyectoRoute.setFont(new Font("Trebuchet MS", Font.PLAIN, 11));
		frmProyectoRoute.setTitle("Proyecto Router");
		frmProyectoRoute.setBounds(100, 100, 570, 500);
		frmProyectoRoute.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
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
		 * forwarding table para probar que si genera los
		 * radio buttons a partir de los nodos existentes  */ 
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
		
		mensajeRecibido = "From:REMITENTE"
				+ "\nTo:YO"
				+ "\nMsg:hola"
				+ "\nque hace"
				+ "\nhaciendo pruebas"
				+ "\no que hace"
				+ "\n."
				+ "\nEOF";
		frmProyectoRoute.getContentPane().setLayout(new BorderLayout(0, 0));
		
		
		
		final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
		frmProyectoRoute.getContentPane().add(tabbedPane);
		
		JPanel recibidosPanel = new JPanel();
		tabbedPane.addTab("Recibidos", null, recibidosPanel, null);
		recibidosPanel.setLayout(null);
		
		final JPanel enviarPanel = new JPanel();
		tabbedPane.addTab("Enviar Nuevo", null, enviarPanel, null);
		
		//listScroller.setPreferredSize(new Dimension(250, 80));
		

		for(String key: map.keySet()){
			//final JRadioButton button1 = new JRadioButton(key);
			//nodosPanel.add(button1);
			nodos.add(key);
			//listModel.addElement(key);
		}
		final String[] nodosArray = new String[nodos.size()];
		nodos.toArray(nodosArray);
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
		
		//System.out.println(nodosArray.length);
		
		
		final JList nodosList = new JList(nodos.toArray(nodosArray));
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
				
				mensajeEnviado = 
						"From:" + hostname
						+ "\nTo:"+ para
						+ "\nMsg:"+ cuerpoMsj + 
						"\nEOF";
				System.out.println(mensajeEnviado);
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
		
		/*DefaultTableModel dtm = new DefaultTableModel(0,2);
		table = new JTable(dtm);
		model = (DefaultTableModel) table.getModel();
		table.setBounds(35, 55, 622, 155);
		recibidosPanel.add(table);
		
		
		
		JLabel lblCuerpoDelMensaje = new JLabel("Cuerpo del Mensaje:");
		lblCuerpoDelMensaje.setBounds(25, 241, 200, 14);
		recibidosPanel.add(lblCuerpoDelMensaje);*/
		
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
				String[] extract = extraerMensaje();
				msgRcbTextArea.setText( msgRcbTextArea.getText() + "\n" +extract[0] + " dice: " + extract[1] +"\n");
			}
		});
		btnMostrarMsjPrueba.setBounds(393, 373, 146, 23);
		recibidosPanel.add(btnMostrarMsjPrueba);
		
		;
		
		
	}
}

package listenerForwarding;
import java.awt.*;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Setup {
    public static final int FORWARDING_PORT = 1981; //default port
    public static InetAddress address = null;
    public static String ROUTER_NAME;


    public static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public static void println(String s){
        print(s + "\n");
    }

    public static void print(String s){
        System.out.print(s);
    }

    public static void println(){
        print("\n");
    }

    public static void main(String[] args) throws IOException {
        int num = 0;
        if (args.length == 1 || args.length > 3) {
            if (args[0].equals("-i")) {
                if (args.length > 3) {
                    try {
                        num = Integer.parseInt(args[1]);
                    } catch (NumberFormatException nfe) {
                    }
                }
                Enumeration nifEnm = NetworkInterface.getNetworkInterfaces();
                while (nifEnm.hasMoreElements()) {
                    NetworkInterface nif = (NetworkInterface) nifEnm.nextElement();
                    if (!nif.isLoopback() && nif.getInterfaceAddresses().size() > 0) {
                        Enumeration addrEnum = nif.getInetAddresses();
                        while (addrEnum.hasMoreElements()) {
                            InetAddress a = (InetAddress) addrEnum.nextElement();
                            if (a instanceof Inet4Address) {
                                // mostrar interfaces?
                                if (num == 0) {
                                    System.out.println(String.format("%d\t%s\t%s",
                                            nif.getIndex(), nif.getName(), nif.getDisplayName()));
                                    System.out.println("\t" + a.getHostAddress());
                                } else if (num == nif.getIndex()) {
                                    address = a;
                                }
                            } // end-if is inet4
                        } // end-while address
                    } //end-if not loopback
                } // end-while interfaces
            } else {
                showHelp();
            } // end-if -i
        } else {
            showHelp();
        } // end-if args


        if (address == null) {
            if (num > 0) {
                System.err.println("El numero de interfaz es invalido!");
                System.exit(2);
            } else {
                showHelp();
            }
        } else {
            ROUTER_NAME = args[2];
            // parsear vecinos
            int n = args.length - 3;

            StringTokenizer st;

            for (int i = 0; i < n; i++) {
                try{
                    st = new StringTokenizer(args[i+3], ":");
                    String name = st.nextToken();
                    InetAddress addr = InetAddress.getByName(st.nextToken());

                } catch (Exception ex) {
                    System.out.println("Error parsing neighbors: " + ex.getMessage());
                    showHelp();
                }
            }

            System.out.println("Utilizando IP: " + address.getHostAddress());

            //Router r = new Router(ROUTER_NAME, address, ROUTING_PORT, nbrList);
            //r.run();

     
          
        }
    }

    public static void showHelp() {
        System.out.println();
        System.out.println("Uso: java Setup -i [interface ROUTER_NAME NAME:NEIGHBOR:COST [NAME:NEIGHBOR:COST ...]]");
        System.exit(1);
    }

}

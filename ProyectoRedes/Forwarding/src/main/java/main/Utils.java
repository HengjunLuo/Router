package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class Utils {
	private static final Pattern PATTERN = Pattern.compile(
	        "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

	/**
	 * Match an V4 IP as string.
	 * @param ip
	 * @return
	 */
	public static boolean validate(String ip) {
	    return PATTERN.matcher(ip).matches();
	}
	
	/**
	 * Search for an V4 IP within interface received.
	 * @param interfaceName
	 * @return
	 */
	public static String getIPV4Address(String interfaceName) {
		try {
			// Look through all interfaces
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface next = interfaces.nextElement();
				if (next.getDisplayName().equalsIgnoreCase(interfaceName)) {
					Enumeration<InetAddress> addresses = next.getInetAddresses();
					InetAddress address = null, result = null;
					int count = 0;
					while (addresses.hasMoreElements()) {
						address = addresses.nextElement();
						if (Utils.validate(address.getHostAddress())) {
							result = address;
							count++;
						}
					}
					// check that only one IP found
					if (count == 1) {
						return result.getHostAddress();
					} else {
						System.out.println("WARNING: " + count + " IP addresses found for '" + interfaceName + "' interface");
						break;
					}
				}
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			System.out.println("ERROR: Unexpected error trying to find IP address.");
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static void printLog(int type, String error, String TAG) {
		switch (type) {
			case 1:
				System.err.println("ERROR - " + TAG + ": " + error);
				break;
			case 2:
				System.out.println("WARNING - " + TAG + ": " + error);
				break;
			case 3:
				System.out.println("INFO - " + TAG + ": " + error);
				break;
		}
	}
	
	public static ArrayList<String> readFile(File file) {
        FileReader fr = null;
        BufferedReader br = null;
        ArrayList<String> content = new ArrayList<String>();
//        System.out.println("Reading file " + file.getName() + "...");
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            String line;
            while((line = br.readLine()) != null) {
            	if (line.trim().equals(""))
            		continue;
                content.add(line.trim());
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if( null != fr ){
                    fr.close();
                }
            } catch (Exception e2){
               e2.printStackTrace();
            }
        }
        
        return content;
	}
}

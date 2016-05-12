package network;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import main.Utils;
import router.RouterController;

public class ConnectivityChecker implements Runnable {
	
	private String TAG = "ReconnectionChecker";

	public ConnectivityChecker(){}
	
	public void run() {
		// Initial sleep
	    try {
	        Thread.sleep(RouterController.timeU * 1000);
	    } catch (InterruptedException ex) {
	    	Utils.printLog(2, ex.getMessage(), TAG);
	    }
	    Timer timer = new Timer();
	    
	    // Check each RouterController.timeU seconds
	    timer.schedule(
    		new TimerTask() {
    			public void run() {
    				NetworkController.checkKeepAlive();
    			}
    		},
    		0,
    		RouterController.timeU * 1000
	    ); 
    }
}
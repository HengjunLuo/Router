package network;

import java.util.Timer;
import java.util.TimerTask;

import main.Utils;
import router.RouterController;

public class SenderThread implements Runnable {
	
	private String TAG = "SenderThread";

	public SenderThread(){}
	
	public void run() {
		// Initial sleep
	    try {
	        Thread.sleep(RouterController.timeT * 1000);
	    } catch (InterruptedException ex) {
	    	Utils.printLog(2, ex.getMessage(), TAG);
	    }
	    Timer timer = new Timer();
	    
	    // Check each RouterController.timeU seconds
	    timer.schedule(
    		new TimerTask() {
    			public void run() {
    				RouterController.considerSendingPackets();
    			}
    		},
    		0,
    		RouterController.timeT * 1000
	    ); 
    }
}
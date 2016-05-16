package main;

import router.RouterController;

public class Main {

	public static void main(String[] args) {
		
		 //RouterController router = new RouterController();
		 Thread router = new Thread(new RouterController());
		 router.start();

	}

}
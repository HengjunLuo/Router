package main;

import router.RouterController;

public class Main {
	public static void main(String[] args) {
		 Thread router = new Thread(new RouterController());
		 router.start();
	}
}
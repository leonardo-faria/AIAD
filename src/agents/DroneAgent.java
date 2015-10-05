package agents;

import credit.Credit;
import sajas.core.Agent;
import tools.Tool;

public class DroneAgent extends Worker {

	private static int VELOCITY = 5;
	private static boolean ROAD = false; //true estrada, false ar
	private static int BATTERY_CAPACITY = 250;
	private static int LOAD_CAPACITY = 100;
	private static Tool f1;
	
	private Credit credit;
	private int batteryLeft;
	private int loadLeft;
	private int position[];
}

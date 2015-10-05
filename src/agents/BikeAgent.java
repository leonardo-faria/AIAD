package agents;

import credit.Credit;
import sajas.core.Agent;
import tools.Tool;

public class BikeAgent extends Worker {

	private static int VELOCITY = 4;
	private static boolean ROAD = true; //true estrada, false ar
	private static int BATTERY_CAPACITY = 350;
	private static int LOAD_CAPACITY = 300;
	private static Tool f1;
	private static Tool f3;
	
	private Credit credit;
	private int batteryLeft;
	private int loadLeft;
	private int position[];
	
}

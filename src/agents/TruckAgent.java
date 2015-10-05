package agents;

import credit.Credit;
import sajas.core.Agent;
import tools.Tool;

public class TruckAgent extends Worker {

	private static int VELOCITY = 1;
	private static boolean ROAD = true; //true estrada, false ar
	private static int BATTERY_CAPACITY = 3000;
	private static int LOAD_CAPACITY = 1000;
	private static Tool f2;
	private static Tool f3;
	
	private Credit credit;
	private int batteryLeft;
	private int loadLeft;
	private int position[];
}

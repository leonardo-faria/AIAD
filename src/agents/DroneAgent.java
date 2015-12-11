package agents;

import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;

public class DroneAgent extends Worker {

	public DroneAgent(Coord c, Object2DGrid space) {
		super(c, space);
		charge = 10;
		maxCharge = 250;
		speed = 1;
		maxload = 100;
		tools.add("1");
	}	
}

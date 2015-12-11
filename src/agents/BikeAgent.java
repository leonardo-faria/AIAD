package agents;

import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;

public class BikeAgent extends Worker {

	public BikeAgent(Coord c, Object2DGrid space) {
		super(c, space);
		charge = 10;
		maxCharge = 350;
		speed = 2;
		maxload = 300;
		tools.add("1");
		tools.add("3");
	}	
}

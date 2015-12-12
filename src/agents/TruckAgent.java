package agents;

import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;

public class TruckAgent extends Worker {

	public TruckAgent(Coord c, Object2DGrid space) {
		super(c, space);
		charge = 10;
		maxCharge = 3000;
		speed = 5;
		maxload = 1000;
		tools.add("2");
		tools.add("4");
	}	
}

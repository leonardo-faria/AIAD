package agents;

import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;

public class CarAgent extends Worker {

	public CarAgent(Coord c, Object2DGrid space) {
		super(c, space);
		charge = 10;
		maxCharge = 500;
		speed = 3;
		maxload = 550;
		tools.add("1");
		tools.add("2");
	}
}

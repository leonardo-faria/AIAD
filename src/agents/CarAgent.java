package agents;

import java.util.ArrayList;

import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;

public class CarAgent extends Worker {

	public CarAgent(Coord c, Object2DGrid space) {
		super(c, space);
		charge= 10;
		maxCharge= 1000;
		speed=1;
//		addBehaviour(createMoves(makeRoute(c, new Coord(80, 80))));
//		Job j = new Job(createMovess(makeRoute(c, new Coord(80, 80))), new ArrayList<>(), 0);
//		addBehaviour(j);
	}
}

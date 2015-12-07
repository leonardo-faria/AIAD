package actions;

import agents.Worker;
import sajas.core.behaviours.SimpleBehaviour;
import utils.Coord;

public class Charge extends SimpleBehaviour {

	private static final long serialVersionUID = 1L;

	Coord location;

	public Charge(Coord c) {
		location = c;
	}

	@Override
	public void action() {
		if (((Worker) myAgent).getX() == 1)
			return;
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}

}

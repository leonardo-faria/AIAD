package actions;

import agents.Worker;
import sajas.core.behaviours.SimpleBehaviour;
import utils.Coord;

public class Charge extends SimpleBehaviour {

	private static final long serialVersionUID = 1L;

	Coord location;
	boolean done;
	public Charge(Coord c) {
		location = c;
		done=false;
	}

	@Override
	public void action() {
		if (((Worker) myAgent).getCoord().equals(location))
		{
			System.out.println("charged!");
			((Worker) myAgent).fullCharge();
			done=true;
		}
	}

	@Override
	public boolean done() {
		return done;
	}

}

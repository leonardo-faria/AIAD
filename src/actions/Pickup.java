package actions;

import agents.Holder;
import agents.Worker;
import product.Product;
import sajas.core.behaviours.SimpleBehaviour;

public class Pickup extends SimpleBehaviour {

	private static final long serialVersionUID = 1L;
	Holder location;
	Product p;
	boolean done;

	/**
	 * 
	 * @param p product
	 * @param c	place from where to pickup
	 */
	public Pickup(Product p, Holder c) {
		this.p=p;
		this.location=c;
		done=false;
	}

	@Override
	public void action() {
		if (((Worker) myAgent).getCoord().equals(location.getCoord()))
		{
			p.getLocation().drop(p);
			((Worker) myAgent).pickup(p);
			p.setLocation(((Worker) myAgent));
			done = true;
		}
	}

	@Override
	public boolean done() {
		return done;
	}
}

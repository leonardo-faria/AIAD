package actions;

import agents.Holder;
import agents.Worker;
import product.Product;
import sajas.core.behaviours.SimpleBehaviour;

public class Drop extends SimpleBehaviour {

	private static final long serialVersionUID = 1L;
	Holder location;
	Product p;
	boolean done;

	/**
	 * 
	 * @param p product to drop
	 * @param c location to where to drop
	 */
	public Drop(Product p, Holder c) {
		this.p=p;
		this.location=c;
		done=false;
	}
	
	@Override
	public void action() {
		if (((Worker) myAgent).getCoord().equals(location.getCoord()))
		{
			p.getLocation().drop(p);
			location.pickup(p);
			p.setLocation(location);
			done = true;
		}
	}

	@Override
	public boolean done() {
		return done;
	}

}

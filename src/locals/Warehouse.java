package locals;

import java.util.ArrayList;

import agents.Holder;
import product.Product;
import utils.Coord;

public class Warehouse extends Local implements Holder {
	ArrayList<Product> stored;
	Coord pos;
	
	public Warehouse(Coord p) {
		pos = p;
		stored = new ArrayList<Product>();
	}
	
	@Override
	public void pickup(Product p) {
		stored.add(p);
	}

	@Override
	public void drop(Product p) {
		stored.remove(p);
	}

	@Override
	public Coord getCoord() {
		return pos;
	}

}

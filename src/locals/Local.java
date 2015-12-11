package locals;

import java.util.ArrayList;

import agents.Holder;
import product.Product;
import utils.Coord;

public class Local implements Holder {
	ArrayList<Product> stored;
	Coord pos;
	String type;
	int id;

	public Local(Coord p, int id, String type, ArrayList<Product> stored){
		pos = p;
		this.id = id;
		this.type = type;
		this.stored = stored;
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
	
	@Override
	public String getName(){
		return type + id;
	}

}

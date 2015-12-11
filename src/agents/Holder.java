package agents;

import product.Product;
import utils.Coord;

public interface Holder {
	public void pickup(Product p);

	public void drop(Product p);

	public Coord getCoord();
	
	public String getName();
}

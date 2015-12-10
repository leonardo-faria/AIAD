package locals;

import java.util.ArrayList;

import agents.Holder;
import product.Product;
import utils.Coord;

public class Warehouse extends Local {
	
	public Warehouse(Coord p,int id) {
		super(p,id,"Warehouse",new ArrayList<Product>());
	}
}

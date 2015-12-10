package product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import agents.Holder;
import javafx.util.Pair;
import product.Product.ProSpecs;

public class CompositeProduct extends Product {


	static HashMap<String, CompProSpecs> compositeProductTypes = new HashMap<>();
	
	class CompProSpecs extends ProSpecs{
		ArrayList<ProSpecs> materias;
		public CompProSpecs(int weight, int price, ArrayList<ProSpecs> m) {
			super(weight, price);
			materials = m;
		}

		public ArrayList<ProSpecs> materials;
	}
	
	public CompositeProduct(String name, Holder owner){
		if(compositeProductTypes.containsKey(name))
			compositeProductTypes.put(name, new CompProSpecs(0, 0,new ArrayList<>()));
		this.name = name;
		weight = productTypes.get(name).weight;
		this.location = owner;
		id = new AtomicInteger(idgen.incrementAndGet());
		
	}

	@Override
	public int getCost() {
		return 0;
	}

	
}

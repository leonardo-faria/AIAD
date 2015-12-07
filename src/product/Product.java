package product;

import java.util.ArrayList;
import java.util.HashMap;

import agents.Holder;
import agents.Worker;
import javafx.util.Pair;

public class Product {
	String name;
	int weight;
	Worker Owner;
	Holder location;
	int id;
	static int idgen = 0;

	static HashMap<String, Pair<Integer, ArrayList<String>>> productTypes = new HashMap<String, Pair<Integer, ArrayList<String>>>();

	public Product(String name, Holder owner) throws Exception {
		if (!productTypes.containsKey(name))
			throw new Exception("inexistent type:" + name);
		this.name = name;
		weight = productTypes.get(name).getKey();
		this.location = owner;
		id=idgen;
	}

	
	@Override
	public boolean equals(Object obj) {
		return id == ((Product) obj).id;
	}


	static public void addType(String s, int w, ArrayList<String> a) {
		productTypes.put(s, new Pair<Integer, ArrayList<String>>(w, a));
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public Holder getLocation() {
		return location;
	}

	public void setLocation(Holder location) {
		this.location = location;
	}

	public static HashMap<String, Pair<Integer, ArrayList<String>>> getProductTypes() {
		return productTypes;
	}

}

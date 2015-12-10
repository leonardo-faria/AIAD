package product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import agents.Holder;
import agents.Worker;
import javafx.util.Pair;

public class Product {
	String name;
	int weight;
	Worker Owner;
	Holder location;
	AtomicInteger id;
	static AtomicInteger idgen = new AtomicInteger(0);

	static HashMap<String,  ProSpecs> productTypes = new HashMap<>();
	
	public class ProSpecs{
		public int weight;
		public int price;
		public ProSpecs(int weight, int price) {
			super();
			this.weight = weight;
			this.price = price;
		}
	}
	
	
	private int price;
	
	public Product(String name, Holder owner) {
		if (!productTypes.containsKey(name))
			productTypes.put(name, new ProSpecs(0, 0));
		this.name = name;
		weight = productTypes.get(name).weight;
		this.location = owner;
		id = new AtomicInteger(idgen.incrementAndGet());
	}

	@Override
	public boolean equals(Object obj) {
		return id == ((Product) obj).id;
	}

	static public void addType(String s,ProSpecs p) {
		productTypes.put(s, p);
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

	public static HashMap<String, ProSpecs> getProductTypes() {
		return productTypes;
	}

	public int getCost(){
		return price;
	}
}

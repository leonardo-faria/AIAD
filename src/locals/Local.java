package locals;

import java.awt.Color;
import java.util.ArrayList;

import agents.Holder;
import product.Product;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import utils.Coord;

public class Local implements Holder, Drawable{
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

	@Override
	public void draw(SimGraphics g) {
		g.setDrawingCoordinates(pos.getX() * g.getCurWidth(),
				pos.getY() * g.getCurHeight(), 0);
		g.drawFastRect(Color.blue);
		
	}

	@Override
	public int getX() {
		return pos.getX();
	}

	@Override
	public int getY() {
		return pos.getY();
	}

}

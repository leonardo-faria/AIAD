package locals;

import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import utils.Coord;

public class Store implements Drawable{

	Coord pos;
	
	public Store(Coord pos) {
		super();
		this.pos = pos;
	}

	public Coord getPos() {
		return pos;
	}

	@Override
	public void draw(SimGraphics g) {
		g.setDrawingCoordinates(pos.getX() * g.getCurWidth(),
				pos.getY() * g.getCurHeight(), 0);
		g.drawFastRect(Color.pink);
		
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

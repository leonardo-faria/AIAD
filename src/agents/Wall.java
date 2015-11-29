package agents;

import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

public class Wall implements Drawable {
	ArrayList<ArrayList<Boolean>> map;

	public Wall(String filename) {
		map=new ArrayList<ArrayList<Boolean>>();
	}

	@Override
	public void draw(SimGraphics g) {
		for (int i = 0; i < map.size(); i++) {
			for (int j = 0; j < map.size(); j++) {
				g.setDrawingCoordinates(i * g.getCurWidth(), j * g.getCellHeightScale(), 0);
				g.drawFastRect(Color.red);
			}
		}
	}

	public int getX() {
		return 0;
	}

	public int getY() {
		return 0;
	}

}

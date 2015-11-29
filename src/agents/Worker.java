package agents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import sajas.core.Agent;
import uchicago.src.sim.gui.DisplayConstants;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

public abstract class Worker extends Agent implements Drawable {
	int x, y;

	public Worker(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public void draw(SimGraphics g) {
		g.setDrawingCoordinates(x * g.getCurWidth(), y * g.getCellHeightScale(), 0);
		g.drawFastRect(Color.green);
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}
}

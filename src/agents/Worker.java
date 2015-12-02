package agents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import main.Main;
import sajas.core.Agent;
import sajas.core.behaviours.SimpleBehaviour;
import uchicago.src.sim.gui.DisplayConstants;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;
import utils.DefaultHashMap;

public abstract class Worker extends Agent implements Drawable {

	Coord pos;
	Object2DGrid space;
	LinkedList<Move> moves;

	public class Move extends SimpleBehaviour {
		private static final long serialVersionUID = 1L;
		Coord c;
		boolean done = false;

		public Move(Coord c) {
			this.c = c;
		}

		@Override
		public void action() {
			space.putObjectAt(Worker.this.pos.getX(), Worker.this.pos.getY(), null);
			Worker.this.pos.setX(c.getX());
			Worker.this.pos.setY(c.getY());
			space.putObjectAt(Worker.this.pos.getX(), Worker.this.pos.getY(), Worker.this);
			done = true;
		}

		@Override
		public boolean done() {
			return done;
		}

	}

	public Worker(Coord c, Object2DGrid space) {
		pos = c;
		this.space = space;
		moves = new LinkedList<Move>();
	}

	@Override
	public void draw(SimGraphics g) {
		g.setDrawingCoordinates(pos.getX() * g.getCurWidth(), pos.getY() * g.getCurHeight(), 0);
		g.drawFastRect(Color.green);
	}

	public int getX() {
		return pos.getX();
	}

	public void setX(int x) {
		this.pos.setX(x);
	}

	public int getY() {
		return pos.getY();
	}

	public void setY(int y) {
		this.pos.setY(y);
	}

	public void makeRoute(Coord start, Coord goal) {

		ArrayList<Coord> openSet = new ArrayList<Coord>();
		openSet.add(start);
		ArrayList<Coord> closedSet = new ArrayList<Coord>();
		HashMap<Coord, Coord> cameFrom = new HashMap<Coord, Coord>();

		DefaultHashMap<Coord, Integer> g_score = new DefaultHashMap<Coord, Integer>(Integer.MAX_VALUE);
		g_score.put(start, 0);
		DefaultHashMap<Coord, Integer> f_score = new DefaultHashMap<Coord, Integer>(Integer.MAX_VALUE);
		f_score.put(start, g_score.get(start) + Coord.heuristic(start, goal) + 10);
		while (!openSet.isEmpty()) {

			Coord current = f_score.keyOfLowestValue(openSet);
			if (current.equals(goal)) {
				moves.addFirst(new Move(current));
				while (cameFrom.containsKey(current)) {
					current = cameFrom.get(current);
					moves.addFirst(new Move(current));
				}
				for (Move move : moves) {
					addBehaviour(move);
				}
				return;
			}

			openSet.remove(current);
			closedSet.add(current);
			ArrayList<Coord> neighbor = current.getNeighbours(Wall.map);

			for (int i = 0; i < neighbor.size(); i++) {

				if (closedSet.contains(neighbor.get(i))) {
					continue;
				}

				int tentative_g_score = g_score.get(current) + 1;
				if (!openSet.contains(neighbor.get(i)))
					openSet.add(neighbor.get(i));
				else if (tentative_g_score >= g_score.get(neighbor.get(i)))
					continue;

				cameFrom.put(neighbor.get(i), current);
				g_score.put(neighbor.get(i), tentative_g_score);
				f_score.put(neighbor.get(i), tentative_g_score + Coord.heuristic(neighbor.get(i), goal));
			}
		}
		return;
	}

}

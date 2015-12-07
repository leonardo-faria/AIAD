package agents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import actions.Charge;
import javafx.util.Pair;
import main.Main;
import product.Product;
import sajas.core.Agent;
import sajas.core.behaviours.SimpleBehaviour;
import uchicago.src.sim.gui.DisplayConstants;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;
import utils.DefaultHashMap;

public abstract class Worker extends Agent implements Drawable, Holder {

	int speed;
	boolean fly;
	int charge;
	int load;
	int maxCharge;
	int maxload;

	Coord pos;
	Object2DGrid space;

	ArrayList<Product> stored;
	ArrayList<Product> owned;

	public class Move extends SimpleBehaviour {
		private static final long serialVersionUID = 1L;
		LinkedList<Coord> cl;
		int x;

		public Move(LinkedList<Coord> cl) {
			x = 0;
			this.cl = cl;
		}

		@Override
		public void action() {
			if (x++ == speed) {
				((Worker) myAgent).charge--;
				Coord c = cl.getFirst();
				space.putObjectAt(Worker.this.pos.getX(), Worker.this.pos.getY(), null);
				Worker.this.pos.setX(c.getX());
				Worker.this.pos.setY(c.getY());
				space.putObjectAt(Worker.this.pos.getX(), Worker.this.pos.getY(), Worker.this);
				x = 0;
				cl.removeFirst();
			}
		}

		@Override
		public boolean done() {
			return cl.isEmpty();
		}

	}

	public Worker(Coord c, Object2DGrid space) {
		pos = c;
		this.space = space;
		stored = new ArrayList<>();
		owned = new ArrayList<>();
	}

	@Override
	public void draw(SimGraphics g) {
		g.setDrawingCoordinates(pos.getX() * g.getCurWidth(), pos.getY() * g.getCurHeight(), 0);
		g.drawFastRect(Color.green);
	}

	public Coord getCoord() {
		return pos;
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

	public Pair<LinkedList<Coord>, Coord> makeRoute(Coord start, Coord goal) {
		return makeRoute(start, goal, charge);
	}

	public Pair<LinkedList<Coord>, Coord> makeRoute(Coord start, Coord goal, int charge) {

		ArrayList<Coord> openSet = new ArrayList<Coord>();
		openSet.add(start);
		ArrayList<Coord> closedSet = new ArrayList<Coord>();
		HashMap<Coord, Coord> cameFrom = new HashMap<Coord, Coord>();

		DefaultHashMap<Coord, Integer> g_score = new DefaultHashMap<Coord, Integer>(Integer.MAX_VALUE);
		g_score.put(start, 0);
		DefaultHashMap<Coord, Integer> f_score = new DefaultHashMap<Coord, Integer>(Integer.MAX_VALUE);
		f_score.put(start, g_score.get(start) + Coord.heuristic(start, goal));
		while (!openSet.isEmpty()) {

			Coord current = f_score.keyOfLowestValue(openSet);
			if (current.equals(goal)) {

				LinkedList<Coord> moves = new LinkedList<Coord>();
				moves.addFirst(current);
				while (cameFrom.containsKey(current)) {
					current = cameFrom.get(current);
					moves.addFirst(current);
				}
				if (possibleRoute(moves, charge)) {
					return new Pair<LinkedList<Coord>, Coord>(moves, null);
				} else {
					moves = closestChargerPath(start);
					addBehaviour(new Charge(moves.getLast()));
					moves.addAll(makeRoute(moves.getLast(), goal, maxCharge).getKey());
					return new Pair<LinkedList<Coord>, Coord>(moves, moves.getLast());
				}
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
		return null;
	}

	public LinkedList<Coord> closestChargerPath(Coord start) {
		ArrayList<Coord> openSet = new ArrayList<Coord>();
		openSet.add(start);
		ArrayList<Coord> closedSet = new ArrayList<Coord>();
		HashMap<Coord, Coord> cameFrom = new HashMap<Coord, Coord>();

		DefaultHashMap<Coord, Integer> g_score = new DefaultHashMap<Coord, Integer>(Integer.MAX_VALUE);
		g_score.put(start, 0);
		DefaultHashMap<Coord, Integer> f_score = new DefaultHashMap<Coord, Integer>(Integer.MAX_VALUE);
		f_score.put(start, g_score.get(start) + 10);
		while (!openSet.isEmpty()) {
			Coord current = f_score.keyOfLowestValue(openSet);
			if (Wall.map.get(current.getX()).get(current.getY()).equals(2)) {

				LinkedList<Coord> moves = new LinkedList<Coord>();
				moves.addFirst(current);
				while (cameFrom.containsKey(current)) {
					current = cameFrom.get(current);
					moves.addFirst(current);
				}
				return moves;
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
				f_score.put(neighbor.get(i), tentative_g_score);
			}
		}
		return null;
	}

	public boolean possibleRoute(LinkedList<Coord> r, int charge) {
		return closestChargerPath(r.getLast()).size() < charge - r.size();
	}

	public void scheduleMoves(Pair<LinkedList<Coord>, Coord> m) {
		addBehaviour(new Move(m.getKey()));
		if (m.getValue() != null)
			addBehaviour(new Charge(m.getValue()));
	}

	public void fullCharge() {
		charge = maxCharge;
	}

	public void pickup(Product p) {
		stored.add(p);
	}
	
	public void drop(Product p){
		stored.remove(p);
	}
}

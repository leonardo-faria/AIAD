package agents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sajas.core.Agent;
import uchicago.src.sim.gui.DisplayConstants;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import utils.Coord;
import utils.DefaultHashMap;

public abstract class Worker extends Agent implements Drawable {
	int x, y;

	public Worker(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public void draw(SimGraphics g) {
		g.setDrawingCoordinates(x * g.getCurWidth(), y * g.getCurHeight(), 0);
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

	public static void main(String[] args) {
		new Wall("temp.txt");
		System.out.println(getRoute(new Coord(1, 4), new Coord(4, 10)));
	}

	public static ArrayList<Coord> getRoute(Coord start, Coord goal) {

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
				return make_path(cameFrom, goal);// TODO return
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

	public static ArrayList<Coord> make_path(HashMap<Coord, Coord> cameFrom, Coord current) {
		ArrayList<Coord> total_path = new ArrayList<Coord>();
		total_path.add(current);
		while (cameFrom.containsKey(current)) {
			current = cameFrom.get(current);
			total_path.add(0, current);
			Wall.map.get(current.getX()).set(current.getY(), 2);
		}
		return total_path;
	}
}

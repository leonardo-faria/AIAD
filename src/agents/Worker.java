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
		getRoute(new int[] { 1, 2 }, new int[] { 2, 2 });
	}

	public static class DefaultHashMap<K, V> extends HashMap<K, V> {
		private static final long serialVersionUID = 1L;
		protected V defaultValue;

		public DefaultHashMap(V defaultValue) {
			this.defaultValue = defaultValue;
		}

		@Override
		public V get(Object k) {
			return containsKey(k) ? super.get(k) : defaultValue;
		}

		public K keyOfLowestValue() {
			K key = null;
			Integer min = Integer.MAX_VALUE;
			for (Map.Entry<K, V> e : this.entrySet()) {
				if ((int) e.getValue() < min) {
					key = e.getKey();
					min = (int) e.getValue();
				}
			}
			return key;
		}
	}

	public static ArrayList<int[]> getRoute(int start[], int goal[]) {

		ArrayList<int[]> openSet = new ArrayList<int[]>();
		openSet.add(start);
		ArrayList<int[]> closedSet = new ArrayList<int[]>();
		HashMap<int[], int[]> cameFrom = new HashMap<int[], int[]>();

		DefaultHashMap<int[], Integer> g_score = new DefaultHashMap<int[], Integer>(Integer.MAX_VALUE);
		g_score.put(start, 0);
		DefaultHashMap<int[], Integer> f_score = new DefaultHashMap<int[], Integer>(Integer.MAX_VALUE);
		f_score.put(start, g_score.get(start) + heuristic(start, goal));

		f_score.keyOfLowestValue();
		System.out.println(f_score.keyOfLowestValue());
		return openSet;
	}

	public static int heuristic(int[] start, int[] goal) {
		return Math.abs((goal[0] - start[0])) + Math.abs((goal[1] - start[1]));
	}
}

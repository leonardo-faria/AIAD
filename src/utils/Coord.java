package utils;

import java.util.ArrayList;

public class Coord implements Comparable<Coord> {
	int x, y;
	static Boolean DIAGONAL = false;

	public Coord(int x, int y) {
		this.x = x;
		this.y = y;
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

	public ArrayList<Coord> getNeighbours(ArrayList<ArrayList<Integer>> map) {
		ArrayList<Coord> neighbours = new ArrayList<Coord>();
		if (this.x - 1 >= 0)
			if (!map.get(this.x - 1).get(this.y).equals(1))
				neighbours.add(new Coord(x - 1, y));
		if (this.x + 1 < map.size())
			if (!map.get(this.x + 1).get(this.y).equals(1))
				neighbours.add(new Coord(x + 1, y));
		if (this.y - 1 >= 0)
			if (!map.get(this.x).get(this.y - 1).equals(1))
				neighbours.add(new Coord(x, y - 1));
		if (this.y + 1 < map.get(0).size())
			if (!map.get(this.x).get(this.y + 1).equals(1))
				neighbours.add(new Coord(x, y + 1));
		if (DIAGONAL) {
			if (this.x - 1 >= 0 && this.y - 1 >= 0)
				if (!map.get(this.x - 1).get(this.y - 1).equals(1))
					neighbours.add(new Coord(x - 1, y - 1));
			if (this.x + 1 >= 0 && this.y - 1 >= 0)
				if (!map.get(this.x + 1).get(this.y - 1).equals(1))
					neighbours.add(new Coord(x + 1, y - 1));
			if (this.x - 1 >= 0 && this.y + 1 >= 0)
				if (!map.get(this.x - 1).get(this.y + 1).equals(1))
					neighbours.add(new Coord(x - 1, y + 1));
			if (this.x + 1 >= 0 && this.y + 1 >= 0)
				if (!map.get(this.x + 1).get(this.y + 1).equals(1))
					neighbours.add(new Coord(x + 1, y + 1));
		}
		return neighbours;
	}

	public static int heuristic(Coord start, Coord goal) {
		return Math.abs((goal.x - start.x)) + Math.abs((goal.y - start.y));
	}

	@Override
	public String toString() {
		return "{" + x + ";" + y + "}";
	}

	@Override
	public boolean equals(Object obj) {
		Coord c = (Coord) obj;
		return this.x == c.x && this.y == c.y;
	}

	@Override
	public int compareTo(Coord c) {
		if (this.x == c.x && this.y == c.y)
			return 0;
		else
			return 1;
	}

	@Override
	public int hashCode() {
		return x * 1009 + y;
	}

}

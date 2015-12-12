package agents;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import locals.BatteryChargeCenter;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import utils.Coord;

public class Wall implements Drawable {
	public static ArrayList<ArrayList<Integer>> map;
	public static ArrayList<BatteryChargeCenter> batCenter;
	static <T> ArrayList<ArrayList<T>> transpose(ArrayList<ArrayList<T>> table) {
		ArrayList<ArrayList<T>> ret = new ArrayList<ArrayList<T>>();
		final int N = table.get(0).size();
		for (int i = 0; i < N; i++) {
			ArrayList<T> col = new ArrayList<T>();
			for (ArrayList<T> row : table) {
				col.add(row.get(i));
			}
			ret.add(col);
		}
		return ret;
	}

	public Wall(String fileName) {

		String line = null;
		batCenter=new ArrayList<>();
		ArrayList<ArrayList<Integer>> temp = new ArrayList<ArrayList<Integer>>();
		
		try {
			FileReader fileReader = new FileReader(fileName);

			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				ArrayList<Integer> al = new ArrayList<Integer>();
				for (int i = 0; i < line.length(); i++) {
					al.add((int) line.charAt(i) - (int) '0');
					if( line.charAt(i) == '2')
						batCenter.add(new BatteryChargeCenter(new Coord(temp.size(), i)));
				}
				temp.add(al);
			}
			bufferedReader.close();

			map = transpose(temp);
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + fileName + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + fileName + "'");
		}

	}

	@Override
	public void draw(SimGraphics g) {
		for (int i = 0; i < map.size(); i++) {
			for (int j = 0; j < map.get(i).size(); j++) {
				if (map.get(i).get(j) == 1) {
					g.setDrawingCoordinates(i * g.getCurWidth(), j * g.getCurHeight(), 0);
					g.drawFastRect(Color.red);
				}
			}
		}
		for (int i = 0; i < batCenter.size(); i++) {
			g.setDrawingCoordinates(batCenter.get(i).getPos().getY() * g.getCurWidth(), batCenter.get(i).getPos().getX() * g.getCurHeight(), 0);
			g.drawFastRect(Color.yellow);
		}
	}

	public int getHeight() {
		return map.size();
	}

	public int getWidth() {
		return map.get(0).size();
	}

	public int getX() {
		return 0;
	}

	public int getY() {
		return 0;
	}

}

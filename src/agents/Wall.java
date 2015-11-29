package agents;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

public class Wall implements Drawable {
	ArrayList<ArrayList<Integer>> map;

	
	public Wall(String fileName) {

		String line = null;
		map = new ArrayList<ArrayList<Integer>>();
		
		try {
			FileReader fileReader = new FileReader(fileName);

			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				ArrayList<Integer> al = new ArrayList<Integer>();
				for (int i = 0; i < line.length(); i++) {
					al.add((int) line.charAt(i) - (int) '0');
				}
				map.add(al);
			}
			bufferedReader.close();
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
				if (map.get(i).get(j) == 1)
					g.setDrawingCoordinates(j * g.getCurWidth(), i * g.getCurHeight(), 0);
				g.drawFastRect(Color.red);
			}
		}
	}

	public int getHeight(){
		return map.size();
	}
	public int getWidth(){
		return map.get(0).size();
	}
	
	public int getX() {
		return 0;
	}

	public int getY() {
		return 0;
	}

}

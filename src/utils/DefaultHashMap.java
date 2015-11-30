package utils;

import java.util.ArrayList;
import java.util.HashMap;

public class DefaultHashMap<Coord, Integer> extends HashMap<Coord, Integer> {
	private static int BIG_NUMBER = 9999;
	private static final long serialVersionUID = 1L;
	protected Integer defaultValue;

	public DefaultHashMap(Integer defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public Integer get(Object k) {
		return containsKey(k) ? super.get(k) : defaultValue;
	}
	
	public Coord keyOfLowestValue(ArrayList<Coord> coordSet) {
		Coord key = null;
		int min = BIG_NUMBER;
		for (Coord c : coordSet) {
			if ( (int) this.get(c) < (int) min ) {
				key = c;
				min = (int) this.get(c);
			}
		}
		return key;
	}

}

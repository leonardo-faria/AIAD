package locals;

import utils.Coord;

public class BatteryChargeCenter {
	Coord pos;

	public Coord getPos() {
		return pos;
	}

	public void setPos(Coord pos) {
		this.pos = pos;
	}

	public BatteryChargeCenter(Coord pos) {
		this.pos = pos;
	}
}

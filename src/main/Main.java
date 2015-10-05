package main;

import model.Model;
import uchicago.src.sim.engine.SimInit;

public class Main {

	private static final boolean BATCH_MODE = true;

	public static void main(String[] args) {
		boolean runMode = !BATCH_MODE; // BATCH_MODE or !BATCH_MODE
		// create a simulation
		SimInit init = new SimInit();
		// create a model
		Model model = new Model();
		// load model into simulation:
		init.loadModel(model, null, runMode);
	}

}

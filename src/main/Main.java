package main;

import java.util.ArrayList;

import agents.CarAgent;
import agents.Wall;
import agents.Worker;
import jade.core.Profile;
import jade.core.ProfileImpl;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.space.Object2DTorus;
import utils.Coord;

public class Main extends Repast3Launcher {

	private static final boolean BATCH_MODE = false;
	private ContainerController mainContainer;
	private ContainerController agentContainer;
	public static final boolean SEPARATE_CONTAINERS = false;
	public static final int NUM_AGENTS = 2;

	private int worldXSize = 100;
	private int worldYSize = 100;

	CarAgent car1;
	CarAgent car2;
	Wall wall;

	DisplaySurface dsurf;
	Object2DGrid space;
	ArrayList<Object> drawList;
	public static ArrayList<Worker> workerList;


	public static void main(String[] args) {
		boolean runMode = BATCH_MODE; // BATCH_MODE or !BATCH_MODE
		SimInit init = new SimInit();
		init.setNumRuns(10); // works only in batch mode
		init.loadModel(new Main(), null, BATCH_MODE);

	}

	private void launchAgents() {
		try {

			wall = new Wall("temp.txt");
			setWorldYSize(wall.getHeight());
			setWorldXSize(wall.getWidth());
			workerList = new ArrayList<>();
			space = new Object2DGrid(worldXSize, worldYSize);
			car1 = new CarAgent(new Coord(1, 1), space);
			car2 = new CarAgent(new Coord(2, 5), space);
			workerList.add(car1);
			workerList.add(car2);
			car1.setArguments(new String[] { "receiver" });
			car2.setArguments(new String[] { "sender" });
			agentContainer.acceptNewAgent("Agente1", car1).start();
			agentContainer.acceptNewAgent("Agente2", car2).start();

			scheduleAgent(car1);
			scheduleAgent(car2);
		} catch (Exception e) {

		}
	}

	@Override
	public String getName() {
		return "Transportes";
	}

	@Override
	protected void launchJADE() {
		Runtime rt = Runtime.instance();
		Profile p1 = new ProfileImpl();
		mainContainer = rt.createMainContainer(p1);

		if (SEPARATE_CONTAINERS) {
			Profile p2 = new ProfileImpl();
			agentContainer = rt.createAgentContainer(p2);
		} else {
			agentContainer = mainContainer;
		}

		launchAgents();
	}

	@Override
	public void begin() { 
		super.begin();
		if (!BATCH_MODE) {
			dsurf = new DisplaySurface(this, "T&T");
			registerDisplaySurface("T&T", dsurf);
			buildDisplay();
			buildModel();
		}
	}

	private void buildDisplay() {
		drawList = new ArrayList<Object>();
		drawList.add(wall);

		Object2DDisplay agentDisplay = new Object2DDisplay(space);
		agentDisplay.setObjectList(drawList);

		dsurf.addDisplayableProbeable(agentDisplay, "Agents");
		addSimEventListener(dsurf);
		dsurf.display();
		getSchedule().scheduleActionBeginning(1, this, "step");

	}

	public void step() {
		dsurf.updateDisplay();
	}

	private void buildModel() {
		space.putObjectAt(car1.getX(), car1.getY(), car1);
		drawList.add(car1);
		space.putObjectAt(car2.getX(), car2.getY(), car2);
		drawList.add(car2);

	}

	public String[] getInitParam() {
		String[] params = { "worldXSize", "worldYSize" };
		return params;
	}

	public int getWorldXSize() {
		return worldXSize;
	}

	public void setWorldXSize(int size) {
		worldXSize = size;
	}

	public int getWorldYSize() {
		return worldYSize;
	}

	public void setWorldYSize(int size) {
		worldYSize = size;
	}
}

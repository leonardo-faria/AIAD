package main;

import jade.core.Profile;
import jade.core.ProfileImpl;
import locals.Warehouse;
import product.Product;

import java.util.ArrayList;

import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;
import agents.CarAgent;
import agents.Wall;
import agents.Worker;

public class Main extends Repast3Launcher {

	private static final boolean BATCH_MODE = false;
	private ContainerController mainContainer;
	private ContainerController agentContainer;
	public static final boolean SEPARATE_CONTAINERS = false;
	public static final int NUM_AGENTS = 3;

	private int worldXSize = 100;
	private int worldYSize = 100;

	CarAgent car1, car2, car3;
	Wall wall;

	DisplaySurface dsurf;
	Object2DGrid space;
	ArrayList<Object> drawList;
	ArrayList<Worker> workerList;
	Warehouse warehouse1;
	Warehouse warehouse2;

	public static void main(String[] args) {
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
			car3 = new CarAgent(new Coord(4, 5), space);
			workerList.add(car1);
			workerList.add(car2);
			workerList.add(car3);
			car1.setArguments(new String[] { "receiver" });
			car2.setArguments(new String[] { "sender" });
			car3.setArguments(new String[] { "receiver" });
			agentContainer.acceptNewAgent("Agente1", car1).start();
			agentContainer.acceptNewAgent("Agente3", car3).start();
			agentContainer.acceptNewAgent("Agente2", car2).start();

			scheduleAgent(car1);
			scheduleAgent(car2);
			scheduleAgent(car3);

			warehouse1 = new Warehouse(new Coord(50, 20));
			warehouse2 = new Warehouse(new Coord(40, 40));
			Product.addType("p", 0);
			Product p =new Product("p", car1);
			p.setLocation(warehouse2);
			warehouse1.pickup(p);
			System.out.println("plano:"+car1.planTransport(p, warehouse1));
			car1.addBehaviour(car1.planTransport(p, warehouse1));
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
		for (int i = 0; i < workerList.size(); i++) {
			space.putObjectAt(workerList.get(i).getX(), workerList.get(i).getY(), workerList.get(i));
			drawList.add(workerList.get(i));
		}
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

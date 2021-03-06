package main;

import jade.core.Profile;
import jade.core.ProfileImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import locals.Local;
import locals.Store;
import locals.Wall;
import locals.Warehouse;
import product.Product;
import product.Product.ProSpecs;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;
import agents.BikeAgent;
import agents.CarAgent;
import agents.DroneAgent;
import agents.SystemAgent;
import agents.TruckAgent;
import agents.Worker;

public class Main extends Repast3Launcher {

	private static final boolean BATCH_MODE = false;
	private ContainerController mainContainer;
	private ContainerController agentContainer;
	public static final boolean SEPARATE_CONTAINERS = false;
	public static final int NUM_AGENTS = 3;

	private int worldXSize = 100;
	private int worldYSize = 100;

	Wall wall;
	SystemAgent sys;

	DisplaySurface dsurf;
	Object2DGrid space;
	ArrayList<Object> drawList;
	public static ArrayList<Worker> workerList;
	public static ArrayList<Local> locals;
	public static ArrayList<Store> stores;
	public static ArrayList<String> tools;
	ArrayList<OpenSequenceGraph> graphs;
	
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
			CarAgent car1 = new CarAgent(new Coord(1, 1), space);
			CarAgent car2 = new CarAgent(new Coord(2, 5), space);
			DroneAgent drone1 = new DroneAgent(new Coord(4, 5), space);
			DroneAgent drone2 = new DroneAgent(new Coord(40, 50), space);
			DroneAgent drone3 = new DroneAgent(new Coord(80, 5), space);
			TruckAgent truck1 = new TruckAgent(new Coord(3, 5), space);
			TruckAgent truck2 = new TruckAgent(new Coord(5, 76), space);
			BikeAgent bike = new BikeAgent(new Coord(50, 76), space);
			
			sys = new SystemAgent(new Coord(0, 0), space); 
			Set<String> toolsT = new HashSet<String>();
			tools = new ArrayList<String>();
			toolsT.addAll(car1.getTools());
			toolsT.addAll(drone1.getTools());
			toolsT.addAll(truck1.getTools());
			toolsT.addAll(bike.getTools());
			tools.addAll(toolsT);
			workerList.add(car1);
			workerList.add(car2);
			workerList.add(drone1);
			workerList.add(drone2);
			workerList.add(drone3);
			workerList.add(truck1);
			workerList.add(truck2);
			workerList.add(bike);
			locals = new ArrayList<>();
			locals.add(new Warehouse(new Coord(50, 20), 1));
			locals.add(new Warehouse(new Coord(40, 40), 2));
			locals.add(new Warehouse(new Coord(60, 4), 3));
			locals.add(new Warehouse(new Coord(84, 55), 4));
			locals.add(new Warehouse(new Coord(60, 44), 5));
			locals.add(new Warehouse(new Coord(30, 4), 6));
			locals.add(new Warehouse(new Coord(39, 70), 6));
			stores=new ArrayList<>();
			stores.add(new Store(new Coord(20, 20)));
			stores.add(new Store(new Coord(4, 2)));
			stores.add(new Store(new Coord(20, 60)));
			stores.add(new Store(new Coord(60, 20)));
			Product.addType("p", new ProSpecs(0, 0, stores.get(0).getPos()));
			Product p = new Product("p", car1);
			p.setLocation(locals.get(1));
			agentContainer.acceptNewAgent("Agente2", car1).start();
			agentContainer.acceptNewAgent("Agente3", drone1).start();
			agentContainer.acceptNewAgent("Agente5", drone2).start();
			agentContainer.acceptNewAgent("Agente7", drone3).start();
			agentContainer.acceptNewAgent("Agente4", truck1).start();
			agentContainer.acceptNewAgent("Agente6", truck2).start();
			agentContainer.acceptNewAgent("Agente1", car2).start();
			agentContainer.acceptNewAgent("Agente8", bike).start();
			agentContainer.acceptNewAgent("System", sys).start();

			for (Worker w : workerList) {
				scheduleAgent(w);
			}
			scheduleAgent(sys);

			locals.get(0).pickup(p);
			// car1.addBehaviour(car1.planAssemble(tools, locals.get(0)));
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
			buildGraphics();
			
		}
	}

	private void buildGraphics(){
		 
		graphs=new ArrayList<>();
		
		OpenSequenceGraph graph1 = new OpenSequenceGraph("ProbOfSuccess", this);
		graph1.setXRange(0, 50);
		graph1.setYRange(0, 1);
		graph1.setAxisTitles("time", "Avg. ProbOfSuccess");
		class AverageSuc implements Sequence {
		  public double getSValue() {
		    double totalMoney = 0;
		    for (int i = 0; i < workerList.size(); i++) {
		      Worker a = workerList.get(i);
		      totalMoney += a.getProbOfSuccess();
		    }
		    return totalMoney / workerList.size();
		  }
		}
		graph1.addSequence("Avg. ProbOfSuccess", new AverageSuc());
		graph1.display();
		
		OpenSequenceGraph graph2 = new OpenSequenceGraph("Money", this);
		graph2.setXRange(0, 50);
		graph2.setYRange(4500, 5000);
		graph2.setAxisTitles("time", "Avg. Money");
		class AverageCredits implements Sequence {
		  public double getSValue() {
		    double totalMoney = 0;
		    for (int i = 0; i < workerList.size(); i++) {
		      Worker a = workerList.get(i);
		      totalMoney += a.getMoney();
		    }
		    return totalMoney / workerList.size();
		  }
		}
		graph2.addSequence("Avg. Money", new AverageCredits());
		graph2.display();
		

		graphs.add(graph1);
		graphs.add(graph2);
	}
	
	private void buildDisplay() {
		drawList = new ArrayList<Object>();
		drawList.add(wall);
		drawList.addAll(locals);
		drawList.addAll(stores);
		Object2DDisplay agentDisplay = new Object2DDisplay(space);
		agentDisplay.setObjectList(drawList);

		dsurf.addDisplayableProbeable(agentDisplay, "Agents");
		addSimEventListener(dsurf);
		dsurf.display();
		getSchedule().scheduleActionBeginning(1, this, "step");

	}

	public void step() {
		dsurf.updateDisplay();
		for (OpenSequenceGraph graph : graphs) {
			graph.step();
		}
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

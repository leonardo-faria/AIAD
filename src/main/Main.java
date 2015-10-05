package main;

import agents.CarAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.engine.SimInit;

public class Main extends Repast3Launcher {

	private static final boolean BATCH_MODE = true;
	private ContainerController mainContainer;
	private ContainerController agentContainer;
	public static final boolean SEPARATE_CONTAINERS = false;
	public static final int NUM_AGENTS = 2;
	

	public static void main(String[] args) {
		boolean runMode = !BATCH_MODE; // BATCH_MODE or !BATCH_MODE
		SimInit init = new SimInit();
		init.setNumRuns(10);   // works only in batch mode
		init.loadModel(new Main(), null, BATCH_MODE);
	}
	
	private void launchAgents() {
		try {
			CarAgent car1 = new CarAgent();
			CarAgent car2 = new CarAgent();
			car1.setArguments(new String[]{"ping"});
			car2.setArguments(new String[]{"pong"});
			agentContainer.acceptNewAgent("Novo agente1", car1);
			agentContainer.acceptNewAgent("Novo agente2", car2);
		}
		catch(Exception e) {
			
		}
	}
	
	@Override
	public String[] getInitParam() {
		return new String[0];
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
		
		if(SEPARATE_CONTAINERS) {
			Profile p2 = new ProfileImpl();
			agentContainer = rt.createAgentContainer(p2);
		} else {
			agentContainer = mainContainer;
		}
		
		launchAgents();
	}

}

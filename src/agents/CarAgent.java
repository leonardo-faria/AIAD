package agents;

import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import credit.Credit;
import sajas.core.Agent;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.domain.DFService;
import tools.Tool;
import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;
import main.Main;

public class CarAgent extends Worker {

	public CarAgent(Coord c, Object2DGrid space) {
		super(c, space);
		charge= 10;
		maxCharge= 1000;
		speed=1;
		scheduleMoves(makeRoute(c, new Coord(80, 80)));
	}
}

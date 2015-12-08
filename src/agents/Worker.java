package agents;

import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import main.Main;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.core.behaviours.TickerBehaviour;
import sajas.domain.DFService;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;
import utils.DefaultHashMap;

public abstract class Worker extends Agent implements Drawable {

	int speed;
	boolean fly, isInAuction;
	int charge;
	int load;
	int maxCharge;
	int maxload;
	// HashMap<jade.core.AID, Integer> proposeValues = new HashMap<>();

	Coord pos;
	Object2DGrid space;

	public class RespondToTask extends Behaviour {

		private static final long serialVersionUID = 1L;
		boolean done;
		
		public RespondToTask() {
			done = false;
		}
		@Override
		public void action() {
			ACLMessage msg = receive();
			if(msg != null) {
				switch (msg.getPerformative()) {
				case ACLMessage.CFP:
					System.out.println("Sou o " + myAgent.getName() + " e recebi uma msg com " + msg.getContent());
					ACLMessage reply = msg.createReply();
					if(this.getAgent().getName().equals("Agente3@Transportes")){
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent("100");
					}
					else {
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent("200");
					}
					send(reply);
					done = true;
					break;
				case ACLMessage.ACCEPT_PROPOSAL:
					System.out.println("Bue fixe sou o escolhido, aka " + this.getAgent().getName());
					done = true;
					//fazer task
					break;
				}
			}
			
		}

		@Override
		public boolean done() {
			return done;
		}
		
	}
	
	public class RequestTask extends Behaviour {
		private static final long serialVersionUID = 1L;
		private int numOfResponses = 0;
		private int bestPrice;
		private jade.core.AID winnerWorker;
		private int step;
		private String tipo;

		public RequestTask() {
			Object[] args = getArguments();
			this.tipo = (String) args[0];
			step = 0;
		}

		@Override
		public void action() {
			
//			ACLMessage msg = receive();
//			if(msg != null) {
//				switch (msg.getPerformative()) {
//				case ACLMessage.CFP:
//					System.out.println("Sou o " + this.getAgent().getName() + " e recebi uma msg com " + msg.getContent());
//					ACLMessage reply = msg.createReply();
//					if(this.getAgent().getName().equals("Agente3@Transportes")){
//						reply.setPerformative(ACLMessage.PROPOSE);
//						reply.setContent("100");
//					}
//					else
//						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
//					send(reply);
//					done = true;
//					break;
//				case ACLMessage.PROPOSE:
//					int val = Integer.parseInt(msg.getContent());
//					proposeValues.put(msg.getSender(), val);
//					numOfResponses++;
//					System.out.println("Agent " + this.getAgent().getName() + " has received a proposal of value "+ val);
//					if(numOfResponses != Main.workerList.size() - 1){
//						block();
//					}
//					else{
//						int min = Collections.min(proposeValues.values());
//						ACLMessage acc = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
//						ACLMessage rej = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
//						for (Entry<jade.core.AID, Integer> entry : proposeValues.entrySet()) {
//							if(entry.getValue() == min && !acc.getAllReceiver().hasNext()){
//								acc.addReceiver(entry.getKey());
//							}
//							else{
//								rej.addReceiver(entry.getKey());
//							}
//						}
//						if(acc.getAllReceiver() != null)
//							send(acc);
//						if(rej.getAllReceiver() != null)
//							send(rej);
//
//						done = true;
//					}
//					break;
//
//				case ACLMessage.ACCEPT_PROPOSAL:
//					System.out.println("Bue fixe sou o escolhido, aka " + this.getAgent().getName());
//					done = true;
//					//fazer task
//					break;
//				case ACLMessage.REJECT_PROPOSAL:
//					if(isInAuction){
//						System.out.println("Agent " + msg.getSender().getLocalName() + " has rejected me :("); 
//						numOfResponses++;
//						done = true;
//					}
//					else {
//						System.out.println("kek cheiro mal, aka " + this.getAgent().getName());
//						done = true;
//					}
//					break;
//				default:
//					done = true;
//					break;
//				}
//			}
//			else
//				done = true;
//			
			
			switch (step) {
			 case 0:
			 // Send the cfp to all workers
				// toma a iniciativa se for agente "sender"
					if (tipo.equals("sender")) {
						System.out.println("is sender");
						isInAuction = true;
						//numOfResponses = 0;
						ACLMessage msg = new ACLMessage(ACLMessage.CFP);
						for (int i = 0; i < Main.workerList.size(); i++){
							if(Main.workerList.get(i).getAID() != myAgent.getAID())
								msg.addReceiver(Main.workerList.get(i).getAID());
						}
						msg.setContent("Mano, queres trabalhar?");
						send(msg);
						step = 1;
					}
			 break;
			 case 1:
				 
				 ACLMessage reply = myAgent.receive();
				 if (reply != null) {
				 // Reply received
					 if (reply.getPerformative() == ACLMessage.PROPOSE) {
						 // This is an offer
						 int price = Integer.parseInt(reply.getContent());
						 System.out.println("O agente " + myAgent.getName() + "ganhou com o preço " + price);
						 if (winnerWorker == null || price < bestPrice) {
							 // This is the best offer at present
							 bestPrice = price;
							 winnerWorker = reply.getSender();
						 }
					 }
					 numOfResponses++;
					 if (numOfResponses >= Main.workerList.size()) {
						 //We received all replies
						 step = 2;
					 }
				 }
				 else {
					 block();
				 }
				 break;
			 case 2:
				 // Send the confirmation to the worker that won the bid
				 ACLMessage confirmation = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				 confirmation.addReceiver(winnerWorker);
				 confirmation.setContent("Ganhaste mano");
				 myAgent.send(confirmation);
				 step = 3;
				 break;
			 case 3:
				 // Receive the confirmation when the task is done
				 reply = myAgent.receive();
				 if (reply != null) {
					 // Confirmation received
					 if (reply.getPerformative() == ACLMessage.INFORM) {
						 // Task done
						 System.out.println("Task done!");
						 myAgent.doDelete();
					 }
					 step = 4;
				 	}
				 	else {
				 		block();
				 	}
				 break;
			} 
		}
		
		@Override
		public boolean done() {
			return ((step == 2 && winnerWorker == null) || step == 4);
		}

	}

	public class Move extends SimpleBehaviour {
		private static final long serialVersionUID = 1L;
		LinkedList<Coord> cl;
		int x;

		public Move(LinkedList<Coord> cl) {
			x = 0;
			this.cl = cl;
		}

		@Override
		public void action() {
			if (x++ == speed) {
				Coord c = cl.getFirst();
				space.putObjectAt(Worker.this.pos.getX(),
						Worker.this.pos.getY(), null);
				Worker.this.pos.setX(c.getX());
				Worker.this.pos.setY(c.getY());
				space.putObjectAt(Worker.this.pos.getX(),
						Worker.this.pos.getY(), Worker.this);
				x = 0;
				cl.removeFirst();
			}
		}

		@Override
		public boolean done() {
			return cl.isEmpty();
		}

	}

	public Worker(Coord c, Object2DGrid space) {
		pos = c;
		isInAuction = false;
		this.space = space;
	}

	protected void setup() {
		String tipo = "";
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			tipo = (String) args[0];
		} else {
			System.out.println("Não especificou o tipo");
		}

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName(getName());
		sd.setType("Agente " + tipo);
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		// cria behaviour

		TickerBehaviour tb = new TickerBehaviour(this, 100) {
			private static final long serialVersionUID = 1L;

			protected void onTick() {
				addBehaviour(new RequestTask());
				addBehaviour(new RespondToTask());
			}

			public void onStart() {
			}
		};
		addBehaviour(tb);

	}

	@Override
	public void draw(SimGraphics g) {
		g.setDrawingCoordinates(pos.getX() * g.getCurWidth(),
				pos.getY() * g.getCurHeight(), 0);
		g.drawFastRect(Color.green);
	}

	public int getX() {
		return pos.getX();
	}

	public void setX(int x) {
		this.pos.setX(x);
	}

	public int getY() {
		return pos.getY();
	}

	public void setY(int y) {
		this.pos.setY(y);
	}

	public LinkedList<Coord> makeRoute(Coord start, Coord goal) {
		return makeRoute(start, goal, charge);
	}

	public LinkedList<Coord> makeRoute(Coord start, Coord goal, int charge) {

		ArrayList<Coord> openSet = new ArrayList<Coord>();
		openSet.add(start);
		ArrayList<Coord> closedSet = new ArrayList<Coord>();
		HashMap<Coord, Coord> cameFrom = new HashMap<Coord, Coord>();

		DefaultHashMap<Coord, Integer> g_score = new DefaultHashMap<Coord, Integer>(
				Integer.MAX_VALUE);
		g_score.put(start, 0);
		DefaultHashMap<Coord, Integer> f_score = new DefaultHashMap<Coord, Integer>(
				Integer.MAX_VALUE);
		f_score.put(start, g_score.get(start) + Coord.heuristic(start, goal));
		while (!openSet.isEmpty()) {

			Coord current = f_score.keyOfLowestValue(openSet);
			if (current.equals(goal)) {

				LinkedList<Coord> moves = new LinkedList<Coord>();
				moves.addFirst(current);
				while (cameFrom.containsKey(current)) {
					current = cameFrom.get(current);
					moves.addFirst(current);
				}
				if (possibleRoute(moves, charge)) {
					return moves;
				} else {
					moves = closestChargerPath(start);
					moves.addAll(makeRoute(moves.getLast(), goal, maxCharge));
					return moves;
				}
			}

			openSet.remove(current);
			closedSet.add(current);
			ArrayList<Coord> neighbor = current.getNeighbours(Wall.map);

			for (int i = 0; i < neighbor.size(); i++) {

				if (closedSet.contains(neighbor.get(i))) {
					continue;
				}

				int tentative_g_score = g_score.get(current) + 1;

				if (!openSet.contains(neighbor.get(i)))
					openSet.add(neighbor.get(i));
				else if (tentative_g_score >= g_score.get(neighbor.get(i)))
					continue;

				cameFrom.put(neighbor.get(i), current);
				g_score.put(neighbor.get(i), tentative_g_score);
				f_score.put(
						neighbor.get(i),
						tentative_g_score
								+ Coord.heuristic(neighbor.get(i), goal));
			}
		}
		return null;
	}

	public LinkedList<Coord> closestChargerPath(Coord start) {
		ArrayList<Coord> openSet = new ArrayList<Coord>();
		openSet.add(start);
		ArrayList<Coord> closedSet = new ArrayList<Coord>();
		HashMap<Coord, Coord> cameFrom = new HashMap<Coord, Coord>();

		DefaultHashMap<Coord, Integer> g_score = new DefaultHashMap<Coord, Integer>(
				Integer.MAX_VALUE);
		g_score.put(start, 0);
		DefaultHashMap<Coord, Integer> f_score = new DefaultHashMap<Coord, Integer>(
				Integer.MAX_VALUE);
		f_score.put(start, g_score.get(start) + 10);
		while (!openSet.isEmpty()) {
			Coord current = f_score.keyOfLowestValue(openSet);
			if (Wall.map.get(current.getX()).get(current.getY()).equals(2)) {

				LinkedList<Coord> moves = new LinkedList<Coord>();
				moves.addFirst(current);
				while (cameFrom.containsKey(current)) {
					current = cameFrom.get(current);
					moves.addFirst(current);
				}
				return moves;
			}

			openSet.remove(current);
			closedSet.add(current);
			ArrayList<Coord> neighbor = current.getNeighbours(Wall.map);

			for (int i = 0; i < neighbor.size(); i++) {

				if (closedSet.contains(neighbor.get(i))) {
					continue;
				}

				int tentative_g_score = g_score.get(current) + 1;

				if (!openSet.contains(neighbor.get(i)))
					openSet.add(neighbor.get(i));
				else if (tentative_g_score >= g_score.get(neighbor.get(i)))
					continue;

				cameFrom.put(neighbor.get(i), current);
				g_score.put(neighbor.get(i), tentative_g_score);
				f_score.put(neighbor.get(i), tentative_g_score);
			}
		}
		return null;
	}

	public boolean possibleRoute(LinkedList<Coord> r, int charge) {
		return closestChargerPath(r.getLast()).size() < charge - r.size();
	}

	public void scheduleMoves(LinkedList<Coord> m) {
		addBehaviour(new Move(m));
	}
}

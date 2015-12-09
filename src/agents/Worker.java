package agents;

import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import sajas.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.domain.DFService;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;
import utils.DefaultHashMap;

public abstract class Worker extends Agent implements Drawable {

	int speed;
	boolean fly;
	int charge;
	int load;
	int maxCharge;
	int maxload;
	private jade.core.AID[] agents;

	Coord pos;
	Object2DGrid space;

	public class RespondToTask extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate
					.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println("Sou o " + myAgent.getName()
				+ " e recebi uma msg com " + msg.getContent());
				ACLMessage reply = msg.createReply();
				if (myAgent.getName().equals("Agente3@Transportes")) {
					reply.setContent("200");
					reply.setPerformative(ACLMessage.PROPOSE);
					addBehaviour(new TaskConfirmation());
					
				} else {
					reply.setContent("100");
					reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
				}
				send(reply);
				System.out.println("Sou o " + myAgent.getName()
				+ " e enviei uma proposta de " + reply.getContent());
			} else {
				block();
			}

		}

	}

	public class TaskConfirmation extends Behaviour {

		private static final long serialVersionUID = 1L;
		private boolean done = false;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println("Fui aceite sou especial - " + myAgent.getName());
				done = true;
			}
			else {
				block();
			}
		}

		@Override
		public boolean done() {
			return done;
		}

	}

	public class RequestTask extends Behaviour {
		private static final long serialVersionUID = 1L;
		private int numOfResponses;
		private int bestPrice;
		private jade.core.AID winnerWorker;
		private int step;
		private MessageTemplate mt;

		public RequestTask() {
			bestPrice = Integer.MAX_VALUE;
			step = 0;
			numOfResponses = 0;
			updateAgents();
		}

		@Override
		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all workers
				System.out.println("Step 0 - Sending messages to agents");
				ACLMessage msg = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < agents.length; i++) {
					if (agents[i] != myAgent.getAID())
						msg.addReceiver(agents[i]);
				}
				msg.setContent("Mano, queres trabalhar?");
				msg.setConversationId("task-request");
				msg.setReplyWith("msg" + System.currentTimeMillis());
				send(msg);
				mt = MessageTemplate
						.and(MessageTemplate
								.MatchConversationId("task-request"),
								MessageTemplate.MatchInReplyTo(msg
										.getReplyWith()));
				step = 1;
				break;
			case 1:

				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					System.out.println("Step1 - Reply received");
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer
						int price = Integer.parseInt(reply.getContent());
						System.out
						.println("Recebi uma mensagem com a proposta de "
								+ price);
						if (price < bestPrice) {
							// This is the best offer at present
							bestPrice = price;
							winnerWorker = reply.getSender();
						}
					}
					numOfResponses++;
					if (numOfResponses >= agents.length - 1) {
						// We received all replies
						step = 2;
						System.out.println("O agente "
								+ winnerWorker.getName()
								+ " ganhou com o preço " + bestPrice);
					}
				} else {
					block();
				}
				break;
			case 2:
				// Send the confirmation to the worker that won the bid
				System.out.println("Step2 - Sending confirmation\n");
				ACLMessage confirmation = new ACLMessage(
						ACLMessage.ACCEPT_PROPOSAL);
				confirmation.addReceiver(winnerWorker);
				confirmation.setContent("Ganhaste mano");
				confirmation.setConversationId("task-request");
				confirmation.setReplyWith("confirmation"
						+ System.currentTimeMillis());
				send(confirmation);
				System.out.println(myAgent.getName() + " mandei a confirmação");
				mt = MessageTemplate.and(MessageTemplate
						.MatchConversationId("task-request"), MessageTemplate
						.MatchInReplyTo(confirmation.getReplyWith()));
				step = 3;
				break;
			case 3:
				System.out.println("Step 3 - Waiting task complete status");
				// Receive the confirmation when the task is done
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Confirmation received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Task done
						System.out.println("Task done!");
						myAgent.doDelete();
					}
					step = 4;
				} else {
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
			updateAgents();
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		// cria behaviours

		if(getLocalName().equals("Agente2"))
			addBehaviour(new RequestTask());

		addBehaviour(new RespondToTask());
	}

	private void updateAgents() {
		// Update the list of seller agents
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		dfd.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, dfd);
			agents = new AID[result.length];
			for (int i = 0; i < result.length; ++i) {
				agents[i] = result[i].getName();
			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
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

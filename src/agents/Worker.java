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

import javafx.util.Pair;
import locals.Local;
import main.Main;
import product.Product;
import sajas.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.domain.DFService;
import tools.Tool;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;
import utils.Coord;
import utils.DefaultHashMap;

public abstract class Worker extends Agent implements Drawable, Holder {

	public static final String ASSEMBLY_TASK = "1";
	public static final String AQUISITION_TASK = "2";
	public static final String TRANSPORT_TASK = "3";
	public static final String PROTOCOL_FIXED_TASK = "Fixed Task";
	public static final String PROTOCOL_TASK = "Normal Task";

	int speed;
	boolean fly;
	int charge;
	int load;
	int maxCharge;
	int maxload;
	private jade.core.AID[] agents;
	Behaviour requestedJob;

	ArrayList<Product> stored;
	ArrayList<Product> owned;

	Coord pos;
	Object2DGrid space;

	public Job parseJob(ACLMessage msg){
		Job proposed = null;
		String[] tasksID = msg.getConversationId().split("-");
		String[] specs = msg.getContent().split(" ");
		switch (tasksID[1]) {
		case ASSEMBLY_TASK:
			//Formato_conteudo: Tipo_Produto Coord_X Coord_Y
			proposed = planAssemble(specs[0], new Coord(Integer.parseInt(specs[1]), Integer.parseInt(specs[2])));
			break;
		case AQUISITION_TASK:

			break;
		case TRANSPORT_TASK:
			Product p = null;
			Local l = null;
			//Formato_conteudo: Nome_Produto Nome_Local1 Nome_Local2 Tempo
			for(int i=0;i<Main.locals.size();i++){
				if(Main.locals.get(i).getName().equals(specs[1])){
					p = new Product(specs[0], Main.locals.get(i)); //TODO mudar para lista d produtos

				}
				if(Main.locals.get(i).getName().equals(specs[2])){
					l = Main.locals.get(i);
				}
			}

			proposed = planTransport(p, l, msg.getSender());
			proposed.maxtime = Integer.parseInt(specs[3]);
			break;

		default:
			break;
		}

		return proposed;

	}

	public class Job extends SimpleBehaviour {

		private static final long serialVersionUID = 1L;

		ArrayList<Behaviour> tasks;
		ArrayList<Tool> tools;
		int maxtime=0;
		int time;
		int step;
		boolean started;
		private jade.core.AID requester;
		boolean done;

		public Job(ArrayList<Behaviour> tasks, ArrayList<Tool> tools, int time,jade.core.AID requester) {
			this.tasks = tasks;
			this.tools = tools;
			this.time = time;
			this.requester = requester;
			started = false;
			done = false;
			step = 0;
		}

		public int cost(){
			return time;
		}

		@Override
		public void action() {
			if (tasks.get(tasks.size() - 1).done()) {
				done = true;
				return;
			}
			if (!started) {
				Worker.this.addBehaviour(tasks.get(step++));
				started = true;
			} else if (tasks.get(step - 1).done()) {
				Worker.this.addBehaviour(tasks.get(step++));
			}
		}

		@Override
		public boolean done() {
			return done;
		}
		@Override
		public int onEnd(){
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setContent("done");
			msg.addReceiver(requester);
			send(msg);
			System.out.println("Fiz a tarefa e enviei a confirmação");
			return maxtime;
			
		}

	}


	public class Pickup extends SimpleBehaviour {

		private static final long serialVersionUID = 1L;
		Holder location;
		Product p;
		boolean done;

		/**
		 * 
		 * @param p
		 *            product
		 * @param c
		 *            place from where to pickup
		 */
		public Pickup(Product p, Holder c) {
			this.p = p;
			this.location = c;
			done = false;
		}

		@Override
		public void action() {
			p.getLocation().drop(p);
			pickup(p);
			p.setLocation(Worker.this);
			done = true;
		}

		@Override
		public boolean done() {
			return done;
		}
	}

	public class Drop extends SimpleBehaviour {

		private static final long serialVersionUID = 1L;
		Holder location;
		Product p;
		boolean done;

		/**
		 * 
		 * @param p
		 *            product to drop
		 * @param c
		 *            location to where to drop
		 */
		public Drop(Product p, Holder c) {
			this.p = p;
			this.location = c;
			done = false;
		}

		@Override
		public void action() {
			p.getLocation().drop(p);
			location.pickup(p);
			p.setLocation(location);
			done = true;
		}

		@Override
		public boolean done() {
			return done;
		}

	}

	public class Charge extends SimpleBehaviour {

		private static final long serialVersionUID = 1L;

		Coord location;
		boolean done;

		public Charge(Coord c) {
			location = c;
			done = false;
		}

		@Override
		public void action() {
			if (getCoord().equals(location)) {
				System.out.println("charged");
				fullCharge();
				done = true;
			}
		}

		@Override
		public boolean done() {
			return done;
		}

	}

	public Job planAssemble(String productType, Coord location) {
		return null;
	}

	public Job planTransport(Product p, Holder location, jade.core.AID requester) {
		return planTransport(p, location, charge,requester);
	}

	public Job planTransport(Product p, Holder location, int charge, jade.core.AID requester) {
		ArrayList<Behaviour> tasks = new ArrayList<Behaviour>();
		ArrayList<Tool> tools = new ArrayList<Tool>();
		if (p.getWeight() > this.maxload)
			return null;
		if (!stored.contains(p)) {
			if (!this.getCoord().equals(p.getLocation().getCoord())) {
				Pair<Pair<LinkedList<Coord>,Coord>, Integer> route = makeRoute(getCoord(), p.getLocation().getCoord(), charge);
				tasks.add(createMoves(route.getKey()));
				charge = route.getValue();
			}
			tasks.add(new Pickup(p, p.getLocation()));
		}
		tasks.add(createMoves(makeRoute(p.getLocation().getCoord(), location.getCoord(), charge).getKey()));
		System.out.println(charge);
		tasks.add(new Drop(p, location));
		return new Job(tasks, tools, 0, requester);
	}

	public class RespondToTask extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				String content = msg.getContent();
				System.out.println("Sou o " + myAgent.getName()
				+ " e recebi uma msg com " + content);

				ACLMessage reply = msg.createReply();

				//analisar conteudo, ver se vale a pena fazer a tarefa
				parseJob(msg);
				if (myAgent.getName().equals("Agente3@Transportes")) {
					reply.setContent("50");
					reply.setPerformative(ACLMessage.PROPOSE);
					System.out
					.println("Sou o " + myAgent.getName() + " e enviei uma proposta de " + reply.getContent());
					addBehaviour(new TaskConfirmation());

				} else {
					reply.setContent("100");
					reply.setPerformative(ACLMessage.PROPOSE);
					System.out
					.println("Sou o " + myAgent.getName() + " e enviei uma proposta de " + reply.getContent());
					addBehaviour(new TaskConfirmation());
				}
				send(reply);
			} else {
				block();
			}

		}

	}

	public class RespondToFixedTask extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.not(MessageTemplate.MatchContent("done")));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				//Verificar se vale a pena fazer ou não, se fizer mandar accept, se não mandar reject
				ACLMessage reply = msg.createReply();
				if (myAgent.getName().equals("Agente3@Transportes")) {
					reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					reply.setConversationId(reply.getConversationId());
					send(reply);
					System.out
					.println("Sou o " + myAgent.getName() + " e enviei a confirmação de que ia tentar fazer a tarefa " + msg.getContent());
					//criar job
					Job b = parseJob(msg);

					addBehaviour(b);
				} else {
					reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
					reply.setConversationId(reply.getConversationId());
					send(reply);
					System.out
					.println("Sou o " + myAgent.getName() + " e enviei um reject à tarefa de preço fixo " + msg.getContent());
				}
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
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
					MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
					System.out.println("Fui aceite sou especial - " + myAgent.getName());
				} else {
					System.out.println("Lol caguei nem a queria - " + myAgent.getName());
				}
				done = true;
			} else {
				block();
			}
		}

		@Override
		public boolean done() {
			return done;
		}

	}

	public class RequestTaskFixedPrice extends Behaviour {

		private static final long serialVersionUID = 1L;
		private int numOfResponses = 0;
		private int numAccepted = 0;
		private int step;
		private int price;
		String request;
		private boolean done = false;
		private MessageTemplate mt;

		public RequestTaskFixedPrice(int price) {
			this.price = price;
			updateAgents();
		}

		@Override
		public void action() {
			switch (step) {
			case 0:
				// Send the inform to all workers
				System.out.println("Step 0 - Sending messages to agents fixed price");
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				for (int i = 0; i < agents.length; i++) {
					if (agents[i] != myAgent.getAID())
						msg.addReceiver(agents[i]);
				}
				msg.setContent("Mesa Warehouse1 Warehouse2 " + price);
				request = "task-3";
				msg.setConversationId(request);
				msg.setReplyWith("msg-fixed" + System.currentTimeMillis());
				send(msg);
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId(request),
						MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
				step = 1;
				break;
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					System.out.println("Step1 - Reply received");
					if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
						System.out.println("Recebi uma confirmação a dizer que o agente " + reply.getSender() + "vai tentar faze-la");
						numAccepted++;

					}
					if (reply.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
						System.out.println("Recebi uma rejeição a dizer que o agente " + reply.getSender() + "não vai fazer a tarefa");
					}
					numOfResponses++;
					if (numOfResponses >= agents.length - 1) {
						if(numAccepted == 0){
							step = 3;
							System.out.println("Everyone rejected the task");						}
						else {
							// We received all replies
							System.out.println("Got an accept, waiting for task done inform...");
							step = 2;
							mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
									MessageTemplate.MatchContent("done"));
						}
					}
				} else {
					block();
				}
				break;
			case 2:
				System.out.println("Step 2");
				reply = myAgent.receive(mt);
				if (reply != null) {
					//pagar ao cliente
					//reply.getSender();
					System.out.println("Paguei ao agente " + reply.getSender() + " " + price);
					done = true;
				}
				else {
					block();
				}
				break;
			case 3:
				//Cancelar, ninguém quis fazer a tarefa
				System.out.println("Ninguém quis fazer a tarefa");
				done = true;
				break;
			default:
				break;
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
		private ArrayList<jade.core.AID> rejectedAgents;
		private int step;
		private long timeout, wakeupTime;
		String request;
		private MessageTemplate mt;

		public RequestTask() {
			bestPrice = Integer.MAX_VALUE;
			rejectedAgents = new ArrayList<jade.core.AID>();
			step = 0;
			numOfResponses = 0;
			updateAgents();
			timeout = 500;
		}

		//TODO handle global rejection of auction
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
				msg.setContent("Mesa Warehouse2 Warehouse1 8000");
				request = "task-3";
				msg.setConversationId(request);
				msg.setReplyWith("msg" + System.currentTimeMillis());
				send(msg);
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId(request),
						MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
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
						System.out.println("Recebi uma mensagem com a proposta de " + price);
						if (price < bestPrice) {
							// This is the best offer at present
							if (winnerWorker != null)
								rejectedAgents.add(winnerWorker);
							bestPrice = price;
							winnerWorker = reply.getSender();
						} else
							rejectedAgents.add(reply.getSender());
					}
					numOfResponses++;
					if (numOfResponses >= agents.length - 1) {
						if(winnerWorker == null){
							step = 5;
							System.out.println("Everyone rejected the auction");
							wakeupTime = System.currentTimeMillis() + timeout;
						}
						else {
							// We received all replies
							step = 2;
							System.out.println("O agente "
									+ winnerWorker.getName()
									+ " ganhou com o preço " + bestPrice);
						}
					}
				} else {
					block();
				}
				break;
			case 2:
				// Send the confirmation to the worker that won the bid and
				// rejections to all the rest
				System.out.println("Step2 - Sending confirmation\n");
				ACLMessage confirmation = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				confirmation.addReceiver(winnerWorker);
				confirmation.setContent("Ganhaste mano");
				confirmation.setConversationId(request);
				confirmation.setReplyWith("confirmation" + System.currentTimeMillis());
				send(confirmation);

				ACLMessage rejection = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
				for (int i = 0; i < rejectedAgents.size(); i++)
					rejection.addReceiver(rejectedAgents.get(i));
				rejection.setConversationId(request);
				rejection.setReplyWith("confirmation" + System.currentTimeMillis());
				send(rejection);

				System.out.println(myAgent.getName() + " mandei a confirmação");
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId(request),
						MessageTemplate.MatchInReplyTo(confirmation.getReplyWith()));
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
					}
					step = 4;
				} else {
					block();
				}
				break;
			case 5:
				step = 4;
				//desisto, para ja cancela o leilao
				//				long dt = wakeupTime - System.currentTimeMillis();
				//				 if (dt <= 0) {
				//			         System.out.println("oi");
				//			      } else 
				//			         block(dt);
				//				bestPrice = Integer.MAX_VALUE;
				//				rejectedAgents = new ArrayList<jade.core.AID>();
				//				step = 0;
				//				numOfResponses = 0;
				//				updateAgents();


				break;
			default:
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
		Charge charge;
		boolean setToCharge;

		public Move(LinkedList<Coord> cl, Charge charge) {
			x = 0;
			this.cl = cl;
			if (charge != null) {
				this.charge = charge;
				setToCharge = true;
			} else {
				this.charge = null;
				setToCharge = false;
			}
		}

		@Override
		public void action() {
			if (setToCharge) {
				addBehaviour(charge);
				setToCharge = false;
			}
			if (x++ == speed) {
				Worker.this.charge--;
				Coord c = cl.getFirst();
				space.putObjectAt(Worker.this.pos.getX(), Worker.this.pos.getY(), null);
				Worker.this.pos.setX(c.getX());
				Worker.this.pos.setY(c.getY());
				space.putObjectAt(Worker.this.pos.getX(), Worker.this.pos.getY(), Worker.this);
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
		stored = new ArrayList<>();
		owned = new ArrayList<>();
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

		if(getLocalName().equals("Agente2")){
			addBehaviour(new RequestTaskFixedPrice(800));
		}

		addBehaviour(new RespondToFixedTask());
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
		g.setDrawingCoordinates(pos.getX() * g.getCurWidth(), pos.getY() * g.getCurHeight(), 0);
		g.drawFastRect(Color.green);
	}

	public Coord getCoord() {
		return pos;
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

	public Pair<Pair<LinkedList<Coord>, Coord>, Integer> makeRoute(Coord start, Coord goal) {
		return makeRoute(start, goal, charge);
	}

	public Pair<Pair<LinkedList<Coord>, Coord>, Integer> makeRoute(Coord start, Coord goal, int charge) {

		ArrayList<Coord> openSet = new ArrayList<Coord>();
		openSet.add(start);
		ArrayList<Coord> closedSet = new ArrayList<Coord>();
		HashMap<Coord, Coord> cameFrom = new HashMap<Coord, Coord>();

		DefaultHashMap<Coord, Integer> g_score = new DefaultHashMap<Coord, Integer>(Integer.MAX_VALUE);
		g_score.put(start, 0);
		DefaultHashMap<Coord, Integer> f_score = new DefaultHashMap<Coord, Integer>(Integer.MAX_VALUE);
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
					return new Pair<Pair<LinkedList<Coord>, Coord>, Integer>(new Pair<LinkedList<Coord>, Coord>(moves, null), charge - moves.size());
				} else {
					moves = closestChargerPath(start);
					Coord chargePos = moves.getLast();
					LinkedList<Coord> r = makeRoute(moves.getLast(), goal, maxCharge).getKey().getKey();
					moves.addAll(r);
					return new Pair<Pair<LinkedList<Coord>,Coord>,Integer>( new Pair<LinkedList<Coord>, Coord>(moves, chargePos),maxCharge-r.size());
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
				f_score.put(neighbor.get(i), tentative_g_score + Coord.heuristic(neighbor.get(i), goal));
			}
		}
		return null;
	}

	public LinkedList<Coord> closestChargerPath(Coord start) {
		ArrayList<Coord> openSet = new ArrayList<Coord>();
		openSet.add(start);
		ArrayList<Coord> closedSet = new ArrayList<Coord>();
		HashMap<Coord, Coord> cameFrom = new HashMap<Coord, Coord>();

		DefaultHashMap<Coord, Integer> g_score = new DefaultHashMap<Coord, Integer>(Integer.MAX_VALUE);
		g_score.put(start, 0);
		DefaultHashMap<Coord, Integer> f_score = new DefaultHashMap<Coord, Integer>(Integer.MAX_VALUE);
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

	public Move createMoves(Pair<LinkedList<Coord>, Coord> m) {
		if (m.getValue() != null)
			return new Move(m.getKey(), new Charge(m.getValue()));
		else
			return new Move(m.getKey(), null);
	}

	public void fullCharge() {
		charge = maxCharge;
	}

	public void pickup(Product p) {
		stored.add(p);
	}

	public void drop(Product p) {
		stored.remove(p);
	}
}

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
import java.util.List;
import java.util.Random;

import javafx.util.Pair;
import locals.Local;
import locals.Wall;
import main.Main;
import product.Product;
import product.Product.ProSpecs;
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

public abstract class Worker extends Agent implements Drawable, Holder {

	public static final String ASSEMBLY_TASK = "1";
	public static final String AQUISITION_TASK = "2";
	public static final String TRANSPORT_TASK = "3";
	public static final String PROTOCOL_FIXED_TASK = "Fixed Task";
	public static final String PROTOCOL_TASK = "Normal Task";
	public static final double LEARNING_RATE = 0.6;

	int speed;
	private double probOfSuccess;
	boolean fly = false;
	boolean ongoingJob = false, handlingMsg = false;
	int charge;
	int load;
	int maxCharge;
	int maxload;
	int credits;
	int prevDistance;
	private jade.core.AID[] agents;
	Job proposedJob;
	Behaviour requestedJob;
	boolean blockMessages = false;
	boolean jobFailed = false;

	ArrayList<Product> stored;
	ArrayList<Product> owned;
	ArrayList<String> tools;

	enum jobTypes {
		ASSEMBLY_TASK, AQUISITION_TASK, TRANSPORT_TASK
	};

	HashMap<jobTypes, ArrayList<Integer>> pastJobs = new HashMap<jobTypes, ArrayList<Integer>>();

	Coord pos;
	Object2DGrid space;

	public ArrayList<String> getTools() {
		return tools;
	}

	public void setTools(ArrayList<String> tools) {
		this.tools = tools;
	}

	public Job parseJob(ACLMessage msg, jade.core.AID aid) {
		Job proposed = null;
		Local l = null;
		String[] tasksID = msg.getConversationId().split("-");
		String[] specs = msg.getContent().split(" ");
		Worker provider = null;
		for (int i = 0; i < Main.workerList.size(); i++)
			if (Main.workerList.get(i).getAID() == aid) {
				provider = Main.workerList.get(i);
				break;
			}

		switch (tasksID[1]) {
		case ASSEMBLY_TASK:
			// Formato_conteudo: f1-f2 Nome_Local Tempo Multa
			String[] t = specs[0].split("-");
			ArrayList<String> tools = new ArrayList<>();
			for (int i = 0; i < t.length; i++)
				tools.add(t[i]);

			for (int i = 0; i < Main.locals.size(); i++) {
				if (Main.locals.get(i).getName().equals(specs[1])) {
					l = Main.locals.get(i);
					break;
				}
			}
			if (tasksID[0].equals("auction")) {
				proposed = planAssemble(tools, l, Integer.parseInt(specs[3]));
			} else
				proposed = planAssemble(tools, l, 0);
			if (proposed != null) {
				proposed.maxtime = Integer.parseInt(specs[2]);
				proposed.payoff = proposed.maxtime;
			}

			break;
		case AQUISITION_TASK:
			// Formato_conteudo: Tipo_Produto Nome_Local1 Tempo
			for (int i = 0; i < Main.locals.size(); i++) {
				if (Main.locals.get(i).getName().equals(specs[1])) {
					l = Main.locals.get(i);
					break;
				}
			}
			proposed = planAquisition(specs[0], l);
			if (proposed != null) {
				proposed.maxtime = Integer.parseInt(specs[2]);
				proposed.payoff = proposed.maxtime;
			}
			break;
		case TRANSPORT_TASK:
			Product p = null;
			// Formato_conteudo: Nome_Produto Nome_Local1 Nome_Local2 Tempo
			for (int i = 0; i < Main.locals.size(); i++) {
				if (Main.locals.get(i).getName().equals(specs[1])) {
					p = new Product(specs[0], Main.locals.get(i));
				}
				if (Main.locals.get(i).getName().equals(specs[2])) {
					l = Main.locals.get(i);
				}
			}
			proposed = planTransport(p, l);
			if (proposed != null) {
				proposed.maxtime = Integer.parseInt(specs[3]);
				proposed.payoff = proposed.maxtime;
			}
			break;

		default:
			break;
		}

		if (proposed != null) {
			proposed.doneMsg = msg.createReply();
			proposed.doneMsg.setConversationId(msg.getConversationId());
			proposed.provider = provider;
			proposed.receiver = msg.getSender();
		}
		return proposed;

	}

	public int getCost(ArrayList<String> reqTools, int distance) {
		int estimatedTime = 0;
		if (fly)
			distance *= 2;
		for (int i = 0; i < reqTools.size(); i++) {
			if (!tools.contains(reqTools.get(i))) {
				estimatedTime += (distance / (probOfSuccess * (searchTool(reqTools.get(i)) / searchTool(null))));
			}
		}
		return distance * speed + estimatedTime;
	}

	public class Buy extends SimpleBehaviour {

		private static final long serialVersionUID = 1L;
		int price;
		boolean done;

		public Buy(int p) {
			price = p;
			done = false;
		}

		@Override
		public void action() {
			credits -= price;
			done = true;
		}

		@Override
		public boolean done() {
			return done;
		}

	}

	public class Job extends SimpleBehaviour {

		private static final long serialVersionUID = 1L;

		ArrayList<Behaviour> tasks;
		ArrayList<String> tools;
		int payoff;
		int maxtime;
		int proposedTime;
		int estimatedTime;
		int step;
		int distance;
		int creditsSpent = 0;
		boolean started;
		ACLMessage doneMsg;
		Worker provider;
		jade.core.AID receiver;
		int fine;
		boolean done;

		public Job(ArrayList<Behaviour> tasks, ArrayList<String> tools, int time, int pay) {
			this.tasks = tasks;
			this.tools = tools;
			proposedTime = time;
			started = false;
			done = false;
			step = 0;
			fine = 0;
			payoff = pay;
		}

		@Override
		public void onStart() {
			ongoingJob = true;
		}

		public int getCost() {
			estimatedTime = 0;
			for (int i = 0; i < tools.size(); i++) {
				if (!provider.tools.contains(tools.get(i))) {
					estimatedTime += (distance / (provider.probOfSuccess
							* (provider.searchTool(tools.get(i)) / provider.searchTool(null))));
				}
			}
			provider.prevDistance = distance;
			return distance * provider.speed + creditsSpent + estimatedTime + (int) ((1 - probOfSuccess) * fine);
		}

		public void cancel() {
			done = true;
			jobFailed = true;
		}

		@Override
		public void action() {
			if (tasks.get(tasks.size() - 1).done()) {
				done = true;
				return;
			}
			if (done) {
				// cancel current job
				if (tasks.get(step - 1) instanceof Move)
					((Move) tasks.get(step - 1)).stop();
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
		public int onEnd() {
			if (!jobFailed) {
				credits += payoff;
				System.out.println(getLocalName() + " recebeu " + payoff);
				doneMsg.setPerformative(ACLMessage.INFORM);
				doneMsg.setContent("done");
				System.out.println(getLocalName() +" has done the task and sent the confirmation");
			} else {
				jobFailed = false;
				doneMsg.setPerformative(ACLMessage.FAILURE);
				doneMsg.setContent("done");
				System.out.println(getLocalName() + " failed the task and sent the confirmation");
			}
			ongoingJob = false;
			handlingMsg = false;
			send(doneMsg);
			return 1;

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
				System.out.println("I'm " + myAgent.getLocalName() + " and I just charged by battery");
				fullCharge();
				done = true;
			}
		}

		@Override
		public boolean done() {
			return done;
		}

	}

	public Job planTransport(Product p, Holder location) {
		return planTransport(p, location, charge);
	}

	public Job planTransport(Product p, Holder location, int charge) {
		ArrayList<Behaviour> tasks = new ArrayList<Behaviour>();
		ArrayList<String> tools = new ArrayList<String>();
		if (p.getWeight() > this.maxload)
			return null;
		int distance = 0;
		if (!stored.contains(p)) {
			if (!this.getCoord().equals(p.getLocation().getCoord())) {
				Pair<Pair<LinkedList<Coord>, Coord>, Integer> route = makeRoute(getCoord(), p.getLocation().getCoord(),
						charge);
				tasks.add(createMoves(route.getKey()));
				charge = route.getValue();
				distance += route.getKey().getKey().size();
			}
			tasks.add(new Pickup(p, p.getLocation()));
		}
		Pair<Pair<LinkedList<Coord>, Coord>, Integer> r = makeRoute(p.getLocation().getCoord(), location.getCoord(),
				charge);
		tasks.add(createMoves(r.getKey()));

		distance += r.getKey().getKey().size();
		tasks.add(new Drop(p, location));
		Job j = new Job(tasks, tools, distance, 0);
		j.distance = distance;
		return j;
	}

	public Job planAquisition(String ptype, Holder location) {
		return planAquisition(ptype, location, charge);
	}

	public Job planAquisition(String ptype, Holder location, int charge) {
		ProSpecs ps = Product.getProductTypes().get(ptype);
		ArrayList<Behaviour> tasks = new ArrayList<Behaviour>();
		ArrayList<String> tools = new ArrayList<String>();
		if (ps.weight > this.maxload)
			return null;
		if (ps.price > this.credits)
			return null;
		int distance = 0;
		if (!this.getCoord().equals(ps.seller)) {
			Pair<Pair<LinkedList<Coord>, Coord>, Integer> route = makeRoute(getCoord(), ps.seller, charge);
			tasks.add(createMoves(route.getKey()));
			charge = route.getValue();
			distance += route.getKey().getKey().size();
		}
		Product p = new Product(ptype, this);
		tasks.add(new Buy(p.getCost()));
		tasks.add(new Pickup(p, this));

		Pair<Pair<LinkedList<Coord>, Coord>, Integer> r = makeRoute(ps.seller, location.getCoord(), charge);
		distance += r.getKey().getKey().size();
		tasks.add(createMoves(r.getKey()));
		tasks.add(new Drop(p, location));
		Job j = new Job(tasks, tools, distance, 0);
		j.distance = distance;
		return j;
	}

	public Job planAssemble(ArrayList<String> neededTools, Holder location, int fine) {
		ArrayList<Behaviour> tasks = new ArrayList<>();
		ArrayList<String> missingTools = new ArrayList<String>();

		if (!tools.containsAll(neededTools)) {
			for (String tool : neededTools)
				if (!tools.contains(tool)) {
					missingTools.add(tool);
				}
		}
		if (missingTools.containsAll(neededTools))
			return null;
		FullAssemble fa = new FullAssemble(missingTools, location, fine);
		tasks.add(fa);
		Job j = new Job(tasks, neededTools, fa.distance, 0);
		j.distance = fa.distance;
		j.fine = fine;
		return j;
	}

	public class FullAssemble extends Behaviour {

		private static final long serialVersionUID = 1L;
		jade.core.AID requester;
		Move myAssemble;
		boolean started, done;
		int distance;
		int payoff;
		Holder location;
		Behaviour requestAssemble;
		int fine;

		public FullAssemble(ArrayList<String> missingtools, Holder location, int fine) {
			Pair<Pair<LinkedList<Coord>, Coord>, Integer> r = makeRoute(getCoord(), location.getCoord());
			myAssemble = createMoves(r.getKey());
			// planear pedir a outro
			distance = r.getKey().getKey().size();
			this.location = location;
			started = false;
			done = false;
			requestAssemble = null;
			this.fine = fine;
			String t = "";
			for (int i = 0; i < missingtools.size(); i++) {
				t += missingtools.get(i);
				if (i != missingtools.size() - 1)
					t += "-";
			}
			if (!t.equals("")) {
				if (pastJobs.get(jobTypes.ASSEMBLY_TASK).size() >= 3) {
					ArrayList<Integer> cpy = pastJobs.get(jobTypes.ASSEMBLY_TASK);
					double averageValue = 0;
					for (int i = 0; i < cpy.size(); i++) {
						averageValue += cpy.get(i);
					}
					averageValue /= cpy.size();
					requestAssemble = new RequestTaskFixedPrice(Worker.ASSEMBLY_TASK, t + " " + location.getName(),
							(int) averageValue, false);
				} else{
					requestAssemble = new RequestTask(Worker.ASSEMBLY_TASK, t + " " + location.getName(), 0, 0, false);
				}

			}
		}

		@Override
		public void action() {
			if (!started) {
				addBehaviour(myAssemble);
				if (requestAssemble != null) {
					addBehaviour(requestAssemble);
				}
				started = true;
			}
			if (requestAssemble != null) {
				if (requestAssemble instanceof RequestTask) {
					if (((RequestTask) requestAssemble).failed) {
						myAssemble.stop();
						done = true;
						jobFailed = true;
						credits -= fine;
						System.out.println(getLocalName() + " pagou de multa " + fine);
					} else if (myAssemble.done() && requestAssemble.done()) {
						done = true;
					}
				}
				else {
					if (((RequestTaskFixedPrice) requestAssemble).failed) {
						myAssemble.stop();
						done = true;
						jobFailed = true;
					} else if (myAssemble.done() && requestAssemble.done()) {
						done = true;
					}
				}
			} else if (myAssemble.done()) {
				done = true;
			}
		}

		@Override
		public boolean done() {
			return done;
		}

	}

	public class RespondToTask extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				String content = msg.getContent();
				System.out.println("I'm " + myAgent.getLocalName() + " and I received a message with " + content + " from " + msg.getSender().getLocalName());

				// Verificar se vale a pena fazer ou não, se fizer mandar
				// accept, se não mandar reject
				ACLMessage reply = msg.createReply();
				// criar job
				if(!ongoingJob && !handlingMsg){
					proposedJob = parseJob(msg, myAgent.getAID());
					if (proposedJob != null) {
						handlingMsg = true;
						int cost = proposedJob.getCost();
						System.out.println("Custo do " + myAgent.getLocalName() + ": " + cost);
						if (cost <= proposedJob.maxtime) {
							proposedJob.payoff = cost;
							reply.setPerformative(ACLMessage.PROPOSE);
							reply.setConversationId(reply.getConversationId());
							reply.setContent("" + cost);
							System.out.println("I'm " + myAgent.getLocalName() + " and I sent a propose with the value "
									+ reply.getContent() + " to " + msg.getSender().getLocalName());
							addBehaviour(new TaskConfirmation());
						} else {
							reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
							reply.setConversationId(reply.getConversationId());
							System.out.println("I'm " + myAgent.getLocalName() + " and I rejected the task - " + content);
							handlingMsg = false;
						}
					}
					else{
						String why;
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						reply.setConversationId(reply.getConversationId());
						reply.setContent("notbusy");
						why = "I couldnt do it";
						System.out.println("I'm " + myAgent.getLocalName() + " and I sent a reject to the auction task - "
								+ msg.getContent() + " because " + why);
					}
				}else {
					reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
					reply.setConversationId(reply.getConversationId());
					String why;
					reply.setContent("busy");
					why = "I was busy";
					System.out.println("I'm " + myAgent.getLocalName() + " and I sent a reject to the auction task - "
							+ msg.getContent() + " because " + why);
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
			MessageTemplate temp = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
					MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
			MessageTemplate mt = MessageTemplate.and(temp, MessageTemplate.not(MessageTemplate.MatchContent("done")));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ver se e pa cancelar ongoingjob
				if (msg.getPerformative() == ACLMessage.CANCEL && ongoingJob) {
					proposedJob.cancel();
				} else if (msg.getPerformative() == ACLMessage.INFORM) {
					// Verificar se vale a pena fazer ou não, se fizer mandar
					// accept, se não mandar reject
					ACLMessage reply = msg.createReply();
					// criar job
					proposedJob = parseJob(msg, myAgent.getAID());
					if(!ongoingJob && !handlingMsg){
						if (proposedJob != null) {
							handlingMsg = true;
							int cost = proposedJob.getCost();
							System.out.println("Custo do " + myAgent.getLocalName() + ": " + cost);
							if (cost <= proposedJob.maxtime) {
								reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
								reply.setConversationId(reply.getConversationId());
								System.out.println("I'm " + myAgent.getLocalName()
								+ " and I sent a confirmation that I'll try to do the fixed price task - "
								+ msg.getContent());
								addBehaviour(proposedJob);
							} else {
								reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
								reply.setConversationId(reply.getConversationId());
								System.out.println("I'm " + myAgent.getLocalName()
								+ " and I sent a reject to the fixed price task - " + msg.getContent());
								handlingMsg = false;
							}
						}
						else{
							String why;
							reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
							reply.setConversationId(reply.getConversationId());
							reply.setContent("notbusy");
							why = "I couldnt do it";
							System.out.println("I'm " + myAgent.getLocalName() + " and I sent a reject to the auction task - "
									+ msg.getContent() + " because " + why);
						}
					}else {
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						reply.setConversationId(reply.getConversationId());
						String why;
						reply.setContent("busy");
						why = "I was busy";
						System.out.println("I'm " + myAgent.getLocalName() + " and I sent a reject to the auction task - "
								+ msg.getContent() + " because " + why);
					}
					send(reply);
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
					System.out.println("My proposal was accepted - " + myAgent.getLocalName());
					addBehaviour(proposedJob);
				} else {
					System.out.println("My proposal was refused - " + myAgent.getLocalName());
				}
				handlingMsg = false;
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
		private int numBusy = 0;
		private int step;
		private int price;
		String request;
		private boolean done = false;
		private MessageTemplate mt;
		private String specs;
		boolean failed, agentCompletedTask;
		private ArrayList<jade.core.AID> agentsTrying;
		private boolean systemTask;

		public RequestTaskFixedPrice(String type, String specs, int price, boolean systemTask) {
			this.price = price;
			request = "fixed-" + type;
			this.specs = specs;
			updateAgents();
			failed = false;
			agentCompletedTask = false;
			agentsTrying = new ArrayList<jade.core.AID>();
			this.systemTask = systemTask;
		}

		@Override
		public void action() {
			switch (step) {
			case 0:
				// Send the inform to all workers
				System.out.println("Step 0 - Sending messages to agents fixed price");
				handlingMsg = true;
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				for (int i = 0; i < agents.length; i++) {
					if (agents[i] != myAgent.getAID())
						msg.addReceiver(agents[i]);
				}

				msg.setContent(specs + " " + price);
				msg.setConversationId(request);

				if (price == 0) {
					proposedJob = parseJob(msg, myAgent.getAID());
					if (proposedJob == null) {
						String[] temp = specs.split(" ");
						String[] t = temp[0].split("-");
						ArrayList<String> temp2 = new ArrayList<>();
						for (int i = 0; i < t.length; i++)
							temp2.add(t[i]);
						price = getCost(temp2, prevDistance);
					} else {
						price = proposedJob.getCost();
					}
				}
				msg.setContent(specs + " " + price);
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
						System.out.println(
								"Received a confirmation from " + reply.getSender() + ", he will try to do it");
						numAccepted++;
						agentsTrying.add(reply.getSender());
					}
					if (reply.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
						System.out.println("Received a reject task from agent " + reply.getSender()
						+ ", he ins't going to do the task");
						if (reply.getContent() == "busy")
							numBusy++;

					}
					numOfResponses++;
					if (numOfResponses >= agents.length - 1) {
						if (numAccepted == 0) {
							step = 3;
							System.out.println("Everyone rejected the task");
						} else {
							// We received all replies
							int numRejectedNotBusy = numOfResponses - (numAccepted + numBusy);

							float rejectPercent = numRejectedNotBusy / (numAccepted + numBusy);
							if (rejectPercent < 0.5) {
								probOfSuccess += (1 - rejectPercent) * LEARNING_RATE * (1 - probOfSuccess);
							} else
								probOfSuccess -= (1 - rejectPercent) * LEARNING_RATE * (1 - probOfSuccess);

							System.out.println("\n" + probOfSuccess + "\n");
							System.out.println("Got an accept, waiting for task done inform...");
							numOfResponses = 0;
							step = 2;
							MessageTemplate temp = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
									MessageTemplate.MatchPerformative(ACLMessage.FAILURE));
							mt = MessageTemplate.and(temp,MessageTemplate.MatchContent("done"));
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
					handlingMsg = false;
					numOfResponses++;
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Task done
						credits -= price;
						System.out.println(getLocalName() + " pagou " + price);
						System.out.println("The task I made is done!");
						agentCompletedTask = true;
						msg = new ACLMessage(ACLMessage.CANCEL);
						msg.setContent("backoff");
						for (int i = 0; i < agentsTrying.size(); i++) {
							if (reply.getSender() != agentsTrying.get(i))
								msg.addReceiver(agentsTrying.get(i));
						}
						send(msg);
						done = true;
					} else {
						// Task failed
						System.out.println("The agent that tried to do the task failed");
						if (numOfResponses == numAccepted && !agentCompletedTask)
							step = 3;
					}

				} else {
					block();
				}
				break;
			case 3:
				// Cancelar, ninguém quis fazer a tarefa
				System.out.println("No one wanted to do the task");
				done = true;
				failed = true;
				break;
			default:
				break;
			}
		}

		@Override
		public boolean done() {
			return done;
		}
		
		@Override
		public int onEnd() {
			if (systemTask) {
				generateRandomTasks();
			}
			return 0;
		}

	}

	public class RequestTask extends Behaviour {
		private static final long serialVersionUID = 1L;
		private int numOfResponses;
		private int bestPrice;
		private int numProposes = 0;
		private int numBusy = 0;
		private jade.core.AID winnerWorker;
		private ArrayList<jade.core.AID> rejectedAgents;
		private int step;
		String request;
		boolean failed;
		private MessageTemplate mt;
		private int proposedTime;
		private int fine;
		private String specs;
		private String taskType;
		private boolean systemTask;

		public RequestTask(String taskType, String specs, int proposedTime, int fine, boolean systemTask) {
			this.taskType = taskType;
			request = "auction-" + taskType;
			bestPrice = Integer.MAX_VALUE;
			rejectedAgents = new ArrayList<jade.core.AID>();
			step = 0;
			numOfResponses = 0;
			failed = false;
			updateAgents();
			this.specs = specs;
			this.proposedTime = proposedTime;
			this.fine = fine;
			this.systemTask = systemTask;
		}

		@Override
		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all workers
				handlingMsg = true;
				System.out.println("Step 0 - " + myAgent.getLocalName() + "Sending messages to agents");
				ACLMessage msg = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < agents.length; i++) {
					if (agents[i] != myAgent.getAID())
						msg.addReceiver(agents[i]);
				}
				msg.setConversationId(request);
				msg.setContent(specs + " " + proposedTime + " " + fine);
				if (proposedTime == 0) {
					proposedJob = parseJob(msg, myAgent.getAID());
					if (proposedJob == null) {
						String[] temp = specs.split(" ");
						String[] t = temp[0].split("-");
						ArrayList<String> temp2 = new ArrayList<>();
						for (int i = 0; i < t.length; i++)
							temp2.add(t[i]);
						proposedTime = getCost(temp2, prevDistance);
					} else {
						proposedTime = proposedJob.getCost();
					}
				}
				double tempFine;
				if (fine == 0) {
					tempFine = (1 / 3.0) * proposedTime;
					fine = (int) tempFine;
				}
				msg.setContent(specs + " " + proposedTime + " " + fine);
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
						System.out.println("I received a propose of " + price);
						if (price < bestPrice) {
							// This is the best offer at present
							if (winnerWorker != null)
								rejectedAgents.add(winnerWorker);
							bestPrice = price;
							winnerWorker = reply.getSender();
						} else
							rejectedAgents.add(reply.getSender());
						numProposes++;
					} else if (reply.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
						if (reply.getContent() == "busy")
							numBusy++;
					}
					numOfResponses++;
					if (numOfResponses >= agents.length - 1) {
						if (winnerWorker == null) {
							step = 5;
							System.out.println("Everyone rejected the auction of" + myAgent.getLocalName());
						} else {
							// We received all replies
							step = 2;
							int numRejectedNotBusy = numOfResponses - (numProposes + numBusy);

							float rejectPercent = numRejectedNotBusy / (numProposes + numBusy);
							if (rejectPercent < 0.5) {
								probOfSuccess += (1 - rejectPercent) * LEARNING_RATE * (1 - probOfSuccess);
							} else
								probOfSuccess -= (1 - rejectPercent) * LEARNING_RATE * (1 - probOfSuccess);
							System.out.println("\n" + probOfSuccess + "\n");
							System.out.println(
									"The agent " + winnerWorker.getLocalName() + " won with the value " + bestPrice);
						}
					}
				} else {
					block();
				}
				break;
			case 2:
				// Send the confirmation to the worker that won the bid and
				// rejections to all the rest

				switch (taskType) {
				case AQUISITION_TASK:
					pastJobs.get(jobTypes.AQUISITION_TASK).add(bestPrice);
					break;
				case ASSEMBLY_TASK:
					pastJobs.get(jobTypes.ASSEMBLY_TASK).add(bestPrice);
					break;
				case TRANSPORT_TASK:
					pastJobs.get(jobTypes.TRANSPORT_TASK).add(bestPrice);
					break;
				}
				System.out.println("Step2 - Sending confirmation");
				ACLMessage confirmation = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				confirmation.addReceiver(winnerWorker);
				confirmation.setContent("You won the bid");
				confirmation.setConversationId(request);
				confirmation.setReplyWith("confirmation" + System.currentTimeMillis());
				send(confirmation);

				ACLMessage rejection = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
				for (int i = 0; i < rejectedAgents.size(); i++)
					rejection.addReceiver(rejectedAgents.get(i));
				rejection.setConversationId(request);
				rejection.setReplyWith("confirmation" + System.currentTimeMillis());
				send(rejection);

				System.out.println(myAgent.getLocalName() + " sent the confirmation");
				MessageTemplate temp = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
						MessageTemplate.MatchPerformative(ACLMessage.FAILURE));
				mt = MessageTemplate.and(temp, MessageTemplate.MatchConversationId(request));
				step = 3;
				break;
			case 3:
				System.out.println("Step 3 - Waiting task complete status");
				// Receive the confirmation when the task is done
				reply = myAgent.receive(mt);
				if (reply != null) {
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Task done
						credits -= bestPrice;
						System.out.println(getLocalName() + " pagou " + bestPrice);
						System.out.println("The task I made is done!");
						step = 4;
					} else {
						// Task failed
						System.out.println("The agent I hired failed the task");
						System.out.println(getLocalName() + " recebeu de compensaçao " + fine);
						credits += fine;
						step = 5;
					}
				} else {
					block();
				}
				break;
			case 5:
				// leilao falhou
				failed = true;
				handlingMsg = false;
				step = 4;

				break;
			default:
				break;
			}
		}

		@Override
		public boolean done() {
			return ((step == 2 && winnerWorker == null) || step == 4);
		}

		@Override
		public int onEnd() {
			if (systemTask) {
				generateRandomTasks();
			}
			return 0;
		}

	}

	public class Move extends SimpleBehaviour {
		private static final long serialVersionUID = 1L;
		LinkedList<Coord> cl;
		int x;
		Charge charge;
		boolean setToCharge;
		boolean stoped;

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
			stoped = false;
		}

		public void stop() {
			stoped = true;

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
			return cl.isEmpty() || stoped;
		}

	}

	public Worker(Coord c, Object2DGrid space) {
		pos = c;
		this.space = space;
		stored = new ArrayList<>();
		owned = new ArrayList<>();
		tools = new ArrayList<>();
		probOfSuccess = 0.5;
		prevDistance = 0;
		credits = 5000;
		pastJobs.put(jobTypes.ASSEMBLY_TASK, new ArrayList<Integer>());
		pastJobs.put(jobTypes.TRANSPORT_TASK, new ArrayList<Integer>());
		pastJobs.put(jobTypes.AQUISITION_TASK, new ArrayList<Integer>());
	}

	private void generateRandomTasks() {
		Random r = new Random();
		Behaviour random = null;
		int taskTypeR = r.nextInt(4 - 1 + 1);
		int jobtType = r.nextInt(2);
		System.out.println("job type: " + jobtType);
		String taskTypeS = Integer.toString(taskTypeR);
		switch (taskTypeS) {
		case ASSEMBLY_TASK:
			// generating tools needed
			String toolsNeeded = "";
			ArrayList<String> toolsTemp = new ArrayList<String>();
			toolsTemp.addAll(Main.tools);
			int nTools = 1 + r.nextInt(toolsTemp.size());
			System.out.println("N tools: " + nTools);
			for (int i = 0; i < nTools; i++) {
				int index = r.nextInt(toolsTemp.size());
				if (i != nTools - 1)
					toolsNeeded += toolsTemp.get(index) + "-";
				else
					toolsNeeded += toolsTemp.get(index);
				toolsTemp.remove(index);
			}
			System.out.println("Tools needed: " + toolsNeeded);

			// generating local
			ArrayList<String> localTemp = new ArrayList<String>();
			for (int i = 0; i < Main.locals.size(); i++)
				localTemp.add(Main.locals.get(i).getName());
			int localSize = localTemp.size();
			int index = r.nextInt(localSize);
			String specs = toolsNeeded + " " + localTemp.get(index);
			if(jobtType == 0)
				random = new RequestTask(taskTypeS, specs, 2000, 0, true);
			else random = new RequestTaskFixedPrice(taskTypeS, specs, 2000, true);
			break;
		case AQUISITION_TASK:
			int nr = r.nextInt(10000);
			int w = r.nextInt(2500);
			int p = r.nextInt(3000);
			int x = r.nextInt();
			int y = r.nextInt();
			// não sei se crio um produto novo ou se vou buscar aos que lá tem
			// como tou a fazer no transport
			Product.getProductTypes().put("Product" + nr, new ProSpecs(w, p, new Coord(50, 50)));
			ArrayList<String> localTempA = new ArrayList<String>();
			for (int i = 0; i < Main.locals.size(); i++)
				localTempA.add(Main.locals.get(i).getName());
			int localSizeA = localTempA.size();
			int indexA = r.nextInt(localSizeA);
			String msg = "Product" + nr + " " + localTempA.get(indexA);
			if(jobtType == 0)
				random = new RequestTask(taskTypeS, msg, 2000, 0, true);
			else random = new RequestTaskFixedPrice(taskTypeS, msg, 2000, true);
			break;
		case TRANSPORT_TASK:
			int indexB = r.nextInt(Product.getProductTypes().size());
			List<String> keys = new ArrayList<String>(Product.getProductTypes().keySet());
			String msgT = keys.get(indexB);
			ArrayList<String> localTempB = new ArrayList<String>();
			for (int i = 0; i < Main.locals.size(); i++)
				localTempB.add(Main.locals.get(i).getName());
			int localSizeB = localTempB.size();
			int indexB1 = r.nextInt(localSizeB);
			msgT += " " + localTempB.get(indexB1);
			localTempB.remove(indexB1);
			int indexB2 = r.nextInt(localSizeB - 1);
			msgT += " " + localTempB.get(indexB2);
			if(jobtType == 0)
				random = new RequestTask(taskTypeS, msgT, 2000, 0, true);
			else
				random = new RequestTaskFixedPrice(taskTypeS, msgT, 2000, true);
			break;
		default:
			break;
		}
		if (random != null)
			addBehaviour(random);
		else
			generateRandomTasks();
	}

	protected void setup() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		for (int i = 0; i < tools.size(); i++) {
			ServiceDescription sd = new ServiceDescription();
			sd.setOwnership(getLocalName());
			sd.setName(tools.get(i));
			sd.setType("tool");
			dfd.addServices(sd);
		}
		try {
			if (!getLocalName().equals("System"))
				DFService.register(this, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		// cria behaviours

		if (getLocalName().equals("Agente1")) {
			/*
			 * if(pastJobs.get(jobTypes.ASSEMBLY_TASK).size() >= 3){
			 * ArrayList<Integer> cpy = pastJobs.get(jobTypes.ASSEMBLY_TASK);
			 * double averageValue = 0; for(int i = 0; i < cpy.size();i++){
			 * averageValue += cpy.get(i); } averageValue /= cpy.size();
			 * addBehaviour(new RequestTaskFixedPrice("1", "1-4-3 Warehouse1",
			 * (int) averageValue)); }
			 */

			// addBehaviour(new RequestTask("1", "1-4-3 Warehouse1", 0, 0));
			// addBehaviour(new RequestTaskFixedPrice("2", "p Warehouse2", 0));
			// addBehaviour(new RequestTaskFixedPrice("3",
			// "1-4-3 Warehouse1 Warehouse2", 0));
			// generateRandomTasks();
			// System.out.println("acabou");

		}
		if (!getLocalName().equals("System")) {
			addBehaviour(new RespondToFixedTask());
			addBehaviour(new RespondToTask());
		} else {
			//generateRandomTasks();
		}

		if(getLocalName().equals("Agente1")){
			addBehaviour(new RequestTaskFixedPrice("1", "1-3-4 Warehouse2", 0,false));
		}
	}

	private float searchTool(String name) {
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("tool");
		if (name != null)
			sd.setName(name);
		dfd.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, dfd);
			return result.length;
		} catch (FIPAException e) {
			e.printStackTrace();
			return 0;
		}
	}

	private void updateAgents() {
		// Update the list of seller agents
		DFAgentDescription dfd = new DFAgentDescription();
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

	public int getMoney() {
		return credits;
	}

	public double getProbOfSuccess() {
		return probOfSuccess;

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
					return new Pair<Pair<LinkedList<Coord>, Coord>, Integer>(
							new Pair<LinkedList<Coord>, Coord>(moves, null), charge - moves.size());
				} else {
					moves = closestChargerPath(start);
					Coord chargePos = moves.getLast();
					LinkedList<Coord> r = makeRoute(moves.getLast(), goal, maxCharge).getKey().getKey();
					moves.addAll(r);
					return new Pair<Pair<LinkedList<Coord>, Coord>, Integer>(
							new Pair<LinkedList<Coord>, Coord>(moves, chargePos), maxCharge - r.size());
				}
			}

			openSet.remove(current);
			closedSet.add(current);
			ArrayList<Coord> neighbor = current.getNeighbours(Wall.map, fly);

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
			ArrayList<Coord> neighbor = current.getNeighbours(Wall.map, fly);

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

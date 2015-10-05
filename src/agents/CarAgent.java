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

public class CarAgent extends Worker {

	private static int VELOCITY = 3;
	private static boolean ROAD = true; // true estrada, false ar
	private static int BATTERY_CAPACITY = 500;
	private static int LOAD_CAPACITY = 550;
	private static Tool f1;
	private static Tool f2;

	private Credit credit;
	private int batteryLeft;
	private int loadLeft;
	private int position[];

	class ola extends SimpleBehaviour {

		private int n = 0;

		// construtor do behaviour
		public ola(Agent a) {
			super(a);
		}

		public void action() {
			ACLMessage msg = blockingReceive();
			if (msg.getPerformative() == ACLMessage.INFORM) {
				System.out.println(++n + " " + getLocalName() + ": recebi " + msg.getContent());
				// cria resposta
				ACLMessage reply = msg.createReply();
				// preenche conteúdo da mensagem
				if (msg.getContent().equals("ping"))
					reply.setContent("pong");
				else
					reply.setContent("ping");
				// envia mensagem
				send(reply);
			}
		}

		// método done
		public boolean done() {
			return n == 10;
		}
	}

	protected void setup() {
		String tipo = "";
		// obtém argumentos
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			tipo = (String) args[0];
		} else {
			System.out.println("Não especificou o tipo");
		}

		// regista agente no DF
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
		ola b = new ola(this);
		addBehaviour(b);

		// toma a iniciativa se for agente "pong"
		if (tipo.equals("pong")) {
			// pesquisa DF por agentes "ping"
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd1 = new ServiceDescription();
			sd1.setType("Agente ping");
			template.addServices(sd1);
			try {
				DFAgentDescription[] result = DFService.search(this, template);
				// envia mensagem "pong" inicial a todos os agentes "ping"
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				for (int i = 0; i < result.length; ++i)
					msg.addReceiver(result[i].getName());
				msg.setContent("pong");
				send(msg);
			} catch (FIPAException e) {
				e.printStackTrace();
			}
		}
	}

}

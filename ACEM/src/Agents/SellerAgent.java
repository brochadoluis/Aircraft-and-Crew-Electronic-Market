package Agents;

import Utils.Aircraft;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SubscriptionInitiator;
import jade.util.leap.Iterator;

/**
 * Created by Luis on 21/02/2017.
 */
public class SellerAgent extends Agent {
    /**
     * * Company's Identifier: @company
     */
    private final String role = "Seller";
    private String company = "";

    protected void setup() {
        // Build the description used as template for the subscription
        DFAgentDescription dfd;
        DFServices dfs = new DFServices();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(role);
        sd.setName(getLocalName());
        // sd.addProperties(new Property("country", "Italy"));
        dfd = dfs.register(sd, this);
        System.out.println("SELLER REGISTERED");
        try {
            System.out.println(getLocalName() + " is waiting for a message");
            ACLMessage msg = blockingReceive();
            //ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            //msg = receive(MessageTemplate.MatchPerformative(ACLMessage.CFP))
            System.out.println(getLocalName() + " received msg" + msg);

            if ("JavaSerialization".equals(msg.getLanguage())) {
                Aircraft a1 = (Aircraft) msg.getContentObject();
                String performative = Integer.toString(msg.getPerformative());
                System.out.println(getLocalName() + " STRING PERFORMATIVE = " + performative + " AND INTEGER PERFORMATIVE = " + msg.getPerformative());
                Aircraft a = new Aircraft(a1.getType(), a1.getCapacity());
                System.out.println(getLocalName() + " read Java Object " + a1.getClass().getName() + " and " + a1.toString());
                System.out.println(getLocalName() + "Aircraft A:");
                a.printAircraft();
                System.out.println("\n\n" + getLocalName() + " Aircraft A1: Type = " + a1.getType() + " and Capacity = " + a1.getCapacity());
                a.printAircraft();
            } else
                System.out.println(getLocalName() + " read Java String " + msg.getContent());
        } catch (UnreadableException e3) {
            System.err.println(getLocalName() + " catched exception " + e3.getMessage());
        }

            /* Recebeu, processa e responde */
    }
}

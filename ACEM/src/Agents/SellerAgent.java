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
    private MessageHandler msgHandler = new MessageHandler();

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
        System.out.println(sd.getName() + " is waiting for a message");
        ACLMessage msg = blockingReceive();
        msgHandler.receiveMsg(this, msg);
        /* Recebeu, processa e responde */
    }
}

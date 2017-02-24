package Agents;

import Utils.Aircraft;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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
    private MarketHandler marketHandler = new MarketHandler();
    private MessageHandler msgHandler = new MessageHandler();
    private DFServices dfs = new DFServices();
    private AID[] receiver;

    protected void setup() {
        // Build the description used as template for the subscription
        DFAgentDescription dfd;

        ServiceDescription sd = new ServiceDescription();
        sd.setType(role);
        sd.setName(getLocalName());
        // sd.addProperties(new Property("country", "Italy"));
        dfd = dfs.register(sd, this);
        addBehaviour(new ListeningBehaviour());
        marketHandler = new MarketHandler();

        /**
         * Msg Handler returns the resources asked for. The Agent must check if there are any resources available
         * and create a propose msg with the best fit
         * sendPropossMsg
         */
    };
    private class ListeningBehaviour extends CyclicBehaviour{

        @Override
        public void action() {
            msgHandler = new MessageHandler();
            ACLMessage receivedMsg = this.getAgent().receive();
            //ACLMessage receivedMsg = blockingReceive();
            System.out.println(this.getAgent().getLocalName() + " received a message: " + receivedMsg);
            if (receivedMsg != null) {
                System.out.println("A Chamar o market");
                receiver = new AID[]{receivedMsg.getSender()};
                marketHandler.setReceivers(receiver);
                marketHandler.processPerformative(this.getAgent(),receivedMsg,role);
                /*if (receivedMsg.getPerformative() != ACLMessage.ACCEPT_PROPOSAL) {
                    Aircraft aircraftToBeLeased = new Aircraft("Airbus 747", 200);
                    *//**
                     * receivedMsg.getSender is the sender from the last received message
                     *//*
                    msgHandler.prepareProposeSeller(this.getAgent(),receivedMsg.getSender(),aircraftToBeLeased);
                } else{
                    System.out.println("receivedMsg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL = " + (receivedMsg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL));
                    if (receivedMsg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                        System.out.println("Agreement achieved");
                        *//*aircraftAsked = msgHandler.getMsgResources(this.getAgent(), receivedMsg);
                        aircraftAsked.printAircraft();*//*

                    }
                }*/
            }
            block();
            /**
             * TODO else needed here?
             */
        }
    }
}
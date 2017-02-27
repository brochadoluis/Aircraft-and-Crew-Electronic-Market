package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

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
    }

    /**
     * After receiving a ACCEPT_PROPOSAL and finishs the contract, needs to deregister, update resources and register again
     */
    private class ListeningBehaviour extends CyclicBehaviour{

        @Override
        public void action() {
            msgHandler = new MessageHandler();
            ACLMessage receivedMsg = this.getAgent().receive();
            System.out.println(this.getAgent().getLocalName() + " received a message: " + receivedMsg);
            if (receivedMsg != null) {
                System.out.println("A Chamar o market");
                receiver = new AID[]{receivedMsg.getSender()};
                marketHandler.setReceivers(receiver);
                marketHandler.processPerformative(this.getAgent(),receivedMsg,role);
            }
            block();
            /**
             * TODO else needed here?
             */
        }
    }
}
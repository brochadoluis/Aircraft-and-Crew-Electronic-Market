package Agents;

import Utils.*;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luis on 20/02/2017.
 */
public class BuyerAgent extends Agent{
    /**
     * Resources to search in the EM: @missingResources
     * Company's Identifier: @company
     */
    private ArrayList<Aircraft> missingResources = new ArrayList<>();
    private float resourcesCost;
    private Date scheduledDeparture, flightDuration, flightDelay;
    private final String role = "Buyer";
    private String company = "";
    private MessageHandler msgHandler = new MessageHandler();
    private MarketHandler marketHandler;
    /**
     * TODO melhor estrutura para guardar do historico
     */
    List<ACLMessage> receivedMsgs = new ArrayList<>();
    Aircraft a1;
    protected void setup() {
        // initiateParameters();
        DFAgentDescription dfd;
        DFServices dfs = new DFServices();
        ServiceDescription sd  = new ServiceDescription();
        marketHandler = new MarketHandler();
        sd.setType(role);
        sd.setName( getLocalName() );
        //sd.addProperties(new Property("country", "Italy"));
        dfd = dfs.register(sd, this);

        AID agent = dfs.getService("Buyer", this);
        System.out.println("\nAgents: "
                +(agent==null ? "not Found" : agent.getName()));
        System.out.println("A procurar sellers");

        agent = dfs.getService("Seller",this);
        System.out.println("\nSeller: "
                +(agent==null ? "not Found" : agent.getName()));
        /* Saves to an array all sellers registered in DFService  */
        System.out.println("Uma lista bonita de sellers");
        marketHandler.setReceivers(dfs.searchDF("Seller", this));
        System.out.print("\nSELLERS: ");
        for (AID seller:marketHandler.getReceivers()){
            // Mudar para comparar a companhia. Se for a mesma, não entra para a lista de sellers
            //    if(!seller.getLocalName().equals(sd.getName())){
            System.out.print( seller.getLocalName() + ",  ");
            //  }
            /**
             * Found disruption
             * Resource needed = new Aircraft("Boeing 777", 396);
             */
            a1  = new Aircraft("Boeing 777", 396);
            marketHandler.setResourcesNeeded(a1);
        }
        addBehaviour(new SendCFPBehaviour());
        addBehaviour(new ListeningBehaviour());
        /**
         * Cyclic behaviour:
         * Send CFP to all agents registered as sellers
         * Start: Receive Proposals
         * Evaluate them
         * Answers with Comments/New Proposal?
         * Jumps to Start
         */
        //doDelete();
        //System.exit(0);
    }

    private void initiateParameters() {
        /**
         * Connect to DB and read values
         */
    }

    private class SendCFPBehaviour extends OneShotBehaviour{

        @Override
        public void action() {
            msgHandler = new MessageHandler();
            System.out.println("\nAircraft Missing: " + "Type = " + a1.getType() + " and Capacity = " + a1.getCapacity());
            System.out.println();
            msgHandler.prepareCFP(this.getAgent(), marketHandler.getReceivers(),a1);
        }
    }

    private class ListeningBehaviour extends CyclicBehaviour{

        @Override
        public void action() {
            msgHandler = new MessageHandler();
            if (receivedMsgs.size() < marketHandler.getReceivers().length) {
                ACLMessage receivedMsg = this.getAgent().receive();
                if (receivedMsg != null){
                    System.out.println(this.getAgent().getLocalName() + " received a message: " + receivedMsg);
                    marketHandler.processPerformative(this.getAgent(),receivedMsg,role);
                }
                block();
                /**
                 * Aircraft aircraftToLease is the Resource received from another Company
                 * Necessary to calculate the utility to evaluate if the Resource is worth or not
                 */
            }
            /**
             * TODO else needed here?
             */
        }
        /**
         * After receiving messages from all Sellers, there are tasks to be done
         */
        //doDelete();
        //onEnd();
    }
}








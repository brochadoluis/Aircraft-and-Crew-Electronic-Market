package agents;

import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import utils.*;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Created by Luis on 20/02/2017.
 */
public class BuyerAgent extends Agent{
    /**
     * Resources to search in the EM: @missingResources
     * Company's Identifier: @company
     */
    private ArrayList<Resource> resourcesMissing = new ArrayList<>();
    private float resourcesCost, maximumDisruptionCost;
    //private Data scheduledDeparture, flightDuration, flightDelay;
    private final String role = "Buyer";
    private String company = "";
    private MessageHandler msgHandler = new MessageHandler();
    private MarketHandler marketHandler;
    /**
     * TODO melhor estrutura para guardar do historico
     */
    private List<ACLMessage> receivedMsgs = new ArrayList<>();
    private Resource a1,cm1;
    protected void setup() {
        // initiateParameters();
        DFServices dfs = registerInDFS();
        findReceivers(dfs);
        /**
         * Connection to DB
         * Found disruption
         * Resource needed = new Aircraft("Boeing 777", 396);
         */
        //The price calculated by Buyer is the maximum to be paid
        a1  = new Aircraft(0f,"Boeing 777", 396);
        cm1 = new CrewMember(0f,2, "Pilot", "English A2");
        resourcesMissing.add(a1);
        resourcesMissing.add(cm1);
        marketHandler.setResourcesNeeded(resourcesMissing);
        DFAgentDescription dfd = new DFAgentDescription();
        System.out.println("My AID = " + this.getAID());
        try {
            DFAgentDescription list[] = DFService.search( this, dfd );
            for (DFAgentDescription element:list) {
                System.out.println("Portanto " + element.getName());
                if(Objects.equals(element.getName(), this.getAID()))
                    System.out.println("Sou o mesmo agente. Problema parcialmente resolvido");
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        sendFirstCPF();
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

    private void sendFirstCPF() {
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        // We want to receive a reply in 10 secs
        System.currentTimeMillis();
        msg.setReplyByDate(new Date(System.currentTimeMillis()+10000));
        try {
            msg.setContentObject(resourcesMissing);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void findReceivers(DFServices dfs) {
        AID agent = dfs.getService("Buyer", this);
        System.out.println("\nagents: "
                +(agent==null ? "not Found" : agent.getName()));
        System.out.println("A procurar sellers");

        agent = dfs.getService("Seller",this);
        System.out.println("\nSeller: "
                +(agent==null ? "not Found" : agent.getName()));
        /* Saves to an array all sellers registered in DFService  */
        System.out.println("Uma lista bonita de sellers");
//        AID[] sellers = dfs.searchDF("Seller", this);
        marketHandler.setReceivers(dfs.searchDF( "Seller",this));
        System.out.print("\nSELLERS: ");
        for (AID seller:marketHandler.getReceivers()){
            // Mudar para comparar a companhia? Se for a mesma, não entra para a lista de sellers
            //    if(!seller.getLocalName().equals(sd.getName())){
            System.out.print( seller.getLocalName() + ",  ");
        }
    }

    private DFServices registerInDFS() {
        DFAgentDescription dfd = new DFAgentDescription();
        DFServices dfs = new DFServices();
        ServiceDescription sd  = new ServiceDescription();
        marketHandler = new MarketHandler();
        sd.setType(role);
        sd.setName( getLocalName() );
        //sd.addProperties(new Property("country", "Italy"));
        dfd = dfs.register(sd, this);
        return dfs;
    }

    protected void takeDown() {
        System.out.println("Deregister " + this.getLocalName() +" from DFS. Bye...");
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
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
            System.out.println("\n\n\n\nResources Needed:");
            for (Resource r:resourcesMissing) {
                r.printResource();
            }
            System.out.println();
            msgHandler.prepareCFP(this.getAgent(), marketHandler.getReceivers(), resourcesMissing);
        }
    }

    /**
     * Needs a doDelete() when sends a AcceptProposal
     */
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
                else
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








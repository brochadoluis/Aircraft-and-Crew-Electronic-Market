package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;
import utils.Aircraft;
import utils.CrewMember;
import utils.Resource;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;


/**
 * Created by Luis on 20/02/2017.
 */
public class BuyerAgent extends Agent implements Serializable{
    /**
     * Resources to search in the EM: @missingResources
     * Company's Identifier: @company
     *
     *
     * Sends first CFP
     * Calls behaviour with msg handlers
     */
    private ArrayList<Resource> resourcesMissing = new ArrayList<>();
    private float resourcesCost;
    private double maximumDisruptionCost;
    //private Data scheduledDeparture, flightDuration, flightDelay;
    private final String role = "Buyer";
    private String company = "";
    private ArrayList<AID> sellers = new ArrayList<>();
    private int negotiationParticipants;
    /**
     * TODO melhor estrutura para guardar do historico
     */
    private List<ACLMessage> receivedMsgs = new ArrayList<>();
    private Resource a1,cm1;
    protected void setup() {
        // initiateParameters();
        // Read the maximum cost as argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            maximumDisruptionCost = (double) args[0];
            System.out.println("Worst case scenario " + maximumDisruptionCost + " € as cost.");
            DFServices dfs = registerInDFS();
            findReceivers(dfs);
            /**
             * Connection to DB
             * Found disruption
             * Resource needed = new Aircraft("Boeing 777", 396);
             */
            //The price calculated by Buyer is the maximum to be paid
            a1 = new Aircraft(0f, "Boeing 777", 396);
            cm1 = new CrewMember(0f, 2, "Pilot", "English A2");
            resourcesMissing.add(a1);
            resourcesMissing.add(cm1);
            negotiationParticipants = sellers.size();
            DFAgentDescription dfd = new DFAgentDescription();
            // Fill the CFP message
            ACLMessage msg = new ACLMessage(ACLMessage.CFP);
            for (AID seller:sellers) {
                msg.addReceiver(seller);
            }
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            // We want to receive a reply in 10 secs
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
            try {
                msg.setContentObject(resourcesMissing);
            } catch (IOException e) {
                e.printStackTrace();
            }
            addBehaviour(new ContractNetInitiator(this, msg){
                protected void handlePropose(ACLMessage propose, Vector v) {
                    try {
                        System.out.println("Agent "+propose.getSender().getName()+" proposed "+propose.getContentObject());
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }

                protected void handleRefuse(ACLMessage refuse) {
                    System.out.println("Agent "+refuse.getSender().getName()+" refused");
                }

                protected void handleFailure(Agent agent, ACLMessage failure) {
                    if (failure.getSender().equals(agent.getAMS())) {
                        // FAILURE notification from the JADE runtime: the receiver
                        // does not exist
                        System.out.println("Responder does not exist");
                    }
                    else {
                        System.out.println("Agent "+failure.getSender().getName()+" failed");
                    }
                    // Immediate failure --> we will not receive a response from this agent
                    negotiationParticipants--;
                }

                protected void handleAllResponses(Vector responses, Vector acceptances) {
                    if (responses.size() < negotiationParticipants) {
                        // Some responder didn't reply within the specified timeout
                        System.out.println("Timeout expired: missing "+(negotiationParticipants - responses.size())+" responses");
                    }
                    // Evaluate proposals.
                    //int bestProposal = -1;
                    ArrayList<Resource> bestProposal = new ArrayList<>();
                    AID bestProposer = null;
                    ACLMessage accept = null;
                    Enumeration e = responses.elements();
                    while (e.hasMoreElements()) {
                        ACLMessage msg = (ACLMessage) e.nextElement();
                        if (msg.getPerformative() == ACLMessage.PROPOSE) {
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            acceptances.addElement(reply);
                            ArrayList<Resource >resourcesFromProposal = getMsgResources(msg);
                            // Auxiliar ArrayList to compare if proposed resources are equal to the ones needed
                            ArrayList<Resource> resourcesProposed = getMsgResources(msg);
                            if(proposalMeetsNeeds(msg.getSender(), resourcesProposed, resourcesMissing)){
                                //if (proposal > bestProposal) {
                                //  bestProposal = proposal;
                                bestProposer = msg.getSender();
                                accept = reply;
                                bestProposal = new ArrayList<>(resourcesFromProposal);
                            }
                        }
                    }
                    // Accept the proposal of the best proposer
                    if (accept != null) {
                        System.out.println("Accepting proposal "+bestProposal+" from responder "+bestProposer.getName());
                        accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    }
                }

                protected void handleInform(ACLMessage inform) {
                    System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
                    doDelete();
                }
                //doDelete();
                //System.exit(0);
            });
        }
    }

    private boolean proposalMeetsNeeds(AID receiver, ArrayList<Resource> resourcesProposed, ArrayList<Resource> resourcesAsked) {
        System.out.println("Resources Received: \n\n");
        System.out.println("RFP size = "+ resourcesProposed.size() );
        System.out.println("RA size = "+ resourcesAsked.size() );
        for(int i = 0; i < resourcesAsked.size(); i++){
            for(int j = 0; j < resourcesProposed.size(); j++){
                if(resourcesAsked.get(i).compareResource(resourcesProposed.get(j))){
                    resourcesProposed.remove(j);
                }
                else
                    continue;
            }
        }
        if(resourcesProposed.isEmpty())
            return true;

        else
            return false;
    }

    private void findReceivers(DFServices dfs) {
        AID agent = dfs.getService(this);
        System.out.println("\nagents: "
                +(agent==null ? "not Found" : agent.getName()));
        System.out.println("A procurar sellers");
        agent = dfs.getService(this);
        System.out.println("\nSeller: "
                +(agent==null ? "not Found" : agent.getName()));
        /* Saves to an ArrayList all other agents registered in DFService  */
        System.out.println("Uma lista bonita de sellers");
        sellers = dfs.searchDF(this);
        System.out.println("\nSELLERS: ");
        for (AID seller:sellers) {
            System.out.println(seller.getName() + ",  ");
            System.out.println(seller.getLocalName() + ",  ");
        }
    }

    private DFServices registerInDFS() {
        DFAgentDescription dfd = new DFAgentDescription();
        DFServices dfs = new DFServices();
        ServiceDescription sd  = new ServiceDescription();
        //sd.setType(role);
        sd.setName( getLocalName() );
        //sd.addProperties(new Property("country", "Italy"));
        dfd = dfs.register(this);
        return dfs;
    }

    private void initiateParameters() {
        /**
         * Connect to DB and read values
         */
    }
    protected ArrayList<Resource> getMsgResources(ACLMessage msg) {

        ArrayList<Resource> resourcesToBeLeased = null;
        try {
            resourcesToBeLeased = (ArrayList<Resource>) msg.getContentObject();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
        return resourcesToBeLeased;
    }
}








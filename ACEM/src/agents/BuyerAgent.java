package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
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
    private PriorityQueue<Proposal> bestProposalQ = new PriorityQueue<>();
    private ArrayList<Resource> resourcesMissing = new ArrayList<>();
    private float resourcesCost;
    private double maximumDisruptionCost;
    //private Data scheduledDeparture, flightDuration, flightDelay;
    private final String role = "Buyer";
    private String company = "";
    private ArrayList<AID> sellers = new ArrayList<>();
    private int negotiationParticipants;
    private Integer round = 0;
    private Resource a1,cm1;
    private Proposal proposal;
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
            System.out.println("ROUND NUMBER CFP SEND: " + round);
            ACLMessage msg = new ACLMessage(ACLMessage.CFP);
            for (AID seller:sellers) {
                msg.addReceiver(seller);
            }
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            // We want to receive a reply in 10 secs
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
            try {
                msg.setContentObject(resourcesMissing);
                round++;
            } catch (IOException e) {
                e.printStackTrace();
            }
            addBehaviour(new ContractNetInitiator(this, msg){
                protected void handlePropose(ACLMessage propose, Vector v) {
                    try {
                        System.out.println("Agent "+propose.getSender().getName()+" proposed "+propose.getContentObject());
                        System.out.println("ROUND NUMBER HANDLE PROPOSE: " + round);
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }

                protected void handleRefuse(ACLMessage refuse) {
                    System.out.println("Agent "+refuse.getSender().getName()+" refused");
                    System.out.println("ROUND NUMBER HANDLE REFUSE: " + round);
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
                    //ArrayList<Resource> bestProposal = new ArrayList<>();
                    AID bestProposer = null;
                    ACLMessage accept = null;
                    Enumeration e = responses.elements();
                    while (e.hasMoreElements()) {
                        ACLMessage msg = (ACLMessage) e.nextElement();
                        if (msg.getPerformative() == ACLMessage.PROPOSE) {
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            acceptances.addElement(reply);
                            //Replace resource list with <Price,Availability>
                            //Remove getMsgResources, or change it to getMsgContent/getProposal whatever
                            //ArrayList<Resource >resourcesFromProposal = getMsgResources(msg);
                            try {
                                proposal = (Proposal) msg.getContentObject();
                            } catch (UnreadableException e1) {
                                e1.printStackTrace();
                            }
                            // Auxiliar ArrayList to compare if proposed resources are equal to the ones needed
                            //ArrayList<Resource> resourcesProposed = getMsgResources(msg);
                            /**
                             * Isto muda porque o seller so propoe se de facto tiver o que e pedido
                             * Passa a fazer o calculo da utilidades das propostas todas
                             * E insere a melhor na stack/queue.
                             * Prepara para todos os sellers os comentarios as propostas feitas
                             * Envia reject para todos com os comentarios e lanca novo CFP
                             * sendo que o detentor da bestProposal recebe um <OK,OK>
                             */
                            System.out.println("evaluateProposal(msg.getSender(), resourcesProposed, resourcesMissing) ");
                            evaluateProposal(msg.getSender(), proposal, responses);
                            if(!bestProposalQ.isEmpty()){
                                //if (proposal > bestProposal) {
                                //  bestProposal = proposal;
                                bestProposer = msg.getSender();
                                accept = reply;
                                System.out.println("Best Proposal is ");
                                bestProposalQ.peek().printProposal();
                                //bestProposalQ.add();
                            }
                            else{
                                //just to test the REJECT
                                System.out.println("Rejecting proposal "+ proposal.toString()+" from responder "+ msg.getSender());
                                try {
                                    reply.setContentObject("Lower");
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                                round++;
                                //send another CFP
                                System.out.println("A enviar mais um CFP");
                                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                                for (AID seller:sellers) {
                                    cfp.addReceiver(seller);
                                }
                                cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                                cfp.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                                try {
                                    cfp.setContentObject("");
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                                round++;
                            }
                        }
                    }
                    // Accept the proposal of the best proposer
                    if (accept != null) {
                        System.out.println("Accepting proposal "+bestProposalQ.toString()+" from responder "+bestProposer.getName());
                        accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        round++;
                        System.out.println("ROUND NUMBER AFTER SEND ACCEPT: " + round);
                    }
                    //Como nao existe accept, e necesario criar os comenatios, enviar REJECT com os comentarios e em seguida enviar novo CFP
                    else{
                        System.out.println("Don't like what I'm seeing");
                    }
                }

                protected void handleInform(ACLMessage inform) {
                    System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
                    System.out.println("ROUND NUMBER HANDLE INFORM: " + round);
                    doDelete();
                }
                //doDelete();
                //System.exit(0);
            });
        }
    }

    /**
     * evaluateProposal method evaluates the proposals received
     * according to Buyer's utility. Selects the best and adds it to
     * the Best Proposal Queue
     * @param receiver: TO BE REMOVED
     * @param proposal: Proposal read from ACL Message
     * @param responses: Vector with all the responses received
     * @return true if all resources match, false otherwise
     */
    private void evaluateProposal(AID receiver, Proposal proposal, Vector responses) {
        System.out.println("Proposal ");
        proposal.printProposal();
        System.out.println("Responses " + responses);
        double priceAsked = proposal.getPrice();
        if(priceAsked <= maximumDisruptionCost) {
            bestProposalQ.add(proposal);
            System.out.println("PROPOSAL ADDED TO QUEUE");
        }
        /*for(int i = 0; i < resourcesAsked.size(); i++){
            for(int j = 0; j < resourcesProposed.size(); j++){
                System.out.println("resourcesAsked.get(i).compareResource(resourcesProposed.get(j))" +resourcesAsked.get(i).compareResource(resourcesProposed.get(j)));
                if(resourcesAsked.get(i).compareResource(resourcesProposed.get(j))){
                    System.out.println("Resources asked: " + resourcesAsked);
                    System.out.println("Resources proposed: " + resourcesProposed);
                    resourcesAsked.remove(i);
                    resourcesProposed.remove(j);
                }
                else {
                    System.out.println("Resources dont match");
                    continue;
                }
            }
        }
        System.out.println(" resourcesProposed.isEmpty() " + resourcesProposed.isEmpty());
        System.out.println(" resourcesAsked.isEmpty() " + resourcesAsked.isEmpty());
        *///return resourcesProposed.isEmpty() && resourcesAsked.isEmpty();
//        return matchCounter == resourcesAsked.size();
    }

    private void findReceivers(DFServices dfs) {
        AID agent = dfs.getService(this);
        /* Saves to an ArrayList all other agents registered in DFService  */
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

    /**
     * getMsgResources method converts the ACL message content
     * to Resource type
     * @param msg : ACL message to be procesed
     * @return A list of resources contained in msg
     */
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
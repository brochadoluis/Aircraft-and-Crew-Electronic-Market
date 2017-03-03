package agents;

import jade.core.Agent;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetResponder;
import utils.Aircraft;
import utils.CrewMember;
import utils.Resource;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Created by Luis on 21/02/2017.
 */
public class SellerAgent extends Agent implements Serializable{
    /**
     * * Company's Identifier: @company
     */
    private final String role = "Seller";
    private String company = "";
    private DFServices dfs = new DFServices();
    //Each queue position must be a Proposal, this means, ArrayList<ArrayList <Resource>>
    private Queue possibleMatchesQ = new LinkedList();
    private PriorityQueue<ArrayList<ArrayList <Resource>>> resourcesQueue = null;
    private Integer round = 0;
    //Round - Proposal - Resources??
    //Lista de litas com Ronda-Recurso-Proposta?
    private HashMap<Integer,Proposal> negotiationHistoric = new HashMap<>();

    protected void setup() {
        // Build the description used as template for the subscription
        DFAgentDescription dfd;
        ServiceDescription sd = new ServiceDescription();
        //sd.setType(role);
        sd.setName(getLocalName());
        // sd.addProperties(new Property("country", "Italy"));
        dfd = dfs.register(this);
        System.out.println("Agent "+getLocalName()+" waiting for CFP...");
        System.out.println("ROUND NUMBER BEFORE GETTING CFP: " + round);
        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));

        addBehaviour(new ContractNetResponder(this, template) {
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
                ArrayList<ArrayList<Resource>> queueHead;
                ArrayList<ArrayList<Resource>> solutions = new ArrayList<>();
                /**
                 * Fetch Resources in DataBase
                 * Compare Resource form message with DataBase Fetch results
                 * Select the most similar Resource found in DataBase, to the one asked
                 * Compare both Resources
                 * Calculate utility and price and creates msg with resource's price and availability
                 * To test, the resource found is defined below
                 */
                /**
                 * Se for o primeiro CFP, a pilha esta vazia, e a estrutura do historico tambem. Ai faz o que
                 * esta em baixo.
                 * Se nao for o primeiro CFP, nao e preciso ir buscar os recursos A BD nem fazer match
                 * e so avaliar a proposta (comentarios), alterar ou nao, a proposta a fazer
                 *
                 */
                System.out.println("Agent " + getLocalName() + ": CFP received from " + cfp.getSender().getName() + ". Action is " + cfp.getContent());
                round++;
                System.out.println("ROUND NUMBER AFTER RECEIVE CFP: " + round);
                //First Round, means that seller needs to fetch for available resources in DB
                // if queue is empty and historic too, then its the first round, if historic is not empty, there are no more resources to negotiate
                if(resourcesQueue == null) {
                    ArrayList<Resource> askedResources = getMsgResources(cfp);
                    //Fetch funtion, writes to an ArrayList the resources available
//                    queueHead = getAvailableMatchingResources(askedResources);
                    solutions = getAvailableMatchingResources(askedResources);
                }
                //putResourcesIntoQueue(queueHead);
                queueHead =  putResourcesIntoQueue(solutions);
                while(!resourcesQueue.isEmpty()) {
                    ArrayList <ArrayList<Resource>>  head = resourcesQueue.poll();
                    System.out.println("Queue Item = " + head);
                    System.out.println("Next Head is: " + resourcesQueue.peek());
                }
                System.out.println("resourecesQyueue.isEmpty = " + resourcesQueue.isEmpty());
                if (!resourcesQueue.isEmpty()) {
                    System.out.println("Agent " + getLocalName() + ": Searching for resources to lease ");
                    System.out.println("Agent " + getLocalName() + ": Proposing ");
                    ACLMessage propose = cfp.createReply();
                    propose.setPerformative(ACLMessage.PROPOSE);
                    try {
                        propose.setContentObject(queueHead);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return propose;
                }
                else {
                    // We refuse to provide a proposal
                    System.out.println("Agent " + getLocalName() + ": Refuse");
                    throw new RefuseException("Can't lease the resources asked");
                }
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,ACLMessage accept) throws FailureException {
                System.out.println("Agent "+getLocalName()+": Proposal accepted");
                round++;
                System.out.println("ROUND NUMBER HANDLE ACCEPT PROPOSAL: " + round);
                if (true) { // contractualization
                    System.out.println("Agent "+getLocalName()+": Action successfully performed");
                    ACLMessage inform = accept.createReply();
                    inform.setPerformative(ACLMessage.INFORM);
                    return inform;
                }
                else {
                    System.out.println("Agent "+getLocalName()+": Action execution failed");
                    throw new FailureException("unexpected-error");
                }
            }

            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                System.out.println("Agent "+getLocalName()+": Proposal rejected");
                round++;
                System.out.println("ROUND NUMBER HANDLE REJECT: " + round);
                /**
                 * Tenta negociar o primeiro recurso na stack/queue, se a utilidade descer demasiado (exemplo, ficar ao mesmo nivel da utilidade da proxima
                 * posiçao da stack/queue) muda o recurso a ser negociado, isto e, o recurso no topo da stack/queue.
                 */
            }
        } );
    }

    /**
     * getAvailableMatchingResources method fetches Resources in DataBase
     * Compare Resources from message with DataBase fetch results
     * Pushes the most similar Resources found in DataBase to a stack/queue
     * Apply compareAskedResourceWithAvailableOnes and send the result to a stack/queue
     * @param askedResources
     */
    private ArrayList<ArrayList<Resource>> getAvailableMatchingResources(ArrayList<Resource> askedResources) {
        ArrayList<Resource> availableResources = new ArrayList<>();
        Resource r1  = new Aircraft(1432.53f,"Boeing 777", 396);
        Resource r2 = new CrewMember(3840.54f,2, "Pilot", "English A2");
        Resource r3 = new Aircraft(1200.534f,"Boeing 767", 375);
        Resource r4 = new CrewMember(689.54f,8, "Flight Attendant", "English B2");
        Resource r5 = new Aircraft(1200.534f,"Airbus A318", 132);
        Resource r6 = new CrewMember(689.54f,1, "Flight Medic", "English A2");
        Resource r7 = new Aircraft(1200.534f,"Airbus A330-200 Freighter", 407);
        Resource r8 = new CrewMember(689.54f,4, "Load Master", "English C2");
        availableResources.add(r1);
        availableResources.add(r2);
        availableResources.add(r3);
        availableResources.add(r4);
        availableResources.add(r5);
        availableResources.add(r6);
        availableResources.add(r7);
        availableResources.add(r8);

        //Depending on the query to the DB, compare may not be needed
        return compareAskedResourceWithAvailableOnes(askedResources, availableResources);
    }

    //Return peek or element from queue
    private ArrayList<ArrayList<Resource>> putResourcesIntoQueue(ArrayList<ArrayList<Resource>> solutions) {
        if(solutions.size() == 0) {
            System.out.println("Resources not available");
            //exits negotiation
        }
        else{
            int queueSize = solutions.size();
            Comparator<ArrayList<ArrayList<Resource>>>  comparator = new UtilityComparator();
            resourcesQueue = new PriorityQueue<>(queueSize, comparator );
            //for(int i = 0; i < solutions.size(); i++)
                resourcesQueue.add(solutions);
            //for (Resource resource:possibleMatches){
            /**
             * Calculates utility
             * Creates/Initializes priority queue with comparator
             * Comparator<Integer> comparator = new UtilityComparator();
             * PriorityQueue<ArrayList <Resources>> queue =
             * new PriorityQueue<ArrayList <Resources>>(solutions.size(), comparator);
             * queue.add("short");
             * queue.add("very long indeed");
             * queue.add("medium");
             *
             */
            //}
        }
        /**
         * returns the top element with peek() or element()
         * after each round, necessary to remove the top (to variable head for example) and compare the utility of the proposal (after
         * comments applied) with the new top of the queue. If the difference is too small, the head is not added to queue again, and the new
         * top is now the resource under negotiation
         */
        return resourcesQueue.peek();
        //return possibleMatches;
        //possibleMatchesQ.add(resource);
        //possibleMatchesS.push(resource);
    }

    /**
     * compareAskedResourceWithAvailableOnes method, compares the resources asked by Buyer Agent
     * with the resources available for one Seller
     * @param askedResources: List of resources asked by Buyer
     * @param availableResources: List of Seller's all available resources
     * @return An Arraylist of resources, containing the resources that match with the ones asked
     */
    private ArrayList< ArrayList<Resource> > compareAskedResourceWithAvailableOnes(ArrayList<Resource> askedResources, ArrayList<Resource> availableResources) {
        ArrayList< ArrayList<Resource> > solutions = new ArrayList<>();
        for (Resource askedResource:askedResources) {//para cada 1 destes, adiciona 1 lista ao solutions
            ArrayList<Resource> aux = new ArrayList<>();
            for (Resource resource : availableResources) {
                if (!resource.compareResource(askedResource))
                    continue;
                else{
                    //push
                    aux.add(resource);
                    System.out.println(resource + "Added to possibleMatches");
                }
            }
            solutions.add(aux);
        }
        for (ArrayList<Resource> sol:solutions) {
            System.out.println("Solutions:  " + sol);
        }
        System.out.println("Solutions Complete: " + solutions);
        //findPossibleMatches(solutions);
        return solutions;
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
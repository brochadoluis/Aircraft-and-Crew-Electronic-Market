package agents;

import jade.core.Agent;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.SSIteratedContractNetResponder;
import jade.util.Logger;
import utils.Aircraft;
import utils.CrewMember;
import utils.Resource;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;


/**
 * Created by Luis on 21/02/2017.
 */
public class SellerAgent extends Agent implements Serializable{
    /**
     * * Company's Identifier: @company
     */
    private final Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());
    //    private final String role = "Seller";
    private String company = "";
    private DFServices dfs = new DFServices();
    private PriorityQueue<ArrayList<Resource>> resourcesQueue = null;
    ArrayList<ArrayList<Resource>> solutions = new ArrayList<>();
    private Integer round = 0;
    //Round - Proposal - Resources??
    //Lista de litas com Ronda-Recurso-Proposta?
    //HashMap<Ronda,Proposta>
    private HashMap<Integer,Proposal> negotiationHistoric = new HashMap<>();
    /**
     * Price range between:
     * min = displacement cost + 50% displacement cost
     * max = displacement cost + 200% displacement cost
     */
    private HashMap<Resource,Double> resourcePrice = new HashMap<>();
    private HashMap<Resource,Long> resourceMaxAvailability = new HashMap<>();
    private Proposal proposal;
    /**
     * Max availability is the availability read from DB
     * The availaibility sent in the proposal may not be equal to max, to
     * ensure that availability is a negotiable parameter
     */
    long scheduledDeparture,delay, duration, maxAvailability;



    @Override
    protected void setup() {
        // Build the description used as template for the subscription
        createLogger();
        DFAgentDescription dfd;
        ServiceDescription sd = new ServiceDescription();
        //sd.setType(role);
        sd.setName(getLocalName());
        // sd.addProperties(new Property("country", "Italy"));
        dfd = dfs.register(this);
        logger.log(Level.INFO,"Agent {0} waiting for CFP...  ",getLocalName());
        logger.log(Level.INFO,"Round n(before receiving CFP> {0})", round);
        /*MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));*/
        ACLMessage cfp = blockingReceive();
        addBehaviour(new SSIteratedContractNetResponder(this, cfp) {

            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
                /**
                 * Fetch Resources in DataBase
                 * Compare Resource form message with DataBase Fetch results
                 * Select the most similar Resource found in DataBase, to the one asked
                 * Compare both Resources
                 * Calculate utility and price and creates msg with resource's price and availability
                 * To test, the resource found is defined below
                 *//*
                *//**
                 * Se for o primeiro CFP, a pilha esta vazia, e a estrutura do historico tambem. Ai faz o que
                 * esta em baixo.
                 * Se nao for o primeiro CFP, nao e preciso ir buscar os recursos A BD nem fazer match
                 * e so avaliar a proposta (comentarios), alterar ou nao, a proposta a fazer
                 *
                 */
                //Test: no resources available => doesnt respond to cfp/respond refuse?
                updateRound();
                logger.log(Level.INFO,"Agent {0}: CFP received from {1}. Action is {2} ",new Object[]{getLocalName(),cfp.getSender().getLocalName(),cfp.getContent()});
                logger.log(Level.INFO,"Round n(upon receiving CFP: {0})", round);
                /**
                 * First Round means that sellers need to fetch for available resources in the DataBase
                 * The first round is identified by having both, Resource's Queue and Negotiation Historic HashMap, empty
                 */
                if(resourcesQueue == null && negotiationHistoric.isEmpty()) {
                    handleFirstCFP(cfp);
                }/*
                while(!resourcesQueue.isEmpty()){
                    ArrayList<Resource> head = resourcesQueue.poll();
                    for (Resource r: head)
                        r.printResource();
                }*/
                else{
                    ArrayList<Resource> queueHead;
                    queueHead =  resourcesQueue.peek();
                    /**
                     * Comments received in Reject, are applied to the proposal, price and availability may be updated
                     */
                    Proposal rejectedProposal = null;
                    try {
                        rejectedProposal = (Proposal) cfp.getContentObject();
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                    System.out.println("rejected proposal = " +  rejectedProposal);
                    rejectedProposal.printComments();
                    negotiationHistoric.put(round-1,rejectedProposal);
                    proposal = new Proposal(40000F,queueHead, this.getAgent().getAID());
                }
                if (!resourcesQueue.isEmpty()){
                    System.out.println("!resourcesQueue.isEmpty()");
                    ACLMessage propose = cfp.createReply();
                    propose.setPerformative(ACLMessage.PROPOSE);
                    System.out.println("Proposal antes de enviar " + proposal);
                    try {
                        //Add proposal to historic
                        propose.setContentObject(proposal);
                        System.out.println("ASASDSAD = " + proposal);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE,"Could not set the message's content {0}",e);
                    }
                    return propose;
                }
                else {
                    // We refuse to provide a proposal
                    logger.log(Level.INFO,"Agent {0}: Refuse",getLocalName());
                    throw new RefuseException("Can't lease the resources asked");
                }
            }

            private Proposal handleFirstCFP(ACLMessage cfp) {
                ArrayList<Resource> queueHead;
                ArrayList<Resource> askedResources = getMsgResources(cfp);
                logger.log(Level.INFO,"Agent {0}: Searching for resources to lease ",getLocalName());
                logger.log(Level.INFO,"Agent {0}: Proposing",getLocalName());
                //Fetch funtion, writes to an ArrayList the resources available
                solutions = getAvailableMatchingResources(askedResources);
                System.out.println("SOLUTIONS SIZE = " + solutions.size());
                putResourcesIntoQueue(solutions);
                queueHead =  resourcesQueue.peek();
                /**
                 * Price here must be calculated, for example, in getAvailableMatchingResources
                 */
                proposal = new Proposal(40000F,queueHead, this.getAgent().getAID());
                long worstAvailability = getWorstAvailability(queueHead);
                proposal.setAvailability(worstAvailability);
                proposal.setPrice(65000F);
                negotiationHistoric.put(round,proposal);

                return proposal;
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,ACLMessage accept) throws FailureException {
                logger.log(Level.INFO,"Agent {0}: Proposal Accepted",getLocalName());
                updateRound();
                logger.log(Level.INFO,"Round n(handle accept proposal): {0}",round);
                if (true) { // contractualization
                    logger.log(Level.INFO,"Agent {0}: Action successfully performed",getLocalName());
                    ACLMessage inform = accept.createReply();
                    inform.setPerformative(ACLMessage.INFORM);
                    return inform;
                }
                else {
                    logger.log(Level.INFO,"Agent {0}: Action execution failed",getLocalName());
                    throw new FailureException("unexpected-error");
                }
            }

            @Override
            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                //Content == null => end?
                logger.log(Level.INFO,"Agent {0}: Proposal rejected",getLocalName());
                try {
                    logger.log(Level.INFO,"Message received is {0}:",reject.getContentObject());
                    proposal.setPriceComment(reject.getContentObject().toString());
                } catch (UnreadableException e) {
                    logger.log(Level.INFO,"Could not et message's content {0}",e);
                }
                updateRound();
                logger.log(Level.INFO,"Round n(handle reject): {0}",round);
                negotiationHistoric.put(round,proposal);
                logger.log(Level.INFO,"Historic updated. It has now: ");
                negotiationHistoric.get(round).printComments();
                /**
                 * Loads round-1 key form historic, updates proposal to the same parameters and
                 * adds the comments received in the reject
                 */
                /**
                 * Tenta negociar o primeiro recurso na stack/queue, se a utilidade descer demasiado (exemplo, ficar ao mesmo nivel da utilidade da proxima
                 * posiçao da stack/queue) muda o recurso a ser negociado, isto e, o recurso no topo da stack/queue.
                 */
            }
        } );
    }

    private void updateRound() {
        round++;
    }

    private void createLogger() {
        logger.setLevel(Level.FINE);
        // create an instance of Logger at the top of the file, as you would do with log4j
        FileHandler fh = null;   // true forces append mode
        try {
            String logName = this.getLocalName() + " logFile.log";
            fh = new FileHandler(logName, false);
            SimpleFormatter sf = new SimpleFormatter();
            fh.setFormatter(sf);
            logger.addHandler(fh);

        } catch (IOException e) {
            logger.log(Level.SEVERE,"Could not create Log File {0} " , e);
        }
    }

    /**
     * getAvailableMatchingResources method fetches Resources in DataBase
     * Builds an ArrayList<Resource> for each query
     * Combines the ArrayLists obtained from the queries
     * Pushes the result to the queue, by utility, in decreasing order,
     * using the Utility Comparator
     * @param askedResources: Resources received in ACL Message
     */
    private ArrayList<ArrayList<Resource>> getAvailableMatchingResources(ArrayList<Resource> askedResources) {
        ArrayList<Resource> availableResources = new ArrayList<>();
        ArrayList<ArrayList<Resource>> allResourcesCombinations = new ArrayList<>();
        /**
         * For each askedResource, create an ArrayList<Resource> where the resource to fetch in the DB
         * is askedResource.getClass (Switch here to prepare the query?)
         * Check something to get the availability of each resource (time needed to make the resource
         * available in the local needed) and that displacement price to set min price.
         *
         * http://stackoverflow.com/questions/14767697/dynamically-creating-arraylist-inside-a-loop
         * Lista de listas, cria uma lista, chama a XPTO e adiciona essa lista a lista de listas
         * assim ja da para fazer combinaçoes
         * Exemplo
         * FUNÇAO_XPTO(askedResource.getClass)
         * FUNÇAO_XPTO
         * switch(agument)
         * case Aircraft -> faz query com capacidade do aviao
         * case CrewMember -> faz query com os campos do tripulante
         *
         *
         * para combinar numa lista, tipo {{1,2,3},{1,2,3},{1,2,3}}
         * http://stackoverflow.com/questions/9446929/value-combinations-of-n-subarrays-in-java
         * experimentar este
         *
         * Falta tambem ver se os recuros estao disponiveis na duraçao pretendida
         *
         */
        Resource r1  = new Aircraft("Boeing 777", 396);
        r1.setAvailability(1500L);
        Resource r2 = new CrewMember(2, "Pilot", "English A2");
        r2.setAvailability(12900L);
        Resource r3 = new Aircraft("Boeing 767", 400);;
        r3.setAvailability(1900L);
        Resource r4 = new Aircraft("Airbus A330-200 Freighter", 407);
        r4.setAvailability(900L);
        Resource r5  = new Aircraft("Boeing 777", 396);
        r5.setAvailability(1000L);
        Resource r6 = new CrewMember(2, "Pilot", "English A2");
        r6.setAvailability(1150L);
        availableResources.add(r1);
        availableResources.add(r2);
        availableResources.add(r3);
        availableResources.add(r4);
        availableResources.add(r5);
        availableResources.add(r6);
        logger.log(Level.INFO,"Available Resources {0}", availableResources);
        //The query returns resources within the parameters, so there's no reason to compare

        for(int i = 0; i < availableResources.size(); i++) {
            for (int j = i; j < availableResources.size(); j++) {
                if (availableResources.get(i).getClass() != availableResources.get(j).getClass()) {
                    ArrayList<Resource> combination = new ArrayList<>();
                    combination.ensureCapacity(askedResources.size());
                    combination.add(availableResources.get(i));
                    combination.add(availableResources.get(j));
                    allResourcesCombinations.add(combination);
                    continue;
                }
            }
        }
        /*for (ArrayList<Resource> r:allResourcesCombinations
             ) {

        }*/
        return allResourcesCombinations;
    }

    //Return peek or element from queue
    private ArrayList<Resource> putResourcesIntoQueue(ArrayList<ArrayList<Resource>> solutions) {
        if(solutions.isEmpty()) {
            logger.log(Level.INFO,"Resources not available");
            //exits negotiation
        }
        else{
            int queueSize = solutions.size();
            Comparator<ArrayList<Resource>>  comparator = new UtilityComparator();
            resourcesQueue = new PriorityQueue<>(queueSize, comparator );
            for(int i = 0; i < solutions.size(); i++) {
                System.out.println("Solutions.get(" + i + ") = " +solutions.get(i));

                resourcesQueue.add(solutions.get(i));
            }
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
        }
        /**
         * returns the top element with peek() or element()
         * after each round, necessary to remove the top (to variable head for example) and compare the utility of the proposal (after
         * comments applied) with the new top of the queue. If the difference is too small, the head is not added to queue again, and the new
         * top is now the resource under negotiation
         */
        return resourcesQueue.peek();
    }

    /**
     * setProposalAvailability method, finds a proposal's worst (higher) availability,
     * among the resources contained in the proposal
     * @param resources: ArrayList with the resources to be negotiated
     * @return A Long value, corresponding to the worst availability among the resources to be negotiated
     */
    private long getWorstAvailability(ArrayList<Resource> resources) {
        long worstAvailability = -1;
        if(resources.get(0).getAvailability() != null) {
            worstAvailability = resources.get(0).getAvailability();
            for (int i = 0; i < resources.size(); i++) {
                Resource aResource = resources.get(i);
                worstAvailability = aResource.getAvailability();
                if (aResource.getAvailability() > worstAvailability) {
                    worstAvailability = aResource.getAvailability();
                }
            }
        }
        return worstAvailability;
    }

    /**
     * getMsgResources method converts the ACL message content
     * to Resource type
     * @param msg : ACL message to be procesed
     * @return A list of resources contained in msg
     */
    protected ArrayList<Resource> getMsgResources(ACLMessage msg) {
        ArrayList<Resource> resourcesToBeLeased = null;
        Proposal cfp = null;
        try {
            cfp = (Proposal) msg.getContentObject();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
        resourcesToBeLeased = (ArrayList<Resource>) cfp.getResourcesProposed();
        scheduledDeparture = cfp.getScheduledDeparture();
        delay = cfp.getDelay();
        return resourcesToBeLeased;
    }
}
package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
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
    //HashMap<round,proposal>
    private HashMap<Integer,Proposal> negotiationHistoric = new HashMap<>();
    /**
     * Price range between:
     * min = displacement cost + 50% displacement cost
     * max = displacement cost + 200% displacement cost
     */
    private HashMap<Resource,Double> resourcePrice = new HashMap<>();
    private HashMap<Resource,Long> resourceMaxAvailability = new HashMap<>();
    private Proposal proposal;
    private double maximumPrice, minimumPrice;
    /**
     * Max availability is the availability read from DB
     * The availaibility sent in the proposal may not be equal to max, to
     * ensure that availability is a negotiable parameter
     */
    long scheduledDeparture,delay, duration, maxAvailability;
    private final String LOWER = "LOWER";
    private final String MUCH_LOWER = "MUCH LOWER";
    private final String OK = "OK";



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
        logger.log(Level.INFO, "Agent {0} waiting for CFP...  ", getLocalName());
        logger.log(Level.INFO, "Round n(before receiving CFP> {0})", round);
        /*MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));*/

        ACLMessage msg = blockingReceive();

        addBehaviour(new SSIteratedContractNetResponder(this, msg){
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
                /**
                 * Fetch Resources in DataBase
                 * Compare Resource form message with DataBase Fetch results
                 * Select the most similar Resource found in DataBase, to the one asked
                 * Compare both Resources
                 * Calculate utility and price and creates msg with resource's price and availability
                 * To test, the resource found is defined below
                 */
                //Test: no resources available => doesnt respond to cfp/respond refuse?
                updateRound();
                System.out.println("MESSAGE RECEIVED IS: " + cfp.getPerformative());
                logger.log(Level.INFO, "Agent {0}: CFP received from {1}. Action is {2} ", new Object[]{getLocalName(), cfp.getSender().getLocalName(), cfp.getContent()});
                logger.log(Level.INFO, "Round n(upon receiving CFP: {0})", round);
                /**
                 * First Round means that sellers need to fetch for available resources in the DataBase
                 * The first round is identified by having both, Resource's Queue and Negotiation Historic HashMap, empty
                 */
                if (resourcesQueue == null && negotiationHistoric.isEmpty()) {
                    handleFirstCFP(cfp);
                } else {
                    /**
                     * Comments received in Reject, may be applied to the proposal, price and availability may be updated
                     */
                    Proposal rejectedProposal = null;
                    try {
                        rejectedProposal = (Proposal) cfp.getContentObject();
                    } catch (UnreadableException e) {
                        logger.log(Level.SEVERE, "Could not get the message's content {0}", e);
                    }
                    System.out.println("rejected proposal = " + rejectedProposal);
                    rejectedProposal.printComments();
                    //Add proposal to historic
                    negotiationHistoric.put(round - 1, rejectedProposal);
                    proposal = applyCommentsToProposal(rejectedProposal);
                }
                if (!resourcesQueue.isEmpty()) {
                    System.out.println("!resourcesQueue.isEmpty() = " + !resourcesQueue.isEmpty());
                    ACLMessage propose = cfp.createReply();
                    propose.setPerformative(ACLMessage.PROPOSE);
                    try {
                        System.out.println("Setting message content");
                        proposal.printProposal();
                        propose.setContentObject(proposal);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Could not set the message's content {0}", e);
                    }
                    return propose;
                } else {
                    // We refuse to provide a proposal
                    logger.log(Level.INFO, "Agent {0}: Refuse", getLocalName());
                    ACLMessage refuse = cfp.createReply();
                    refuse.setPerformative(ACLMessage.REFUSE);
                    try {
                        refuse.setContentObject("Refused. Bye");
                        resetNegotiationStructure();
                        negotiationHistoric = new HashMap<>();
                        return refuse;
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Could not set the message's content {0}", e);
                    }
                    throw new RefuseException("Can't lease the resources asked");
                }
            }

            private Proposal handleFirstCFP(ACLMessage cfp) {
                ArrayList<Resource> queueHead;
                ArrayList<Resource> askedResources = getMsgResources(cfp);
                logger.log(Level.INFO, "Agent {0}: Searching for resources to lease ", getLocalName());
                logger.log(Level.INFO, "Agent {0}: Proposing", getLocalName());
                //Fetch funtion, writes to an ArrayList the resources available
                solutions = getAvailableMatchingResources(askedResources);
                System.out.println("SOLUTIONS SIZE = " + solutions.size());
                for (ArrayList<Resource> r : solutions) {
                    System.out.println("NO HABDLE FIRST CFP, SOLUTION: " + r);
                }
                putResourcesIntoQueue(solutions);
                queueHead = resourcesQueue.peek();
                double displacementCosts = sumResourcesPrice(queueHead);
                minimumPrice = displacementCosts + (displacementCosts * 0.5);
                maximumPrice = displacementCosts + (displacementCosts * 2);
                System.out.println("Costs: " + displacementCosts);
                System.out.println("Minimum Price: " + minimumPrice);
                System.out.println("Maximum Price: " + maximumPrice);
                long worstAvailability = getWorstAvailability(queueHead);
                proposal = new Proposal(maximumPrice, worstAvailability, queueHead, this.getAgent().getAID());
                //proposal.setAvailability(worstAvailability);
                System.out.println("Delay to minime = " + delay);
                System.out.println("PRPOSING PROPOSAL AVAILABILITY = " + proposal.getAvailability());
                negotiationHistoric.put(round, proposal);

                return proposal;
            }

            @Override
            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                resetNegotiationStructure();
                /**
                 * Ends this seller participation in the negotiation
                 */
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                logger.log(Level.INFO, "Agent {0}: Proposal Accepted", getLocalName());
                updateRound();
                logger.log(Level.INFO, "Round n(handle accept proposal): {0}", round);
                if (true) { // contractualization
                    logger.log(Level.INFO, "Agent {0}: Action successfully performed", getLocalName());
                    ACLMessage inform = accept.createReply();
                    inform.setPerformative(ACLMessage.INFORM);
                    resetNegotiationStructure();
                    return inform;
                } else {
                    logger.log(Level.INFO, "Agent {0}: Action execution failed", getLocalName());
                    throw new FailureException("unexpected-error");
                }
            }
        });
    }

    private void resetNegotiationStructure() {
        resourcesQueue = null;
        solutions = new ArrayList<>();
        round = 0;
    }

    private Proposal applyCommentsToProposal(Proposal rejectedProposal) {
        ArrayList<Resource> queueHead = resourcesQueue.poll();
        double displacementCosts = sumResourcesPrice(queueHead);
        double minPrice = displacementCosts + (displacementCosts*0.5);
        double maxPrice = displacementCosts + (displacementCosts*2);
        System.out.println("Costs in applyCommentsToProposal: " + displacementCosts);
        System.out.println("Minimum Price in applyCommentsToProposal: " + minPrice);
        System.out.println("Maximum Price in applyCommentsToProposal: " + maxPrice);
        String priceComment = rejectedProposal.getPriceComment();
        String availabilityComment = rejectedProposal.getAvailabilityComment();
        switch (priceComment){
            case OK:
                System.out.println("OK received on price. Nothing to change");
                break;
            case LOWER:
                System.out.println("LOWER received on price. Going to decrease a bit the price");
                rejectedProposal.setPrice(rejectedProposal.getPrice()*0.85);
                System.out.println("LOWER: New price = " + rejectedProposal.getPrice()*0.85);
                break;
            case MUCH_LOWER:
                System.out.println("MUCH LOWER received on price. Going to decrease a lot the price");
                rejectedProposal.setPrice(rejectedProposal.getPrice()*0.6);
                System.out.println("LOWER: New price = " + rejectedProposal.getPrice()*0.6);
                break;
            default:
                break;
        }
        switch (availabilityComment){
            case OK:
                System.out.println("OK received on availability. Nothing to change");
                break;
            case LOWER:
                System.out.println("LOWER received on availability. Checking if it is possible to reduce");
                //Something to check this
                rejectedProposal.setAvailability((long) (rejectedProposal.getAvailability()*0.85));
                System.out.println("LOWER: New availability = " + rejectedProposal.getAvailability()*0.85);
                break;
            case MUCH_LOWER:
                System.out.println("MUCH LOWER received on availability. Checking if it is possible to reduce");
                //Something to check this
                rejectedProposal.setAvailability((long) (rejectedProposal.getAvailability()*0.6));
                System.out.println("LOWER: New availability = " + rejectedProposal.getAvailability()*0.6);
                break;
            default:
                break;
        }
        System.out.println("if(rejectedProposal.getPrice()/maxPrice "  + (rejectedProposal.getPrice()/maxPrice));
        System.out.println("Availability = " + availabilityComment);
        System.out.println("rejectedProposal.getPrice()/maxPrice <= 0.5 && availabilityComment.equals(OK) = "  +
                (rejectedProposal.getPrice()/maxPrice <= 0.5 && availabilityComment.equals(OK)));
        if (!resourcesQueue.isEmpty()) {
            if (hasBetterUtility(rejectedProposal, resourcesQueue.peek())) {
                resourcesQueue.add(queueHead);
            }
        }//remove this else, and implement a decent stop condition
        else{
            System.out.println("Queue is empty, the resource contained in queueHead is the lasts available option");
        }

//        }
        proposal = new Proposal(rejectedProposal.getPrice(), rejectedProposal.getAvailability(), rejectedProposal.getResourcesProposed(), this.getAID());
        return proposal;
    }

    private boolean hasBetterUtility(Proposal rejectedProposal, ArrayList<Resource> peek) {
        System.out.println("rejectedProposal.getPrice() " + rejectedProposal.getPrice()/maximumPrice);
        System.out.println("sumResourcesPrice(peek) " + sumResourcesPrice(peek)/maximumPrice);
        System.out.println("(utilityCalculation(rejectedProposal.getPrice())) " + (utilityCalculation(rejectedProposal.getPrice())));
        System.out.println("(utilityCalculation(sumResourcesPrice(peek))) " + (utilityCalculation(sumResourcesPrice(peek))));
        System.out.println("(utilityCalculation(rejectedProposal.getPrice()) > (utilityCalculation(sumResourcesPrice(peek)))) " + (utilityCalculation(rejectedProposal.getPrice()) > (utilityCalculation(sumResourcesPrice(peek)))));
        return (utilityCalculation(rejectedProposal.getPrice()) > (utilityCalculation(sumResourcesPrice(peek))));
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

    public double sumResourcesPrice(ArrayList<Resource> resourcesToBeProposed){
        double sum = 0;
        for (Resource aResource:resourcesToBeProposed) {
            sum += aResource.getPrice();
        }
        return sum;
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
        /**
         * The starting available date must be read too, so the resource availability  is
         * ScheduledDeparture - what was read from DB
         * and if that difference is < 0,
         * compare it with delay (delay - that difference, because both are negative).
         * if >0 means that the resource isnt good -1 - (-3) 2 => -3 is later than -1
         * Ideal scenario, difference = delay
         * Otherwise, the more negative the better.
         */
        Resource r1  = new Aircraft("Boeing 777", 396);
        r1.setPrice(23543.23D);
        r1.setAvailability(0L);
        Resource r2  = new Aircraft("Boeing 777", 396);
        r2.setPrice(23543.23D);
        r2.setAvailability(10000L);
        Resource r3 = new Aircraft("Boeing 767", 400);;
        r3.setPrice(31423.89D);
        r3.setAvailability(19000L);
        Resource r4 = new Aircraft("Airbus A330-200 Freighter", 407);
        r4.setPrice(451123.51D);
        r4.setAvailability(9000L);
        Resource r5 = new CrewMember(2, "Pilot", "English A2");
        r5.setPrice(5543.23D);
        r5.setAvailability(129000L);
        Resource r6 = new CrewMember(2, "Pilot", "English A2");
        r6.setPrice(65333.21D);
        r6.setAvailability(11500L);
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

    private double utilityCalculation(double priceOffered) {
        return((priceOffered - minimumPrice)/(maximumPrice-minimumPrice));
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
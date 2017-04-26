package agents;

import cbr.CBR;
import cbr.Data;
import cbr.FileManager;
import db_connection.Loader;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SSIteratedContractNetResponder;
import jade.proto.SSResponderDispatcher;
import jade.util.Logger;
import utils.Aircraft;
import utils.CrewMember;
import utils.Flight;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;



/**
 * Created by Luis on 21/02/2017.
 */
public class SellerAgent extends Agent implements Serializable{
    private final String OK = "OK";
    private final String LOWER = "LOWER";
    private final String MUCH_LOWER = "MUCH LOWER";
    private final String CPT = "cpt";
    private final String OPT = "opt";
    private final String SCB = "scb";
    private final String CCB = "ccb";
    private final String CAB = "cab";

    private final Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());
    ArrayList<Flight> solutions = new ArrayList<>();
    private DFServices dfs = new DFServices();
    private PriorityQueue<Flight> flightsQueue = null;
    private Integer round = 0;
    //HashMap<round,proposal>
    private HashMap<Integer,Proposal> negotiationHistoric = new HashMap<>();
    /**
     * * Company's Identif ier: @company
     */
    //    private final String role = "Seller";
    private String company = "";
    /**
     * Price range between:
     * min = displacement cost + 50% displacement cost
     * max = displacement cost + 200% displacement cost
     */
    private Proposal proposal;
    private double maximumPrice;
    private double minimumPrice;
    /**
     * Max availability is the availability read from DB
     * The availaibility sent in the proposal may not be equal to max, to
     * ensure that availability is a negotiable parameter
     */
    private long scheduledDeparture;
    private long delay;
    private long tripTime;
    private long maxAvailability;
    private ArrayList<AID> participants = new ArrayList<>();
    private File dataFile;
    private Data dataSet = null;
    private FileManager fm = new FileManager();
    private CBR cbr;
    private boolean recencyOutcome = false;
    private Loader db;
    private String tail_number;
    private double atc_avg_cost_nautical_mile;
    private double maintenance_avg_cost_minute;
    private double fuel_avg_cost_minute;
    private double airport_handling_cost;
    private int nCPT;
    private int nOPT;
    private int nSCB;
    private int nCCB;
    private int nCAB;
    private int minCapacity;
    private String fleet;
    private double crewMemberId;
    private int seniority;
    private String rank;
    private String status;
    private int hourly_salary;
    private String origin;
    private String destination;

    @Override
    protected void setup() {
        // Build the description used as template for the subscription
        createLogger();
        createDataBaseFile(getLocalName() + " Database File ");
        DFAgentDescription dfd;
        ServiceDescription sd = new ServiceDescription();
        //sd.setType(role);
        sd.setName(getLocalName());
        // sd.addProperties(new Property("country", "Italy"));
        dfd = dfs.register(this);
        participants = dfs.searchDF(this);

        logger.log(Level.INFO, "Agent {0} waiting for CFP...  ", getLocalName());
        logger.log(Level.INFO, "Round n(before receiving CFP> {0})", round);
        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));


        addBehaviour(new SSResponderDispatcher(this, template) {
            @Override
            protected Behaviour createResponder(ACLMessage aclMessage) {
                addBehaviour(new SSIteratedContractNetResponder(this.getAgent(), aclMessage) {
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
                        logger.log(Level.INFO, "Agent {0}: CFP received from {1}. Action is {2} ", new Object[]{getLocalName(), cfp.getSender().getLocalName(), cfp.getContent()});
                        logger.log(Level.INFO, "Round n(upon receiving CFP: {0})", round);
                        Proposal rejectedProposal = null;
                        /**
                         * First Round means that sellers need to fetch for available resources in the DataBase
                         * The first round is identif ied by having both, Resource's Queue and Negotiation Historic HashMap, empty
                         */
                        if (flightsQueue == null && negotiationHistoric.isEmpty()) {
                            handleFirstCFP(cfp);
                            logger.log(Level.INFO, "proposal toString = {0}", proposal.toString());
                        } else {
                            /**
                             * Comments received in Reject, may be applied to the proposal, price and availability may be updated
                             */
                            //Checks if historic has more that one entry. If so, an evaluation to last proposal can be made
                            System.out.println("REJECTED PROPOSAL = NULL");
                            try {
                                rejectedProposal = (Proposal) cfp.getContentObject();
                                System.out.println("rejectedProposal.toString() " + rejectedProposal.toString());
                            } catch (UnreadableException e) {
                                logger.log(Level.SEVERE, "Could not get the message's content {0}", e);
                            }
                            //Add proposal to historic
                            System.out.println("Rejected proposal get comments " );
                            rejectedProposal.printComments();
                            System.out.println();
                            negotiationHistoric.put(round - 1, rejectedProposal);
                            proposal = applyCommentsToProposal(rejectedProposal);
                            logger.log(Level.INFO, "proposal toString = {0}", rejectedProposal.toString());
                        }
                        if (!flightsQueue.isEmpty()) {
                            ACLMessage propose = cfp.createReply();
                            propose.setPerformative(ACLMessage.PROPOSE);
                            try {
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
                        Flight queueHead;
                        Flight askedFlight = getMsgResources(cfp);
                        logger.log(Level.INFO, "Agent {0}: Searching for resources to lease ", getLocalName());
                        logger.log(Level.INFO, "Agent {0}: Proposing", getLocalName());
                        //Fetch funtion, writes to an ArrayList the resources available
                        solutions = getAvailableMatchingResources(askedFlight);
                        System.out.println("Solutions  "  + solutions);
                        putResourcesIntoQueue(solutions);
                        queueHead = flightsQueue.peek();
                        double displacementCosts = sumResourcesPrice(queueHead);
                        minimumPrice = displacementCosts + (displacementCosts * 0.5);
                        maximumPrice = displacementCosts + (displacementCosts * 2);
                        long worstAvailability = getWorstAvailability(queueHead);
                        proposal = new Proposal(maximumPrice, worstAvailability, this.getAgent().getAID());
                        negotiationHistoric.put(round, proposal);

                        return proposal;
                    }

                    @Override
                    protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                        logger.log(Level.INFO, "Agent {0}: Proposal Rejected", getLocalName());
                        resetNegotiationStructure();
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
                return null;
            }
        });
    }

    private ArrayList<String> createCase(Proposal rejectedProposal) {
        // PRICE MUST BE IN FUNCTION OF AVAILABILITY
        ArrayList<String> newCase = new ArrayList<>();
        newCase.add(rejectedProposal.getPriceComment());
        newCase.add(rejectedProposal.getAvailabilityComment());
        newCase.add(String.valueOf(participants.size()));

        //Seller is available to lower the price
        if (rejectedProposal.getPrice() > minimumPrice){
            newCase.add(LOWER);
        }
        //Seller isn't available to lower the price
        else if(rejectedProposal.getPrice() == minimumPrice){
            newCase.add(OK);
        }
        else
            newCase.add(rejectedProposal.getPriceComment());

        if (proposal.getAvailability() < rejectedProposal.getAvailability()){
            newCase.add(LOWER);
        }
        else if (proposal.getAvailability() == rejectedProposal.getAvailability()){
            newCase.add(OK);
        }
        else
            newCase.add(rejectedProposal.getAvailabilityComment());

        newCase.add("-1");

        System.out.println("NEW CASE COM CQUALQWEW WNMERRSDM A MENOS " + newCase);
        return newCase;
    }

    private double evaluateOutcome(Proposal proposalToEvaluate, Proposal rejectedProposal) {
        double priceCommentToEvaluate = commentToInt(proposalToEvaluate.getPriceComment());
        double availabilityCommentToEvaluate = commentToInt(proposalToEvaluate.getAvailabilityComment());
        double rejectedPriceComment = commentToInt(rejectedProposal.getPriceComment());
        double rejectedAvailabilityComment = commentToInt(rejectedProposal.getAvailabilityComment());
        double priceEvaluation = priceCommentToEvaluate - rejectedPriceComment;
        double availabilityEvaluation = availabilityCommentToEvaluate - rejectedAvailabilityComment;
        double result = 0;
        if (priceEvaluation > 0)
            result += 0.5;
        if (availabilityEvaluation > 0)
            result += 0.5;
        return result;
    }

    private int commentToInt(String comment) {
        switch (comment){
            case "OK":
                return 0;
            case "LOWER":
                return 1;
            case "MUCH LOWER":
                return 2;
        }
        return -1;
    }

    private void createDataBaseFile(String databaseName) {
        dataFile = new File(databaseName);
        dataSet = new Data();
        try {
            if (dataFile.createNewFile()){
                logger.log(Level.INFO, "Agent {0}: Database File created", getLocalName());
            }
            else {
                logger.log(Level.INFO, "Agent {0}: Database File already exists. Loading...", getLocalName());
                dataSet = fm.read(databaseName);
                System.out.println("DataSet = " + dataSet);
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Could not create or open the specif ied file", e);
        }
    }

    private void resetNegotiationStructure() {
        flightsQueue = null;
        negotiationHistoric = new HashMap<>();
        solutions = new ArrayList<>();
        round = 0;
    }

    private Proposal applyCommentsToProposal(Proposal rejectedProposal) {
        Flight queueHead = flightsQueue.poll();
//        double displacementCosts = sumResourcesPrice(queueHead);
        ArrayList<String> comments = findSimilarCases(rejectedProposal);
        System.out.println("Comments in Apply Comments to Proposal " + comments);
        String priceComment = comments.get(0);
        String availabilityComment = comments.get(1);
        ArrayList<String> action = new ArrayList<>();

        switch (priceComment){
            case OK:
                action.add(OK);
//                System.out.println("OK received on price. Nothing to change");
                break;
            case LOWER:
                action.add(LOWER);
//                System.out.println("LOWER received on price. Going to decrease price by 15%");
                rejectedProposal.setPrice(rejectedProposal.getPrice()*0.85);
//                System.out.println("LOWER: New price = " + rejectedProposal.getPrice()*0.85);
                break;
            case MUCH_LOWER:
                action.add(MUCH_LOWER);
//                System.out.println("MUCH LOWER received on price. Going to decrease price by 40%");
                rejectedProposal.setPrice(rejectedProposal.getPrice()*0.6);
//                System.out.println("LOWER: New price = " + rejectedProposal.getPrice()*0.6);
                break;
            default:
                break;
        }
        switch (availabilityComment){
            case OK:
                action.add(OK);
//                System.out.println("OK received on availability. Nothing to change");
                break;
            case LOWER:
                action.add(LOWER);
//                System.out.println("LOWER received on availability. Checking if it is possible to reduce");
                //Something to check this
                rejectedProposal.setAvailability((long) (rejectedProposal.getAvailability()*0.85));
//                System.out.println("LOWER: New availability = " + rejectedProposal.getAvailability()*0.85);
                break;
            case MUCH_LOWER:
                action.add(MUCH_LOWER);
//                System.out.println("MUCH LOWER received on availability. Checking if it is possible to reduce");
                //Something to check this
                rejectedProposal.setAvailability((long) (rejectedProposal.getAvailability()*0.6));
//                System.out.println("LOWER: New availability = " + rejectedProposal.getAvailability()*0.6);
                break;
            default:
                break;
        }
        // Aqui entra a aprendizagem dos agentes
        if (hasBetterUtility(rejectedProposal, queueHead)){
            flightsQueue.add(queueHead);
        }
        else{
            /**
             * TODO: something here
             */
        }
        proposal = new Proposal(rejectedProposal.getPrice(), rejectedProposal.getAvailability(),this.getAID());
        return proposal;
    }

    private ArrayList<String> findSimilarCases(Proposal rejectedProposal) {
        if (round > 1){
            Proposal proposalToEvaluate = negotiationHistoric.get(round - 1);
            System.out.println("negotiationHistoric.get(round - 1) " + negotiationHistoric.get(round - 1));
            System.out.println("Round " + round);
            double previousRoundCaseEvaluation = evaluateOutcome(proposalToEvaluate,rejectedProposal);
            int outcomeIndex = dataSet.addOutcome(previousRoundCaseEvaluation);
            System.out.println();
            ArrayList<String> newCase = createCase(rejectedProposal);
            ArrayList<String> action = new ArrayList<>();
            if(dataSet.getSize() > 1){
                ArrayList<Integer> similarCaseIndexes = dataSet.getEuclideanDistances(newCase);
                //Use simulated annealing to chose a case amongst the various cases
                //For now we will choose the case with the best evaluation
                int similarCaseIndex = dataSet.getBestEvaluation(similarCaseIndexes);
                System.out.println("SIMILAR CASE INDEX = " + similarCaseIndex);

                if(similarCaseIndex > 0 ){System.out.println("euclideanDistance = " + dataSet.getLine(similarCaseIndex));

//                similarCaseIndex++;
                    if(recencyOutcome){
                        //recency on outcome
                        Proposal previousProposal = negotiationHistoric.get(round - 1);
                        Proposal beforePreviousProposal = negotiationHistoric.get(round - 2);
                        double recencyEvaluation = evaluateOutcome(previousProposal,beforePreviousProposal);
                        double xpto = dataSet.recencyOutcome(recencyEvaluation, similarCaseIndex);
                        System.out.println("XPTO = " + xpto);
                        recencyOutcome = false;
                    }
                    System.out.println("euclideanDistante Index = " + similarCaseIndexes);
                    System.out.println("similarCaseIndex = " + similarCaseIndex);
                    System.out.println("euclideanDistante = " + dataSet.getLine(similarCaseIndex));
                    System.out.println("Action of euclideanDistante " + dataSet.getAction(similarCaseIndex));
                    System.out.println("Outcome of euclideanDistante " + dataSet.getOutcome(similarCaseIndex));
                    //The case exists in the Data Base
                    if(dataSet.getEuclideanDistance(similarCaseIndex) == 0.0){
                        System.out.println("Encontrei um caso igual");
                        double evaluation = Double.parseDouble(dataSet.getOutcome(similarCaseIndex));
                        System.out.println("Outcome of euclideanDistante using evaluation variable " + evaluation);
                        //<= 0.5 to test, this condition must be >= 0.5, that is, when the outcome is positive
                        System.out.println("O caso que encontrei tem avaliacao positiva");
                        recencyOutcome = true;
                        //Previous experiences show an improvement so the same action is going to be applied
                        action = dataSet.getNFeatures(2, similarCaseIndex);
                        System.out.println("Comments quando a distancia e 0 = " + action);

                        /**
                         * TODO: Else, agent must decide what to do, considering the fact that there are no similar previous experiences, add new case if there are no similar cases to the decision made
                         */

                    }

                }
                //Distance != 0. Does the same as the following, it just adds the case to the dataset
                else {
                    //As there are no similar cases, current case is added to the dataSet
                    System.out.println("Estou a a adiconar um caso porque não existe caso igual");
                    System.out.println("Adicionar novo caso, quando nao ha casos iguais. Caso: " + newCase);
                    dataSet.addCase(newCase);
                    action.add(rejectedProposal.getPriceComment());
                    action.add(rejectedProposal.getAvailabilityComment());
                    System.out.println("Comments quando a distancia nao e 0 = " + action);
                }
            }
            else {
                //As there are no similar cases, current case is added to the dataSet
                System.out.println("Estou a a adiconar um caso porque não existe caso  no dataSet");
                System.out.println("Adicionar novo caso, quando nao ha casos no dataSet. Caso: " + newCase);
                dataSet.addCase(newCase);
                action.add(rejectedProposal.getPriceComment());
                action.add(rejectedProposal.getAvailabilityComment());
                System.out.println("Comments quando o dataSet nao tem mais do que o cabeçalho = " + action);
            }
            try {
                fm.save(dataSet,getLocalName() + " Database File ");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Could not save to Database File {0}", e);
            }

            if(action.isEmpty()) {
                action.add(rejectedProposal.getPriceComment());
                action.add(rejectedProposal.getAvailabilityComment());
            }
            return action;
        }
        return null;
    }

    private boolean hasBetterUtility(Proposal rejectedProposal, Flight peek) {
        return (utilityCalculation(rejectedProposal.getPrice()) > (utilityCalculation(sumResourcesPrice(peek))));
    }

    private void updateRound() {
        round++;
    }

    private void createLogger() {
        logger.setLevel(Level.FINE);
        // create an instance of Log at the top of the file, as you would do with log4j
        /*FileHandler fh = null;   // true forces append mode
        try {
            String logName = this.getLocalName() + " logFile.log";
            fh = new FileHandler(logName, false);
            SimpleFormatter sf = new SimpleFormatter();
            fh.setFormatter(sf);
            logger.addHandler(fh);

        } catch (IOException e) {
            logger.log(Level.SEVERE,"Could not create Log File {0} " , e);
        }*/
    }

    protected double sumResourcesPrice(Flight flightToBeProposed){
        double sum = 0;
        //deprecated method
        flightToBeProposed.setPrice(1234.12);
        sum += flightToBeProposed.getPrice();
        return sum;
    }

    /**
     * getAvailableMatchingResources method fetches Resources in DataBase
     * Builds an ArrayList<Resource> for each query
     * Combines the ArrayLists obtained from the queries
     * Pushes the result to the queue, by utility, in decreasing order,
     * using the Utility Comparator
     * @param askedFlight: Resources received in ACL Message
     */
    private ArrayList<Flight> getAvailableMatchingResources(Flight askedFlight) {
        ArrayList<Flight> availableResources = new ArrayList<>();
//        ArrayList<ArrayList<Resource>> allResourcesCombinations = new ArrayList<>();
/*        Flight f1 = new Flight();
        Flight f2 = new Flight();
        Aircraft a1 = new Aircraft(396);
        a1.setTail_number("ASD");
        a1.setAvailability(0L);
        f1.setAircraft(a1);
        Aircraft a2 = new Aircraft(396);
        a2.setTail_number("ASD");
        a2.setAvailability(0L);
        f2.setAircraft(a2);
        System.out.println("a1.equals(a2);" +a1.equals(a2));
//        f1.setAircraft(a2);
        CrewMember cm1 = new CrewMember();
        cm1.setAvailability(129000L);
        CrewMember cm2 = new CrewMember();
        CrewMember cm3 = new CrewMember();
        CrewMember cm4 = new CrewMember();
        cm1.setCrewMemberId(123.12);
        cm2.setCrewMemberId(123.11);
        cm3.setCrewMemberId(123.12);
        cm4.setCrewMemberId(123.11);

        System.out.println("cm1.equals(cm2);" +cm1.equals(cm2));

        System.out.println("Available resources "+ f1);
        f1.printFlight();
        availableResources.add(f1);
        ArrayList<CrewMember> cms1 = new ArrayList<>();
        ArrayList<CrewMember> cms2 = new ArrayList<>();
        cms1.add(cm1);
        cms2.add(cm3);
        cms2.add(cm2);
        cms1.add(cm4);
        f1.setCrewMembers(cms1);
        f2.setCrewMembers(cms2);

        boolean commonList = f1.equals(f2);
        System.out.println("Equal Flights? " + commonList);*/
        db = new Loader();
        db.establishConnection();
        availableResources = findSimilarResources(askedFlight);
//        convert ResultSet to ArrayList?

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
         * and if that dif ference is < 0,
         * compare it with delay (delay - that dif ference, because both are negative).
         * if >0 means that the resource isnt good -1 - (-3) 2 => -3 is later than -1
         * Ideal scenario, dif ference = delay
         * Otherwise, the more negative the better.
         */

        /*
        Resource r2  = new Aircraft("Boeing 777", 396);
        r2.setPrice(23543.23D);
        r2.setAvailability(10000L);
        Resource r3 = new Aircraft("Boeing 767", 400);;
        r3.setPrice(31423.89D);
        r3.setAvailability(0L);
        Resource r4 = new Aircraft("Airbus A330-200 Freighter", 407);
        r4.setPrice(451123.51D);
        r4.setAvailability(9000L);

        Resource r6 = new CrewMember(2, "Pilot", "English A2");
        r6.setPrice(65333.21D);
        r6.setAvailability(0L);
        switch (this.getLocalName()){
            case "Seller1":
                availableResources.add(r1);
                availableResources.add(r5);
                break;
            case "Seller2":
                availableResources.add(r3);
                availableResources.add(r6);
                break;
            default:
                break;
        }*/
        logger.log(Level.INFO,"Available Resources {0}", availableResources);
        //The query returns resources within the parameters, so there's no reason to compare
/*

        for (int i = 0; i < availableResources.size(); i++) {
            for (int j = i; j < availableResources.size(); j++) {
                if (availableResources.get(i).getClass() != availableResources.get(j).getClass()) {
                    ArrayList<Resource> combination = new ArrayList<>();
                    combination.ensureCapacity(askedFlight.size());
                    combination.add(availableResources.get(i));
                    combination.add(availableResources.get(j));
                    allResourcesCombinations.add(combination);
                    continue;
                }
            }
        }
        return allResourcesCombinations;*/
        return availableResources;
    }

    private ArrayList<Flight> findSimilarResources(Flight askedFlight) {
        String aircraftQuery = "";
        String crewQuery = "";
        String distanceQuery = "";
        Boolean isAircraftNeeded = false;
        ArrayList<Flight> matchingResources = new ArrayList<>();
        if (askedFlight.getAircraft() != null){
            System.out.println("askedFlight.getFleet() " + askedFlight.getFleet());
            //prepare query for aircraft
            /*aircraftQuery = "SELECT \uFEFFtail_number, atc_avg_cost_nautical_mile,\n" +
                    "maintenance_avg_cost_minute, fuel_avg_cost_minute,\n" +
                    "airport_handling_cost, capacity, fleet,\n" +
                    "cpt,opt,scb,ccb,cab\n " +
                    "FROM `thesis`.`aircraft`\n" +
                    "WHERE capacity >= "+ askedFlight.getAircraft().getCapacity() + "\n" +
                    "AND fleet = '"+ askedFlight.getFleet()+"';\n";
            crewQuery = "SELECT \uFEFFcrew_member_number, rank, seniority, hourly_salary, status\n" +
                    "FROM `thesis`.`crew_member`\n" +
                    "WHERE fleet LIKE '"+ askedFlight.getFleet() +"'\n" +
                    "AND status NOT LIKE 'PRG'" +
                    "AND status NOT LIKE 'REF';";*/
            aircraftQuery = "SELECT \uFEFFtail_number, atc_avg_cost_nautical_mile,\n" +
                    "maintenance_avg_cost_minute, fuel_avg_cost_minute,\n" +
                    "airport_handling_cost, capacity, fleet,\n" +
                    "cpt,opt,scb,ccb,cab\n " +
                    "FROM `test`.`aircraft`\n" +
                    "WHERE capacity >= "+ askedFlight.getAircraft().getCapacity() + "\n" +
                    "AND fleet = '"+ askedFlight.getFleet()+"';\n";
            crewQuery = "SELECT \uFEFFcrew_member_number, rank, seniority, hourly_salary, status\n" +
                    "FROM `test`.`crew_member`\n" +
                    "WHERE fleet LIKE '"+ askedFlight.getFleet() +"'\n" +
                    "AND status NOT LIKE 'PRG'" +
                    "AND status NOT LIKE 'REF'" +
                    "ORDER BY hourly_salary DESC, seniority ASC;";
            distanceQuery = "SELECT distance_in_nautical_miles\n" +
                    "FROM `test`.`distances`\n" +
                    "WHERE \uFEFForigin LIKE '"+ origin +"'\n" +
                    "AND destination LIKE '"+ destination +"';";
            isAircraftNeeded = true;
        }
        else if(!askedFlight.getCrewMembers().isEmpty()){
            //prepare query for crew members
        }
        ArrayList<Aircraft> availableAircraft = new ArrayList<>();
        // 0 - CPT , 1 - OPT, 2 - SCB, 3 - CCB, 4 - CAB
        ArrayList<ArrayList<CrewMember>> availableCrew = new ArrayList<>();
        ArrayList<CrewMember> cpts = new ArrayList<>();
        ArrayList<CrewMember> opts = new ArrayList<>();
        ArrayList<CrewMember> scbs = new ArrayList<>();
        ArrayList<CrewMember> ccbs = new ArrayList<>();
        ArrayList<CrewMember> cabs = new ArrayList<>();
        availableCrew.add(cpts);
        availableCrew.add(opts);
        availableCrew.add(scbs);
        availableCrew.add(ccbs);
        availableCrew.add(cabs);
        ResultSet rs = db.fetchDataBase(aircraftQuery);
        if (rs != null) {
            ResultSetMetaData rsmd;
            try {
                rsmd = rs.getMetaData();
                while (rs.next()) {
                    if (isAircraftNeeded) {
                        //remove isAircraftNeeded from this method and create 2 different methods to extract data from query result
                        //Extract Aircraft Data
                        extractAircraftQueryData(rs, rsmd, rsmd.getColumnCount(),availableAircraft);
//                      addFlightToFlightsQueue(disruptedFlight,resourceType);
                        //Extract crew Data
                        //Match aircraft with crew
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        ResultSet rsDistance = db.fetchDataBase(distanceQuery);
        double distance = 0;
        if (rsDistance != null) {
            ResultSetMetaData rsmdDistance;
            try {
                rsmdDistance = rsDistance.getMetaData();
                while (rsDistance.next()) {
                    //Extract crew Data
                    distance = extractDistanceQueryData(rsDistance, rsmdDistance, rsmdDistance.getColumnCount());
                    //Match aircraft with crew
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            //Se available aircraft estiver vazio, tem de sair da negociação porque não tem os recursos necessários
        }
        ResultSet rsCrew = db.fetchDataBase(crewQuery);
        if (rsCrew != null) {
            ResultSetMetaData rsmdCrew;
            try {
                rsmdCrew = rsCrew.getMetaData();
                while (rsCrew.next()) {
                    //Extract crew Data
                    extractCrewQueryData(rsCrew, rsmdCrew, rsmdCrew.getColumnCount(), availableCrew);
                    //Match aircraft with crew
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        getBestFlights(availableCrew,availableAircraft.get(0),matchingResources,distance);
        System.out.println("matching resources  = " + matchingResources);
        for (Flight f1: matchingResources) {
            f1.printFlight();
            System.out.println("\n");
        }
        return matchingResources;
    }
        return null;
}

    private double extractDistanceQueryData(ResultSet rsDistance, ResultSetMetaData rsmdDistance, int columnCount) throws SQLException {
        double distance = Double.parseDouble(rsDistance.getString(1).replace(",","."));
        System.out.println("Distance read = " + distance);
        return distance;
    }

    public ArrayList<Flight> getBestFlights(ArrayList<ArrayList<CrewMember>> allCrewList, Aircraft availableAircraft, ArrayList<Flight> bestThreeFlights, double distance){
//        while(bestThreeFlights.size() < 3){
        if (bestThreeFlights.isEmpty()){
            System.out.println("Add flight to the flights list");
            ArrayList<CrewMember> oneCombination = combineCrewMembers(allCrewList, availableAircraft, 0);
            Flight f = new Flight();
            f.setAircraft(availableAircraft);
            f.setCrewMembers(oneCombination);
            f.setFleet(fleet);
            f.setTripTime(tripTime);
            double flightTotalCost = getFlightTotalCost(f, distance);
            System.out.println("Flight total cost " + flightTotalCost);
            for (int i = 0; i < f.getCrewMembers().size();i++){
                System.out.println("Salario por hora do " + f.getCrewMembers().get(i).getCategory() + " e " + f.getCrewMembers().get(i).getHourly_salary());
            }
            f.setPrice(flightTotalCost);
            bestThreeFlights.add(f);

            //add flight because there isn't any
        }
        boolean existsEqualFlight = false;
        if (!bestThreeFlights.isEmpty()){
            if (bestThreeFlights.size() < 3){
                for (int i = 0; i < 3; i++){
                    ArrayList<CrewMember> oneCombination = combineCrewMembers(allCrewList, availableAircraft, i);
                    Flight f = new Flight();
                    f.setAircraft(availableAircraft);
                    f.setCrewMembers(oneCombination);
                    f.getAircraft().printResource();
                    for (Flight flight:bestThreeFlights) {
                        if (flight.equals(f))
                            existsEqualFlight = true;
                    }
                    if (existsEqualFlight){
                        System.out.println("Flight not added");
                        existsEqualFlight = false;
                        continue;
                    }
                    else{
                        f.setFleet(fleet);
                        f.printFlight();
                        System.out.println("Flight added");
                        bestThreeFlights.add(f);
                    }
                }
            }
            else
                return bestThreeFlights;
        }
        return bestThreeFlights;
    }

    private double getFlightTotalCost(Flight flight, double distance) {
        double tripTimeInMinutes = (double)tripTime/60000;
        double tripTimeInHours = (double)tripTime/3600000;
        double aircraftCost = flight.getAircraft().getCostNauticalMile() * distance +
                flight.getAircraft().getMaintenanceCost() * tripTimeInMinutes +
                flight.getAircraft().getFuelCost() * tripTimeInMinutes +
                flight.getAircraft().getAirportHandlingCost();
        System.out.println("flight.getAircraft().getMaintenanceCost() " + flight.getAircraft().getMaintenanceCost() * tripTimeInMinutes);
        System.out.println("Aircraft total cost " + aircraftCost);
        double crewCost = 0;
        for (int i = 0; i < flight.getCrewMembers().size();i++){
            System.out.println("Baby steps " + (flight.getCrewMembers().get(i).getHourly_salary()*tripTimeInHours));
            System.out.println("tripTimeInHours " + tripTimeInHours);
            crewCost += flight.getCrewMembers().get(i).getHourly_salary()*tripTimeInHours;
            System.out.println("Crew step cost " + crewCost);
        }
        System.out.println("Crew total cost " + crewCost);
        return (aircraftCost + crewCost);
    }

    private ArrayList<CrewMember> combineCrewMembers(ArrayList<ArrayList<CrewMember>> allCrewList, Aircraft availableAircraft, int index) {
        ArrayList<CrewMember> combination = new ArrayList<>();
        int cptsNeeded = availableAircraft.getCptNumber();
        int optsNeeded = availableAircraft.getOptNumber();
        int scbsNeeded = availableAircraft.getScbNumber();
        int ccbsNeeded = availableAircraft.getCcbNumber();
        int cabsNeeded = availableAircraft.getCabNumber();
        boolean atTheEnd = false;
        for (int i = 0; i < allCrewList.size(); i++){
            for (int j = index; j < allCrewList.get(i).size(); j++){
                if (atTheEnd){
                    j = 0;
                    atTheEnd = false;
                }
                if (i == 0 && cptsNeeded > 0){
                    combination.add(allCrewList.get(i).get(j));
                    cptsNeeded-=1;
                    if(j == allCrewList.get(i).size()-1 && cptsNeeded != 0){
                        j = 0;
                        atTheEnd = true;
                        continue;
                    }
                }
                if (i == 1 && optsNeeded > 0){
                    combination.add(allCrewList.get(i).get(j));
                    optsNeeded-=1;
                    if(j == allCrewList.get(i).size()-1 && optsNeeded != 0){
                        j = 0;
                        atTheEnd = true;
                        continue;
                    }
                }
                if (i == 2 && scbsNeeded > 0){
                    combination.add(allCrewList.get(i).get(j));
                    scbsNeeded-=1;
                    if(j == allCrewList.get(i).size()-1 && scbsNeeded != 0){
                        j = 0;
                        atTheEnd = true;
                        continue;
                    }
                }
                if (i == 3 && ccbsNeeded > 0){
                    combination.add(allCrewList.get(i).get(j));
                    ccbsNeeded-=1;
                    if(j == allCrewList.get(i).size()-1 && ccbsNeeded != 0){
                        j = 0;
                        atTheEnd = true;
                        continue;
                    }
                }
                if (i == 4 && cabsNeeded > 0){
                    combination.add(allCrewList.get(i).get(j));
                    cabsNeeded-=1;
                    if(j == allCrewList.get(i).size()-1 && cabsNeeded != 0){
                        j = 0;
                        atTheEnd = true;
                        continue;
                    }
                }
            }
        }
        if (cptsNeeded != 0 || optsNeeded != 0 || scbsNeeded != 0 || ccbsNeeded != 0 || cabsNeeded != 0)
            System.out.println("Esta a falhar qualquer coisa");
        return combination;
    }

    private ArrayList<ArrayList <CrewMember>> extractCrewQueryData(ResultSet rsCrew, ResultSetMetaData rsmdCrew, int columnCount, ArrayList<ArrayList<CrewMember>> availableCrew) throws SQLException {
        for (int i = 1; i <= columnCount; i++) {
            queryToVariables(rsCrew, rsmdCrew, i, false);
        }
        CrewMember cm = new CrewMember();
        cm.setCrewMemberId(crewMemberId);
        cm.setCategory(rank);
        cm.setStatus(status);
        cm.setSeniority(seniority);
        cm.setHourly_salary(hourly_salary);
        cm.printResource();
        addCrewMemberToList(availableCrew, cm);
        System.out.println("\n\n\nAvailable Crew = " + availableCrew);
        System.out.println("CAB size " + availableCrew.get(3).size());
        return availableCrew;
    }

    private void addCrewMemberToList(ArrayList<ArrayList<CrewMember>> availableCrew, CrewMember cm) {
        if (rank.equalsIgnoreCase(CPT)) {
            availableCrew.get(0).add(cm);
        }
        else if (rank.equalsIgnoreCase(OPT)){
            availableCrew.get(1).add(cm);
        }
        else if (rank.equalsIgnoreCase(SCB)) {
            availableCrew.get(2).add(cm);
        }
        else if (rank.equalsIgnoreCase(CCB)){
            availableCrew.get(3).add(cm);
        }
        else if (rank.equalsIgnoreCase(CAB)){
            availableCrew.get(4).add(cm);
        }
        else
            System.out.println("Not a recognized rank");
    }


    private ArrayList<Aircraft> extractAircraftQueryData(ResultSet rs, ResultSetMetaData rsmd, int columnsNumber, ArrayList<Aircraft> availableAircraft) throws SQLException {
//        ArrayList<Aircraft> availableAircraft = new ArrayList<>();
        Aircraft matchingAircraft = null;
        for (int i = 1; i <= columnsNumber; i++) {
            queryToVariables(rs, rsmd, i, true);
//               Adiciona tudo
            System.out.println(rsmd.getColumnLabel(i) + " : " + rs.getString(i));
        }
        System.out.println("minCapacity " + minCapacity);
        matchingAircraft = new Aircraft(minCapacity);
        System.out.println("Tail num " + tail_number);
        matchingAircraft.setTail_number(tail_number);
        matchingAircraft.setCostNauticalMile(atc_avg_cost_nautical_mile);
        matchingAircraft.setMaintenanceCost(maintenance_avg_cost_minute);
        matchingAircraft.setFuelCost(fuel_avg_cost_minute);
        matchingAircraft.setAirportHandlingCost(airport_handling_cost);
        matchingAircraft.setCptNumber(nCPT);
        matchingAircraft.setOptNumber(nOPT);
        matchingAircraft.setScbNumber(nSCB);
        matchingAircraft.setCcbNumber(nCCB);
        matchingAircraft.setCabNumber(nCAB);
        availableAircraft.add(matchingAircraft);/*
        System.out.println("Available aircrafts " + availableAircraft + " With size " + availableAircraft.size());
        System.out.println("This aircraft needs " + matchingAircraft.getCptNumber() + " cpts");
        System.out.println("This aircraft needs " + matchingAircraft.getOptNumber() + " opts");
        System.out.println("This aircraft needs " + matchingAircraft.getCabNumber() + " cabs");*/

        return availableAircraft;
    }

    private void addCrewMembersToFlight(Flight flight) {
        addCrewMemberToFlight(flight, CPT);
        addCrewMemberToFlight(flight, OPT);
        addCrewMemberToFlight(flight, SCB);
        addCrewMemberToFlight(flight, CCB);
        addCrewMemberToFlight(flight, CAB);

        //Adicionar tambem o preço e(depois de os somar) e guardar numa variavel do buyer e nao do voo!!!!!
    }

    private void addCrewMemberToFlight(Flight flight, String resourceType) {
        if (CPT.equalsIgnoreCase(resourceType)){
            CrewMember cm = setCrewMemberVariables(flight, nCPT, CPT);
            if (cm != null) {
                System.out.println("Cm " + cm.getCategory());
                flight.addCrewMember(cm);
            }
        }
        if (OPT.equalsIgnoreCase(resourceType)){
            CrewMember cm = setCrewMemberVariables(flight, nOPT, OPT);
            if (cm != null)
                flight.addCrewMember(cm);
        }
        if (SCB.equalsIgnoreCase(resourceType)) {
            CrewMember cm = setCrewMemberVariables(flight, nSCB, SCB);
            if (cm != null)
                flight.addCrewMember(cm);
        }
        if (CCB.equalsIgnoreCase(resourceType)) {
            CrewMember cm = setCrewMemberVariables(flight, nCCB, CCB);
            if (cm != null)
                flight.addCrewMember(cm);
        }
        if (CAB.equalsIgnoreCase(resourceType)){
            CrewMember cm = setCrewMemberVariables(flight, nCAB, CAB);
            if (cm != null)
                flight.addCrewMember(cm);
        }
    }
    private CrewMember setCrewMemberVariables(Flight flight, int crewMemberNumber, String crewMemberCategory) {
        if (crewMemberNumber != 0) {
            CrewMember cm = new CrewMember();
            cm.setNumber(crewMemberNumber);
            cm.setCategory(crewMemberCategory);
            return cm;
        }
        return null;
    }

    private void queryToVariables(ResultSet rs, ResultSetMetaData rsmd, int index, boolean isAircraftNeeded) throws SQLException {
        if (isAircraftNeeded) {
            switch (rsmd.getColumnLabel(index)) {
                case "\uFEFFtail_number":
                    tail_number = rs.getString(index);
                    System.out.println("AFTER READ TAIL NUM " + tail_number);
                    break;
                case "atc_avg_cost_nautical_mile":
                    atc_avg_cost_nautical_mile = Double.parseDouble(rs.getString(index).replace(',', '.'));
                    break;
                case "maintenance_avg_cost_minute":
                    maintenance_avg_cost_minute = Double.parseDouble(rs.getString(index).replace(',', '.'));
                    break;
                case "fuel_avg_cost_minute":
                    fuel_avg_cost_minute = Double.parseDouble(rs.getString(index).replace(',', '.'));
                    break;
                case "airport_handling_cost":
                    airport_handling_cost = Double.parseDouble(rs.getString(index).replace(',', '.'));
                    break;
                case "capacity":
                    minCapacity = Integer.parseInt(rs.getString(index));
                    System.out.println("MinCapacity Read " + minCapacity);
                    break;
                case "fleet":
                    fleet = rs.getString(index);
                    break;
                case CPT:
                    nCPT = Integer.parseInt(rs.getString(index));
                    break;
                case OPT:
                    nOPT = Integer.parseInt(rs.getString(index));
                    break;
                case SCB:
                    nSCB = Integer.parseInt(rs.getString(index));
                    break;
                case CCB:
                    nCCB = Integer.parseInt(rs.getString(index));
                    break;
                case CAB:
                    nCAB = Integer.parseInt(rs.getString(index));
                    break;
                default:
                    break;
            }
        } else {
            switch (rsmd.getColumnLabel(index)) {
                case "\uFEFFcrew_member_number":
                    crewMemberId = Double.parseDouble(rs.getString(index));
                    break;
                case "seniority":
                    seniority = Integer.parseInt(rs.getString(index));
                    break;
                case "rank":
                    rank = rs.getString(index);
                    break;
                case "fleet":
                    fleet = rs.getString(index);
                    break;
                case "status":
                    status = rs.getString(index);
                    break;
                case "hourly_salary":
                    hourly_salary = Integer.parseInt(rs.getString(index));
                    break;
                default:
                    break;
            }
        }
    }

    //Return peek or element from queue
    private Flight putResourcesIntoQueue(ArrayList<Flight> solutions) {
        if (solutions.isEmpty()) {
            logger.log(Level.INFO,"Resources not available");
            //exits negotiation by sending refuse
        }
        else{
            int queueSize = solutions.size();
            System.out.println("Solutions " + solutions);
            Comparator<Flight>  comparator = new UtilityComparator();

            flightsQueue = new PriorityQueue<>(queueSize, comparator );
            for(int i = 0; i < solutions.size(); i++) {
                flightsQueue.add(solutions.get(i));
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
        return flightsQueue.peek();
    }

    private double utilityCalculation(double priceOffered) {
        return((priceOffered - minimumPrice)/(maximumPrice-minimumPrice));
    }

    /**
     * setProposalAvailability method, finds a proposal's worst (higher) availability,
     * among the resources contained in the proposal
     * @param flight: ArrayList with the resources to be negotiated
     * @return A Long value, corresponding to the worst availability among the resources to be negotiated
     */
    private long getWorstAvailability(Flight flight) {
        //deprecated? unless its implemented the availability in aircraft and crewmembers
        flight.getAircraft().setAvailability(0L);
        return flight.getAircraft().getAvailability();
    }

    /**
     * getMsgResources method converts the ACL message content
     * to Resource type
     * @param msg : ACL message to be procesed
     * @return A list of resources contained in msg
     */
    private Flight getMsgResources(ACLMessage msg) {
        Flight disruptedFlight;
        Proposal cfp = null;
        try {
            cfp = (Proposal) msg.getContentObject();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
        disruptedFlight = cfp.getFlight();
        scheduledDeparture = disruptedFlight.getScheduledDeparture();
        tripTime = disruptedFlight.getTripTime();
        delay = disruptedFlight.getDelay();
        origin = disruptedFlight.getOrigin();
        destination = disruptedFlight.getDestination();
        System.out.println("Origin " + origin);
        System.out.println("Destination " + destination);
        System.out.println("Departure " + scheduledDeparture);
        System.out.println("Delay " + delay);
        System.out.println("TripTimeMiille " + tripTime);
        if (!origin.equalsIgnoreCase("") && !destination.equalsIgnoreCase("")){

        }

        return disruptedFlight;
    }
}
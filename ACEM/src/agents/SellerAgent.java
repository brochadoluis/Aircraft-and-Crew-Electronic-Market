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
import java.util.concurrent.ThreadLocalRandom;
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
    ArrayList<Flight> flightsList = new ArrayList<>();
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
    private ArrayList<String> actionsToTake = new ArrayList<>();
    private String tail_number;
    private String fleet;
    private String rank;
    private String status;
    private String origin;
    private String destination;
    private double atc_avg_cost_nautical_mile;
    private double maintenance_avg_cost_minute;
    private double fuel_avg_cost_minute;
    private double airport_handling_cost;
    private double crewMemberId;
    private int nCPT;
    private int nOPT;
    private int nSCB;
    private int nCCB;
    private int nCAB;
    private int minCapacity;
    private int seniority;
    private int hourly_salary;
    private int cmAvailability;
    private int aircraftAvailability;
    private int flightUnderNegotiationIndex = 0;



    /**
     * QUando a utilidade do recrusos seguinte e melhor que a do atual, aumentar o index.
     * Se receber comentarios para descer disponibilidade, andar para tras na lista (decrementar o index)
     */

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
        participants = dfs.searchAllSellers(this);
//        participants.add(this.getAID());
        System.out.println("Participants after read = " + participants);

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
                        if (flightsList.isEmpty() && negotiationHistoric.isEmpty()) {
                            handleFirstCFP(cfp);
                            logger.log(Level.INFO, "proposal toString = {0}", proposal.toString());
                        } else {
                            /**
                             * Comments received in Reject, may be applied to the proposal, price and availability may be updated
                             */
                            //Checks if historic has more that one entry. If so, an evaluation to last proposal can be made
                            try {
                                rejectedProposal = (Proposal) cfp.getContentObject();
                                System.out.println("rejectedProposal.toString() " + rejectedProposal.toString());
                            } catch (UnreadableException e) {
                                logger.log(Level.SEVERE, "Could not get the message's content {0}", e);
                            }
                            //Add proposal to historic/*
                            /*System.out.println("Rejected proposal get comments " );
                            rejectedProposal.printComments();
                            System.out.println();*/
                            negotiationHistoric.put(round - 1, rejectedProposal);
                            proposal = applyCommentsToProposal(rejectedProposal);
                            logger.log(Level.INFO, "proposal toString = {0}", rejectedProposal.toString());
                        }
                        if (!flightsList.isEmpty()) {
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
                        Flight firstAvailableFlight;
                        Flight askedFlight = getMsgResources(cfp);
                        logger.log(Level.INFO, "Agent {0}: Searching for resources to lease ", getLocalName());
                        logger.log(Level.INFO, "Agent {0}: Proposing", getLocalName());
                        //Fetch function, writes to an ArrayList the resources available
                        flightsList = getAvailableMatchingResources(askedFlight);
                        System.out.println("flightsList  "  + flightsList);
                        orderResourcesList(flightsList);
                        /**
                         * TODO: Change here
                         */
                        firstAvailableFlight = flightsList.get(0);
                        double flightCost = firstAvailableFlight.getPrice();
                        System.out.println("Flight Cost  = " + flightCost);
                        minimumPrice = flightCost + (flightCost * 0.5);
                        maximumPrice = flightCost + (flightCost * 2);
                        setFlightAvailability(firstAvailableFlight);
                        proposal = new Proposal(maximumPrice, firstAvailableFlight.getAvailability(), this.getAgent().getAID());
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
                            System.out.println("Resources to be leased: ");
//                            flightsList.get(1).printFlight();
                            for (int i = 0; i < flightsList.size(); i++){
                                flightsList.get(i).printFlight();
                            }
                            System.out.println("TailNum is " + flightsList.get(flightUnderNegotiationIndex).getAircraft().getTail_number() + " Costing " + flightsList.get(flightUnderNegotiationIndex).getPrice() + " with "
                                    + flightsList.get(flightUnderNegotiationIndex).getAvailability() + " ms delay. Departure at: " + (scheduledDeparture +
                                    flightsList.get(flightUnderNegotiationIndex).getAvailability()) + ". This is the flight with index: " + flightUnderNegotiationIndex);
                            //update outcome of this/previous negotiation round
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

    /**
     * Add to case structure the resources under negotiation (aircraft + crew, full crew, N crew members
     * Receveis rejected proposal and action to be done and adds the strings to the ArrayList<String> newCase
     * @param rejectedProposal
     * @return
     */
    private ArrayList<String> createCase(Proposal rejectedProposal) {
        // PRICE MUST BE IN FUNCTION OF AVAILABILITY
        ArrayList<String> newCase = new ArrayList<>();
        newCase.add(rejectedProposal.getPriceComment());
        newCase.add(rejectedProposal.getAvailabilityComment());
        newCase.add(String.valueOf(participants.size()));
        System.out.println("participants = " + participants);

        //Seller is available to lower the price
        if (rejectedProposal.getPrice() > minimumPrice){
            newCase.add(LOWER);
        }
        //Seller isn't available to lower the price
        else if(Math.abs(rejectedProposal.getPrice() - minimumPrice) < 0.000000000001){
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
        flightsList = new ArrayList<>();
        round = 0;
    }

    private Proposal applyCommentsToProposal(Proposal rejectedProposal) {
        /**
         * TODO: change here
         */
        Flight currentFlightUnderNegotiation = flightsList.get(flightUnderNegotiationIndex);
        System.out.println("Fligh Under Negotiation = " + flightUnderNegotiationIndex);
//        double displacementCosts = sumResourcesPrice(queueHead);
        ArrayList<String> newCase = createCase(rejectedProposal);
        ArrayList<String> action = new ArrayList<>();
        ArrayList<String> comments = new ArrayList<>();// = findSimilarCases(rejectedProposal);
        ArrayList<Integer> similarCases = findSimilarCases(rejectedProposal);
        System.out.println("Similar Cases = " + similarCases);
        int similarCase;
        boolean foundEqualCase = false;
        if (similarCases.size() > 1){
            similarCase = simulatedAnnealing(similarCases);
            System.out.println("Similar Case Choosen is: " + similarCase);
            foundEqualCase = true;
        }
        else{
            similarCase = similarCases.get(0);
        }
//        similarCase = 3;
        switch (similarCase){
            case -1:
            case -2:
                //Distance != 0 => index = -1. Does the same as the following, it just adds the case to the dataset
                System.out.println("Dunno what to do");
                //As there are no similar cases, current case is added to the dataSet
                System.out.println("Estou a a adiconar um caso porque não existe caso igual");
                System.out.println("Adicionar novo caso, quando nao ha casos iguais. Caso: " + newCase);
                System.out.println("New case = " + newCase);
                dataSet.addCase(newCase);
                comments.add(rejectedProposal.getPriceComment());
                comments.add(rejectedProposal.getAvailabilityComment());
                /*action.add(rejectedProposal.getPriceComment());
                action.add(rejectedProposal.getAvailabilityComment());*/
                System.out.println("Comments quando a distancia nao e 0 = " + action);
                break;
            /*case -2:
                //As there are no cases, current case is added to the dataSet
                System.out.println("Estou a a adiconar um caso porque não existe caso  no dataSet");
                System.out.println("Adicionar novo caso, quando nao ha casos no dataSet. Caso: " + newCase);
                dataSet.addCase(newCase);
                comments.add(rejectedProposal.getPriceComment());
                comments.add(rejectedProposal.getAvailabilityComment());
                action.add(rejectedProposal.getPriceComment());
                action.add(rejectedProposal.getAvailabilityComment());
                System.out.println("Comments quando o dataSet nao tem mais do que o cabeçalho = " + action);
                break;*/
            default:
                comments = dataSet.getNFeatures(2,similarCase);
                action = dataSet.getAction(similarCase);
                break;
        }
        for (String s: comments) {
            System.out.println("Comment is " + s);
        }
        System.out.println("action is: " + action);
        if (!action.isEmpty()){

        }
        for (String s2: action) {
            System.out.println("Action found is " + s2);
        }
//        System.out.println("Comments in Apply Comments to Proposal " + comments);
        actionsToTake = new ArrayList<>();
        String priceComment = comments.get(0);
        String availabilityComment = comments.get(1);
        String priceAction = "";
        String availabilityAction = "";

        switch (priceComment){
            case OK:
                priceAction = OK;
//                System.out.println("OK received on price. Nothing to change");
                break;
            case LOWER:
                priceAction = LOWER;
//                System.out.println("LOWER received on price. Going to decrease price by 15%");
                flightUnderNegotiationIndex++;
//                System.out.println("LOWER: New price = " + rejectedProposal.getPrice()*0.85);
                break;
            case MUCH_LOWER:
                priceAction = MUCH_LOWER;
                flightUnderNegotiationIndex+=3;
//                System.out.println("MUCH LOWER received on price. Going to decrease price by 40%");
//                System.out.println("LOWER: New price = " + rejectedProposal.getPrice()*0.6);
                break;
            default:
                break;
        }
        switch (availabilityComment){
            case OK:
                availabilityAction = OK;
//                System.out.println("OK received on availability. Nothing to change");
                break;
            case LOWER:
                System.out.println("LOWER received on availability. Checking if it is possible to reduce");
                if (flightUnderNegotiationIndex > 1  && flightsList.size() > 1){
                    System.out.println("There are flights with better availability.");
                    flightUnderNegotiationIndex--;
                    //update price here?
                    availabilityAction = LOWER;
                }
                else {
                    availabilityAction = OK;
                }
                //Something to check this
//                rejectedProposal.setAvailability((long) (rejectedProposal.getAvailability()*0.85));
                System.out.println("LOWER: New availability = " + rejectedProposal.getAvailability()*0.85);
                break;
            case MUCH_LOWER:
                System.out.println("MUCH LOWER received on availability. Checking if it is possible to reduce");
                if (flightUnderNegotiationIndex > 3){
                    if (flightsList.size() > 3){

                    }
                }
                System.out.println("There are flights with better availability.");
                flightUnderNegotiationIndex-=3;
                availabilityAction = MUCH_LOWER;
                //update price here?
                //Something to check this
//                rejectedProposal.setAvailability((long) (rejectedProposal.getAvailability()*0.6));
                System.out.println("MUCH LOWER: New availability = " + rejectedProposal.getAvailability()*0.6);
                break;
            default:
                break;
        }
        //Change index according to the availability comment received
        // Aqui entra a aprendizagem dos agentes
        actionsToTake.add(OK);
        actionsToTake.add(LOWER);
        if (foundEqualCase){
            System.out.println("dataSet.checkIfActionExists(actionsToTake,similarCases) " + dataSet.checkIfActionExists(actionsToTake,similarCases));
            if (!dataSet.checkIfActionExists(actionsToTake,similarCases)){
                System.out.println(" Nao existe, bota adicionar mais um");
                ArrayList<String> updatedCase = new ArrayList<>();
                updatedCase.add(comments.get(0));
                updatedCase.add(comments.get(1));
                updatedCase.add(String.valueOf(participants.size()));
                updatedCase.add(actionsToTake.get(0));
                updatedCase.add(actionsToTake.get(1));
                updatedCase.add("-1");
                dataSet.addCase(updatedCase);
            }
        }
        //Add action to case, verify if action exists, if it doens't, add new case?
        System.out.println("Nunca piora a utilidade?");
        if (hasBetterUtility(rejectedProposal, currentFlightUnderNegotiation)){
            System.out.println("o indice do voo a ser negociado e: " + flightUnderNegotiationIndex);
            System.out.println("atualizei o index dos voos");
//            flightsQueue.add(queueHead);
            System.out.println("O outro recurso e melhor agora. Atualiza ai");
            proposal = new Proposal(rejectedProposal.getPrice(), rejectedProposal.getAvailability(),this.getAID());
        }
        else{

            System.out.println("EASTER EGGS");
            System.out.println("Flight is: " + flightUnderNegotiationIndex);
            System.out.println("Rejected Proposal is: ");
            flightUnderNegotiationIndex--;
            currentFlightUnderNegotiation = flightsList.get(flightUnderNegotiationIndex);
            rejectedProposal.printProposal();
            proposal = new Proposal(currentFlightUnderNegotiation.getPrice(), currentFlightUnderNegotiation.getAvailability(),this.getAID());
        }

        System.out.println("Comments.equals(Actions to take) " + comments.equals(actionsToTake));
        try {
            fm.save(dataSet,getLocalName() + " Database File ");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save to Database File {0}", e);
        }
        return proposal;
    }

    private int simulatedAnnealing(ArrayList<Integer> similarCases) {
        double temperature = 1000;
        double coolingRate = 0.003;
        int randomNum = ThreadLocalRandom.current().nextInt(0, similarCases.size());
        int currentSolution = similarCases.get(randomNum);
        int best = currentSolution;
        while (temperature > 1){
            int newSolution = currentSolution;
            double currentEnergy = Double.parseDouble(dataSet.getOutcome(currentSolution));
            int randomNum2 = ThreadLocalRandom.current().nextInt(0, similarCases.size());
            while (randomNum == randomNum2) {
                randomNum2 = ThreadLocalRandom.current().nextInt(0, similarCases.size());
            }
            double neighbourEnergy = Double.parseDouble(dataSet.getOutcome(similarCases.get(randomNum2)));

            // Decide if we should accept the neighbour
            if (acceptanceProbability(currentEnergy, neighbourEnergy, temperature) > Math.random()) {
                currentSolution = newSolution;
            }

            double bestEnergy = Double.parseDouble(dataSet.getOutcome(best));
            // Keep track of the best solution found
            if (currentEnergy < bestEnergy){
                best = currentSolution;
            }

            // Cool system
            temperature *= 1-coolingRate;
        }
        return best;
    }

    public double acceptanceProbability(double energy, double newEnergy, double temperature) {
        // If the new solution is better, accept it
        if (newEnergy < energy) {
            return 1.0;
        }
        // If the new solution is worse, calculate an acceptance probability
        return Math.exp((energy - newEnergy) / temperature);
    }

    private ArrayList<Integer> findSimilarCases(Proposal rejectedProposal) {
        if (round > 1){
            Proposal proposalToEvaluate = negotiationHistoric.get(round - 1);
            System.out.println("negotiationHistoric.get(round - 1) " + negotiationHistoric.get(round - 1));
            System.out.println("Round " + round);
            double previousRoundCaseEvaluation = evaluateOutcome(proposalToEvaluate,rejectedProposal);
            dataSet.addOutcome(previousRoundCaseEvaluation);
            System.out.println();
            ArrayList<String> newCase = createCase(rejectedProposal);
            ArrayList<String> action;
            if(dataSet.getSize() > 1){
                ArrayList<Integer> similarCaseIndexes = dataSet.getEuclideanDistances(newCase);
                for (Integer i: similarCaseIndexes) {
                    System.out.println("This case is: " + i);
                }
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
                        /**
                         * a action nao pode ser ir buscar ao caso. A action e em funcao do que decidiu fazer. Aqui vai buscar isso
                         * e vai ver se ja existe action igual. se sim, recencyOutcome, senao, faz novo caso.
                         *
                         */
                        action = dataSet.getNFeatures(2, similarCaseIndex);
                        System.out.println("Comments quando a distancia e 0 = " + action);
                        return similarCaseIndexes;
/*
                        for (String s: actionsToTake) {
                            System.out.println("Action to take is: " + s);
                        }
                        System.out.println("SAO IGUAIS>? " + dataSet.checkIfActionExists(actionsToTake,similarCaseIndexes));
                        action = actionsToTake;
*/
                        /**
                         * TODO: Else, agent must decide what to do, considering the fact that there are no similar previous experiences,
                         * add new case if there are no similar cases to the decision made
                         */
                    }
                }
                //Distance != 0 => index = -1. Does the same as the following, it just adds the case to the dataset
                else {
/*
                    System.out.println("Dunno what to do");
                    //As there are no similar cases, current case is added to the dataSet
                    System.out.println("Estou a a adiconar um caso porque não existe caso igual");
                    System.out.println("Adicionar novo caso, quando nao ha casos iguais. Caso: " + newCase);
                    dataSet.addCase(newCase);
                    action.add(rejectedProposal.getPriceComment());
                    action.add(rejectedProposal.getAvailabilityComment());
                    System.out.println("Comments quando a distancia nao e 0 = " + action);
*/
                    ArrayList<Integer> noneEquals = new ArrayList<>();
                    noneEquals.add(-1);
                    return noneEquals;
                }
            }
            else {
                //As there are no cases, current case is added to the dataSet
/*                System.out.println("Estou a a adiconar um caso porque não existe caso  no dataSet");
                System.out.println("Adicionar novo caso, quando nao ha casos no dataSet. Caso: " + newCase);
                dataSet.addCase(newCase);
                action.add(rejectedProposal.getPriceComment());
                action.add(rejectedProposal.getAvailabilityComment());
                System.out.println("Comments quando o dataSet nao tem mais do que o cabeçalho = " + action);*/
                ArrayList<Integer> noneExistent = new ArrayList<>();
                noneExistent.add(-2);
                return noneExistent;
            }
            /*if(action.isEmpty()) {
                action.add(rejectedProposal.getPriceComment());
                action.add(rejectedProposal.getAvailabilityComment());
            }*/
        }
        return new ArrayList<>();
    }

    private boolean hasBetterUtility(Proposal rejectedProposal, Flight peek) {
        System.out.println("utilityCalculation(rejectedProposal.getPrice()) > (utilityCalculation(sumResourcesPrice(peek))) " + (utilityCalculation(rejectedProposal.getPrice(),rejectedProposal.getAvailability()) > (utilityCalculation(peek.getPrice(),peek.getAvailability()))));
        return (utilityCalculation(rejectedProposal.getPrice(),rejectedProposal.getAvailability()) > (utilityCalculation(peek.getPrice(),peek.getAvailability())));
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
        ArrayList<Flight> availableResources;
        db = new Loader();
        db.establishConnection();
        availableResources = findSimilarResources(askedFlight);

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

        logger.log(Level.INFO,"Available Resources {0}", availableResources);
        //The query returns resources within the parameters, so there's no reason to compare

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
                    //order por disponibilidade Desc e  (100% e disponibilidade na hora)
            crewQuery = "SELECT \uFEFFcrew_member_number, rank, seniority, hourly_salary, `availability(%)`, status\n" +
                    "FROM `thesis`.`crew_member`\n" +
                    "WHERE fleet LIKE '"+ askedFlight.getFleet() +"'\n" +
                    "AND status NOT LIKE 'PRG'" +
                    "AND status NOT LIKE 'REF'" +
                    "ORDER BY hourly_salary DESC, `availability(%)` DESC, seniority ASC;";
            distanceQuery = "SELECT distance_in_nautical_miles\n" +
                    "FROM `thesis`.`distances`\n" +
                    "WHERE \uFEFForigin LIKE '"+ origin +"'\n" +
                    "AND destination LIKE '"+ destination +"';";*/
            aircraftQuery = "SELECT \uFEFFtail_number, `availability(%)`, atc_avg_cost_nautical_mile,\n" +
                    "maintenance_avg_cost_minute, fuel_avg_cost_minute,\n" +
                    "airport_handling_cost, capacity, fleet,\n" +
                    "cpt,opt,scb,ccb,cab\n " +
                    "FROM `test`.`aircraft`\n" +
                    "WHERE capacity >= "+ askedFlight.getAircraft().getCapacity() + "\n" +
                    "AND fleet = '"+ askedFlight.getFleet()+"'" +
                    "ORDER BY `availability(%)` DESC;";
            //order por preço primeiro e so depois disponibilidade Desc (100% e disponibilidade na hora)
            crewQuery = "SELECT \uFEFFcrew_member_number, rank, seniority, hourly_salary, `availability(%)`,status\n" +
                    "FROM `test`.`crew_member`\n" +
                    "WHERE fleet LIKE '"+ askedFlight.getFleet() +"'\n" +
                    "AND status NOT LIKE 'PRG'" +
                    "AND status NOT LIKE 'REF'" +
                    "ORDER BY hourly_salary DESC, `availability(%)` DESC, seniority ASC;";
            //order por disponibilidade Desc (100% e disponibilidade na hora) e so depois o salario e seniority
            distanceQuery = "SELECT distance_in_nautical_miles\n" +
                    "FROM `test`.`distances`\n" +
                    "WHERE \uFEFForigin LIKE '"+ origin +"'\n" +
                    "AND destination LIKE '"+ destination +"';";
            isAircraftNeeded = true;
        }
        else if(!askedFlight.getCrewMembers().isEmpty()){
            /**
             * TODO: Prepare query for crew members
             */
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
                while (rsDistance.next()) {
                    //Extract crew Data
                    distance = extractDistanceQueryData(rsDistance);
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
            getBestFlights(availableCrew,availableAircraft,matchingResources,distance);
            System.out.println("matching resources  = " + matchingResources);
            System.out.println("This means: \n");
            for (Flight f : matchingResources) {
                f.printFlight();
            }
            System.out.println("O primeiro voo tem: " + matchingResources.get(0).getAvailability() + " e custa  "+ matchingResources.get(0).getPrice());
            return matchingResources;
        }
        return null;
    }

    private double extractDistanceQueryData(ResultSet rsDistance) throws SQLException {
        return Double.parseDouble(rsDistance.getString(1).replace(",","."));
    }

    public ArrayList<Flight> getBestFlights(ArrayList<ArrayList<CrewMember>> allCrewList, ArrayList<Aircraft> availableAircraft, ArrayList<Flight> bestFlights, double distance){
        boolean existsEqualFlight = false;
        ArrayList<ArrayList<CrewMember>> allCombinations = combineAllCrewMembers(allCrewList, availableAircraft.get(0));
        for (int i = 0; i < availableAircraft.size(); i++){
            double flightTotalCost;
            if (allCombinations != null){
                for (ArrayList<CrewMember> cmList: allCombinations) {
                    Flight aFlight = new Flight();
                    aFlight.setAircraft(availableAircraft.get(i));
                    aFlight.setCrewMembers(cmList);
                    int crewMembersNum = aFlight.getAircraft().getCptNumber() +
                            aFlight.getAircraft().getOptNumber() +
                            aFlight.getAircraft().getScbNumber() +
                            aFlight.getAircraft().getCcbNumber() +
                            aFlight.getAircraft().getCabNumber();
                    //Se o numero de tripulantes for menor que a soma dos tripulantes necessarios,entao aborta
                    if (aFlight.getCrewMembers().isEmpty() || aFlight.getCrewMembers().size() < crewMembersNum){
                        System.out.println("Inviable option");
                        continue;
                    }
                    flightTotalCost = getFlightTotalCost(aFlight, distance);
                    aFlight.setPrice(flightTotalCost);
                    setFlightAvailability(aFlight);
                    if (!bestFlights.isEmpty()){
                        for (Flight flight : bestFlights) {
                            if (flight.equals(aFlight))
                                existsEqualFlight = true;
                        }
                        if (existsEqualFlight){
                            System.out.println("Flight not added");
                            existsEqualFlight = false;
                        }
                        else{
                            aFlight.setTripTime(tripTime);
                            aFlight.setFleet(fleet);
                            System.out.println("Flight added");
                            System.out.println("Aircraft ID " + availableAircraft.get(i).getTail_number());
                            bestFlights.add(aFlight);
                        }
                    } else {
                        aFlight.setFleet(fleet);
                        aFlight.setTripTime(tripTime);
                        aFlight.setCrewMembers(cmList);
                        flightTotalCost = getFlightTotalCost(aFlight, distance);
                        aFlight.setPrice(flightTotalCost);
                        bestFlights.add(aFlight);

                    }

                }
            }
            else
                break;
        }
        System.out.println(" What do we have? " + bestFlights.size());
        return bestFlights;
    }

    private double getFlightTotalCost(Flight flight, double distance) {
        double tripTimeInMinutes = (double)tripTime/60000;
        double tripTimeInHours = (double)tripTime/3600000;
        double aircraftCost = flight.getAircraft().getCostNauticalMile() * distance +
                flight.getAircraft().getMaintenanceCost() * tripTimeInMinutes +
                flight.getAircraft().getFuelCost() * tripTimeInMinutes +
                flight.getAircraft().getAirportHandlingCost();
//        System.out.println("flight.getAircraft().getMaintenanceCost() " + flight.getAircraft().getMaintenanceCost() * tripTimeInMinutes);
//        System.out.println("Aircraft total cost " + aircraftCost);
        double crewCost = 0;
        for (int i = 0; i < flight.getCrewMembers().size();i++){
//            System.out.println("Baby steps " + (flight.getCrewMembers().get(i).getHourly_salary()*tripTimeInHours));
//            System.out.println("tripTimeInHours " + tripTimeInHours);
            crewCost += flight.getCrewMembers().get(i).getHourly_salary()*tripTimeInHours;
//            System.out.println("Crew step cost " + crewCost);
        }
//        System.out.println("Crew total cost " + crewCost);
        return (aircraftCost + crewCost);
    }

    private ArrayList<ArrayList<CrewMember>> combineAllCrewMembers(ArrayList<ArrayList<CrewMember>> allCrewList, Aircraft availableAircraft) {
        ArrayList<ArrayList<CrewMember>> allCombinationsForAircraft;
        ArrayList<ArrayList<Integer>> differentCrewMembers = new ArrayList<>();
        ArrayList<ArrayList<Integer>> combinedCrews = new ArrayList<>();
        differentCrewMembers.ensureCapacity(allCrewList.size());
        for (int i = 0; i < allCrewList.size(); i++){
            differentCrewMembers.add(getDifferentCrewMembers(i, allCrewList));
        }
        System.out.println("\nDifferente crew members " + differentCrewMembers);
        //combinedCrews = combineDifferentIndexes(differentCrewMembers,allCrewList);
        permute(differentCrewMembers,0, new ArrayList<>(), combinedCrews);
        System.out.println("Combined Crew  " + combinedCrews);
        allCombinationsForAircraft = createCrews(availableAircraft,combinedCrews,allCrewList);
        return allCombinationsForAircraft;
    }

    private ArrayList<ArrayList<CrewMember>> createCrews(Aircraft availableAircraft, ArrayList<ArrayList<Integer>> combinedCrews, ArrayList<ArrayList<CrewMember>> allCrewList) {
        ArrayList<Integer> crewMembersNeeded = new ArrayList<>();
        crewMembersNeeded.add(availableAircraft.getCptNumber());
        crewMembersNeeded.add(availableAircraft.getOptNumber());
        crewMembersNeeded.add(availableAircraft.getScbNumber());
        crewMembersNeeded.add(availableAircraft.getCcbNumber());
        crewMembersNeeded.add(availableAircraft.getCabNumber());
        ArrayList<ArrayList<CrewMember>> allCrewsForAircraft = new ArrayList<>();
        ArrayList<ArrayList<Integer>> requiredCombinations = removeUnnecessaryCombinations(combinedCrews,crewMembersNeeded);
        System.out.println("Required comb " + requiredCombinations);
        for (int i = 0; i < requiredCombinations.size(); i++) {
            ArrayList<Integer> currentCrew = requiredCombinations.get(i);
            ArrayList<CrewMember> aCrew = addNecessaryCrewMembers(allCrewList, crewMembersNeeded, currentCrew);
            allCrewsForAircraft.add(aCrew);
        }
        return allCrewsForAircraft;
    }

    private ArrayList<ArrayList<Integer>> removeUnnecessaryCombinations(ArrayList<ArrayList<Integer>> combinedCrews, ArrayList<Integer> crewMembersNeeded) {
        ArrayList<ArrayList<Integer>> updatedCombinedCrews = new ArrayList<>();
        for (int i = 0; i < crewMembersNeeded.size(); i++){
            if (crewMembersNeeded.get(i) == 0){
                int value = combinedCrews.get(i).get(0);
                for (int j = 0; j < combinedCrews.size();j++){
                    if (combinedCrews.get(j).get(i) == value){
                        updatedCombinedCrews.add(combinedCrews.get(j));
                    }
                }
            }
        }
        return updatedCombinedCrews;
    }

    private ArrayList<CrewMember> addNecessaryCrewMembers(ArrayList<ArrayList<CrewMember>> allCrewList, ArrayList<Integer> crewMembersNeeded, ArrayList<Integer> currentCrew) {
        ArrayList<CrewMember> oneCrew = new ArrayList<>();
        for (int i = 0; i < currentCrew.size();i++){
            if (crewMembersNeeded.get(i) != 0){
                int index = currentCrew.get(i);
                CrewMember cmToBeAdded = allCrewList.get(i).get(index);
                oneCrew.add(cmToBeAdded);
                if (oneCrew.size() < crewMembersNeeded.get(i)){
                    index++;
                    while (oneCrew.size() < crewMembersNeeded.get(i)){
                        CrewMember anotherCmToBeAdded = allCrewList.get(i).get(index);
                        oneCrew.add(anotherCmToBeAdded);
                        index++;
                    }
                }
            }
        }
        return oneCrew;
    }

    //Refactor this method
    private ArrayList<ArrayList<Integer>> combineDifferentIndexes(ArrayList<ArrayList<Integer>> differentCrewMembers, ArrayList<ArrayList<CrewMember>> allCrewList) {
        ArrayList<ArrayList<Integer>> allCombinations = new ArrayList<>();
        for (int cptIndex = 0; cptIndex < differentCrewMembers.get(0).size(); cptIndex++){
            for (int optIndex = 0; optIndex < differentCrewMembers.get(1).size(); optIndex++){
                for (int scbIndex = 0; scbIndex < differentCrewMembers.get(2).size(); scbIndex++){
                    for (int ccbIndex = 0; ccbIndex < differentCrewMembers.get(3).size(); ccbIndex++){
                        for (int cabIndex = 0; cabIndex < differentCrewMembers.get(4).size(); cabIndex++){
                            ArrayList<Integer> combination = new ArrayList<>();
                            combination.add(differentCrewMembers.get(0).get(cptIndex));
                            combination.add(differentCrewMembers.get(1).get(optIndex));
                            combination.add(differentCrewMembers.get(2).get(scbIndex));
                            combination.add(differentCrewMembers.get(3).get(ccbIndex));
                            combination.add(differentCrewMembers.get(4).get(cabIndex));
                            allCombinations.add(combination);
                        }
                    }
                }
            }
        }
        return allCombinations;
    }

    public void permute(ArrayList<ArrayList<Integer>> differentCrewMembers, int index, ArrayList<Integer> output, ArrayList<ArrayList<Integer>> o2){
        if(index == differentCrewMembers.size()){
            o2.add((ArrayList<Integer>) output.clone());
        }
        else{
            for(int i=0 ; i < differentCrewMembers.get(index).size(); i++){
                output.add(differentCrewMembers.get(index).get(i));
                permute(differentCrewMembers,index+1,output,o2);
                output.remove(output.size() - 1);
            }
        }
    }

    private ArrayList<Integer> getDifferentCrewMembers(int rank, ArrayList<ArrayList<CrewMember>> allCrewList) {
        ArrayList<Integer> differentCrewMembers = new ArrayList<>();
        boolean alreadyExists = false;
        for(int i = 0; i < allCrewList.get(rank).size();i++){
            if (i != 0){
                for(int j = 0; j < differentCrewMembers.size(); j++){
                    //also consider availability when implemented
                    if (allCrewList.get(rank).get(i).equals(allCrewList.get(rank).get(differentCrewMembers.get(j))))
//                    if (allCrewList.get(rank).get(i).getHourly_salary() == allCrewList.get(rank).get(differentCrewMembers.get(j)).getHourly_salary())
                        alreadyExists = true;
                }
                if (!alreadyExists){
                    differentCrewMembers.add(i);
                }
                alreadyExists = false;
            }
            else {
                differentCrewMembers.add(i);
            }
        }
        return differentCrewMembers;
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

        double availabilityPercent = cmAvailability/(double) 100;
//        System.out.println("Scheduled Departure " + scheduledDeparture);
//        System.out.println("Scheduled Departure delayed " + (scheduledDeparture+delay));
        /*System.out.println("Availability (%) " + availabilityPercent );
        System.out.println("cm availability = " + (long)(delay*availabilityPercent));*/
        long cmAvailLong = (long) (/*scheduledDeparture+*/(delay*availabilityPercent));
//        System.out.println("cm availability long milli = " + (scheduledDeparture+cmAvailLong));
        /*System.out.println("Delay = " + (delay *//*+ scheduledDeparture*//* - cmAvailLong));
        System.out.println("Delay to minimize " + delay + "\nResouce Delay " + (scheduledDeparture-cmAvailLong));
        System.out.println("real availability (mili after sceduled departure) = " + (cmAvailLong-scheduledDeparture));
        System.out.println("New sceduled departure " + scheduledDeparture+cmAvailLong);*/
        cm.setAvailability(cmAvailLong);
        cm.printResource();
        addCrewMemberToList(availableCrew, cm);
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
//            System.out.println(rsmd.getColumnLabel(i) + " : " + rs.getString(i));
        }
        matchingAircraft = new Aircraft(minCapacity);
        matchingAircraft.setTail_number(tail_number);
        double availabilityPercent = aircraftAvailability/(double) 100;
        long aircraftAvailLong = (long) (/*scheduledDeparture+*/(delay*availabilityPercent));
        matchingAircraft.setAvailability(aircraftAvailLong);
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
                case "availability(%)":
                    aircraftAvailability = Integer.parseInt(rs.getString(index));
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
                case "availability(%)":
                    cmAvailability = Integer.parseInt(rs.getString(index));
                    break;
                default:
                    break;
            }
        }
    }

    //Return peek or element from queue
    private void orderResourcesList(ArrayList<Flight> flightsList) {
        if (flightsList.isEmpty()) {
            logger.log(Level.INFO,"Resources not available");
            //exits negotiation by sending refuse
        }
        else{
            Comparator<Flight>  comparator = new UtilityComparator();
            Collections.sort(flightsList, comparator);
//            flightsList.sort((f1,f2)-> f1.compare(f2));
            /*int queueSize = flightsList.size();
            System.out.println("flightsList " + flightsList);
            Comparator<Flight>  comparator = new UtilityComparator();

            flightsQueue = new PriorityQueue<>(queueSize, comparator );
*/            for(int i = 0; i < flightsList.size(); i++) {
                System.out.println("flightsList.get(i).getPrice()" + flightsList.get(i).getPrice());
            }
            /**
             * Calculates utility
             * Creates/Initializes priority queue with comparator
             * Comparator<Integer> comparator = new UtilityComparator();
             * PriorityQueue<ArrayList <Resources>> queue =
             * new PriorityQueue<ArrayList <Resources>>(flightsList.size(), comparator);
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
    }

    private double utilityCalculation(double priceOffered, long availabilityOffered) {
        /*System.out.println("Price Utility is : " + ((priceOffered - minimumPrice)/(maximumPrice-minimumPrice)));
        System.out.println("Minimum Price " + minimumPrice);
        System.out.println("Maximum Price " + maximumPrice);
        System.out.println("Availability Utility is : " + ((delay - availabilityOffered)));
        double priceUt = (priceOffered - minimumPrice)/(maximumPrice-minimumPrice);
        //given that the minimum availability is 0
        System.out.println("Delay is " + delay);
        System.out.println("Availability offered is  " + availabilityOffered);
        double  availabilityUt = (((delay - availabilityOffered))/60000);
        System.out.println("Utility is : " + (priceUt/availabilityUt));*/
        return ((priceOffered - minimumPrice)/(maximumPrice-minimumPrice));
    }

    /**
     * setProposalAvailability method, finds a proposal's worst (higher) availability,
     * among the resources contained in the proposal
     * @param flight: ArrayList with the resources to be negotiated
     * @return A Long value, corresponding to the worst availability among the resources to be negotiated
     */
    private void setFlightAvailability(Flight flight) {
        long worstAvailability = -1;
        System.out.println("flight.getAvailability()" + flight.getAvailability());
        if (flight.getAvailability() == null) {
            if (flight.getAircraft() != null){
                worstAvailability = flight.getAircraft().getAvailability();
            }
            System.out.println("flight.getAircraft().getAvailability();" + flight.getAircraft().getAvailability());
            System.out.println("worstavail " + worstAvailability);
            for (int i = 0; i < flight.getCrewMembers().size(); i++) {
                if (flight.getCrewMembers().get(i).getAvailability() > worstAvailability) {
                    worstAvailability = flight.getCrewMembers().get(i).getAvailability();
                }
            }
            flight.setAvailability(worstAvailability);
        }
        System.out.println("Fligh avail " + flight.getAvailability());
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
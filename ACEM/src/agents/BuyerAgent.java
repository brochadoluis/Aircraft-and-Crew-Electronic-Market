package agents;


import db_connection.Loader;
import hirondelle.date4j.DateTime;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;
import jade.util.Logger;
import utils.*;

import java.io.IOException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;


/**
 * Created by Luis on 20/02/2017.
 */
public class BuyerAgent extends Agent implements Serializable {
    private final String LOWER = "LOWER";
    private final String MUCH_LOWER = "MUCH LOWER";
    private final String OK = "OK";
    private final String CPT = "CPT";
    private final String OPT = "OPT";
    private final String SCC = "SCC";
    private final String CC = "CC";
    private final String CAB = "CAB";

    private final Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());
    private Log log;
    private Proposal bestProposal = null;
    private Flight disruptedFlight = new Flight();
    private double maximumDisruptionCost; // this is imported
    private DateTime scheduledDeparture;
    private DateTime tripTime;
    private String resourceType = "";
    private String origin = "";
    private String  destination = "";
    private String fleet = "";
    private double aircraftDisruptionCost = 0;
    private int total_pax = 0;
    private int delayInMinutes = 0;
    private double crewDisruptionCost = 0;
    private String company = "";
    private ArrayList<AID> sellers = new ArrayList<>();
    private int negotiationParticipants;
    private Integer round = 0;
    private long scheduledDepartureMilli;
    private long tripTimeMilli;
    private long delayInMilli;
    private int crewMemberSalary;
    private Proposal proposal;
    private String resourceAffected = "";
    private Loader db = new Loader();
    private int  nCPT = 0;
    private int  nOPT = 0;
    private int  nSCC = 0;
    private int  nCC = 0;
    private int  nCAB = 0;
    private boolean isAircraftNeeded = false;
//    private final String role = "Buyer";

    @Override
    protected void setup() {
        createLogger();
        // Read the maximum cost as argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            resourceAffected = String.valueOf(args[0]);
            initiateParameters();

            logger.log(Level.INFO, "Worst case scenario {0} € as cost.", maximumDisruptionCost);
            DFServices dfs = registerInDFS();
            findReceivers(dfs);
            /**
             * Connection to DB
             * Found disruption
             * Resource needed = new Aircraft("Boeing 777", 396);
             * findDisruption() - simulates a disruption, creating the resources to search in the market
             */
            //The price calculated by Buyer is the maximum to be paid
            negotiationParticipants = sellers.size();
            DFAgentDescription dfd = new DFAgentDescription();
            // Fill the CFP message
            ACLMessage msg = sendFirstCFP();
            addBehaviour(new ContractNetInitiator(this, msg) {
                @Override
                protected void handlePropose(ACLMessage propose, Vector v) {
                    try {
                        logger.log(Level.INFO, "Agent{0}, proposed {1}", new Object[]{propose.getSender().getName(), propose.getContentObject()});
                        logger.log(Level.INFO, "Round n (Handle Propose): {0}", round);
                    } catch (UnreadableException e) {
                        logger.log(Level.SEVERE, "Could not get message's content: {0} ", e);
                    }
                }

                @Override
                protected void handleRefuse(ACLMessage refuse) {
                    logger.log(Level.INFO, "Agent{0}, refused ", refuse.getSender().getName());
                    logger.log(Level.INFO, "Round n (Handle Refuse): {0}", round);
                    negotiationParticipants--;
                    /*for (int i = 0; i < sellers.size(); i++) {
                        if (sellers.get(i).getName().equals(refuse.getSender().getName())) {
                            sellers.remove(i);
                            negotiationParticipants--;
                        }
                    }
                    if (negotiationParticipants == 0) {
                        Fazer set as repostas
                        logger.log(Level.INFO, "TakeDown ");
                        takeDown(this.getAgent());
                        doDelete();
                    }*/

                    /**
                     * Removes seller from receivers, and removes his response from responses vector
                     */
                }

                protected void handleFailure(Agent agent, ACLMessage failure) {
                    if (failure.getSender().equals(agent.getAMS())) {
                        // FAILURE notification from the JADE runtime: the receiver
                        // does not exist
                        logger.log(Level.WARNING, "Responder does not exists");
                    } else {
                        logger.log(Level.INFO, "Agent{0} failed", failure.getSender().getName());
                    }
                    // Immediate failure --> we will not receive a response from this agent
                    negotiationParticipants--;
                }

                @Override
                protected void handleAllResponses(Vector responses, Vector acceptances) {
                    if (responses.size() < negotiationParticipants) {
                        // Some responder didn't reply within the specified timeout
                        logger.log(Level.INFO, "Timeout expired: missing {0} responses", negotiationParticipants - responses.size());
                    }
                    // Evaluate proposals.
                    AID bestProposer = null;
                    ACLMessage accept = null;
                    Enumeration e = responses.elements();
                    while (e.hasMoreElements()) {
                        ACLMessage msg = (ACLMessage) e.nextElement();
                        if (negotiationParticipants == 0){
//                            System.out.println("Nao ha mais participantes");
//                            System.out.println("Best is: " + proposal.getSender());
//                            bestProposal.printProposal();
                            bestProposer = bestProposal.getSender();
//                            System.out.println("msg ali = " + msg);
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.CFP);
                            acceptances.addElement(reply);
                            accept = reply;
//                        logger.log(Level.INFO, "Accepting proposal {0}, from responder {1}", new Object[]{bestProposal, bestProposer.getName()});
                            //accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            setRefuses(acceptances);
                            try {
                                accept.setContentObject(bestProposal);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            logger.log(Level.INFO, "Round n (After Accept): {0} ", round);
//                            newIteration(acceptances);
                        }
                        if (msg.getPerformative() == ACLMessage.PROPOSE) {

                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.CFP);
                            acceptances.addElement(reply);
                            proposal = retrieveProposalContent(msg);
//                            System.out.println("Proposal =");
                            evaluateProposal(proposal);

                        }
                            /*if (msg.getPerformative() == ACLMessage.REFUSE) {
                                responses.remove(msg);
                            }*/
                    }
                    // Accept the proposal of the best proposer
                    if (accept != null) {
                        //sets one to accept and all others to refuse
//                        System.out.println("Best Proposal price " + bestProposal.getPrice());
//                        System.out.println("Best Proposal avail " + bestProposal.getAvailability());
                        logger.log(Level.INFO, "Accepting proposal {0}, from responder {1}", new Object[]{bestProposal, bestProposer.getName()});
                        accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        setRefuses(acceptances);
                        logger.log(Level.INFO, "Round n (After Accept): {0} ", round);
                        logger.log(Level.INFO, "Accepted proposal from round: {0} ", bestProposal.getRound());
////                        System.out.println("Accepted proposal from round: " + bestProposal.getRound() + " and with price = " + bestProposal.getPrice());
                        //voltar a ler a lista de sellers, identificar a melhor proposta e enviar accept para esse e reject para todos os outros
                    }
                    //accept == null => send another cfp
                    else {
                        //Para cada resposta, fazer reply com CFP
                        logger.log(Level.SEVERE, "Setting proposals feedback");
                        setProposalsFeedback(responses, acceptances);
                        logger.log(Level.SEVERE, "Preparing another CFP");
                        logger.log(Level.SEVERE, "new iteration");
                        newIteration(acceptances);
                    }
                    updateRound();
                }

                @Override
                protected void handleInform(ACLMessage inform) {
                    logger.log(Level.SEVERE, "Agent {0} successfully performed the requested action", inform.getSender().getName());
                    logger.log(Level.INFO, "Round n (Handle Inform): {0}", round);
                    takeDown();
                    System.exit(0);
                }
            });
        } else {
            logger.log(Level.SEVERE, "Invalid number of arguments or arguments are null");
        }
    }

    private void setRefuses(Vector acceptances){
        for(int i = 0; i < acceptances.size(); i++) {
            ACLMessage reject = (ACLMessage) acceptances.get(i);
////            System.out.println("ACLMessage) acceptances.get(i) " + acceptances.get(i));
            if(reject.getPerformative() != ACLMessage.ACCEPT_PROPOSAL)
                reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
        }

////        System.out.println("Acceptances size = " + acceptances.size());
    }

    private ACLMessage sendFirstCFP() {
        logger.log(Level.INFO, "Round n (Before first CFP): {0}", round);
//        disruptedFlight.printFlight();
        Proposal cfp = new Proposal(0F, 0L, disruptedFlight, this.getAID());
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        for (AID seller : sellers) {
            msg.addReceiver(seller);
        }
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET);
        // We want to receive a reply in 10 secs
        //msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        try {
            msg.setContentObject(cfp);
            updateRound();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not set message's content: {0} ", e);
        }
////        System.out.println("msg = " + msg);
        return msg;
    }

    private void updateRound() {
        round++;
    }

    /**
     * Use delay percentage? example: until 20% delay is OK, 20% to 50% or 60% is LOWER, 60% to 100% is MUCH LOWER
     * Price is always negociable
     * @param responses
     * @param acceptances
     */
    private void setProposalsFeedback(Vector responses, Vector acceptances) {
        for (int i = 0; i < responses.size(); i++) {
            ACLMessage msgReceived = (ACLMessage) responses.get(i);
            Proposal response;
            try {
                response = (Proposal) msgReceived.getContentObject();
                ACLMessage msgToSend = (ACLMessage) acceptances.get(i);

                double proposedPrice = response.getPrice();
                long proposedAvailability = response.getAvailability();
                Proposal proposalWithComments = new Proposal(proposedPrice, proposedAvailability, response.getFlight(), response.getSender());
                //These conditions need to be updated
//                if (bestProposal.getAvailability() != 0) {
                double priceInterval = proposedPrice/maximumDisruptionCost;
                double availabilityInterval = proposedAvailability / (double) delayInMilli;
                if (isBetween(0.60,1.1, availabilityInterval)) {
                    proposalWithComments.setAvailabilityComment(MUCH_LOWER);
                    if (isBetween(0,0.15, priceInterval)){
                        proposalWithComments.setPriceComment(OK);
                    }
                    else if (isBetween(0.15,0.45, priceInterval)){
                        proposalWithComments.setPriceComment(LOWER);
                    }
                    else if (isBetween(0.45,1.1, priceInterval)){
                        proposalWithComments.setPriceComment(MUCH_LOWER);
                    }
                    else {
                        proposalWithComments.setPriceComment(LOWER);
                    }
                } else if (isBetween(0.20,0.60, availabilityInterval)) {
                    proposalWithComments.setAvailabilityComment(LOWER);
                    if (isBetween(0,0.45, priceInterval)){
                        proposalWithComments.setPriceComment(OK);
                    }
                    else if (isBetween(0.45,0.80, priceInterval)){
                        proposalWithComments.setPriceComment(LOWER);
                    }
                    else if (isBetween(0.80,1.1, priceInterval)){
                        proposalWithComments.setPriceComment(MUCH_LOWER);
                    }
                    else
                        proposalWithComments.setPriceComment(MUCH_LOWER);
                } else if (isBetween(0,0.20, availabilityInterval)) {
                    proposalWithComments.setAvailabilityComment(OK);
                    if (isBetween(0,0.70, priceInterval)){
                        proposalWithComments.setPriceComment(OK);
                    }
                    else if (isBetween(0.70,1.1, priceInterval)){
                        proposalWithComments.setPriceComment(LOWER);
                    }
                    else
                        proposalWithComments.setPriceComment(MUCH_LOWER);
                }

                //Considers only price or availability too? If depends on availability also, the better the availability comment, the more expensive it can be
//                if (proposalWithComments.getAvailabilityComment().equalsIgnoreCase(OK) && isBetween(0,maximumDisruptionCost*0.3, priceInterval)){
                /*if (isBetween(maximumDisruptionCost*0.6,maximumDisruptionCost, priceInterval)){
                    proposalWithComments.setPriceComment(MUCH_LOWER);
//                    System.out.println("Comment Price set to MUCH LOWER");
                } else if (isBetween(maximumDisruptionCost*0.15,maximumDisruptionCost*0.6, priceInterval)) {
                    proposalWithComments.setPriceComment(LOWER);
//                    System.out.println("Comment Price set to LOWER");
                } else if (isBetween(0,maximumDisruptionCost*0.15, priceInterval)) {
                    proposalWithComments.setPriceComment(OK);
//                    System.out.println("Comment Price set to OK");
                }*/
                /*} else
                    proposalWithComments.setAvailabilityComment(OK);*/
                msgToSend.setContentObject(proposalWithComments);
            } catch (UnreadableException e) {
                logger.log(Level.SEVERE, "Could not get message's content: {0} ", e);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Could not set message's content: {0} ", e);
            }

        }
    }

    private boolean isBetween(double lowerBound, double upperBound, double value){
        if (value >= lowerBound && value < upperBound)
            return  true;
        return false;
    }

    private Proposal retrieveProposalContent(ACLMessage msg) {
        Proposal p;
        try {
            p = (Proposal) msg.getContentObject();
            return p;
        } catch (UnreadableException e) {
            logger.log(Level.SEVERE, "Could not get message's content {0}", e);
        }
        return null;
    }

    private void createLogger() {
        /*logger.setLevel(Level.SEVERE);
        String logfile = this.getLocalName();
        log = new Log(logfile);
        // create an instance of Log at the top of the file, as you would do with log4j
        FileHandler fh;   // true forces append mode
        try {
            fh = new FileHandler("Buyer logFile.log", false);
            SimpleFormatter sf = new SimpleFormatter();
            fh.setFormatter(sf);
            logger.addHandler(fh);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not create Log File {0} ", e);
        }*/
    }

    /**
     * evaluateProposal method evaluates the proposals received
     * according to Buyer's utility. Sets comments
     *
     * @param proposal : Proposal read from ACL Message
     * @return true if all resources match, false otherwise
     */
    private void evaluateProposal(Proposal proposal) {
        logger.log(Level.INFO, " Proposal ");
        logger.log(Level.INFO, "Agent {2} send {0} price and {1} availability ", new Object[]{proposal.getPrice(), proposal.getAvailability(), proposal.getSender()});
//        proposal.printProposal();
        double priceOffered = proposal.getPrice();
        long availabilityOffered = proposal.getAvailability();
        double utility = utilityCalculation(priceOffered, availabilityOffered);
////        System.out.println("Current prtoposal utility " + utility);
        logger.log(Level.INFO, "Current Proposal utility {0}", utility);
        if (bestProposal == null) {
//            System.out.println("Adicionei proposta");
//            proposal.printProposal();
            bestProposal = proposal;
            bestProposal.setRound(round);
        } else {
            double bestProposalUtility;
            bestProposalUtility = utilityCalculation(bestProposal.getPrice(), bestProposal.getAvailability());
            logger.log(Level.INFO, "Best Proposal utility {0}", bestProposalUtility);
////            System.out.println("best proposal utility " + bestProposalUtility);
////            System.out.println("Current proposal utility " + utility);
            logger.log(Level.INFO, "Current Proposal utility {0}", utility);
            if (utility > bestProposalUtility) {
//                System.out.println("Best Proposal Updated");
                bestProposal = proposal;
                bestProposal.setRound(round);
            }
            else if (utility == bestProposalUtility){
                if (priceOffered < bestProposal.getPrice()){
                    bestProposal = proposal;
                }
            }
        }

    }

    private double utilityCalculation(double priceOffered, long availabilityOffered) {
        long proposedDeparture = scheduledDepartureMilli + availabilityOffered;
//        System.out.println("((double) scheduledDepartureMilli / proposedDeparture) " + ((double) availabilityOffered/ delayInMilli));
//        System.out.println("Real utility is " + (((1.0 -(double) availabilityOffered/ delayInMilli) + (1.0 - (priceOffered / maximumDisruptionCost))) / 2));
        return (((1.0 -(double) availabilityOffered/ delayInMilli))*0.2 + (1.0 - (priceOffered / maximumDisruptionCost))*0.8);

    }

    private void findReceivers(DFServices dfs) {
        /* Saves to an ArrayList all other agents registered in DFService  */
        sellers = dfs.searchDF(this);
//        System.out.println("Sellers = " + sellers);
//        System.out.println("Sellers Size " + sellers.size());
        logger.log(Level.INFO, " Sellers: ");
        for (AID seller : sellers) {
            logger.log(Level.INFO, " {0}, ", seller.getLocalName());
        }
    }

    private DFServices registerInDFS() {
        DFAgentDescription dfd = new DFAgentDescription();
        DFServices dfs = new DFServices();
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getLocalName());
        //sd.addProperties(new Property("country", "Italy"));
        dfd = dfs.register(this);
        return dfs;
    }

    private void initiateParameters() {
        /**
         * Connect to DB and read values
         * Buyer receives the resource affected and the date  as argument
         * this method gets the
         */
        db.establishConnection();
        getResourceType(resourceAffected);
    }

    private void extractQueryData(ResultSet rs, ResultSetMetaData rsmd, int columnsNumber, String resourceType) throws SQLException {
        for (int i = 1; i <= columnsNumber; i++) {
//            if ("aircraft".equals(resourceType)){
            queryToVariables(rs, rsmd, i);
//            }
//            System.out.println(rsmd.getColumnLabel(i) + " : " + rs.getString(i));
        }
//        System.out.println("TripTime " + scheduledDeparture);
        DateTime dateTimeTripTime = scheduledDeparture.plus(0,0,0,tripTime.getHour(),tripTime.getMinute(),tripTime.getSecond(),0,DateTime.DayOverflow.FirstDay);
        scheduledDepartureMilli = scheduledDeparture.getMilliseconds(TimeZone.getTimeZone("GMT"));
        tripTimeMilli = ((dateTimeTripTime.getMilliseconds(TimeZone.getTimeZone("GMT"))) - scheduledDepartureMilli);
        delayInMilli = delayInMinutes * 60 * 1000;
        double tripTimeInHours = (double)tripTimeMilli/3600000;
//        System.out.println("delayInMilli " + delayInMilli);
//        System.out.println("");
        if (isAircraftNeeded){
            maximumDisruptionCost = aircraftDisruptionCost;
        }
        else if ("all crew".equalsIgnoreCase(resourceType)) {
            maximumDisruptionCost = crewDisruptionCost;
        }
        else
            maximumDisruptionCost = (crewMemberSalary * tripTimeInHours) * 1.5;
    }

    private void getResourceType(String resourceAffected) {
        String dbRow = "SELECT * FROM sample.buyer WHERE \uFEFFresource_affected LIKE '" + resourceAffected + "';";
//        String dbRow = "SELECT * FROM sample.buyer WHERE \uFEFFresource_affected LIKE '" + resourceAffected + "';";
        ResultSet unidentifiedResource = db.fetchDataBase(dbRow);
        if (unidentifiedResource != null){
            try {
                unidentifiedResource.next();
                resourceType = unidentifiedResource.getString("missing_resources");
//                System.out.println("Resource Type  " + resourceType);
                prepareQuery(resourceType, resourceAffected);
            } catch (SQLException e) {
                // handle any errors
//                System.out.println("SQLException: " + e.getMessage());
//                System.out.println("SQLState: " + e.getSQLState());
//                System.out.println("VendorError: " + e.getErrorCode());
            }
        }
    }

    private void prepareQuery(String resourceType, String resourceAffected) {
        String query = "";
        String salaryQuery = "";
        if ("aircraft".equals(resourceType)) {
            //Aircraft + Crew are need, but the agent just asks for an aircraft and the seller knows that the crew is included
            /*query = "SELECT origin, destination, scheduled_time_of_departure, departure_delay_in_minutes, \n" +
                    "estimated_trip_time, total_pax, crew_res_type, cost_disr_aircraft, cost_disr_crew\n" +
                    "FROM thesis.buyer\n" +
                    "WHERE \uFEFFresource_affected = '" + resourceAffected + "';";
        }
        else if ("all crew".equals(resourceType)) {
            // All crew is needed
            query = "SELECT CPT, OPT, SCC, CC, CAB,scheduled_time_of_departure, departure_delay_in_minutes, \n" +
                    "estimated_trip_time, crew_res_type, cost_disr_aircraft, cost_disr_crew\n" +
                    "FROM thesis.buyer\n" +
                    "WHERE \uFEFFresource_affected LIKE '" + resourceAffected + "';";
        }
        else if (CPT.equalsIgnoreCase(resourceType) || OPT.equalsIgnoreCase(resourceType)
                || SCC.equalsIgnoreCase(resourceType) || CC.equalsIgnoreCase(resourceType)
                || CAB.equalsIgnoreCase(resourceType)) {
            //only 1 crew member type, or more than 1?
            query = "SELECT  \uFEFFmissing_resource,scheduled_time_of_departure, departure_delay_in_minutes, \n" +
                    "estimated_trip_time, crew_res_type, cost_disr_aircraft, cost_disr_crew, \n" +
                    "CPT, OPT, SCC, CC, CAB\n" +
                    "FROM thesis.buyer\n" +
                    "WHERE \uFEFFresource_affected LIKE '" + resourceAffected + "';";*/
            query = "SELECT origin, destination, scheduled_time_of_departure, departure_delay_in_minutes, \n" +
                    "estimated_trip_time, total_pax, crew_res_type, cost_disr_aircraft, cost_disr_crew\n" +
                    "FROM sample.buyer\n" +
                    "WHERE \uFEFFresource_affected = '" + resourceAffected + "';";
            isAircraftNeeded = true;
        }
        else if ("all crew".equals(resourceType)) {
            // All crew is needed
            query = "SELECT CPT, OPT, SCC, CC, CAB,scheduled_time_of_departure, departure_delay_in_minutes, \n" +
                    "estimated_trip_time, crew_res_type, cost_disr_aircraft, cost_disr_crew\n" +
                    "FROM sample.buyer\n" +
                    "WHERE \uFEFFresource_affected LIKE '" + resourceAffected + "';";
        }
        else if (CPT.equalsIgnoreCase(resourceType) || OPT.equalsIgnoreCase(resourceType)
                || SCC.equalsIgnoreCase(resourceType) || CC.equalsIgnoreCase(resourceType)
                || CAB.equalsIgnoreCase(resourceType)) {
            //only 1 crew member type, or more than 1?
            query = "SELECT  missing_resources,scheduled_time_of_departure, departure_delay_in_minutes, \n" +
                    "estimated_trip_time, crew_res_type, cost_disr_aircraft, cost_disr_crew, \n" +
                    " "+resourceType+"\n" +
                    "FROM sample.buyer\n" +
                    "WHERE \uFEFFresource_affected LIKE '" + resourceAffected + "';";
            salaryQuery ="SELECT MAX(hourly_salary) AS salary FROM crew_member WHERE rank LIKE '"+ resourceType +"';" ;
            ResultSet rs = db.fetchDataBase(salaryQuery);
            if (rs != null) {
                ResultSetMetaData rsmd;
                try {
                    rsmd = rs.getMetaData();
                    while (rs.next()) {
                        extractSalary(rs, rsmd);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Salary is: " + crewMemberSalary);
        }
        ResultSet rs = db.fetchDataBase(query);
        if (rs != null) {
            ResultSetMetaData rsmd;
            try {
                rsmd = rs.getMetaData();
                while (rs.next()) {
                    extractQueryData(rs, rsmd, rsmd.getColumnCount(), resourceType);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            initializeFlightVariables(disruptedFlight,resourceType);
        }
    }

    private void initializeFlightVariables(Flight flight, String resourceType) {
        flight.setScheduledDeparture(scheduledDepartureMilli);
        flight.setTripTime(tripTimeMilli);
        flight.setDelay(delayInMilli);
        flight.setOrigin(origin);
        flight.setDestination(destination);
        flight.setFleet(fleet);
        addResourcesToFlight(resourceType);
    }

    private void addResourcesToFlight(String resourceType) {
        if ("aircraft".equalsIgnoreCase(resourceType)){
            Aircraft aircraftNeeded = new Aircraft();
            setAircraftVariables(aircraftNeeded);
            disruptedFlight.setAircraft(aircraftNeeded);
        }
        //change to accept one of each
        else if (CPT.equalsIgnoreCase(resourceType) || OPT.equalsIgnoreCase(resourceType)
                || SCC.equalsIgnoreCase(resourceType) || CC.equalsIgnoreCase(resourceType)
                || CAB.equalsIgnoreCase(resourceType)){
            addCrewMemberToFlight(disruptedFlight, resourceType);
        }
        else if ("all crew".equalsIgnoreCase(resourceType)){
            addCrewMemberToFlight(disruptedFlight, CPT);
            addCrewMemberToFlight(disruptedFlight, OPT);
            addCrewMemberToFlight(disruptedFlight, SCC);
            addCrewMemberToFlight(disruptedFlight, CC);
            addCrewMemberToFlight(disruptedFlight, CAB);

        }
        //Adicionar tambem o preço e(depois de os somar) e guardar numa variavel do buyer e nao do voo!!!!!
    }

    private void addCrewMemberToFlight(Flight flight, String resourceType) {
        if (CPT.equalsIgnoreCase(resourceType)){
            CrewMember cm = verifyCrewMember(flight, nCPT, resourceType);
            if (cm != null) {
                flight.addCrewMember(cm);
            }
        }
        if (OPT.equalsIgnoreCase(resourceType)){
            CrewMember cm = verifyCrewMember(flight, nOPT, resourceType);
            if (cm != null)
                flight.addCrewMember(cm);
        }
        if (SCC.equalsIgnoreCase(resourceType)) {
            CrewMember cm = verifyCrewMember(flight, nSCC, resourceType);
            if (cm != null)
                flight.addCrewMember(cm);
        }
        if (CC.equalsIgnoreCase(resourceType)) {
            CrewMember cm = verifyCrewMember(flight, nCC, resourceType);
            if (cm != null)
                flight.addCrewMember(cm);
        }
        if (CAB.equalsIgnoreCase(resourceType)){
            CrewMember cm = verifyCrewMember(flight, nCAB, resourceType);
//            System.out.println("NCAB = " + nCAB);
            if (cm != null){
//                System.out.println("Cm " + cm.getCategory());
//                System.out.println("Cm Number is  " + cm.getNumber());
                flight.addCrewMember(cm);
            }
        }
    }

    private CrewMember verifyCrewMember(Flight flight, int crewMemberNumber, String crewMemberCategory) {
//        if (crewMemberNumber != 0) {
        if (crewMemberNumber >= 0) {
            CrewMember cm = new CrewMember();
            cm.setNumber(crewMemberNumber);
            cm.setCategory(crewMemberCategory);
            return cm;
        }
        return null;
    }

    private void setAircraftVariables(Aircraft aircraftNeeded) {
        aircraftNeeded.setCapacity(total_pax);
    }

    private void queryToVariables(ResultSet rs, ResultSetMetaData rsmd, int index) throws SQLException {
        switch (rsmd.getColumnLabel(index)){
            case "origin":
                origin = rs.getString(index);
                break;
            case "destination":
                destination = rs.getString(index);
                break;
            case "scheduled_time_of_departure":
                scheduledDeparture = new DateTime(rs.getString(index));
                break;
            case "departure_delay_in_minutes":
                delayInMinutes = Integer.parseInt(rs.getString(index));
                break;
            case "estimated_trip_time":
                tripTime = new DateTime("0" + rs.getString(index));
                break;
            case "total_pax":
                total_pax = Integer.parseInt(rs.getString(index));
                break;
            case "crew_res_type":
                fleet = rs.getString(index);
                break;
            case "cost_disr_aircraft":
                aircraftDisruptionCost = Double.parseDouble(rs.getString(index).replace(",","."));
                break;
            case "cost_disr_crew":
                crewDisruptionCost = Double.parseDouble(rs.getString(index).replace(",","."));
                break;
            case CPT:
                nCPT = Integer.parseInt(rs.getString(index));
                break;
            case OPT:
                nOPT = Integer.parseInt(rs.getString(index));
                break;
            case SCC:
                nSCC = Integer.parseInt(rs.getString(index));
                break;
            case CC:
                nCC = Integer.parseInt(rs.getString(index));
                break;
            case CAB:
                nCAB = Integer.parseInt(rs.getString(index));
                break;
            default:
                break;
        }
    }
    private void extractSalary(ResultSet rs, ResultSetMetaData rsmd) throws SQLException {
        switch (rsmd.getColumnLabel(1)) {
            case "salary":
                crewMemberSalary = Integer.parseInt(rs.getString(1));
                break;
            default:
                break;
        }
    }

    private void takeDown(Agent a) {
        logger.log(Level.INFO, "Agent {0} has been unregistered from DFS. ", a.getLocalName());
        // Deregister from the yellow pages
        try {
            DFService.deregister(a);
        } catch (FIPAException fe) {
            logger.log(Level.SEVERE, "Could not deregister agent: ", fe);
        }
    }
}
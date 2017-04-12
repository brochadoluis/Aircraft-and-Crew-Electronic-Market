package agents;


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
import utils.Aircraft;
import utils.CrewMember;
import utils.Log;
import utils.Resource;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;


/**
 * Created by Luis on 20/02/2017.
 */
public class BuyerAgent extends Agent implements Serializable {
    private final String LOWER = "LOWER";
    private final String MUCH_LOWER = "MUCH LOWER";
    private final String OK = "OK";
    private final Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());
    private Log log;
    private Proposal bestProposal = null;
    private ArrayList<Resource> resourcesMissing = new ArrayList<>();
    private double maximumDisruptionCost;
    private DateTime scheduledDeparture;
    private DateTime flightDuration;
    private DateTime flightDelay;
    private String company = "";
    private ArrayList<AID> sellers = new ArrayList<>();
    private int negotiationParticipants;
    private Integer round = 0;
    private long scheduledDepartureMilli;
    private long flightDurationMilli;
    private long flightDelayMilli;
    private long durationInMilli;
    private long delayToMinimizeInMilli;
    private Proposal proposal;
//    private final String role = "Buyer";

    @Override
    protected void setup() {
        createLogger();
        initiateParameters();
        // Read the maximum cost as argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            maximumDisruptionCost = Double.parseDouble(String.valueOf(args[0]));
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
                    for (int i = 0; i < sellers.size(); i++) {
                        if (sellers.get(i).getName().equals(refuse.getSender().getName())) {
                            sellers.remove(i);
                            negotiationParticipants--;
                        }
                    }
                    if (negotiationParticipants == 0) {
                        logger.log(Level.INFO, "TakeDown ");
                        takeDown(this.getAgent());
                        doDelete();
                    }

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
                        if (msg.getPerformative() == ACLMessage.PROPOSE) {
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.CFP);
                            acceptances.addElement(reply);
                            proposal = retrieveProposalContent(msg);
                            evaluateProposal(proposal);
                            if (round == 7) {
                                bestProposal = proposal;
                                bestProposer = msg.getSender();
                                accept = reply;
                                logger.log(Level.INFO, "Best proposal is: {0}", bestProposal.getAvailability() );
                                bestProposal.printProposal();
                            }
                        }
                        if (msg.getPerformative() == ACLMessage.REFUSE) {
                            responses.remove(msg);
                        }
                    }
                    // Accept the proposal of the best proposer
                    if (accept != null) {
                        //sets one to accept and all others to refuse
                        logger.log(Level.INFO, "Accepting proposal {0}, from responder {1}", new Object[]{bestProposal, bestProposer.getName()});
                        accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        setRefuses(acceptances);
                        logger.log(Level.INFO, "Round n (After Accept): {0} ", round);
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
                }
            });
        } else {
            logger.log(Level.SEVERE, "Invalid number of arguments or arguments are null");
        }
    }

    private void setRefuses(Vector acceptances){
        for(int i = 0; i < acceptances.size(); i++) {
            ACLMessage reject = (ACLMessage) acceptances.get(i);
            if(reject.getPerformative() != ACLMessage.ACCEPT_PROPOSAL)
                reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
        }
    }

    private ACLMessage sendFirstCFP() {
        logger.log(Level.INFO, "Round n (Before first CFP): {0}", round);
        Proposal cfp = new Proposal(0F, 1L, resourcesMissing, this.getAID());
        cfp.setScheduledDeparture(1489053600000L);
        long maxDelay = getMaxDelay(resourcesMissing);
        cfp.setDelay(maxDelay);
        cfp.setDuration(durationInMilli);
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        for (AID seller : sellers) {
            msg.addReceiver(seller);
        }
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET);
        // We want to receive a reply in 10 secs
        msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        try {
            msg.setContentObject(cfp);
            updateRound();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not set message's content: {0} ", e);
        }
        return msg;
    }

    private void updateRound() {
        round++;
    }

    private void setProposalsFeedback(Vector responses, Vector acceptances) {
        for (int i = 0; i < responses.size(); i++) {
            ACLMessage msgReceived = (ACLMessage) responses.get(i);
            Proposal response;
            try {
                response = (Proposal) msgReceived.getContentObject();
                ACLMessage msgToSend = (ACLMessage) acceptances.get(i);
                double proposedPrice = response.getPrice();
                long proposedAvailability = response.getAvailability();
                Proposal proposalWithComments = new Proposal(proposedPrice, proposedAvailability, response.getResourcesProposed(), response.getSender());
                //These conditions need to be updated
                if (proposedPrice / bestProposal.getPrice() >= 5) {
                    proposalWithComments.setPriceComment(MUCH_LOWER);
//                    System.out.println("Comment Price set to MUCH LOWER");
                } else if (proposedPrice / bestProposal.getPrice() < 5) {
                    proposalWithComments.setPriceComment(LOWER);
//                    System.out.println("Comment Price set to LOWER");
                } else if (proposedPrice / bestProposal.getPrice() <= 3) {
                    proposalWithComments.setPriceComment(OK);
//                    System.out.println("Comment Price set to OK");
                }
                if (bestProposal.getAvailability() != 0) {
                    if (proposedAvailability / bestProposal.getAvailability() >= 5) {
                        proposalWithComments.setAvailabilityComment(MUCH_LOWER);
//                        System.out.println("Comment Availability set to MUCH LOWER");
                    } else if (proposedAvailability / bestProposal.getAvailability() < 5) {
                        proposalWithComments.setAvailabilityComment(LOWER);
//                        System.out.println("Comment Availability set to LOWER");
                    } else if (proposedAvailability / bestProposal.getAvailability() <= 1) {
//                        System.out.println("proposedAvailability " + proposedAvailability);
                        proposalWithComments.setAvailabilityComment(OK);
//                        System.out.println("Comment Availability set to OK");
                    }
                } else
                    proposalWithComments.setAvailabilityComment(OK);
                msgToSend.setContentObject(proposalWithComments);
            } catch (UnreadableException e) {
                logger.log(Level.SEVERE, "Could not get message's content: {0} ", e);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Could not set message's content: {0} ", e);
            }
        }
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
        logger.setLevel(Level.CONFIG);
        /*String logfile = this.getLocalName();
        log = new Log(logfile);*/
        // create an instance of Log at the top of the file, as you would do with log4j
        /*FileHandler fh;   // true forces append mode
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
        proposal.printProposal();
        double priceOffered = proposal.getPrice();
        long availabilityOffered = proposal.getAvailability();
        double utility = utilityCalculation(priceOffered, availabilityOffered);

        if (bestProposal == null) {
            bestProposal = proposal;
        } else {
            double bestProposalUtility;
            bestProposalUtility = utilityCalculation(bestProposal.getPrice(), bestProposal.getAvailability());
            if (utility > bestProposalUtility) {
                bestProposal = proposal;
            }
            //equals?
        }
        /**
         * Queue == null
         * -> push
         * else
         * compare utilities
         * >queue?
         * push
         * else,
         * discard
         */
    }

    private long getMaxDelay(ArrayList<Resource> resources) {
        long maxDelay = -1;
        if (resources.get(0).getDelay() != null) {
            maxDelay = resources.get(0).getDelay();
            for (int i = 0; i < resources.size(); i++) {
                Resource aResource = resources.get(i);
                maxDelay = aResource.getDelay();
                if (aResource.getDelay() > maxDelay) {
                    maxDelay = aResource.getDelay();
                }
            }
        }
        return maxDelay;
    }

    private double utilityCalculation(double priceOffered, long availabilityOffered) {
        long proposedDeparture = scheduledDepartureMilli + availabilityOffered;

        return ((((double) scheduledDepartureMilli / proposedDeparture) + (1.0 - (priceOffered / maximumDisruptionCost))) / 2);

    }

    private void findReceivers(DFServices dfs) {
        /* Saves to an ArrayList all other agents registered in DFService  */
        sellers = dfs.searchDF(this);
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
         */
        scheduledDeparture = new DateTime("2017-03-09 10:00:00");
        flightDelay = new DateTime("2017-03-09 10:45:00");
        flightDuration = new DateTime("2017-03-09 18:00:00");
        scheduledDepartureMilli = scheduledDeparture.getMilliseconds(TimeZone.getTimeZone("GMT"));
        flightDelayMilli = flightDelay.getMilliseconds(TimeZone.getTimeZone("GMT"));
        flightDurationMilli = flightDuration.getMilliseconds(TimeZone.getTimeZone("GMT"));
        durationInMilli = flightDurationMilli - scheduledDepartureMilli;
        delayToMinimizeInMilli = scheduledDepartureMilli - flightDelayMilli;
        Resource a1 = new Aircraft("Boeing 777", 396);
        Resource cm1 = new CrewMember(2, "Pilot", "English A2");
        a1.setDelay(delayToMinimizeInMilli);
        cm1.setDelay(delayToMinimizeInMilli + 360000);
        resourcesMissing.add(a1);
        resourcesMissing.add(cm1);

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
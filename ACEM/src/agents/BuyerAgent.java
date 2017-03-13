package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;
import jade.util.Logger;
import utils.Aircraft;
import utils.CrewMember;
import utils.Resource;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import hirondelle.date4j.*;



/**
 * Created by Luis on 20/02/2017.
 */
public class BuyerAgent extends Agent implements Serializable{
    private final String LOWER = "LOWER";
    private final String HIGHER = "HIGHER";
    private final String OK = "OK";
    private final Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());
    private PriorityQueue<Proposal> bestProposalQ = new PriorityQueue<>();
    private ArrayList<Resource> resourcesMissing = new ArrayList<>();
    private double maximumDisruptionCost;
    private DateTime scheduledDeparture, flightDuration, flightDelay;
    private String company = "";
    private ArrayList<AID> sellers = new ArrayList<>();
    private int negotiationParticipants;
    private Integer round = 0;
    private long scheduledDepartureMilli, flightDurationMilli, flightDelayMilli, durationInMilli, delayToMinimizeInMilli;
    private Proposal proposal;
//    private final String role = "Buyer";

    @Override
    protected void setup() {
        createLogger();
        initiateParameters();
        // Read the maximum cost as argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            maximumDisruptionCost = (double) args[0];
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
            logger.log(Level.INFO,"Round n (Before first CFP): {0}", round);
            Proposal cfp = new Proposal(0F,resourcesMissing, this.getAID());
            cfp.setScheduledDeparture(1489053600000L);
            cfp.setDelay(-2700000L);
            ACLMessage msg = new ACLMessage(ACLMessage.CFP);
            for (AID seller:sellers) {
                msg.addReceiver(seller);
            }
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET);
            // We want to receive a reply in 10 secs
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
            try {
                msg.setContentObject(cfp);
                updateRound();
            } catch (IOException e) {
                logger.log(Level.SEVERE,"Could not set message's content: {0} ", e);
            }
            addBehaviour(new ContractNetInitiator(this, msg){
                @Override
                protected void handlePropose(ACLMessage propose, Vector v) {
                    try {
                        logger.log(Level.INFO,"Agent{0}, proposed {1}", new Object[]{propose.getSender().getName(),propose.getContentObject()});
                        logger.log(Level.INFO,"Round n (Handle Propose): {0}", round);
                    } catch (UnreadableException e) {
                        logger.log(Level.SEVERE,"Could not get message's content: {0} ", e);
                    }
                }

                @Override
                protected void handleRefuse(ACLMessage refuse) {
                    logger.log(Level.INFO,"Agent{0}, refused ",refuse.getSender().getName());
                    logger.log(Level.INFO,"Round n (Handle Refuse): {0}", round);
                }

                protected void handleFailure(Agent agent, ACLMessage failure) {
                    if (failure.getSender().equals(agent.getAMS())) {
                        // FAILURE notification from the JADE runtime: the receiver
                        // does not exist
                        logger.log(Level.WARNING,"Responder does not exists");
                    }
                    else {
                        logger.log(Level.INFO,"Agent{0} failed", failure.getSender().getName());
                    }
                    // Immediate failure --> we will not receive a response from this agent
                    negotiationParticipants--;
                }

                @Override
                protected void handleAllResponses(Vector responses, Vector acceptances) {
                    if (responses.size() < negotiationParticipants) {
                        // Some responder didn't reply within the specified timeout
                        logger.log(Level.INFO,"Timeout expired: missing {0} responses", negotiationParticipants-responses.size());
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
                            if (!bestProposalQ.isEmpty()) {
                                bestProposer = msg.getSender();
                                accept = reply;
                                logger.log(Level.INFO, "Best proposal is: ");
                                bestProposalQ.peek().printProposal();
                            }
                        }
                    }
                    setProposalsFeedback(responses, acceptances);
                    // Accept the proposal of the best proposer
                    if (accept != null) {
                        logger.log(Level.INFO,"Accepting proposal {0}, from responder {1}", new Object[]{bestProposalQ.toString(),bestProposer.getName()});
                        accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        updateRound();
                        logger.log(Level.INFO,"Round n (After Accept): {1}", round);
                    }
                    //accept == null => send another cfp
                    else{
                        //Para cada resposta, fazer reply com CFP
                        logger.log(Level.SEVERE,"Preparing another CFP");
                        /*Vector v = new Vector();
                        for (Object response:responses) {
                            ACLMessage resp = (ACLMessage) response;
                            ACLMessage cfp2 = resp.createReply();
                            cfp2.setPerformative(ACLMessage.CFP);
                            cfp2.addReceiver(resp.getSender());
                            cfp2.setProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET);
                            // We want to receive a reply in 10 secs
                            cfp2.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                            try {
                                cfp2.setContentObject(null);
                                updateRound();
                            } catch (IOException e1) {
                                logger.log(Level.SEVERE,"Could not set message's content: {0} ", e1);
                            }
                            v.add(cfp2);
                        }
                        for (Object vo:v) {
                            logger.log(Level.SEVERE,"vo = {0}", vo);
                        }*/

                        newIteration(acceptances);
                        logger.log(Level.SEVERE,"new iteration");
                        updateRound();
                        logger.log(Level.SEVERE,"Sending another CFP");
                        updateRound();
                    }

                }

                @Override
                protected void handleInform(ACLMessage inform) {
                    logger.log(Level.SEVERE,"Agent {0} successfully performed the requested action", inform.getSender().getName());
                    logger.log(Level.INFO,"Round n (Handle Inform): {0}", round);
                }
            });
        }
        else {
            logger.log(Level.SEVERE, "Invalid number of arguments or arguments are null");
        }
    }

    private void updateRound() {
        round++;
    }

    private void setProposalsFeedback(Vector responses, Vector acceptances) {
        for(int i = 0; i < responses.size(); i++){
            ACLMessage msgReceived = (ACLMessage) responses.get(i);
            Proposal response;
            try {
                response = (Proposal) msgReceived.getContentObject();
                ACLMessage msgToSend = (ACLMessage) acceptances.get(i);
                Proposal proposalWithComments = new Proposal(response.getPrice(),response.getResourcesProposed(),response.getSender());
                /**
                 * Evaluate/Compare with the proposal in the Best Proposal Queue
                 * if(preformative == performativa qlq)
                 * faz qlq coisa
                 */
                proposalWithComments.setPriceComment(LOWER);
                proposalWithComments.setAvailabilityComment(OK);
                msgToSend.setContentObject(proposalWithComments);
            } catch (UnreadableException e) {
                logger.log(Level.SEVERE,"Could not get message's content: {0} ", e);
            } catch (IOException e) {
                logger.log(Level.SEVERE,"Could not set message's content: {0} ", e);
            }
        }
/*
        Enumeration e = acceptances.elements();
        System.out.println("e = " + acceptances.size());
        while (e.hasMoreElements()) {
            ACLMessage reject = (ACLMessage) e.nextElement();
            System.out.println("reject  = " + reject);
            System.out.println("reject  = " + proposal);
            try {
                System.out.println("Reject ACL = " + reject.getContentObject());
            } catch (UnreadableException e1) {
                e1.printStackTrace();
            }
            if (reject.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                *//**
         * Something to know what comments to do
         *//*
                System.out.println("isto = " + e);
                Proposal rejectProposal;
                try {
                    rejectProposal = (Proposal) reject.getContentObject();
                    System.out.println("Reject Proposal; asdasdsa " + rejectProposal);
                } catch (UnreadableException e1) {
                    e1.printStackTrace();
                }
                //Proposal rejectProposal = retrieveProposalContent(reject);
                //System.out.println("Reject Proposal = " + rejectProposal);
                //rejectProposal.setPriceComment("LOWER");
                //rejectProposal.setAvailabilityComment("LOWER");
                // Feedback evaluation
                *//*try {
                    reject.setContentObject(rejectProposal);
                } catch (IOException e1) {
                    logger.log(Level.SEVERE, "Could not set message's content {0}", e1);
                }*//*

            }
        }*/
    }

    private Proposal retrieveProposalContent(ACLMessage msg) {
        Proposal p;
        try {
            p = (Proposal) msg.getContentObject();
            return p;
        } catch (UnreadableException e) {
            logger.log(Level.SEVERE,"Could not get message's content {0}", e);
        }
        return null;
    }

    private void createLogger() {
        logger.setLevel(Level.CONFIG);
        // create an instance of Logger at the top of the file, as you would do with log4j
        FileHandler fh = null;   // true forces append mode
        try {
            fh = new FileHandler("Buyer logFile.log", false);
            SimpleFormatter sf = new SimpleFormatter();
            fh.setFormatter(sf);
            logger.addHandler(fh);

        } catch (IOException e) {
            logger.log(Level.SEVERE,"Could not create Log File {0} " , e);
        }
    }

    /**
     * evaluateProposal method evaluates the proposals received
     * according to Buyer's utility. Sets comments
     *
     * @param proposal : Proposal read from ACL Message
     * @return true if all resources match, false otherwise
     */
    private void evaluateProposal(Proposal proposal) {
        logger.log(Level.CONFIG," Proposal ");
        proposal.printProposal();
        double priceAsked = proposal.getPrice();
        if(priceAsked <= maximumDisruptionCost) {
            bestProposalQ.add(proposal);
            logger.log(Level.CONFIG," Proposal added to queue ");
        }
        /**
         * Passa a fazer o calculo da utilidades das propostas todas
         * E insere a melhor na stack/queue.
         * Prepara para todos os sellers os comentarios as propostas feitas
         * Envia reject para todos com os comentarios e lanca novo CFP
         * sendo que o detentor da bestProposal recebe um <OK,OK>
         */
    }

    private void findReceivers(DFServices dfs) {
        /* Saves to an ArrayList all other agents registered in DFService  */
        sellers = dfs.searchDF(this);
        logger.log(Level.INFO," Sellers: ");
        for (AID seller:sellers) {
            logger.log(Level.INFO," {0}, ", seller.getLocalName());
        }
    }

    private DFServices registerInDFS() {
        DFAgentDescription dfd = new DFAgentDescription();
        DFServices dfs = new DFServices();
        ServiceDescription sd  = new ServiceDescription();
        sd.setName( getLocalName() );
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
        resourcesMissing.add(a1);
        resourcesMissing.add(cm1);
    }
}
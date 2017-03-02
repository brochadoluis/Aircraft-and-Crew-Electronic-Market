package agents;

import jade.core.AID;
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
import java.util.ArrayList;
import java.util.HashMap;

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
    private ArrayList<AID> receiver = new ArrayList<>();
    private ArrayList<Resource> availableResources = new ArrayList<>();
    private ArrayList<Resource> possibleMatches;

    protected void setup() {
        // Build the description used as template for the subscription
        DFAgentDescription dfd;

        ServiceDescription sd = new ServiceDescription();
        //sd.setType(role);
        sd.setName(getLocalName());
        // sd.addProperties(new Property("country", "Italy"));
        dfd = dfs.register(this);
        System.out.println("Agent "+getLocalName()+" waiting for CFP...");
        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));

        addBehaviour(new ContractNetResponder(this, template) {
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
                System.out.println("Agent " + getLocalName() + ": CFP received from " + cfp.getSender().getName() + ". Action is " + cfp.getContent());
                ArrayList<Resource> askedResources = getMsgResources(cfp);
                //Fetch funtion, writes to an ArrayList the resources available
                getAvailableResources();
                //Compare the resources asked with the ones available
                possibleMatches = compareAskedResourceWithAvailableOnes(askedResources, availableResources);
                if (!possibleMatches.isEmpty()) {
                    System.out.println("Agent " + getLocalName() + ": Searching for resources to lease ");
                    ArrayList<Resource> solutions = findBestSolution(possibleMatches);
                    System.out.println("Agent " + getLocalName() + ": Proposing ");
                    ACLMessage propose = cfp.createReply();
                    propose.setPerformative(ACLMessage.PROPOSE);
                    try {
                        propose.setContentObject(solutions);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return propose;
                } else {
                    // We refuse to provide a proposal
                    System.out.println("Agent " + getLocalName() + ": Refuse");
                    throw new RefuseException("Can't lease the resources asked");
                }
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,ACLMessage accept) throws FailureException {
                System.out.println("Agent "+getLocalName()+": Proposal accepted");
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
            }
        } );
    }

    private void getAvailableResources() {
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
    }

    private ArrayList<Resource> compareAskedResourceWithAvailableOnes(ArrayList<Resource> askedResources, ArrayList<Resource> availableResources) {
        possibleMatches = new ArrayList<>();
        for (Resource askedResource:askedResources) {
            for (Resource resource : availableResources) {
                if (!resource.compareResource(askedResource))
                    continue;
                else{
                    possibleMatches.add(resource);
                    System.out.println(resource + "Added to possibleMatches");
                }
            }
        }
        return possibleMatches;
    }

    private ArrayList<Resource> findBestSolution(ArrayList<Resource> possibleMatches) {
        /**
         * Chave Resource, Valor Double, uma vez que esta a receber uma lista de
         * Rescursos, assim encontra-se o recursos pretendido e manda-se para a stack/queue/list
         * E quando se encontrar um melhor remove-se o recurso a ser substituido.
         */
        HashMap<Resource,Double> utilitiesMap = new HashMap<Resource, Double>();
        /**
         * Aplica funçao de utilidade aos recursos encontrados e retorna o melhor.
         * Escolhe sempre os recursos cujos emprestimos tenham a maior utilidade para o seller
         * Neste caso escolhe o recurso pedido, uma vez que ainda nao ha funçoes para o calculo das utilidades
         * nem estão incluidos os tempos de partida e atraso dos voos.
         */
        if(possibleMatches.size() == 0)
            //Ver como proceder aqui. Seller abandona negociaçoes?
            System.out.println("Resource not available");
            //Only one possible match
        else{
            //for (Resource resource:possibleMatches){
            /**
             * Calculates utility
             * Adds resource to map
             */
            //}
            /**
             * Iterates through the Hashmap
             * Finds the resource with the highest utility
             * Adds it to a list
             * Returns a list with the resources to be negotiated
             */
        }
        return possibleMatches;
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
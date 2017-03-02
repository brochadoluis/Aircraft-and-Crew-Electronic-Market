package agents;

import utils.*;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Luis on 24/02/2017.
 */
public class MarketHandler {
    private MessageHandler msgHandler = new MessageHandler();
    private AID[] receivers;
    private ArrayList<Resource> resourcesNeeded;
    private ArrayList<Resource> availableResources = new ArrayList<>();
    private ArrayList<Resource> possibleMatches;
    /**
     * processPerformative method gets a message's performative and according to the message's performative,
     * the respective processing method is called
     * @param agent TO BE REMOVED
     * @param msg: ACL Message to be processed
     * @return
     */
    protected void processPerformative(Agent agent, ACLMessage msg, String role) {
        if ("JavaSerialization".equals(msg.getLanguage())) {
            System.out.println("MSG.GETPERFORMATIVE = " + msg.getPerformative());
            switch (msg.getPerformative()){
                /**
                 * PERFORMATIVE - Value
                 * ACCEPT PROPOSAL - 0
                 * CFP - 3
                 * NOT UNDERSTOOD - 10
                 * PROPOSE - 11
                 * REJECT PROPOSAL - 15
                 */
                case ACLMessage.ACCEPT_PROPOSAL:
                    System.out.println(agent.getLocalName() + " ACCEPT PROPOSAL RECEIVED ");
                    processAccept(agent,msg);
                    break;
                case ACLMessage.CFP:
                    System.out.println(agent.getLocalName() + " CFP RECEIVED ");
                    processCFP(agent,msg);
                    break;
                case ACLMessage.NOT_UNDERSTOOD:
                    System.out.println(agent.getLocalName() + " NOT UNDERSTOOD RECEIVED ");
                    //processNotUnderstood(msg);
                    break;
                case ACLMessage.PROPOSE:
                    System.out.println(agent.getLocalName() + " PROPOSE RECEIVED ");
                    processPropose(agent,msg,role);
                    break;
                case ACLMessage.REJECT_PROPOSAL:
                    System.out.println(agent.getLocalName() + " REJECT PROPOSAL RECEIVED ");
                    processReject(msg);
                    break;
                default:
                    System.out.println("Unknown Performative");
                    break;
            }
        } else{
            System.out.println("Unknown Language");
        }
    }

    /**
     * processCFP method gets the resources in the CFP received, processes it,
     * compares it to method invoker own resources and prepares a proposal
     * @param agent: TO BE REMOVED
     * @param msg: ACL message received by the method invoker. In this case, a CFP message
     * @return
     */
    private ACLMessage processCFP(Agent agent, ACLMessage msg) {
        msgHandler = new MessageHandler();
        ArrayList<Resource> askedResources = msgHandler.getMsgResources(agent,msg);
        //Fetch funtion, writes to an ArrayList the resources available
        getAvailableResources(agent);
        //Compare the resources asked with the ones available
        possibleMatches = compareAskedResourceWithAvailableOnes(askedResources,availableResources);

        ArrayList<Resource> solutions = findBestSolution(possibleMatches);
        /*System.out.println("SOLUTIONS : ");
        for (Resource sol:solutions) {
            sol.printResource();
        }*/
        /**
         * Fetch Resources in DataBase
         * Compare Resource form message with DataBase Fetch results
         * Select the most similar Resource found in DataBase, to the one asked
         * Compare both Resources
         * Calculate utility and price
         * To test, the resource found is defined below
         */
        /**
         * In the case of seller, use addReplyTo?
         */
        msgHandler.preparePropose(agent,receivers,solutions);
        return msg;
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

    //ESTA FUNCAO PROCESSA OS COMENTARIOS NA MENSAGEM, NO CASO DO SELLER, PARA MELHORAR A PROPOSTA
    /**
     * processPropose method evaluates the proposal received, improves it and proposes it again
     * until an agreement is reached
     * @param agent: TO BE REMOVED
     * @param msg: ACL message received by method invoker. In this case, a PROPOSE message
     *           it contains the resources under negotiation. In the case of being invoked
     *           by the BUYER role,the message contains only the resources and price.
     *           In the case of being invoked by the SELLER role, the message also contains
     *           the best offer from the previous round and some comments to help improving the
     *           receiver's proposal
     * @param role: Role played by the agent who invoked this method
     * @return
     */
    private ACLMessage processPropose(Agent agent, ACLMessage msg, String role) {
        msgHandler = new MessageHandler();
        /**
         * If Seller, evaluate ultility and comments
         * Use agentStrategy to improve proposal
         * If Buyer, evaluate all proposal's utility
         * selects the best and proposes to all Sellers but agent
         * the best proposal received.
         *
         */
        switch (role){
            case "Buyer":
                ArrayList<Resource> resourcesFromProposal;
                System.out.println("Sou o BUYER");
                /**
                 * cenas descritas acima
                 */
                //At this point, receivers list can only have one receiver
                //resourcesFromProposal may be useful if not all resources match. Check if it is really needed
                resourcesFromProposal = msgHandler.getMsgResources(agent,msg);
                // Auxiliar ArrayList to compare if proposed resources are equal to the ones needed
                ArrayList<Resource> resourcesProposed = msgHandler.getMsgResources(agent,msg);
                if(proposalMeetsNeeds(agent, resourcesProposed, resourcesNeeded))
                    msgHandler.prepareAccept(agent, receivers, resourcesNeeded);
                else
                    msgHandler.preparePropose(agent, receivers, resourcesNeeded);

                float totalPrice = 0;
                for (Resource r:msgHandler.getMsgResources(agent,msg)) {
                    totalPrice+= r.getPrice();
                }
                System.out.println("Proposal total cost = " +totalPrice );
                break;
            case "Seller":
                msgHandler.preparePropose(agent,receivers,msgHandler.getMsgResources(agent,msg));
                break;
            default:
                break;
        }
        return msg;
    }

    /**
     * processAccept method ends the negotiation when invoked.
     * @param agent: TO BE REMOVED
     * @param msg: ACL ACCEPT_PROPOSAL containing the agreed terms
     * @return
     */
    private ACLMessage processAccept(Agent agent, ACLMessage msg) {
        /**
         * Contratualize and Buyer dies
         */
        System.out.println("ACCEPT RECEIVED");
        System.out.println("I am " + agent.getLocalName());
        /**
         * Deregister, update database, register again
         */
        return msg;
    }

    /**
     * processReject method rejects a proposal and removes the proponent form the
     * list of potential sellers. If there is only one seller remaining in the market,
     * a REJECT_PROPOSAL implies that seller doesn't want to negotiate anymore, being the final
     * offer, the PROPOSE message sent after the REJECT_PROPOSAL
     * @param msg: ACL REJECT_PROPOSAL containing the proposal
     * @return
     */
    private ACLMessage processReject(ACLMessage msg) {
        /**
         * If there is only one Seller left, 2 consecutive rejects
         * imply that Buyer must accept current terms
         * Else, one reject removes the seller from sellers list (stops receiving messages)
         */
        return msg;
    }

    public AID[] getReceivers() {
        return receivers;
    }

    public void setReceivers(AID[] receivers) {
        this.receivers = receivers;
    }

    public void setResourcesNeeded(ArrayList<Resource>  resourcesMissing) {
        this.resourcesNeeded = resourcesMissing;
    }

    /**
     * getAvailableResources method searches in the Seller's database for available resources
     * and writes them directly to a Seller's available resources list
     * @param agent TO BE REMOVED
     */
    private void getAvailableResources(Agent agent) {
        System.out.println("Getting available resources in " + agent.getLocalName()) ;
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

    /**
     * compareAskedResourceWithAvailableOnes method, compares the resources asked by Buyer Agent with the resources available
     * for one Seller
     * @param askedResources: List of resources asked by Buyer
     * @param availableResources: List of Seller's all available resources
     * @return An Arraylist of resources, containing the resources that match with the ones asked
     */
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

    /**
     * proposalMeetsNeeds method compares the resources proposed by one Seller
     * to check if they match the resources asked in the market
     * @param agent
     * @param resourcesProposed: Clone List with resources read from ACL Message
     * @param resourcesAsked: List of resources missing
     * @return true if all resources match, false otherwise
     */
    private boolean proposalMeetsNeeds(Agent agent, ArrayList<Resource> resourcesProposed, ArrayList<Resource> resourcesAsked) {
        System.out.println("Resources Received: \n\n");
        System.out.println("RFP size = "+ resourcesProposed.size() );
        System.out.println("RA size = "+ resourcesAsked.size() );
        for(int i = 0; i < resourcesAsked.size(); i++){
            for(int j = 0; j < resourcesProposed.size(); j++){
                if(resourcesAsked.get(i).compareResource(resourcesProposed.get(j))){
                    resourcesProposed.remove(j);
                }
                else
                    continue;
            }
        }
        if(resourcesProposed.isEmpty())
            return true;

        else
            return false;
    }
}
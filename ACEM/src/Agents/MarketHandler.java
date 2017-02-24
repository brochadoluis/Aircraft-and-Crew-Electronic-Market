package Agents;

import Utils.Aircraft;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

/**
 * Created by Luis on 24/02/2017.
 */
public class MarketHandler {
    private MessageHandler msgHandler = new MessageHandler();
    private AID[] receivers;
    private Aircraft resourcesNeeded;
    private int i = 0;
    /**
     * processPerformative method gets a message's performative and according to the message's performative,
     * the respective processing method is called
     * @param agent TO BE REMOVED
     * @param msg: ACL Message to be processed
     * @return Resources contained in msg
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
            }
        } else{
            System.out.println("Unknown Performative");
        }
    }

    /**
     * processCFP method gets the resources in the CFP received, processes it,
     * compares it to method invoker own resources and prepares a proposal
     * @param sender: TO BE REMOVED
     * @param msg: ACL message received by the method invoker. In this case, a CFP message
     * @return
     */
    private ACLMessage processCFP(Agent sender, ACLMessage msg) {
        msgHandler = new MessageHandler();
        Aircraft a = msgHandler.getMsgResources(sender,msg);
        System.out.println("PROCESS CFP \n\n\nResources asked:\n");
        a.printAircraft();
        /**
         * Fetch Resources in DataBase
         * Compare Resourse form message with DataBase Fetch results
         * Select the most similar Resource found in DataBase, to the one asked
         * Compare both Resources
         * Calculate utility and price
         * To test, the resource found is defined below
         */
        Aircraft solution = new Aircraft("Airbus 747",400);
        /**
         * In the case of seller, use addReplyTo?
         */
        msgHandler.preparePropose(sender,receivers,solution);
        return msg;
    }

    //ESTA FUNCAO PROCESSA OS COMENTARIOS NA MENSAGEM, NO CASO DO SELLER, PARA MELHORAR A PROPOSTA
    /**
     * processPropose method evaluates the proposal received, improves it and proposes it again
     * until an agreement is reached
     * @param sender: TO BE REMOVED
     * @param msg: ACL message received by method invoker. In this case, a PROPOSE message
     *           it contains the resources under negotiation. In the case of being invoked
     *           by the BUYER role,the message contains only the resources and price.
     *           In the case of being invoked by the SELLER role, the message also contains
     *           the best offer from the previous round and some comments to help improving the
     *           receiver's proposal
     * @param role: Role played by the agent who invoked this method
     * @return
     */
    private ACLMessage processPropose(Agent sender, ACLMessage msg, String role) {
        msgHandler = new MessageHandler();
        /**
         * If Seller, evaluate ultility and comments
         * Use agentStrategy to improve proposal
         * If Buyer, evaluate all proposal's utility
         * selects the best and proposes to all Sellers but sender
         * the best proposal received.
         *
         */
        switch (role){
            case "Buyer":
                System.out.println("Sou o BUYER");
                /**
                 * cenas descritas acima
                 */
                //At this point, receivers list can only have one receiver
                if(msgHandler.getMsgResources(sender,msg).compareAircrafts(resourcesNeeded)){
                    msgHandler.prepareAccept(sender,receivers,resourcesNeeded);
                }
                msgHandler.preparePropose(sender,receivers,resourcesNeeded);
                break;
            case "Seller":
                System.out.println("Sou o SELLER\n");
                System.out.println("Recebi o seguinte recurso: \n");
                msgHandler.getMsgResources(sender,msg).printAircraft();
                if(i == 3){
                    Aircraft finalAircraft = new Aircraft("Boeing 777", 396);
                    msgHandler.preparePropose(sender,receivers,finalAircraft);
                }
                else
                    msgHandler.preparePropose(sender,receivers,msgHandler.getMsgResources(sender,msg));
                i++;
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

    public void setResourcesNeeded(Aircraft resourcesNeeded) {
        this.resourcesNeeded = resourcesNeeded;
    }

}
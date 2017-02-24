package Agents;

import Utils.Aircraft;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;


/**
 * Created by Luis on 21/02/2017.
 */
public class MessageHandler {

    public MessageHandler() {}
    /* processPerformative returns type Resources REMOVE AGENT PARAMETER AFTER DEBUG */
    /* Aircraft must be replaced by RESOURCES */
    /**
     * getMsgResources method converts the ACL message content
     * to Resource type
     * @param msg: ACL message to be procesed
     * @return Resources contained in msg
     */
    protected Aircraft getMsgResources(Agent agent, ACLMessage msg) {

        Aircraft aircraftToBeLeased = null;
        try {
            aircraftToBeLeased = (Aircraft) msg.getContentObject();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
//        aircraftToBeLeased.printAircraft();
        /*if(msg.getSender().getLocalName() != agent.getLocalName())
            System.out.println("Nao envio mensagens para mim");*/
        return aircraftToBeLeased;
    }

    /**
     * prepareCFP method prepares a Call-For-Proposal message
     * Adds a list of receiver agents to sender's agent receivers list
     * CFP message asks for proposals to lease a certain resource
     * @param sender: Agent who will send the Call-For-Proposal message
     * @param receivers: List of agents that will receive the Call-For-Proposal message
     * @param aircraftToBeLeased: Resource contained in the Call-For-Proposal message
     */
    protected void prepareCFP(Agent sender, AID [] receivers, Aircraft aircraftToBeLeased){

        try {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            System.out.println("Performative : " + cfp.getPerformative());
            for (AID receiver:receivers) {
                cfp.addReceiver(receiver);
            }
            cfp.setContentObject(aircraftToBeLeased);
            serializeAndSend(sender,cfp);
        } catch (IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * preparesPropose method prepares a Propose message
     * Adds receiver agent to sender's agent receiver list
     * Porpose specifies the terms of the proposal, to lease the resource resource or similar
     * @param sender: Agent who will send the Propose message
     * @param receivers: List of agents that will receive the propose message
     * @param aircraftToBeLeased: Resource contained in the Propose message
     */
    //AQUI PRECISA DOS COMENTARIOS A PROPOSTA FEITA, NULL SE FOR O SELLER A ENVIAR, COM CONTEUDO SE FOR O BUYER
    protected void preparePropose(Agent sender, AID[] receivers, Aircraft aircraftToBeLeased) {
        try {
            ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
            System.out.println("Performative : " + propose.getPerformative());
            for (AID receiver:receivers) {
                propose.addReceiver(receiver);
            }
            //msg.addReceiver(receivers);
            propose.setContentObject(aircraftToBeLeased);
            serializeAndSend(sender,propose);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * preparesAccept method prepares a Accept_Proposal message
     * Adds a receiver agent to sender's agent receiver list,
     * @param sender: Agent who will send the Accept_Proposal message
     * @param receivers: Agent that wins the negotiation
     * @param aircraftToBeLeased: Resource contained in the Propose message
     */
    public void prepareAccept(Agent sender, AID[] receivers, Aircraft aircraftToBeLeased) {
        try {
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            System.out.println("Performative : " + accept.getPerformative());
            for (AID receiver:receivers) {
                accept.addReceiver(receiver);
                System.out.println("Receiver = "+ receiver);
            }
            accept.setContentObject(aircraftToBeLeased);
            serializeAndSend(sender,accept);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    /**
     * serializeAndSend method serializes and sends an ACL message
     * @param sender TO BE REMOVED
     * @param msg: ACL message to be serialized and sent
     */
    protected void serializeAndSend(Agent sender,ACLMessage msg) {
        //System.out.println("MSG = "+ msg);
        msg.setLanguage("JavaSerialization");
        sender.send(msg);
        System.out.println("Message sent from " + sender.getLocalName());

        // System.out.println(agent.getLocalName()+" sent 1st msg "+msg);

        //msg.setDefaultEnvelope();
        //msg.getEnvelope().setAclRepresentation(FIPANames.ACLCodec.BITEFFICIENT);
        //sender.send(msg);
        //  System.out.println(agent.getLocalName()+" sent 1st msg with bit-efficient aclCodec "+msg);

        //msg.getEnvelope().setAclRepresentation(FIPANames.ACLCodec.XML);
        //sender.send(msg);
        //    System.out.println(agent.getLocalName()+" sent 1st msg with xml aclCodec "+msg);
    }
}

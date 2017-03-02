package agents;

import utils.*;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by Luis on 21/02/2017.
 */
public class MessageHandler {
    /**
     * getMsgResources method converts the ACL message content
     * to Resource type
     * @param msg: ACL message to be procesed
     * @return A list of resources contained in msg
     */
    protected ArrayList<Resource> getMsgResources(Agent agent, ACLMessage msg) {

        ArrayList<Resource> resourcesToBeLeased = null;
        try {
            resourcesToBeLeased = (ArrayList<Resource>) msg.getContentObject();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
        return resourcesToBeLeased;
    }

    /**
     * prepareCFP method prepares a Call-For-Proposal message
     * Adds a list of receiver agents to sender's agent receivers list
     * CFP message asks for proposals to lease a certain resource
     * @param sender: Agent who will send the Call-For-Proposal message
     * @param receivers: List of agents that will receive the Call-For-Proposal message
     * @param resourcesToBeLeased: Resources contained in the Call-For-Proposal message
     */
    protected void prepareCFP(Agent sender, AID [] receivers, ArrayList<Resource> resourcesToBeLeased){

        try {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            System.out.println("Performative : " + cfp.getPerformative());
            for (AID receiver:receivers) {
                cfp.addReceiver(receiver);
            }
            cfp.setContentObject(resourcesToBeLeased);
            serializeAndSend(sender,cfp);
        } catch (IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * preparesPropose method prepares a PROPOSE message
     * Adds receiver agent to sender's agent receivers list
     * PROPOSE specifies the terms of the proposal, to lease the resource resource or similar
     * @param sender: Agent who will send the Propose message
     * @param receivers: List of agents that will receive the propose message
     * @param resourcesToBeLeased: Resources contained in the Propose message
     */
    protected void preparePropose(Agent sender, AID[] receivers, ArrayList<Resource> resourcesToBeLeased) {
        try {
            ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
            System.out.println("Performative : " + propose.getPerformative());
            for (AID receiver:receivers) {
                propose.addReceiver(receiver);
            }
            System.out.println("Preparing Proposal in "+ sender.getLocalName() );
            propose.setContentObject(resourcesToBeLeased);
            serializeAndSend(sender,propose);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * preparesAccept method prepares an ACCEPT_PROPOSAL message
     * Adds a receiver agent to sender's agent receiver list,
     * @param sender: Agent who will send the Accept_Proposal message
     * @param receivers: Agent that wins the negotiation
     * @param resourcesToBeLeased: Resources contained in the Propose message
     */
    public void prepareAccept(Agent sender, AID[] receivers, ArrayList<Resource> resourcesToBeLeased) {
        try {
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            System.out.println("Performative : " + accept.getPerformative());
            for (AID receiver:receivers) {
                accept.addReceiver(receiver);
                System.out.println("Receiver = "+ receiver);
            }
            accept.setContentObject(resourcesToBeLeased);
            serializeAndSend(sender,accept);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    //public void prepareReject(Agent sender, AID[] receivers, ArrayList<Resource> resourcesToBeLeased)

    /**
     * serializeAndSend method serializes and sends an ACL message
     * @param sender TO BE REMOVED
     * @param msg: ACL message to be serialized and sent
     */
    protected void serializeAndSend(Agent sender,ACLMessage msg) {
        msg.setLanguage("JavaSerialization");
        System.out.println("Performative to send : "+ msg.getPerformative() + " in " + sender.getLocalName());
        sender.send(msg);
        System.out.println("Message sent from " + sender.getLocalName());
        if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
            System.out.println("msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL " +( msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL));
            sender.doDelete();
        }

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

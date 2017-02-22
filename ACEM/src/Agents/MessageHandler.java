package Agents;

import Utils.Aircraft;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.IOException;


/**
 * Created by Luis on 21/02/2017.
 */
public class MessageHandler {

    public MessageHandler() {}

    protected void receiveMsg(Agent agent, ACLMessage msg){

        try {
            System.out.println(agent.getLocalName() + " received msg" + msg);

            if ("JavaSerialization".equals(msg.getLanguage())) {
                Aircraft a1 = (Aircraft) msg.getContentObject();
                String performative = Integer.toString(msg.getPerformative());
                System.out.println(agent.getLocalName() + " STRING PERFORMATIVE = " + performative + " AND INTEGER PERFORMATIVE = " + msg.getPerformative());
                Aircraft a = new Aircraft(a1.getType(), a1.getCapacity());
                System.out.println(agent.getLocalName() + " read Java Object " + a1.getClass().getName() + " and " + a1.toString());
                System.out.println(agent.getLocalName() + "Aircraft A:");
                a.printAircraft();
                System.out.println("\n\n" + agent.getLocalName() + " Aircraft A1: Type = " + a1.getType() + " and Capacity = " + a1.getCapacity());
                a.printAircraft();
            } else
                System.out.println(agent.getLocalName() + " read Java String " + msg.getContent());
        } catch (UnreadableException e3) {
            System.err.println(agent.getLocalName() + " catched exception " + e3.getMessage());
        }
    }

    protected void sendCFP(Agent agent, AID [] sellers, Aircraft aircraft){


        try {
            ACLMessage msg = new ACLMessage(ACLMessage.CFP);

            for (AID seller:sellers) {
                msg.addReceiver(seller);
            }msg.setContentObject(aircraft);
            System.out.println("MSG = "+ msg);
            msg.setLanguage("JavaSerialization");
            agent.send(msg);
            System.out.println(agent.getLocalName()+" sent 1st msg "+msg);

            msg.setDefaultEnvelope();
            msg.getEnvelope().setAclRepresentation(FIPANames.ACLCodec.BITEFFICIENT);
            agent.send(msg);
            System.out.println(agent.getLocalName()+" sent 1st msg with bit-efficient aclCodec "+msg);

            msg.getEnvelope().setAclRepresentation(FIPANames.ACLCodec.XML);
            agent.send(msg);
            System.out.println(agent.getLocalName()+" sent 1st msg with xml aclCodec "+msg);

        } catch (IOException e ) {
            e.printStackTrace();
        }
    }
}

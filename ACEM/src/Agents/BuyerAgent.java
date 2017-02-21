package Agents;
/*


import Utils.CrewMember;
import Utils.Resources;
import jade.core.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;
import jade.util.leap.Iterator;
import sun.management.Agent;
import Utils.Date;
import java.lang.String;
*/
import Utils.*;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Luis on 20/02/2017.
 */
public class BuyerAgent extends Agent{
    /**
     * Resources to search in the EM: @missingResources
     * Company's Identifier: @company
     */
    private ArrayList<Aircraft> missingResources = new ArrayList<>();
    private float resourcesCost;
    private Date scheduledDeparture, flightDuration, flightDelay;
    private final String role = "Buyer";
    private String company = "";

    protected void setup()
    {
       // initiateParameters();
        DFAgentDescription dfd;
        DFServices dfs = new DFServices();
        ServiceDescription sd  = new ServiceDescription();
        sd.setType(role);
        sd.setName( getLocalName() );
        //sd.addProperties(new Property("country", "Italy"));
        dfd = dfs.register(sd, this);

        AID agent = dfs.getService("Buyer", this);
        System.out.println("\nAgents: "
                +(agent==null ? "not Found" : agent.getName()));
        System.out.println("A procurar sellers");

        agent = dfs.getService("Seller",this);
        System.out.println("\nSeller: "
                +(agent==null ? "not Found" : agent.getName()));
        /* Saves to an array all sellers registered in DFService  */
        System.out.println("Uma lista bonita de sellers");
        AID [] sellers = dfs.searchDF("Seller", this);
        System.out.print("\nSELLERS: ");
        for (AID seller:sellers){
            // Mudar para comparar a companhia. Se for igual não entra para a lista de buyers
            if(!seller.getLocalName().equals(sd.getName())){
                System.out.print( seller.getLocalName() + ",  ");
            }
        }
        /*for (int i=0; i<sellers.length; i++){
            // Mudar para comparar a companhia. Se for igual não entra para a lista de buyers
            if(!sellers[i].getLocalName().equals(sd.getName())){
                System.out.print( sellers[i].getLocalName() + ",  ");
            }
        }*/
        Aircraft a1 = new Aircraft("Boeing 777", 396);
        System.out.println("\nAircraft Missing: " + "Type = " + a1.getType() + " and Capacity = " + a1.getCapacity());
        System.out.println("\nAircraft Missing: " + a1);
        System.out.println();
        /* Messaging */
        AID reader = new AID();
        try {
            ACLMessage msg = new ACLMessage(ACLMessage.CFP);

            for (AID seller:sellers) {
                msg.addReceiver(seller);
            }
            /*for(Integer i = 0; i < sellers.length; i++){
                msg.addReceiver(sellers[i]);
            }*/


            msg.setContentObject(a1);
            msg.setLanguage("JavaSerialization");
            send(msg);
            System.out.println(getLocalName()+" sent 1st msg "+msg);

            msg.setDefaultEnvelope();
            msg.getEnvelope().setAclRepresentation(FIPANames.ACLCodec.BITEFFICIENT);
            send(msg);
            System.out.println(getLocalName()+" sent 1st msg with bit-efficient aclCodec "+msg);

            msg.getEnvelope().setAclRepresentation(FIPANames.ACLCodec.XML);
            send(msg);
            System.out.println(getLocalName()+" sent 1st msg with xml aclCodec "+msg);
/* Enviou, bloqueia em espera por respostas = sellers.lenght */
        } catch (IOException e ) {
            e.printStackTrace();
        }

        doDelete();
        System.exit(0);
    }

    private void initiateParameters() {
        /**
         * Connect to DB and read values
         */
    }


}



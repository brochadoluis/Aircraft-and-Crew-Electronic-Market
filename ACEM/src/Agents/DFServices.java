package Agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;

import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import jade.util.leap.Iterator;

/**
 * Created by Luis on 21/02/2017.
 */
public class DFServices {


    protected void takeDown(Agent a)
    {
        try { DFService.deregister(a); }
        catch (Exception e) {}
    }

// -------------------- Utility methods to access DF ----------------

    protected DFAgentDescription register(ServiceDescription sd, Agent a)
//  --------------------------------------
    {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(a.getAID());
        dfd.addServices(sd);

        try {
            DFAgentDescription list[] = DFService.search( a, dfd );
            if ( list.length>0 )
                DFService.deregister(a);

            dfd.addServices(sd);
            DFService.register(a,dfd);
            System.out.println(a.getLocalName()+ " succeeded in registration with DF");
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            System.err.println(a.getLocalName() + " registration with DF unsucceeded. Reason: " + fe.getMessage());
        }
        return dfd;
    }

    protected AID getService(String service, Agent a)
//  ---------------------------------
    {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType( service );
        dfd.addServices(sd);
        try
        {
            DFAgentDescription[] result = DFService.search(a, dfd);
            if (result.length>0)
                return result[0].getName() ;
        }
        catch (FIPAException fe) { fe.printStackTrace(); }
        return null;
    }

    protected AID [] searchDF( String service, Agent a )
//  ---------------------------------
    {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType( service );
        dfd.addServices(sd);

        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxResults(new Long(-1));

        try
        {
            DFAgentDescription[] result = DFService.search(a, dfd, ALL);
            AID[] agents = new AID[result.length];
            for (int i=0; i<result.length; i++)
                agents[i] = result[i].getName() ;
            return agents;

        }
        catch (FIPAException fe) { fe.printStackTrace(); }

        return null;
    }

    protected void subscription ( ServiceDescription sd, DFAgentDescription dfd, Agent a){

        SearchConstraints sc = new SearchConstraints();
        // We want to receive all results
        sc.setMaxResults(new Long(-1));

        a.addBehaviour(new SubscriptionInitiator(a, DFService.createSubscriptionMessage(a, a.getDefaultDF(), dfd, sc)) {
            protected void handleInform(ACLMessage inform) {
                System.out.println("Agent "+a.getLocalName()+": Notification received from DF");
                try {
                    System.out.println("Percebo 0 disto eheheh");
                    DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
                    if (results.length > 0) {
                        System.out.println("Results > 0");
                        for (int i = 0; i < results.length; ++i) {
                            System.out.println("A escolher o servico");
                            DFAgentDescription dfd = results[i];
                            AID provider = dfd.getName();
                            System.out.println("DFD NAME : " + dfd.getName());
                            System.out.println("SD TYPE : " + sd.getType());
                            // The same agent may provide several services; we are only interested
                            // in the weather-forcast one
                            Iterator it = dfd.getAllServices();
                            while (it.hasNext()) {
                                ServiceDescription sd = (ServiceDescription) it.next();
                                if (sd.getType().equals("EM")) {
                                    System.out.println("Seller service for Italy found:");
                                    System.out.println("- Service \""+sd.getName()+"\" provided by agent "+provider.getName());
                                }
                            }
                        }
                    }
                    System.out.println();
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        } );
    }
}

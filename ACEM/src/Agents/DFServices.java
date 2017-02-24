package Agents;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;


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

    protected DFAgentDescription register(ServiceDescription sd, Agent agent)
//  --------------------------------------
    {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(agent.getAID());
        dfd.addServices(sd);

        try {
            DFAgentDescription list[] = DFService.search( agent, dfd );
            if ( list.length>0 )
                DFService.deregister(agent);

            dfd.addServices(sd);
            DFService.register(agent,dfd);
            System.out.println(agent.getLocalName()+ " succeeded in registration with DF");
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            System.err.println(agent.getLocalName() + " registration with DF unsucceeded. Reason: " + fe.getMessage());
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
}
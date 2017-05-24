package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.ArrayList;
import java.util.Objects;


/**
 * Created by Luis on 21/02/2017.
 */
public class DFServices {


    protected void takeDown(Agent a) {
        System.out.println("Deregister " + a.getLocalName() +" from DFS. Bye...");
        // Deregister from the yellow pages
        try {
            DFService.deregister(a);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

// -------------------- Utility methods to access DF ----------------

    protected DFAgentDescription register(Agent a)    {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(a.getAID());
        //dfd.addServices(sd);

        try {
            DFAgentDescription list[] = DFService.search(a, dfd);
            if ( list.length>0 )
                DFService.deregister(a);

            //dfd.addServices(sd);
            DFService.register(a,dfd);
            System.out.println(a.getLocalName()+ " succeeded in registration with DF");
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
            System.err.println(a.getLocalName() + " registration with DF unsucceeded. Reason: " + fe.getMessage());
        }
        return dfd;
    }

    protected AID getService(Agent a)    {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        //sd.setType( service );
        //dfd.addServices(sd);
        try
        {
            DFAgentDescription[] result = DFService.search(a, dfd);
            if (result.length>0)
                return result[0].getName() ;
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return null;
    }

    protected ArrayList<AID> searchDF(Agent a)    {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        //sd.setType( service );
        //dfd.addServices(sd);

        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxResults(new Long(-1));

        try
        {
            DFAgentDescription[] result = DFService.search(a, dfd, ALL);
            ArrayList<AID> agents = new ArrayList<>();
            for (int i=0; i<result.length; i++) {
                if (Objects.equals(result[i].getName(), a.getAID())) {
                    continue;
                }
                else
                    agents.add(result[i].getName());
            }
            return agents;
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return null;
    }

    protected ArrayList<AID> searchAllSellers(Agent a)    {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        //sd.setType( service );
        //dfd.addServices(sd);

        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxResults(new Long(-1));

        try
        {
            DFAgentDescription[] result = DFService.search(a, dfd, ALL);
            ArrayList<AID> agents = new ArrayList<>();
            for (int i=0; i<result.length; i++) {
                agents.add(result[i].getName());
            }
            return agents;
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return null;
    }
}
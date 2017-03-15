import agents.BuyerAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
/**
 * Created by Luis on 22/02/2017.
 */
public class Market {

    public static void main(String[] args) throws StaleProxyException {

        // Get a hold on JADE runtime
        Runtime rt = Runtime.instance();

        // Exit the JVM when there are no more containers around
        rt.setCloseVM(true);
        System.out.print("runtime created\n");

        // Create a default profile
        Profile profile = new ProfileImpl(null, 1200, null);
        System.out.print("profile created\n");

        System.out.println("Launching a whole in-process platform..." + profile);
        AgentContainer mainContainer = rt.createMainContainer(profile);

        // now set the default Profile to start a container
        ProfileImpl pContainer = new ProfileImpl(null, 1200, null);
        System.out.println("Launching the agent container ..." + pContainer);

        AgentContainer cont = rt.createAgentContainer(pContainer);
        System.out.println("Launching the agent container after ..." + pContainer);

        System.out.println("containers created");
        System.out.println("Launching the rma agent on the main container ...");
        AgentController seller1 = mainContainer.createNewAgent("Seller1",
                "agents.SellerAgent", new Object[0]);
        AgentController seller2 = mainContainer.createNewAgent("Seller2",
                "agents.SellerAgent", new Object[0]);
        AgentController seller3 = mainContainer.createNewAgent("Seller3",
                "agents.SellerAgent", new Object[0]);
        AgentController seller4 = mainContainer.createNewAgent("Seller4",
                "agents.SellerAgent", new Object[0]);
        AgentController seller5 = mainContainer.createNewAgent("Seller5",
                "agents.SellerAgent", new Object[0]);

        Object[] arguments = new Object[1];
        arguments[0] = 100000.12;
        AgentController buyer = mainContainer.createNewAgent("Buyer",
                "agents.BuyerAgent", arguments);
        //AgentController buyer2 = mainContainer.createNewAgent("Buyer2",
          //      "agents.BuyerAgent", new Object[0]);

        seller1.start();
        //seller2.start();
        //seller3.start();
        //seller4.start();
        //seller5.start();
        buyer.start();
        //buyer2.start();
    }
}
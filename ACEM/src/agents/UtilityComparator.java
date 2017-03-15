package agents;

import utils.Resource;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by Luis on 03/03/2017.
 */

/**
 *  Nao sera um comparator de double? Visto que a utilidade sera um double
 */
public class UtilityComparator implements Comparator<ArrayList<Resource>> {
    private SellerAgent sa = new SellerAgent();
    @Override
    public int compare(ArrayList<Resource> resources1, ArrayList<Resource> resources2) {
        double r1TotalPrice, r2TotalPrice;
        r1TotalPrice = sa.sumResourcesPrice(resources1);
        r2TotalPrice = sa.sumResourcesPrice(resources2);

        if(r1TotalPrice < r2TotalPrice)
            return 1;
        if(r1TotalPrice > r2TotalPrice)
            return -1;

        return 0;
    }
}

package agents;

import utils.Flight;

import java.util.Comparator;

/**
 * Created by Luis on 03/03/2017.
 */

/**
 *  Nao sera um comparator de double? Visto que a utilidade sera um double
 */
public class UtilityComparator implements Comparator<Flight> {
    private SellerAgent sa = new SellerAgent();

    @Override
    public int compare(Flight f1, Flight f2) {
        double r1TotalPrice, r2TotalPrice;
        r1TotalPrice = sa.sumResourcesPrice(f1);
        r2TotalPrice = sa.sumResourcesPrice(f1);

        if(r1TotalPrice < r2TotalPrice)
            return 1;
        if(r1TotalPrice > r2TotalPrice)
            return -1;

        return 0;
    }
}

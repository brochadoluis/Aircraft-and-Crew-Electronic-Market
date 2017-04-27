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

    @Override
    public int compare(Flight aFlight, Flight anotherFlight) {
        double aFlightTotalPrice, anotherFlighTotalPrice;
        aFlightTotalPrice = aFlight.getPrice();
        anotherFlighTotalPrice = anotherFlight.getPrice();

        if(aFlightTotalPrice < anotherFlighTotalPrice)
            return 1;
        if(aFlightTotalPrice > anotherFlighTotalPrice)
            return -1;

        return 0;
    }
}

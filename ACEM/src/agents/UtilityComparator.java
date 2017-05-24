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
//        double aFlightTotalPrice, anotherFlightTotalPrice;
        long aFlightAvailability, anotherFlightAvailability;
//        aFlightTotalPrice = aFlight.getPrice();
//        anotherFlightTotalPrice = anotherFlight.getPrice();
        aFlightAvailability = aFlight.getAvailability();
        anotherFlightAvailability = anotherFlight.getAvailability();

        if (aFlightAvailability < anotherFlightAvailability)
            return -1;

        if (aFlightAvailability > anotherFlightAvailability)
            return 1;

        return 0;
}
}

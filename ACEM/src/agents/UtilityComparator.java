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
public class UtilityComparator implements Comparator<ArrayList<ArrayList<Resource>>> {

    @Override
    public int compare(ArrayList<ArrayList<Resource>> resources1, ArrayList<ArrayList <Resource>> resources2) {
        ArrayList<ArrayList<Resource>> proposed1 = resources1;
        ArrayList<ArrayList<Resource>> proposed2 = resources2;
        /**
         * Calculate Utilities and if p1 is higher than p2, return 1, return false otherwise?
         */
        return 0;
    }

/*    @Override
    public int compare(Object o1, Object o2) {

        return 0;
    }*/
}

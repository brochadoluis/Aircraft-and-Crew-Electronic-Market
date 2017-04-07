package cbr;

import utils.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Luis on 04/04/2017.
 */
public class CBR {
    private Data data;
    private ArrayList<String> features = new ArrayList<>();
    // Feature Name, Weight
    private HashMap<String,Integer> featuresWeights = new HashMap<String, Integer>();
    private final int OK = 0;
    private final int LOWER = 1;
    private final int MUCH_LOWER = 2;
    private Log log;
    private double mostSimilarCaseDistance;

    /**
     *
     * TODO: READ FEATURES WEIGHTS/IMPORTANCE AS AGENT ARGUMENT
     */


    public CBR(Data data) {
        this.data = data;
        log = new Log("CBR Log");
    }

    public Data getData() {
        return data;
    }

    protected void setData(Data data) {
        this.data = data;
    }

    protected void setFeatures(ArrayList<String> feats){
        for(int i = 0; i < data.getNumFeatures(); i++){
            features.add(feats.get(i));
        }
        fillWeights(feats);
    }

    public ArrayList<String> getFeatures() {
        return features;
    }

    /**
     * calculateEuclideanDistance calculates the Euclidean Distance between the current case and the case passed as a second argument
     * @param newCase: An ArrayList containing the current case
     * @param dataSetCase: An ArrayList containing the current case read from the dataSet
     * @return: Euclidean Distance between the two arguments
     */
    private double calculateEuclideanDistance(ArrayList<String> newCase, ArrayList<String> dataSetCase){
        double sum = 0.0;
        double euclideanDistance;
        ArrayList<Integer> newCaseInt = CommentsToInt(newCase);
        ArrayList<Integer> dataSetCaseInt = CommentsToInt(dataSetCase);
        for(int i = 0; i < newCaseInt.size(); i++){
//            log.write(" newCase.get(i) " + newCase.get(i));
//            log.write(" data.get(0).get(i) " + data.getLine(0).get(i));
            log.write("(newCaseInt.get(i) - dataSetCaseInt.get(i) " + (newCaseInt.get(i) - dataSetCaseInt.get(i)));
            System.out.println("featuresWeights = " + featuresWeights);
            System.out.println("newCaseInt.get(i) = " + newCaseInt.get(i));
            System.out.println("dataSetCaseInt.get(i),2.0) = " + dataSetCaseInt.get(i));
            System.out.println("dataSetCase.get(i) = " + dataSetCase.get(i));
            System.out.println("featuresWeights.get(dataSetCase.get(i) = " + featuresWeights.get(data.getLine(0).get(i)));
            sum += Math.pow(newCaseInt.get(i) - dataSetCaseInt.get(i),2.0) * featuresWeights.get(data.getLine(0).get(i));

        }
        euclideanDistance = Math.sqrt(sum);
        log.write("Euclidean Distance = " + euclideanDistance);

        return euclideanDistance;
    }

    /**
     * getBestEuclideanDistance calculates the Euclidean Distance among newCase and all the cases in the dataSet
     * @param newCase: An ArrayList containing the current case
     * @param dataSet: Data set
     * @return: The index of the most similar case to newCase
     */
    protected int getBestEuclideanDistance(ArrayList<String> newCase, Data dataSet){
        ArrayList<Double> euclideanDistances = new ArrayList<>();
        System.out.println("Data Set Size + " + dataSet.getSize());
        for(int i = 1; i < dataSet.getSize(); i++){
            System.out.println("dataSet.getLine ( " + i + " ) " +dataSet.getLine(i) );
            euclideanDistances.add(calculateEuclideanDistance(newCase,dataSet.getLine(i)));
        }
        //find lowest value index
        System.out.println("Euclidean Distanves size = " + euclideanDistances.size());
        int index = findLowest(euclideanDistances);
        mostSimilarCaseDistance = euclideanDistances.get(index);
        System.out.println("most silmiar distance = " + mostSimilarCaseDistance);
        System.out.println("Index in get best euclidiean disncaesd = " + index);
        return index;
    }

    private int findLowest(ArrayList<Double> euclideanDistances) {
        int index = 0;
        System.out.println("euclidean distances size  = " + euclideanDistances.size());
        double min = euclideanDistances.get(index);
        System.out.println("Min = " + min);
        for(int i = 0; i < euclideanDistances.size(); i++){
            System.out.println("euclideanDistances.size() = "+ euclideanDistances.size());
            System.out.println("euclideanDistances.get " + i + " " + euclideanDistances.get(i));
            if(euclideanDistances.get(i) < min){
                min = euclideanDistances.get(i);
                index = i;
                System.out.println("i = " + i);
                System.out.println("Index = " + index);
                System.out.println("Minimum = " + min);
            }
            else
                continue;
        }
        System.out.println("Indezx = " + index);
        return index;
    }

    protected double getEuclideanDistance(){
        return mostSimilarCaseDistance;
    }

    private ArrayList<Integer> CommentsToInt(ArrayList<String> aCase) {
        ArrayList<Integer> commentValues = new ArrayList<>();
        for(int i = 0; i < data.getNumFeatures(); i++){
            log.write("aCase size " + aCase.get(i));
            switch (aCase.get(i)){
                case "OK":
                    commentValues.add(0);
                    break;
                case "LOWER":
                    commentValues.add(1);
                    break;
                case "MUCH LOWER":
                    commentValues.add(2);
                    break;
                default:
//                    commentValues.add(Integer.parseInt(aCase.get(i)));
                    break;
            }
        }
        return commentValues;
    }

    protected void fillWeights(ArrayList<String> features){
        for(int i = 0; i < features.size(); i++){
            switch (features.get(i)){
                case "Price Comment":
                    featuresWeights.put(features.get(i),2);
                    break;
                case "Availability Comment":
                    featuresWeights.put(features.get(i),1);
                    break;
                case "Number of Sellers":
                    featuresWeights.put(features.get(i),4);
                    break;
            }
        }
        log.write("features weights " + featuresWeights);
    }
}

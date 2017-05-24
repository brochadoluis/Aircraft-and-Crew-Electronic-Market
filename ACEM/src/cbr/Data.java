package cbr;

import utils.Log;

import java.util.ArrayList;

/**
 * Created by Luis on 03/04/2017.
 */
public class Data {
    // Case , Data
    private ArrayList<ArrayList<String> > data;
    private int lineSize = 0;
    private final int numFeatures = 3;
    private CBR cbr;
    private Log log;

    public Data(ArrayList<ArrayList<String>> data) {
        if(data == null){
            this.data = new ArrayList<>();
            setHeader();
            cbr = new CBR(this);
            log = new Log("Data Log");

        }
        else{
            this.data = data;
            cbr = new CBR(this);
            log = new Log("Data Log");
        }
    }

    public Data() {
        this.data = new ArrayList<>();
        cbr = new CBR(this);
        setHeader();
    }

    public void setHeader(){
        ArrayList<String> header = new ArrayList<>();
        header.add("Price Comment");
        header.add("Availability Comment");
        header.add("Number of Sellers");
        header.add("Price Action");
        header.add("Availability Action");
        header.add("Evaluation");
        data.add(header);
        this.lineSize = data.get(0).size();
        cbr.setFeatures(header);
    }

    public ArrayList<String> getHeader(){
        return data.get(0);
    }

    public void addCase(ArrayList<String> caseString) {
        if(caseString.size() <= lineSize) {
            ArrayList<String> line = new ArrayList<>();
            for(int i = 0; i < caseString.size(); i++){
                line.add(caseString.get(i));
            }
            data.add(line);
        }
        setCBRData();
    }

    /**
     *
     * @param outcome: Outcome of an action
     * @return: the index of the case that had it's outcome added
     */
    public int addOutcome(double outcome){
        int index = 0;
        for (int i = 1; i < data.size(); i++) {
            if(data.get(i).get(lineSize-1).equals("-1")){
                data.get(i).remove(lineSize-1);
                data.get(i).add(String.valueOf(outcome));
                index = i;
            }
            else
                continue;
        }
        setCBRData();

        return index;
    }

    /**
     * Update only in the last iteration, or update after each round?
     * @param newOutcome
     * @param line
     * @return
     */
    public double recencyOutcome(double newOutcome,int line){
        double oldOutcome = Double.parseDouble(data.get(line).get(lineSize-1));
        data.get(line).remove(lineSize-1);
        //This way, the outcome has 3 decimal places
        double updatedOutcome = ((oldOutcome * 0.2 + newOutcome * 0.8));//*1000)/1000;
        data.get(line).add(String.valueOf(updatedOutcome));
        return ((oldOutcome * 0.2 + newOutcome * 0.8));//*1000)/1000;
    }

    public ArrayList<Integer> getEuclideanDistances(ArrayList<String> newCase){
        return cbr.getEuclideanDistances(newCase,this);
    }

    /**
     * Replace this method with simulated annealing
     * simulated annealing copied from here
     * http://www.theprojectspot.com/tutorial-post/simulated-annealing-algorithm-for-beginners/6
     * @param indexes
     * @return
     */
    public int getBestEvaluation(ArrayList<Integer> indexes) {
        int index = -1;
        double maximum = 0.0;
        for (int i = 0; i < indexes.size(); i++){
            System.out.println("Indexes at " + i + " = " +indexes.get(i) );
            double currentEvaluation = Double.parseDouble(getOutcome(indexes.get(i)));
            if(index < 0 && maximum <= currentEvaluation){
                maximum = currentEvaluation;
                index = indexes.get(i);
            }
            if(maximum < currentEvaluation){
                maximum = currentEvaluation;
                index = indexes.get(i);
            }
            else
                continue;
        }
        return index;
    }

    public int getCaseToApply(ArrayList<Integer> indexes){
        int index = -1;
        double temperature = 100;
        double coolingRate = 0.3;


        return index;
    }

    public int getSize() {
        return data.size();
    }

    public ArrayList<String> getLine(int index) {
        return data.get(index);
    }

    public int getLineSize() {
        return lineSize;
    }

    public String getString(int outerIndex, int innerIndex) {
        return data.get(outerIndex).get(innerIndex);
    }

    public int getNumFeatures() {
        return numFeatures;
    }

    public ArrayList<String> getAction(int line){
        ArrayList<String> action = new ArrayList<>();
        //Action taken on Price
        action.add(data.get(line).get(lineSize-3));
        //Action taken on Availability
        action.add(data.get(line).get(lineSize-2));

        return action;
    }

    public boolean checkIfActionExists(ArrayList<String> action, ArrayList<Integer> indexes){
        for (int i = 0; i < indexes.size(); i++){
            int index = indexes.get(i);
            ArrayList<String> caseAction = new ArrayList<>();
            caseAction.add(data.get(index).get(lineSize-3));
            System.out.println("data.get(index).get(lineSize-3) " + data.get(index).get(lineSize-3) );
            caseAction.add(data.get(index).get(lineSize-2));
            System.out.println("data.get(index).get(lineSize-2) " + data.get(index).get(lineSize-2));
            if (action.get(0).equalsIgnoreCase(caseAction.get(0)) && action.get(1).equalsIgnoreCase(caseAction.get(1)))
                return true;
        }
        return false;
    }

    public String getOutcome(int line){
        return data.get(line).get(lineSize - 1);
    }

    public double getEuclideanDistance(int index){
        return cbr.getEuclideanDistance(index);
    }

    public ArrayList<String> getNFeatures(int numberOfFeaturesToReturn, int line) {
        ArrayList<String> featuresRequired = new ArrayList<>();
        for(int i = 0; i < numberOfFeaturesToReturn; i++){
            featuresRequired.add(data.get(line).get(i));
        }
        return featuresRequired;
    }

    private void setCBRData() {
        cbr.setData(this);
    }
}
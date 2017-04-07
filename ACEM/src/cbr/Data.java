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
        System.out.println("Adding case");
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
            for(int j = 0; j < lineSize; j++){
                if(data.get(i).get(j) == "-1"){
                    data.get(i).remove(j);
                    data.get(i).add(String.valueOf(outcome));
                    index = i;
                }
                else
                    continue;
            }
        }
        setCBRData();
        System.out.println("INDEX ===== " + index);
        return index;
    }

    public int getBestEuclideanDistance(ArrayList<String> newCase){
        return cbr.getBestEuclideanDistance(newCase,this);
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
        System.out.println("data = " + data);
        System.out.println("OuterIdnex - " + outerIndex);
        System.out.println("InnerIndex - " + innerIndex);
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

    public String getOutcome(int line){
        return data.get(line).get(lineSize - 1);
    }

    public double getEuclideanDistance(){
        return cbr.getEuclideanDistance();
    }

    public ArrayList<String> getNFeatures(int numberOfFeaturesToReturn, int line) {
        ArrayList<String> featuresRequired = new ArrayList<>();
        for(int i = 0; i < numberOfFeaturesToReturn; i++){
            System.out.println("Line = " + line);
            featuresRequired.add(data.get(line).get(i));
        }
        return featuresRequired;
    }

    private void setCBRData() {
        cbr.setData(this);
    }
}

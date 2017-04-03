package cbr;

import java.util.ArrayList;

/**
 * Created by Luis on 03/04/2017.
 */
public class Data {
    // Case , Data
    private ArrayList<ArrayList<String> > data;
    private int lineSize = 0;

    public Data(ArrayList<ArrayList<String>> data) {
        if(data == null){
            this.data = new ArrayList<>();
            setHeader();
        }
        else{
            this.data = data;
        }
    }

    public Data() {
        this.data = new ArrayList<>();
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
    }

    public void addOutcome(int outcome){
        for (int i = 1; i < data.size(); i++) {
            for(int j = 0; j < lineSize; j++){
                if(data.get(i).get(j) == null){
                    data.get(i).remove(j);
                    data.get(i).add(String.valueOf(outcome));
                }
            }
        }
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
}

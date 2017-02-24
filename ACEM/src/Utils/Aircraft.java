package Utils;

import java.io.Serializable;

/**
 * Created by Luis on 20/02/2017.
 */
public class Aircraft extends Resources implements Serializable{
    /**
     * Needed aircraft type : @type
     * Needed aircraft capacity : @capacity
     */
    private String type;
    private Integer capacity;

    public Aircraft(String type, Integer capacity) {
        this.type = type;
        this.capacity = capacity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void printAircraft(){
        System.out.println("Aircraft Type = " + this.type +" and Aircraft Capacity =  " + this.capacity);
    }
    public boolean compareAircrafts(Aircraft a1){
        if(a1.getType().equals(this.getType()) && a1.getCapacity().equals(this.getCapacity()))
            return true;
        else
            return false;
    }
}

package utils;

import jade.core.AID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.LongToDoubleFunction;

/**
 * Created by Luis on 20/02/2017.
 */
public class Aircraft implements Serializable {
    /**
     * Needed aircraft type : @type
     * Needed aircraft capacity : @capacity
     */
    private String type;
    private Integer capacity;
    private Long aircraftAvailability = null;

    public Aircraft(){}

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

    public Long getAvailability() {
        return aircraftAvailability;
    }

    public void setAvailability(Long aircraftAvailability) {
        this.aircraftAvailability = aircraftAvailability;
    }

    public void printResource() {
        System.out.print("Aircraft Type = " + this.type +" and Aircraft Capacity =  " + this.capacity + " with ");
    }

}

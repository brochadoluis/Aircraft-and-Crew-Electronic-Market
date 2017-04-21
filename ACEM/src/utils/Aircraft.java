package utils;

import jade.core.AID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.LongToDoubleFunction;

/**
 * Created by Luis on 20/02/2017.
 */
public class Aircraft extends Resource implements Serializable {
    /**
     * Needed aircraft type : @type
     * Needed aircraft capacity : @capacity
     */
    private String type;
    private Integer capacity;
    private String origin;
    private String destination;
    private Long scheduledDeparture = null;
    private Long tripTime = null;
    private Long aircraftAvailability = null;

    public Aircraft(){}

    public Aircraft(String type, Integer capacity) {
        super();
        this.type = type;
        this.capacity = capacity;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public long getScheduledDeparture() {
        return scheduledDeparture;
    }

    public void setScheduledDeparture(long scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
    }

    public long getTripTime() {
        return tripTime;
    }

    public void setTripTime(long tripTime) {
        this.tripTime = tripTime;
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

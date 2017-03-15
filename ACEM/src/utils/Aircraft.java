package utils;

import java.io.Serializable;
import java.util.function.LongToDoubleFunction;

/**
 * Created by Luis on 20/02/2017.
 */
public class Aircraft extends Resource implements Serializable{
    /**
     * Needed aircraft type : @type
     * Needed aircraft capacity : @capacity
     */
    private String type;
    private Integer capacity;
    private Long aircraftAvailability = null;
    private Long aircraftDelay = null;
    private double displacementCost = 0;

    public Aircraft(String type, Integer capacity) {
        super();
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

    @Override
    public Double getPrice() {
        return displacementCost;
    }

    @Override
    public void setPrice(double cost) {
        this.displacementCost = cost;
    }

    public Integer getNumber() {
        return null;
    }

    public String getCategory() {
        return null;
    }

    public String getQualifications() {
        return null;
    }

    @Override
    public Long getDelay(){
        return aircraftDelay;
    }

    @Override
    public void setDelay(Long aircraftDelay) {
        this.aircraftDelay = aircraftDelay;
    }

    @Override
    public Long getAvailability() {
        return aircraftAvailability;
    }

    @Override
    public void setAvailability(Long aircraftAvailability) {
        this.aircraftAvailability = aircraftAvailability;
    }

    @Override
    public void printResource() {
        System.out.print("Aircraft Type = " + this.type +" and Aircraft Capacity =  " + this.capacity + " with ");
    }

}

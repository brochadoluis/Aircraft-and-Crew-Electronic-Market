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
    private String tail_number;
    private String type;
    private Integer capacity;
    private Long aircraftAvailability = null;
    private double atc_avg_cost_nautical_mile;
    private double maintenance_avg_cost_minute;
    private double fuel_avg_cost_minute;
    private double airport_handling_cost;
    private int cptNumber = 0;
    private int optNumber = 0;
    private int scbNumber = 0;
    private int ccbNumber = 0;
    private int cabNumber = 0;


    public Aircraft(){}

    public Aircraft(/*String type, */Integer capacity) {
//        this.type = type;
        this.capacity = capacity;
    }

    public String getTail_number() {
        return tail_number;
    }

    public void setTail_number(String tail_number) {
        this.tail_number = tail_number;
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

    public double getCostNauticalMile() {
        return atc_avg_cost_nautical_mile;
    }

    public void setCostNauticalMile(double atc_avg_cost_nautical_mile) {
        this.atc_avg_cost_nautical_mile = atc_avg_cost_nautical_mile;
    }

    public double getMaintenanceCost() {
        return maintenance_avg_cost_minute;
    }

    public void setMaintenanceCost(double maintenance_avg_cost_minute) {
        this.maintenance_avg_cost_minute = maintenance_avg_cost_minute;
    }

    public double getFuelCost() {
        return fuel_avg_cost_minute;
    }

    public void setFuelCost(double fuel_avg_cost_minute) {
        this.fuel_avg_cost_minute = fuel_avg_cost_minute;
    }

    public double getAirportHandlingCost() {
        return airport_handling_cost;
    }

    public void setAirportHandlingCost(double airport_handling_cost) {
        this.airport_handling_cost = airport_handling_cost;
    }

    public int getCptNumber() {
        return cptNumber;
    }

    public void setCptNumber(int cptNumber) {
        this.cptNumber = cptNumber;
    }

    public int getOptNumber() {
        return optNumber;
    }

    public void setOptNumber(int optNumber) {
        this.optNumber = optNumber;
    }

    public int getScbNumber() {
        return scbNumber;
    }

    public void setScbNumber(int scbNumber) {
        this.scbNumber = scbNumber;
    }

    public int getCcbNumber() {
        return ccbNumber;
    }

    public void setCcbNumber(int ccbNumber) {
        this.ccbNumber = ccbNumber;
    }

    public int getCabNumber() {
        return cabNumber;
    }

    public void setCabNumber(int cabNumber) {
        this.cabNumber = cabNumber;
    }

    public boolean equals(Aircraft anAircraft){
        return (Math.abs(this.atc_avg_cost_nautical_mile - anAircraft.getCostNauticalMile()) < 0.000000000001 &&
                Math.abs(this.maintenance_avg_cost_minute - anAircraft.getMaintenanceCost()) < 0.000000000001 &&
                Math.abs(this.fuel_avg_cost_minute - anAircraft.getFuelCost()) < 0.000000000001 &&
                        Math.abs(this.airport_handling_cost - anAircraft.getAirportHandlingCost()) < 0.000000000001);
    }

    public void printResource() {
        System.out.print("Aircraft Type = " + this.type +" and Aircraft Capacity =  " + this.capacity + " with ");
    }

}

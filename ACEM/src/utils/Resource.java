package utils;

import java.io.Serializable;

/**
 * Created by Luis on 20/02/2017.
 */
public class Resource implements Serializable {

    private double displacementCost;
    private long availability;
    private long delay;
    private String fleet;

    public Resource() {
    }

    /**
     * Common methods to Aircraft and Crew
     */
    public Double getPrice() {
        return this.displacementCost;
    }

    public void setPrice(double displacementCost) {
        this.displacementCost = displacementCost;
    }

    public Long getAvailability() {
        return this.availability;
    }

    public void setAvailability(Long availability) {
        this.availability = availability;
    }

    public Long getDelay() {
        return this.delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public void printResource() {
        System.out.print("Resource Type = " + this.getClass().getSimpleName() +", fleet =  " + this.fleet + " and " + delay + " ms delay.");
    }

    public String getFleet() {
        return this.fleet;
    }

    public void setFleet(String fleet) {
        this.fleet = fleet;
    }
}

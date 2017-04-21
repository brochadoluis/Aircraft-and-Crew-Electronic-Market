package utils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Luis on 21/04/2017.
 */
public class Flight implements Serializable{
    private ArrayList<CrewMember> crewMembers;
    private Aircraft aircraft;
    private Long scheduledDeparture;
    private Long tripTime;
    private Long delay;
    private String fleet;
    private String origin;
    private String destination;
    /**
     * cost is the total cost of the resources. Depending on the disruption it can be:
     * Aircraft Cost + Crew Members Cost (aircraft and all crew needed)
     * Crew Members Cost (all crew needed)
     * Crew Member Cost (just one crew member needed)
     */
    private Double cost;

    public Flight() {
        this.crewMembers = new ArrayList<>();
    }

    public ArrayList<CrewMember> getCrewMembers() {
        return crewMembers;
    }

    public void setCrewMembers(ArrayList<CrewMember> crewMembers) {
        this.crewMembers = crewMembers;
    }

    public void addCrewMember(CrewMember cm){
        this.crewMembers.add(cm);
    }

    public Aircraft getAircraft() {
        return aircraft;
    }

    public void setAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
    }



    public Double getPrice() {
        return this.cost;
    }

    public void setPrice(double cost) {
        this.cost = cost;
    }

    public Long getDelay() {
        return this.delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public String getFleet() {
        return this.fleet;
    }

    public void setFleet(String fleet) {
        this.fleet = fleet;
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

    public void printFlgiht() {
        System.out.print("Aircraft = " + this.aircraft+ ", Crew Members =  " + this.crewMembers +
                ", fleet =  " + this.fleet +", departure =  " + this.scheduledDeparture +
                ", trip time =  " + this.tripTime + " and " + delay + " ms delay.");
    }

}

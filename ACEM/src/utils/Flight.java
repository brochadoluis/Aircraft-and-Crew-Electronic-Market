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
    private Long availability;
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

    public Long getAvailability() {
        return availability;
    }

    public void setAvailability(Long availability) {
        this.availability = availability;
    }

    public boolean equals(Flight aFlight){
        /*if (this.getAircraft() != null) {
            if (this.getAircraft().equals(f1.getAircraft())) {
                for (int i = 0; i < crewMembers.size(); i++) {
                    if (!this.crewMembers.get(i).equals(f1.getCrewMembers().get(i)))
                        return false;
                }
                return true;
            } else
                return false;
        }
        else {
            for (int i = 0; i < crewMembers.size(); i++) {
                if (!this.crewMembers.get(i).equals(f1.getCrewMembers().get(i)))
                    return false;
            }
            return true;
        }*/
        System.out.println("this cost " +this.cost);
        System.out.println("aflight cost " + aFlight.getPrice());
        System.out.println("this availability " + this.availability);
        System.out.println("aflight availability "+ aFlight.getAvailability());
        return ((Math.abs(this.cost - aFlight.getPrice())< 0.000000000001) &&
                Math.abs(this.availability - aFlight.getAvailability()) < 0.000000000001);
    }

    public void printFlight() {
        System.out.print("Aircraft = " + this.aircraft.getTail_number()+ ", Crew Members =  " + this.crewMembers +
                ", fleet =  " + this.fleet +", departure =  " + this.scheduledDeparture +
                ", trip time =  " + this.tripTime + " and " + availability + " ms delay and costing " +this.cost + ".\n");
    }

}

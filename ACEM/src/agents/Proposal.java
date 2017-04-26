package agents;

import jade.core.AID;
import utils.Flight;
import utils.Resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luis on 03/03/2017.
 */
public class Proposal implements Serializable {
    private Double price;
    private Long availability;
    private Flight flight;
    /**
     * private Date availability; ou Integer, em que 0 significa que cumpre o horario, mesmo que seja com antecedência,
     * ou seja, nao importa se entrega antes de tempo, importa que o voo sera cumprido sem atraso
     * e cada incremento significa 1 minuto de atraso
     */
    private String priceComment;
    private String availabilityComment;
    private AID sender;

    public Proposal(double price, long availability, Flight disruptedFlight, AID sender) {
        this.price = price;
        this.availability = availability;
        this.flight = disruptedFlight;
        this.sender = sender;
    }

    public Proposal(double price, long availability,AID sender) {
        this.price = price;
        this.availability = availability;
        this.sender = sender;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getAvailability() {
        return availability;
    }

    public void setAvailability(long availability) {
        this.availability = availability;
    }

    public String getPriceComment() {
        return priceComment;
    }

    public void setPriceComment(String priceComment) {
        this.priceComment = priceComment;
    }

    public String getAvailabilityComment() {
        return availabilityComment;
    }

    public void setAvailabilityComment(String availabilityComment) {
        this.availabilityComment = availabilityComment;
    }

    public Flight getFlight() {
        return flight;
    }

    public void setFlight(Flight flight) {
        this.flight = flight;
    }

    public AID getSender() {
        return sender;
    }

    public void setSender(AID sender) {
        this.sender = sender;
    }

    public void printProposal(){
        System.out.println("Price = " + this.price +"€ and availability = " + availability + ". Comments: " + priceComment + " price  and " + availabilityComment + " availability.");
    }

    public void printComments(){
        System.out.println("Comment on price = " + this.priceComment +", and comment on availability = " + this.availabilityComment);
    }

    public String toString(){
        return "< " + this.getPriceComment() + " , " + this.getAvailabilityComment() +" > " ;
    }
}

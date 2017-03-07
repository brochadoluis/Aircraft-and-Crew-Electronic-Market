package agents;

import jade.core.AID;
import utils.Resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Luis on 03/03/2017.
 */
public class Proposal implements Serializable {
    private double price;
    /**
     * private Date availability; ou Integer, em que 0 significa que cumpre o horario, mesmo que seja com antecedência,
     * ou seja, nao importa se entrega antes de tempo, importa que o voo sera cumprido sem atraso
     * e cada incremento significa 1 minuto de atraso
     */
    private String priceComment;
    private String availabilityComment;
    private ArrayList<Resource> resourcesProposed = new ArrayList<>();
    private Integer round;

    private AID sender;

    public Proposal(double price, /*Date availability,*/ List<Resource> resourcesProposed, AID sender) {
        this.price = price;
        this.resourcesProposed = (ArrayList<Resource>) resourcesProposed;
        this.sender = sender;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

//    public Date getAvailability() {
//        return availability;
//    }

//    public void setAvailability(Date availability) {
//        this.availability = availability;
//    }

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

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public List<Resource> getResourcesProposed() {
        return resourcesProposed;
    }

    public void setResourcesProposed(List<Resource> resourcesProposed) {
        this.resourcesProposed = (ArrayList<Resource>) resourcesProposed;
    }

    public AID getSender() {
        return sender;
    }

    public void setSender(AID sender) {
        this.sender = sender;
    }

    public void printProposal(){
        System.out.println("Price = " + this.price +"€. Comments: " + priceComment + " price ");
    }

    public void printComments(){
        System.out.println("Comment on price = " + this.priceComment /*+", and comment on availability = " + this.availabilityComment*/);
    }
}

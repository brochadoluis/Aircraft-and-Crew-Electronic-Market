package agents;

import java.util.Date;

/**
 * Created by Luis on 03/03/2017.
 */
public class Proposal {
    private double price;
    private Date availability;
    private String priceComment,availabilityComment;
    //Resources being negotiated?

    public Proposal(double price, Date availability) {
        this.price = price;
        this.availability = availability;
        this.priceComment = "";
        this.availabilityComment = "";
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Date getAvailability() {
        return availability;
    }

    public void setAvailability(Date availability) {
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
}

package utils;

import java.io.Serializable;

/**
 * Created by Luis on 20/02/2017.
 */
public abstract class Resource implements Serializable{
   // private Date startTime, duration, delay;
    private float price;
    //disponibilidade?

    /*public Resource(Date startTime, Date duration, Date delay, double price) {
        super();
        this.startTime = startTime;
        this.duration = duration;
        this.delay = delay;
        this.price = price;
    }*/
    public Resource(float price) {
        super();
        this.price = price;
    }

    /*public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getDuration() {
        return duration;
    }

    public void setDuration(Date duration) {
        this.duration = duration;
    }

    public Date getDelay() {
        return delay;
    }

    public void setDelay(Date delay) {
        this.delay = delay;
    }
*/
    public double getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public abstract void printResource();

    public void print(){
        System.out.println(/*"Departure Date = " + this.startTime +
                ",  predicted delay =  " + this.delay +
                ",  flight duration =  " + this.duration +
                ", */ "Estimated cost =  " + this.price);
    }

    public abstract boolean compareResource(Resource r);

    public abstract String getType();

    public abstract Integer getCapacity();

    public abstract Integer getNumber();

    public abstract String getCategory();

    public abstract String getQualifications();

}

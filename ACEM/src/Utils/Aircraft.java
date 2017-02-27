package Utils;

import java.io.Serializable;

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


    /*public Aircraft(Date startTime, Date duration, Date delay, float price) {
		super(startTime, duration, delay, price);
		// TODO Auto-generated constructor stub
	}*/

    public Aircraft(float price, String type, Integer capacity) {
		super(price);
        this.type = type;
        this.capacity = capacity;
	}

	/*public Aircraft(String type, Integer capacity) {
        this.type = type;
        this.capacity = capacity;
    }*/

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void printAircraft(){
        System.out.println("Aircraft Type = " + this.type +" and Aircraft Capacity =  " + this.capacity);
    }

    @Override
    public void printResource() {
        System.out.print("Aircraft Type = " + this.type +" and Aircraft Capacity =  " + this.capacity + " with ");
        super.print();
        System.out.println();

    }

    public boolean compareAircraft(Aircraft a1){
        if(a1.getType().equals(this.getType()) && a1.getCapacity().equals(this.getCapacity()))
            return true;
        else
            return false;
    }

    @Override
    public boolean compareResource(Resource r1) {
        if(r1.getClass().equals(this.getClass())){

            return compareAircraft((Aircraft) r1);
        }
        return false;
    }

    @Override
    public Integer getNumber() {
        return null;
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getQualifications() {
        return null;
    }
}

package utils;

import java.io.Serializable;

/**
 * Created by Luis on 20/02/2017.
 */
public class CrewMember extends Resource implements Serializable{
    /**
     * Needed crew members : @number
     * Needed crew members category : @category
     * Needed crew members qualifications : @qualifications
     */
    private Integer number;
    private String category;
    private String qualifications;

    /*public CrewMember(Integer number, String category, String qualifications) {
        this.number = number;
        this.category = category;
        this.qualifications = qualifications;
    }*/

    public CrewMember(float price, Integer number, String category, String qualifications) {
        super(price);
        this.number = number;
        this.category = category;
        this.qualifications = qualifications;
    }

    @Override
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getQualifications() {
        return qualifications;
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }

    @Override
    public void printResource() {
        System.out.print("Crew Member Number = " + this.number +", Crew Member Category =  "
                + this.category +" and Crew Member qualifications =  " + this.qualifications + " with ");
        super.print();
    }

    public boolean compareCrewMembers(CrewMember cm){
        return(cm.getNumber().equals(this.getNumber()) &&
                cm.getCategory().equals(this.getCategory()) &&
                cm.getQualifications().equals(this.getQualifications()));
    }

    @Override
    public boolean compareResource(Resource r) {
        if(r.getClass().equals(this.getClass())){

            return compareCrewMembers((CrewMember) r);
        }
        return false;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public Integer getCapacity() {
        return null;
    }
}

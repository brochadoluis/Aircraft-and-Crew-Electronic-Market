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
    private Long crewMemberAvailability = null;

    public CrewMember(Integer number, String category, String qualifications) {
        super();
        this.number = number;
        this.category = category;
        this.qualifications = qualifications;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQualifications() {
        return qualifications;
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }

    public void printResource() {
        System.out.print("Crew Member Number = " + this.number +", Crew Member Category =  "
                + this.category +" and Crew Member qualifications =  " + this.qualifications + " with ");
    }

    public String getType() {
        return null;
    }

    public Integer getCapacity() {
        return null;
    }

    @Override
    public Long getAvailability() {
        return crewMemberAvailability;
    }

    @Override
    public void setAvailability(Long crewMemberAvailability) {
        this.crewMemberAvailability = crewMemberAvailability;
    }
}

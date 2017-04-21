package utils;

import java.io.Serializable;

/**
 * Created by Luis on 20/02/2017.
 */
public class CrewMember implements Serializable {
    /**
     * Needed crew members : @number
     * Needed crew members category : @category
     * Needed crew members qualifications : @qualifications
     */
    private Integer number;
    private String category;
    private String qualifications;
    private Long crewMemberAvailability = null;

    public CrewMember(){}

    public CrewMember(Integer number, String category, String qualifications) {
        this.number = number;
        this.category = category;
        this.qualifications = qualifications;
    }

    private double displacementCost;

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
        System.out.print("Crew Member Number = " + this.number +" and Crew Member Category =  "
                + this.category);
    }

    public Long getAvailability() {
        return crewMemberAvailability;
    }

    public void setAvailability(Long crewMemberAvailability) {
        this.crewMemberAvailability = crewMemberAvailability;
    }

}

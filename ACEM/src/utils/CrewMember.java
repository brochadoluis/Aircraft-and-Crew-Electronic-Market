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
    private double crewMemberId;
    private int seniority;
    private String status;
    private int hourly_salary;
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

    public double getCrewMemberId() {
        return crewMemberId;
    }

    public void setCrewMemberId(double crewMemberId) {
        this.crewMemberId = crewMemberId;
    }

    public int getSeniority() {
        return seniority;
    }

    public void setSeniority(int seniority) {
        this.seniority = seniority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getHourly_salary() {
        return hourly_salary;
    }

    public void setHourly_salary(int hourly_salary) {
        this.hourly_salary = hourly_salary;
    }

    public String getQualifications() {
        return qualifications;
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }

    public Long getAvailability() {
        return crewMemberAvailability;
    }

    public void setAvailability(Long crewMemberAvailability) {
        this.crewMemberAvailability = crewMemberAvailability;
    }

    public boolean equals(CrewMember aCrewMember){
        return (Math.abs(hourly_salary - aCrewMember.getHourly_salary()) <  0.000000000001);
    }

    public void printResource() {
        System.out.print("Crew Member Number = " + this.crewMemberId +" and Crew Member Category =  "
                + this.category +" with " + this.hourly_salary + " salary and " + this.seniority + " seniority. " );
    }
}

package utils;

import java.io.Serializable;

/**
 * Created by Luis on 20/02/2017.
 */
public abstract class Resource implements Serializable{

    public Resource() {

        super();
    }

    public abstract void printResource();

    public abstract Long getAvailability();

    public abstract void setAvailability(Long availability);
}

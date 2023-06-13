package com.example.falschefreunde;

/**
 *Class for the database
 * time is commented out since it is not implemented
 */
public class Dataset {

    /**
     * id for the row
     */
    public long id;

    /**
     * Name of the player
     */
    public String name;

    /**
     *time the player needed for all levels
     */
    public String time;

    /**
     * Constructor
     * @param name: Name of the player
     * @param time: Time the player needed to finish all levels
     */
    public Dataset(String name, String time){
        this.name = name;
        this.time = time;

        id = -1;
    }

    /**
     * Constructor without parameter
     */
    public Dataset(){

    }
}
